package com.example.mobilnaaplikacijatim29.ui.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.mobilnaaplikacijatim29.R;
import com.example.mobilnaaplikacijatim29.data.api.ApiClient;
import com.example.mobilnaaplikacijatim29.data.model.BlockNoteRequest;
import com.example.mobilnaaplikacijatim29.data.model.BlockableUser;
import com.example.mobilnaaplikacijatim29.data.session.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UserBlockingFragment extends Fragment {
    private SessionManager session;
    private LinearLayout usersContainer;
    private TextView message;
    private View driversButton;
    private View passengersButton;
    private boolean showingDrivers = true;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_user_blocking, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        session = new SessionManager(requireContext());
        usersContainer = view.findViewById(R.id.blocking_users_container);
        message = view.findViewById(R.id.blocking_message);
        driversButton = view.findViewById(R.id.blocking_show_drivers);
        passengersButton = view.findViewById(R.id.blocking_show_passengers);
        driversButton.setOnClickListener(v -> {
            showingDrivers = true;
            loadUsers();
        });
        passengersButton.setOnClickListener(v -> {
            showingDrivers = false;
            loadUsers();
        });
        loadUsers();
    }

    private void loadUsers() {
        driversButton.setEnabled(!showingDrivers);
        passengersButton.setEnabled(showingDrivers);
        usersContainer.removeAllViews();
        showMessage("Učitavanje " + (showingDrivers ? "vozača" : "putnika") + "...", false);
        Call<List<BlockableUser>> call = showingDrivers
                ? ApiClient.getApi().getAllDriversForBlocking(session.getAuthorizationHeader())
                : ApiClient.getApi().getAllPassengersForBlocking(session.getAuthorizationHeader());
        call.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<List<BlockableUser>> call,
                                   @NonNull Response<List<BlockableUser>> response) {
                if (!isAdded()) return;
                if (!response.isSuccessful() || response.body() == null) {
                    showMessage("Korisnici nisu učitani (HTTP " + response.code() + ").", true);
                    return;
                }
                message.setVisibility(View.GONE);
                if (response.body().isEmpty()) {
                    showMessage("Nema korisnika u ovoj grupi.", false);
                    return;
                }
                for (BlockableUser user : response.body()) addUserCard(user);
            }

            @Override
            public void onFailure(@NonNull Call<List<BlockableUser>> call,
                                  @NonNull Throwable throwable) {
                if (isAdded()) showMessage("Backend nije dostupan: " + throwable.getMessage(), true);
            }
        });
    }

    private void addUserCard(BlockableUser user) {
        MaterialCardView card = new MaterialCardView(requireContext());
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(0, 0, 0, dp(12));
        card.setLayoutParams(cardParams);
        card.setRadius(dp(12));
        card.setCardElevation(dp(2));
        card.setContentPadding(dp(16), dp(14), dp(16), dp(14));

        LinearLayout content = new LinearLayout(requireContext());
        content.setOrientation(LinearLayout.VERTICAL);
        TextView identity = new TextView(requireContext());
        identity.setText(safe(user.getName()) + " " + safe(user.getSurname())
                + "\n" + safe(user.getEmail()) + "\n"
                + (user.isBlocked() ? "Status: BLOKIRAN" : "Status: nije blokiran"));
        identity.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_TitleMedium);
        content.addView(identity);

        TextInputLayout noteLayout = new TextInputLayout(requireContext());
        noteLayout.setHint("Napomena / razlog blokiranja");
        LinearLayout.LayoutParams noteParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        noteParams.setMargins(0, dp(12), 0, 0);
        noteLayout.setLayoutParams(noteParams);
        TextInputEditText note = new TextInputEditText(noteLayout.getContext());
        note.setMinLines(2);
        note.setMaxLines(4);
        note.setText(user.getBlockReason() == null ? "" : user.getBlockReason());
        noteLayout.addView(note);
        content.addView(noteLayout);

        LinearLayout actions = new LinearLayout(requireContext());
        actions.setOrientation(LinearLayout.HORIZONTAL);
        MaterialButton statusButton = new MaterialButton(requireContext(), null,
                com.google.android.material.R.attr.materialButtonOutlinedStyle);
        statusButton.setText(user.isBlocked() ? "Odblokiraj" : "Blokiraj");
        statusButton.setLayoutParams(weightedParams());
        MaterialButton saveNote = new MaterialButton(requireContext());
        saveNote.setText("Sačuvaj napomenu");
        LinearLayout.LayoutParams saveParams = weightedParams();
        saveParams.setMargins(dp(8), 0, 0, 0);
        saveNote.setLayoutParams(saveParams);
        actions.addView(statusButton);
        actions.addView(saveNote);
        content.addView(actions);

        statusButton.setOnClickListener(v -> {
            String reason = note.getText() == null ? "" : note.getText().toString().trim();
            Call<BlockableUser> call = user.isBlocked()
                    ? ApiClient.getApi().unblockUser(session.getAuthorizationHeader(), user.getId())
                    : ApiClient.getApi().blockUser(session.getAuthorizationHeader(), user.getId(),
                            new BlockNoteRequest(reason));
            executeAction(call, user.isBlocked() ? "Korisnik je odblokiran." : "Korisnik je blokiran.");
        });
        saveNote.setOnClickListener(v -> {
            String reason = note.getText() == null ? "" : note.getText().toString().trim();
            executeAction(ApiClient.getApi().updateBlockNote(session.getAuthorizationHeader(),
                    user.getId(), new BlockNoteRequest(reason)), "Napomena je sačuvana.");
        });

        card.addView(content);
        usersContainer.addView(card);
    }

    private void executeAction(Call<BlockableUser> call, String successMessage) {
        call.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<BlockableUser> call,
                                   @NonNull Response<BlockableUser> response) {
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null) {
                    Toast.makeText(requireContext(), successMessage, Toast.LENGTH_SHORT).show();
                    loadUsers();
                } else {
                    showMessage("Operacija nije uspela (HTTP " + response.code() + ").", true);
                }
            }

            @Override
            public void onFailure(@NonNull Call<BlockableUser> call,
                                  @NonNull Throwable throwable) {
                if (isAdded()) showMessage("Backend nije dostupan: " + throwable.getMessage(), true);
            }
        });
    }

    private LinearLayout.LayoutParams weightedParams() {
        return new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
    }

    private void showMessage(String value, boolean error) {
        message.setText(value);
        message.setTextColor(requireContext().getColor(error
                ? android.R.color.holo_red_dark : android.R.color.holo_green_dark));
        message.setVisibility(View.VISIBLE);
    }

    private static String safe(String value) { return value == null ? "—" : value; }
    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
