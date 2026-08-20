package com.example.mobilnaaplikacijatim29.ui.passenger;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputFilter;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AlertDialog;

import com.example.mobilnaaplikacijatim29.MainActivity;
import com.example.mobilnaaplikacijatim29.R;
import com.example.mobilnaaplikacijatim29.data.api.ApiClient;
import com.example.mobilnaaplikacijatim29.data.model.LocationResponse;
import com.example.mobilnaaplikacijatim29.data.model.RideReviewRequest;
import com.example.mobilnaaplikacijatim29.data.model.RideTrackingResponse;
import com.example.mobilnaaplikacijatim29.data.session.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

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
    private TextView actionMessage;
    private MaterialButton finishButton;
    private MaterialButton reviewButton;
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
        actionMessage = view.findViewById(R.id.current_ride_action_message);
        finishButton = view.findViewById(R.id.current_ride_finish_button);
        reviewButton = view.findViewById(R.id.current_ride_review_button);
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
        finishButton.setOnClickListener(v -> confirmFinishRide(rideId));
        reviewButton.setOnClickListener(v -> showReviewDialog(rideId));
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
        renderRoute(value);
        renderVehicle(value.getVehicleLocation());
        renderActions(value);
        if (!isFinished(value.getStatus())) schedule();
    }

    private void renderActions(RideTrackingResponse value) {
        boolean driver = "driver".equalsIgnoreCase(session.getRole());
        boolean passenger = "user".equalsIgnoreCase(session.getRole());
        boolean started = "STARTED".equals(value.getStatus());
        boolean destinationReached = value.getProgressPercent() >= 99.9;
        finishButton.setVisibility(driver && started && destinationReached
                ? View.VISIBLE : View.GONE);
        reviewButton.setVisibility(passenger && value.canReview()
                ? View.VISIBLE : View.GONE);

        if (driver && started && !destinationReached) {
            actionMessage.setText("Završetak vožnje biće dostupan kada vozilo stigne "
                    + "na odredište.");
            actionMessage.setVisibility(View.VISIBLE);
        } else if ("FINISHED".equals(value.getStatus())) {
            String text = String.format(Locale.getDefault(),
                    "Vožnja je plaćena. Cena: %.0f RSD", value.getPrice());
            if (passenger && value.isAlreadyReviewed()) {
                text += "\nOvu vožnju ste već ocenili.";
            } else if (passenger && value.canReview()) {
                text += "\nOcenu možete ostaviti do "
                        + displayDateTime(value.getReviewDeadline()) + ".";
            }
            actionMessage.setText(text);
            actionMessage.setVisibility(View.VISIBLE);
        } else {
            actionMessage.setVisibility(View.GONE);
        }
    }

    private void confirmFinishRide(long rideId) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Završetak vožnje")
                .setMessage("Potvrdite da su svi putnici izašli i da je vožnja plaćena u vozilu.")
                .setNegativeButton("Odustani", null)
                .setPositiveButton("Završi vožnju", (dialog, which) -> finishRide(rideId))
                .show();
    }

    private void finishRide(long rideId) {
        finishButton.setEnabled(false);
        ApiClient.getApi().finishRide(session.getAuthorizationHeader(), rideId, false)
                .enqueue(new Callback<>() {
                    @Override public void onResponse(@NonNull Call<Void> call,
                                                     @NonNull Response<Void> response) {
                        if (!isAdded() || finishButton == null) return;
                        finishButton.setEnabled(true);
                        if (!response.isSuccessful()) {
                            actionMessage.setText(errorMessage(response,
                                    "Završetak vožnje nije uspeo"));
                            actionMessage.setVisibility(View.VISIBLE);
                            return;
                        }
                        Toast.makeText(requireContext(),
                                "Vožnja je završena i evidentirana kao plaćena.",
                                Toast.LENGTH_LONG).show();
                        ((MainActivity) requireActivity()).navigateTo(R.id.nav_dashboard);
                    }

                    @Override public void onFailure(@NonNull Call<Void> call,
                                                    @NonNull Throwable throwable) {
                        if (!isAdded() || finishButton == null) return;
                        finishButton.setEnabled(true);
                        actionMessage.setText("Backend nije dostupan: " + throwable.getMessage());
                        actionMessage.setVisibility(View.VISIBLE);
                    }
                });
    }

    private void showReviewDialog(long rideId) {
        LinearLayout form = new LinearLayout(requireContext());
        form.setOrientation(LinearLayout.VERTICAL);
        int padding = dp(20);
        form.setPadding(padding, 0, padding, 0);

        TextView driverLabel = new TextView(requireContext());
        driverLabel.setText("Ocena vozača");
        form.addView(driverLabel);
        RatingBar driverRating = ratingBar();
        form.addView(driverRating);

        TextView vehicleLabel = new TextView(requireContext());
        vehicleLabel.setText("Ocena vozila");
        form.addView(vehicleLabel);
        RatingBar vehicleRating = ratingBar();
        form.addView(vehicleRating);

        EditText comment = new EditText(requireContext());
        comment.setHint("Komentar (opciono, najviše 200 karaktera)");
        comment.setFilters(new InputFilter[]{new InputFilter.LengthFilter(200)});
        form.addView(comment);

        new AlertDialog.Builder(requireContext())
                .setTitle("Oceni vožnju")
                .setView(form)
                .setNegativeButton("Kasnije", null)
                .setPositiveButton("Sačuvaj", (dialog, which) -> submitReview(
                        rideId, Math.round(driverRating.getRating()),
                        Math.round(vehicleRating.getRating()),
                        comment.getText().toString().trim()))
                .show();
    }

    private RatingBar ratingBar() {
        RatingBar ratingBar = new RatingBar(requireContext(), null,
                android.R.attr.ratingBarStyle);
        ratingBar.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        ratingBar.setNumStars(5);
        ratingBar.setStepSize(1f);
        ratingBar.setRating(5f);
        ratingBar.setIsIndicator(false);
        return ratingBar;
    }

    private void submitReview(long rideId, int driverRating, int vehicleRating,
                              String comment) {
        reviewButton.setEnabled(false);
        ApiClient.getApi().reviewRide(session.getAuthorizationHeader(), rideId,
                        new RideReviewRequest(driverRating, vehicleRating, comment))
                .enqueue(new Callback<>() {
                    @Override public void onResponse(@NonNull Call<Void> call,
                                                     @NonNull Response<Void> response) {
                        if (!isAdded() || reviewButton == null) return;
                        reviewButton.setEnabled(true);
                        if (!response.isSuccessful()) {
                            actionMessage.setText(errorMessage(response,
                                    "Ocenjivanje nije uspelo"));
                            actionMessage.setVisibility(View.VISIBLE);
                            return;
                        }
                        Toast.makeText(requireContext(), "Ocena je sačuvana.",
                                Toast.LENGTH_SHORT).show();
                        loadTracking();
                    }

                    @Override public void onFailure(@NonNull Call<Void> call,
                                                    @NonNull Throwable throwable) {
                        if (!isAdded() || reviewButton == null) return;
                        reviewButton.setEnabled(true);
                        actionMessage.setText("Backend nije dostupan: " + throwable.getMessage());
                        actionMessage.setVisibility(View.VISIBLE);
                    }
                });
    }

    private static String errorMessage(Response<?> response, String fallback) {
        try {
            if (response.errorBody() != null) {
                JsonObject json = JsonParser.parseString(
                        response.errorBody().string()).getAsJsonObject();
                if (json.has("detail")) return json.get("detail").getAsString();
                if (json.has("message")) return json.get("message").getAsString();
            }
        } catch (Exception ignored) { }
        return fallback + " (HTTP " + response.code() + ").";
    }

    private static String displayDateTime(String value) {
        if (value == null || value.isBlank()) return "—";
        String normalized = value.replace('T', ' ');
        return normalized.length() > 16 ? normalized.substring(0, 16) : normalized;
    }

    private void renderRoute(RideTrackingResponse value) {
        List<LocationResponse> geometry = value.getRouteGeometry();
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
        Marker originMarker = marker(pointOrFallback(value.getOrigin(), points.get(0)),
                locationTitle("Polazište", value.getOrigin()));
        originMarker.setIcon(ContextCompat.getDrawable(requireContext(),
                R.drawable.ic_route_origin));
        map.getOverlays().add(originMarker);

        for (int i = 0; i < value.getStops().size(); i++) {
            LocationResponse stop = value.getStops().get(i);
            GeoPoint stopPoint = point(stop);
            if (stopPoint == null) continue;
            Marker stopMarker = marker(stopPoint, locationTitle("Stanica " + (i + 1), stop));
            stopMarker.setIcon(ContextCompat.getDrawable(requireContext(),
                    R.drawable.ic_route_stop));
            map.getOverlays().add(stopMarker);
        }

        Marker destinationMarker = marker(
                pointOrFallback(value.getDestination(), points.get(points.size() - 1)),
                locationTitle("Odredište", value.getDestination()));
        destinationMarker.setIcon(ContextCompat.getDrawable(requireContext(),
                R.drawable.ic_route_destination));
        map.getOverlays().add(destinationMarker);
        if (!viewportInitialized) {
            viewportInitialized = true;
            map.post(() -> {
                if (map != null) map.zoomToBoundingBox(
                        BoundingBox.fromGeoPoints(points), true, 80);
            });
        }
    }

    private static GeoPoint pointOrFallback(LocationResponse location, GeoPoint fallback) {
        GeoPoint point = point(location);
        return point == null ? fallback : point;
    }

    private static GeoPoint point(LocationResponse location) {
        if (location == null || location.getLatitude() == null
                || location.getLongitude() == null) return null;
        return new GeoPoint(location.getLatitude(), location.getLongitude());
    }

    private static String locationTitle(String label, LocationResponse location) {
        if (location == null || location.getAddress() == null
                || location.getAddress().isBlank()) return label;
        return label + ": " + location.getAddress();
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

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
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
        actionMessage = null;
        finishButton = null;
        reviewButton = null;
        super.onDestroyView();
    }
}
