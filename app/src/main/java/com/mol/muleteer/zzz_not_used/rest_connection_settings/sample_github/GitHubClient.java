package com.mol.muleteer.zzz_not_used.rest_connection_settings.sample_github;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

/**
 * Created by igor shaula
 */
public interface GitHubClient {

   @GET("/repos/{owner}/{repo}/getContributors")
   Call<List<Contributor>> getContributors(
            @Path("owner") String owner,
            @Path("repo") String repo
   );
}