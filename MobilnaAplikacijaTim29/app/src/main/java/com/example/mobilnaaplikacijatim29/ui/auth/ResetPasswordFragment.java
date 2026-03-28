package com.example.mobilnaaplikacijatim29.ui.auth;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.mobilnaaplikacijatim29.MainActivity;
import com.example.mobilnaaplikacijatim29.R;
import com.example.mobilnaaplikacijatim29.data.api.ApiClient;
import com.example.mobilnaaplikacijatim29.data.model.ResetPasswordRequest;
import com.google.android.material.textfield.TextInputEditText;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ResetPasswordFragment extends Fragment {
    private static final String ARG_TOKEN = "token";

    public static ResetPasswordFragment newInstance(String token) {
        ResetPasswordFragment fragment = new ResetPasswordFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TOKEN, token);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_set_password, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        ((TextView) view.findViewById(R.id.set_password_title)).setText("Postavljanje nove lozinke");
        bindSubmit(view);
    }

    private void bindSubmit(View view) {
        TextInputEditText passwordInput = view.findViewById(R.id.set_password);
        TextInputEditText confirmInput = view.findViewById(R.id.set_password_confirm);
        TextView message = view.findViewById(R.id.set_password_message);
        View submit = view.findViewById(R.id.set_password_submit);
        submit.setOnClickListener(v -> {
            String token = getArguments() == null ? null : getArguments().getString(ARG_TOKEN);
            String password = textOf(passwordInput);
            String confirm = textOf(confirmInput);
            String validation = validate(token, password, confirm);
            if (validation != null) {
                show(message, validation);
                return;
            }
            submit.setEnabled(false);
            ApiClient.getApi().resetPassword(new ResetPasswordRequest(token, password, confirm))
                    .enqueue(new Callback<>() {
                        @Override
                        public void onResponse(@NonNull Call<Void> call,
                                               @NonNull Response<Void> response) {
                            submit.setEnabled(true);
                            if (response.isSuccessful()) {
                                ((MainActivity) requireActivity()).navigateTo(R.id.nav_login);
                            } else {
                                show(message, "Link je nevažeći ili je istekao (HTTP "
                                        + response.code() + ").");
                            }
                        }

                        @Override
                        public void onFailure(@NonNull Call<Void> call,
                                              @NonNull Throwable throwable) {
                            submit.setEnabled(true);
                            show(message, "Backend nije dostupan: " + throwable.getMessage());
                        }
                    });
        });
    }

    static String validate(String token, String password, String confirm) {
        if (token == null || token.isBlank()) return "Nedostaje token iz email linka.";
        if (password.length() < 8 || !password.matches(".*[A-Z].*")
                || !password.matches(".*\\d.*")) {
            return "Lozinka mora imati 8 znakova, veliko slovo i broj.";
        }
        return password.equals(confirm) ? null : "Lozinke se ne podudaraju.";
    }

    static String textOf(TextInputEditText input) {
        return input.getText() == null ? "" : input.getText().toString();
    }

    static void show(TextView view, String text) {
        view.setText(text);
        view.setVisibility(View.VISIBLE);
    }
}
