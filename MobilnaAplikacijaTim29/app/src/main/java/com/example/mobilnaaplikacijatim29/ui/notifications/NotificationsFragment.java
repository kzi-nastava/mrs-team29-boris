package com.example.mobilnaaplikacijatim29.ui.notifications;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.mobilnaaplikacijatim29.R;
import com.example.mobilnaaplikacijatim29.MainActivity;
import com.example.mobilnaaplikacijatim29.data.api.ApiClient;
import com.example.mobilnaaplikacijatim29.data.model.AppNotification;
import com.example.mobilnaaplikacijatim29.data.session.SessionManager;
import com.google.android.material.card.MaterialCardView;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NotificationsFragment extends Fragment {
    private LinearLayout container;
    private TextView message;
    private SessionManager session;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup parent,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_notifications, parent, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        session = new SessionManager(requireContext());
        container = view.findViewById(R.id.notifications_container);
        message = view.findViewById(R.id.notifications_message);
        load();
    }

    private void load() {
        ApiClient.getApi().getNotifications(session.getAuthorizationHeader())
                .enqueue(new Callback<>() {
                    @Override public void onResponse(@NonNull Call<List<AppNotification>> call,
                                                     @NonNull Response<List<AppNotification>> response) {
                        if (!isAdded()) return;
                        if (!response.isSuccessful() || response.body() == null) {
                            message.setText("Obaveštenja nisu učitana (HTTP " + response.code() + ").");
                            return;
                        }
                        render(response.body());
                        ApiClient.getApi().markNotificationsSeen(session.getAuthorizationHeader())
                                .enqueue(new Callback<>() {
                                    @Override public void onResponse(@NonNull Call<Void> call,
                                                                     @NonNull Response<Void> response) { }
                                    @Override public void onFailure(@NonNull Call<Void> call,
                                                                    @NonNull Throwable throwable) { }
                                });
                    }
                    @Override public void onFailure(@NonNull Call<List<AppNotification>> call,
                                                    @NonNull Throwable throwable) {
                        if (isAdded()) message.setText("Backend nije dostupan: " + throwable.getMessage());
                    }
                });
    }

    private void render(List<AppNotification> values) {
        container.removeAllViews();
        message.setVisibility(values.isEmpty() ? View.VISIBLE : View.GONE);
        if (values.isEmpty()) {
            message.setText("Još nema obaveštenja.");
            return;
        }
        for (AppNotification value : values) {
            MaterialCardView card = new MaterialCardView(requireContext());
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 0, 0, dp(10));
            card.setLayoutParams(params);
            card.setRadius(dp(12));
            card.setCardElevation(value.isSeen() ? 1 : 4);
            card.setContentPadding(dp(14), dp(12), dp(14), dp(12));
            TextView text = new TextView(requireContext());
            text.setText((value.isSeen() ? "" : "NOVO\n") + safe(value.getContent())
                    + (value.getRideId() == null ? "" : "\nVožnja #" + value.getRideId())
                    + "\n" + displayDate(value.getCreatedAt()));
            card.addView(text);
            if (value.getRideId() != null && !"RIDE_REJECTED".equals(value.getType())) {
                card.setClickable(true);
                card.setFocusable(true);
                card.setOnClickListener(view -> ((MainActivity) requireActivity())
                        .navigateToRideTracking(value.getRideId()));
            }
            container.addView(card);
        }
    }

    private static String displayDate(String value) {
        if (value == null) return "";
        String normalized = value.replace('T', ' ');
        return normalized.length() > 16 ? normalized.substring(0, 16) : normalized;
    }
    private static String safe(String value) { return value == null ? "" : value; }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}
