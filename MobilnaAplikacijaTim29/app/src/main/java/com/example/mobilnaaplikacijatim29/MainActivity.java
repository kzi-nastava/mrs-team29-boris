package com.example.mobilnaaplikacijatim29;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.example.mobilnaaplikacijatim29.data.api.ApiClient;
import com.example.mobilnaaplikacijatim29.data.session.SessionManager;
import com.example.mobilnaaplikacijatim29.ui.admin.AdminDashboardFragment;
import com.example.mobilnaaplikacijatim29.ui.admin.DriverRegistrationFragment;
import com.example.mobilnaaplikacijatim29.ui.auth.CompleteDriverRegistrationFragment;
import com.example.mobilnaaplikacijatim29.ui.auth.ForgotPasswordFragment;
import com.example.mobilnaaplikacijatim29.ui.auth.LoginFragment;
import com.example.mobilnaaplikacijatim29.ui.auth.ResetPasswordFragment;
import com.example.mobilnaaplikacijatim29.ui.driver.DriverDashboardFragment;
import com.example.mobilnaaplikacijatim29.ui.home.HomeFragment;
import com.example.mobilnaaplikacijatim29.ui.passenger.PassengerDashboardFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    public interface LogoutCallback {
        void onFailure(String message);
    }

    private BottomNavigationView bottomNavigation;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        sessionManager = new SessionManager(this);
        bottomNavigation = findViewById(R.id.bottom_navigation);
        bottomNavigation.setOnItemSelectedListener(item -> {
            showDestination(item.getItemId(), null);
            return true;
        });
        configureNavigationForSession();

        if (savedInstanceState == null && !handleDeepLink(getIntent())) {
            bottomNavigation.setSelectedItemId(R.id.nav_home);
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleDeepLink(intent);
    }

    public void navigateTo(int destinationId) {
        if (bottomNavigation.getMenu().findItem(destinationId) != null
                && bottomNavigation.getMenu().findItem(destinationId).isVisible()) {
            if (bottomNavigation.getSelectedItemId() == destinationId) {
                showDestination(destinationId, null);
            } else {
                bottomNavigation.setSelectedItemId(destinationId);
            }
        } else {
            showDestination(destinationId, null);
        }
    }

    public void navigateAfterLogin() {
        configureNavigationForSession();
        bottomNavigation.setSelectedItemId(R.id.nav_dashboard);
    }

    public void requestLogout(LogoutCallback callback) {
        if (!sessionManager.isLoggedIn()) {
            finishLogout();
            return;
        }
        ApiClient.getApi().logout(sessionManager.getAuthorizationHeader())
                .enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(@NonNull Call<Void> call,
                                           @NonNull Response<Void> response) {
                        if (response.isSuccessful()) {
                            finishLogout();
                        } else {
                            callback.onFailure(response.code() == 409
                                    ? "Ne možete se odjaviti dok imate aktivnu vožnju."
                                    : "Odjava nije uspela (HTTP " + response.code() + ").");
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<Void> call,
                                          @NonNull Throwable throwable) {
                        callback.onFailure("Backend nije dostupan: " + throwable.getMessage());
                    }
                });
    }

    private void finishLogout() {
        sessionManager.clear();
        configureNavigationForSession();
        Toast.makeText(this, "Uspešno ste se odjavili.", Toast.LENGTH_SHORT).show();
        bottomNavigation.setSelectedItemId(R.id.nav_home);
    }

    private void configureNavigationForSession() {
        boolean loggedIn = sessionManager.isLoggedIn();
        bottomNavigation.getMenu().findItem(R.id.nav_login).setVisible(!loggedIn);
        bottomNavigation.getMenu().findItem(R.id.nav_dashboard).setVisible(loggedIn);
        if (loggedIn) {
            String role = sessionManager.getRole();
            String title = "user".equalsIgnoreCase(role) ? "Putnik"
                    : "driver".equalsIgnoreCase(role) ? "Vozač" : "Admin";
            bottomNavigation.getMenu().findItem(R.id.nav_dashboard).setTitle(title);
        }
    }

    private void showDestination(int destinationId, String token) {
        Fragment fragment;
        if (destinationId == R.id.nav_login) {
            fragment = new LoginFragment();
        } else if (destinationId == R.id.nav_dashboard) {
            String role = sessionManager.getRole();
            if ("admin".equalsIgnoreCase(role)) {
                fragment = new AdminDashboardFragment();
            } else if ("driver".equalsIgnoreCase(role)) {
                fragment = new DriverDashboardFragment();
            } else {
                fragment = new PassengerDashboardFragment();
            }
        } else if (destinationId == R.id.nav_driver_registration) {
            fragment = new DriverRegistrationFragment();
        } else if (destinationId == R.id.nav_forgot_password) {
            fragment = new ForgotPasswordFragment();
        } else if (destinationId == R.id.nav_reset_password) {
            fragment = ResetPasswordFragment.newInstance(token);
        } else if (destinationId == R.id.nav_complete_driver_registration) {
            fragment = CompleteDriverRegistrationFragment.newInstance(token);
        } else {
            fragment = new HomeFragment();
        }

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    private boolean handleDeepLink(Intent intent) {
        Uri data = intent == null ? null : intent.getData();
        if (data == null || !"clickanddrive".equalsIgnoreCase(data.getScheme())) {
            return false;
        }
        String token = data.getQueryParameter("token");
        if ("reset-password".equalsIgnoreCase(data.getHost())) {
            showDestination(R.id.nav_reset_password, token);
            return true;
        }
        if ("complete-registration".equalsIgnoreCase(data.getHost())) {
            showDestination(R.id.nav_complete_driver_registration, token);
            return true;
        }
        return false;
    }
}
