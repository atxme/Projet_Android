package com.example.quiz;

import android.app.Application;
import com.google.firebase.FirebaseApp;
// import com.google.firebase.appcheck.FirebaseAppCheck;
// import com.google.firebase.appcheck.safetynet.SafetyNetAppCheckProviderFactory;

public class QuizApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        
        // Initialiser Firebase
        FirebaseApp.initializeApp(this);
        
        // App Check temporairement désactivé pour éviter les erreurs
        /*
        FirebaseAppCheck firebaseAppCheck = FirebaseAppCheck.getInstance();
        firebaseAppCheck.installAppCheckProviderFactory(
            SafetyNetAppCheckProviderFactory.getInstance());
        */
    }
} 