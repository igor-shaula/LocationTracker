package com.solveast.geo_tracker.entity;

/**
 * Created by igor shaula - to hold data for GSON \
 */
public class ContinuousMode {

   private int id;
   private long switchTime;
   private String state;

   public ContinuousMode(int id, long switchTime, String state) {
      this.id = id;
      this.switchTime = switchTime;
      this.state = state;
   }
}