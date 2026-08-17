package com.socaciu.michael.mycomunicator;
/**
 * Broadcast receiver:
 * intercepts SMS messages and sets Application statics:
 * - origination address - a phone number 
 * - the SMS text
 * AND
 * starts the singleTask activity "SMSActivity" with intent that
 * calls SMSActivity.onNewIntent.
 * 
 * This is an experiment to use the TTS and App as singleton service
 */

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.util.Log;
import com.socaciu.michael.mycomunicator.SMSReaderService;

//import com.michael.socaciu.SMSTextReader.ReadTextApp;

public class SMSNotify2 extends BroadcastReceiver {

    private static final String LOG_TAG = "SMSReceiver";
    public static final int NOTIFICATION_ID_RECEIVED = 0x1221;
    static final String ACTION = "android.provider.Telephony.SMS_RECEIVED";
    static final String CHANNEL_ID = "TTS_ID";
    static final String ONGOING_NOTIFICATION_ID = "TTS_NOT_ID";
    private CharSequence tickerMessage = null;
    //private Bundle notifyBundle;
    //Intent notifyIntent;
    SMSReaderService mService;
    boolean mBound = false;

    //@Override
    //protected void onStart() {
    //    Intent SMSintent;
    //    SMSintent = new Intent(this, SMSReaderService.class);
    //}

    @Override
	public void onReceive(Context context, Intent intent) {
    	Log.d("TTSRead", "ENTER onReceive");
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (intent.getAction().equals(SMSNotify2.ACTION)) {
            Log.d("TTSRead", "Broadcast Notification received");
            //StringBuilder sb = new StringBuilder();

            // MVS_HERE: should actually pass ddirectly the intent to start the service?
            Bundle txtBundle = intent.getExtras();

            if(txtBundle != null) {
                SmsMessage smsMessage;
                Log.d("TTSRead", "Not NULL extras");
                if (Build.VERSION.SDK_INT >= 19) { //KITKAT
                    SmsMessage[] msgs = Telephony.Sms.Intents.getMessagesFromIntent(intent);
                    // MVS_TO_DO ??? are there more than one message" if yes process them all
                    // part of message queueing design
                    smsMessage = msgs[0];
                    Log.d("TTSRead", "LG19BR Address: " + smsMessage.getDisplayOriginatingAddress());
                    Log.d("TTSRead", "LG19BR Message: " + smsMessage.getDisplayMessageBody());
                } else {
                    Object[] pdus = (Object[]) txtBundle.get("pdus");
                    for (Object pdu : pdus) {
                        smsMessage = SmsMessage.createFromPdu((byte[]) pdu);
                        //ReadTextApp.setOriginatingAddress(smsMessage.getDisplayOriginatingAddress());
                        //ReadTextApp.setTextMessage(smsMessage.getDisplayMessageBody());
                        Log.d("TTSRead", "Address: " + smsMessage.getDisplayOriginatingAddress());
                        Log.d("TTSRead", "Message: " + smsMessage.getDisplayMessageBody());
                    }
                }
                Log.d("TTSRead", "WTF_01");
            }
            else
            {
                Log.d("TTSRead", "NULL Text Bundle");
            }

            Intent svcIntent = new Intent(context, SMSReaderService.class);
            svcIntent.putExtras(txtBundle);
            // TRY THIS January 27 2019:
            // Start AND BIND the service
            // use onBind to communicate SMSs to the started service.

            //MVS
            //Intent notificationIntent = new Intent(context, SMSReaderService.class);
            //notificationIntent.putExtras(txtBundle);
            //PendingIntent pendingIntent =
            //        PendingIntent.getActivity(context, 0, notificationIntent, 0);

            //Notification notification =
            //        new Notification.Builder(context, "3")
            //                .setContentTitle("TTS")
            //                .setContentText("TTS")
            //                .setSmallIcon(R.drawable.chat)
            //                .setContentIntent(pendingIntent)
            //                .setTicker("TTStick")
            //                .build();

            //startForeground(ONGOING_NOTIFICATION_ID, notification)
            //SMSReaderService.startForeground(7,)

            //MVS: Start REGULAR SERVICE
            //context.startService(svcIntent);

            //MVS_HERE this should be processed in service
            //if (bundle != null) {
            //    SmsMessage smsMessage;
            //    if (Build.VERSION.SDK_INT >= 19) { //KITKAT
            //        SmsMessage[] msgs = Telephony.Sms.Intents.getMessagesFromIntent(intent);
            //        // MVS_TO_DO ??? are there more than one message" if yes process them all
            //        // part of message queueing design
            //        smsMessage = msgs[0];
            //    } else {
            //        Object[] pdus = (Object[]) bundle.get("pdus");
            //        for (Object pdu : pdus) {
            //            smsMessage = SmsMessage.createFromPdu((byte[]) pdu);
            //            //ReadTextApp.setOriginatingAddress(smsMessage.getDisplayOriginatingAddress());
            ////ReadTextApp.setTextMessage(smsMessage.getDisplayMessageBody());
            //            Log.d("TTSRead", "Address: " + smsMessage.getDisplayOriginatingAddress());
            //            Log.d("TTSRead", "Message: " + smsMessage.getDisplayMessageBody());
            //        }
                }

                //Log.d("TTSRead", "Address: " + smsMessage.getDisplayOriginatingAddress());
                //Log.d("TTSRead", "Message: " + smsMessage.getDisplayMessageBody());
                //MVS_HERE
                //notifyBundle.putString("address", smsMessage.getDisplayOriginatingAddress() );
                // compose an intent with extension to carry the SMS
                // pass the SMS to the Service
                // call startService

            abortBroadcast();
               }




 
            //Intent i = new Intent(context, SMSActivity.class);
            //i.setFlags(FLAG_ACTIVITY_NEW_TASK);
            //i.setFlags(0x10000000);
            //context.startActivity(i);
            
            /*
            CharSequence appName = "SMSNotifyExample";
            this.tickerMessage = sb.toString();
            Long theWhen = System.currentTimeMillis();

            PendingIntent.getBroadcast((Context) appName, 0, i, 0);
            Notification notif = new Notification(R.drawable.incoming, this.tickerMessage, theWhen);

            notif.vibrate = new long[] { 100, 250, 100, 500};
            nm.notify(R.string.alert_message, notif);
            */
        }
