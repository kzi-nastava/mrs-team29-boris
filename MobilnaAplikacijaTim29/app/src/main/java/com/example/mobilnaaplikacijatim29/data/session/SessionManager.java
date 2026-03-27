package com.example.mobilnaaplikacijatim29.data.session;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.mobilnaaplikacijatim29.data.model.LoginResponse;

public class SessionManager {

    private static final String PREFERENCES_NAME = "click_and_drive_session";
    private static final String KEY_TOKEN = "token";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_ROLE = "role";

    private final SharedPreferences preferences;

    public SessionManager(Context context) {
        preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
    }

    public void save(LoginResponse response) {
        preferences.edit()
                .putString(KEY_TOKEN, response.getToken())
                .putLong(KEY_USER_ID, response.getUserId())
                .putString(KEY_EMAIL, response.getEmail())
                .putString(KEY_ROLE, response.getRole())
                .apply();
    }

    public boolean isLoggedIn() {
        return preferences.getString(KEY_TOKEN, null) != null;
    }

    public String getEmail() {
        return preferences.getString(KEY_EMAIL, "");
    }

    public long getUserId() {
        return preferences.getLong(KEY_USER_ID, -1L);
    }

    public String getAuthorizationHeader() {
        return "Bearer " + getToken();
    }

    public String getRole() {
        return preferences.getString(KEY_ROLE, "");
    }

    public String getToken() {
        return preferences.getString(KEY_TOKEN, "");
    }

    public void clear() {
        preferences.edit().clear().apply();
    }
}
