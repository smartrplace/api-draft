package org.smartrplace.apps.hw.install;

import org.smartrplace.apps.hw.install.config.HardwareInstallConfig;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class LocalDeviceId {
    public static Map<String, String> types = new HashMap<>();
    {
        types.put("DoorWindowSensor", "WS");
    }
    public static String getTypeShortId(String type) {
        String id = types.get(type);
        if (id != null) {
            System.out.println("No shorthand found for type " + type);
            return id;
        }
        return type;
    }

    private static final Pattern ID_PATTERN = Pattern.compile("\\w+-(\\d+)");

    public static String generateDeviceId(InstallAppDevice dev, HardwareInstallConfig cfg) {
        String typeId = getTypeShortId(dev.device().getClass().getName());
        int maxSerial = 1;
        for(InstallAppDevice d : cfg.knownDevices().getAllElements()) {
            if (d.device().getClass() == dev.device().getClass()) {
                String id = d.deviceId().getValue();
                Matcher m = ID_PATTERN.matcher(id);
                if (!m.find()) continue;
                int serial = Integer.parseInt(m.group(1));
                if (maxSerial < serial)
                    maxSerial = serial;
            }
        }
        return String.format("%s-%04d", typeId, maxSerial + 1);
    }
}
