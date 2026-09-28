package com.socaciu.michael.mycomunicator;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.util.Log;

public class SMSNotify2 extends BroadcastReceiver {

    private static final String TAG = "TTSRead";
    public static final int NOTIFICATION_ID_RECEIVED = 0x1221;
    static final String ACTION = Telephony.Sms.Intents.SMS_RECEIVED_ACTION;

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "SMSNotify2.onReceive action=" + intent.getAction());

        if (!ACTION.equals(intent.getAction())) {
            return;
        }

        // 1) Log every received message.
        SmsMessage[] msgs = Telephony.Sms.Intents.getMessagesFromIntent(intent);
        if (msgs != null && msgs.length > 0) {
            for (SmsMessage sms : msgs) {
                if (sms == null) continue;
                Log.d(TAG, "SMS received - from: " + sms.getDisplayOriginatingAddress()
                        + " | body: " + sms.getDisplayMessageBody());
            }
        } else {
            Log.d(TAG, "SMSNotify2: no SMS messages found in intent");
        }

        // 2) Hand the message to the foreground service so it can be spoken.
        Intent svcIntent = new Intent(context, SMSReaderService.class);
        Bundle extras = intent.getExtras();
        if (extras != null) {
            svcIntent.putExtras(extras);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(svcIntent);
        } else {
            context.startService(svcIntent);
        }
        Log.d(TAG, "SMSNotify2: forwarded SMS to SMSReaderService");

        // NOTE: do NOT call abortBroadcast() - SMS_RECEIVED is an informational
        // broadcast; aborting it needlessly hides the message from other apps
        // and throws if the broadcast is not ordered.
    }
}
