package com.example.tugis3

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner

/**
 * Özel test runner: Hilt grafiklerini enjekte edebilmek için HiltTestApplication kullanır.
 */
class HiltTestRunner : AndroidJUnitRunner() {
    override fun newApplication(cl: ClassLoader?, name: String?, context: Context?): Application {
        return super.newApplication(cl, "dagger.hilt.android.testing.HiltTestApplication", context)
    }
}

