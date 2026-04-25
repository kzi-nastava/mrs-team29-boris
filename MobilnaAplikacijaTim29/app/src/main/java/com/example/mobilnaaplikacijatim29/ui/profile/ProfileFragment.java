package com.example.mobilnaaplikacijatim29.ui.profile;

import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Patterns;
import android.view.*;
import android.widget.*;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.mobilnaaplikacijatim29.R;
import com.example.mobilnaaplikacijatim29.data.api.ApiClient;
import com.example.mobilnaaplikacijatim29.data.model.*;
import com.example.mobilnaaplikacijatim29.data.session.SessionManager;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;

import java.io.InputStream;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileFragment extends Fragment {
    private View root;
    private SessionManager session;
    private ProfileResponse profile;
    private Spinner gender;
    private Spinner vehicleType;
    private TextView message;
    private ImageView image;

    private final ActivityResultLauncher<String> imagePicker = registerForActivityResult(
            new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) uploadImage(uri);
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        root = view;
        session = new SessionManager(requireContext());
        message = view.findViewById(R.id.profile_message);
        image = view.findViewById(R.id.profile_image);
        gender = view.findViewById(R.id.profile_gender);
        vehicleType = view.findViewById(R.id.profile_vehicle_type);
        gender.setAdapter(adapter(new String[]{"Muški", "Ženski"}));
        vehicleType.setAdapter(adapter(new String[]{"Standardno", "Luksuzno", "Kombi"}));
        view.findViewById(R.id.profile_save).setOnClickListener(v -> saveProfile());
        view.findViewById(R.id.profile_change_password).setOnClickListener(v -> changePassword());
        view.findViewById(R.id.profile_choose_image).setOnClickListener(v -> imagePicker.launch("image/*"));
        view.findViewById(R.id.profile_delete_image).setOnClickListener(v -> deleteImage());
        loadProfile();
    }

    private ArrayAdapter<String> adapter(String[] values) {
        return new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, values);
    }

    private void loadProfile() {
        ApiClient.getApi().getOwnProfile(session.getAuthorizationHeader())
                .enqueue(profileCallback("Profil je učitan."));
    }

    private void populate(ProfileResponse value) {
        profile = value;
        setText(R.id.profile_name, value.getName());
        setText(R.id.profile_surname, value.getSurname());
        setText(R.id.profile_email, value.getEmail());
        setText(R.id.profile_address, value.getAddress());
        setText(R.id.profile_phone, value.getPhone());
        gender.setSelection("FEMALE".equals(value.getGender()) ? 1 : 0);
        root.findViewById(R.id.profile_pending).setVisibility(
                value.isProfileChangePending() ? View.VISIBLE : View.GONE);
        boolean driver = "driver".equalsIgnoreCase(value.getRole());
        root.findViewById(R.id.profile_driver_section).setVisibility(driver ? View.VISIBLE : View.GONE);
        if (driver && value.getVehicle() != null) {
            int minutes = value.getActiveMinutesLast24Hours() == null
                    ? 0 : value.getActiveMinutesLast24Hours();
            ((TextView) root.findViewById(R.id.profile_active_hours)).setText(
                    "Aktivan u poslednja 24h: " + (minutes / 60) + " h " + (minutes % 60) + " min");
            ProfileVehicle vehicle = value.getVehicle();
            setText(R.id.profile_vehicle_model, vehicle.getModel());
            setText(R.id.profile_vehicle_registration, vehicle.getRegistration());
            setText(R.id.profile_vehicle_seats, String.valueOf(vehicle.getSeats()));
            vehicleType.setSelection("LUXURY".equals(vehicle.getType()) ? 1
                    : "VAN".equals(vehicle.getType()) ? 2 : 0);
            ((MaterialSwitch) root.findViewById(R.id.profile_vehicle_baby))
                    .setChecked(vehicle.isBabyFriendly());
            ((MaterialSwitch) root.findViewById(R.id.profile_vehicle_pet))
                    .setChecked(vehicle.isPetFriendly());
        }
        loadImage(value.getProfileImageUrl());
    }

    private void saveProfile() {
        String name = text(R.id.profile_name);
        String surname = text(R.id.profile_surname);
        String email = text(R.id.profile_email);
        String address = text(R.id.profile_address);
        String phone = text(R.id.profile_phone);
        if (name.length() < 2 || surname.isBlank() || address.isBlank() || phone.isBlank()
                || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            show("Popunite sva lična polja ispravnim podacima.", true);
            return;
        }
        ProfileVehicleUpdateRequest vehicle = null;
        if (profile != null && "driver".equalsIgnoreCase(profile.getRole())) {
            int seats;
            try { seats = Integer.parseInt(text(R.id.profile_vehicle_seats)); }
            catch (NumberFormatException exception) { show("Broj mesta nije ispravan.", true); return; }
            if (seats < 4 || seats > 12 || text(R.id.profile_vehicle_model).isBlank()
                    || text(R.id.profile_vehicle_registration).isBlank()) {
                show("Proverite podatke vozila i broj mesta (4–12).", true);
                return;
            }
            String[] types = {"STANDARD", "LUXURY", "VAN"};
            vehicle = new ProfileVehicleUpdateRequest(text(R.id.profile_vehicle_model),
                    types[vehicleType.getSelectedItemPosition()],
                    text(R.id.profile_vehicle_registration).toUpperCase(), seats,
                    ((MaterialSwitch) root.findViewById(R.id.profile_vehicle_baby)).isChecked(),
                    ((MaterialSwitch) root.findViewById(R.id.profile_vehicle_pet)).isChecked());
        }
        ProfileUpdateRequest request = new ProfileUpdateRequest(name, surname, email,
                gender.getSelectedItemPosition() == 0 ? "MALE" : "FEMALE",
                address, phone, vehicle);
        ApiClient.getApi().updateOwnProfile(session.getAuthorizationHeader(), request)
                .enqueue(profileCallback(profile != null && "driver".equalsIgnoreCase(profile.getRole())
                        ? "Zahtev je poslat administratoru." : "Profil je sačuvan."));
    }

    private void changePassword() {
        String current = text(R.id.profile_current_password);
        String next = text(R.id.profile_new_password);
        String confirm = text(R.id.profile_confirm_password);
        if (current.isBlank() || next.length() < 8 || !next.matches(".*[A-Z].*")
                || !next.matches(".*\\d.*") || !next.equals(confirm)) {
            show("Nova lozinka mora imati 8 znakova, veliko slovo i broj; potvrda mora biti ista.", true);
            return;
        }
        ApiClient.getApi().changeProfilePassword(session.getAuthorizationHeader(),
                new PasswordChangeRequest(current, next, confirm)).enqueue(new Callback<>() {
            @Override public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (response.isSuccessful()) {
                    setText(R.id.profile_current_password, "");
                    setText(R.id.profile_new_password, "");
                    setText(R.id.profile_confirm_password, "");
                    show("Lozinka je promenjena.", false);
                } else show("Promena lozinke nije uspela (HTTP " + response.code() + ").", true);
            }
            @Override public void onFailure(@NonNull Call<Void> call, @NonNull Throwable throwable) {
                show("Backend nije dostupan: " + throwable.getMessage(), true);
            }
        });
    }

    private void uploadImage(Uri uri) {
        try (InputStream stream = requireContext().getContentResolver().openInputStream(uri)) {
            if (stream == null) { show("Slika nije dostupna.", true); return; }
            byte[] bytes = stream.readAllBytes();
            String mime = requireContext().getContentResolver().getType(uri);
            if (mime == null) mime = "image/jpeg";
            RequestBody body = RequestBody.create(MediaType.parse(mime), bytes);
            MultipartBody.Part part = MultipartBody.Part.createFormData("file", "profile-image", body);
            ApiClient.getApi().uploadProfileImage(session.getAuthorizationHeader(), part)
                    .enqueue(profileCallback(profile != null && "driver".equalsIgnoreCase(profile.getRole())
                            ? "Slika čeka odobrenje administratora." : "Slika je sačuvana."));
        } catch (Exception exception) {
            show("Slika nije mogla da se pročita.", true);
        }
    }

    private void deleteImage() {
        ApiClient.getApi().deleteProfileImage(session.getAuthorizationHeader())
                .enqueue(profileCallback(profile != null && "driver".equalsIgnoreCase(profile.getRole())
                        ? "Uklanjanje slike čeka odobrenje." : "Slika je uklonjena."));
    }

    private Callback<ProfileResponse> profileCallback(String successMessage) {
        return new Callback<>() {
            @Override public void onResponse(@NonNull Call<ProfileResponse> call,
                                             @NonNull Response<ProfileResponse> response) {
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null) {
                    populate(response.body());
                    show(successMessage, false);
                } else show("Operacija nije uspela (HTTP " + response.code() + ").", true);
            }
            @Override public void onFailure(@NonNull Call<ProfileResponse> call,
                                            @NonNull Throwable throwable) {
                if (isAdded()) show("Backend nije dostupan: " + throwable.getMessage(), true);
            }
        };
    }

    private void loadImage(String url) {
        image.setImageResource(R.mipmap.ic_launcher);
        if (url == null || url.isBlank()) return;
        ApiClient.getApi().downloadFile(url).enqueue(new Callback<>() {
            @Override public void onResponse(@NonNull Call<ResponseBody> call,
                                             @NonNull Response<ResponseBody> response) {
                if (isAdded() && response.isSuccessful() && response.body() != null) {
                    image.setImageBitmap(BitmapFactory.decodeStream(response.body().byteStream()));
                }
            }
            @Override public void onFailure(@NonNull Call<ResponseBody> call,
                                            @NonNull Throwable throwable) { }
        });
    }

    private String text(int id) {
        TextInputEditText input = root.findViewById(id);
        return input.getText() == null ? "" : input.getText().toString().trim();
    }

    private void setText(int id, String value) {
        ((TextInputEditText) root.findViewById(id)).setText(value == null ? "" : value);
    }

    private void show(String value, boolean error) {
        message.setText(value);
        message.setTextColor(requireContext().getColor(error
                ? android.R.color.holo_red_dark : android.R.color.holo_green_dark));
        message.setVisibility(View.VISIBLE);
    }
}
