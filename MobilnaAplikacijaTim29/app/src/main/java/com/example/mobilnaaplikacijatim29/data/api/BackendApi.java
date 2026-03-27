package com.example.mobilnaaplikacijatim29.data.api;

import com.example.mobilnaaplikacijatim29.data.model.ActiveVehicleResponse;
import com.example.mobilnaaplikacijatim29.data.model.LoginRequest;
import com.example.mobilnaaplikacijatim29.data.model.LoginResponse;
import com.example.mobilnaaplikacijatim29.data.model.RegisterRequest;
import com.example.mobilnaaplikacijatim29.data.model.UserProfileResponse;
import com.example.mobilnaaplikacijatim29.data.model.ForgotPasswordRequest;
import com.example.mobilnaaplikacijatim29.data.model.ResetPasswordRequest;
import com.example.mobilnaaplikacijatim29.data.model.DriverStatusRequest;
import com.example.mobilnaaplikacijatim29.data.model.DriverStatusResponse;
import com.example.mobilnaaplikacijatim29.data.model.DriverRegistrationRequest;
import com.example.mobilnaaplikacijatim29.data.model.DriverRegistrationResponse;
import com.example.mobilnaaplikacijatim29.data.model.CompleteRegistrationRequest;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.PATCH;
import retrofit2.http.Path;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface BackendApi {

    @GET("api/vehicles/active")
    Call<List<ActiveVehicleResponse>> getActiveVehicles();

    @POST("api/user/auth/login")
    Call<LoginResponse> login(@Body LoginRequest request);

    @POST("api/user/auth/register")
    Call<UserProfileResponse> register(@Body RegisterRequest request);

    @POST("api/user/auth/forgot-password")
    Call<Void> forgotPassword(@Body ForgotPasswordRequest request);

    @POST("api/user/auth/reset-password")
    Call<Void> resetPassword(@Body ResetPasswordRequest request);

    @POST("api/user/auth/logout")
    Call<Void> logout(@Header("Authorization") String authorization);

    @GET("api/user/drivers/{id}/status")
    Call<DriverStatusResponse> getDriverStatus(
            @Header("Authorization") String authorization,
            @Path("id") long driverId
    );

    @PATCH("api/user/drivers/{id}/status")
    Call<DriverStatusResponse> changeDriverStatus(
            @Header("Authorization") String authorization,
            @Path("id") long driverId,
            @Body DriverStatusRequest request
    );

    @POST("api/admin/drivers")
    Call<DriverRegistrationResponse> registerDriver(
            @Header("Authorization") String authorization,
            @Query("platform") String platform,
            @Body DriverRegistrationRequest request
    );

    @POST("api/drivers/complete-registration")
    Call<Void> completeDriverRegistration(@Body CompleteRegistrationRequest request);
}
