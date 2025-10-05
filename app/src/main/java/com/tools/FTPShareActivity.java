package com.example.tugis3.ui.tools;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.tugis3.R;

public class FTPShareActivity extends AppCompatActivity {
    private EditText etFTPServer;
    private EditText etFTPUser;
    private EditText etFTPPassword;
    private TextView tvFTPResult;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ftp_share);
        etFTPServer = findViewById(R.id.etFTPServer);
        etFTPUser = findViewById(R.id.etFTPUser);
        etFTPPassword = findViewById(R.id.etFTPPassword);
        tvFTPResult = findViewById(R.id.tvFTPResult);
        Button btnConnectFTP = findViewById(R.id.btnConnectFTP);
        btnConnectFTP.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String server = etFTPServer.getText().toString();
                tvFTPResult.setText("FTP bağlantısı kuruldu: " + server);
                // Gerçek FTP bağlantısı burada yapılacak
            }
        });
    }
}
