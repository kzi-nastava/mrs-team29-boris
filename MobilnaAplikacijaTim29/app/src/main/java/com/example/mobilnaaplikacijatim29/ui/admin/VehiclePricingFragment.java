package com.example.mobilnaaplikacijatim29.ui.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.mobilnaaplikacijatim29.R;
import com.example.mobilnaaplikacijatim29.data.api.ApiClient;
import com.example.mobilnaaplikacijatim29.data.model.VehiclePriceConfig;
import com.example.mobilnaaplikacijatim29.data.session.SessionManager;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class VehiclePricingFragment extends Fragment {
    private SessionManager session;
    private TextInputEditText standard;
    private TextInputEditText luxury;
    private TextInputEditText van;
    private TextInputEditText perKm;
    private TextView message;
    private View saveButton;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_vehicle_pricing, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        session = new SessionManager(requireContext());
        standard = view.findViewById(R.id.price_standard);
        luxury = view.findViewById(R.id.price_luxury);
        van = view.findViewById(R.id.price_van);
        perKm = view.findViewById(R.id.price_per_km);
        message = view.findViewById(R.id.prices_message);
        saveButton = view.findViewById(R.id.prices_save);
        saveButton.setOnClickListener(v -> savePrices());
        loadPrices();
    }

    private void loadPrices() {
        setLoading(true, "Učitavanje cenovnika...");
        ApiClient.getApi().getVehiclePrices(session.getAuthorizationHeader())
                .enqueue(new Callback<>() {
                    @Override
                    public void onResponse(@NonNull Call<VehiclePriceConfig> call,
                                           @NonNull Response<VehiclePriceConfig> response) {
                        if (!isAdded()) return;
                        if (!response.isSuccessful() || response.body() == null) {
                            showError("Cenovnik nije učitan (HTTP " + response.code() + ").");
                            return;
                        }
                        populate(response.body());
                        setLoading(false, "");
                    }

                    @Override
                    public void onFailure(@NonNull Call<VehiclePriceConfig> call,
                                          @NonNull Throwable throwable) {
                        if (isAdded()) showError("Backend nije dostupan: " + throwable.getMessage());
                    }
                });
    }

    private void savePrices() {
        clearErrors();
        Double standardValue = readPrice(standard, R.id.price_standard_layout);
        Double luxuryValue = readPrice(luxury, R.id.price_luxury_layout);
        Double vanValue = readPrice(van, R.id.price_van_layout);
        Double perKmValue = readPrice(perKm, R.id.price_per_km_layout);
        if (standardValue == null || luxuryValue == null || vanValue == null || perKmValue == null) {
            showError("Ispravite označena polja.");
            return;
        }

        setLoading(true, "Čuvanje cenovnika...");
        ApiClient.getApi().updateVehiclePrices(session.getAuthorizationHeader(),
                        new VehiclePriceConfig(standardValue, luxuryValue, vanValue, perKmValue))
                .enqueue(new Callback<>() {
                    @Override
                    public void onResponse(@NonNull Call<VehiclePriceConfig> call,
                                           @NonNull Response<VehiclePriceConfig> response) {
                        if (!isAdded()) return;
                        if (!response.isSuccessful() || response.body() == null) {
                            showError("Cenovnik nije sačuvan (HTTP " + response.code() + ").");
                            return;
                        }
                        populate(response.body());
                        setLoading(false, "Cenovnik je uspešno sačuvan.");
                    }

                    @Override
                    public void onFailure(@NonNull Call<VehiclePriceConfig> call,
                                          @NonNull Throwable throwable) {
                        if (isAdded()) showError("Backend nije dostupan: " + throwable.getMessage());
                    }
                });
    }

    private Double readPrice(TextInputEditText input, int layoutId) {
        try {
            String raw = input.getText() == null ? "" : input.getText().toString().trim();
            double value = Double.parseDouble(raw.replace(',', '.'));
            if (!validPrice(value)) throw new NumberFormatException();
            return value;
        } catch (NumberFormatException exception) {
            TextInputLayout layout = requireView().findViewById(layoutId);
            layout.setError("Unesite cenu veću od 0 i do 1.000.000 RSD");
            return null;
        }
    }

    static boolean validPrice(double value) {
        return Double.isFinite(value) && value > 0 && value <= 1_000_000;
    }

    private void populate(VehiclePriceConfig prices) {
        standard.setText(format(prices.getStandardBasePrice()));
        luxury.setText(format(prices.getLuxuryBasePrice()));
        van.setText(format(prices.getVanBasePrice()));
        perKm.setText(format(prices.getPricePerKm()));
    }

    private void clearErrors() {
        int[] layouts = {R.id.price_standard_layout, R.id.price_luxury_layout,
                R.id.price_van_layout, R.id.price_per_km_layout};
        for (int id : layouts) ((TextInputLayout) requireView().findViewById(id)).setError(null);
    }

    private void setLoading(boolean loading, String text) {
        saveButton.setEnabled(!loading);
        message.setText(text);
        message.setTextColor(com.google.android.material.color.MaterialColors.getColor(
                message, loading ? com.google.android.material.R.attr.colorOnSurfaceVariant
                        : com.google.android.material.R.attr.colorPrimary));
        message.setVisibility(text.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void showError(String text) {
        saveButton.setEnabled(true);
        message.setText(text);
        message.setTextColor(com.google.android.material.color.MaterialColors.getColor(
                message, com.google.android.material.R.attr.colorError));
        message.setVisibility(View.VISIBLE);
    }

    private static String format(double value) {
        return Math.abs(value - Math.rint(value)) < 0.001
                ? String.format(Locale.ROOT, "%.0f", value)
                : String.format(Locale.ROOT, "%.2f", value);
    }
}
