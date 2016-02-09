package com.mol.muleteer.zzz_not_used.rest_connection_settings;

import com.mol.muleteer.entity_description.DriverData;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Path;

/**
 * Created by igor shaula
 */
public interface MyRetrofitInterface {

   @Headers("Content-Type: application/json")
   @POST("{path}")
   Call<DriverData> makeDriverDataCall(@Path("path") String path, @Body DriverData driverData);
}