package com.example.mobilnaaplikacijatim29.ui.driver;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.mobilnaaplikacijatim29.R;
import com.example.mobilnaaplikacijatim29.data.api.ApiClient;
import com.example.mobilnaaplikacijatim29.data.model.DriverRideHistoryItem;
import com.example.mobilnaaplikacijatim29.data.model.InconsistencyReport;
import com.example.mobilnaaplikacijatim29.data.model.RideHistoryLocation;
import com.example.mobilnaaplikacijatim29.data.model.RidePassenger;
import com.example.mobilnaaplikacijatim29.data.model.RideReview;
import com.example.mobilnaaplikacijatim29.data.session.SessionManager;

import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase;
import org.osmdroid.tileprovider.tilesource.TileSourcePolicy;
import org.osmdroid.tileprovider.tilesource.XYTileSource;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;

import java.util.Arrays;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DriverRideDetailFragment extends Fragment {
    private static final String ARG_RIDE_ID = "ride_id";
    private static final String ARG_GUEST = "guest";
    private static final OnlineTileSourceBase OPEN_STREET_MAP = new XYTileSource(
            "OpenStreetMap", 0, 19, 256, ".png",
            new String[]{"https://tile.openstreetmap.org/"},
            "© OpenStreetMap contributors",
            new TileSourcePolicy(2, TileSourcePolicy.FLAG_NO_BULK
                    | TileSourcePolicy.FLAG_NO_PREVENTIVE
                    | TileSourcePolicy.FLAG_USER_AGENT_MEANINGFUL));

    private MapView map;
    private TextView message;
    private TextView basicInfo;
    private TextView cancellationInfo;
    private TextView passengersInfo;
    private TextView reportsInfo;
    private TextView reviewsInfo;

    public static DriverRideDetailFragment newInstance(long rideId, boolean guest) {
        Bundle arguments = new Bundle();
        arguments.putLong(ARG_RIDE_ID, rideId);
        arguments.putBoolean(ARG_GUEST, guest);
        DriverRideDetailFragment fragment = new DriverRideDetailFragment();
        fragment.setArguments(arguments);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_driver_ride_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        map = view.findViewById(R.id.ride_detail_map);
        message = view.findViewById(R.id.ride_detail_message);
        basicInfo = view.findViewById(R.id.ride_detail_basic);
        cancellationInfo = view.findViewById(R.id.ride_detail_cancellation);
        passengersInfo = view.findViewById(R.id.ride_detail_passengers);
        reportsInfo = view.findViewById(R.id.ride_detail_reports);
        reviewsInfo = view.findViewById(R.id.ride_detail_reviews);
        map.setTileSource(OPEN_STREET_MAP);
        map.setMultiTouchControls(true);
        map.setOnTouchListener((mapView, event) -> {
            int action = event.getActionMasked();
            mapView.getParent().requestDisallowInterceptTouchEvent(
                    action != MotionEvent.ACTION_UP && action != MotionEvent.ACTION_CANCEL);
            return false;
        });
        loadRide();
    }

    private void loadRide() {
        SessionManager session = new SessionManager(requireContext());
        long rideId = requireArguments().getLong(ARG_RIDE_ID);
        boolean guest = requireArguments().getBoolean(ARG_GUEST);
        ApiClient.getApi().getDriverRideHistoryDetail(session.getAuthorizationHeader(),
                        session.getUserId(), rideId, guest)
                .enqueue(new Callback<>() {
                    @Override
                    public void onResponse(@NonNull Call<DriverRideHistoryItem> call,
                                           @NonNull Response<DriverRideHistoryItem> response) {
                        if (!isAdded()) return;
                        if (!response.isSuccessful() || response.body() == null) {
                            showError("Detalji nisu učitani (HTTP " + response.code() + ").");
                            return;
                        }
                        showRide(response.body());
                    }

                    @Override
                    public void onFailure(@NonNull Call<DriverRideHistoryItem> call,
                                          @NonNull Throwable throwable) {
                        if (isAdded()) showError("Backend nije dostupan: " + throwable.getMessage());
                    }
                });
    }

    private void showRide(DriverRideHistoryItem ride) {
        message.setVisibility(View.GONE);
        basicInfo.setText("Početak: " + DriverRideHistoryFragment.displayDate(ride.getStartTime())
                + "\nKraj: " + DriverRideHistoryFragment.displayDate(ride.getEndTime())
                + "\nPolazište: " + address(ride.getOrigin())
                + "\nOdredište: " + address(ride.getDestination())
                + "\nCena: " + String.format(Locale.getDefault(), "%.2f RSD", ride.getTotalPrice())
                + "\nStatus: " + safe(ride.getStatus())
                + "\nPANIC pokrenut: " + yesNo(ride.isPanicPressed()));

        cancellationInfo.setText(ride.isCanceled()
                ? "Vožnja je otkazana.\nOtkazao/la: " + safe(ride.getCanceledBy())
                    + "\nRazlog: " + safe(ride.getCancellationReason())
                : "Vožnja nije otkazana.");
        passengersInfo.setText(passengerText(ride));
        reportsInfo.setText(reportText(ride));
        reviewsInfo.setText(reviewText(ride));
        showRoute(ride.getOrigin(), ride.getDestination());
    }

    private void showRoute(RideHistoryLocation origin, RideHistoryLocation destination) {
        map.getOverlays().clear();
        GeoPoint from = point(origin);
        GeoPoint to = point(destination);
        if (from == null || to == null) {
            map.setVisibility(View.GONE);
            return;
        }
        Marker originMarker = marker(from, "Polazište", address(origin));
        Marker destinationMarker = marker(to, "Odredište", address(destination));
        Polyline route = new Polyline();
        route.setPoints(Arrays.asList(from, to));
        route.setColor(Color.rgb(25, 118, 210));
        route.setWidth(8f);
        route.setTitle("Sačuvani pravac (polazište–odredište)");
        map.getOverlays().add(route);
        map.getOverlays().add(originMarker);
        map.getOverlays().add(destinationMarker);
        map.post(() -> {
            if (map != null) map.zoomToBoundingBox(
                    BoundingBox.fromGeoPoints(Arrays.asList(from, to)), true, 90);
        });
        map.invalidate();
    }

    private Marker marker(GeoPoint point, String title, String description) {
        Marker marker = new Marker(map);
        marker.setPosition(point);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        marker.setTitle(title);
        marker.setSnippet(description);
        return marker;
    }

    private String passengerText(DriverRideHistoryItem ride) {
        if (ride.getPassengers().isEmpty()) return "Nema evidentiranih putnika.";
        StringBuilder text = new StringBuilder();
        for (RidePassenger passenger : ride.getPassengers()) {
            if (text.length() > 0) text.append("\n\n");
            text.append(safe(passenger.getName())).append(' ')
                    .append(safe(passenger.getSurname()))
                    .append("\nE-mail: ").append(safe(passenger.getEmail()))
                    .append("\nTelefon: ").append(safe(passenger.getPhone()));
        }
        return text.toString();
    }

    private String reportText(DriverRideHistoryItem ride) {
        if (ride.getInconsistencyReports().isEmpty()) return "Nema prijava nekonzistentnosti.";
        StringBuilder text = new StringBuilder();
        for (InconsistencyReport report : ride.getInconsistencyReports()) {
            if (text.length() > 0) text.append("\n\n");
            text.append(safe(report.getPassengerEmail())).append(": ")
                    .append(safe(report.getNote())).append("\n")
                    .append(DriverRideHistoryFragment.displayDate(report.getCreatedAt()));
        }
        return text.toString();
    }

    private String reviewText(DriverRideHistoryItem ride) {
        if (ride.getReviews().isEmpty()) return "Nema ocena za ovu vožnju.";
        StringBuilder text = new StringBuilder();
        for (RideReview review : ride.getReviews()) {
            if (text.length() > 0) text.append("\n\n");
            text.append(safe(review.getPassengerEmail()))
                    .append(" — vozač ").append(review.getDriverRating()).append("/5, vozilo ")
                    .append(review.getVehicleRating()).append("/5")
                    .append("\n").append(safe(review.getComment()));
        }
        return text.toString();
    }

    private void showError(String value) {
        message.setText(value);
        message.setVisibility(View.VISIBLE);
    }

    private static GeoPoint point(RideHistoryLocation location) {
        if (location == null || location.getLatitude() == null || location.getLongitude() == null) {
            return null;
        }
        return new GeoPoint(location.getLatitude(), location.getLongitude());
    }
    private static String address(RideHistoryLocation location) {
        return location == null ? "—" : safe(location.getAddress());
    }
    private static String yesNo(boolean value) { return value ? "DA" : "NE"; }
    private static String safe(String value) {
        return value == null || value.trim().isEmpty() ? "—" : value;
    }

    @Override public void onResume() { super.onResume(); if (map != null) map.onResume(); }
    @Override public void onPause() { if (map != null) map.onPause(); super.onPause(); }
    @Override public void onDestroyView() { map = null; super.onDestroyView(); }
}
