package com.mol.drivergps.rest_connection;

import com.mol.drivergps.entity_description.DriverData;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

/**
 * Created by igor shaula
 */
public interface MyRetrofitInterface {

   @POST("/location/driver_id")
   Call<DriverData> makeDriverDataCall(@Body DriverData driverData);
}