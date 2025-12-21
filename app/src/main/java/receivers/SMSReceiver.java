package receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.widget.Toast;
import utils.DataManager;

/**
 * SMSReceiver - Listens for incoming SMS messages
 * Can be used for:
 * 1. Receiving confirmation SMS from emergency contacts
 * 2. Receiving commands via SMS
 * 3. Monitoring for specific keywords
 */
public class SMSReceiver extends BroadcastReceiver {

    private static final String EMERGENCY_KEYWORD = "EMERGENCY";
    private static final String CONFIRMATION_KEYWORD = "OK";
    private static final String STOP_KEYWORD = "STOP";

    @Override
    public void onReceive(Context context, Intent intent) {
        DataManager dataManager = new DataManager(context);

        // Check if SMS monitoring is enabled (using existing getSetting method)
        boolean smsMonitoringEnabled = dataManager.getSetting("sms_monitoring_enabled", false);
        if (!smsMonitoringEnabled) {
            return;
        }

        if (intent.getAction() == null) {
            return;
        }

        if (!intent.getAction().equals("android.provider.Telephony.SMS_RECEIVED") &&
                !intent.getAction().equals("android.provider.Telephony.SMS_DELIVER")) {
            return;
        }

        Bundle bundle = intent.getExtras();
        if (bundle == null) {
            return;
        }

        try {
            Object[] pdus = (Object[]) bundle.get("pdus");
            if (pdus == null) {
                return;
            }

            SmsMessage[] messages = new SmsMessage[pdus.length];
            StringBuilder sb = new StringBuilder();

            for (int i = 0; i < pdus.length; i++) {
                // For Android M (API 23) and above
                String format = bundle.getString("format");
                messages[i] = SmsMessage.createFromPdu((byte[]) pdus[i], format);

                // Get message content
                String messageBody = messages[i].getMessageBody();
                String senderPhone = messages[i].getOriginatingAddress();

                // Clean phone number
                senderPhone = senderPhone.replaceAll("[^0-9+]", "");

                sb.append("From: ").append(senderPhone).append("\n");
                sb.append("Message: ").append(messageBody).append("\n\n");

                // Process the SMS
                processSMS(context, senderPhone, messageBody, dataManager);
            }

            // Log all received messages
            logSMSMessages(context, sb.toString(), dataManager);

        } catch (Exception e) {
            e.printStackTrace();
            logError(context, "SMS processing error: " + e.getMessage(), dataManager);
        }
    }

    /**
     * Process incoming SMS
     */
    private void processSMS(Context context, String senderPhone, String messageBody, DataManager dataManager) {
        String upperBody = messageBody.toUpperCase().trim();

        // Check if this is from an emergency contact
        boolean isFromEmergencyContact = isFromEmergencyContact(senderPhone, dataManager);

        // Check for emergency keyword
        if (upperBody.contains(EMERGENCY_KEYWORD)) {
            handleEmergencySMS(context, senderPhone, messageBody, isFromEmergencyContact, dataManager);
        }

        // Check for confirmation keyword
        else if (upperBody.contains(CONFIRMATION_KEYWORD) && isFromEmergencyContact) {
            handleConfirmationSMS(context, senderPhone, messageBody, dataManager);
        }

        // Check for stop keyword
        else if (upperBody.contains(STOP_KEYWORD) && isFromEmergencyContact) {
            handleStopSMS(context, senderPhone, messageBody, dataManager);
        }

        // Check if message contains location request
        else if (upperBody.contains("LOCATION") || upperBody.contains("WHERE") || upperBody.contains("LOCATE")) {
            handleLocationRequestSMS(context, senderPhone, messageBody, isFromEmergencyContact, dataManager);
        }

        // Log all SMS from emergency contacts
        if (isFromEmergencyContact) {
            logEmergencyContactSMS(context, senderPhone, messageBody, dataManager);
        }
    }

