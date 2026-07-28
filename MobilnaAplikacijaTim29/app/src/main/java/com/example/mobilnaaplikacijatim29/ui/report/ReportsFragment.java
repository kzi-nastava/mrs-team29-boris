package com.example.mobilnaaplikacijatim29.ui.report;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.mobilnaaplikacijatim29.R;
import com.example.mobilnaaplikacijatim29.data.api.ApiClient;
import com.example.mobilnaaplikacijatim29.data.model.AdminReportUser;
import com.example.mobilnaaplikacijatim29.data.model.DailyReportStats;
import com.example.mobilnaaplikacijatim29.data.model.ReportRequest;
import com.example.mobilnaaplikacijatim29.data.model.ReportResponse;
import com.example.mobilnaaplikacijatim29.data.model.ReportSummary;
import com.example.mobilnaaplikacijatim29.data.session.SessionManager;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ReportsFragment extends Fragment {
    private final List<ReportTarget> targets = new ArrayList<>();
    private SessionManager session;
    private TextView dateFrom;
    private TextView dateTo;
    private TextView message;
    private TextView summary;
    private Spinner targetSpinner;
    private View adminControls;
    private ReportChartView ridesChart;
    private ReportChartView kilometersChart;
    private ReportChartView moneyChart;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_reports, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        session = new SessionManager(requireContext());
        dateFrom = view.findViewById(R.id.report_date_from);
        dateTo = view.findViewById(R.id.report_date_to);
        message = view.findViewById(R.id.report_message);
        summary = view.findViewById(R.id.report_summary);
        targetSpinner = view.findViewById(R.id.report_target);
        adminControls = view.findViewById(R.id.report_admin_controls);
        ridesChart = view.findViewById(R.id.report_rides_chart);
        kilometersChart = view.findViewById(R.id.report_km_chart);
        moneyChart = view.findViewById(R.id.report_money_chart);

        LocalDate today = LocalDate.now();
        dateFrom.setText(today.minusDays(6).toString());
        dateTo.setText(today.toString());
        dateFrom.setOnClickListener(v -> selectDate(dateFrom));
        dateTo.setOnClickListener(v -> selectDate(dateTo));
        view.findViewById(R.id.report_generate).setOnClickListener(v -> generateReport());

        if ("admin".equalsIgnoreCase(session.getRole())) {
            adminControls.setVisibility(View.VISIBLE);
            loadAdminTargets();
        } else {
            adminControls.setVisibility(View.GONE);
            generateReport();
        }
    }

    private void selectDate(TextView target) {
        LocalDate selected;
        try {
            selected = LocalDate.parse(target.getText().toString());
        } catch (Exception ignored) {
            selected = LocalDate.now();
        }
        new DatePickerDialog(requireContext(), (picker, year, month, day) ->
                target.setText(String.format(Locale.ROOT, "%04d-%02d-%02d", year, month + 1, day)),
                selected.getYear(), selected.getMonthValue() - 1, selected.getDayOfMonth()).show();
    }

    private void loadAdminTargets() {
        message.setText("Učitavanje korisnika...");
        message.setVisibility(View.VISIBLE);
        ApiClient.getApi().getReportUsers(session.getAuthorizationHeader())
                .enqueue(new Callback<>() {
                    @Override
                    public void onResponse(@NonNull Call<List<AdminReportUser>> call,
                                           @NonNull Response<List<AdminReportUser>> response) {
                        if (!isAdded()) return;
                        if (!response.isSuccessful() || response.body() == null) {
                            showError("Korisnici nisu učitani (HTTP " + response.code() + ").");
                            return;
                        }
                        targets.clear();
                        targets.add(new ReportTarget("Svi vozači", null, "ALL_DRIVERS"));
                        targets.add(new ReportTarget("Svi putnici", null, "ALL_PASSENGERS"));
                        for (AdminReportUser user : response.body()) {
                            if ("DRIVER".equals(user.getRole()) || "PASSENGER".equals(user.getRole())) {
                                targets.add(new ReportTarget(user.toString(), user.getId(), user.getRole()));
                            }
                        }
                        targetSpinner.setAdapter(new ArrayAdapter<>(requireContext(),
                                android.R.layout.simple_spinner_dropdown_item, targets));
                        message.setVisibility(View.GONE);
                        generateReport();
                    }

                    @Override
                    public void onFailure(@NonNull Call<List<AdminReportUser>> call,
                                          @NonNull Throwable throwable) {
                        if (isAdded()) showError("Backend nije dostupan: " + throwable.getMessage());
                    }
                });
    }

    private void generateReport() {
        LocalDate from;
        LocalDate to;
        try {
            from = LocalDate.parse(dateFrom.getText().toString());
            to = LocalDate.parse(dateTo.getText().toString());
        } catch (Exception exception) {
            showError("Izaberite ispravne datume.");
            return;
        }
        if (from.isAfter(to)) {
            showError("Početni datum ne može biti posle krajnjeg.");
            return;
        }

        Long userId = null;
        String userType = null;
        if ("admin".equalsIgnoreCase(session.getRole())) {
            ReportTarget target = (ReportTarget) targetSpinner.getSelectedItem();
            if (target == null) {
                showError("Sačekajte da se učita izbor korisnika.");
                return;
            }
            userId = target.userId;
            userType = target.userType;
        }

        message.setText("Generisanje izveštaja...");
        message.setVisibility(View.VISIBLE);
        summary.setVisibility(View.GONE);
        ReportRequest request = new ReportRequest(from + "T00:00:00", to + "T00:00:00",
                userId, userType);
        ApiClient.getApi().generateReport(session.getAuthorizationHeader(), request)
                .enqueue(new Callback<>() {
                    @Override
                    public void onResponse(@NonNull Call<ReportResponse> call,
                                           @NonNull Response<ReportResponse> response) {
                        if (!isAdded()) return;
                        if (!response.isSuccessful() || response.body() == null) {
                            showError("Izveštaj nije generisan (HTTP " + response.code() + ").");
                            return;
                        }
                        showReport(response.body());
                    }

                    @Override
                    public void onFailure(@NonNull Call<ReportResponse> call,
                                          @NonNull Throwable throwable) {
                        if (isAdded()) showError("Backend nije dostupan: " + throwable.getMessage());
                    }
                });
    }

    private void showReport(ReportResponse report) {
        message.setVisibility(View.GONE);
        List<String> labels = new ArrayList<>();
        List<Double> rides = new ArrayList<>();
        List<Double> kilometers = new ArrayList<>();
        List<Double> money = new ArrayList<>();
        for (DailyReportStats day : report.getDailyStats()) {
            labels.add(day.getDate());
            rides.add((double) day.getNumberOfRides());
            kilometers.add(day.getTotalKilometers());
            money.add(day.getTotalMoney());
        }
        String moneyName = report.isEarnings() ? "Zarada (RSD)" : "Potrošnja (RSD)";
        ridesChart.setData("Broj vožnji po danu", labels, rides);
        kilometersChart.setData("Pređeni kilometri po danu", labels, kilometers);
        moneyChart.setData(moneyName + " po danu", labels, money);

        ReportSummary stats = report.getSummary();
        if (stats == null) {
            showError("Backend nije vratio zbirne podatke.");
            return;
        }
        summary.setText(String.format(Locale.getDefault(),
                "Kumulativno za period\nVožnje: %d  •  Kilometri: %.2f km  •  %s: %.2f RSD\n\n"
                        + "Dnevni prosek\nVožnje: %.2f  •  Kilometri: %.2f km  •  %s: %.2f RSD",
                stats.getTotalRides(), stats.getTotalKilometers(), moneyName, stats.getTotalMoney(),
                stats.getAvgRidesPerDay(), stats.getAvgKilometersPerDay(), moneyName,
                stats.getAvgMoneyPerDay()));
        summary.setVisibility(View.VISIBLE);
    }

    private void showError(String value) {
        message.setText(value);
        message.setVisibility(View.VISIBLE);
    }

    private static final class ReportTarget {
        private final String label;
        private final Long userId;
        private final String userType;

        private ReportTarget(String label, Long userId, String userType) {
            this.label = label;
            this.userId = userId;
            this.userType = userType;
        }
        @NonNull @Override public String toString() { return label; }
    }
}
