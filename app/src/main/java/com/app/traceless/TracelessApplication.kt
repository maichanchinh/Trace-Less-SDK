// app/src/main/kotlin/com/app/traceless/TracelessApplication.kt
package com.app.traceless

import android.app.Application
import com.app.traceless.analytic.Analytics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Application class that initializes SDK and Firebase adapter.
 * 
 * Event Flow:
 * 1. SDK emits events via Analytics.events Flow
 * 2. This class collects the Flow
 * 3. FirebaseAdapter dispatches to Firebase Analytics
 */
class TracelessApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Plant Timber in debug builds
        try {
            Timber.plant(Timber.DebugTree())
        } catch (e: NoClassDefFoundError) {
            // Timber not available in release builds
        }
        
        // Initialize SDK (no Firebase dependency here)
        Analytics.initialize()
        
//        // Initialize Firebase adapter (has Firebase dependency)
//        firebaseAdapter = FirebaseAdapter(this)
//        firebaseAdapter.startCollecting()


        CoroutineScope(Dispatchers.Main).launch {
            Analytics.events.collectLatest { event ->
                Timber.d("[Tracker] Collecting event: name: ${event.name}, screen: ${event.screenName}, manual: ${event.isManual},",
                    " element: ${event.elementId}, action: ${event.action}, params: ${event.params}")
                // Dispatch to Firebase Analytics
            }
        }
        
        // Observe lifecycle to handle app state changes
        setupLifecycleObserver()
    }
    
    private fun setupLifecycleObserver() {
    }
    
    override fun onTerminate() {
        super.onTerminate()
    }
}