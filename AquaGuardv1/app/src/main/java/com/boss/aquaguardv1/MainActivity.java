package com.boss.aquaguardv1;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    // UI components
    private TextView txtCrop, txtLocation, txtTempToday, txtSoilMoistureToday, txtWindToday;
    private LinearLayout forecastContainer, adviceContainer;
    private LineChart lineChartRain;
    private Button btnFetch;

    // HTTP client for API calls
    private OkHttpClient client = new OkHttpClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize UI components
        initUI();

        // Set up chart appearance
        setupLineChart();

        // Set button click listener
        btnFetch.setOnClickListener(v -> fetchDataButtonClicked());
    }

    /**
     * Initialize all UI elements
     */
    private void initUI() {
        txtCrop = findViewById(R.id.txtCrop);
        txtLocation = findViewById(R.id.txtLocation);
        txtTempToday = findViewById(R.id.txtTempToday);
        txtSoilMoistureToday = findViewById(R.id.txtSoilMoistureToday);
        txtWindToday = findViewById(R.id.txtWindToday);
        forecastContainer = findViewById(R.id.forecastContainer);
        adviceContainer = findViewById(R.id.adviceContainer);
        lineChartRain = findViewById(R.id.LineChartRain);
        btnFetch = findViewById(R.id.btnFetch);
    }

    /**
     * Set up LineChart style and behavior
     */
    private void setupLineChart() {
        lineChartRain.getDescription().setEnabled(false);
        lineChartRain.setDrawGridBackground(false);
        lineChartRain.setDrawBorders(false);

        XAxis xAxis = lineChartRain.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);

        YAxis leftAxis = lineChartRain.getAxisLeft();
        leftAxis.setDrawGridLines(false);
        lineChartRain.getAxisRight().setEnabled(false);

        lineChartRain.setTouchEnabled(true);
        lineChartRain.setPinchZoom(true);
        lineChartRain.setDoubleTapToZoomEnabled(false);
    }

    /**
     * Handle Fetch button click
     */
    private void fetchDataButtonClicked() {
        String cropName = "Wheat";
        String locationName = "Pokhara, Nepal";

        txtCrop.setText("Crop: " + cropName);
        txtLocation.setText("Location: " + locationName);

        double latitude = 28.2096;
        double longitude = 83.9856;

        fetchWeatherData(latitude, longitude, cropName);
    }

    /**
     * Fetch weather data from Open-Meteo API
     */
    private void fetchWeatherData(double lat, double lon, String crop) {
        String url = "https://api.open-meteo.com/v1/forecast?latitude=" + lat
                + "&longitude=" + lon
                + "&hourly=temperature_2m,soil_moisture_0_1cm,soil_moisture_1_3cm,soil_moisture_3_9cm,wind_speed_10m,precipitation"
                + "&timezone=auto";

        Request request = new Request.Builder().url(url).build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "API call failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "API Error: " + response.code(), Toast.LENGTH_SHORT).show());
                    return;
                }

                try {
                    String jsonData = response.body().string();
                    parseAndDisplayData(jsonData);
                } catch (Exception e) {
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "Parsing error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * Parse JSON response and update UI
     */
    private void parseAndDisplayData(String jsonData) throws Exception {
        JSONObject json = new JSONObject(jsonData);
        JSONObject hourly = json.getJSONObject("hourly");

        JSONArray temps = hourly.getJSONArray("temperature_2m");
        JSONArray moist0_1 = hourly.getJSONArray("soil_moisture_0_1cm");
        JSONArray moist1_3 = hourly.getJSONArray("soil_moisture_1_3cm");
        JSONArray moist3_9 = hourly.getJSONArray("soil_moisture_3_9cm");
        JSONArray winds = hourly.getJSONArray("wind_speed_10m");
        JSONArray rain = hourly.getJSONArray("precipitation");

        runOnUiThread(() -> updateUI(temps, moist0_1, moist1_3, moist3_9, winds, rain));
    }

    /**
     * Update all UI elements including forecast cards, advice cards, and chart
     */
    private void updateUI(JSONArray temps, JSONArray moist0_1, JSONArray moist1_3, JSONArray moist3_9,
                          JSONArray winds, JSONArray rain) {

        // Show today's data
        txtTempToday.setText(temps.optString(0) + "°C");
        double soilAvg = (moist0_1.optDouble(0, 0) + moist1_3.optDouble(0, 0) + moist3_9.optDouble(0, 0)) / 3;
        txtSoilMoistureToday.setText(String.format("%.2f", soilAvg));
        txtWindToday.setText(winds.optString(0) + " km/h");

        // Clear previous forecast/advice/cards
        forecastContainer.removeAllViews();
        adviceContainer.removeAllViews();
        lineChartRain.clear();

        OnnxHelper mlModel = null;
        try {
            mlModel = new OnnxHelper(MainActivity.this);
        } catch (Exception e) {
            e.printStackTrace();
        }

        ArrayList<Entry> rainEntries = new ArrayList<>();
        ArrayList<String> adviceList = new ArrayList<>();

        for (int day = 0; day < 7; day++) {
            int hourStart = day * 24;
            int hourEnd = Math.min(hourStart + 24, temps.length());

            double avgTemp = 0, avgWind = 0, avgRain = 0, avgSoil = 0;
            int count = 0;

            for (int h = hourStart; h < hourEnd; h++) {
                avgTemp += temps.optDouble(h, 0);
                avgWind += winds.optDouble(h, 0);
                avgRain += rain.optDouble(h, 0);
                avgSoil += (moist0_1.optDouble(h, 0) + moist1_3.optDouble(h, 0) + moist3_9.optDouble(h, 0)) / 3;
                count++;
            }

            avgTemp /= count;
            avgWind /= count;
            avgRain /= count;
            avgSoil /= count;

            // Add forecast card
            forecastContainer.addView(createForecastCard(day, avgTemp, avgWind));

            // Add advice card
            String advice = generateAdvice(avgSoil, avgTemp, mlModel);
            adviceList.add(advice);
            adviceContainer.addView(createAdviceCard(advice));

            // Add rain chart entry
            rainEntries.add(new Entry(day, (float) avgRain));
        }

        // Populate chart with data
        setupChartData(rainEntries, adviceList);
    }

    /**
     * Create a forecast card view
     */
    private CardView createForecastCard(int day, double avgTemp, double avgWind) {
        CardView card = new CardView(this);
        card.setRadius(20);
        card.setCardElevation(8);
        card.setUseCompatPadding(true);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(16, 16, 16, 16);
        card.setLayoutParams(params);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(20, 20, 20, 20);
        layout.setGravity(Gravity.CENTER);

        TextView dayText = new TextView(this);
        dayText.setText("Day " + (day + 1));
        dayText.setGravity(Gravity.CENTER);
        dayText.setTextSize(18);
        layout.addView(dayText);

        TextView detailsText = new TextView(this);
        detailsText.setText("Temp: " + String.format("%.1f°C", avgTemp) +
                "\nWind: " + String.format("%.1f km/h", avgWind));
        layout.addView(detailsText);

        card.addView(layout);
        return card;
    }

    /**
     * Generate advice string for the day
     */
    private String generateAdvice(double avgSoil, double avgTemp, OnnxHelper mlModel) {
        String advice = "";

        if (avgSoil < 0.3) advice += "💧 Soil dry. ";
        else advice += "✅ Soil moisture sufficient. ";

        if (mlModel != null) {
            try {
                float scaledSoil = (float) (avgSoil * 1000.0f);
                float scaledTemp = (float) avgTemp;
                float[] mlInput = new float[]{scaledSoil, scaledTemp};
                float[] mlOutput = mlModel.predict(mlInput, mlInput.length);

                if (mlOutput[0] <= 0.5) advice += "⚠️ Irrigation needed today!";
                else advice += "✅ No irrigation needed today.";

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return advice;
    }

    /**
     * Create a CardView for advice text
     */
    private CardView createAdviceCard(String advice) {
        CardView card = new CardView(this);
        card.setRadius(20);
        card.setCardElevation(6);
        card.setUseCompatPadding(true);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(16, 16, 16, 16);
        card.setLayoutParams(params);

        TextView adviceText = new TextView(this);
        adviceText.setText(advice);
        adviceText.setPadding(20, 20, 20, 20);
        card.addView(adviceText);

        return card;
    }

    /**
     * Set up the chart with rain forecast and marker
     */
    private void setupChartData(ArrayList<Entry> rainEntries, ArrayList<String> adviceList) {
        LineDataSet lineDataSet = new LineDataSet(rainEntries, "Rain Forecast (mm)");
        lineDataSet.setColor(ContextCompat.getColor(this, R.color.purple_500));
        lineDataSet.setCircleColor(ContextCompat.getColor(this, R.color.purple_700));
        lineDataSet.setCircleRadius(6f);
        lineDataSet.setLineWidth(3f);
        lineDataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        lineDataSet.setDrawFilled(true);
        lineDataSet.setDrawValues(true);

        // Gradient fill
        Drawable drawable = ContextCompat.getDrawable(this, R.drawable.gradient_line);
        lineDataSet.setFillDrawable(drawable);

        LineData lineData = new LineData(lineDataSet);
        lineChartRain.setData(lineData);

        // Custom marker view
        CustomMarkerView marker = new CustomMarkerView(this, R.layout.marker_view, rainEntries, adviceList);
        lineChartRain.setMarker(marker);

        lineChartRain.invalidate();
    }
}
