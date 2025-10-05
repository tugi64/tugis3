package com.example.tugis3.ui.tools;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.tugis3.R;

public class COGOActivity extends AppCompatActivity {
    private EditText etCOGOInput;
    private TextView tvCOGOResult;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cogo);
        etCOGOInput = findViewById(R.id.etCOGOInput);
        tvCOGOResult = findViewById(R.id.tvCOGOResult);
        Button btnCalculateCOGO = findViewById(R.id.btnCalculateCOGO);
        btnCalculateCOGO.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String input = etCOGOInput.getText().toString();
                tvCOGOResult.setText("COGO sonucu: " + input);
                // Gerçek COGO hesaplaması burada yapılacak
            }
        });
    }
}
