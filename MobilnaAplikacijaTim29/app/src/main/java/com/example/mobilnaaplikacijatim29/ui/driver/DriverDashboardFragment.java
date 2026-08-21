package com.example.mobilnaaplikacijatim29.ui.driver;

import android.os.Bundle;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.mobilnaaplikacijatim29.MainActivity;
import com.example.mobilnaaplikacijatim29.R;
import com.example.mobilnaaplikacijatim29.data.api.ApiClient;
import com.example.mobilnaaplikacijatim29.data.model.DriverStatusRequest;
import com.example.mobilnaaplikacijatim29.data.model.DriverStatusResponse;
import com.example.mobilnaaplikacijatim29.data.model.PageResponse;
import com.example.mobilnaaplikacijatim29.data.model.RidePassenger;
import com.example.mobilnaaplikacijatim29.data.model.ScheduledRide;
import com.example.mobilnaaplikacijatim29.data.model.StartRideRequest;
import com.example.mobilnaaplikacijatim29.data.session.SessionManager;
import com.example.mobilnaaplikacijatim29.domain.RideStartCountdown;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.List;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DriverDashboardFragment extends Fragment {
    private SessionManager session;
    private TextView statusView;
    private TextView messageView;
    private View setActiveButton;
    private View setInactiveButton;
    private LinearLayout assignedRidesContainer;
    private TextView assignedRidesStatus;
    private boolean assignedRidesRequestInProgress;

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
        setActiveButton = view.findViewById(R.id.driver_set_active);
        setInactiveButton = view.findViewById(R.id.driver_set_inactive);
        assignedRidesContainer = view.findViewById(R.id.driver_assigned_rides_container);
        assignedRidesStatus = view.findViewById(R.id.driver_assigned_rides_status);
        ((TextView) view.findViewById(R.id.dashboard_title)).setText("Panel vozača");
        ((TextView) view.findViewById(R.id.dashboard_subtitle))
                .setText("Prijavljeni ste kao " + session.getEmail());
        view.findViewById(R.id.driver_status_controls).setVisibility(View.VISIBLE);
        view.findViewById(R.id.driver_assigned_rides_section).setVisibility(View.VISIBLE);
        view.findViewById(R.id.driver_ride_history).setVisibility(View.VISIBLE);
        view.findViewById(R.id.driver_ride_history).setOnClickListener(v ->
                ((MainActivity) requireActivity()).navigateTo(R.id.nav_driver_ride_history));
        view.findViewById(R.id.reports_button).setVisibility(View.VISIBLE);
        view.findViewById(R.id.reports_button).setOnClickListener(v ->
                ((MainActivity) requireActivity()).navigateTo(R.id.nav_reports));
        setActiveButton.setOnClickListener(v -> changeStatus("ACTIVE"));
        setInactiveButton.setOnClickListener(v -> changeStatus("INACTIVE"));
        loadStatus();
    }

    private void loadAssignedRides() {
        if (assignedRidesRequestInProgress || session == null
                || assignedRidesStatus == null || assignedRidesContainer == null) return;
        assignedRidesRequestInProgress = true;
        assignedRidesStatus.setVisibility(View.VISIBLE);
        assignedRidesStatus.setText("Učitavanje dodeljenih vožnji...");
        ApiClient.getApi().getDriverScheduledRides(
                        session.getAuthorizationHeader(), session.getUserId(), 1, 20)
                .enqueue(new Callback<>() {
                    @Override
                    public void onResponse(@NonNull Call<PageResponse<ScheduledRide>> call,
                                           @NonNull Response<PageResponse<ScheduledRide>> response) {
                        assignedRidesRequestInProgress = false;
                        if (!isAdded() || assignedRidesContainer == null) return;
                        if (!response.isSuccessful() || response.body() == null) {
                            assignedRidesStatus.setText("Dodeljene vožnje nisu učitane (HTTP "
                                    + response.code() + ").");
                            return;
                        }
                        renderAssignedRides(response.body().getContent());
                    }

                    @Override
                    public void onFailure(@NonNull Call<PageResponse<ScheduledRide>> call,
                                          @NonNull Throwable throwable) {
                        assignedRidesRequestInProgress = false;
                        if (isAdded() && assignedRidesStatus != null) assignedRidesStatus.setText(
                                "Backend nije dostupan: " + throwable.getMessage());
                    }
                });
    }

    private void renderAssignedRides(List<ScheduledRide> rides) {
        assignedRidesContainer.removeAllViews();
        if (rides.isEmpty()) {
            assignedRidesStatus.setText("Trenutno nemate dodeljenih zakazanih vožnji.");
            assignedRidesStatus.setVisibility(View.VISIBLE);
            return;
        }
        assignedRidesStatus.setVisibility(View.GONE);
        for (ScheduledRide ride : rides) addAssignedRideCard(ride);
    }

    private void addAssignedRideCard(ScheduledRide ride) {
        MaterialCardView card = new MaterialCardView(requireContext());
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(0, 0, 0, dp(10));
        card.setLayoutParams(cardParams);
        card.setRadius(dp(12));
        card.setCardElevation(dp(2));
        card.setContentPadding(dp(14), dp(12), dp(14), dp(12));

        LinearLayout content = new LinearLayout(requireContext());
        content.setOrientation(LinearLayout.VERTICAL);
        TextView details = new TextView(requireContext());
        details.setText("Vožnja #" + ride.getId()
                + "\n" + safe(ride.getOrigin()) + " → " + safe(ride.getDestination())
                + "\nZakazano: " + displayTime(ride.getScheduledTime())
                + "\nPutnici: " + passengerNames(ride.getPassengers()));
        details.setTextAppearance(
                com.google.android.material.R.style.TextAppearance_Material3_BodyLarge);
        content.addView(details);

        TextView countdown = new TextView(requireContext());
        countdown.setTextAppearance(
                com.google.android.material.R.style.TextAppearance_Material3_TitleMedium);
        LinearLayout.LayoutParams countdownParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        countdownParams.setMargins(0, dp(10), 0, 0);
        countdown.setLayoutParams(countdownParams);
        content.addView(countdown);

        MaterialButton startButton = new MaterialButton(requireContext());
        startButton.setText("Započni vožnju");
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        buttonParams.setMargins(0, dp(10), 0, 0);
        startButton.setLayoutParams(buttonParams);
        startButton.setOnClickListener(v -> confirmStartRide(ride, startButton));
        content.addView(startButton);
        card.addView(content);
        assignedRidesContainer.addView(card);
        configureStartAvailability(startButton, countdown, ride);
    }

    private void configureStartAvailability(MaterialButton button, TextView countdown,
                                            ScheduledRide ride) {
        Long remainingSeconds = resolveRemainingSeconds(ride);
        if (remainingSeconds == null) {
            button.setEnabled(false);
            countdown.setText("Početak za: vreme nije dostupno");
            return;
        }
        if (RideStartCountdown.canStart(remainingSeconds)) {
            button.setEnabled(true);
            countdown.setText("Zakazano vreme je stiglo — vožnja može da počne.");
            return;
        }

        button.setEnabled(false);
        long deadline = SystemClock.elapsedRealtime() + remainingSeconds * 1000L;
        Runnable tick = new Runnable() {
            @Override
            public void run() {
                if (!isAdded() || !button.isAttachedToWindow()) return;
                long remainingMillis = deadline - SystemClock.elapsedRealtime();
                long seconds = Math.max(0L, (remainingMillis + 999L) / 1000L);
                countdown.setText("Početak za: " + RideStartCountdown.format(seconds));
                if (RideStartCountdown.canStart(seconds)) {
                    countdown.setText("Zakazano vreme je stiglo — vožnja može da počne.");
                    button.setEnabled(true);
                    return;
                }
                button.postDelayed(this, Math.min(1000L, remainingMillis));
            }
        };
        button.post(tick);
    }

    private Long resolveRemainingSeconds(ScheduledRide ride) {
        if (ride.getSecondsUntilStart() != null) {
            return Math.max(0L, ride.getSecondsUntilStart());
        }
        try {
            LocalDateTime scheduled = LocalDateTime.parse(ride.getScheduledTime());
            long remainingMillis = Duration.between(LocalDateTime.now(), scheduled).toMillis();
            if (remainingMillis <= 0) return 0L;
            return (remainingMillis + 999L) / 1000L;
        } catch (DateTimeParseException | NullPointerException exception) {
            return null;
        }
    }

    private void confirmStartRide(ScheduledRide ride, MaterialButton button) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Početak vožnje")
                .setMessage("Potvrdite da su svi putnici pristupili vozilu i da vožnja može da počne.")
                .setNegativeButton("Odustani", null)
                .setPositiveButton("Započni", (dialog, which) -> startRide(ride, button))
                .show();
    }

    private void startRide(ScheduledRide ride, MaterialButton button) {
        button.setEnabled(false);
        ApiClient.getApi().startRide(session.getAuthorizationHeader(), ride.getId(),
                        new StartRideRequest(ride.isGuest()))
                .enqueue(new Callback<>() {
                    @Override
                    public void onResponse(@NonNull Call<Void> call,
                                           @NonNull Response<Void> response) {
                        if (!isAdded() || assignedRidesStatus == null) return;
                        if (!response.isSuccessful()) {
                            button.setEnabled(true);
                            assignedRidesStatus.setText(startError(response));
                            assignedRidesStatus.setVisibility(View.VISIBLE);
                            return;
                        }
                        Toast.makeText(requireContext(), "Vožnja je započeta.",
                                Toast.LENGTH_SHORT).show();
                        ((MainActivity) requireActivity()).navigateToRideTracking(ride.getId());
                    }

                    @Override
                    public void onFailure(@NonNull Call<Void> call,
                                          @NonNull Throwable throwable) {
                        if (!isAdded() || assignedRidesStatus == null) return;
                        button.setEnabled(true);
                        assignedRidesStatus.setText(
                                "Backend nije dostupan: " + throwable.getMessage());
                        assignedRidesStatus.setVisibility(View.VISIBLE);
                    }
                });
    }

    private String startError(Response<?> response) {
        try {
            if (response.errorBody() != null) {
                JsonObject json = JsonParser.parseString(
                        response.errorBody().string()).getAsJsonObject();
                if (json.has("detail")) return json.get("detail").getAsString();
                if (json.has("message")) return json.get("message").getAsString();
            }
        } catch (Exception ignored) { }
        return "Početak vožnje nije uspeo (HTTP " + response.code() + ").";
    }

    private static String passengerNames(List<RidePassenger> passengers) {
        if (passengers.isEmpty()) return "nema evidentiranih putnika";
        StringBuilder value = new StringBuilder();
        for (RidePassenger passenger : passengers) {
            if (value.length() > 0) value.append(", ");
            value.append(safe(passenger.getName())).append(' ')
                    .append(safe(passenger.getSurname()));
        }
        return value.toString();
    }

    private static String displayTime(String value) {
        return value == null || value.isBlank() ? "—" : value.replace('T', ' ');
    }

    private static String safe(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
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
                boolean isActive = "ACTIVE".equals(status.getStatus());
                setActiveButton.setVisibility(isActive ? View.GONE : View.VISIBLE);
                setInactiveButton.setVisibility(isActive ? View.VISIBLE : View.GONE);
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

    @Override
    public void onResume() {
        super.onResume();
        loadAssignedRides();
    }

    @Override
    public void onDestroyView() {
        assignedRidesRequestInProgress = false;
        assignedRidesContainer = null;
        assignedRidesStatus = null;
        super.onDestroyView();
    }
}
