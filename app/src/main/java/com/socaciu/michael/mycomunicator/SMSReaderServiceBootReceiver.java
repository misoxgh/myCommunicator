package com.socaciu.michael.mycomunicator;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by Michael on 7/5/2017.
 */
public class SMSReaderServiceBootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            context.startService(new Intent(context,
                    SMSReaderService.class));
        }
    }
}
