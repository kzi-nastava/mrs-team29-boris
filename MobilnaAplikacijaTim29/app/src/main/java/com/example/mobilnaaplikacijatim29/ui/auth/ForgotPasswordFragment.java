package com.example.mobilnaaplikacijatim29.ui.auth;

import android.os.Bundle;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.mobilnaaplikacijatim29.R;
import com.example.mobilnaaplikacijatim29.data.api.ApiClient;
import com.example.mobilnaaplikacijatim29.data.model.ForgotPasswordRequest;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ForgotPasswordFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_forgot_password, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        TextInputEditText emailInput = view.findViewById(R.id.forgot_email);
        TextInputLayout emailLayout = view.findViewById(R.id.forgot_email_layout);
        TextView message = view.findViewById(R.id.forgot_message);
        View submit = view.findViewById(R.id.forgot_submit);
        submit.setOnClickListener(v -> {
            String email = emailInput.getText() == null ? "" : emailInput.getText().toString().trim();
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                emailLayout.setError("Unesite ispravnu email adresu.");
                return;
            }
            emailLayout.setError(null);
            submit.setEnabled(false);
            ApiClient.getApi().forgotPassword(new ForgotPasswordRequest(email))
                    .enqueue(new Callback<>() {
                        @Override
                        public void onResponse(@NonNull Call<Void> call,
                                               @NonNull Response<Void> response) {
                            submit.setEnabled(true);
                            message.setVisibility(View.VISIBLE);
                            message.setText(response.isSuccessful()
                                    ? "Ako nalog postoji, link je poslat na email."
                                    : "Slanje nije uspelo (HTTP " + response.code() + ").");
                        }

                        @Override
                        public void onFailure(@NonNull Call<Void> call,
                                              @NonNull Throwable throwable) {
                            submit.setEnabled(true);
                            message.setVisibility(View.VISIBLE);
                            message.setText("Backend nije dostupan: " + throwable.getMessage());
                        }
                    });
        });
    }
}
