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
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;

import java.util.Locale;

public class CompassFragment extends Fragment implements SensorEventListener {

    private static final float FILTER_ALPHA = 0.15f;

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor magnetometer;

    private View compassDialContainer;
    private TextView tvDegrees, tvDirection, tvMagneticField, tvSensorStatus;
    private SwitchCompat switchLowPass;

    private float[] gravityValues = new float[3];
    private float[] geomagneticValues = new float[3];
    private boolean hasGravity = false;
    private boolean hasGeomagnetic = false;

    private final float[] rMatrix = new float[9];
    private final float[] iMatrix = new float[9];
    private final float[] orientationValues = new float[3];

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_compass, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        compassDialContainer = view.findViewById(R.id.compassDialContainer);
        tvDegrees = view.findViewById(R.id.tvDegrees);
        tvDirection = view.findViewById(R.id.tvDirection);
        tvMagneticField = view.findViewById(R.id.tvMagneticField);
        tvSensorStatus = view.findViewById(R.id.tvSensorStatus);
        switchLowPass = view.findViewById(R.id.switchLowPass);

        Context context = requireContext();
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);

        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        }

        if (accelerometer == null || magnetometer == null) {
            tvSensorStatus.setText("Thiết bị thiếu cảm biến Gia tốc hoặc Từ trường!");
            tvSensorStatus.setTextColor(Color.parseColor("#FF3B30"));
        } else {
            tvSensorStatus.setText("Cảm biến: Sẵn sàng");
            tvSensorStatus.setTextColor(Color.parseColor("#34C759"));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (sensorManager != null) {
            if (accelerometer != null) {
                sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
            }
            if (magnetometer != null) {
                sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI);
            }
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
        boolean useFilter = switchLowPass != null && switchLowPass.isChecked();

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            if (useFilter && hasGravity) {
                gravityValues = applyLowPassFilter(event.values, gravityValues);
            } else {
                gravityValues = event.values.clone();
            }
            hasGravity = true;
        }

        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            if (useFilter && hasGeomagnetic) {
                geomagneticValues = applyLowPassFilter(event.values, geomagneticValues);
            } else {
                geomagneticValues = event.values.clone();
            }
            hasGeomagnetic = true;

            updateMagneticFieldStrength(event.values);
        }

        if (hasGravity && hasGeomagnetic) {
            boolean success = SensorManager.getRotationMatrix(rMatrix, iMatrix, gravityValues, geomagneticValues);
            if (success) {
                SensorManager.getOrientation(rMatrix, orientationValues);

                float azimuthInRadians = orientationValues[0];
                float azimuthInDegrees = (float) Math.toDegrees(azimuthInRadians);

                azimuthInDegrees = (azimuthInDegrees + 360f) % 360f;

                updateCompassUI(azimuthInDegrees);
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    private float[] applyLowPassFilter(float[] input, float[] output) {
        if (output == null)
            return input.clone();
        for (int i = 0; i < input.length; i++) {
            output[i] = output[i] + FILTER_ALPHA * (input[i] - output[i]);
        }
        return output;
    }

    private void updateCompassUI(float degrees) {
        int roundedDegrees = Math.round(degrees) % 360;
        if (tvDegrees != null) {
            tvDegrees.setText(String.format(Locale.getDefault(), "%d°", roundedDegrees));
        }
        if (tvDirection != null) {
            tvDirection.setText(getDirectionLabel(roundedDegrees));
        }

        if (compassDialContainer != null) {
            compassDialContainer.setRotation(-degrees);
        }
    }

    private String getDirectionLabel(int degrees) {
        if (degrees >= 338 || degrees < 23) {
            return "Bắc (N)";
        } else if (degrees < 68) {
            return "Đông Bắc (NE)";
        } else if (degrees < 113) {
            return "Đông (E)";
        } else if (degrees < 158) {
            return "Đông Nam (SE)";
        } else if (degrees < 203) {
            return "Nam (S)";
        } else if (degrees < 248) {
            return "Tây Nam (SW)";
        } else if (degrees < 293) {
            return "Tây (W)";
        } else {
            return "Tây Bắc (NW)";
        }
    }

    private void updateMagneticFieldStrength(float[] magValues) {
        double magnitude = Math.sqrt(
                magValues[0] * magValues[0] +
                        magValues[1] * magValues[1] +
                        magValues[2] * magValues[2]);

        if (tvMagneticField != null) {
            tvMagneticField.setText(String.format(Locale.getDefault(),
                    "Từ trường: %.1f µT", magnitude));
        }
    }
}