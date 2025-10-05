package com.example.tugis3.ui.tools;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.tugis3.R;

public class FileShareActivity extends AppCompatActivity {
    private EditText etFilePath;
    private TextView tvFileShareResult;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_share);
        etFilePath = findViewById(R.id.etFilePath);
        tvFileShareResult = findViewById(R.id.tvFileShareResult);
        Button btnShareFile = findViewById(R.id.btnShareFile);
        btnShareFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String path = etFilePath.getText().toString();
                tvFileShareResult.setText("Dosya paylaşıldı: " + path);
                // Gerçek dosya paylaşımı burada yapılacak
            }
        });
    }
}
