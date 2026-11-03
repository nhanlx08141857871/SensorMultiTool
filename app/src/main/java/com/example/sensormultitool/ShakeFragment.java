package com.example.sensormultitool;

import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.Random;

public class ShakeFragment extends Fragment implements SensorEventListener {

    private static final float SHAKE_THRESHOLD_GRAVITY = 2.75f;
    private static final int SHAKE_SLOP_TIME_MS = 500;

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Vibrator vibrator;

    private LinearLayout rootLayout;
    private TextView tvShakeCount, tvGForce, tvAxisValues, tvSensorStatus;
    private Button btnReset;

    private long shakeTimestamp = 0;
    private int shakeCount = 0;
    private final Random random = new Random();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_shake, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rootLayout = view.findViewById(R.id.shake_root);
        tvShakeCount = view.findViewById(R.id.tvShakeCount);
        tvGForce = view.findViewById(R.id.tvGForce);
        tvAxisValues = view.findViewById(R.id.tvAxisValues);
        tvSensorStatus = view.findViewById(R.id.tvSensorStatus);
        btnReset = view.findViewById(R.id.btnReset);

        Context context = requireContext();
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }
        vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);

        if (accelerometer == null) {
            tvSensorStatus.setText("Thiết bị không hỗ trợ cảm biến gia tốc!");
            btnReset.setEnabled(false);
        }

        btnReset.setOnClickListener(v -> resetCount());
    }

    @Override
    public void onResume() {
        super.onResume();
        if (sensorManager != null && accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() != Sensor.TYPE_ACCELEROMETER) return;

        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];

        tvAxisValues.setText(String.format("X: %.2f  Y: %.2f  Z: %.2f", x, y, z));

        float gX = x / SensorManager.GRAVITY_EARTH;
        float gY = y / SensorManager.GRAVITY_EARTH;
        float gZ = z / SensorManager.GRAVITY_EARTH;

        float gForce = (float) Math.sqrt(gX * gX + gY * gY + gZ * gZ);
        tvGForce.setText(String.format("Lực: %.2f g", gForce));

        if (gForce > SHAKE_THRESHOLD_GRAVITY) {
            long now = System.currentTimeMillis();
            if (shakeTimestamp + SHAKE_SLOP_TIME_MS > now) {
                return;
            }
            shakeTimestamp = now;
            onShakeDetected();
        }
    }

    private void onShakeDetected() {
        shakeCount++;
        tvShakeCount.setText("Số lần lắc: " + shakeCount);
        tvSensorStatus.setText("Phát hiện lắc!");

        if (rootLayout != null) {
            int randomColor = Color.rgb(
                    180 + random.nextInt(60),
                    180 + random.nextInt(60),
                    180 + random.nextInt(60)
            );
            rootLayout.setBackgroundColor(randomColor);
        }

        if (vibrator != null && vibrator.hasVibrator()) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(200);
            }
        }
    }

    private void resetCount() {
        shakeCount = 0;
        tvShakeCount.setText("Số lần lắc: 0");
        tvSensorStatus.setText("Lắc điện thoại để kiểm tra");
        if (rootLayout != null) {
            rootLayout.setBackgroundColor(Color.WHITE);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // không cần xử lý
    }
}