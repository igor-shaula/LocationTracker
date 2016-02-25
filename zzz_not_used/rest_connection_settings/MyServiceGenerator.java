/*
package com.solveast.gps_tracker.zzz_not_used.rest_connection_settings;

import okhttp3.OkHttpClient;
import retrofit2.GsonConverterFactory;
import retrofit2.Retrofit;

*/
/**
 * Created by igor shaula
 *//*

public class MyServiceGenerator {

   public static final String API_BASE_URL = "http://gps_tracker.herokuapp.com";

   public static <S> S createService(Class<S> serviceClass) {
*/
/*
      Interceptor newInterseptor = new Interceptor() {
         @Override
         public Response intercept(Chain chain) throws IOException {
            Request original = chain.request();
            // Customize the request
            Request newRequest = original.newBuilder()
                     .addHeader("Content-Type", "application/json")
                     .method(original.method(), original.body())
                     .build();
            // Customize or return the response
            return chain.proceed(newRequest);
         }
      };
      httpClientBuilder.interceptors().add(newInterseptor);
*//*

      OkHttpClient.Builder httpClientBuilder = new OkHttpClient.Builder();

      Retrofit.Builder retrofitBuilder = new Retrofit.Builder()
               .baseUrl(API_BASE_URL)
               .addConverterFactory(GsonConverterFactory.create());

      Retrofit retrofit = retrofitBuilder.client(httpClientBuilder.build()).build();
      return retrofit.create(serviceClass);
   }
}*/
