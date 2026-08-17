package com.socaciu.michael.mycomunicator;

import android.app.Notification;
import android.app.Notification.Builder;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.provider.Telephony;
import android.speech.tts.TextToSpeech;
import android.telephony.SmsMessage;
import android.util.Log;
import android.widget.Toast;

import java.util.Locale;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import static android.app.Notification.*;

/**
 * Created by Michael on 7/5/2017.
 */

//https://developer.android.com/guide/components/services
public class SMSReaderService extends Service implements TextToSpeech.OnInitListener{

    private static final String CLASSTAG = SMSReaderService.class.getSimpleName();
    private static final String LOC = "LOC";
    private static final String ZIP = "ZIP";
    private static final long ALERT_QUIET_PERIOD = 10000;
    private static final long ALERT_POLL_INTERVAL = 15000;
    private TextToSpeech mTTS;

    // convenience for Activity classes in the same process to get current device location
    // (so they don't have to repeat all the LocationManager and provider stuff locally)
    // (this would NOT work across applications, only for things in the same PROCESS)
    public static String deviceLocationZIP = "94102";

    private Timer timer;
    private NotificationManager nm;
    private Looper mServiceLooper;
    private ServiceHandler mServiceHandler;

    // Binder given to clients
    private final IBinder binder = new LocalBinder();

    public class LocalBinder extends Binder {
        SMSReaderService getService() {
            return SMSReaderService.this;
        }
    }

    // Random number generator
    private final Random mGenerator = new Random();

