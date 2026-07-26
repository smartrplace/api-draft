package org.smartrplace.os.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.simple.StringResource;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;

/** Resolves the MAC address of a device that is reachable via IP on the local network.
 *
 * <p>The MAC address of a remote host is only visible on the same layer-2 network segment. On the
 * gateway (Ubuntu/Linux) it is obtained from the kernel ARP/neighbour cache: the host is pinged once
 * to make sure it has an entry in the cache, then {@code ip neigh show <host>} (fallback
 * {@code arp -n <host>}) is read and the MAC parsed out. If the device is behind a router/bridge the
 * MAC reported is the one of that controller (see the network-identifier documentation on
 * {@link InstallAppDevice#networkIdentifier()}).</p>
 *
 * <p>This is a Linux/Ubuntu-only feature; on Windows (dev machine) {@link #resolveMacForIp} returns
 * {@code null}. All external commands are executed via {@link ProcessBuilder} argument arrays (no
 * shell), so the host value cannot be used for shell injection; in addition the host is validated
 * against a strict character set.</p>
 */
public class MacAddressResolver {

	private static final Pattern MAC_PATTERN = Pattern.compile("([0-9a-fA-F]{2}:){5}[0-9a-fA-F]{2}");
	private static final Pattern HOST_PATTERN = Pattern.compile("[0-9a-zA-Z.\\-]+");
	private static final String ZERO_MAC = "00:00:00:00:00:00";

	/** Extract the bare host/IP from values like {@code "http://192.168.1.5:2001"}, {@code "/192.168.1.5"}
	 * or {@code "192.168.1.5"}. Returns {@code null} if nothing usable is contained. */
	public static String extractHost(String ipOrUrl) {
		if(ipOrUrl == null)
			return null;
		String s = ipOrUrl.trim();
		if(s.isEmpty())
			return null;
		int scheme = s.indexOf("//");
		if(scheme >= 0)
			s = s.substring(scheme + 2);
		else if(s.startsWith("/"))
			s = s.substring(1);
		int slash = s.indexOf('/');
		if(slash >= 0)
			s = s.substring(0, slash);
		// strip ":port" (IPv4 / hostname only, IPv6 is not expected for these devices)
		int colon = s.indexOf(':');
		if(colon >= 0)
			s = s.substring(0, colon);
		s = s.trim();
		return s.isEmpty() ? null : s;
	}

	/** Resolve the MAC address for the given IP or URL. Blocking (up to a few seconds). Returns the
	 * lower-case colon-separated MAC or {@code null} if it could not be determined (host unreachable,
	 * not on the local segment, running on Windows, command not available, ...). */
	public static String resolveMacForIp(String ipOrUrl, ApplicationManager appMan) {
		String host = extractHost(ipOrUrl);
		if(host == null || !HOST_PATTERN.matcher(host).matches())
			return null;
		String os = System.getProperty("os.name");
		if(os != null && os.toLowerCase().contains("windows"))
			return null; // ARP-cache lookup is a Linux/Ubuntu-only feature

		// make sure the host has an entry in the ARP/neighbour cache
		runQuietly(new String[] {"ping", "-c", "1", "-W", "1", host}, 3);

		String mac = parseMac(runCapturing(new String[] {"ip", "neigh", "show", host}, 3));
		if(mac == null)
			mac = parseMac(runCapturing(new String[] {"arp", "-n", host}, 3));
		if(mac != null && appMan != null)
			appMan.getLogger().info("Resolved MAC {} for host {}", mac, host);
		return mac;
	}

	/** Resolve the MAC address for the given IP/URL in a background thread and store it in
	 * {@link InstallAppDevice#macAddress()} <b>only if it differs</b> from the currently stored value. If the
	 * resolved value equals the stored one nothing is written, so the last-write timestamp of the resource is
	 * kept unchanged. This is meant to be called on every system startup: a MAC that changed because the
	 * hardware was exchanged is updated, an unchanged one is left untouched, and if the MAC cannot be resolved
	 * (host down / not on the local segment) the existing value is kept. The lookup is done off the calling
	 * (OGEMA startup) thread because pinging can block for up to a second per device. */
	public static void updateMacAddressIfChanged(final InstallAppDevice object, final String ipOrUrl,
			final ApplicationManager appMan) {
		if(object == null)
			return;
		final StringResource macRes = object.macAddress();
		final String host = extractHost(ipOrUrl);
		if(host == null)
			return;
		Thread t = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					String mac = resolveMacForIp(host, appMan);
					if(mac == null)
						return; // could not determine - keep whatever is stored
					String current = macRes.isActive() ? macRes.getValue() : null;
					if(current != null && mac.equalsIgnoreCase(current.trim()))
						return; // unchanged - do not write to keep the last-write timestamp
					macRes.create();
					macRes.setValue(mac);
					macRes.activate(true);
					if(appMan != null)
						appMan.getLogger().info("Set MAC {} for device {} (was {})", mac,
								object.getLocation(), current);
				} catch(Exception e) {
					if(appMan != null)
						appMan.getLogger().warn("Could not resolve/set MAC for device "
								+ object.getLocation(), e);
				}
			}
		}, "mac-resolve-" + host);
		t.setDaemon(true);
		t.start();
	}

	static String parseMac(String text) {
		if(text == null)
			return null;
		Matcher m = MAC_PATTERN.matcher(text);
		while(m.find()) {
			String mac = m.group().toLowerCase();
			if(!mac.equals(ZERO_MAC))
				return mac;
		}
		return null;
	}

	private static String runCapturing(String[] cmd, int maxSeconds) {
		try {
			Process p = new ProcessBuilder(cmd).redirectErrorStream(true).start();
			StringBuilder sb = new StringBuilder();
			try(Reader r = new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8)) {
				char[] buf = new char[1024];
				int read;
				while((read = r.read(buf)) != -1)
					sb.append(buf, 0, read);
			}
			p.waitFor(maxSeconds, TimeUnit.SECONDS);
			return sb.toString();
		} catch(IOException e) {
			return null; // command not available (e.g. "ip"/"arp" missing) -> caller falls back
		} catch(InterruptedException e) {
			Thread.currentThread().interrupt();
			return null;
		}
	}

	private static void runQuietly(String[] cmd, int maxSeconds) {
		try {
			Process p = new ProcessBuilder(cmd).redirectErrorStream(true).start();
			try(InputStream in = p.getInputStream()) {
				byte[] b = new byte[512];
				while(in.read(b) != -1) {
					// drain so the process does not block on a full output buffer
				}
			}
			p.waitFor(maxSeconds, TimeUnit.SECONDS);
		} catch(IOException e) {
			// ping not permitted or host down - ignore, the cache may still have an entry
		} catch(InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}
}
