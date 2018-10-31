package com.cshriakhil.vicinity;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by thegamer007 on 1/2/18.
 */

public class ApiClient {
    static String BASE_URL = "http://192.168.137.1:8080/";
    private static Gson gson = new GsonBuilder().setLenient().create();
    private static Retrofit retro = null;

    public static Retrofit getInstance() {
        if (retro == null) {
            retro = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .build();
        }
        return retro;
    }

    public static void setBaseUrl(String url) {
        BASE_URL = url;
        retro = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();
    }
}
