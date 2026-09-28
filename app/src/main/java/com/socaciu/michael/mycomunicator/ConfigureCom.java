package com.socaciu.michael.mycomunicator;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.IBinder;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class  ConfigureCom extends AppCompatActivity {
    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private Button startButton;
    protected View.OnClickListener startOnClickListener;
    protected View.OnClickListener stopOnClickListener;
    SMSReaderService mService = null;
    boolean mBound = false;

    private static final boolean AUTO_HIDE = true;
    private static final String CHANNEL_ID = "TTS_ChNotId";
    private static final String ONGOING_NOTIFICATION_ID = "TTS_notId";
    public static final String CHANNEL_NOTIFICATION_1 = "channel_not_1";
    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

    private static final int REQ_PERMISSIONS = 4711;
    /** Optional READ_CONTACTS grant, requested separately so it never blocks startup. */
    private static final int REQ_CONTACTS = 4712;

    // Tracks whether the user has actually pressed START, persisted across
    // activity/process restarts. Without this, the app would come up looking
    // stopped (white START button) while a receiver-enabled flag left over
    // from a previous run silently kept capturing and speaking SMS.
    private static final String PREFS_NAME = "com.socaciu.michael.mycomunicator.prefs";
    private static final String PREF_KEY_RUNNING = "running";

    /**
     * Some older devices needs a small delay between UI widget updates
     * and a change of the status and navigation bar.
     */
    private static final int UI_ANIMATION_DELAY = 300;
    private final Handler mHideHandler = new Handler();
    private View mContentView;
    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            // Delayed removal of status and navigation bar

            // Note that some of these constants are new as of API 16 (Jelly Bean)
            // and API 19 (KitKat). It is safe to use them, as they are inlined
            // at compile-time and do nothing on earlier devices.
            mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    };
    private View mControlsView;
    private final Runnable mShowPart2Runnable = new Runnable() {
        @Override
        public void run() {
            // Delayed display of UI elements
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.show();
            }
            mControlsView.setVisibility(View.VISIBLE);
        }
    };
    private boolean mVisible;
    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };
    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    private final View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (AUTO_HIDE) {
                delayedHide(AUTO_HIDE_DELAY_MILLIS);
            }
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("TTSRead", "ConfigureCom: onCreate");
        setContentView(R.layout.activity_configure_com);

        createNotifcationChannels();

        this.startButton = (Button) findViewById(R.id.start_button);
        startOnClickListener = (new View.OnClickListener() {
            public void onClick(final View v) {
                Log.d("TTSRead", " startOnClickListener");
                doStart();
            }
        });

        stopOnClickListener = (new View.OnClickListener() {
            public void onClick(final View v) {
                Log.d("TTSRead", " stopOnClickListener");
                doStop();
            }
        });

        // Reflect the real state, not an assumed "just installed, nothing
        // running" one: if the user had previously pressed START and never
        // pressed STOP, the service is still meant to be running (it's an
        // always-available foreground service), so keep showing STOP/red.
        // Otherwise show START/green and make sure nothing is left capturing
        // or speaking SMS - guards against a receiver-enabled flag left over
        // from an earlier run surviving while our own "running" flag did not.
        boolean running = isMarkedRunning();
        applyButtonUi(running);
        if (!running) {
            setSmsReceiverEnabled(false);
        }

        mVisible = true;
        mControlsView = findViewById(R.id.fullscreen_content_controls);
        mContentView = findViewById(R.id.fullscreen_content);


        // Set up the user interaction to manually show or hide the system UI.
        mContentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggle();
            }
        });

        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.
        findViewById(R.id.dummy_button).setOnTouchListener(mDelayHideTouchListener);

    }

    @Override
    protected void onStart() {
        super.onStart();
        // The SMSReaderService is started/stopped explicitly from the
        // START / STOP button (doStart / doStop), not from the activity
        // lifecycle, so that it remains an always-available foreground service.
        Log.d("TTSRead", "onStart");

        // Ask for READ_CONTACTS here too: users who granted RECEIVE_SMS before
        // this feature existed pass the doStart() gate without ever being
        // prompted for contacts, which left every caller spoken as "unknown".
        ensureContactsPermission();
    }

    private void createNotifcationChannels(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            Log.d("TTSRead", "created the NotifcationChannels NULL for NOW ?");
            ;
        }

    }

    private void doStart() {
        Log.d("TTSRead", " doStart");

        // RECEIVE_SMS is a dangerous permission: without a runtime grant the
        // SMSNotify2 broadcast receiver is never invoked, so nothing is ever
        // logged or spoken. Ask for it (and POST_NOTIFICATIONS on API 33+)
        // before enabling anything; doStart() re-runs once the user grants.
        if (!hasRequiredPermissions()) {
            requestRequiredPermissions();
            return;
        }

        // Optional: lets the service announce the caller's name instead of "unknown".
        ensureContactsPermission();

        applyButtonUi(true);
        setMarkedRunning(true);

        // 1) Enable the SMS broadcast receiver (declared disabled in the manifest).
        setSmsReceiverEnabled(true);

        // 2) Start the always-available foreground service, and also bind so the
        //    activity can call into it. Because it is *started* (not only
        //    bound) it keeps running after the activity unbinds.
        Intent intent = new Intent(this, SMSReaderService.class);
        ContextCompat.startForegroundService(this, intent);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);

        Log.d("TTSRead", "-STARTED + BOUND SMSReaderService, receiver ENABLED-");
    }

    private void doStop() {
        Log.d("TTSRead", " doStop");
        applyButtonUi(false);
        setMarkedRunning(false);

        setSmsReceiverEnabled(false);

        if (mBound) {
            unbindService(connection);
            mBound = false;
            mService = null;
        }
        stopService(new Intent(this, SMSReaderService.class));

        Log.d("TTSRead", "-STOPPED SMSReaderService, receiver DISABLED-");
    }

    /**
     * Sets the button's color, label and click target for the given state:
     * green "START" when stopped, red "STOP" when running.
     */
    private void applyButtonUi(boolean running) {
        if (running) {
            startButton.setBackgroundColor(0xffff0000); // RED
            startButton.setText(" STOP");
            startButton.setOnClickListener(stopOnClickListener);
        } else {
            startButton.setBackgroundColor(0xff00ff00); // GREEN
            startButton.setText(" START");
            startButton.setOnClickListener(startOnClickListener);
        }
    }

    private boolean isMarkedRunning() {
        return getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .getBoolean(PREF_KEY_RUNNING, false);
    }

    private void setMarkedRunning(boolean running) {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .edit()
                .putBoolean(PREF_KEY_RUNNING, running)
                .apply();
    }

    /** Enables/disables the manifest-declared {@link SMSNotify2} receiver at runtime. */
    private void setSmsReceiverEnabled(boolean enabled) {
        ComponentName receiver = new ComponentName(this, SMSNotify2.class);
        int state = enabled
                ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                : PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
        getPackageManager().setComponentEnabledSetting(
                receiver, state, PackageManager.DONT_KILL_APP);
        Log.d("TTSRead", "SMSNotify2 receiver enabled=" + enabled);
    }

    /**
     * Permissions the service cannot run without. Startup is gated on these.
     * READ_CONTACTS is intentionally NOT here - it is optional and requested
     * separately via {@link #ensureContactsPermission()}.
     */
    private String[] requiredPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return new String[]{Manifest.permission.RECEIVE_SMS,
                    Manifest.permission.POST_NOTIFICATIONS};
        }
        return new String[]{Manifest.permission.RECEIVE_SMS};
    }

    /**
     * Requests READ_CONTACTS if not already granted. Used only to announce the
     * caller's name; if the user denies it, callers are spoken as "unknown".
     */
    private void ensureContactsPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {
            Log.d("TTSRead", "requesting optional READ_CONTACTS permission");
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_CONTACTS}, REQ_CONTACTS);
        }
    }

    private boolean hasRequiredPermissions() {
        for (String p : requiredPermissions()) {
            if (ContextCompat.checkSelfPermission(this, p)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private void requestRequiredPermissions() {
        ActivityCompat.requestPermissions(this, requiredPermissions(), REQ_PERMISSIONS);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_PERMISSIONS && hasRequiredPermissions()) {
            Log.d("TTSRead", "permissions granted, continuing doStart()");
            doStart();
        } else if (requestCode == REQ_PERMISSIONS) {
            Toast.makeText(this, "RECEIVE_SMS permission is required to read messages aloud",
                    Toast.LENGTH_LONG).show();
        } else if (requestCode == REQ_CONTACTS) {
            boolean granted = grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED;
            Log.d("TTSRead", "READ_CONTACTS granted=" + granted);
            if (!granted) {
                Toast.makeText(this, "Without Contacts access, callers are announced as \"unknown\"",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder binderFromBoundService) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            SMSReaderService.LocalBinder binder = (SMSReaderService.LocalBinder)  binderFromBoundService;
            mService = binder.getService();
            mBound = true;
            Log.d("TTSRead"," onServiceConnected");
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100);
    }

    private void toggle() {
        if (mVisible) {
            hide();
        } else {
            show();
        }
    }

    private void hide() {
        // Hide UI first
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        mControlsView.setVisibility(View.GONE);
        mVisible = false;

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.removeCallbacks(mShowPart2Runnable);
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
    }

    @SuppressLint("InlinedApi")
    private void show() {
        // Show the system bar
        mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        mVisible = true;

        // Schedule a runnable to display UI elements after a delay
        mHideHandler.removeCallbacks(mHidePart2Runnable);
        mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY);
    }

    /**
     * Schedules a call to hide() in [delay] milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }
}
