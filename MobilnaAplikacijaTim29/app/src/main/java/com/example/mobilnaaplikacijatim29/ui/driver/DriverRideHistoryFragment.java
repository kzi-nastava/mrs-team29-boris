package com.example.mobilnaaplikacijatim29.ui.driver;

import android.app.DatePickerDialog;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.mobilnaaplikacijatim29.MainActivity;
import com.example.mobilnaaplikacijatim29.R;
import com.example.mobilnaaplikacijatim29.data.api.ApiClient;
import com.example.mobilnaaplikacijatim29.data.model.DriverRideHistoryItem;
import com.example.mobilnaaplikacijatim29.data.model.RideHistoryLocation;
import com.example.mobilnaaplikacijatim29.data.session.SessionManager;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DriverRideHistoryFragment extends Fragment implements SensorEventListener {
    private final List<DriverRideHistoryItem> rides = new ArrayList<>();
    private SessionManager session;
    private LinearLayout ridesContainer;
    private TextView message;
    private TextView fromDate;
    private TextView toDate;
    private TextView directionButton;
    private Spinner sortSpinner;
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private boolean ascending;
    private long lastShakeTime;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_driver_ride_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        session = new SessionManager(requireContext());
        ridesContainer = view.findViewById(R.id.ride_history_container);
        message = view.findViewById(R.id.ride_history_message);
        fromDate = view.findViewById(R.id.ride_history_from);
        toDate = view.findViewById(R.id.ride_history_to);
        directionButton = view.findViewById(R.id.ride_history_direction);
        sortSpinner = view.findViewById(R.id.ride_history_sort);

        String[] fields = {"Datum", "Ruta", "Početak", "Kraj", "Cena", "Status", "Otkazivanje"};
        sortSpinner.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, fields));
        sortSpinner.setSelection(0);
        directionButton.setText("Smer: najnovije prvo");

        fromDate.setOnClickListener(v -> selectDate(fromDate));
        toDate.setOnClickListener(v -> selectDate(toDate));
        view.findViewById(R.id.ride_history_apply).setOnClickListener(v -> loadHistory());
        view.findViewById(R.id.ride_history_clear).setOnClickListener(v -> {
            fromDate.setText("");
            toDate.setText("");
            loadHistory();
        });
        view.findViewById(R.id.ride_history_sort_apply).setOnClickListener(v -> renderRides());
        directionButton.setOnClickListener(v -> {
            ascending = !ascending;
            updateDirectionLabel();
            renderRides();
        });

        sensorManager = (SensorManager) requireContext().getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        loadHistory();
    }

    private void selectDate(TextView target) {
        Calendar now = Calendar.getInstance();
        new DatePickerDialog(requireContext(), (picker, year, month, day) ->
                target.setText(String.format(Locale.ROOT, "%04d-%02d-%02d", year, month + 1, day)),
                now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH))
                .show();
    }

    private void loadHistory() {
        message.setVisibility(View.VISIBLE);
        message.setText("Učitavanje istorije...");
        String from = textOrNull(fromDate);
        String to = textOrNull(toDate);
        ApiClient.getApi().getDriverRideHistory(session.getAuthorizationHeader(),
                        session.getUserId(), from, to)
                .enqueue(new Callback<>() {
                    @Override
                    public void onResponse(@NonNull Call<List<DriverRideHistoryItem>> call,
                                           @NonNull Response<List<DriverRideHistoryItem>> response) {
                        if (!isAdded()) return;
                        if (!response.isSuccessful() || response.body() == null) {
                            showError("Istorija nije učitana (HTTP " + response.code() + ").");
                            return;
                        }
                        rides.clear();
                        rides.addAll(response.body());
                        renderRides();
                    }

                    @Override
                    public void onFailure(@NonNull Call<List<DriverRideHistoryItem>> call,
                                          @NonNull Throwable throwable) {
                        if (isAdded()) showError("Backend nije dostupan: " + throwable.getMessage());
                    }
                });
    }

    private void renderRides() {
        if (ridesContainer == null) return;
        rides.sort(RideHistorySorter.comparator(
                String.valueOf(sortSpinner.getSelectedItem()), ascending));
        ridesContainer.removeAllViews();
        if (rides.isEmpty()) {
            message.setText("Nema vožnji za izabrani period.");
            message.setVisibility(View.VISIBLE);
            return;
        }
        message.setVisibility(View.GONE);
        for (DriverRideHistoryItem ride : rides) addRideCard(ride);
    }

    private void addRideCard(DriverRideHistoryItem ride) {
        MaterialCardView card = new MaterialCardView(requireContext());
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(0, 0, 0, dp(12));
        card.setLayoutParams(cardParams);
        card.setCardElevation(dp(2));
        card.setRadius(dp(12));
        card.setClickable(true);
        card.setFocusable(true);
        card.setContentPadding(dp(16), dp(14), dp(16), dp(14));

        TextView content = new TextView(requireContext());
        content.setText("Vožnja #" + ride.getId() + "\n"
                + address(ride.getOrigin()) + " → " + address(ride.getDestination()) + "\n"
                + "Početak: " + displayDate(ride.getStartTime()) + "\n"
                + "Kraj: " + displayDate(ride.getEndTime()) + "\n"
                + "Status: " + status(ride) + "  •  Cena: "
                + String.format(Locale.getDefault(), "%.2f RSD", ride.getTotalPrice()));
        content.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyLarge);
        card.addView(content);
        card.setOnClickListener(v -> ((MainActivity) requireActivity())
                .navigateToDriverRideDetail(ride.getId(), ride.isGuest()));
        ridesContainer.addView(card);
    }

    private String status(DriverRideHistoryItem ride) {
        if (!ride.isCanceled()) return safe(ride.getStatus());
        return "OTKAZANA" + (ride.getCanceledBy() == null ? "" : " (" + ride.getCanceledBy() + ")");
    }

    private void showError(String value) {
        ridesContainer.removeAllViews();
        message.setText(value);
        message.setVisibility(View.VISIBLE);
    }

    private void updateDirectionLabel() {
        directionButton.setText(ascending ? "Smer: rastuće" : "Smer: opadajuće");
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float x = event.values[0] / SensorManager.GRAVITY_EARTH;
        float y = event.values[1] / SensorManager.GRAVITY_EARTH;
        float z = event.values[2] / SensorManager.GRAVITY_EARTH;
        float force = (float) Math.sqrt(x * x + y * y + z * z);
        long now = System.currentTimeMillis();
        if (force > 2.7f && now - lastShakeTime > 1000L) {
            lastShakeTime = now;
            sortSpinner.setSelection(0);
            ascending = !ascending;
            updateDirectionLabel();
            renderRides();
            Toast.makeText(requireContext(), "Sortiranje po datumu je promenjeno.",
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) { }

    @Override
    public void onResume() {
        super.onResume();
        if (accelerometer != null) sensorManager.registerListener(
                this, accelerometer, SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    public void onPause() {
        if (sensorManager != null) sensorManager.unregisterListener(this);
        super.onPause();
    }

    private static String textOrNull(TextView view) {
        String value = view.getText().toString().trim();
        return value.isEmpty() ? null : value;
    }
    private static String address(RideHistoryLocation location) {
        return location == null ? "Nepoznata lokacija" : safe(location.getAddress());
    }
    static String displayDate(String value) {
        if (value == null || value.isEmpty()) return "—";
        return value.replace('T', ' ');
    }
    private static String safe(String value) { return value == null ? "—" : value; }
    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
