package utils;

import android.content.Context;
import android.content.Intent;
import android.os.BatteryManager;

public class EmergencyMessageBuilder {

    public static String build(Context context, DataManager dataManager) {
        String message = dataManager.getSOSMessage();

        String emergencyLocation = dataManager.getString("emergency_location", "");
        String lastLocation = dataManager.getString("last_location", "");

        String location;
        if (!emergencyLocation.isEmpty()) {
            location = emergencyLocation;
        } else if (!lastLocation.isEmpty()) {
            location = lastLocation;
        } else {
            location = "📍 Location: Unable to get location";
        }

        message = message.replace("{LOCATION}", location);

        if (!message.contains("Time:")) {
            String timestamp = android.text.format.DateFormat.format(
                    "dd-MM-yyyy HH:mm:ss",
                    System.currentTimeMillis()
            ).toString();
            message += "\n\n⏰ Time: " + timestamp;
        }

        // Battery
        try {
            Intent batteryIntent = context.registerReceiver(
                    null,
                    new android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            );
            if (batteryIntent != null) {
                int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                if (level >= 0 && scale > 0) {
                    int pct = (int) ((level * 100f) / scale);
                    message += "\n🔋 Battery: " + pct + "%";
                }
            }
        } catch (Exception ignored) {}

        return message;
    }
}
