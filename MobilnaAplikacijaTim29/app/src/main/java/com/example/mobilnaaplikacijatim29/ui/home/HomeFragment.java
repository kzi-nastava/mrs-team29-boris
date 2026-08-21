package com.example.mobilnaaplikacijatim29.ui.home;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.fragment.app.Fragment;

import com.example.mobilnaaplikacijatim29.MainActivity;
import com.example.mobilnaaplikacijatim29.R;
import com.example.mobilnaaplikacijatim29.data.api.ApiClient;
import com.example.mobilnaaplikacijatim29.data.model.ActiveVehicleResponse;
import com.example.mobilnaaplikacijatim29.data.model.BookingLocation;
import com.example.mobilnaaplikacijatim29.data.model.CreateRideRequest;
import com.example.mobilnaaplikacijatim29.data.model.LocationResponse;
import com.example.mobilnaaplikacijatim29.data.model.RideBookingResponse;
import com.example.mobilnaaplikacijatim29.data.model.RoutePreviewRequest;
import com.example.mobilnaaplikacijatim29.data.model.RoutePreviewResponse;
import com.example.mobilnaaplikacijatim29.data.model.VehiclePriceConfig;
import com.example.mobilnaaplikacijatim29.data.session.SessionManager;
import com.example.mobilnaaplikacijatim29.domain.RideBookingCalculator;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase;
import org.osmdroid.tileprovider.tilesource.TileSourcePolicy;
import org.osmdroid.tileprovider.tilesource.XYTileSource;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.infowindow.MarkerInfoWindow;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeFragment extends Fragment {
    private static final GeoPoint NOVI_SAD = new GeoPoint(45.2671, 19.8335);
    private static final long VEHICLE_REFRESH_INTERVAL_MS = 3000L;
    private static final DateTimeFormatter API_TIME = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final OnlineTileSourceBase OPEN_STREET_MAP = new XYTileSource(
            "OpenStreetMap", 0, 19, 256, ".png",
            new String[]{"https://tile.openstreetmap.org/"},
            "© OpenStreetMap contributors",
            new TileSourcePolicy(2, TileSourcePolicy.FLAG_NO_BULK
                    | TileSourcePolicy.FLAG_NO_PREVENTIVE
                    | TileSourcePolicy.FLAG_USER_AGENT_MEANINGFUL));

    private enum SelectionMode { NONE, ORIGIN, STOP, DESTINATION }

    private MapView mapView;
    private TextView vehiclesStatus;
    private TextView bookingInstruction;
    private TextView routeSummary;
    private TextView estimate;
    private TextView bookingMessage;
    private Spinner vehicleType;
    private MaterialSwitch babySwitch;
    private MaterialSwitch petSwitch;
    private MaterialSwitch scheduleSwitch;
    private View pickTimeButton;
    private View submitButton;
    private SessionManager session;
    private VehiclePriceConfig prices;
    private BookingLocation origin;
    private BookingLocation destination;
    private final List<BookingLocation> stops = new ArrayList<>();
    private final List<Marker> vehicleMarkers = new ArrayList<>();
    private final List<Marker> bookingMarkers = new ArrayList<>();
    private final List<GeoPoint> roadGeometry = new ArrayList<>();
    private Polyline bookingLine;
    private Double roadDistanceKm;
    private Integer roadDurationMinutes;
    private boolean routePreviewReady;
    private int routeRevision;
    private SelectionMode selectionMode = SelectionMode.NONE;
    private LocalDateTime selectedTime;
    private final Handler refreshHandler = new Handler(Looper.getMainLooper());
    private boolean vehicleRequestInProgress;
    private boolean mapViewportInitialized;
    private boolean showVehicleList;
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
            map.getParent().requestDisallowInterceptTouchEvent(action != MotionEvent.ACTION_UP
                    && action != MotionEvent.ACTION_CANCEL);
            return false;
        });
        mapView.getOverlays().add(new MapEventsOverlay(new MapEventsReceiver() {
            @Override public boolean singleTapConfirmedHelper(GeoPoint point) {
                return selectBookingPoint(point);
            }
            @Override public boolean longPressHelper(GeoPoint point) { return false; }
        }));

        session = new SessionManager(requireContext());
        TextView sessionStatus = view.findViewById(R.id.session_status);
        vehiclesStatus = view.findViewById(R.id.vehicles_status);

        boolean passenger = session.isLoggedIn() && "user".equalsIgnoreCase(session.getRole());
        showVehicleList = !passenger;
        view.findViewById(R.id.home_intro_title)
                .setVisibility(passenger ? View.GONE : View.VISIBLE);
        view.findViewById(R.id.home_intro_subtitle)
                .setVisibility(passenger ? View.GONE : View.VISIBLE);
        sessionStatus.setVisibility(passenger ? View.GONE : View.VISIBLE);
        vehiclesStatus.setVisibility(showVehicleList ? View.VISIBLE : View.GONE);
        if (!passenger) sessionStatus.setText("Niste prijavljeni.");
        view.findViewById(R.id.ride_booking_section)
                .setVisibility(passenger ? View.VISIBLE : View.GONE);
        view.findViewById(R.id.passenger_reports_button)
                .setVisibility(passenger ? View.VISIBLE : View.GONE);
        if (passenger) setupBooking(view);
    }

    private void setupBooking(View view) {
        bookingInstruction = view.findViewById(R.id.booking_instruction);
        routeSummary = view.findViewById(R.id.booking_route_summary);
        estimate = view.findViewById(R.id.booking_estimate);
        bookingMessage = view.findViewById(R.id.booking_message);
        vehicleType = view.findViewById(R.id.booking_vehicle_type);
        babySwitch = view.findViewById(R.id.booking_baby);
        petSwitch = view.findViewById(R.id.booking_pet);
        scheduleSwitch = view.findViewById(R.id.booking_schedule_later);
        pickTimeButton = view.findViewById(R.id.booking_pick_time);
        submitButton = view.findViewById(R.id.booking_submit);
        view.findViewById(R.id.passenger_reports_button).setOnClickListener(v ->
                ((MainActivity) requireActivity()).navigateTo(R.id.nav_reports));
        vehicleType.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"Standardno", "Luksuzno", "Kombi"}));

        view.findViewById(R.id.booking_set_origin).setOnClickListener(v ->
                chooseMode(SelectionMode.ORIGIN, "Dodirni polazište na mapi."));
        view.findViewById(R.id.booking_add_stop).setOnClickListener(v -> {
            if (origin == null) {
                showBookingMessage("Prvo izaberi polazište.", true);
            } else chooseMode(SelectionMode.STOP,
                    "Dodirni sledeću stanicu na mapi. Redosled dodavanja se čuva.");
        });
        view.findViewById(R.id.booking_set_destination).setOnClickListener(v -> {
            if (origin == null) {
                showBookingMessage("Prvo izaberi polazište.", true);
            } else chooseMode(SelectionMode.DESTINATION, "Dodirni odredište na mapi.");
        });
        view.findViewById(R.id.booking_clear_route).setOnClickListener(v -> clearBookingRoute());
        scheduleSwitch.setOnCheckedChangeListener((button, checked) -> {
            pickTimeButton.setVisibility(checked ? View.VISIBLE : View.GONE);
            if (!checked) selectedTime = null;
        });
        pickTimeButton.setOnClickListener(v -> pickDateTime());
        view.findViewById(R.id.booking_demo_schedule).setOnClickListener(v -> {
            selectedTime = LocalDateTime.now().plusSeconds(70);
            scheduleSwitch.setChecked(true);
            ((TextView) pickTimeButton).setText("Demo zakazano: "
                    + selectedTime.format(DateTimeFormatter.ofPattern("HH:mm:ss")));
            showBookingMessage("Zakazaćete vožnju za 70 minuta.", false);
        });
        submitButton.setOnClickListener(v -> submitRide());
        submitButton.setEnabled(false);
        vehicleType.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> parent, View view,
                                                  int position, long id) { updateEstimate(); }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) { }
        });

        ApiClient.getApi().getBookingPrices(session.getAuthorizationHeader())
                .enqueue(new Callback<>() {
                    @Override public void onResponse(@NonNull Call<VehiclePriceConfig> call,
                                                     @NonNull Response<VehiclePriceConfig> response) {
                        if (isAdded() && response.isSuccessful()) {
                            prices = response.body();
                            updateEstimate();
                        }
                    }
                    @Override public void onFailure(@NonNull Call<VehiclePriceConfig> call,
                                                    @NonNull Throwable throwable) { }
                });
    }

    private void chooseMode(SelectionMode mode, String instruction) {
        selectionMode = mode;
        bookingInstruction.setText(instruction);
        showBookingMessage("", false);
    }

    private boolean selectBookingPoint(GeoPoint point) {
        if (selectionMode == SelectionMode.NONE || bookingInstruction == null) return false;
        BookingLocation location = new BookingLocation(point.getLongitude(), point.getLatitude(),
                String.format(Locale.US, "Tačka %.5f, %.5f",
                        point.getLatitude(), point.getLongitude()));
        if (selectionMode == SelectionMode.ORIGIN) origin = location;
        else if (selectionMode == SelectionMode.STOP) stops.add(location);
        else destination = location;
        selectionMode = SelectionMode.NONE;
        bookingInstruction.setText("Tačka je dodata. Izaberi sledeći korak.");
        bookingPointsChanged();
        return true;
    }

    private void bookingPointsChanged() {
        routeRevision++;
        roadGeometry.clear();
        roadDistanceKm = null;
        roadDurationMinutes = null;
        routePreviewReady = false;
        if (submitButton != null) submitButton.setEnabled(false);
        renderBookingRoute();
        if (origin != null && destination != null) loadRoadRoute(routeRevision);
    }

    private void renderBookingRoute() {
        mapView.getOverlays().removeAll(bookingMarkers);
        bookingMarkers.clear();
        if (bookingLine != null) mapView.getOverlays().remove(bookingLine);
        List<GeoPoint> waypointPoints = bookingPoints();
        for (int i = 0; i < waypointPoints.size(); i++) {
            boolean isOrigin = i == 0;
            boolean isDestination = destination != null && i == waypointPoints.size() - 1;
            Marker marker = new Marker(mapView);
            marker.setPosition(waypointPoints.get(i));
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            marker.setTitle(isOrigin ? "Polazište"
                    : isDestination ? "Odredište"
                    : "Stanica " + i);
            marker.setIcon(ContextCompat.getDrawable(requireContext(),
                    isOrigin ? R.drawable.ic_route_origin
                            : isDestination ? R.drawable.ic_route_destination
                            : R.drawable.ic_route_stop));
            bookingMarkers.add(marker);
            mapView.getOverlays().add(marker);
        }
        List<GeoPoint> displayedRoute = roadGeometry.isEmpty() ? waypointPoints : roadGeometry;
        if (displayedRoute.size() > 1) {
            bookingLine = new Polyline();
            bookingLine.setPoints(displayedRoute);
            bookingLine.setColor(Color.rgb(25, 118, 210));
            bookingLine.setWidth(8f);
            bookingLine.setTitle(roadGeometry.isEmpty()
                    ? "Izabrane tačke" : "Drumska ruta");
            mapView.getOverlays().add(bookingLine);
        }
        routeSummary.setText("Polazište: " + (origin == null ? "nije izabrano" : origin.getAddress())
                + "\nStanice: " + stops.size()
                + "\nOdredište: " + (destination == null ? "nije izabrano" : destination.getAddress()));
        updateEstimate();
        mapView.invalidate();
    }

    private List<GeoPoint> bookingPoints() {
        List<GeoPoint> points = new ArrayList<>();
        if (origin != null) points.add(point(origin));
        for (BookingLocation stop : stops) points.add(point(stop));
        if (destination != null) points.add(point(destination));
        return points;
    }

    private void clearBookingRoute() {
        origin = null;
        destination = null;
        stops.clear();
        selectionMode = SelectionMode.NONE;
        bookingPointsChanged();
        bookingInstruction.setText("Ruta je obrisana. Izaberi polazište i dodirni mapu.");
    }

    private void updateEstimate() {
        if (estimate == null || origin == null || destination == null) return;
        if (!routePreviewReady || roadDistanceKm == null || roadDurationMinutes == null) {
            estimate.setText("Računanje precizne drumske rute...");
            return;
        }
        double distance = roadDistanceKm;
        if (prices == null) {
            estimate.setText(String.format(Locale.getDefault(),
                    "Drumska udaljenost: %.2f km\nProcenjeno trajanje: %d min",
                    distance, roadDurationMinutes));
            return;
        }
        double base = vehicleType.getSelectedItemPosition() == 1 ? prices.getLuxuryBasePrice()
                : vehicleType.getSelectedItemPosition() == 2 ? prices.getVanBasePrice()
                : prices.getStandardBasePrice();
        double price = Math.round(RideBookingCalculator.price(
                base, prices.getPricePerKm(), distance));
        estimate.setText(String.format(Locale.getDefault(),
                "Drumska udaljenost: %.2f km\nProcenjeno trajanje: %d min"
                        + "\nCena: %.0f + %.2f × %.0f = %.0f RSD",
                distance, roadDurationMinutes, base, distance, prices.getPricePerKm(), price));
    }

    private void pickDateTime() {
        LocalDateTime initial = selectedTime == null
                ? LocalDateTime.now().plusMinutes(30) : selectedTime;
        new DatePickerDialog(requireContext(), (picker, year, month, day) ->
                new TimePickerDialog(requireContext(), (timePicker, hour, minute) -> {
                    selectedTime = LocalDateTime.of(year, month + 1, day, hour, minute);
                    ((TextView) pickTimeButton).setText("Zakazano: "
                            + selectedTime.format(DateTimeFormatter.ofPattern("dd.MM.yyyy. HH:mm")));
                }, initial.getHour(), initial.getMinute(), true).show(),
                initial.getYear(), initial.getMonthValue() - 1, initial.getDayOfMonth()).show();
    }

    private void submitRide() {
        if (origin == null || destination == null) {
            showBookingMessage("Izaberi polazište i odredište na mapi.", true);
            return;
        }
        if (!routePreviewReady || roadDistanceKm == null || roadDurationMinutes == null) {
            showBookingMessage("Sačekaj da se izračuna precizna drumska ruta.", true);
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime scheduled = RideBookingCalculator.requestedStart(
                now, scheduleSwitch.isChecked(), selectedTime);
        if (scheduled == null) {
            showBookingMessage("Izaberi vreme zakazane vožnje.", true);
            return;
        }
        if (!RideBookingCalculator.isAllowedSchedule(now, scheduled)) {
            showBookingMessage("Vožnja mora biti zakazana u narednih 5 sati.", true);
            return;
        }
        List<String> emails = passengerEmails();
        if (emails == null) return;
        double distance = roadDistanceKm;
        int duration = roadDurationMinutes;
        String[] types = {"STANDARD", "LUXURY", "VAN"};
        String selectedVehicleType = types[vehicleType.getSelectedItemPosition()];
        boolean babyRequested = babySwitch.isChecked();
        boolean petRequested = petSwitch.isChecked();
        CreateRideRequest request = new CreateRideRequest(origin, destination,
                new ArrayList<>(stops), emails, selectedVehicleType,
                scheduled.format(API_TIME), babyRequested, petRequested,
                duration, distance);
        submitButton.setEnabled(false);
        ApiClient.getApi().createRide(session.getAuthorizationHeader(), request)
                .enqueue(new Callback<>() {
                    @Override public void onResponse(@NonNull Call<RideBookingResponse> call,
                                                     @NonNull Response<RideBookingResponse> response) {
                        if (!isAdded()) return;
                        submitButton.setEnabled(true);
                        if (!response.isSuccessful() || response.body() == null) {
                            showBookingResult(false, errorMessage(response));
                            return;
                        }
                        RideBookingResponse ride = response.body();
                        if ("FAILED".equalsIgnoreCase(ride.getStatus())) {
                            String requirements = selectedVehicleType
                                    + (babyRequested ? ", prevoz bebe" : "")
                                    + (petRequested ? ", kućni ljubimac" : "");
                            showBookingResult(false,
                                    "Nema slobodnog vozača koji odgovara zahtevima: "
                                            + requirements + ". Obaveštenje je sačuvano.");
                        } else {
                            showBookingResult(true, String.format(Locale.getDefault(),
                                    "Vožnja #%d je prihvaćena. Cena: %.0f RSD. Vozač je obavešten.",
                                    ride.getId(), ride.getPrice()));
                        }
                    }
                    @Override public void onFailure(@NonNull Call<RideBookingResponse> call,
                                                    @NonNull Throwable throwable) {
                        if (!isAdded()) return;
                        submitButton.setEnabled(true);
                        showBookingResult(false,
                                "Backend nije dostupan: " + throwable.getMessage());
                    }
                });
    }

    private void showBookingResult(boolean successful, String message) {
        if (!isAdded()) return;
        new AlertDialog.Builder(requireContext())
                .setTitle(successful
                        ? "Vožnja je uspešno poručena"
                        : "Vožnja nije poručena")
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton("U redu", (dialog, which) -> {
                    if (isAdded()) {
                        ((MainActivity) requireActivity()).navigateTo(R.id.nav_dashboard);
                    }
                })
                .show();
    }

    private List<String> passengerEmails() {
        TextInputEditText input = requireView().findViewById(R.id.booking_passenger_emails);
        String raw = input.getText() == null ? "" : input.getText().toString().trim();
        List<String> emails = new ArrayList<>();
        if (raw.isEmpty()) return emails;
        for (String part : raw.split(",")) {
            String email = part.trim();
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                showBookingMessage("Neispravna email adresa putnika: " + email, true);
                return null;
            }
            if (!email.equalsIgnoreCase(session.getEmail()) && !emails.contains(email)) {
                emails.add(email);
            }
        }
        return emails;
    }

    private String errorMessage(Response<?> response) {
        try {
            if (response.errorBody() != null) {
                JsonObject json = JsonParser.parseString(response.errorBody().string()).getAsJsonObject();
                if (json.has("detail")) return json.get("detail").getAsString();
                if (json.has("message")) return json.get("message").getAsString();
            }
        } catch (Exception ignored) { }
        return response.code() == 403 ? "Nalog nema dozvolu za poručivanje vožnje."
                : "Poručivanje nije uspelo (HTTP " + response.code() + ").";
    }

    private void showBookingMessage(String value, boolean error) {
        if (bookingMessage == null) return;
        bookingMessage.setText(value);
        bookingMessage.setTextColor(requireContext().getColor(error
                ? android.R.color.holo_red_dark : android.R.color.holo_green_dark));
        bookingMessage.setVisibility(value.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void loadRoadRoute(int revision) {
        showBookingMessage("Računanje drumske rute...", false);
        ApiClient.getApi().previewRoute(session.getAuthorizationHeader(),
                        new RoutePreviewRequest(origin, new ArrayList<>(stops), destination))
                .enqueue(new Callback<>() {
                    @Override public void onResponse(@NonNull Call<RoutePreviewResponse> call,
                                                     @NonNull Response<RoutePreviewResponse> response) {
                        if (!isAdded() || revision != routeRevision) return;
                        if (!response.isSuccessful() || response.body() == null
                                || response.body().getGeometry() == null
                                || response.body().getGeometry().size() < 2) {
                            showBookingMessage(errorMessage(response), true);
                            updateEstimate();
                            return;
                        }
                        roadGeometry.clear();
                        for (BookingLocation point : response.body().getGeometry()) {
                            roadGeometry.add(new GeoPoint(point.getLatitude(), point.getLongitude()));
                        }
                        roadDistanceKm = response.body().getDistanceKm();
                        roadDurationMinutes = response.body().getDurationMinutes();
                        routePreviewReady = true;
                        submitButton.setEnabled(true);
                        showBookingMessage("Precizna drumska ruta je izračunata.", false);
                        renderBookingRoute();
                    }

                    @Override public void onFailure(@NonNull Call<RoutePreviewResponse> call,
                                                    @NonNull Throwable throwable) {
                        if (!isAdded() || revision != routeRevision) return;
                        showBookingMessage("Routing servis nije dostupan: "
                                + throwable.getMessage(), true);
                        updateEstimate();
                    }
                });
    }

    private GeoPoint point(BookingLocation location) {
        return new GeoPoint(location.getLatitude(), location.getLongitude());
    }

    private void loadActiveVehicles() {
        if (!isResumed() || vehicleRequestInProgress || vehiclesStatus == null) return;
        vehicleRequestInProgress = true;
        ApiClient.getApi().getActiveVehicles().enqueue(new Callback<>() {
            @Override public void onResponse(@NonNull Call<List<ActiveVehicleResponse>> call,
                                             @NonNull Response<List<ActiveVehicleResponse>> response) {
                vehicleRequestInProgress = false;
                if (vehiclesStatus == null || mapView == null) return;
                if (!response.isSuccessful() || response.body() == null) {
                    if (showVehicleList) vehiclesStatus.setText(
                            "Backend je odgovorio greškom: HTTP " + response.code());
                    scheduleVehicleRefresh();
                    return;
                }
                List<ActiveVehicleResponse> vehicles = response.body();
                showVehiclesOnMap(vehicles);
                if (!showVehicleList) {
                    // The passenger home keeps the live map without duplicating a textual list.
                } else if (vehicles.isEmpty()) {
                    vehiclesStatus.setText("Backend je povezan. Trenutno nema aktivnih vozila.");
                } else {
                    StringBuilder text = new StringBuilder("Aktivna vozila: ")
                            .append(vehicles.size()).append("\n");
                    for (ActiveVehicleResponse vehicle : vehicles) {
                        LocationResponse location = vehicle.getCurrentLocation();
                        text.append("• Vozilo #").append(vehicle.getId())
                                .append(" — ").append(driverName(vehicle));
                        if (!vehicle.isBusy()) {
                            text.append(" — slobodno");
                        }
                        if (!vehicle.isBusy() && location != null
                                && location.getAddress() != null) {
                            text.append(" — ").append(location.getAddress());
                        }
                        text.append("\n");
                    }
                    vehiclesStatus.setText(text.toString().trim());
                }
                scheduleVehicleRefresh();
            }
            @Override public void onFailure(@NonNull Call<List<ActiveVehicleResponse>> call,
                                            @NonNull Throwable throwable) {
                vehicleRequestInProgress = false;
                if (vehiclesStatus != null && showVehicleList) vehiclesStatus.setText(
                        "Povezivanje sa backendom nije uspelo:\n" + throwable.getMessage());
                scheduleVehicleRefresh();
            }
        });
    }

    private void scheduleVehicleRefresh() {
        refreshHandler.removeCallbacks(vehicleRefresh);
        if (isResumed()) refreshHandler.postDelayed(vehicleRefresh, VEHICLE_REFRESH_INTERVAL_MS);
    }

    private void showVehiclesOnMap(List<ActiveVehicleResponse> vehicles) {
        mapView.getOverlays().removeAll(vehicleMarkers);
        vehicleMarkers.clear();
        List<GeoPoint> positions = new ArrayList<>();
        MarkerInfoWindow vehicleInfoWindow = new MarkerInfoWindow(
                R.layout.vehicle_marker_info_window, mapView);
        for (ActiveVehicleResponse vehicle : vehicles) {
            LocationResponse location = vehicle.getCurrentLocation();
            if (location == null || location.getLatitude() == null || location.getLongitude() == null) continue;
            GeoPoint position = new GeoPoint(location.getLatitude(), location.getLongitude());
            positions.add(position);
            Marker marker = new Marker(mapView);
            marker.setPosition(position);
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            marker.setTitle("Vozilo #" + vehicle.getId());
            marker.setSnippet("<b>Vozač:</b> " + TextUtils.htmlEncode(driverName(vehicle))
                    + "<br>"
                    + (vehicle.isBusy() ? "Zauzeto" : "Slobodno"));
            marker.setInfoWindow(vehicleInfoWindow);
            marker.setIcon(createVehicleIcon(vehicle.isBusy()));
            vehicleMarkers.add(marker);
            mapView.getOverlays().add(marker);
        }
        if (!mapViewportInitialized && !positions.isEmpty()) {
            mapViewportInitialized = true;
            if (positions.size() == 1) {
                mapView.getController().setZoom(16.0);
                mapView.getController().animateTo(positions.get(0));
            } else {
                mapView.post(() -> {
                    if (mapView != null) mapView.zoomToBoundingBox(
                            BoundingBox.fromGeoPoints(positions), true, 80);
                });
            }
        }
        mapView.invalidate();
    }

    private static String driverName(ActiveVehicleResponse vehicle) {
        String value = vehicle.getDriverName();
        return value == null || value.isBlank() ? "Nepoznat vozač" : value;
    }

    private Drawable createVehicleIcon(boolean busy) {
        Drawable drawable = ContextCompat.getDrawable(requireContext(),
                R.drawable.ic_car_marker);
        if (drawable == null) return null;
        Drawable icon = DrawableCompat.wrap(drawable).mutate();
        DrawableCompat.setTint(icon, busy ? Color.rgb(198, 40, 40) : Color.rgb(46, 125, 50));
        return icon;
    }

    @Override public void onResume() {
        super.onResume();
        if (mapView != null) mapView.onResume();
        refreshHandler.removeCallbacks(vehicleRefresh);
        loadActiveVehicles();
    }

    @Override public void onPause() {
        refreshHandler.removeCallbacks(vehicleRefresh);
        if (mapView != null) mapView.onPause();
        super.onPause();
    }

    @Override public void onDestroyView() {
        refreshHandler.removeCallbacks(vehicleRefresh);
        vehiclesStatus = null;
        mapView = null;
        super.onDestroyView();
    }
}
