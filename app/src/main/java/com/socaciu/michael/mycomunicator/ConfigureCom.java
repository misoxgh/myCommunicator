package com.socaciu.michael.mycomunicator;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.IBinder;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
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
    private Button test1Button;
    private Button test2Button;
    protected View.OnClickListener startOnClickListener;
    protected View.OnClickListener stopOnClickListener;
    protected View.OnClickListener test1OnClickListener;
    protected View.OnClickListener test2OnClickListener;
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
        startButton.setOnClickListener(startOnClickListener);

        this.test1Button = (Button) findViewById(R.id.test1_button);
        test1OnClickListener = (new View.OnClickListener() {
            public void onClick(final View v) {
                Log.d("TTSRead", "test1OnClickListener TEST1");
                if (mBound) {
                    // Call a method from the LocalService.
                    // However, if this call were something that might hang, then this request should
                    // occur in a separate thread to avoid slowing down the activity performance.
                    int num = mService.getRandomNumber();
                    Log.d("TTSRead", "Got number from service: " + num);
                    //Toast.makeText(this, "number: " + num, Toast.LENGTH_SHORT).show();
                }
            }
        });
        test1Button.setOnClickListener(test1OnClickListener);

        this.test2Button = (Button) findViewById(R.id.test2_button);
        test2OnClickListener = (new View.OnClickListener() {
            public void onClick(final View v) {
                Log.d("TTSRead", "test2OnClickListener TEST2");
            }
        });
        test2Button.setOnClickListener(test2OnClickListener);

        stopOnClickListener = (new View.OnClickListener() {
            public void onClick(final View v) {
                Log.d("TTSRead", " stopOnClickListener");
                doStop();
            }
        });


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

        Context context = getApplicationContext();
        Intent intent = new Intent(this, SMSReaderService.class);
        context.startService(intent);

        Log.d("TTSRead", "-STARTED-");
    }

    private void createNotifcationChannels(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            ;
        }

    }

    private void doStart() {
        Log.d("TTSRead", " doStart");
        startButton.setBackgroundColor(0xffff0000); //RED
        startButton.setText(" STOP");
        startButton.setOnClickListener(stopOnClickListener);
        //MVS code for regular service
        Intent intent = new Intent(this, SMSReaderService.class);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);

        if (mService != null) {
            Log.d("TTSRead", "-BOUND-");
        }
        // FOREGROUND, start here ?
        //Intent notificationIntent = new Intent(this, SMSReaderService.class);
        //notificationIntent.putExtras(bundle);
        //PendingIntent pendingIntent =
        //        PendingIntent.getActivity(this, 0, notificationIntent, 0);

        //Notification notification =
        //        new Notification.Builder(this, CHANNEL_ID)
        //                .setContentTitle("TTS")
        //                .setContentText("TTS")
        //                .setSmallIcon(R.drawable.chat)
        //                .setContentIntent(pendingIntent)
         //               .setTicker("TTStick")
        //                .build();

        //startForeground(ONGOING_NOTIFICATION_ID, notification);
        //MVS regular start
        //startService(ONGOING_NOTIFICATION_ID,notification);
    }

    private void doStop() {
        Log.d("TTSRead"," doStop");
        startButton.setBackgroundColor(0xff00ff00);  //GREEN
        startButton.setText(" START");
        startButton.setOnClickListener(startOnClickListener);
        //Intent intent = new Intent(this, SMSReaderService.class);
        //startService(intent);
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
