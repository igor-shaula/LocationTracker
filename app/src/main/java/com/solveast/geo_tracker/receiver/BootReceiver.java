package com.solveast.geo_tracker.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.solveast.geo_tracker.MyLog;
import com.solveast.geo_tracker.service.MainService;

/**
 * Created by igor shaula - to reload the service after system reboot \
 */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        context.startService(new Intent(context, MainService.class));
        MyLog.i("onReceive worked = service is launched");
    }
}