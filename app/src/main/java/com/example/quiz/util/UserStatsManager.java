package com.example.quiz.util;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * Gestionnaire des statistiques utilisateur synchronisées avec Firebase
 */
public class UserStatsManager {
    private static final String TAG = "UserStatsManager";
    private static final String USERS_COLLECTION = "users";
    
    private final FirebaseFirestore db;
    private final FirebaseUser currentUser;
    
    public UserStatsManager() {
        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
    }
    
    /**
     * Vérifie si l'utilisateur est connecté
     * @return true si l'utilisateur est connecté
     */
    public boolean isUserLoggedIn() {
        return currentUser != null;
    }
    
    /**
     * Met à jour les statistiques après une partie
     * @param questionsAnswered Nombre de questions répondues
     * @param correctAnswers Nombre de réponses correctes
     */
    public void updateStats(int questionsAnswered, int correctAnswers) {
        if (!isUserLoggedIn()) {
            Log.d(TAG, "Utilisateur non connecté, impossible de sauvegarder les statistiques");
            return;
        }
        
        String userId = currentUser.getUid();
        DocumentReference userRef = db.collection(USERS_COLLECTION).document(userId);
        
        Map<String, Object> updates = new HashMap<>();
        updates.put("gamesPlayed", FieldValue.increment(1));
        updates.put("questionsAnswered", FieldValue.increment(questionsAnswered));
        updates.put("correctAnswers", FieldValue.increment(correctAnswers));
        
        userRef.update(updates)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Statistiques mises à jour avec succès");
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Erreur lors de la mise à jour des statistiques", e);
                // Si le document n'existe pas, le créer
                if (e.getMessage() != null && e.getMessage().contains("No document to update")) {
                    createUserProfile(questionsAnswered, correctAnswers);
                }
            });
    }
    
    /**
     * Crée un profil utilisateur complet si celui-ci n'existe pas encore
     */
    private void createUserProfile(int questionsAnswered, int correctAnswers) {
        if (!isUserLoggedIn()) return;
        
        String userId = currentUser.getUid();
        Map<String, Object> userProfile = new HashMap<>();
        userProfile.put("uid", userId);
        userProfile.put("email", currentUser.getEmail());
        userProfile.put("displayName", currentUser.getDisplayName());
        userProfile.put("photoUrl", currentUser.getPhotoUrl() != null ? currentUser.getPhotoUrl().toString() : null);
        userProfile.put("createdAt", System.currentTimeMillis());
        
        // Initialiser les statistiques
        userProfile.put("gamesPlayed", 1);
        userProfile.put("questionsAnswered", questionsAnswered);
        userProfile.put("correctAnswers", correctAnswers);
        
        db.collection(USERS_COLLECTION).document(userId)
            .set(userProfile)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Profil utilisateur créé avec succès");
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Erreur lors de la création du profil utilisateur", e);
            });
    }
    
    /**
     * Récupère les statistiques de l'utilisateur
     * @param callback Callback qui sera appelé avec les données des statistiques
     */
    public void getUserStats(OnStatsLoadedListener callback) {
        if (!isUserLoggedIn()) {
            callback.onStatsLoaded(0, 0, 0);
            return;
        }
        
        String userId = currentUser.getUid();
        db.collection(USERS_COLLECTION).document(userId)
            .get()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult() != null) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        // Récupérer les statistiques
                        Long gamesPlayed = document.getLong("gamesPlayed");
                        Long questionsAnswered = document.getLong("questionsAnswered");
                        Long correctAnswers = document.getLong("correctAnswers");
                        
                        // Utiliser des valeurs par défaut si nulles
                        int games = gamesPlayed != null ? gamesPlayed.intValue() : 0;
                        int questions = questionsAnswered != null ? questionsAnswered.intValue() : 0;
                        int correct = correctAnswers != null ? correctAnswers.intValue() : 0;
                        
                        callback.onStatsLoaded(games, questions, correct);
                    } else {
                        // Document n'existe pas
                        callback.onStatsLoaded(0, 0, 0);
                    }
                } else {
                    // Erreur
                    Log.e(TAG, "Erreur lors de la récupération des statistiques", task.getException());
                    callback.onStatsLoaded(0, 0, 0);
                }
            });
    }
    
    /**
     * Interface de callback pour récupérer les statistiques
     */
    public interface OnStatsLoadedListener {
        void onStatsLoaded(int gamesPlayed, int questionsAnswered, int correctAnswers);
    }
} 