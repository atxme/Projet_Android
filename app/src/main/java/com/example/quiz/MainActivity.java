package com.example.quiz;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.quiz.databinding.ActivityMainBinding;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    private NavController navController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        // Configurer la Toolbar comme ActionBar
        Toolbar toolbar = binding.toolbar;
        setSupportActionBar(toolbar);
        
        // Configuration de la navigation
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        
        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();
            
            // Configure Bottom Navigation
            BottomNavigationView bottomNav = binding.bottomNavigation;
            AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                    R.id.homeFragment, R.id.exploreFragment, R.id.createFragment, R.id.profileFragment)
                    .build();
            
            NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
            NavigationUI.setupWithNavController(bottomNav, navController);
        }
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        return navController.navigateUp() || super.onSupportNavigateUp();
    }
}