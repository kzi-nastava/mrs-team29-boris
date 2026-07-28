package com.example.mobilnaaplikacijatim29.ui.admin;

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
import com.example.mobilnaaplikacijatim29.data.session.SessionManager;

public class AdminDashboardFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        SessionManager session = new SessionManager(requireContext());
        ((TextView) view.findViewById(R.id.dashboard_title)).setText("Administratorski panel");
        ((TextView) view.findViewById(R.id.dashboard_subtitle))
                .setText("Prijavljeni ste kao " + session.getEmail());
        View registerDriver = view.findViewById(R.id.admin_register_driver);
        registerDriver.setVisibility(View.VISIBLE);
        registerDriver.setOnClickListener(v ->
                ((MainActivity) requireActivity()).navigateTo(R.id.nav_driver_registration));
        View profileRequests = view.findViewById(R.id.admin_profile_change_requests);
        profileRequests.setVisibility(View.VISIBLE);
        profileRequests.setOnClickListener(v ->
                ((MainActivity) requireActivity()).navigateTo(R.id.nav_profile_change_requests));
        View userBlocking = view.findViewById(R.id.admin_user_blocking);
        userBlocking.setVisibility(View.VISIBLE);
        userBlocking.setOnClickListener(v ->
                ((MainActivity) requireActivity()).navigateTo(R.id.nav_user_blocking));
        View reports = view.findViewById(R.id.reports_button);
        reports.setVisibility(View.VISIBLE);
        reports.setOnClickListener(v ->
                ((MainActivity) requireActivity()).navigateTo(R.id.nav_reports));
        view.findViewById(R.id.dashboard_logout).setOnClickListener(v ->
                ((MainActivity) requireActivity()).requestLogout(message -> showMessage(view, message)));
    }

    private void showMessage(View view, String message) {
        TextView messageView = view.findViewById(R.id.dashboard_message);
        messageView.setText(message);
        messageView.setVisibility(View.VISIBLE);
    }
}
