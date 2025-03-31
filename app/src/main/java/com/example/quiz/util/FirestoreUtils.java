package com.example.quiz.util;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.quiz.model.Question;
import com.example.quiz.model.Quiz;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Collections;

/**
 * Classe utilitaire pour gérer les interactions avec Firestore
 */
public class FirestoreUtils {
    private static final String TAG = "FirestoreUtils";
    
    public interface OnQuestionsLoadedListener {
        void onQuestionsLoaded(List<Question> questions);
        void onError(Exception e);
    }
    
    public interface OnQuizzesLoadedListener {
        void onQuizzesLoaded(List<Quiz> quizzes);
        void onError(Exception e);
    }
    
    public interface OnOperationCompleteListener {
        void onSuccess();
        void onError(Exception e);
    }
    
    public interface OnQuizCreatedListener {
        void onSuccess(String quizId);
        void onError(Exception e);
    }
    
    /**
     * Charge toutes les questions depuis Firestore
     */
    public static void loadQuestions(OnQuestionsLoadedListener listener) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("questions")
            .get()
            .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                @Override
                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                    if (task.isSuccessful()) {
                        List<Question> questions = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String documentId = document.getId();
                            Map<String, Object> data = document.getData();
                            Question question = Question.fromMap(data, documentId);
                            questions.add(question);
                        }
                        listener.onQuestionsLoaded(questions);
                    } else {
                        Log.e(TAG, "Erreur lors du chargement des questions", task.getException());
                        listener.onError(task.getException());
                    }
                }
            });
    }
    
    /**
     * Charge toutes les questions pour un quiz spécifique
     */
    public static void loadQuestionsForQuiz(Quiz quiz, OnQuestionsLoadedListener listener) {
        if (quiz == null || quiz.getQuestionIds() == null || quiz.getQuestionIds().isEmpty()) {
            listener.onQuestionsLoaded(new ArrayList<>());
            return;
        }
        
        List<String> questionIds = quiz.getQuestionIds();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        List<Question> questions = new ArrayList<>();
        final int[] completedQueries = {0};
        final Exception[] error = {null};
        
        for (String questionId : questionIds) {
            db.collection("questions").document(questionId)
                .get()
                .addOnCompleteListener(task -> {
                    completedQueries[0]++;
                    
                    if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                        Map<String, Object> data = task.getResult().getData();
                        Question question = Question.fromMap(data, questionId);
                        questions.add(question);
                    } else if (task.getException() != null) {
                        Log.e(TAG, "Erreur lors du chargement de la question " + questionId, task.getException());
                        error[0] = task.getException();
                    }
                    
                    // Vérifier si nous avons terminé le chargement de toutes les questions
                    if (completedQueries[0] >= questionIds.size()) {
                        if (error[0] != null) {
                            listener.onError(error[0]);
                        } else {
                            // Mettre à jour les questions du quiz
                            quiz.setQuestions(questions);
                            listener.onQuestionsLoaded(questions);
                        }
                    }
                });
        }
    }
    
    /**
     * Charge les questions à partir d'une liste d'IDs
     */
    public static void loadQuestionsById(List<String> questionIds, OnQuestionsLoadedListener listener) {
        if (questionIds == null || questionIds.isEmpty()) {
            listener.onQuestionsLoaded(new ArrayList<>());
            return;
        }
        
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        List<Question> questions = new ArrayList<>();
        
        // Compteur pour suivre les requêtes terminées
        final int[] count = {0};
        final int total = questionIds.size();
        
        for (String questionId : questionIds) {
            db.collection("questions").document(questionId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        try {
                            Question question = Question.fromMap(documentSnapshot.getData(), documentSnapshot.getId());
                            questions.add(question);
                        } catch (Exception e) {
                            Log.e(TAG, "Erreur lors de la conversion de la question", e);
                        }
                    }
                    
                    // Incrémenter le compteur
                    count[0]++;
                    
                    // Si toutes les requêtes sont terminées, appeler le listener
                    if (count[0] >= total) {
                        // Trier les questions selon l'ordre des IDs
                        Collections.sort(questions, (q1, q2) -> {
                            int idx1 = questionIds.indexOf(q1.getId());
                            int idx2 = questionIds.indexOf(q2.getId());
                            return Integer.compare(idx1, idx2);
                        });
                        
                        listener.onQuestionsLoaded(questions);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Erreur lors du chargement de la question " + questionId, e);
                    
                    // Incrémenter le compteur même en cas d'erreur
                    count[0]++;
                    
                    // Si toutes les requêtes sont terminées, appeler le listener
                    if (count[0] >= total) {
                        // Trier les questions selon l'ordre des IDs
                        Collections.sort(questions, (q1, q2) -> {
                            int idx1 = questionIds.indexOf(q1.getId());
                            int idx2 = questionIds.indexOf(q2.getId());
                            return Integer.compare(idx1, idx2);
                        });
                        
                        listener.onQuestionsLoaded(questions);
                    }
                });
        }
    }
    
    /**
     * Méthode alternative utilisant une requête "in" pour charger plusieurs questions en une seule requête
     * Note: Cette méthode est limitée à 10 IDs maximum par requête Firestore
     */
    public static void loadQuestionsByIdBatched(List<String> questionIds, OnQuestionsLoadedListener listener) {
        if (questionIds == null || questionIds.isEmpty()) {
            listener.onQuestionsLoaded(new ArrayList<>());
            return;
        }
        
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        List<Question> questions = new ArrayList<>();
        
        // Diviser les questionIds en lots de 10 (limite Firestore pour les requêtes "in")
        List<List<String>> batches = new ArrayList<>();
        for (int i = 0; i < questionIds.size(); i += 10) {
            int end = Math.min(i + 10, questionIds.size());
            batches.add(questionIds.subList(i, end));
        }
        
        // Compteur pour suivre les requêtes par lots terminées
        final int[] batchCount = {0};
        final int totalBatches = batches.size();
        
        for (List<String> batch : batches) {
            db.collection("questions")
                .whereIn(FieldPath.documentId(), batch)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        try {
                            Question question = Question.fromMap(doc.getData(), doc.getId());
                            questions.add(question);
                        } catch (Exception e) {
                            Log.e(TAG, "Erreur lors de la conversion de la question", e);
                        }
                    }
                    
                    // Incrémenter le compteur de lots
                    batchCount[0]++;
                    
                    // Si tous les lots sont traités, appeler le listener
                    if (batchCount[0] >= totalBatches) {
                        // Trier les questions selon l'ordre des IDs
                        Collections.sort(questions, (q1, q2) -> {
                            int idx1 = questionIds.indexOf(q1.getId());
                            int idx2 = questionIds.indexOf(q2.getId());
                            return Integer.compare(idx1, idx2);
                        });
                        
                        listener.onQuestionsLoaded(questions);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Erreur lors du chargement d'un lot de questions", e);
                    
                    // Incrémenter le compteur de lots même en cas d'erreur
                    batchCount[0]++;
                    
                    // Si tous les lots sont traités, appeler le listener
                    if (batchCount[0] >= totalBatches) {
                        // Trier les questions selon l'ordre des IDs
                        Collections.sort(questions, (q1, q2) -> {
                            int idx1 = questionIds.indexOf(q1.getId());
                            int idx2 = questionIds.indexOf(q2.getId());
                            return Integer.compare(idx1, idx2);
                        });
                        
                        listener.onQuestionsLoaded(questions);
                    }
                });
        }
    }
    
    /**
     * Charge tous les quizzes depuis Firestore
     */
    public static void loadAllQuizzes(OnQuizzesLoadedListener listener) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Log.d(TAG, "Début chargement des quizzes depuis Firestore");
        db.collection("quizzes")
            .get()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Log.d(TAG, "Requête Firestore réussie, documents: " + (task.getResult() != null ? task.getResult().size() : 0));
                    List<Quiz> quizzes = new ArrayList<>();
                    for (DocumentSnapshot document : task.getResult()) {
                        try {
                            Log.d(TAG, "Traitement du document: " + document.getId());
                            Quiz quiz = Quiz.fromMap(document.getData(), document.getId());
                            quizzes.add(quiz);
                            Log.d(TAG, "Quiz ajouté: " + quiz.getTitle());
                        } catch (Exception e) {
                            Log.e(TAG, "Erreur lors de la conversion du document " + document.getId() + " en quiz: " + e.getMessage(), e);
                        }
                    }
                    listener.onQuizzesLoaded(quizzes);
                } else {
                    Log.e(TAG, "Erreur lors du chargement des quizzes", task.getException());
                    listener.onError(task.getException());
                }
            });
    }
    
    /**
     * Ajoute une nouvelle question à Firestore
     */
    public static void addQuestion(Question question, OnOperationCompleteListener listener) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        
        // Si aucun ID n'est défini, en créer un
        if (question.getId() == null || question.getId().isEmpty()) {
            question.setId(db.collection("questions").document().getId());
        }
        
        db.collection("questions")
            .document(question.getId())
            .set(question.toMap())
            .addOnSuccessListener(aVoid -> listener.onSuccess())
            .addOnFailureListener(e -> {
                Log.e(TAG, "Erreur lors de l'ajout de la question", e);
                listener.onError(e);
            });
    }
    
    /**
     * Crée un nouveau quiz dans Firestore
     */
    public static void createQuiz(Quiz quiz, OnQuizCreatedListener listener) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        
        // Si l'ID est vide, générer un nouvel ID
        if (quiz.getId() == null || quiz.getId().isEmpty()) {
            quiz.setId(db.collection("quizzes").document().getId());
        }
        
        // Définir la date de création si elle n'est pas définie
        if (quiz.getCreatedAt() == 0) {
            quiz.setCreatedAt(System.currentTimeMillis());
        }
        
        db.collection("quizzes")
            .document(quiz.getId())
            .set(quiz.toMap())
            .addOnSuccessListener(aVoid -> listener.onSuccess(quiz.getId()))
            .addOnFailureListener(e -> {
                Log.e(TAG, "Erreur lors de la création du quiz", e);
                listener.onError(e);
            });
    }
    
    /**
     * Met à jour un quiz existant avec ses questions dans Firestore
     */
    public static void updateQuizWithQuestions(Quiz quiz, OnOperationCompleteListener listener) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        
        // Mettre à jour les questionIds avec la liste actuelle des IDs de questions
        List<String> questionIds = new ArrayList<>();
        for (Question question : quiz.getQuestions()) {
            if (question.getId() != null && !question.getId().isEmpty()) {
                questionIds.add(question.getId());
            }
        }
        quiz.setQuestionIds(questionIds);
        
        // Mettre à jour le quiz dans Firestore
        db.collection("quizzes")
            .document(quiz.getId())
            .set(quiz.toMap())
            .addOnSuccessListener(aVoid -> listener.onSuccess())
            .addOnFailureListener(e -> {
                Log.e(TAG, "Erreur lors de la mise à jour du quiz", e);
                listener.onError(e);
            });
    }
    
    /**
     * Supprime un quiz de Firestore
     */
    public static void deleteQuiz(String quizId, OnOperationCompleteListener listener) {
        if (quizId == null || quizId.isEmpty()) {
            listener.onError(new IllegalArgumentException("L'ID du quiz ne peut pas être vide"));
            return;
        }
        
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("quizzes")
            .document(quizId)
            .delete()
            .addOnSuccessListener(aVoid -> listener.onSuccess())
            .addOnFailureListener(e -> {
                Log.e(TAG, "Erreur lors de la suppression du quiz " + quizId, e);
                listener.onError(e);
            });
    }
    
    /**
     * Supprime une question de Firestore
     */
    public static void deleteQuestion(String questionId, OnOperationCompleteListener listener) {
        if (questionId == null || questionId.isEmpty()) {
            listener.onError(new IllegalArgumentException("L'ID de la question ne peut pas être vide"));
            return;
        }
        
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("questions")
            .document(questionId)
            .delete()
            .addOnSuccessListener(aVoid -> listener.onSuccess())
            .addOnFailureListener(e -> {
                Log.e(TAG, "Erreur lors de la suppression de la question " + questionId, e);
                listener.onError(e);
            });
    }
    
    /**
     * Charge un quiz spécifique depuis Firestore
     */
    public static void loadQuiz(String quizId, OnQuizLoadedListener listener) {
        if (quizId == null || quizId.isEmpty()) {
            listener.onError(new IllegalArgumentException("L'ID du quiz ne peut pas être vide"));
            return;
        }
        
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("quizzes")
            .document(quizId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    try {
                        String id = documentSnapshot.getId();
                        String title = documentSnapshot.getString("title");
                        String description = documentSnapshot.getString("description");
                        String imageUrl = documentSnapshot.getString("imageUrl");
                        String authorId = documentSnapshot.getString("authorId");
                        String authorName = documentSnapshot.getString("authorName");
                        long playCount = documentSnapshot.getLong("playCount") != null ? documentSnapshot.getLong("playCount") : 0;
                        double rating = documentSnapshot.getDouble("rating") != null ? documentSnapshot.getDouble("rating") : 0.0;
                        long createdAt = documentSnapshot.getLong("createdAt") != null ? documentSnapshot.getLong("createdAt") : System.currentTimeMillis();
                        
                        Quiz quiz = new Quiz(id, title, description, imageUrl, authorId, authorName);
                        quiz.setPlayCount((int) playCount);
                        quiz.setRating(rating);
                        quiz.setCreatedAt(createdAt);
                        
                        // Récupérer la liste des IDs de questions
                        List<String> questionIds = (List<String>) documentSnapshot.get("questionIds");
                        if (questionIds != null) {
                            quiz.setQuestionIds(questionIds);
                        }
                        
                        listener.onQuizLoaded(quiz);
                    } catch (Exception e) {
                        Log.e(TAG, "Erreur lors de la conversion du document en quiz", e);
                        listener.onError(e);
                    }
                } else {
                    listener.onError(new Exception("Quiz non trouvé"));
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Erreur lors du chargement du quiz " + quizId, e);
                listener.onError(e);
            });
    }
    
    public interface OnQuizLoadedListener {
        void onQuizLoaded(Quiz quiz);
        void onError(Exception e);
    }
    
    /**
     * Incrémente le compteur de lectures d'un quiz
     */
    public static void incrementQuizPlayCount(String quizId) {
        if (quizId == null || quizId.isEmpty()) {
            return;
        }
        
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("quizzes").document(quizId)
            .update("playCount", com.google.firebase.firestore.FieldValue.increment(1))
            .addOnFailureListener(e -> 
                Log.e(TAG, "Erreur lors de l'incrémentation du compteur de lecture pour " + quizId, e)
            );
    }
} 