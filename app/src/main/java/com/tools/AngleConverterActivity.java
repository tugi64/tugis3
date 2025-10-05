package com.example.tugis3.ui.tools;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.tugis3.R;

public class AngleConverterActivity extends AppCompatActivity {
    private EditText etInputAngle;
    private TextView tvConvertedAngle;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_angle_converter);
        etInputAngle = findViewById(R.id.etInputAngle);
        tvConvertedAngle = findViewById(R.id.tvConvertedAngle);
        Button btnConvertAngle = findViewById(R.id.btnConvertAngle);
        btnConvertAngle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String angle = etInputAngle.getText().toString();
                tvConvertedAngle.setText("Dönüştürüldü: " + angle);
                // Gerçek açı dönüşümü burada yapılacak
            }
        });
    }
}
