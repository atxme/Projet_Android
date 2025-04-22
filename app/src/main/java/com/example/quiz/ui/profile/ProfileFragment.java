package com.example.quiz.ui.profile;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.bumptech.glide.Glide;
import com.example.quiz.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProfileFragment extends Fragment {

    private static final String TAG = "ProfileFragment";
    private ImageView userProfileImage;
    private TextView userName, userEmail, userId;
    private TextView gamesPlayed, questionsAnswered, correctAnswers;
    private Button logoutButton;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

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
        db = FirebaseFirestore.getInstance();
        
        // Initialize views
        userProfileImage = view.findViewById(R.id.user_profile_image);
        userName = view.findViewById(R.id.user_name);
        userEmail = view.findViewById(R.id.user_email);
        userId = view.findViewById(R.id.user_id);
        gamesPlayed = view.findViewById(R.id.games_played);
        questionsAnswered = view.findViewById(R.id.questions_answered);
        correctAnswers = view.findViewById(R.id.correct_answers);
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
            
            // Set user profile image
            if (user.getPhotoUrl() != null) {
                Glide.with(this)
                    .load(user.getPhotoUrl())
                    .circleCrop()
                    .placeholder(R.drawable.ic_profile)
                    .into(userProfileImage);
            }
            
            // Load user statistics from Firestore
            loadUserStatistics(user.getUid());
        } else {
            // User is not logged in
            userName.setText("Invité");
            userEmail.setText("Non connecté");
            userId.setText("");
            gamesPlayed.setText("Parties jouées: 0");
            questionsAnswered.setText("Questions répondues: 0");
            correctAnswers.setText("Bonnes réponses: 0");
        }
    }
    
    private void loadUserStatistics(String userId) {
        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    // Récupérer les statistiques avec des valeurs par défaut si non définies
                    long gamesCount = documentSnapshot.getLong("gamesPlayed") != null ? 
                            documentSnapshot.getLong("gamesPlayed") : 0;
                    long questionsCount = documentSnapshot.getLong("questionsAnswered") != null ? 
                            documentSnapshot.getLong("questionsAnswered") : 0;
                    long correctCount = documentSnapshot.getLong("correctAnswers") != null ? 
                            documentSnapshot.getLong("correctAnswers") : 0;
                    
                    // Afficher les statistiques
                    gamesPlayed.setText("Parties jouées: " + gamesCount);
                    questionsAnswered.setText("Questions répondues: " + questionsCount);
                    correctAnswers.setText("Bonnes réponses: " + correctCount);
                    
                    // Calculer le pourcentage de bonnes réponses si possible
                    if (questionsCount > 0) {
                        double correctPercentage = (double) correctCount / questionsCount * 100;
                        correctAnswers.setText(String.format("Bonnes réponses: %d (%.1f%%)", 
                                correctCount, correctPercentage));
                    }
                } else {
                    // Le document utilisateur n'existe pas
                    Log.d(TAG, "Aucun document utilisateur trouvé pour l'ID: " + userId);
                    gamesPlayed.setText("Parties jouées: 0");
                    questionsAnswered.setText("Questions répondues: 0");
                    correctAnswers.setText("Bonnes réponses: 0");
                }
            })
            .addOnFailureListener(e -> {
                Log.w(TAG, "Erreur lors du chargement des statistiques utilisateur", e);
                gamesPlayed.setText("Parties jouées: --");
                questionsAnswered.setText("Questions répondues: --");
                correctAnswers.setText("Bonnes réponses: --");
            });
    }
    
    private void logout() {
        mAuth.signOut();
        Toast.makeText(requireContext(), "Déconnecté", Toast.LENGTH_SHORT).show();
        
        // Navigate to auth screen
        NavController navController = Navigation.findNavController(requireView());
        navController.navigate(R.id.authFragment);
    }
} 