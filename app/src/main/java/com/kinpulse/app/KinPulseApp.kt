package com.kinpulse.app

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.kinpulse.app.data.AuthRepository
import com.kinpulse.app.data.HealthRepository

class AppContainer {
    val auth = AuthRepository(FirebaseAuth.getInstance())
    val health = HealthRepository(FirebaseFirestore.getInstance())
}

class KinPulseApp : Application() {

    /** Null when app/google-services.json was missing at build time. */
    var container: AppContainer? = null
        private set

    override fun onCreate() {
        super.onCreate()
        if (FirebaseApp.getApps(this).isNotEmpty()) {
            container = AppContainer()
        }
    }
}
