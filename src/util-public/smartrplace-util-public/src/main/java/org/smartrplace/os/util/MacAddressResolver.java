package org.smartrplace.os.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
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
 * to make sure it has an entry in the cache, then the kernel ARP table ({@code /proc/net/arp}, with
 * {@code ip neigh show <host>} / {@code arp -n <host>} as fallbacks) is read and the MAC parsed out. If
 * the device is behind a router/bridge the MAC reported is the one of that controller (see the
 * network-identifier documentation on {@link InstallAppDevice#networkIdentifier()}).</p>
 *
 * <p>{@link #updateMacAddressIfChanged} stores the resolved MAC only when it differs from the stored one
 * (keeping the last-write timestamp otherwise). If the MAC cannot be determined and no value is stored yet
 * it stores {@link #MAC_NOT_FOUND_MESSAGE} as a placeholder; an already stored (real or manual) value is
 * never overwritten by that placeholder.</p>
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

	/** Placeholder stored in the MAC resource when the MAC could not be determined and none was stored before.
	 * Kept as a stable string so that a later successful resolution replaces it (and an unchanged failure does
	 * not rewrite it). */
	public static final String MAC_NOT_FOUND_MESSAGE = "MAC not found";

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

		// make sure the host has an entry in the ARP/neighbour cache (ARP is answered on layer 2 even if
		// the host does not reply to ICMP, so a "failed" ping can still populate the cache)
		runQuietly(new String[] {"ping", "-c", "1", "-W", "2", host}, 4);

		// primary: the kernel ARP table, always readable as a file (no external command needed)
		String mac = readMacFromProcArp(host);
		// fallbacks: neighbour cache / arp command (also cover hosts learned without an arp file entry)
		if(mac == null)
			mac = parseMac(runCapturing(new String[] {"ip", "neigh", "show", host}, 3));
		if(mac == null)
			mac = parseMac(runCapturing(new String[] {"arp", "-n", host}, 3));
		if(appMan != null) {
			if(mac != null)
				appMan.getLogger().info("Resolved MAC {} for host {}", mac, host);
			else
				appMan.getLogger().warn("Could not determine MAC for host {} - device offline, not "
						+ "answering ARP or not on the local network segment", host);
		}
		return mac;
	}

	/** Look up the MAC of a host in the kernel ARP table (/proc/net/arp). Only complete entries (flag 0x2) with
	 * a non-zero MAC are accepted. Returns {@code null} if the host is not (yet) in the table. */
	static String readMacFromProcArp(String host) {
		try {
			List<String> lines = Files.readAllLines(Paths.get("/proc/net/arp"), StandardCharsets.UTF_8);
			for(String line : lines) {
				// columns: IPaddress HWtype Flags HWaddress Mask Device
				String[] cols = line.trim().split("\\s+");
				if(cols.length >= 4 && cols[0].equals(host)) {
					String mac = cols[3];
					if(MAC_PATTERN.matcher(mac).matches() && !mac.equalsIgnoreCase(ZERO_MAC))
						return mac.toLowerCase();
				}
			}
		} catch(IOException | RuntimeException e) {
			// not on Linux or /proc/net/arp unreadable -> caller falls back to the ip/arp commands
		}
		return null;
	}

	/** Resolve the MAC address of the device (from the given IP-address resource) in a background thread and
	 * store it in {@link InstallAppDevice#macAddress()}. Meant to be called once on device startup.
	 * <ul>
	 * <li>The IP resource ({@code ipSource}) is a live handle to a fixed path; it is polled <b>inside</b> the
	 *   background thread because the driver typically populates the device IP only some time (sometimes several
	 *   minutes) after the device itself is detected.</li>
	 * <li>If the IP is not available after a short grace period
	 *   ({@code org.smartrplace.os.util.MacAddressResolver.macGraceSeconds}, default 120s) the placeholder
	 *   {@link #MAC_NOT_FOUND_MESSAGE} is stored for quick feedback, but the thread <b>keeps polling</b> up to
	 *   {@code ...maxIpWaitSeconds} (default 1800s) so a late IP still leads to the real MAC replacing the
	 *   placeholder.</li>
	 * <li>A resolved MAC is stored only when it <b>differs</b> from the stored value, so an unchanged MAC does
	 *   not rewrite the resource (its last-write timestamp is kept). A changed MAC (e.g. after a hardware
	 *   exchange) is updated.</li>
	 * <li>If the MAC cannot be determined (IP never appeared, host down, not on the local segment, running on
	 *   Windows): a real/manual MAC already stored is kept; otherwise the placeholder is stored.</li>
	 * </ul>
	 * The lookup runs off the calling (OGEMA startup) thread because waiting for the IP and pinging block. */
	public static void updateMacAddressIfChanged(final InstallAppDevice object, final StringResource ipSource,
			final ApplicationManager appMan) {
		if(object == null || ipSource == null)
			return;
		final StringResource macRes = object.macAddress();
		// synchronous log on the calling (startup) thread so we can see the hook actually fired for this device
		if(appMan != null)
			appMan.getLogger().info("MAC check scheduled for device {} (ip resource {}, active={})",
					object.getLocation(), ipSource.getPath(), ipSource.isActive());
		Thread t = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					final long graceMs = 1000L * Integer.getInteger(
							"org.smartrplace.os.util.MacAddressResolver.macGraceSeconds", 120);
					final long maxWaitMs = 1000L * Integer.getInteger(
							"org.smartrplace.os.util.MacAddressResolver.maxIpWaitSeconds", 1800);
					final long intervalMs = 15000;
					final long start = System.currentTimeMillis();
					boolean placeholderWritten = false;

					// poll the live IP handle until it becomes available (or we give up)
					String host = null;
					while(true) {
						host = extractHost(ipSource.isActive() ? ipSource.getValue() : null);
						if(host != null)
							break;
						long elapsed = System.currentTimeMillis() - start;
						if(!placeholderWritten && elapsed >= graceMs) {
							// still no IP after the grace period: show the error placeholder now, but keep
							// polling so the real MAC can still replace it once the IP appears.
							storePlaceholderIfNone(macRes, object, appMan, null);
							placeholderWritten = true;
						}
						if(elapsed >= maxWaitMs)
							break;
						Thread.sleep(intervalMs);
					}

					if(host == null) {
						// IP never became available within the wait window
						if(!placeholderWritten)
							storePlaceholderIfNone(macRes, object, appMan, null);
						return;
					}

					String mac = resolveMacForIp(host, appMan);
					if(mac == null)
						storePlaceholderIfNone(macRes, object, appMan, host);
					else
						storeResolvedMac(macRes, object, appMan, mac);
				} catch(Exception e) {
					if(appMan != null)
						appMan.getLogger().warn("Could not resolve/set MAC for device "
								+ object.getLocation(), e);
				}
			}
		}, "mac-resolve-" + object.getName());
		t.setDaemon(true);
		t.start();
	}

	/** Store a successfully resolved MAC, but only if it differs from the currently stored value (keeps the
	 * last-write timestamp for an unchanged MAC; replaces a placeholder or an outdated MAC). */
	private static void storeResolvedMac(StringResource macRes, InstallAppDevice object, ApplicationManager appMan,
			String mac) {
		String current = macRes.isActive() ? macRes.getValue() : null;
		String currentTrim = (current == null) ? "" : current.trim();
		if(mac.equalsIgnoreCase(currentTrim))
			return; // unchanged - do not write to keep the last-write timestamp
		writeMac(macRes, mac);
		if(appMan != null)
			appMan.getLogger().info("Set MAC {} for device {} (was {})", mac, object.getLocation(), current);
	}

	/** Store the {@link #MAC_NOT_FOUND_MESSAGE} placeholder if no real/manual MAC is stored yet. A placeholder
	 * already present is not rewritten (keeps its timestamp). {@code host} is only used for logging. */
	private static void storePlaceholderIfNone(StringResource macRes, InstallAppDevice object,
			ApplicationManager appMan, String host) {
		String current = macRes.isActive() ? macRes.getValue() : null;
		String currentTrim = (current == null) ? "" : current.trim();
		// keep an already stored real MAC or manually entered value untouched
		if(!currentTrim.isEmpty() && !currentTrim.equalsIgnoreCase(MAC_NOT_FOUND_MESSAGE))
			return;
		if(currentTrim.equals(MAC_NOT_FOUND_MESSAGE)) {
			if(appMan != null)
				appMan.getLogger().warn("MAC still not found for device {} (ip '{}'), keeping placeholder '{}'",
						object.getLocation(), host, MAC_NOT_FOUND_MESSAGE);
			return;
		}
		writeMac(macRes, MAC_NOT_FOUND_MESSAGE);
		if(appMan != null)
			appMan.getLogger().warn("No MAC found (yet) for device {} (ip '{}'), stored placeholder '{}'",
					object.getLocation(), host, MAC_NOT_FOUND_MESSAGE);
	}

	private static void writeMac(StringResource macRes, String value) {
		macRes.create();
		macRes.setValue(value);
		macRes.activate(true);
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
