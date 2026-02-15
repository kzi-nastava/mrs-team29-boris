package com.example.mobilnaaplikacijatim29;

import android.os.Bundle;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    // Use 10.0.2.2 for the Android emulator. For a physical phone, replace it
    // with the computer's IPv4 address on the same Wi-Fi network.
    private static final String BACKEND_URL = "http://192.168.0.29:8080/api/rides/ping";
    private final ExecutorService networkExecutor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        TextView backendStatus = findViewById(R.id.backend_status);
        checkBackendConnection(backendStatus);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void checkBackendConnection(TextView backendStatus) {
        networkExecutor.execute(() -> {
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) new URL(BACKEND_URL).openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);

                int responseCode = connection.getResponseCode();
                InputStream responseStream = responseCode >= 200 && responseCode < 400
                        ? connection.getInputStream()
                        : connection.getErrorStream();
                String responseBody = readResponse(responseStream);

                runOnUiThread(() -> backendStatus.setText(
                        "Backend connected\nHTTP " + responseCode + "\n" + responseBody));
            } catch (Exception exception) {
                runOnUiThread(() -> backendStatus.setText(
                        "Backend connection failed\n" + exception.getMessage()));
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }

    private String readResponse(InputStream responseStream) throws Exception {
        if (responseStream == null) {
            return "(empty response)";
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(responseStream, StandardCharsets.UTF_8))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            return response.toString();
        }
    }

    @Override
    protected void onDestroy() {
        networkExecutor.shutdownNow();
        super.onDestroy();
    }
}
