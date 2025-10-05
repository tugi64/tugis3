package com.example.tugis3.ui.tools;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.tugis3.R;

public class PostProcessPointsActivity extends AppCompatActivity {
    private EditText etOffsetX, etOffsetY, etOffsetH, etStartTime, etEndTime;
    private TextView tvResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_process_points);
        etOffsetX = findViewById(R.id.etOffsetX);
        etOffsetY = findViewById(R.id.etOffsetY);
        etOffsetH = findViewById(R.id.etOffsetH);
        etStartTime = findViewById(R.id.etStartTime);
        etEndTime = findViewById(R.id.etEndTime);
        tvResult = findViewById(R.id.tvResult);
        Button btnApplyOffset = findViewById(R.id.btnApplyOffset);
        Button btnMarkerCalibration = findViewById(R.id.btnMarkerCalibration);
        Button btnRefresh = findViewById(R.id.btnRefresh);

        btnApplyOffset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String offsetX = etOffsetX.getText().toString();
                String offsetY = etOffsetY.getText().toString();
                String offsetH = etOffsetH.getText().toString();
                tvResult.setText("Offset uygulandı: X=" + offsetX + " Y=" + offsetY + " H=" + offsetH);
            }
        });

        btnMarkerCalibration.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tvResult.setText("Marker kalibrasyonu yapıldı");
            }
        });

        btnRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tvResult.setText("Noktalar yenilendi");
            }
        });
    }
}
