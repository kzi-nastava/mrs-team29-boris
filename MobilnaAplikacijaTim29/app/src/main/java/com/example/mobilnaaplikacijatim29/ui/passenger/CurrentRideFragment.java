package com.example.mobilnaaplikacijatim29.ui.passenger;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.fragment.app.Fragment;

import com.example.mobilnaaplikacijatim29.R;
import com.example.mobilnaaplikacijatim29.data.api.ApiClient;
import com.example.mobilnaaplikacijatim29.data.model.LocationResponse;
import com.example.mobilnaaplikacijatim29.data.model.RideTrackingResponse;
import com.example.mobilnaaplikacijatim29.data.session.SessionManager;

import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase;
import org.osmdroid.tileprovider.tilesource.TileSourcePolicy;
import org.osmdroid.tileprovider.tilesource.XYTileSource;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CurrentRideFragment extends Fragment {
    private static final String ARG_RIDE_ID = "ride_id";
    private static final long REFRESH_MS = 3000L;
    private static final OnlineTileSourceBase OPEN_STREET_MAP = new XYTileSource(
            "OpenStreetMap", 0, 19, 256, ".png",
            new String[]{"https://tile.openstreetmap.org/"},
            "© OpenStreetMap contributors",
            new TileSourcePolicy(2, TileSourcePolicy.FLAG_NO_BULK
                    | TileSourcePolicy.FLAG_NO_PREVENTIVE
                    | TileSourcePolicy.FLAG_USER_AGENT_MEANINGFUL));

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable refresh = this::loadTracking;
    private MapView map;
    private TextView title;
    private TextView status;
    private ProgressBar progress;
    private Marker vehicleMarker;
    private Polyline routeLine;
    private SessionManager session;
    private boolean requestInProgress;
    private boolean viewportInitialized;

    public static CurrentRideFragment newInstance(long rideId) {
        CurrentRideFragment fragment = new CurrentRideFragment();
        Bundle arguments = new Bundle();
        arguments.putLong(ARG_RIDE_ID, rideId);
        fragment.setArguments(arguments);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_current_ride, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        session = new SessionManager(requireContext());
        map = view.findViewById(R.id.current_ride_map);
        title = view.findViewById(R.id.current_ride_title);
        status = view.findViewById(R.id.current_ride_status);
        progress = view.findViewById(R.id.current_ride_progress);
        long rideId = requireArguments().getLong(ARG_RIDE_ID);
        title.setText("Vožnja #" + rideId);
        map.setTileSource(OPEN_STREET_MAP);
        map.setMultiTouchControls(true);
        map.setOnTouchListener((mapView, event) -> {
            int action = event.getActionMasked();
            mapView.getParent().requestDisallowInterceptTouchEvent(
                    action != MotionEvent.ACTION_UP && action != MotionEvent.ACTION_CANCEL);
            return false;
        });
    }

    private void loadTracking() {
        if (!isResumed() || requestInProgress) return;
        requestInProgress = true;
        long rideId = requireArguments().getLong(ARG_RIDE_ID);
        ApiClient.getApi().getRideTracking(session.getAuthorizationHeader(), rideId)
                .enqueue(new Callback<>() {
                    @Override public void onResponse(@NonNull Call<RideTrackingResponse> call,
                                                     @NonNull Response<RideTrackingResponse> response) {
                        requestInProgress = false;
                        if (!isAdded()) return;
                        if (!response.isSuccessful() || response.body() == null) {
                            status.setText(response.code() == 403
                                    ? "Nemate pristup ovoj vožnji."
                                    : "Praćenje nije dostupno (HTTP " + response.code() + ").");
                            schedule();
                            return;
                        }
                        render(response.body());
                    }

                    @Override public void onFailure(@NonNull Call<RideTrackingResponse> call,
                                                    @NonNull Throwable throwable) {
                        requestInProgress = false;
                        if (isAdded()) {
                            status.setText("Backend nije dostupan: " + throwable.getMessage());
                            schedule();
                        }
                    }
                });
    }

    private void render(RideTrackingResponse value) {
        String state = translatedStatus(value.getStatus());
        status.setText(String.format(Locale.getDefault(),
                "Status: %s\nProcenjeno preostalo vreme: %d min\nNapredak: %.0f%%",
                state, value.getEstimatedTimeInMinutes(), value.getProgressPercent()));
        progress.setProgress((int) Math.round(value.getProgressPercent()));
        renderRoute(value.getRouteGeometry());
        renderVehicle(value.getVehicleLocation());
        if (!isFinished(value.getStatus())) schedule();
    }

    private void renderRoute(List<LocationResponse> geometry) {
        if (routeLine != null || geometry.size() < 2) return;
        List<GeoPoint> points = new ArrayList<>();
        for (LocationResponse location : geometry) {
            if (location.getLatitude() != null && location.getLongitude() != null) {
                points.add(new GeoPoint(location.getLatitude(), location.getLongitude()));
            }
        }
        if (points.size() < 2) return;
        routeLine = new Polyline();
        routeLine.setPoints(points);
        routeLine.setColor(Color.rgb(25, 118, 210));
        routeLine.setWidth(9f);
        routeLine.setTitle("Drumska ruta vožnje");
        map.getOverlays().add(routeLine);
        map.getOverlays().add(marker(points.get(0), "Polazište"));
        map.getOverlays().add(marker(points.get(points.size() - 1), "Odredište"));
        if (!viewportInitialized) {
            viewportInitialized = true;
            map.post(() -> {
                if (map != null) map.zoomToBoundingBox(
                        BoundingBox.fromGeoPoints(points), true, 80);
            });
        }
    }

    private void renderVehicle(LocationResponse location) {
        if (location == null || location.getLatitude() == null || location.getLongitude() == null) {
            return;
        }
        GeoPoint point = new GeoPoint(location.getLatitude(), location.getLongitude());
        if (vehicleMarker == null) {
            vehicleMarker = marker(point, "Trenutni položaj vozila");
            vehicleMarker.setIcon(carIcon());
            map.getOverlays().add(vehicleMarker);
        } else {
            vehicleMarker.setPosition(point);
        }
        map.invalidate();
    }

    private Marker marker(GeoPoint point, String markerTitle) {
        Marker marker = new Marker(map);
        marker.setPosition(point);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        marker.setTitle(markerTitle);
        return marker;
    }

    private Drawable carIcon() {
        Drawable drawable = ContextCompat.getDrawable(requireContext(), R.drawable.ic_car_marker);
        if (drawable == null) return null;
        Drawable icon = DrawableCompat.wrap(drawable).mutate();
        DrawableCompat.setTint(icon, Color.rgb(198, 40, 40));
        return icon;
    }

    private void schedule() {
        handler.removeCallbacks(refresh);
        if (isResumed()) handler.postDelayed(refresh, REFRESH_MS);
    }

    private static boolean isFinished(String value) {
        return "FINISHED".equals(value) || "STOPPED".equals(value)
                || "CANCELED".equals(value) || "FAILED".equals(value);
    }

    private static String translatedStatus(String value) {
        if ("SCHEDULED".equals(value)) return "zakazana";
        if ("STARTED".equals(value)) return "u toku";
        if ("FINISHED".equals(value)) return "završena";
        if ("STOPPED".equals(value)) return "zaustavljena";
        if ("CANCELED".equals(value)) return "otkazana";
        if ("FAILED".equals(value)) return "nije prihvaćena";
        return value == null ? "nepoznat" : value;
    }

    @Override public void onResume() {
        super.onResume();
        if (map != null) map.onResume();
        loadTracking();
    }

    @Override public void onPause() {
        handler.removeCallbacks(refresh);
        if (map != null) map.onPause();
        super.onPause();
    }

    @Override public void onDestroyView() {
        handler.removeCallbacks(refresh);
        map = null;
        super.onDestroyView();
    }
}
