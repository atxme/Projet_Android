package com.example.quiz.ui.quiz;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.quiz.model.Question;
import com.example.quiz.model.Quiz;
import com.example.quiz.util.FirestoreUtils;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

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
    
    // Méthode pour charger un quiz à partir de son ID
    public void loadQuiz(String quizId) {
        this.quizId = quizId;
        isLoading.setValue(true);
        
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
                errorMessage.setValue("Erreur lors du chargement du quiz: " + e.getMessage());
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
                
                // Configurer le timer si nécessaire
                if (quiz.getTimeLimit() > 0) {
                    timeRemaining.setValue(quiz.getTimeLimit());
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
    
    // Méthode pour mettre à jour le score
    public void updateScore(int points) {
        Integer currentScore = score.getValue();
        
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
        
        // Sauvegarder le score dans l'historique si l'utilisateur est connecté
        Integer finalScore = score.getValue();
        Quiz quiz = currentQuiz.getValue();
        
        if (finalScore != null && quiz != null) {
            // Logique de sauvegarde du score (à implémenter)
            // Exemple: sauvegarder dans les préférences utilisateur ou dans Firestore
        }
    }
} 