    /**
     * Check if sender is an emergency contact
     */
    private boolean isFromEmergencyContact(String phoneNumber, DataManager dataManager) {
        // Get all emergency contacts
        String[] contactPhones = dataManager.getContactPhones();

        // Clean the incoming phone number for comparison
        phoneNumber = phoneNumber.replaceAll("[^0-9+]", "");

        for (String contactPhone : contactPhones) {
            if (contactPhone != null) {
                String cleanContactPhone = contactPhone.replaceAll("[^0-9+]", "");
                if (cleanContactPhone.equals(phoneNumber) ||
                        phoneNumber.endsWith(cleanContactPhone) ||
                        cleanContactPhone.endsWith(phoneNumber)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Handle emergency SMS from contacts
     */
    private void handleEmergencySMS(Context context, String senderPhone, String messageBody,
                                    boolean isFromEmergencyContact, DataManager dataManager) {
        if (isFromEmergencyContact) {
            // Emergency from trusted contact - show urgent notification
            Toast.makeText(context, "🚨 Emergency SMS from contact: " + senderPhone,
                    Toast.LENGTH_LONG).show();

            // You could trigger an alert or notification here
            logEmergencyAlert(context, "Emergency SMS received from contact: " + senderPhone, dataManager);
        } else {
            // Emergency from unknown number - log but don't alert
            logSMSAlert(context, "Emergency SMS from unknown: " + senderPhone, dataManager);
        }
    }

    /**
     * Handle confirmation SMS
     */
    private void handleConfirmationSMS(Context context, String senderPhone, String messageBody, DataManager dataManager) {
        Toast.makeText(context, "✅ Confirmation received from: " + senderPhone,
                Toast.LENGTH_SHORT).show();

        // Update emergency status
        dataManager.saveString("last_confirmation_from", senderPhone);
        dataManager.saveString("last_confirmation_time", String.valueOf(System.currentTimeMillis()));

        // Log confirmation
        logConfirmation(context, senderPhone, dataManager);
    }

    /**
     * Handle stop request SMS
     */
    private void handleStopSMS(Context context, String senderPhone, String messageBody, DataManager dataManager) {
        Toast.makeText(context, "⏹️ Stop request from: " + senderPhone,
                Toast.LENGTH_SHORT).show();

        // You could stop ongoing emergency services here
        // Example: services.EmergencyService.stopEmergency(context);

        logStopRequest(context, senderPhone, dataManager);
    }

    /**
     * Handle location request SMS
     */
    private void handleLocationRequestSMS(Context context, String senderPhone, String messageBody,
                                          boolean isFromEmergencyContact, DataManager dataManager) {
        if (isFromEmergencyContact) {
            Toast.makeText(context, "📍 Location requested by: " + senderPhone,
                    Toast.LENGTH_SHORT).show();

            // Get last known location
            String lastLocation = dataManager.getString("last_location", "Location unavailable");

            // You could auto-reply with location here
            // Example: services.SMSService.sendDirectSMS(context, senderPhone, "My location: " + lastLocation);

            logLocationRequest(context, senderPhone, dataManager);
        }
    }

    /**
     * Log SMS messages
     */
    private void logSMSMessages(Context context, String messages, DataManager dataManager) {
        String currentLogs = dataManager.getString("sms_received_logs", "");
        String timestamp = android.text.format.DateFormat.format("yyyy-MM-dd HH:mm:ss", System.currentTimeMillis()).toString();
        String newLog = "[" + timestamp + "]\n" + messages + "---\n";

        // Keep only last 20 SMS logs
        String[] logs = (newLog + currentLogs).split("\n---\n");
        StringBuilder finalLogs = new StringBuilder();
        int count = Math.min(logs.length, 20);

        for (int i = 0; i < count; i++) {
            finalLogs.append(logs[i]);
            if (i < count - 1) {
                finalLogs.append("\n---\n");
            }
        }

        dataManager.saveString("sms_received_logs", finalLogs.toString());
    }

    /**
     * Log emergency contact SMS
     */
    private void logEmergencyContactSMS(Context context, String senderPhone, String messageBody, DataManager dataManager) {
        String currentLogs = dataManager.getString("emergency_contact_sms_logs", "");
        String timestamp = android.text.format.DateFormat.format("HH:mm:ss", System.currentTimeMillis()).toString();
        String newLog = timestamp + " - " + senderPhone + ": " +
                (messageBody.length() > 50 ? messageBody.substring(0, 50) + "..." : messageBody);

        // Keep only last 50 logs
        String[] logs = (newLog + "\n" + currentLogs).split("\n");
        StringBuilder finalLogs = new StringBuilder();
        int count = Math.min(logs.length, 50);

        for (int i = 0; i < count; i++) {
            finalLogs.append(logs[i]);
            if (i < count - 1) {
                finalLogs.append("\n");
            }
        }

        dataManager.saveString("emergency_contact_sms_logs", finalLogs.toString());
    }

    /**
     * Log emergency alert
     */
    private void logEmergencyAlert(Context context, String message, DataManager dataManager) {
        dataManager.saveString("last_emergency_alert", message);
        dataManager.saveString("last_emergency_alert_time", String.valueOf(System.currentTimeMillis()));
    }

    /**
     * Log confirmation
     */
    private void logConfirmation(Context context, String senderPhone, DataManager dataManager) {
        String currentLogs = dataManager.getString("confirmation_logs", "");
        String timestamp = android.text.format.DateFormat.format("yyyy-MM-dd HH:mm:ss", System.currentTimeMillis()).toString();
        String newLog = timestamp + " - Confirmation from: " + senderPhone;

        dataManager.saveString("confirmation_logs", newLog + "\n" + currentLogs);
    }

    /**
     * Log stop request
     */
    private void logStopRequest(Context context, String senderPhone, DataManager dataManager) {
        String currentLogs = dataManager.getString("stop_request_logs", "");
        String timestamp = android.text.format.DateFormat.format("yyyy-MM-dd HH:mm:ss", System.currentTimeMillis()).toString();
        String newLog = timestamp + " - Stop request from: " + senderPhone;

        dataManager.saveString("stop_request_logs", newLog + "\n" + currentLogs);
    }

    /**
     * Log location request
     */
    private void logLocationRequest(Context context, String senderPhone, DataManager dataManager) {
        String currentLogs = dataManager.getString("location_request_logs", "");
        String timestamp = android.text.format.DateFormat.format("yyyy-MM-dd HH:mm:ss", System.currentTimeMillis()).toString();
        String newLog = timestamp + " - Location request from: " + senderPhone;

        dataManager.saveString("location_request_logs", newLog + "\n" + currentLogs);
    }

    /**
     * Log SMS alert
     */
    private void logSMSAlert(Context context, String message, DataManager dataManager) {
        dataManager.saveString("last_sms_alert", message);
    }

    /**
     * Log error
     */
    private void logError(Context context, String error, DataManager dataManager) {
        String currentLogs = dataManager.getString("sms_error_logs", "");
        String timestamp = android.text.format.DateFormat.format("yyyy-MM-dd HH:mm:ss", System.currentTimeMillis()).toString();
        String newLog = timestamp + " - " + error;

        dataManager.saveString("sms_error_logs", newLog + "\n" + currentLogs);
    }

    /**
     * Get SMS logs
     */
    public static String getSMSLogs(Context context) {
        DataManager dataManager = new DataManager(context);
        return dataManager.getString("sms_received_logs", "No SMS received yet");
    }

    /**
     * Clear SMS logs
     */
    public static void clearSMSLogs(Context context) {
        DataManager dataManager = new DataManager(context);
        dataManager.saveString("sms_received_logs", "");
    }

    /**
     * Enable/disable SMS monitoring (FIXED: using existing saveSetting method)
     */
    public static void setSMSMonitoringEnabled(Context context, boolean enabled) {
        DataManager dataManager = new DataManager(context);
        dataManager.saveSetting("sms_monitoring_enabled", enabled);
    }

    /**
     * Check if SMS monitoring is enabled (FIXED: using existing getSetting method)
     */
    public static boolean isSMSMonitoringEnabled(Context context) {
        DataManager dataManager = new DataManager(context);
        return dataManager.getSetting("sms_monitoring_enabled", false);
    }
}