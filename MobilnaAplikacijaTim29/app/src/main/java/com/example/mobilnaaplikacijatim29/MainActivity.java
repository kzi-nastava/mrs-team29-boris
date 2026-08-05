package com.example.mobilnaaplikacijatim29;

import android.content.Intent;
import android.Manifest;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
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
import com.example.mobilnaaplikacijatim29.ui.driver.DriverRideDetailFragment;
import com.example.mobilnaaplikacijatim29.ui.driver.DriverRideHistoryFragment;
import com.example.mobilnaaplikacijatim29.ui.home.HomeFragment;
import com.example.mobilnaaplikacijatim29.ui.profile.ProfileFragment;
import com.example.mobilnaaplikacijatim29.ui.admin.ProfileChangeRequestsFragment;
import com.example.mobilnaaplikacijatim29.ui.admin.UserBlockingFragment;
import com.example.mobilnaaplikacijatim29.ui.admin.VehiclePricingFragment;
import com.example.mobilnaaplikacijatim29.ui.report.ReportsFragment;
import com.example.mobilnaaplikacijatim29.ui.support.SupportChatFragment;
import com.example.mobilnaaplikacijatim29.ui.support.SupportConversationsFragment;
import com.example.mobilnaaplikacijatim29.ui.notifications.NotificationsFragment;
import com.example.mobilnaaplikacijatim29.notifications.SystemNotificationHelper;
import com.example.mobilnaaplikacijatim29.data.model.AppNotification;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    public static final String EXTRA_OPEN_NOTIFICATIONS = "open_notifications";

    public interface LogoutCallback {
        void onFailure(String message);
    }

    private BottomNavigationView bottomNavigation;
    private SessionManager sessionManager;
    private final Handler notificationHandler = new Handler(Looper.getMainLooper());
    private final Runnable notificationRefresh = this::pollNotifications;
    private boolean notificationRequestInProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        sessionManager = new SessionManager(this);
        // Development behavior: a fresh launcher start begins logged out. Do not
        // clear the session when Android recreates the process while an external
        // activity (for example the system image picker) is open.
        if (savedInstanceState == null
                && Intent.ACTION_MAIN.equals(getIntent().getAction())) {
            sessionManager.clear();
        }
        SystemNotificationHelper.createChannel(this);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS}, 2201);
        }
        bottomNavigation = findViewById(R.id.bottom_navigation);
        bottomNavigation.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_login && sessionManager.isLoggedIn()) {
                requestLogout(message ->
                        Toast.makeText(this, message, Toast.LENGTH_LONG).show());
                return false;
            }
            showDestination(item.getItemId(), null);
            return true;
        });
        configureNavigationForSession();
        scheduleNotificationPoll(0);

        if (savedInstanceState == null && !handleAppIntent(getIntent())) {
            if (sessionManager.isLoggedIn()) {
                bottomNavigation.setSelectedItemId(R.id.nav_dashboard);
            } else {
                clearNavigationSelection();
                showDestination(0, null);
            }
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
        handleAppIntent(intent);
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
        scheduleNotificationPoll(0);
        bottomNavigation.setSelectedItemId(R.id.nav_dashboard);
    }

    public void navigateToDriverRideDetail(long rideId, boolean guest) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container,
                        DriverRideDetailFragment.newInstance(rideId, guest))
                .addToBackStack(null)
                .commit();
    }

    public void navigateToSupportChat(long userId, String userLabel) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container,
                        SupportChatFragment.forAdministrator(userId, userLabel))
                .addToBackStack(null)
                .commit();
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
        notificationHandler.removeCallbacks(notificationRefresh);
        sessionManager.clear();
        configureNavigationForSession();
        Toast.makeText(this, "Uspešno ste se odjavili.", Toast.LENGTH_SHORT).show();
        clearNavigationSelection();
        showDestination(0, null);
    }

    private void configureNavigationForSession() {
        boolean loggedIn = sessionManager.isLoggedIn();
        bottomNavigation.getMenu().findItem(R.id.nav_dashboard).setVisible(loggedIn);
        bottomNavigation.getMenu().findItem(R.id.nav_profile).setVisible(loggedIn);
        bottomNavigation.getMenu().findItem(R.id.nav_support).setVisible(loggedIn);
        bottomNavigation.getMenu().findItem(R.id.nav_notifications).setVisible(loggedIn);
        bottomNavigation.getMenu().findItem(R.id.nav_login)
                .setTitle(loggedIn ? "Odjava" : "Prijava")
                .setIcon(loggedIn ? android.R.drawable.ic_menu_close_clear_cancel
                        : android.R.drawable.ic_lock_lock);
        if (loggedIn) {
            String role = sessionManager.getRole();
            String title = "user".equalsIgnoreCase(role) ? "Početna"
                    : "driver".equalsIgnoreCase(role) ? "Vozač" : "Admin";
            bottomNavigation.getMenu().findItem(R.id.nav_dashboard).setTitle(title);
        }
    }

    private void clearNavigationSelection() {
        bottomNavigation.getMenu().setGroupCheckable(0, true, false);
        for (int i = 0; i < bottomNavigation.getMenu().size(); i++) {
            bottomNavigation.getMenu().getItem(i).setChecked(false);
        }
        bottomNavigation.getMenu().setGroupCheckable(0, true, true);
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
                fragment = new HomeFragment();
            }
        } else if (destinationId == R.id.nav_driver_registration) {
            fragment = new DriverRegistrationFragment();
        } else if (destinationId == R.id.nav_profile) {
            fragment = new ProfileFragment();
        } else if (destinationId == R.id.nav_profile_change_requests) {
            fragment = new ProfileChangeRequestsFragment();
        } else if (destinationId == R.id.nav_driver_ride_history) {
            fragment = new DriverRideHistoryFragment();
        } else if (destinationId == R.id.nav_reports) {
            fragment = new ReportsFragment();
        } else if (destinationId == R.id.nav_user_blocking) {
            fragment = new UserBlockingFragment();
        } else if (destinationId == R.id.nav_support) {
            fragment = "admin".equalsIgnoreCase(sessionManager.getRole())
                    ? new SupportConversationsFragment() : new SupportChatFragment();
        } else if (destinationId == R.id.nav_vehicle_prices) {
            fragment = new VehiclePricingFragment();
        } else if (destinationId == R.id.nav_notifications) {
            fragment = new NotificationsFragment();
        } else if (destinationId == R.id.nav_forgot_password) {
            fragment = new ForgotPasswordFragment();
        } else if (destinationId == R.id.nav_reset_password) {
            fragment = ResetPasswordFragment.newInstance(token);
        } else if (destinationId == R.id.nav_complete_driver_registration) {
            fragment = CompleteDriverRegistrationFragment.newInstance(token);
        } else {
            String role = sessionManager.getRole();
            fragment = !sessionManager.isLoggedIn() ? new HomeFragment()
                    : "admin".equalsIgnoreCase(role) ? new AdminDashboardFragment()
                    : "driver".equalsIgnoreCase(role) ? new DriverDashboardFragment()
                    : new HomeFragment();
        }

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    private void pollNotifications() {
        if (sessionManager == null || !sessionManager.isLoggedIn()
                || notificationRequestInProgress) {
            scheduleNotificationPoll(5000);
            return;
        }
        notificationRequestInProgress = true;
        ApiClient.getApi().getNotifications(sessionManager.getAuthorizationHeader())
                .enqueue(new Callback<>() {
                    @Override public void onResponse(@NonNull Call<List<AppNotification>> call,
                                                     @NonNull Response<List<AppNotification>> response) {
                        notificationRequestInProgress = false;
                        if (response.isSuccessful() && response.body() != null) {
                            deliverNewNotifications(response.body());
                        }
                        scheduleNotificationPoll(5000);
                    }
                    @Override public void onFailure(@NonNull Call<List<AppNotification>> call,
                                                    @NonNull Throwable throwable) {
                        notificationRequestInProgress = false;
                        scheduleNotificationPoll(5000);
                    }
                });
    }

    private void deliverNewNotifications(List<AppNotification> values) {
        String key = "last_notification_" + sessionManager.getUserId();
        android.content.SharedPreferences preferences = getSharedPreferences(
                "notification_delivery", MODE_PRIVATE);
        long lastDelivered = preferences.getLong(key, 0L);
        long newest = lastDelivered;
        for (int i = values.size() - 1; i >= 0; i--) {
            AppNotification value = values.get(i);
            if (value.getId() != null && value.getId() > lastDelivered) {
                SystemNotificationHelper.show(this, value);
                newest = Math.max(newest, value.getId());
            }
        }
        if (newest > lastDelivered) preferences.edit().putLong(key, newest).apply();
    }

    private void scheduleNotificationPoll(long delayMs) {
        notificationHandler.removeCallbacks(notificationRefresh);
        notificationHandler.postDelayed(notificationRefresh, delayMs);
    }

    @Override
    protected void onDestroy() {
        notificationHandler.removeCallbacks(notificationRefresh);
        super.onDestroy();
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

    private boolean handleAppIntent(Intent intent) {
        if (intent != null && intent.getBooleanExtra(EXTRA_OPEN_NOTIFICATIONS, false)
                && sessionManager.isLoggedIn()) {
            intent.removeExtra(EXTRA_OPEN_NOTIFICATIONS);
            showDestination(R.id.nav_notifications, null);
            return true;
        }
        return handleDeepLink(intent);
    }
}
