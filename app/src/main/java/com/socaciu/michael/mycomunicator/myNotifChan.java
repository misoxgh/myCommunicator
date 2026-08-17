package com.socaciu.michael.mycomunicator;

import android.app.Application;
import android.app.NotificationManager;
import android.os.Build;

/**
 * Created by Michael on 10/2/2022.
 */
public class myNotifChan extends Application {
   public static final String CHANNEL_ID = "ServiceChannel";

    @Override
    public void onCreate(){
        super.onCreate();

        createNotificationChannel();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.o){
            NotificationChannel serviceChannel = new NotificationChannel(
              CHANNEL_ID,
              name: "ServiceChannel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
        }
    }

}
