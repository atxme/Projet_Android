package com.example.quiz.ui.quiz;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.quiz.model.Question;
import com.example.quiz.model.Quiz;
import com.example.quiz.util.FirestoreUtils;
import com.example.quiz.util.StatsUtils;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QuizViewModel extends ViewModel {
    private static final String TAG = "QuizViewModel";
    
    private MutableLiveData<List<Question>> questions = new MutableLiveData<>();
    private MutableLiveData<Integer> currentQuestionIndex = new MutableLiveData<>(0);
    private MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private MutableLiveData<Quiz> currentQuiz = new MutableLiveData<>();
    
    private MutableLiveData<Integer> score = new MutableLiveData<>(0);
    private MutableLiveData<Integer> timeRemaining = new MutableLiveData<>(0);
    
    // ID du quiz en cours
    private String quizId;
    
    // Compteurs pour les statistiques
    private int correctAnswers = 0;
    private int totalAnswers = 0;
    
    // Stockage des réponses des utilisateurs
    private Map<String, Integer> userAnswers = new HashMap<>();
    
    // Mode de jeu sélectionné
    private Quiz.GameMode selectedGameMode = null;
    
    // Getters pour les LiveData
    public LiveData<List<Question>> getQuestions() {
        return questions;
    }
    
    public LiveData<Integer> getCurrentQuestionIndex() {
        return currentQuestionIndex;
    }
    
    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }
    
    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }
    
    public LiveData<Quiz> getCurrentQuiz() {
        return currentQuiz;
    }
    
    public LiveData<Integer> getScore() {
        return score;
    }
    
    public LiveData<Integer> getTimeRemaining() {
        return timeRemaining;
    }
    
    // Méthodes pour le mode de jeu sélectionné
    public Quiz.GameMode getSelectedGameMode() {
        return selectedGameMode;
    }
    
    public void setSelectedGameMode(Quiz.GameMode mode) {
        this.selectedGameMode = mode;
    }
    
    // Méthode pour charger un quiz à partir de son ID
    public void loadQuiz(String quizId) {
        this.quizId = quizId;
        isLoading.setValue(true);
        errorMessage.setValue(null);
        
        FirestoreUtils.loadQuiz(quizId, new FirestoreUtils.OnQuizLoadedListener() {
            @Override
            public void onQuizLoaded(Quiz quiz) {
                currentQuiz.setValue(quiz);
                
                // Charger les questions du quiz
                loadQuestionsForQuiz(quiz);
            }
            
            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Erreur lors du chargement du quiz", e);
                isLoading.setValue(false);
                errorMessage.setValue("Erreur: " + e.getMessage());
            }
        });
    }
    
    // Méthode pour charger les questions d'un quiz
    private void loadQuestionsForQuiz(Quiz quiz) {
        if (quiz.getQuestionIds() == null || quiz.getQuestionIds().isEmpty()) {
            isLoading.setValue(false);
            errorMessage.setValue("Ce quiz ne contient pas de questions");
            return;
        }
        
        FirestoreUtils.loadQuestionsById(quiz.getQuestionIds(), new FirestoreUtils.OnQuestionsLoadedListener() {
            @Override
            public void onQuestionsLoaded(List<Question> loadedQuestions) {
                // Mélanger les questions si nécessaire
                // loadedQuestions.shuffle(); // À activer si on souhaite mélanger les questions
                
                questions.setValue(loadedQuestions);
                isLoading.setValue(false);
                
                // Réinitialiser l'index de la question courante
                currentQuestionIndex.setValue(0);
                
                // Réinitialiser le score
                score.setValue(0);
                
                // Réinitialiser les compteurs de statistiques
                correctAnswers = 0;
                totalAnswers = 0;
                
                // Configurer le timer si nécessaire
                if (quiz.getTimeLimit() > 0) {
                    timeRemaining.setValue(quiz.getTimeLimit());
                } else if (quiz.getGameMode() == Quiz.GameMode.TIMED) {
                    // Valeur par défaut pour le mode contre la montre (10 secondes)
                    timeRemaining.setValue(10);
                }
            }
            
            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Erreur lors du chargement des questions", e);
                isLoading.setValue(false);
                errorMessage.setValue("Erreur lors du chargement des questions: " + e.getMessage());
            }
        });
    }
    
    // Méthode pour passer à la question suivante
    public void nextQuestion() {
        Integer currentIndex = currentQuestionIndex.getValue();
        List<Question> questionsList = questions.getValue();
        
        if (currentIndex != null && questionsList != null && currentIndex < questionsList.size() - 1) {
            currentQuestionIndex.setValue(currentIndex + 1);
        }
    }
    
    // Méthode pour revenir à la question précédente
    public void previousQuestion() {
        Integer currentIndex = currentQuestionIndex.getValue();
        
        if (currentIndex != null && currentIndex > 0) {
            currentQuestionIndex.setValue(currentIndex - 1);
        }
    }
    
    // Méthode pour récupérer la question courante
    public Question getCurrentQuestion() {
        List<Question> questionsList = questions.getValue();
        Integer currentIndex = currentQuestionIndex.getValue();
        
        if (questionsList != null && currentIndex != null && !questionsList.isEmpty() && currentIndex < questionsList.size()) {
            return questionsList.get(currentIndex);
        }
        
        return null;
    }
    
    // Méthode pour mettre à jour le score et enregistrer la réponse
    public void updateScore(int points) {
        Integer currentScore = score.getValue();
        
        // Incrémenter le compteur total de réponses
        totalAnswers++;
        
        // Si des points ont été gagnés, c'est une bonne réponse
        if (points > 0) {
            correctAnswers++;
            StatsUtils.incrementQuestionStats(true);
        } else {
            StatsUtils.incrementQuestionStats(false);
        }
        
        if (currentScore != null) {
            score.setValue(currentScore + points);
        }
    }
    
    // Méthode pour mettre à jour le temps restant
    public void updateTimeRemaining(int seconds) {
        timeRemaining.setValue(seconds);
    }
    
    // Méthode pour terminer le quiz et sauvegarder les résultats
    public void finishQuiz() {
        // Incrémenter le nombre de fois que le quiz a été joué
        if (quizId != null && !quizId.isEmpty()) {
            FirestoreUtils.incrementQuizPlayCount(quizId);
        }
        
        // Incrémenter le compteur de parties jouées pour l'utilisateur
        StatsUtils.incrementGamesPlayed();
        
        // Sauvegarder le score dans l'historique si l'utilisateur est connecté
        Integer finalScore = score.getValue();
        Quiz quiz = currentQuiz.getValue();
        
        if (finalScore != null && quiz != null) {
            Log.d(TAG, "Quiz terminé avec score: " + finalScore + 
                  ", réponses correctes: " + correctAnswers + "/" + totalAnswers);
        }
    }
} 