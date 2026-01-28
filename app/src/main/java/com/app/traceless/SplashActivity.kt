package com.app.traceless

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.app.traceless.analytic.Analytics
import com.app.traceless.analytic.UIScreen

class SplashActivity : ComponentActivity() {

    private var ready = false

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { !ready }

        super.onCreate(savedInstanceState)

        Analytics.enterScreen(UIScreen.Splash)

        Handler(Looper.getMainLooper()).postDelayed({
            ready = true
            navigateToMain()
        }, 2000)
    }

    private fun navigateToMain() {
        startActivity(Intent(this, HomeActivity::class.java))
        finish()
    }
}