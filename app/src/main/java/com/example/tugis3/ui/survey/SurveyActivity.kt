package com.example.tugis3.ui.survey

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentContainerView
import com.example.tugis3.R
import com.example.tugis3.ui.SurveyFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SurveyMainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Fragment container için layout oluştur
        val fragmentContainer = FragmentContainerView(this).apply {
            id = R.id.survey_fragment_container
        }

        setContentView(fragmentContainer)

        // SurveyFragment'ı göster
        if (savedInstanceState == null) {
            val surveyFragment = SurveyFragment()

            supportFragmentManager.beginTransaction()
                .replace(R.id.survey_fragment_container, surveyFragment)
                .commit()
        }

        // ActionBar başlığını ayarla
        supportActionBar?.title = "GNSS Ölçüm Sistemi"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
