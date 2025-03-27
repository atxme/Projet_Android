package com.example.quiz;

import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private NavController navController;
    private FirebaseAuth mAuth;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // Initialisation de Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        
        // Configuration de la Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        
        // Configuration de la navigation
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        
        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();
            
            // Configuration de la BottomNavigationView si elle existe
            BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
            if (bottomNav != null) {
                NavigationUI.setupWithNavController(bottomNav, navController);
            }
            
            // Configuration de la Toolbar avec NavController
            AppBarConfiguration appBarConfiguration = new AppBarConfiguration
                    .Builder(R.id.homeFragment, R.id.exploreFragment, R.id.profileFragment)
                    .build();
            NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        } else {
            Log.e(TAG, "NavHostFragment est null");
        }
    }
    
    // Configuration globale pour résoudre le problème de null dans onSupportNavigateUp
    private final AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
            R.id.homeFragment, R.id.exploreFragment, R.id.profileFragment).build();
    
    @Override
    protected void onStart() {
        super.onStart();
        // Vérifier si l'utilisateur est connecté à Firebase
        FirebaseUser currentUser = mAuth.getCurrentUser();
        
        // Si l'utilisateur n'est pas connecté à Firebase, rediriger vers l'écran d'authentification
        // seulement lors du premier démarrage de l'application
        if (currentUser == null && navController != null && !isUserLoggedInLocally()) {
            // L'utilisateur n'est pas connecté, rediriger vers l'écran d'authentification
            navController.navigate(R.id.authFragment);
        }
    }
    
    // Méthode pour vérifier si l'utilisateur est connecté localement
    // Ici, on utiliserait SharedPreferences pour stocker cet état
    private boolean isUserLoggedInLocally() {
        // Dans une implémentation réelle, on vérifierait si un utilisateur est enregistré localement dans SharedPreferences
        
        // Exemple de code à implémenter ultérieurement :
        // SharedPreferences prefs = getSharedPreferences("auth_prefs", MODE_PRIVATE);
        // return prefs.getBoolean("is_logged_in", false);
        
        // Pour l'instant, on retourne false pour forcer l'affichage de l'écran d'authentification
        return false;
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Gestion des options du menu
        int id = item.getItemId();
        
        if (id == R.id.action_settings) {
            Toast.makeText(this, "Paramètres", Toast.LENGTH_SHORT).show();
            return true;
        }
        
        return NavigationUI.onNavDestinationSelected(item, navController) || 
               super.onOptionsItemSelected(item);
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        return NavigationUI.navigateUp(navController, appBarConfiguration) || 
               super.onSupportNavigateUp();
    }
}