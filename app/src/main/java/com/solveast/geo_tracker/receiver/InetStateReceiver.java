package com.solveast.geo_tracker.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.solveast.geo_tracker.GlobalKeys;
import com.solveast.geo_tracker.eventbus.RadioStateChangeEvent;

import org.greenrobot.eventbus.EventBus;

/**
 * Created by igor shaula - to react on internet availability \
 */
public class InetStateReceiver extends BroadcastReceiver {
   @Override
   public void onReceive(Context context, Intent intent) {

      ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
      NetworkInfo networkInfo = cm.getActiveNetworkInfo();

      if (networkInfo != null && networkInfo.isConnected())
         EventBus.getDefault().post(new RadioStateChangeEvent(GlobalKeys.EVENT_INET_ON));
      else
         EventBus.getDefault().post(new RadioStateChangeEvent(GlobalKeys.EVENT_INET_OFF));
   }
}