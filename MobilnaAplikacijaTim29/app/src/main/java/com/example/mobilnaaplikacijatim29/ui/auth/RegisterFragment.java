package com.example.mobilnaaplikacijatim29.ui.auth;

import android.os.Bundle;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.mobilnaaplikacijatim29.MainActivity;
import com.example.mobilnaaplikacijatim29.R;
import com.example.mobilnaaplikacijatim29.data.api.ApiClient;
import com.example.mobilnaaplikacijatim29.data.model.RegisterRequest;
import com.example.mobilnaaplikacijatim29.data.model.UserProfileResponse;
import com.google.android.material.textfield.TextInputEditText;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_register, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        TextInputEditText name = view.findViewById(R.id.register_name);
        TextInputEditText lastName = view.findViewById(R.id.register_last_name);
        TextInputEditText email = view.findViewById(R.id.register_email);
        TextInputEditText address = view.findViewById(R.id.register_address);
        TextInputEditText phone = view.findViewById(R.id.register_phone);
        TextInputEditText password = view.findViewById(R.id.register_password);
        TextInputEditText confirmPassword = view.findViewById(R.id.register_confirm_password);
        TextView errorView = view.findViewById(R.id.register_error);
        ProgressBar progress = view.findViewById(R.id.register_progress);
        View submit = view.findViewById(R.id.register_submit);

        submit.setOnClickListener(v -> {
            String nameText = textOf(name);
            String lastNameText = textOf(lastName);
            String emailText = textOf(email);
            String addressText = textOf(address);
            String phoneText = textOf(phone);
            String passwordText = textOf(password);
            String confirmText = textOf(confirmPassword);

            String validationError = validate(nameText, lastNameText, emailText, addressText,
                    phoneText, passwordText, confirmText);
            if (validationError != null) {
                showError(errorView, validationError);
                return;
            }

            errorView.setVisibility(View.GONE);
            setLoading(true, progress, submit);
            RegisterRequest request = new RegisterRequest(nameText, lastNameText, emailText,
                    passwordText, confirmText, addressText, phoneText);
            ApiClient.getApi().register(request).enqueue(new Callback<UserProfileResponse>() {
                @Override
                public void onResponse(@NonNull Call<UserProfileResponse> call,
                                       @NonNull Response<UserProfileResponse> response) {
                    setLoading(false, progress, submit);
                    if (!response.isSuccessful() || response.body() == null) {
                        showError(errorView, "Registracija nije uspela (HTTP "
                                + response.code() + "). Email možda već postoji.");
                        return;
                    }

                    Toast.makeText(requireContext(),
                            "Registracija je uspela. Proverite email za aktivaciju naloga.",
                            Toast.LENGTH_LONG).show();
                    ((MainActivity) requireActivity()).navigateTo(R.id.nav_login);
                }

                @Override
                public void onFailure(@NonNull Call<UserProfileResponse> call,
                                      @NonNull Throwable throwable) {
                    setLoading(false, progress, submit);
                    showError(errorView, "Backend nije dostupan: " + throwable.getMessage());
                }
            });
        });
    }

    private String validate(String name, String lastName, String email, String address,
                            String phone, String password, String confirmPassword) {
        if (name.isEmpty() || lastName.isEmpty() || address.isEmpty() || phone.isEmpty()) {
            return "Sva polja su obavezna.";
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            return "Unesite ispravnu email adresu.";
        }
        if (password.length() < 6) {
            return "Lozinka mora imati najmanje 6 karaktera.";
        }
        if (!password.equals(confirmPassword)) {
            return "Lozinke se ne podudaraju.";
        }
        return null;
    }

    private String textOf(TextInputEditText input) {
        return input.getText() == null ? "" : input.getText().toString().trim();
    }

    private void setLoading(boolean loading, ProgressBar progress, View submit) {
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        submit.setEnabled(!loading);
    }

    private void showError(TextView errorView, String message) {
        errorView.setText(message);
        errorView.setVisibility(View.VISIBLE);
    }
}
