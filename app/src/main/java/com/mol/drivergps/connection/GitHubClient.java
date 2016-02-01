package com.mol.drivergps.connection;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

/**
 * Created by igor shaula
 */
public interface GitHubClient {

   @GET("/repos/{owner}/{repo}/contributors")
   Call<List<Contributor>> contributors(
            @Path("owner") String owner,
            @Path("repo") String repo
   );
}