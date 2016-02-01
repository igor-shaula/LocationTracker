package com.mol.drivergps.broadcast_receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.mol.drivergps.service.MyService;

/**
 * Created by igor shaula
 */
public class MyBootReceiver extends BroadcastReceiver {
   @Override
   public void onReceive(Context context, Intent intent) {

      context.startService(new Intent(context, MyService.class));

   }
}