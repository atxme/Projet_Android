package com.example.quiz.ui.profile;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.bumptech.glide.Glide;
import com.example.quiz.R;
import com.example.quiz.util.UserStatsManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ProfileFragment extends Fragment {

    private static final String TAG = "ProfileFragment";
    private ImageView userProfileImage;
    private TextView userName, userEmail, userId, joinDate;
    private TextView gamesPlayed, questionsAnswered, correctAnswers, userLevel;
    private ProgressBar progressBar;
    private Button logoutButton;
    private FirebaseAuth mAuth;
    private UserStatsManager statsManager;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        statsManager = new UserStatsManager();
        
        // Initialize views
        userProfileImage = view.findViewById(R.id.user_profile_image);
        userName = view.findViewById(R.id.user_name);
        userEmail = view.findViewById(R.id.user_email);
        userId = view.findViewById(R.id.user_id);
        joinDate = view.findViewById(R.id.join_date);
        gamesPlayed = view.findViewById(R.id.games_played);
        questionsAnswered = view.findViewById(R.id.questions_answered);
        correctAnswers = view.findViewById(R.id.correct_answers);
        userLevel = view.findViewById(R.id.user_level);
        progressBar = view.findViewById(R.id.progress_bar);
        logoutButton = view.findViewById(R.id.logout_button);
        
        // Display user information
        displayUserInfo();
        
        // Set logout button click listener
        logoutButton.setOnClickListener(v -> logout());
    }
    
    private void displayUserInfo() {
        FirebaseUser user = mAuth.getCurrentUser();
        
        if (user != null) {
            // Set user display name
            String displayName = user.getDisplayName();
            if (displayName != null && !displayName.isEmpty()) {
                userName.setText(displayName);
            } else {
                userName.setText("Utilisateur");
            }
            
            // Set user email
            String email = user.getEmail();
            if (email != null && !email.isEmpty()) {
                userEmail.setText(email);
            } else {
                userEmail.setText("Email non disponible");
            }
            
            // Set user ID
            userId.setText("ID: " + user.getUid());
            
            // Set account creation date
            if (user.getMetadata() != null && user.getMetadata().getCreationTimestamp() > 0) {
                long creationDate = user.getMetadata().getCreationTimestamp();
                SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                String formattedDate = dateFormat.format(new Date(creationDate));
                joinDate.setText("Membre depuis: " + formattedDate);
            } else {
                joinDate.setText("Membre depuis: Information non disponible");
            }
            
            // Set user profile image
            if (user.getPhotoUrl() != null) {
                Glide.with(this)
                    .load(user.getPhotoUrl())
                    .circleCrop()
                    .placeholder(R.drawable.ic_profile)
                    .into(userProfileImage);
            }
            
            // Load user statistics from Firestore using StatsManager
            loadUserStatistics();
        } else {
            // User is not logged in
            userName.setText("Invité");
            userEmail.setText("Non connecté");
            userId.setText("");
            joinDate.setText("Membre depuis: -");
            gamesPlayed.setText("Parties jouées: 0");
            questionsAnswered.setText("Questions répondues: 0");
            correctAnswers.setText("Bonnes réponses: 0");
            userLevel.setText("Niveau: -");
            progressBar.setProgress(0);
        }
    }
    
    private void loadUserStatistics() {
        // Utiliser le gestionnaire de statistiques pour charger les données
        statsManager.getUserStats(new UserStatsManager.OnStatsLoadedListener() {
            @Override
            public void onStatsLoaded(int gamesCount, int questionsCount, int correctCount) {
                if (isAdded()) { // Vérifier que le fragment est toujours attaché
                    // Afficher les statistiques
                    gamesPlayed.setText("Parties jouées: " + gamesCount);
                    questionsAnswered.setText("Questions répondues: " + questionsCount);
                    
                    // Calculer le pourcentage de bonnes réponses si possible
                    if (questionsCount > 0) {
                        double correctPercentage = (double) correctCount / questionsCount * 100;
                        correctAnswers.setText(String.format("Bonnes réponses: %d (%.1f%%)", 
                                correctCount, correctPercentage));
                        
                        // Mettre à jour la barre de progression
                        progressBar.setProgress((int) correctPercentage);
                        
                        // Définir le niveau utilisateur en fonction des statistiques
                        determineUserLevel(gamesCount, questionsCount, correctPercentage);
                    } else {
                        correctAnswers.setText("Bonnes réponses: 0");
                        userLevel.setText("Niveau: Débutant");
                        progressBar.setProgress(0);
                    }
                }
            }
        });
    }
    
    private void determineUserLevel(long gamesCount, long questionsCount, double correctPercentage) {
        // Déterminer le niveau en fonction des statistiques
        if (gamesCount > 50 && correctPercentage >= 80) {
            userLevel.setText("Expert");
        } else if (gamesCount > 20 && correctPercentage >= 70) {
            userLevel.setText("Confirmé");
        } else if (gamesCount > 10 && correctPercentage >= 60) {
            userLevel.setText("Intermédiaire");
        } else if (gamesCount > 5) {
            userLevel.setText("Novice");
        } else {
            userLevel.setText("Débutant");
        }
    }
    
    private void logout() {
        mAuth.signOut();
        Toast.makeText(requireContext(), "Déconnecté", Toast.LENGTH_SHORT).show();
        
        // Navigate to auth screen
        NavController navController = Navigation.findNavController(requireView());
        navController.navigate(R.id.authFragment);
    }
} 