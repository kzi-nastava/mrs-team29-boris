package com.example.mobilnaaplikacijatim29.ui.support;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.mobilnaaplikacijatim29.MainActivity;
import com.example.mobilnaaplikacijatim29.R;
import com.example.mobilnaaplikacijatim29.data.api.ApiClient;
import com.example.mobilnaaplikacijatim29.data.model.SupportConversation;
import com.example.mobilnaaplikacijatim29.data.session.SessionManager;
import com.google.android.material.card.MaterialCardView;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SupportConversationsFragment extends Fragment {
    private static final long REFRESH_INTERVAL_MS = 3000L;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable refresh = this::loadConversations;
    private LinearLayout container;
    private TextView message;
    private SessionManager session;
    private boolean loading;
    private String signature = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup parent,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_support_conversations, parent, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        session = new SessionManager(requireContext());
        container = view.findViewById(R.id.support_conversations_container);
        message = view.findViewById(R.id.support_conversations_message);
    }

    private void loadConversations() {
        if (!isResumed() || loading) return;
        loading = true;
        ApiClient.getApi().getSupportConversations(session.getAuthorizationHeader())
                .enqueue(new Callback<>() {
                    @Override
                    public void onResponse(@NonNull Call<List<SupportConversation>> call,
                                           @NonNull Response<List<SupportConversation>> response) {
                        loading = false;
                        if (!isAdded()) return;
                        if (!response.isSuccessful() || response.body() == null) {
                            message.setText("Razgovori nisu učitani (HTTP " + response.code() + ").");
                            message.setVisibility(View.VISIBLE);
                        } else render(response.body());
                        schedule();
                    }

                    @Override
                    public void onFailure(@NonNull Call<List<SupportConversation>> call,
                                          @NonNull Throwable throwable) {
                        loading = false;
                        if (isAdded() && container.getChildCount() == 0) {
                            message.setText("Backend nije dostupan: " + throwable.getMessage());
                            message.setVisibility(View.VISIBLE);
                        }
                        schedule();
                    }
                });
    }

    private void render(List<SupportConversation> conversations) {
        StringBuilder signatureBuilder = new StringBuilder();
        for (SupportConversation item : conversations) {
            signatureBuilder.append(item.getUserId()).append(':')
                    .append(item.getLastMessageAt()).append(':')
                    .append(item.getUnreadCount()).append('|');
        }
        String nextSignature = signatureBuilder.toString();
        if (nextSignature.equals(signature)) return;
        signature = nextSignature;
        container.removeAllViews();
        message.setVisibility(conversations.isEmpty() ? View.VISIBLE : View.GONE);
        if (conversations.isEmpty()) {
            message.setText("Još nema razgovora sa korisnicima.");
            return;
        }
        for (SupportConversation item : conversations) addCard(item);
    }

    private void addCard(SupportConversation item) {
        MaterialCardView card = new MaterialCardView(requireContext());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, dp(10));
        card.setLayoutParams(params);
        card.setRadius(dp(12));
        card.setCardElevation(dp(2));
        card.setContentPadding(dp(15), dp(13), dp(15), dp(13));
        card.setClickable(true);
        card.setFocusable(true);

        TextView text = new TextView(requireContext());
        String unread = item.getUnreadCount() > 0 ? "  •  NOVO: " + item.getUnreadCount() : "";
        text.setText(safe(item.getName()) + " " + safe(item.getSurname()) + unread
                + "\n" + safe(item.getEmail()) + " (" + role(item.getRole()) + ")"
                + "\n" + safe(item.getLastMessage()));
        text.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyLarge);
        card.addView(text);
        card.setOnClickListener(v -> ((MainActivity) requireActivity()).navigateToSupportChat(
                item.getUserId(), safe(item.getName()) + " " + safe(item.getSurname())));
        container.addView(card);
    }

    private void schedule() {
        handler.removeCallbacks(refresh);
        if (isResumed()) handler.postDelayed(refresh, REFRESH_INTERVAL_MS);
    }
    private static String role(String role) {
        return "driver".equalsIgnoreCase(role) ? "vozač" : "putnik";
    }
    private static String safe(String value) { return value == null ? "" : value; }
    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override public void onResume() { super.onResume(); loadConversations(); }
    @Override public void onPause() { handler.removeCallbacks(refresh); super.onPause(); }
    @Override public void onDestroyView() {
        handler.removeCallbacks(refresh); container = null; super.onDestroyView();
    }
}
