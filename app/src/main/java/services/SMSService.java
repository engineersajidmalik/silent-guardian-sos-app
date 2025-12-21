package services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.telephony.SmsManager;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import models.Contact;
import utils.DataManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class SMSService extends Service {

    private static final String CHANNEL_ID = "SMSServiceChannel";
    private static final int NOTIFICATION_ID = 1002;

    private DataManager dataManager;
    private SmsManager smsManager;

    @Override
    public void onCreate() {
        super.onCreate();
        dataManager = new DataManager(this);
        smsManager = SmsManager.getDefault();
        createNotificationChannel();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(NOTIFICATION_ID, createNotification());

        if (intent != null) {
            String action = intent.getAction();
            if ("SEND_EMERGENCY_SMS".equals(action)) {
                sendEmergencySMS();
            } else if ("SEND_TEST_SMS".equals(action)) {
                sendTestSMS();
            } else if ("SEND_DIRECT_SMS".equals(action)) {
                sendDirectSMS(intent);
            }
        }
        return START_STICKY;
    }

    private void sendEmergencySMS() {
        List<Contact> emergencyContacts = getEmergencyContactsFromDataManager();

        if (emergencyContacts.isEmpty()) {
            Toast.makeText(this, "No emergency contacts found!", Toast.LENGTH_LONG).show();
            stopSelf();
            return;
        }

        String message = buildEmergencyMessage(this);

        int sentCount = 0;
        for (Contact contact : emergencyContacts) {
            if (sendSMS(contact.getPhone(), message)) sentCount++;
        }

        Toast.makeText(this, "Sent " + sentCount + " emergency SMS(s)", Toast.LENGTH_LONG).show();
        saveEmergencyResult(sentCount, emergencyContacts.size());

        new android.os.Handler().postDelayed(this::stopSelf, 3000);
    }


    /**
     * ✅ Shared message builder for SMS/Email/WhatsApp
     */
    public static String buildEmergencyMessage(Context context) {
        DataManager dm = new DataManager(context);

        String message = dm.getSOSMessage();

        String emergencyLocation = dm.getString("emergency_location", "");
        String lastLocation = dm.getString("last_location", "");

        String location;
        if (!emergencyLocation.isEmpty()) location = emergencyLocation;
        else if (!lastLocation.isEmpty()) location = lastLocation;
        else location = "📍 Location: Unable to get location";

        message = message.replace("{LOCATION}", location);

        if (!message.contains("Time:")) {
            String timestamp = android.text.format.DateFormat
                    .format("dd-MM-yyyy HH:mm:ss", System.currentTimeMillis()).toString();
            message += "\n\n⏰ Time: " + timestamp;
        }

        // Battery info
        try {
            Intent batteryStatus = context.registerReceiver(
                    null,
                    new android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            );
            if (batteryStatus != null) {
                int level = batteryStatus.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1);
                int scale = batteryStatus.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1);
                if (level >= 0 && scale > 0) {
                    float batteryPct = (level * 100f) / (float) scale;
                    message += "\n🔋 Battery: " + (int) batteryPct + "%";
                }
            }
        } catch (Exception ignored) {}

        return message;
    }

    private void sendTestSMS() {
        String testNumber = dataManager.getString("test_phone_number", "");

        if (testNumber.isEmpty()) {
            List<Contact> contacts = getEmergencyContactsFromDataManager();
            if (!contacts.isEmpty()) testNumber = contacts.get(0).getPhone();
            else {
                Toast.makeText(this, "Please set test phone number or add emergency contacts", Toast.LENGTH_LONG).show();
                stopSelf();
                return;
            }
        }

        String testMessage = "✅ Silent Guardian Test Message\n\n" +
                "This is a TEST message.\n\nTime: " +
                android.text.format.DateFormat.format("dd-MM-yyyy HH:mm:ss", System.currentTimeMillis());

        if (sendSMS(testNumber, testMessage)) {
            Toast.makeText(this, "✅ Test SMS sent to " + testNumber, Toast.LENGTH_LONG).show();
            dataManager.saveString("last_test_sent",
                    android.text.format.DateFormat.format("dd-MM-yyyy HH:mm:ss", System.currentTimeMillis()).toString());
        } else {
            Toast.makeText(this, "❌ Failed to send test SMS", Toast.LENGTH_LONG).show();
        }

        new android.os.Handler().postDelayed(this::stopSelf, 3000);
    }

    private void sendDirectSMS(Intent intent) {
        String phoneNumber = intent.getStringExtra("phone_number");
        String message = intent.getStringExtra("message");

        if (phoneNumber == null || phoneNumber.isEmpty() || message == null || message.isEmpty()) {
            Toast.makeText(this, "Invalid SMS parameters", Toast.LENGTH_SHORT).show();
            stopSelf();
            return;
        }

        if (sendSMS(phoneNumber, message)) Toast.makeText(this, "SMS sent to " + phoneNumber, Toast.LENGTH_SHORT).show();
        else Toast.makeText(this, "Failed to send SMS", Toast.LENGTH_SHORT).show();

        new android.os.Handler().postDelayed(this::stopSelf, 2000);
    }

    private boolean sendSMS(String phoneNumber, String message) {
        try {
            phoneNumber = phoneNumber.replaceAll("[^0-9+]", "");
            if (phoneNumber.isEmpty() || phoneNumber.length() < 10) return false;

            ArrayList<String> parts = smsManager.divideMessage(message);
            smsManager.sendMultipartTextMessage(phoneNumber, null, parts, null, null);

            logSentSMS(phoneNumber, message);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            logSMSError("Failed to send to " + phoneNumber + ": " + e.getMessage());
            return false;
        }
    }

    private List<Contact> getEmergencyContactsFromDataManager() {
        List<Contact> contacts = new ArrayList<>();
        Set<String> contactsSet = dataManager.getContactsSet();

        for (String contactData : contactsSet) {
            Contact contact = Contact.fromContactData(contactData);
            if (contact != null) contacts.add(contact);
        }
        return contacts;
    }

    private void logSentSMS(String phoneNumber, String message) {
        String timestamp = android.text.format.DateFormat.format("dd-MM-yyyy HH:mm:ss", System.currentTimeMillis()).toString();
        String shortMessage = message.length() > 100 ? message.substring(0, 100) + "..." : message;
        String newLog = timestamp + " | To: " + phoneNumber + " | " + shortMessage;

        String currentLogs = dataManager.getString("sms_sent_logs", "");
        String allLogs = newLog + "\n" + currentLogs;

        String[] logs = allLogs.split("\n");
        StringBuilder finalLogs = new StringBuilder();
        int count = Math.min(logs.length, 20);

        for (int i = 0; i < count; i++) {
            finalLogs.append(logs[i]);
            if (i < count - 1) finalLogs.append("\n");
        }

        dataManager.saveString("sms_sent_logs", finalLogs.toString());
    }

    private void saveEmergencyResult(int sentCount, int totalContacts) {
        String result = "Emergency sent: " + sentCount + "/" + totalContacts + " contacts at " +
                android.text.format.DateFormat.format("dd-MM-yyyy HH:mm:ss", System.currentTimeMillis()).toString();
        dataManager.saveString("last_emergency_result", result);

        int emergencyCount = dataManager.getInt("emergency_count", 0);
        dataManager.saveInt("emergency_count", emergencyCount + 1);
    }

    private void logSMSError(String error) {
        String timestamp = android.text.format.DateFormat.format("dd-MM-yyyy HH:mm:ss", System.currentTimeMillis()).toString();
        String newLog = timestamp + " | ERROR: " + error;

        String currentLogs = dataManager.getString("sms_error_logs", "");
        String allLogs = newLog + "\n" + currentLogs;

        String[] logs = allLogs.split("\n");
        StringBuilder finalLogs = new StringBuilder();
        int count = Math.min(logs.length, 10);

        for (int i = 0; i < count; i++) {
            finalLogs.append(logs[i]);
            if (i < count - 1) finalLogs.append("\n");
        }

        dataManager.saveString("sms_error_logs", finalLogs.toString());
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "SMS Service Channel",
                    NotificationManager.IMPORTANCE_LOW
            );
            serviceChannel.setDescription("Sends emergency SMS messages");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(serviceChannel);
        }
    }

    private Notification createNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("🚨 Silent Guardian")
                .setContentText("Sending emergency messages...")
                .setSmallIcon(android.R.drawable.ic_dialog_email)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .build();
    }

    @Override
    public void onDestroy() {
        Toast.makeText(this, "SMS service completed", Toast.LENGTH_SHORT).show();
        super.onDestroy();
    }

    public static void startEmergencySMS(Context context) {
        Intent serviceIntent = new Intent(context, SMSService.class);
        serviceIntent.setAction("SEND_EMERGENCY_SMS");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(serviceIntent);
        else context.startService(serviceIntent);
    }

    public static void sendTestMessage(Context context, String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            DataManager dataManager = new DataManager(context);
            phoneNumber = dataManager.getString("test_phone_number", "");

            if (phoneNumber.isEmpty()) {
                Set<String> contactsSet = dataManager.getContactsSet();
                if (!contactsSet.isEmpty()) {
                    String firstContact = contactsSet.iterator().next();
                    String[] parts = firstContact.split("\\|");
                    if (parts.length == 2) phoneNumber = parts[1];
                }
            }
        }

        if (phoneNumber == null || phoneNumber.isEmpty()) {
            Toast.makeText(context, "Please set a test phone number first", Toast.LENGTH_LONG).show();
            return;
        }

        DataManager dataManager = new DataManager(context);
        dataManager.saveString("test_phone_number", phoneNumber);

        Intent serviceIntent = new Intent(context, SMSService.class);
        serviceIntent.setAction("SEND_TEST_SMS");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(serviceIntent);
        else context.startService(serviceIntent);
    }

    public static void sendDirectSMS(Context context, String phoneNumber, String message) {
        Intent serviceIntent = new Intent(context, SMSService.class);
        serviceIntent.setAction("SEND_DIRECT_SMS");
        serviceIntent.putExtra("phone_number", phoneNumber);
        serviceIntent.putExtra("message", message);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(serviceIntent);
        else context.startService(serviceIntent);
    }
}
