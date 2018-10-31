package com.cshriakhil.vicinity;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

/**
 * Created by TheGamer007 on 26-10-2017.
 */

public interface ServerAPI {
    @GET("{mac}/{lon}/{lat}")
    Call<List<LocData>> getUsers(@Path("mac") String mac, @Path("lon") double lon, @Path("lat") double lat);
}
