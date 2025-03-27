plugins {
    id("com.android.application")
    id("com.google.gms.google-services") // Pour Firebase
}

android {
    namespace = "com.example.quiz"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.quiz"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.activity:activity:1.10.1")
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("androidx.navigation:navigation-fragment:2.7.7")
    implementation("androidx.navigation:navigation-ui:2.7.7")
    implementation("androidx.viewpager2:viewpager2:1.0.0")
    
    // Firebase - avec des versions récentes et sécurisées
    implementation(platform("com.google.firebase:firebase-bom:32.8.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    // Storage retiré car nécessite un plan payant
    
    // Sécurité additionnelle pour les communications réseau
    implementation("com.google.android.gms:play-services-auth:21.0.0")
    implementation("com.google.android.gms:play-services-safetynet:18.0.1")
    
    // Glide pour le chargement d'images
    implementation("com.github.bumptech.glide:glide:4.16.0")
    
    // Pour les graphiques
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
    
    // Lottie pour les animations
    implementation("com.airbnb.android:lottie:6.4.0")
    
    // Pour le stockage local d'images
    implementation("androidx.room:room-runtime:2.6.1")
    annotationProcessor("androidx.room:room-compiler:2.6.1")
    
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}