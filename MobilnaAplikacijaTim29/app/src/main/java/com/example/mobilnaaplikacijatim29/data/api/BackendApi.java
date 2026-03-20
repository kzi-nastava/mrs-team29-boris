package com.example.mobilnaaplikacijatim29.data.api;

import com.example.mobilnaaplikacijatim29.data.model.ActiveVehicleResponse;
import com.example.mobilnaaplikacijatim29.data.model.LoginRequest;
import com.example.mobilnaaplikacijatim29.data.model.LoginResponse;
import com.example.mobilnaaplikacijatim29.data.model.RegisterRequest;
import com.example.mobilnaaplikacijatim29.data.model.UserProfileResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

public interface BackendApi {

    @GET("api/vehicles/active")
    Call<List<ActiveVehicleResponse>> getActiveVehicles();

    @POST("api/user/auth/login")
    Call<LoginResponse> login(@Body LoginRequest request);

    @POST("api/user/auth/register")
    Call<UserProfileResponse> register(@Body RegisterRequest request);
}
