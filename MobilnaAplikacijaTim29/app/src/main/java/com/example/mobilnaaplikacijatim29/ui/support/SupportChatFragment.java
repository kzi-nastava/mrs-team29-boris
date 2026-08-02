package com.example.mobilnaaplikacijatim29.ui.support;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.mobilnaaplikacijatim29.R;
import com.example.mobilnaaplikacijatim29.data.api.ApiClient;
import com.example.mobilnaaplikacijatim29.data.model.SupportMessage;
import com.example.mobilnaaplikacijatim29.data.model.SupportMessageRequest;
import com.example.mobilnaaplikacijatim29.data.session.SessionManager;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SupportChatFragment extends Fragment {
    private static final String ARG_USER_ID = "support_user_id";
    private static final String ARG_USER_LABEL = "support_user_label";
    private static final long REFRESH_INTERVAL_MS = 2000L;

    private final Handler refreshHandler = new Handler(Looper.getMainLooper());
    private final Runnable refresh = this::loadMessages;
    private SessionManager session;
    private LinearLayout messagesContainer;
    private ScrollView scrollView;
    private TextView messageView;
    private TextInputEditText input;
    private View sendButton;
    private Long conversationUserId;
    private boolean requestInProgress;
    private String lastSignature = "";

    public static SupportChatFragment forAdministrator(long userId, String userLabel) {
        Bundle arguments = new Bundle();
        arguments.putLong(ARG_USER_ID, userId);
        arguments.putString(ARG_USER_LABEL, userLabel);
        SupportChatFragment fragment = new SupportChatFragment();
        fragment.setArguments(arguments);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_support_chat, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        session = new SessionManager(requireContext());
        messagesContainer = view.findViewById(R.id.support_messages);
        scrollView = view.findViewById(R.id.support_messages_scroll);
        messageView = view.findViewById(R.id.support_chat_message);
        input = view.findViewById(R.id.support_message_input);
        sendButton = view.findViewById(R.id.support_send);

        if (getArguments() != null && getArguments().containsKey(ARG_USER_ID)) {
            conversationUserId = getArguments().getLong(ARG_USER_ID);
            String label = getArguments().getString(ARG_USER_LABEL, "korisnik");
            ((TextView) view.findViewById(R.id.support_chat_title))
                    .setText("Podrška — " + label);
        }
        sendButton.setOnClickListener(v -> sendMessage());
    }

    private void loadMessages() {
        if (!isResumed() || requestInProgress) return;
        requestInProgress = true;
        ApiClient.getApi().getSupportMessages(session.getAuthorizationHeader(), conversationUserId)
                .enqueue(new Callback<>() {
                    @Override
                    public void onResponse(@NonNull Call<List<SupportMessage>> call,
                                           @NonNull Response<List<SupportMessage>> response) {
                        requestInProgress = false;
                        if (!isAdded()) return;
                        if (!response.isSuccessful() || response.body() == null) {
                            showMessage("Razgovor nije učitan (HTTP " + response.code() + ").", true);
                            scheduleRefresh();
                            return;
                        }
                        renderIfChanged(response.body());
                        scheduleRefresh();
                    }

                    @Override
                    public void onFailure(@NonNull Call<List<SupportMessage>> call,
                                          @NonNull Throwable throwable) {
                        requestInProgress = false;
                        if (isAdded() && messagesContainer.getChildCount() == 0) {
                            showMessage("Backend nije dostupan: " + throwable.getMessage(), true);
                        }
                        scheduleRefresh();
                    }
                });
    }

    private void renderIfChanged(List<SupportMessage> messages) {
        String signature = messages.size() + ":"
                + (messages.isEmpty() ? "" : messages.get(messages.size() - 1).getId());
        if (signature.equals(lastSignature)) return;
        lastSignature = signature;
        messagesContainer.removeAllViews();
        messageView.setVisibility(messages.isEmpty() ? View.VISIBLE : View.GONE);
        if (messages.isEmpty()) {
            messageView.setText("Još nema poruka. Pošaljite prvo pitanje podršci.");
            return;
        }
        for (SupportMessage message : messages) addMessage(message);
        scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
    }

    private void addMessage(SupportMessage message) {
        boolean mine = message.getSenderId() != null
                && message.getSenderId() == session.getUserId();
        MaterialCardView bubble = new MaterialCardView(requireContext());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.gravity = mine ? Gravity.END : Gravity.START;
        params.setMargins(mine ? dp(44) : 0, 0, mine ? 0 : dp(44), dp(8));
        bubble.setLayoutParams(params);
        bubble.setRadius(dp(14));
        bubble.setCardElevation(dp(1));
        bubble.setContentPadding(dp(12), dp(9), dp(12), dp(9));

        TextView text = new TextView(requireContext());
        text.setMaxWidth((int) (getResources().getDisplayMetrics().widthPixels * 0.78));
        String author = mine ? "Vi" : "admin".equalsIgnoreCase(message.getSenderRole())
                ? "Podrška" : safe(message.getSenderName());
        text.setText(author + "  •  " + displayDate(message.getSentAt())
                + "\n" + safe(message.getMessage()));
        bubble.addView(text);
        messagesContainer.addView(bubble);
    }

    private void sendMessage() {
        String value = input.getText() == null ? "" : input.getText().toString().trim();
        if (value.isEmpty()) {
            showMessage("Unesite poruku.", true);
            return;
        }
        if (value.length() > 2000) {
            showMessage("Poruka može imati najviše 2000 znakova.", true);
            return;
        }
        sendButton.setEnabled(false);
        ApiClient.getApi().sendSupportMessage(session.getAuthorizationHeader(),
                        new SupportMessageRequest(conversationUserId, value))
                .enqueue(new Callback<>() {
                    @Override
                    public void onResponse(@NonNull Call<SupportMessage> call,
                                           @NonNull Response<SupportMessage> response) {
                        if (!isAdded()) return;
                        sendButton.setEnabled(true);
                        if (!response.isSuccessful()) {
                            showMessage("Poruka nije poslata (HTTP " + response.code() + ").", true);
                            return;
                        }
                        input.setText("");
                        messageView.setVisibility(View.GONE);
                        lastSignature = "";
                        loadMessages();
                    }

                    @Override
                    public void onFailure(@NonNull Call<SupportMessage> call,
                                          @NonNull Throwable throwable) {
                        if (!isAdded()) return;
                        sendButton.setEnabled(true);
                        showMessage("Poruka nije poslata: " + throwable.getMessage(), true);
                    }
                });
    }

    private void scheduleRefresh() {
        refreshHandler.removeCallbacks(refresh);
        if (isResumed()) refreshHandler.postDelayed(refresh, REFRESH_INTERVAL_MS);
    }

    private void showMessage(String value, boolean error) {
        messageView.setText(value);
        messageView.setTextColor(requireContext().getColor(error
                ? android.R.color.holo_red_dark : android.R.color.secondary_text_dark));
        messageView.setVisibility(View.VISIBLE);
    }

    private static String displayDate(String value) {
        if (value == null || value.isBlank()) return "";
        String normalized = value.replace('T', ' ');
        return normalized.length() > 16 ? normalized.substring(0, 16) : normalized;
    }
    private static String safe(String value) { return value == null ? "" : value; }
    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshHandler.removeCallbacks(refresh);
        loadMessages();
    }

    @Override
    public void onPause() {
        refreshHandler.removeCallbacks(refresh);
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        refreshHandler.removeCallbacks(refresh);
        messagesContainer = null;
        scrollView = null;
        super.onDestroyView();
    }
}
