package com.example.mobilnaaplikacijatim29.ui.admin;

import android.os.Bundle;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.mobilnaaplikacijatim29.R;
import com.example.mobilnaaplikacijatim29.data.api.ApiClient;
import com.example.mobilnaaplikacijatim29.data.model.DriverRegistrationRequest;
import com.example.mobilnaaplikacijatim29.data.model.DriverRegistrationResponse;
import com.example.mobilnaaplikacijatim29.data.model.VehicleRegistrationRequest;
import com.example.mobilnaaplikacijatim29.data.session.SessionManager;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DriverRegistrationFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_driver_registration, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        Spinner gender = view.findViewById(R.id.driver_gender);
        Spinner type = view.findViewById(R.id.vehicle_type);
        gender.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"Muški", "Ženski"}));
        type.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"Standardno", "Luksuzno", "Kombi"}));

        TextView message = view.findViewById(R.id.driver_registration_message);
        View submit = view.findViewById(R.id.driver_registration_submit);
        submit.setOnClickListener(v -> {
            String name = text(view, R.id.driver_name);
            String surname = text(view, R.id.driver_surname);
            String email = text(view, R.id.driver_email);
            String address = text(view, R.id.driver_address);
            String phone = text(view, R.id.driver_phone);
            String model = text(view, R.id.vehicle_model);
            String registration = text(view, R.id.vehicle_registration).toUpperCase();
            String seatsText = text(view, R.id.vehicle_seats);

            if (name.isBlank() || surname.isBlank() || address.isBlank() || phone.isBlank()
                    || model.isBlank() || registration.isBlank() || seatsText.isBlank()
                    || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                show(message, "Popunite sva polja ispravnim podacima.");
                return;
            }
            int seats;
            try {
                seats = Integer.parseInt(seatsText);
            } catch (NumberFormatException exception) {
                show(message, "Broj mesta nije ispravan.");
                return;
            }
            if (seats < 4 || seats > 12) {
                show(message, "Broj mesta mora biti između 4 i 12.");
                return;
            }

            String genderValue = gender.getSelectedItemPosition() == 0 ? "MALE" : "FEMALE";
            String[] types = {"STANDARD", "LUXURY", "VAN"};
            MaterialSwitch baby = view.findViewById(R.id.vehicle_baby_friendly);
            MaterialSwitch pet = view.findViewById(R.id.vehicle_pet_friendly);
            VehicleRegistrationRequest vehicle = new VehicleRegistrationRequest(
                    model, types[type.getSelectedItemPosition()], registration, seats,
                    baby.isChecked(), pet.isChecked());
            DriverRegistrationRequest request = new DriverRegistrationRequest(
                    email, name, surname, genderValue, address, phone, vehicle);
            SessionManager session = new SessionManager(requireContext());
            submit.setEnabled(false);
            ApiClient.getApi().registerDriver(session.getAuthorizationHeader(), "mobile", request)
                    .enqueue(new Callback<>() {
                        @Override
                        public void onResponse(@NonNull Call<DriverRegistrationResponse> call,
                                               @NonNull Response<DriverRegistrationResponse> response) {
                            submit.setEnabled(true);
                            if (response.isSuccessful() && response.body() != null) {
                                show(message, "Vozač " + response.body().getEmail()
                                        + " je kreiran. Aktivacioni link je poslat emailom.");
                            } else {
                                show(message, "Kreiranje nije uspelo (HTTP "
                                        + response.code() + "). Proverite email, telefon i tablice.");
                            }
                        }

                        @Override
                        public void onFailure(@NonNull Call<DriverRegistrationResponse> call,
                                              @NonNull Throwable throwable) {
                            submit.setEnabled(true);
                            show(message, "Backend nije dostupan: " + throwable.getMessage());
                        }
                    });
        });
    }

    private String text(View view, int id) {
        TextInputEditText input = view.findViewById(id);
        return input.getText() == null ? "" : input.getText().toString().trim();
    }

    private void show(TextView message, String text) {
        message.setText(text);
        message.setVisibility(View.VISIBLE);
    }
}
