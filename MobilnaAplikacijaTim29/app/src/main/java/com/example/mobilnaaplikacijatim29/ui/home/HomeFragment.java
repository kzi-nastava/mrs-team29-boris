package com.example.mobilnaaplikacijatim29.ui.home;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.fragment.app.Fragment;

import com.example.mobilnaaplikacijatim29.MainActivity;
import com.example.mobilnaaplikacijatim29.R;
import com.example.mobilnaaplikacijatim29.data.api.ApiClient;
import com.example.mobilnaaplikacijatim29.data.model.ActiveVehicleResponse;
import com.example.mobilnaaplikacijatim29.data.model.LocationResponse;
import com.example.mobilnaaplikacijatim29.data.session.SessionManager;

import java.util.List;
import java.util.ArrayList;

import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase;
import org.osmdroid.tileprovider.tilesource.TileSourcePolicy;
import org.osmdroid.tileprovider.tilesource.XYTileSource;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeFragment extends Fragment {

    private static final GeoPoint NOVI_SAD = new GeoPoint(45.2671, 19.8335);
    private static final long VEHICLE_REFRESH_INTERVAL_MS = 3000L;
    private static final OnlineTileSourceBase OPEN_STREET_MAP = new XYTileSource(
            "OpenStreetMap",
            0,
            19,
            256,
            ".png",
            new String[]{"https://tile.openstreetmap.org/"},
            "© OpenStreetMap contributors",
            new TileSourcePolicy(
                    2,
                    TileSourcePolicy.FLAG_NO_BULK
                            | TileSourcePolicy.FLAG_NO_PREVENTIVE
                            | TileSourcePolicy.FLAG_USER_AGENT_MEANINGFUL
            )
    );
    private MapView mapView;
    private TextView vehiclesStatus;
    private final Handler refreshHandler = new Handler(Looper.getMainLooper());
    private boolean vehicleRequestInProgress;
    private boolean mapViewportInitialized;
    private final Runnable vehicleRefresh = this::loadActiveVehicles;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        mapView = view.findViewById(R.id.vehicles_map);
        mapView.setTileSource(OPEN_STREET_MAP);
        mapView.setMultiTouchControls(true);
        mapView.getController().setZoom(13.0);
        mapView.getController().setCenter(NOVI_SAD);
        mapViewportInitialized = false;
        mapView.setOnTouchListener((map, event) -> {
            int action = event.getActionMasked();
            boolean mapGestureInProgress = action != MotionEvent.ACTION_UP
                    && action != MotionEvent.ACTION_CANCEL;
            map.getParent().requestDisallowInterceptTouchEvent(mapGestureInProgress);
            return false;
        });

        SessionManager sessionManager = new SessionManager(requireContext());
        TextView sessionStatus = view.findViewById(R.id.session_status);
        vehiclesStatus = view.findViewById(R.id.vehicles_status);
        View loginButton = view.findViewById(R.id.login_button);
        View logoutButton = view.findViewById(R.id.logout_button);

        if (sessionManager.isLoggedIn()) {
            sessionStatus.setText("Prijavljen korisnik: " + sessionManager.getEmail()
                    + " (" + sessionManager.getRole() + ")");
            loginButton.setVisibility(View.GONE);
            logoutButton.setVisibility(View.VISIBLE);
        } else {
            sessionStatus.setText("Niste prijavljeni.");
        }

        loginButton.setOnClickListener(v ->
                ((MainActivity) requireActivity()).navigateTo(R.id.nav_login));
        logoutButton.setOnClickListener(v -> {
            ((MainActivity) requireActivity()).requestLogout(message ->
                    Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show());
        });

    }

    private void loadActiveVehicles() {
        if (!isResumed() || vehicleRequestInProgress || vehiclesStatus == null) {
            return;
        }
        vehicleRequestInProgress = true;

        ApiClient.getApi().getActiveVehicles().enqueue(new Callback<List<ActiveVehicleResponse>>() {
            @Override
            public void onResponse(@NonNull Call<List<ActiveVehicleResponse>> call,
                                   @NonNull Response<List<ActiveVehicleResponse>> response) {
                vehicleRequestInProgress = false;
                if (vehiclesStatus == null || mapView == null) {
                    return;
                }
                if (!response.isSuccessful() || response.body() == null) {
                    vehiclesStatus.setText("Backend je odgovorio greškom: HTTP " + response.code());
                    scheduleVehicleRefresh();
                    return;
                }

                List<ActiveVehicleResponse> vehicles = response.body();
                showVehiclesOnMap(vehicles);
                if (vehicles.isEmpty()) {
                    vehiclesStatus.setText("Backend je povezan. Trenutno nema aktivnih vozila.");
                    scheduleVehicleRefresh();
                    return;
                }

                StringBuilder text = new StringBuilder("Aktivna vozila: ")
                        .append(vehicles.size()).append("\n");
                for (ActiveVehicleResponse vehicle : vehicles) {
                    LocationResponse location = vehicle.getCurrentLocation();
                    text.append("• Vozilo #").append(vehicle.getId())
                            .append(vehicle.isBusy() ? " — zauzeto" : " — slobodno");
                    if (location != null && location.getAddress() != null) {
                        text.append(" — ").append(location.getAddress());
                    }
                    text.append("\n");
                }
                vehiclesStatus.setText(text.toString().trim());
                scheduleVehicleRefresh();
            }

            @Override
            public void onFailure(@NonNull Call<List<ActiveVehicleResponse>> call,
                                  @NonNull Throwable throwable) {
                vehicleRequestInProgress = false;
                if (vehiclesStatus != null) {
                    vehiclesStatus.setText("Povezivanje sa backendom nije uspelo:\n"
                            + throwable.getMessage());
                }
                scheduleVehicleRefresh();
            }
        });
    }

    private void scheduleVehicleRefresh() {
        refreshHandler.removeCallbacks(vehicleRefresh);
        if (isResumed()) {
            refreshHandler.postDelayed(vehicleRefresh, VEHICLE_REFRESH_INTERVAL_MS);
        }
    }

    private void showVehiclesOnMap(List<ActiveVehicleResponse> vehicles) {
        mapView.getOverlays().clear();
        List<GeoPoint> positions = new ArrayList<>();

        for (ActiveVehicleResponse vehicle : vehicles) {
            LocationResponse location = vehicle.getCurrentLocation();
            if (location == null || location.getLatitude() == null
                    || location.getLongitude() == null) {
                continue;
            }

            GeoPoint position = new GeoPoint(location.getLatitude(), location.getLongitude());
            positions.add(position);

            Marker marker = new Marker(mapView);
            marker.setPosition(position);
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            marker.setTitle("Vozilo #" + vehicle.getId());
            marker.setSnippet(vehicle.isBusy() ? "Zauzeto" : "Slobodno");
            marker.setIcon(createVehicleIcon(vehicle.isBusy()));
            mapView.getOverlays().add(marker);
        }

        if (!mapViewportInitialized && !positions.isEmpty()) {
            mapViewportInitialized = true;
            if (positions.size() == 1) {
                mapView.getController().setZoom(16.0);
                mapView.getController().animateTo(positions.get(0));
            } else {
                mapView.post(() -> {
                    if (mapView != null) {
                        mapView.zoomToBoundingBox(
                                BoundingBox.fromGeoPoints(positions), true, 80);
                    }
                });
            }
        }
        mapView.invalidate();
    }

    private Drawable createVehicleIcon(boolean busy) {
        Drawable drawable = ContextCompat.getDrawable(
                requireContext(), android.R.drawable.ic_menu_mylocation);
        if (drawable == null) {
            return null;
        }
        Drawable icon = DrawableCompat.wrap(drawable).mutate();
        DrawableCompat.setTint(icon, busy ? Color.rgb(198, 40, 40) : Color.rgb(46, 125, 50));
        return icon;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mapView != null) {
            mapView.onResume();
        }
        refreshHandler.removeCallbacks(vehicleRefresh);
        loadActiveVehicles();
    }

    @Override
    public void onPause() {
        refreshHandler.removeCallbacks(vehicleRefresh);
        if (mapView != null) {
            mapView.onPause();
        }
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        refreshHandler.removeCallbacks(vehicleRefresh);
        vehiclesStatus = null;
        mapView = null;
        super.onDestroyView();
    }
}
