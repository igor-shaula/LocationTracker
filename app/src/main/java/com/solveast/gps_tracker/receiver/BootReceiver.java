package com.solveast.gps_tracker.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.solveast.gps_tracker.MyLog;
import com.solveast.gps_tracker.service.MainService;

/**
 * Created by igor shaula
 */
public class BootReceiver extends BroadcastReceiver {
   @Override
   public void onReceive(Context context, Intent intent) {
      context.startService(new Intent(context, MainService.class));
      MyLog.v("onReceive worked = service is launched");
   }
}