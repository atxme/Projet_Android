package com.example.quiz.util;

import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * Classe utilitaire pour gérer les statistiques des utilisateurs
 */
public class StatsUtils {
    private static final String TAG = "StatsUtils";
    private static final String USERS_COLLECTION = "users";

    /**
     * Incrémente le compteur de parties jouées pour l'utilisateur actuel
     */
    public static void incrementGamesPlayed() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            incrementUserStat(user.getUid(), "gamesPlayed", 1);
        }
    }

    /**
     * Incrémente le compteur de questions répondues et optionnellement le compteur de bonnes réponses
     * @param isCorrect true si la réponse est correcte, false sinon
     */
    public static void incrementQuestionStats(boolean isCorrect) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            // Incrémenter le compteur de questions répondues
            incrementUserStat(user.getUid(), "questionsAnswered", 1);
            
            // Si la réponse est correcte, incrémenter aussi le compteur de bonnes réponses
            if (isCorrect) {
                incrementUserStat(user.getUid(), "correctAnswers", 1);
            }
        }
    }
    
    /**
     * Méthode privée pour incrémenter une statistique et créer le profil utilisateur si nécessaire
     * @param userId ID de l'utilisateur
     * @param statField Nom du champ à incrémenter
     * @param increment Valeur d'incrémentation
     */
    private static void incrementUserStat(String userId, String statField, int increment) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        
        // Vérifier si le document utilisateur existe
        db.collection(USERS_COLLECTION).document(userId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    // Le document existe, incrémenter la statistique
                    updateStat(userId, statField, increment);
                } else {
                    // Le document n'existe pas, le créer avec les valeurs initiales
                    createUserProfileWithStats(userId, statField, increment);
                }
            })
            .addOnFailureListener(e -> {
                Log.w(TAG, "Erreur lors de la vérification de l'existence du profil utilisateur", e);
            });
    }
    
    /**
     * Met à jour une statistique utilisateur
     */
    private static void updateStat(String userId, String statField, int increment) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        
        db.collection(USERS_COLLECTION)
            .document(userId)
            .update(statField, FieldValue.increment(increment))
            .addOnSuccessListener(aVoid -> Log.d(TAG, "Statistique " + statField + " incrémentée"))
            .addOnFailureListener(e -> Log.w(TAG, "Erreur lors de l'incrémentation de " + statField, e));
    }
    
    /**
     * Crée un profil utilisateur avec les statistiques initiales
     */
    private static void createUserProfileWithStats(String userId, String statField, int increment) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        
        if (user != null) {
            Map<String, Object> userProfile = new HashMap<>();
            userProfile.put("uid", userId);
            userProfile.put("email", user.getEmail());
            userProfile.put("displayName", user.getDisplayName());
            userProfile.put("photoUrl", user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : null);
            userProfile.put("createdAt", System.currentTimeMillis());
            
            // Initialiser toutes les statistiques à 0
            userProfile.put("gamesPlayed", 0);
            userProfile.put("questionsAnswered", 0);
            userProfile.put("correctAnswers", 0);
            
            // Mettre à jour la statistique spécifique avec la valeur d'incrémentation
            if (statField.equals("gamesPlayed")) {
                userProfile.put("gamesPlayed", increment);
            } else if (statField.equals("questionsAnswered")) {
                userProfile.put("questionsAnswered", increment);
            } else if (statField.equals("correctAnswers")) {
                userProfile.put("correctAnswers", increment);
            }
            
            db.collection(USERS_COLLECTION)
                .document(userId)
                .set(userProfile)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Profil utilisateur créé avec statistiques initiales");
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Erreur lors de la création du profil utilisateur", e);
                });
        }
    }
} 