package com.example.quiz;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.example.quiz.util.BackgroundMusicManager;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private NavController navController;
    private FirebaseAuth mAuth;
    private TextView headerTitle;
    private BottomNavigationView bottomNav;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // Initialisation de Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        
        // Hide the support action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        
        // Initialiser le header title
        headerTitle = findViewById(R.id.header_title);
        
        // Configuration de la navigation
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        
        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();
            
            // Configuration de la BottomNavigationView si elle existe
            bottomNav = findViewById(R.id.bottom_navigation);
            if (bottomNav != null) {
                // Configuration avancée de la BottomNavigationView
                setupBottomNavigation();
            }
            
            // Observer les changements de destination pour mettre à jour le titre
            navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
                updateHeaderTitle(destination);
                updateBottomNavVisibility(destination.getId());
            });
        } else {
            Log.e(TAG, "NavHostFragment est null");
        }
    }
    
    private void setupBottomNavigation() {
        // Configurer le comportement de base
        NavigationUI.setupWithNavController(bottomNav, navController);
        
        // Configurer le listener de navigation personnalisé
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            
            // Si on clique sur l'accueil depuis une page différente, on retourne à l'accueil
            if (itemId == R.id.homeFragment && 
                navController.getCurrentDestination().getId() != R.id.homeFragment) {
                // Effacer la pile de retour et naviguer directement vers l'accueil
                navController.popBackStack(R.id.homeFragment, false);
                return true;
            }
            
            // Si on clique sur le profil
            if (itemId == R.id.profileFragment) {
                if (navController.getCurrentDestination().getId() != R.id.profileFragment) {
                    navController.navigate(R.id.profileFragment);
                }
                return true;
            }
            
            // Comportement par défaut
            return NavigationUI.onNavDestinationSelected(item, navController);
        });
    }
    
    // Méthode pour gérer la visibilité de la barre de navigation
    private void updateBottomNavVisibility(int destinationId) {
        if (bottomNav != null) {
            if (destinationId == R.id.authFragment) {
                // Cacher la barre de navigation sur l'écran d'authentification
                bottomNav.setVisibility(View.GONE);
            } else {
                // Afficher la barre de navigation sur les autres écrans
                // Mais seulement si l'utilisateur est connecté
                FirebaseUser currentUser = mAuth.getCurrentUser();
                if (currentUser != null) {
                    bottomNav.setVisibility(View.VISIBLE);
                } else {
                    bottomNav.setVisibility(View.GONE);
                }
            }
        }
    }
    
    // Méthode pour mettre à jour le titre en fonction de la destination
    private void updateHeaderTitle(NavDestination destination) {
        int destinationId = destination.getId();
        
        if (destinationId == R.id.homeFragment) {
            headerTitle.setText("Accueil");
        } else if (destinationId == R.id.profileFragment) {
            headerTitle.setText("Mon Profil");
        } else if (destinationId == R.id.authFragment) {
            headerTitle.setText("Connexion");
        } else if (destinationId == R.id.quizDetailsFragment) {
            headerTitle.setText("Détails du Quiz");
        } else if (destinationId == R.id.playQuizFragment) {
            headerTitle.setText("Jouer");
        } else if (destinationId == R.id.quizResultsFragment) {
            headerTitle.setText("Résultats");
        } else {
            // Titre par défaut
            headerTitle.setText("Quiz App");
        }
    }
    
    // Configuration globale pour résoudre le problème de null dans onSupportNavigateUp
    private final AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
            R.id.homeFragment, R.id.profileFragment).build();
    
    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null && navController != null && !isUserLoggedInLocally()) {
            navController.navigate(R.id.authFragment);
            if (bottomNav != null) {
                bottomNav.setVisibility(View.GONE);
            }
        }
        // Démarrer la musique de fond uniquement sur les menus
        if (navController != null) {
            int destId = navController.getCurrentDestination() != null ? navController.getCurrentDestination().getId() : -1;
            if (destId == R.id.homeFragment || destId == R.id.profileFragment || destId == R.id.quizResultsFragment || destId == R.id.quizDetailsFragment) {
                BackgroundMusicManager.start(this, R.raw.background_music);
            } else {
                BackgroundMusicManager.stop();
            }
        }
    }
    
    @Override
    protected void onStop() {
        super.onStop();
        BackgroundMusicManager.stop();
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
    
    // Méthode publique pour permettre aux fragments de montrer la barre de navigation
    public void showBottomNavigation() {
        if (bottomNav != null) {
            bottomNav.setVisibility(View.VISIBLE);
        }
    }
}