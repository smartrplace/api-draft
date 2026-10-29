package org.smartrplace.os.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.core.model.ResourceList;
import org.ogema.core.model.simple.StringResource;
import org.ogema.messaging.api.MailSessionServiceI;
import org.ogema.model.gateway.LocalGatewayInformation;
import org.ogema.model.user.NaturalPerson;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.tissue.util.resource.GatewayUtil;

import de.iwes.util.resource.ResourceHelper;
import de.iwes.util.resource.ValueResourceHelper;

/** Resolves the MAC address of a device that is reachable via IP on the local network.
 *
 * <p>The MAC address of a remote host is only visible on the same layer-2 network segment. On the
 * gateway (Ubuntu/Linux) it is obtained from the kernel ARP/neighbour cache: the host is pinged once
 * to make sure it has an entry in the cache, then the kernel ARP table ({@code /proc/net/arp}, with
 * {@code ip neigh show <host>} / {@code arp -n <host>} as fallbacks) is read and the MAC parsed out. If
 * the device is behind a router/bridge the MAC reported is the one of that controller (see the
 * network-identifier documentation on {@link InstallAppDevice#networkIdentifier()}).</p>
 *
 * <p>{@link #updateMacAddressIfChanged} stores the resolved MAC only if no MAC is stored yet. A MAC that is
 * already stored is never replaced automatically: if a different MAC is detected it is added to
 * {@link InstallAppDevice#backupAddress()} as {@value #NEW_MAC_PREFIX}&lt;mac&gt; and an alarm email is sent
 * (only for the first new MAC, as long as such an entry exists no further entries are added). A hardware
 * exchange has to be confirmed by manually removing the old MAC, then the new MAC is stored on the next check.
 * If the MAC cannot be determined and no value is stored yet it stores {@link #MAC_NOT_FOUND_MESSAGE} as a
 * placeholder; an already stored (real or manual) value is never overwritten by that placeholder.</p>
 *
 * <p>If the IP of the device answers with the MAC of another known device (IP now used by another device)
 * {@link InstallAppDevice#macUpdateBlocked()} is set and no MAC is written while it is set. It is reset when
 * the device answers with its own stored MAC again (device online again).</p>
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

	/** Prefix of the entry added to {@link InstallAppDevice#backupAddress()} for a MAC that differs from the stored one */
	public static final String NEW_MAC_PREFIX = "NEW_MAC:";

	/** Contacts (gatewaySuperiorDataRes/responsibilityContacts) whose userRole contains this (case-insensitive)
	 * receive the MAC change email */
	public static final String MAC_CHANGE_RECIPIENT_ROLE = "terminvereinbarung";
	/** Recipient if no contact with role {@link #MAC_CHANGE_RECIPIENT_ROLE} is configured */
	public static final String MAC_CHANGE_FALLBACK_RECIPIENT = "alarming-prio@smartrplace.com";

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
	 * <li>A resolved MAC is stored only if no real MAC is stored yet (empty or placeholder), so an unchanged MAC
	 *   does not rewrite the resource (its last-write timestamp is kept). A changed MAC (e.g. after a hardware
	 *   exchange) is <b>not</b> stored, see {@link #storeResolvedMac}.</li>
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
							logDeviceIpDiagnostics(object, ipSource, appMan);
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
						storeResolvedMac(macRes, object, appMan, mac, host);
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

	/** Store a successfully resolved MAC:
	 * <ul>
	 * <li>Unchanged MAC: nothing is written (keeps the last-write timestamp). The device is online with its own MAC,
	 *   so a set {@link InstallAppDevice#macUpdateBlocked()} is reset.</li>
	 * <li>MAC of another known device: the IP is used by another device, {@link InstallAppDevice#macUpdateBlocked()}
	 *   is set and nothing is written.</li>
	 * <li>{@link InstallAppDevice#macUpdateBlocked()} set: nothing is written.</li>
	 * <li>No MAC stored yet (empty or placeholder): the MAC is stored, a {@value #NEW_MAC_PREFIX} entry in
	 *   {@link InstallAppDevice#backupAddress()} is removed (the change was confirmed by removing the old MAC).</li>
	 * <li>Different MAC stored: the old MAC is kept, the new one is added to
	 *   {@link InstallAppDevice#backupAddress()} and an alarm email is sent - unless a {@value #NEW_MAC_PREFIX} entry
	 *   already exists.</li>
	 * </ul> */
	private static void storeResolvedMac(StringResource macRes, InstallAppDevice object, ApplicationManager appMan,
			String mac, String host) {
		String current = macRes.isActive() ? macRes.getValue() : null;
		String currentTrim = (current == null) ? "" : current.trim();
		if(mac.equalsIgnoreCase(currentTrim)) {
			// unchanged - do not write to keep the last-write timestamp
			if(object.macUpdateBlocked().getValue()) {
				object.macUpdateBlocked().setValue(false);
				if(appMan != null)
					appMan.getLogger().info("Device {} online again with own MAC {}, reset MAC update block",
							object.getLocation(), mac);
			}
			return;
		}
		InstallAppDevice other = (appMan != null) ? getOtherDeviceWithMac(object, mac, appMan) : null;
		if(other != null) {
			ValueResourceHelper.setCreate(object.macUpdateBlocked(), true);
			object.macUpdateBlocked().activate(false);
			appMan.getLogger().warn("IP {} of device {} is used by device {} (MAC {}), blocking MAC update",
					host, object.getLocation(), other.getLocation(), mac);
			return;
		}
		if(object.macUpdateBlocked().getValue()) {
			if(appMan != null)
				appMan.getLogger().info("MAC update blocked for device {}, not storing MAC {} (stored: {})",
						object.getLocation(), mac, current);
			return;
		}
		if(currentTrim.isEmpty() || currentTrim.equalsIgnoreCase(MAC_NOT_FOUND_MESSAGE)) {
			writeMac(macRes, mac);
			removeNewMacEntry(object.backupAddress());
			if(appMan != null)
				appMan.getLogger().info("Set MAC {} for device {} (was {})", mac, object.getLocation(), current);
			return;
		}
		// a different MAC is already stored: keep it, report only the first new one
		StringResource backup = object.backupAddress();
		String backupVal = backup.isActive() ? backup.getValue().trim() : "";
		if(backupVal.contains(NEW_MAC_PREFIX)) {
			if(appMan != null)
				appMan.getLogger().warn("Device {} reports MAC {} (stored: {}), NEW_MAC entry already exists: {}",
						object.getLocation(), mac, current, backupVal);
			return;
		}
		String newEntry = NEW_MAC_PREFIX + mac;
		backup.create();
		backup.setValue(backupVal.isEmpty() ? newEntry : (backupVal + " " + newEntry));
		backup.activate(false);
		if(appMan != null)
			appMan.getLogger().warn("Device {} reports changed MAC {} (stored: {}), added {} to backupAddress",
					object.getLocation(), mac, current, newEntry);
		sendMacChangedEmail(object, currentTrim, mac, host, appMan);
	}

	/** Return another device (not {@code object}) that has the given MAC stored, or {@code null} */
	private static InstallAppDevice getOtherDeviceWithMac(InstallAppDevice object, String mac, ApplicationManager appMan) {
		for(InstallAppDevice iad : appMan.getResourceAccess().getResources(InstallAppDevice.class)) {
			if(iad.equalsLocation(object))
				continue;
			StringResource otherMac = iad.macAddress();
			if(otherMac.isActive() && mac.equalsIgnoreCase(otherMac.getValue().trim()))
				return iad;
		}
		return null;
	}

	/** Remove a {@value #NEW_MAC_PREFIX} entry from the backup address, other content is kept */
	private static void removeNewMacEntry(StringResource backup) {
		if(!backup.isActive())
			return;
		String val = backup.getValue();
		if(!val.contains(NEW_MAC_PREFIX))
			return;
		backup.setValue(val.replaceAll(Pattern.quote(NEW_MAC_PREFIX) + "\\S*", "").trim().replaceAll("\\s+", " "));
	}

	/** Alarm email for a MAC that differs from the stored one, with link to the gateway start page. Sent via the
	 * customer email service ({@link MailSessionServiceI}, same as the device alarm reminders) to all responsibility
	 * contacts with role {@link #MAC_CHANGE_RECIPIENT_ROLE}, or to {@link #MAC_CHANGE_FALLBACK_RECIPIENT} if there is none. */
	private static void sendMacChangedEmail(InstallAppDevice object, String oldMac, String newMac, String host,
			ApplicationManager appMan) {
		if(appMan == null)
			return;
		BundleContext ctx = null;
		ServiceReference<MailSessionServiceI> serviceRef = null;
		try {
			List<String> recipients = getMacChangeRecipients(appMan);
			String gwId = GatewayUtil.getGatewayId(appMan.getResourceAccess());
			String devId = object.deviceId().isActive() ? object.deviceId().getValue() : object.getName();
			String subject = "New MAC address detected " + gwId + ": " + devId;
			StringBuilder sb = new StringBuilder();
			sb.append("A new MAC address was detected for device ").append(devId).append(" on gateway ").append(gwId)
					.append(" at IP ").append(host).append('.')
					.append("<br>Location: ").append(object.device().getLocation())
					.append("<br>Stored MAC address (kept): ").append(oldMac)
					.append("<br>New MAC address: ").append(newMac).append(" (added to backupAddress as ")
					.append(NEW_MAC_PREFIX).append(newMac).append(')')
					.append("<br><br>A device can change its MAC address if the hardware is replaced. This has to be ")
					.append("confirmed by manually removing the stored (first) MAC address in the Network view of the ")
					.append("device expert page. Then the new MAC address is stored with the next check (on restart). ")
					.append("Until then no further new MAC addresses are recorded for this device.")
					.append("<br>If the hardware was not replaced, check whether the IP address is now used by another device.");
			LocalGatewayInformation gwInfo = ResourceHelper.getLocalGwInfo(appMan);
			if(gwInfo != null && gwInfo.gatewayBaseUrl().isActive() && gwInfo.gatewayBaseUrl().getValue().length() > 5) {
				String baseUrl = gwInfo.gatewayBaseUrl().getValue();
				sb.append("<br><br>Gateway: <a href=\"").append(baseUrl).append("\">").append(baseUrl).append("</a>");
			}
			String msg = sb.toString();
			appMan.getLogger().info("Sending MAC change email for {} to {}: {}", object.getLocation(), recipients, msg);
			if(Boolean.getBoolean("org.ogema.messages.email.testwithoutconnection"))
				return;
			ctx = appMan.getAppID().getBundle().getBundleContext();
			serviceRef = ctx.getServiceReference(MailSessionServiceI.class);
			MailSessionServiceI emailService = (serviceRef != null) ? ctx.getService(serviceRef) : null;
			if(emailService == null) {
				appMan.getLogger().error("No mail service configured, cannot send MAC change email for {}", object.getLocation());
				return;
			}
			String senderEmail = System.getProperty("org.smartrplace.apps.alarmingconfig.devicealarmreminder.sender.email",
					"no-reply@smartrplace.com");
			String senderName = System.getProperty("org.smartrplace.apps.alarmingconfig.devicealarmreminder.sender.name",
					"Smartrplace Messaging");
			for(String recipient : recipients) {
				emailService.newMessage()
					.withSender(senderEmail, senderName)
					.withSubject(subject)
					.addHtml(msg)
					.addTo(recipient)
					.send();
			}
		} catch(Exception e) {
			appMan.getLogger().warn("Could not send MAC change email for device " + object.getLocation(), e);
		} finally {
			if(ctx != null && serviceRef != null) {
				try {
					ctx.ungetService(serviceRef);
				} catch(Exception ignore) {}
			}
		}
	}

	/** Email addresses of the responsibility contacts with role {@link #MAC_CHANGE_RECIPIENT_ROLE}, or
	 * {@link #MAC_CHANGE_FALLBACK_RECIPIENT} if there is none */
	private static List<String> getMacChangeRecipients(ApplicationManager appMan) {
		List<String> result = new ArrayList<>();
		ResourceList<NaturalPerson> contacts = appMan.getResourceAccess().getResource("gatewaySuperiorDataRes/responsibilityContacts");
		if(contacts != null && contacts.isActive()) {
			for(NaturalPerson person : contacts.getAllElements()) {
				if(!person.userRole().isActive()
						|| !person.userRole().getValue().toLowerCase().contains(MAC_CHANGE_RECIPIENT_ROLE))
					continue;
				StringResource email = person.getSubResource("emailAddress", StringResource.class);
				if(email != null && email.isActive() && email.getValue().contains("@"))
					result.add(email.getValue().trim());
			}
		}
		if(result.isEmpty()) {
			appMan.getLogger().warn("No responsibility contact with role '{}' and email found, sending MAC change email to {}",
					MAC_CHANGE_RECIPIENT_ROLE, MAC_CHANGE_FALLBACK_RECIPIENT);
			result.add(MAC_CHANGE_FALLBACK_RECIPIENT);
		}
		return result;
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

	/** One-time diagnostic: log the device path, the IP resource handle state, and the direct subresources of
	 * the device (name/type/active) so we can see under which name/type/path the IP is actually stored when the
	 * expected {@code ipAddress} lookup returns nothing. */
	private static void logDeviceIpDiagnostics(InstallAppDevice object, StringResource ipSource,
			ApplicationManager appMan) {
		if(appMan == null)
			return;
		try {
			Resource dev = object.device().getLocationResource();
			StringBuilder sb = new StringBuilder();
			for(Resource sub : dev.getSubResources(false))
				sb.append(sub.getName()).append('(').append(sub.getResourceType().getSimpleName())
						.append(",active=").append(sub.isActive()).append(") ");
			appMan.getLogger().warn("MAC diag for {}: ipSource path={} active={} value='{}'; device={} "
					+ "subresources: {}", object.getLocation(), ipSource.getPath(), ipSource.isActive(),
					ipSource.isActive() ? ipSource.getValue() : null, dev.getPath(), sb.toString());
		} catch(Exception e) {
			appMan.getLogger().warn("MAC diag failed for " + object.getLocation(), e);
		}
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
