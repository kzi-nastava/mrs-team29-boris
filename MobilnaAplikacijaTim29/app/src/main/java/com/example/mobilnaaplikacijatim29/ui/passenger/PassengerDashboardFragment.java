package com.example.mobilnaaplikacijatim29.ui.passenger;

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

public class PassengerDashboardFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        SessionManager session = new SessionManager(requireContext());
        ((TextView) view.findViewById(R.id.dashboard_title)).setText("Putnički nalog");
        ((TextView) view.findViewById(R.id.dashboard_subtitle))
                .setText("Dobro došli, " + session.getEmail()
                        + ". Vožnju možete poručiti na početnoj mapi.");
        View reports = view.findViewById(R.id.reports_button);
        reports.setVisibility(View.VISIBLE);
        reports.setOnClickListener(v ->
                ((MainActivity) requireActivity()).navigateTo(R.id.nav_reports));
    }
}
