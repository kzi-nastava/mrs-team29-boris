package com.example.mobilnaaplikacijatim29.data.api;

import com.example.mobilnaaplikacijatim29.data.model.ActiveVehicleResponse;
import com.example.mobilnaaplikacijatim29.data.model.RoutePreviewRequest;
import com.example.mobilnaaplikacijatim29.data.model.RoutePreviewResponse;
import com.example.mobilnaaplikacijatim29.data.model.RideTrackingResponse;
import com.example.mobilnaaplikacijatim29.data.model.LoginRequest;
import com.example.mobilnaaplikacijatim29.data.model.LoginResponse;
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
import retrofit2.http.PUT;
import retrofit2.http.DELETE;
import retrofit2.http.Multipart;
import retrofit2.http.Part;
import retrofit2.http.Url;
import okhttp3.MultipartBody;
import okhttp3.ResponseBody;
import com.example.mobilnaaplikacijatim29.data.model.ProfileResponse;
import com.example.mobilnaaplikacijatim29.data.model.ProfileUpdateRequest;
import com.example.mobilnaaplikacijatim29.data.model.PasswordChangeRequest;
import com.example.mobilnaaplikacijatim29.data.model.DriverProfileChangeResponse;
import com.example.mobilnaaplikacijatim29.data.model.DriverRideHistoryItem;
import com.example.mobilnaaplikacijatim29.data.model.ReportRequest;
import com.example.mobilnaaplikacijatim29.data.model.ReportResponse;
import com.example.mobilnaaplikacijatim29.data.model.AdminReportUser;
import com.example.mobilnaaplikacijatim29.data.model.BlockableUser;
import com.example.mobilnaaplikacijatim29.data.model.BlockNoteRequest;
import com.example.mobilnaaplikacijatim29.data.model.SupportMessage;
import com.example.mobilnaaplikacijatim29.data.model.SupportMessageRequest;
import com.example.mobilnaaplikacijatim29.data.model.SupportConversation;
import com.example.mobilnaaplikacijatim29.data.model.VehiclePriceConfig;
import com.example.mobilnaaplikacijatim29.data.model.CreateRideRequest;
import com.example.mobilnaaplikacijatim29.data.model.RideBookingResponse;
import com.example.mobilnaaplikacijatim29.data.model.AppNotification;
import com.example.mobilnaaplikacijatim29.data.model.PageResponse;
import com.example.mobilnaaplikacijatim29.data.model.ScheduledRide;
import com.example.mobilnaaplikacijatim29.data.model.StartRideRequest;
import com.example.mobilnaaplikacijatim29.data.model.RideReviewRequest;

public interface BackendApi {

    @GET("api/notifications")
    Call<List<AppNotification>> getNotifications(
            @Header("Authorization") String authorization);

    @POST("api/notifications/seen")
    Call<Void> markNotificationsSeen(
            @Header("Authorization") String authorization);

    @GET("api/vehicles/prices")
    Call<VehiclePriceConfig> getBookingPrices(
            @Header("Authorization") String authorization);

    @POST("api/rides/create-ride")
    Call<RideBookingResponse> createRide(
            @Header("Authorization") String authorization,
            @Body CreateRideRequest request);

    @GET("api/rides/driver/{driverId}")
    Call<PageResponse<ScheduledRide>> getDriverScheduledRides(
            @Header("Authorization") String authorization,
            @Path("driverId") long driverId,
            @Query("page") int page,
            @Query("size") int size);

    @POST("api/rides/{id}/start")
    Call<Void> startRide(
            @Header("Authorization") String authorization,
            @Path("id") long rideId,
            @Body StartRideRequest request);

    @PUT("api/rides/{id}/finish")
    Call<Void> finishRide(
            @Header("Authorization") String authorization,
            @Path("id") long rideId,
            @Query("isGuest") boolean isGuest);

    @POST("api/rides/{id}/review")
    Call<Void> reviewRide(
            @Header("Authorization") String authorization,
            @Path("id") long rideId,
            @Body RideReviewRequest request);

    @GET("api/chat/support/messages")
    Call<List<SupportMessage>> getSupportMessages(
            @Header("Authorization") String authorization,
            @Query("userId") Long userId);

