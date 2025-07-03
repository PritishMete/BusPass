package com.pritish.smartbuss;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            // Start the LocationUpdateService on device boot
            Intent serviceIntent = new Intent(context, LocationUpdateService.class);
            context.startService(serviceIntent);
        }
    }
}