    // Handler that receives messages from the thread,
    // provides the service requested in Message msg in the working thread
    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }
        @Override
        public void handleMessage(Message msg) {
            // speak SMS here
            try {
                // MVS_HERE
                // Get the SMS, possibly store it, speak it
                //Think about the SMS list and how to use it.
                // SMS_list ??? internal class ?
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                // Restore interrupt status.
                Thread.currentThread().interrupt();
            }
            // Stop the service using the startId, so that we don't stop
            // the service in the middle of handling another job
            stopSelf(msg.arg1);
        }
    }

    // MVS_NOT_USED
    private TimerTask task = new TimerTask() {

        @Override
        public void run() {
            // poll for something ...if needed
            ;
            // The polling that can result in sending notification from the TIMER TASK,
            // task that runs in the background - may or not be used with SMS - for example
            // to monitor queues and other resources that grow.
            // The OTHER SOURCE of notifications IS THE SMS Broadcat receiver, which should
            // NOTIFY THE SERVICE of a SMS message received. The message should be saved, queued
            // to be spoken by the TTS Engine.
        }
    };

    // Can this be used to process the SMS message received from the SMS broadcast receiver. It
    // It should retrieve and process the SMS message.
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            notifyFromHandler((String) msg.getData().get(SMSReaderService.LOC), (String) msg.getData().get(
                    SMSReaderService.ZIP));
        }
    };

    @Override
    public void onCreate() {
        // Start up the thread running the service.  Note that we create a
        // separate thread because the service normally runs in the process's
        // main thread, which we don't want to block.  We also make it
        // background priority so CPU-intensive work will not disrupt our UI.
        Log.d("TTSRead", "SMSReaderService onCreate");
        HandlerThread thread = new HandlerThread("ServiceStartArguments",
                Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();

        // Get the HandlerThread's Looper and use it for our Handler
        mServiceLooper = thread.getLooper();
        Log.d("TTSRead", "SMSReaderService NEW ServiceHandler");
        mServiceHandler = new ServiceHandler(mServiceLooper);

        // MVS_NOT USED
        // timer = new Timer();
        // timer.schedule(task, 5000, SMSReaderService.ALERT_POLL_INTERVAL);
        nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        /* instantiate the TTS ENGINE here ? */
        //Log.d("TTSRead", "Create NEW TTS mTTS");
        //mTTS = new TextToSpeech(getApplicationContext(), this);

    }

    // This is the old onStart method that will be called on the pre-2.0
    // platform.  On 2.0 or later we override onStartCommand() so this
    // method will not be called.
    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
        Toast.makeText(this, "service starting", Toast.LENGTH_SHORT).show();
        Log.d("TTSRead", "SMSReaderService onStart");
        // For each start request, send a message to start a job and deliver the
        // start ID so we know which request we're stopping when we finish the job
        Message msg = mServiceHandler.obtainMessage();
        msg.arg1 = startId;
        mServiceHandler.sendMessage(msg);
        return;
    }

    // This is the Service entry point. To speak a text string,
    // call the service startService with an intent that carries
    // the text string.
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Toast.makeText(this, "service starting", Toast.LENGTH_SHORT).show();
        Log.d("TTSRead", "SMSReaderService onStartCommand: " + String.valueOf(startId));
        // For each start request, send a message to start a job and deliver the
        // start ID so we know which request we're stopping when we finish the job

        // Foreground
        Intent notificationIntent = new Intent(this, ConfigureCom.class);
        PendingIntent pendingIntent =
                PendingIntent.getActivity(this, 0, notificationIntent, 0);

        //Notification notification =
        //        new Builder(this, CHANNEL_DEFAULT_IMPORTANCE)
        //                .setContentTitle(getText(R.string.notification_title))
        //                .setContentText(getText(R.string.notification_message))
        //                .setSmallIcon(R.drawable.chat)
        //                .setContentIntent(pendingIntent)
        //                .setTicker(getText(R.string.ticker_text))
        //                .build();

        //MVS_HERE_TO_DO_Feb4
        // Get the SMS MessageS from the intent
        Bundle svcBundle;
        svcBundle = intent.getExtras();
        if(svcBundle != null) {
            SmsMessage smsMessage;
            if (Build.VERSION.SDK_INT >= 19) { //KITKAT
                SmsMessage[] msgs = Telephony.Sms.Intents.getMessagesFromIntent(intent);
                // MVS_TO_DO ??? are there more than one message" if yes process them all
                // part of message queueing design
                smsMessage = msgs[0];
                Log.d("TTSRead", "LG19SVC Address: " + smsMessage.getDisplayOriginatingAddress());
                Log.d("TTSRead", "LG19SCV Message: " + smsMessage.getDisplayMessageBody());
            } else {
                Object[] pdus = (Object[]) svcBundle.get("pdus");
                for (Object pdu : pdus) {
                    smsMessage = SmsMessage.createFromPdu((byte[]) pdu);
                    //ReadTextApp.setOriginatingAddress(smsMessage.getDisplayOriginatingAddress());
                    //ReadTextApp.setTextMessage(smsMessage.getDisplayMessageBody());
                    Log.d("TTSRead", "Address: " + smsMessage.getDisplayOriginatingAddress());
                    Log.d("TTSRead", "Message: " + smsMessage.getDisplayMessageBody());
                }
            }
        }
        else {
            Log.d("TTSRead", "NULL svcBundle");
        }

        //return START_STICKY;

        // Bundle.get("pdus), it is possible that each pdu is a different SMS !!!!????
        // For each pdu one creates a SMS Message: Originating Address / MessageBody
        //???? Should each SMS be sent in a ServiceHandler Message or SMSs be sent packed ????
        //???? should the SMSs be queued here ?
        // Analize Service Handler and see if it can handle multiple messages,
        // Otherwise have a queue either here or in the Service handler.
        //
        //
        //
        // OLD text:
        // and put it the handler message
        // Think to save the SMS in a list and how to use the list
        // the SMS passed in the handler message is to be spoken in the
        // handler thread.

        //Message msg = mServiceHandler.obtainMessage();
        //msg.arg1 = startId;
        //mServiceHandler.sendMessage(msg);
        return START_STICKY;
        // configure and start the TTS ENGINE here ????????
    }

    @Override
    public void onInit(int status) {
        Log.d("TTSRead", "TTSService onInit: " + String.valueOf(status));
        if (status == TextToSpeech.SUCCESS) {
            int result = mTTS.setLanguage(Locale.US);
            if (result == TextToSpeech.LANG_MISSING_DATA ||
                    result == TextToSpeech.LANG_NOT_SUPPORTED) {
                // Language data is missing or the language is not supported.
                Toast.makeText(this, "Language NOT OK", Toast.LENGTH_SHORT).show();
                Log.d("TTSRead", "TTSService init NOT OK");

            } else {
                Toast.makeText(this, "Language OK", Toast.LENGTH_SHORT).show();
                Log.d("TTSRead", "TTSService init OK");
                mTTS.speak("T T S version 3 initialized", TextToSpeech.QUEUE_ADD, null);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mTTS.shutdown();
        Log.d("TTSRead", "Destroy");
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d("TTSRead", "SMSReaderService onBind");
        return binder;
    }

    /** Temporary TEST only method for clients */
    public int getRandomNumber() {
        return mGenerator.nextInt(100);
    }

    // Could this be used to send the TextMessage in the Service Handler ????
    private void sendNotification(String zip, String someString) {
        Message message = Message.obtain();
        Bundle bundle = new Bundle();
        bundle.putString(SMSReaderService.ZIP, zip);
        bundle.putString(SMSReaderService.LOC, someString);
        message.setData(bundle);
        handler.sendMessage(message);
    }

    // This should Notify the User in the UI and enable the User to react: skip, stop the current
    // reading, repeat the reading of the message.
    private void notifyFromHandler(String location, String zip) {

        final Notification n = new Notification(R.drawable.chat, "SMS", System
                .currentTimeMillis());
        nm.notify(Integer.parseInt(zip), n);
    }
}
