package com.mol.muleteer.zzz_not_used.rest_connection_settings.sample_github;

import okhttp3.OkHttpClient;
import retrofit2.GsonConverterFactory;
import retrofit2.Retrofit;

/**
 * Created by igor shaula
 */
public class ServiceGenerator {

   public static final String API_BASE_URL = "https://api.github.com/";

   private static OkHttpClient.Builder httpClient = new OkHttpClient.Builder();

   private static Retrofit.Builder builder = new Retrofit.Builder()
            .baseUrl(API_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create());

   public static <S> S createService(Class<S> serviceClass) {
      Retrofit retrofit = builder.client(httpClient.build()).build();
      return retrofit.create(serviceClass);
   }
}