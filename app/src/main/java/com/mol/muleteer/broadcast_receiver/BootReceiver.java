package com.mol.muleteer.broadcast_receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.mol.muleteer.service.MuleteerService;

/**
 * Created by igor shaula
 */
public class BootReceiver extends BroadcastReceiver {
   @Override
   public void onReceive(Context context, Intent intent) {
      context.startService(new Intent(context, MuleteerService.class));
   }
}