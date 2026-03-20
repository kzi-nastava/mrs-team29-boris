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
import com.example.mobilnaaplikacijatim29.data.model.LoginRequest;
import com.example.mobilnaaplikacijatim29.data.model.LoginResponse;
import com.example.mobilnaaplikacijatim29.data.session.SessionManager;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_login, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        TextInputEditText emailInput = view.findViewById(R.id.login_email);
        TextInputEditText passwordInput = view.findViewById(R.id.login_password);
        TextInputLayout emailLayout = view.findViewById(R.id.login_email_layout);
        TextInputLayout passwordLayout = view.findViewById(R.id.login_password_layout);
        TextView errorView = view.findViewById(R.id.login_error);
        ProgressBar progress = view.findViewById(R.id.login_progress);
        View submit = view.findViewById(R.id.login_submit);

        submit.setOnClickListener(v -> {
            String email = textOf(emailInput);
            String password = textOf(passwordInput);
            emailLayout.setError(null);
            passwordLayout.setError(null);
            errorView.setVisibility(View.GONE);

            boolean valid = true;
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                emailLayout.setError("Unesite ispravnu email adresu.");
                valid = false;
            }
            if (password.isEmpty()) {
                passwordLayout.setError("Lozinka je obavezna.");
                valid = false;
            }
            if (!valid) {
                return;
            }

            setLoading(true, progress, submit);
            ApiClient.getApi().login(new LoginRequest(email, password))
                    .enqueue(new Callback<LoginResponse>() {
                        @Override
                        public void onResponse(@NonNull Call<LoginResponse> call,
                                               @NonNull Response<LoginResponse> response) {
                            setLoading(false, progress, submit);
                            if (!response.isSuccessful() || response.body() == null) {
                                showError(errorView, "Prijava nije uspela (HTTP "
                                        + response.code() + "). Proverite podatke i aktivaciju naloga.");
                                return;
                            }

                            new SessionManager(requireContext()).save(response.body());
                            Toast.makeText(requireContext(), "Uspešna prijava.", Toast.LENGTH_SHORT).show();
                            ((MainActivity) requireActivity()).navigateTo(R.id.nav_home);
                        }

                        @Override
                        public void onFailure(@NonNull Call<LoginResponse> call,
                                              @NonNull Throwable throwable) {
                            setLoading(false, progress, submit);
                            showError(errorView, "Backend nije dostupan: " + throwable.getMessage());
                        }
                    });
        });
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
