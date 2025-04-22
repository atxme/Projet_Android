package com.example.quiz.ui.create;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.quiz.model.Quiz;
import com.example.quiz.util.FirestoreUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class CreateQuizViewModel extends ViewModel {
    private static final String TAG = "CreateQuizViewModel";

    private MutableLiveData<Quiz> currentQuiz = new MutableLiveData<>();
    private MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private MutableLiveData<Boolean> saveSuccess = new MutableLiveData<>(false);
    private MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

    private String currentQuizId = null;

    // Getters pour les LiveData
    public LiveData<Quiz> getCurrentQuiz() {
        return currentQuiz;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public LiveData<Boolean> getSaveSuccess() {
        return saveSuccess;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    // Méthode pour charger un quiz existant
    public void loadQuiz(String quizId) {
        if (quizId == null || quizId.isEmpty()) {
            errorMessage.setValue("ID de quiz invalide");
            return;
        }

        this.currentQuizId = quizId;
        isLoading.setValue(true);

        FirestoreUtils.loadQuiz(quizId, new FirestoreUtils.OnQuizLoadedListener() {
            @Override
            public void onQuizLoaded(Quiz quiz) {
                currentQuiz.setValue(quiz);
                isLoading.setValue(false);
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Erreur lors du chargement du quiz", e);
                errorMessage.setValue("Erreur lors du chargement du quiz: " + e.getMessage());
                isLoading.setValue(false);
            }
        });
    }

    // Méthode pour sauvegarder un quiz
    public void saveQuiz(String title, String description, int timeLimit, 
                        Quiz.GameMode gameMode, boolean isPublished) {
        saveQuiz(title, description, timeLimit, gameMode, isPublished, null);
    }

    // Méthode pour sauvegarder un quiz avec un callback
    public void saveQuiz(String title, String description, int timeLimit, 
                        Quiz.GameMode gameMode, boolean isPublished, Runnable onSuccess) {
        // Récupérer l'utilisateur actuel
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String authorId = currentUser != null ? currentUser.getUid() : "anonymous";
        String authorName = currentUser != null ? 
                (currentUser.getDisplayName() != null ? currentUser.getDisplayName() : "Utilisateur") 
                : "Utilisateur Anonyme";

        // Créer ou mettre à jour le quiz
        Quiz quiz;
        if (currentQuizId != null && !currentQuizId.isEmpty()) {
            // Mise à jour d'un quiz existant
            quiz = currentQuiz.getValue();
            if (quiz == null) {
                quiz = new Quiz(currentQuizId, title, description, "", authorId, authorName);
            } else {
                quiz.setTitle(title);
                quiz.setDescription(description);
            }
        } else {
            // Création d'un nouveau quiz
            quiz = new Quiz("", title, description, "", authorId, authorName);
        }

        // Mettre à jour les propriétés du quiz
        quiz.setTimeLimit(timeLimit);
        quiz.setGameMode(gameMode);
        quiz.setPublished(isPublished);
        quiz.setUpdatedAt(System.currentTimeMillis());

        // Sauvegarder le quiz
        isLoading.setValue(true);
        saveSuccess.setValue(false);

        final Quiz finalQuiz = quiz;
        FirestoreUtils.createQuiz(quiz, new FirestoreUtils.OnQuizCreatedListener() {
            @Override
            public void onSuccess(String quizId) {
                currentQuizId = quizId;
                isLoading.setValue(false);
                saveSuccess.setValue(true);
                
                // Si un quiz est nouveau, recharger le quiz pour avoir toutes les propriétés à jour
                if (finalQuiz.getId() == null || finalQuiz.getId().isEmpty()) {
                    loadQuiz(quizId);
                }
                
                // Exécuter le callback si présent
                if (onSuccess != null) {
                    onSuccess.run();
                }
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Erreur lors de la sauvegarde du quiz", e);
                errorMessage.setValue("Erreur lors de la sauvegarde du quiz: " + e.getMessage());
                isLoading.setValue(false);
                saveSuccess.setValue(false);
            }
        });
    }

    // Retourne l'ID du quiz actuel
    public String getCurrentQuizId() {
        return currentQuizId;
    }
} 