    @POST("api/chat/support/messages")
    Call<SupportMessage> sendSupportMessage(
            @Header("Authorization") String authorization,
            @Body SupportMessageRequest request);

    @GET("api/chat/support/conversations")
    Call<List<SupportConversation>> getSupportConversations(
            @Header("Authorization") String authorization);

    @GET("api/admin/prices")
    Call<VehiclePriceConfig> getVehiclePrices(
            @Header("Authorization") String authorization);

    @PUT("api/admin/prices")
    Call<VehiclePriceConfig> updateVehiclePrices(
            @Header("Authorization") String authorization,
            @Body VehiclePriceConfig prices);

    @GET("api/admin/drivers/all")
    Call<List<BlockableUser>> getAllDriversForBlocking(
            @Header("Authorization") String authorization);

    @GET("api/admin/passengers/all")
    Call<List<BlockableUser>> getAllPassengersForBlocking(
            @Header("Authorization") String authorization);

    @PUT("api/admin/users/{id}/block")
    Call<BlockableUser> blockUser(
            @Header("Authorization") String authorization,
            @Path("id") long userId,
            @Body BlockNoteRequest request);

    @PUT("api/admin/users/{id}/unblock")
    Call<BlockableUser> unblockUser(
            @Header("Authorization") String authorization,
            @Path("id") long userId);

    @PUT("api/admin/users/{id}/note")
    Call<BlockableUser> updateBlockNote(
            @Header("Authorization") String authorization,
            @Path("id") long userId,
            @Body BlockNoteRequest request);

    @POST("api/reports/generate")
    Call<ReportResponse> generateReport(
            @Header("Authorization") String authorization,
            @Body ReportRequest request);

    @GET("api/admin/users")
    Call<List<AdminReportUser>> getReportUsers(
            @Header("Authorization") String authorization);

    @GET("api/drivers/{id}/ride-history")
    Call<List<DriverRideHistoryItem>> getDriverRideHistory(
            @Header("Authorization") String authorization,
            @Path("id") long driverId,
            @Query("from") String from,
            @Query("to") String to);

    @GET("api/drivers/{id}/ride-history/{rideId}")
    Call<DriverRideHistoryItem> getDriverRideHistoryDetail(
            @Header("Authorization") String authorization,
            @Path("id") long driverId,
            @Path("rideId") long rideId,
            @Query("guest") boolean guest);

    @GET("api/vehicles/active")
    Call<List<ActiveVehicleResponse>> getActiveVehicles();

    @POST("api/routing/preview")
    Call<RoutePreviewResponse> previewRoute(
            @Header("Authorization") String authorization,
            @Body RoutePreviewRequest request);

    @GET("api/rides/{id}/tracking")
    Call<RideTrackingResponse> getRideTracking(
            @Header("Authorization") String authorization,
            @Path("id") long rideId);

    @POST("api/user/auth/login")
    Call<LoginResponse> login(@Body LoginRequest request);

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

    @GET("api/profile/me")
    Call<ProfileResponse> getOwnProfile(@Header("Authorization") String authorization);

    @PUT("api/profile/me")
    Call<ProfileResponse> updateOwnProfile(
            @Header("Authorization") String authorization,
            @Body ProfileUpdateRequest request);

    @Multipart
    @POST("api/profile/me/image")
    Call<ProfileResponse> uploadProfileImage(
            @Header("Authorization") String authorization,
            @Part MultipartBody.Part file);

    @DELETE("api/profile/me/image")
    Call<ProfileResponse> deleteProfileImage(@Header("Authorization") String authorization);

    @POST("api/profile/me/password")
    Call<Void> changeProfilePassword(
            @Header("Authorization") String authorization,
            @Body PasswordChangeRequest request);

    @GET("api/profile/driver-change-requests")
    Call<List<DriverProfileChangeResponse>> getDriverProfileChangeRequests(
            @Header("Authorization") String authorization);

    @POST("api/profile/driver-change-requests/{id}/approve")
    Call<Void> approveDriverProfileChange(
            @Header("Authorization") String authorization, @Path("id") long requestId);

    @POST("api/profile/driver-change-requests/{id}/reject")
    Call<Void> rejectDriverProfileChange(
            @Header("Authorization") String authorization, @Path("id") long requestId);

    @GET
    Call<ResponseBody> downloadFile(@Url String url);
}
