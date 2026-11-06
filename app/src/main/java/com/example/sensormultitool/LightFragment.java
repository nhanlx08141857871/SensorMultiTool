package com.example.sensormultitool;

import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class LightFragment extends Fragment implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor lightSensor;

    private LinearLayout rootLayout;
    private TextView tvLuxValue, tvLightCategory, tvSensorStatus;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_light, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rootLayout = view.findViewById(R.id.light_root);
        tvLuxValue = view.findViewById(R.id.tvLuxValue);
        tvLightCategory = view.findViewById(R.id.tvLightCategory);
        tvSensorStatus = view.findViewById(R.id.tvSensorStatus);

        Context context = requireContext();
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        }

        if (lightSensor == null) {
            tvSensorStatus.setText("Thiết bị không hỗ trợ cảm biến ánh sáng!");
            tvLuxValue.setText("0 Lux");
            tvLightCategory.setText("Không xác định");
        } else {
            tvSensorStatus.setText("Đang đo độ sáng môi trường...");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (sensorManager != null && lightSensor != null) {
            sensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
        restoreScreenBrightness();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() != Sensor.TYPE_LIGHT) return;

        float lux = event.values[0];
        tvLuxValue.setText(String.format("%.1f Lux", lux));

        updateEnvironmentUI(lux);

        adjustScreenBrightness(lux);
    }

    private void updateEnvironmentUI(float lux) {
        if (lux < 10) {
            tvLightCategory.setText("Môi trường: Rất tối (Tối đen / Đêm)");
            if (rootLayout != null) rootLayout.setBackgroundColor(Color.parseColor("#212121"));
            tvLuxValue.setTextColor(Color.WHITE);
            tvLightCategory.setTextColor(Color.LTGRAY);
            tvSensorStatus.setTextColor(Color.LTGRAY);
        } else if (lux < 500) {
            tvLightCategory.setText("Môi trường: Trong nhà / Văn phòng");
            if (rootLayout != null) rootLayout.setBackgroundColor(Color.parseColor("#F5F5F5"));
            tvLuxValue.setTextColor(Color.BLACK);
            tvLightCategory.setTextColor(Color.DKGRAY);
            tvSensorStatus.setTextColor(Color.DKGRAY);
        } else if (lux < 10000) {
            tvLightCategory.setText("Môi trường: Ánh sáng ban ngày (Bóng râm)");
            if (rootLayout != null) rootLayout.setBackgroundColor(Color.parseColor("#FFF9C4"));
            tvLuxValue.setTextColor(Color.BLACK);
            tvLightCategory.setTextColor(Color.DKGRAY);
            tvSensorStatus.setTextColor(Color.DKGRAY);
        } else {
            tvLightCategory.setText("Môi trường: Nắng gắt trực tiếp");
            if (rootLayout != null) rootLayout.setBackgroundColor(Color.parseColor("#FFE082"));
            tvLuxValue.setTextColor(Color.BLACK);
            tvLightCategory.setTextColor(Color.DKGRAY);
            tvSensorStatus.setTextColor(Color.DKGRAY);
        }
    }

    private void adjustScreenBrightness(float lux) {
        if (getActivity() == null) return;

        float brightness;
        if (lux <= 0) {
            brightness = 0.05f;
        } else if (lux >= 10000) {
            brightness = 1.0f;
        } else {
            brightness = 0.05f + (lux / 10000.0f) * 0.95f;
        }

        WindowManager.LayoutParams layoutParams = getActivity().getWindow().getAttributes();
        layoutParams.screenBrightness = brightness;
        getActivity().getWindow().setAttributes(layoutParams);
    }

    private void restoreScreenBrightness() {
        if (getActivity() == null) return;

        WindowManager.LayoutParams layoutParams = getActivity().getWindow().getAttributes();
        layoutParams.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE;
        getActivity().getWindow().setAttributes(layoutParams);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }
}