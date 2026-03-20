package com.example.mobilnaaplikacijatim29.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.mobilnaaplikacijatim29.MainActivity;
import com.example.mobilnaaplikacijatim29.R;
import com.example.mobilnaaplikacijatim29.data.api.ApiClient;
import com.example.mobilnaaplikacijatim29.data.model.ActiveVehicleResponse;
import com.example.mobilnaaplikacijatim29.data.model.LocationResponse;
import com.example.mobilnaaplikacijatim29.data.session.SessionManager;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        SessionManager sessionManager = new SessionManager(requireContext());
        TextView sessionStatus = view.findViewById(R.id.session_status);
        TextView vehiclesStatus = view.findViewById(R.id.vehicles_status);
        View loginButton = view.findViewById(R.id.login_button);
        View registerButton = view.findViewById(R.id.register_button);
        View logoutButton = view.findViewById(R.id.logout_button);

        if (sessionManager.isLoggedIn()) {
            sessionStatus.setText("Prijavljen korisnik: " + sessionManager.getEmail()
                    + " (" + sessionManager.getRole() + ")");
            loginButton.setVisibility(View.GONE);
            registerButton.setVisibility(View.GONE);
            logoutButton.setVisibility(View.VISIBLE);
        } else {
            sessionStatus.setText("Niste prijavljeni.");
        }

        loginButton.setOnClickListener(v ->
                ((MainActivity) requireActivity()).navigateTo(R.id.nav_login));
        registerButton.setOnClickListener(v ->
                ((MainActivity) requireActivity()).navigateTo(R.id.nav_register));
        logoutButton.setOnClickListener(v -> {
            sessionManager.clear();
            Toast.makeText(requireContext(), "Uspešno ste se odjavili.", Toast.LENGTH_SHORT).show();
            ((MainActivity) requireActivity()).navigateTo(R.id.nav_home);
        });

        loadActiveVehicles(vehiclesStatus);
    }

    private void loadActiveVehicles(TextView statusView) {
        ApiClient.getApi().getActiveVehicles().enqueue(new Callback<List<ActiveVehicleResponse>>() {
            @Override
            public void onResponse(@NonNull Call<List<ActiveVehicleResponse>> call,
                                   @NonNull Response<List<ActiveVehicleResponse>> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    statusView.setText("Backend je odgovorio greškom: HTTP " + response.code());
                    return;
                }

                List<ActiveVehicleResponse> vehicles = response.body();
                if (vehicles.isEmpty()) {
                    statusView.setText("Backend je povezan. Trenutno nema aktivnih vozila.");
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
                statusView.setText(text.toString().trim());
            }

            @Override
            public void onFailure(@NonNull Call<List<ActiveVehicleResponse>> call,
                                  @NonNull Throwable throwable) {
                statusView.setText("Povezivanje sa backendom nije uspelo:\n" + throwable.getMessage());
            }
        });
    }
}
