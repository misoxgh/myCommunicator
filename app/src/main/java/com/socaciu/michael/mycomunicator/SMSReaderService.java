package com.socaciu.michael.mycomunicator;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.provider.ContactsContract;
import android.provider.Telephony;
import android.speech.tts.TextToSpeech;
import android.telephony.SmsMessage;
import android.text.TextUtils;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Locale;

public class SMSReaderService extends Service implements TextToSpeech.OnInitListener {

    private static final String TAG = "TTSRead";
    private static final int FOREGROUND_ID = 1;

    private TextToSpeech mTTS;
    private boolean mTtsReady = false;
    /** Text received before the TTS engine finished initializing. */
    private final ArrayList<String> mPending = new ArrayList<>();

    private NotificationManager nm;

    private final IBinder binder = new LocalBinder();

    public class LocalBinder extends Binder {
        SMSReaderService getService() {
            return SMSReaderService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "SMSReaderService onCreate");
        nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // TextToSpeech initializes ASYNCHRONOUSLY. speak() will silently fail
        // until onInit() reports SUCCESS, so anything that arrives before then
        // is buffered in mPending and flushed from onInit().
        mTTS = new TextToSpeech(this, this);
        Log.d(TAG, "SMSReaderService mTTS = new TextToSpeech (waiting for onInit)");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "SMSReaderService onStartCommand: " + startId);

        // A service started with startForegroundService() MUST call
        // startForeground() promptly. Do it on every start so the service
        // stays a foreground, "always available" service.
        startForeground(FOREGROUND_ID, buildNotification());

        if (intent != null) {
            speakMessagesFromIntent(intent);
        }

        // START_STICKY + no stopSelf(): the service keeps running so the TTS
        // engine stays initialized and ready for the next SMS.
        return START_STICKY;
    }

    private Notification buildNotification() {
        Intent notificationIntent = new Intent(this, ConfigureCom.class);
        int piFlags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            piFlags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, piFlags);

        return new NotificationCompat.Builder(this, myNotifChan.CHANNEL_ID)
                .setContentTitle("SMS Reader Service")
                .setContentText("Listening for SMS...")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();
    }

    private void speakMessagesFromIntent(Intent intent) {
        SmsMessage[] msgs = Telephony.Sms.Intents.getMessagesFromIntent(intent);
        if (msgs == null || msgs.length == 0) {
            Log.d(TAG, "onStartCommand: no SMS messages in intent");
            return;
        }

        // A single SMS can arrive as several PDUs (multipart). Re-assemble the
        // body per originating address, preserving arrival order.
        Map<String, StringBuilder> bySender = new LinkedHashMap<>();
        for (SmsMessage sms : msgs) {
            if (sms == null) continue;
            String from = sms.getDisplayOriginatingAddress();
            String part = sms.getMessageBody();
            if (part == null) part = sms.getDisplayMessageBody();
            if (from == null) from = "";
            StringBuilder sb = bySender.get(from);
            if (sb == null) {
                sb = new StringBuilder();
                bySender.put(from, sb);
            }
            if (part != null) sb.append(part);
        }

        for (Map.Entry<String, StringBuilder> e : bySender.entrySet()) {
            String body = e.getValue().toString();
            String caller = lookupCallerName(e.getKey());
            Log.d(TAG, "Received SMS from " + e.getKey() + " (" + caller + "): " + body);
            // Speak the caller's name first, then the message body.
            speakOrQueue(caller);
            speakOrQueue(body);
        }
    }

    /**
     * Resolves an originating phone number to a contact display name, falling
     * back to "unknown" when there is no address, no matching contact, or the
     * READ_CONTACTS permission has not been granted.
     */
    private String lookupCallerName(String address) {
        if (TextUtils.isEmpty(address)) {
            return "unknown";
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "READ_CONTACTS not granted, cannot resolve caller name");
            return "unknown";
        }

        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(address));
        Cursor cursor = null;
        try {
            cursor = getContentResolver().query(uri,
                    new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME},
                    null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                String name = cursor.getString(0);
                if (!TextUtils.isEmpty(name)) {
                    return name;
                }
            }
            Log.d(TAG, "No contact matched " + address + ", speaking \"unknown\"");
        } catch (Exception ex) {
            Log.e(TAG, "Contact lookup failed for " + address, ex);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return "unknown";
    }

    private void speakOrQueue(String text) {
        if (text == null || text.trim().isEmpty()) {
            return;
        }
        synchronized (mPending) {
            if (mTtsReady && mTTS != null) {
                int r = mTTS.speak(text, TextToSpeech.QUEUE_ADD, null, "sms");
                Log.d(TAG, "mTTS.speak returned " + r + " for: " + text);
            } else {
                Log.d(TAG, "TTS not ready yet, queueing: " + text);
                mPending.add(text);
            }
        }
    }

    @Override
    public void onInit(int status) {
        if (status != TextToSpeech.SUCCESS) {
            Log.e(TAG, "TextToSpeech init FAILED, status=" + status);
            return;
        }
        int langResult = mTTS.setLanguage(Locale.US);
        if (langResult == TextToSpeech.LANG_MISSING_DATA
                || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.e(TAG, "TextToSpeech: US English not available (" + langResult + ")");
            return;
        }

        synchronized (mPending) {
            mTtsReady = true;
            Log.d(TAG, "TextToSpeech ready, flushing " + mPending.size() + " queued message(s)");
            for (String t : mPending) {
                mTTS.speak(t, TextToSpeech.QUEUE_ADD, null, "sms");
            }
            mPending.clear();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mTTS != null) {
            mTTS.stop();
            mTTS.shutdown();
            mTTS = null;
        }
        mTtsReady = false;
        Log.d(TAG, "SMSReaderService onDestroy");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
}
