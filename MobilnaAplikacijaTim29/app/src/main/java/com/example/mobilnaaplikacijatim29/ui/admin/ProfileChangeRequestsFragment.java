package com.example.mobilnaaplikacijatim29.ui.admin;

import android.os.Bundle;
import android.view.*;
import android.widget.*;
import android.graphics.BitmapFactory;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.mobilnaaplikacijatim29.R;
import com.example.mobilnaaplikacijatim29.data.api.ApiClient;
import com.example.mobilnaaplikacijatim29.data.model.*;
import com.example.mobilnaaplikacijatim29.data.session.SessionManager;
import com.google.android.material.button.MaterialButton;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import okhttp3.ResponseBody;

public class ProfileChangeRequestsFragment extends Fragment {
    private LinearLayout container;
    private TextView message;
    private SessionManager session;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup parent,
                             @Nullable Bundle state) {
        return inflater.inflate(R.layout.fragment_profile_change_requests, parent, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        container = view.findViewById(R.id.profile_requests_container);
        message = view.findViewById(R.id.profile_requests_message);
        session = new SessionManager(requireContext());
        load();
    }

    private void load() {
        ApiClient.getApi().getDriverProfileChangeRequests(session.getAuthorizationHeader())
                .enqueue(new Callback<>() {
                    @Override public void onResponse(@NonNull Call<List<DriverProfileChangeResponse>> call,
                                                     @NonNull Response<List<DriverProfileChangeResponse>> response) {
                        if (!isAdded()) return;
                        container.removeAllViews();
                        if (!response.isSuccessful() || response.body() == null) {
                            message.setText("Zahtevi nisu dostupni (HTTP " + response.code() + ").");
                            return;
                        }
                        message.setText(response.body().isEmpty()
                                ? "Nema zahteva na čekanju." : "Zahtevi na čekanju: " + response.body().size());
                        for (DriverProfileChangeResponse item : response.body()) addRequest(item);
                    }
                    @Override public void onFailure(@NonNull Call<List<DriverProfileChangeResponse>> call,
                                                    @NonNull Throwable throwable) {
                        if (isAdded()) message.setText("Backend nije dostupan: " + throwable.getMessage());
                    }
                });
    }

    private void addRequest(DriverProfileChangeResponse item) {
        ProfileResponse profile = item.getProposedProfile();
        LinearLayout card = new LinearLayout(requireContext());
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(20, 20, 20, 20);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 20, 0, 0);
        card.setLayoutParams(params);
        card.setBackgroundColor(requireContext().getColor(android.R.color.darker_gray));
        TextView details = new TextView(requireContext());
        StringBuilder text = new StringBuilder()
                .append(profile.getName()).append(' ').append(profile.getSurname()).append('\n')
                .append(profile.getEmail()).append('\n')
                .append(profile.getAddress()).append(" • ").append(profile.getPhone());
        if (profile.getVehicle() != null) {
            ProfileVehicle vehicle = profile.getVehicle();
            text.append("\nVozilo: ").append(vehicle.getModel()).append(" • ")
                    .append(vehicle.getType()).append(" • ").append(vehicle.getRegistration())
                    .append(" • ").append(vehicle.getSeats()).append(" mesta")
                    .append("\nBebe: ").append(vehicle.isBabyFriendly() ? "da" : "ne")
                    .append(" • Ljubimci: ").append(vehicle.isPetFriendly() ? "da" : "ne");
        }
        details.setText(text.toString());
        card.addView(details);
        if (profile.getProfileImageUrl() != null && !profile.getProfileImageUrl().isBlank()) {
            ImageView proposedImage = new ImageView(requireContext());
            proposedImage.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 260));
            proposedImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
            proposedImage.setImageResource(R.mipmap.ic_launcher);
            card.addView(proposedImage);
            ApiClient.getApi().downloadFile(profile.getProfileImageUrl()).enqueue(new Callback<>() {
                @Override public void onResponse(@NonNull Call<ResponseBody> call,
                                                 @NonNull Response<ResponseBody> response) {
                    if (isAdded() && response.isSuccessful() && response.body() != null) {
                        proposedImage.setImageBitmap(
                                BitmapFactory.decodeStream(response.body().byteStream()));
                    }
                }
                @Override public void onFailure(@NonNull Call<ResponseBody> call,
                                                @NonNull Throwable throwable) { }
            });
        }
        LinearLayout actions = new LinearLayout(requireContext());
        actions.setOrientation(LinearLayout.HORIZONTAL);
        MaterialButton approve = new MaterialButton(requireContext());
        approve.setText("Odobri");
        MaterialButton reject = new MaterialButton(requireContext());
        reject.setText("Odbij");
        actions.addView(approve, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        actions.addView(reject, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        card.addView(actions);
        approve.setOnClickListener(v -> resolve(item.getRequestId(), true));
        reject.setOnClickListener(v -> resolve(item.getRequestId(), false));
        container.addView(card);
    }

    private void resolve(long id, boolean approve) {
        Call<Void> call = approve
                ? ApiClient.getApi().approveDriverProfileChange(session.getAuthorizationHeader(), id)
                : ApiClient.getApi().rejectDriverProfileChange(session.getAuthorizationHeader(), id);
        call.enqueue(new Callback<>() {
            @Override public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (response.isSuccessful()) load();
                else message.setText("Odluka nije sačuvana (HTTP " + response.code() + ").");
            }
            @Override public void onFailure(@NonNull Call<Void> call, @NonNull Throwable throwable) {
                message.setText("Backend nije dostupan: " + throwable.getMessage());
            }
        });
    }
}
