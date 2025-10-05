package com.example.tugis3.ui.tools;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import com.example.tugis3.R;

public class ToolsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tools);

        Button btnLocalization = findViewById(R.id.btnLocalization);
        Button btnCoordinateConverter = findViewById(R.id.btnCoordinateConverter);
        Button btnAngleConverter = findViewById(R.id.btnAngleConverter);
        Button btnCOGO = findViewById(R.id.btnCOGO);
        Button btnFTPShare = findViewById(R.id.btnFTPShare);
        Button btnFileShare = findViewById(R.id.btnFileShare);
        Button btnPostProcessPoints = findViewById(R.id.btnPostProcessPoints);

        btnLocalization.setOnClickListener(v ->
            startActivity(new Intent(this, LocalizationActivity.class)));

        btnCoordinateConverter.setOnClickListener(v ->
            startActivity(new Intent(this, CoordinateConverterActivity.class)));

        btnAngleConverter.setOnClickListener(v ->
            startActivity(new Intent(this, AngleConverterActivity.class)));

        btnCOGO.setOnClickListener(v ->
            startActivity(new Intent(this, COGOActivity.class)));

        btnFTPShare.setOnClickListener(v ->
            startActivity(new Intent(this, FTPShareActivity.class)));

        btnFileShare.setOnClickListener(v ->
            startActivity(new Intent(this, FileShareActivity.class)));

        btnPostProcessPoints.setOnClickListener(v ->
            startActivity(new Intent(this, PostProcessPointsActivity.class)));
    }
}
