package com.example.mobilnaaplikacijatim29.ui.driver;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.mobilnaaplikacijatim29.MainActivity;
import com.example.mobilnaaplikacijatim29.R;
import com.example.mobilnaaplikacijatim29.data.api.ApiClient;
import com.example.mobilnaaplikacijatim29.data.model.DriverStatusRequest;
import com.example.mobilnaaplikacijatim29.data.model.DriverStatusResponse;
import com.example.mobilnaaplikacijatim29.data.session.SessionManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DriverDashboardFragment extends Fragment {
    private SessionManager session;
    private TextView statusView;
    private TextView messageView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        session = new SessionManager(requireContext());
        statusView = view.findViewById(R.id.driver_status_text);
        messageView = view.findViewById(R.id.dashboard_message);
        ((TextView) view.findViewById(R.id.dashboard_title)).setText("Panel vozača");
        ((TextView) view.findViewById(R.id.dashboard_subtitle))
                .setText("Prijavljeni ste kao " + session.getEmail());
        view.findViewById(R.id.driver_status_controls).setVisibility(View.VISIBLE);
        view.findViewById(R.id.driver_set_active).setOnClickListener(v -> changeStatus("ACTIVE"));
        view.findViewById(R.id.driver_set_inactive).setOnClickListener(v -> changeStatus("INACTIVE"));
        view.findViewById(R.id.dashboard_logout).setOnClickListener(v ->
                ((MainActivity) requireActivity()).requestLogout(this::showError));
        loadStatus();
    }

    private void loadStatus() {
        ApiClient.getApi().getDriverStatus(
                session.getAuthorizationHeader(), session.getUserId())
                .enqueue(statusCallback());
    }

    private void changeStatus(String status) {
        ApiClient.getApi().changeDriverStatus(
                session.getAuthorizationHeader(), session.getUserId(),
                new DriverStatusRequest(status)).enqueue(statusCallback());
    }

    private Callback<DriverStatusResponse> statusCallback() {
        return new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<DriverStatusResponse> call,
                                   @NonNull Response<DriverStatusResponse> response) {
                if (!isAdded()) return;
                if (!response.isSuccessful() || response.body() == null) {
                    showError("Promena statusa nije uspela (HTTP " + response.code() + ").");
                    return;
                }
                DriverStatusResponse status = response.body();
                if (status.isDeactivateAfterRide()) {
                    statusView.setText("Status: aktivan tokom trenutne vožnje; potom neaktivan");
                } else {
                    statusView.setText("Status: " + ("ACTIVE".equals(status.getStatus())
                            ? "aktivan" : "neaktivan"));
                }
                messageView.setVisibility(View.GONE);
            }

            @Override
            public void onFailure(@NonNull Call<DriverStatusResponse> call,
                                  @NonNull Throwable throwable) {
                if (isAdded()) showError("Backend nije dostupan: " + throwable.getMessage());
            }
        };
    }

    private void showError(String message) {
        messageView.setText(message);
        messageView.setVisibility(View.VISIBLE);
    }
}
