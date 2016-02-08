package com.mol.drivergps.rest_connection;

import com.mol.drivergps.entity_description.DriverData;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

/**
 * Created by igor shaula
 */
public interface MyRetrofitInterface {

   @POST("{path}")
//   @GET("{path}")
   Call<DriverData> makeDriverDataCall(@Path("path") String path, @Body DriverData driverData);
//   Call<DriverData> makeDriverDataCall(@Path("path") String path, @Body String stringToSend);
}