package com.example.mobilnaaplikacijatim29.data.api;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public final class ApiClient {

    private static final String BACKEND_BASE_URL = "http://192.168.0.29:8080/";

    private static final BackendApi API = new Retrofit.Builder()
            .baseUrl(BACKEND_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(BackendApi.class);

    private ApiClient() {
    }

    public static BackendApi getApi() {
        return API;
    }
}
