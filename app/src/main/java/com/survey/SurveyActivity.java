package com.example.tugis3.ui.survey;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import androidx.appcompat.app.AppCompatActivity;
import com.example.tugis3.R;

public class SurveyActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_survey);
        GridLayout gridMenu = findViewById(R.id.gridSurveyMenu);
        // Point Survey ikonuna tıklama ile geçiş
        LinearLayout pointSurveyItem = (LinearLayout) gridMenu.getChildAt(0);
        pointSurveyItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Kotlin PointSurveyActivity'ye geçiş
                startActivity(new Intent(SurveyActivity.this, PointSurveyActivity.class));
            }
        });
        // Diğer fonksiyonlar için benzer geçişler eklenebilir
    }
}
