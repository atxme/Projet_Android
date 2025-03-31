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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
     * Enregistre les questions de démonstration dans Firestore
     */
    public static void saveDemoQuestionsToFirestore(OnOperationCompleteListener listener) {
        List<Question> demoQuestions = createDemoQuestions();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        final int[] savedCount = {0};
        
        for (Question question : demoQuestions) {
            // Vérifier si l'ID est défini pour éviter les duplications
            if (question.getId() == null || question.getId().isEmpty()) {
                question.setId(db.collection("questions").document().getId());
            }
            
            db.collection("questions")
                .document(question.getId())
                .set(question.toMap())
                .addOnCompleteListener(task -> {
                    savedCount[0]++;
                    
                    if (!task.isSuccessful()) {
                        Log.e(TAG, "Erreur lors de l'enregistrement de la question " + question.getId(), task.getException());
                    }
                    
                    if (savedCount[0] >= demoQuestions.size()) {
                        if (task.isSuccessful()) {
                            // Créer des quizzes démo avec ces questions
                            createDemoQuizzes(demoQuestions, listener);
                        } else {
                            listener.onError(task.getException());
                        }
                    }
                });
        }
    }
    
    /**
     * Crée des quizzes de démonstration en utilisant les questions démo
     */
    private static void createDemoQuizzes(List<Question> demoQuestions, OnOperationCompleteListener listener) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        
        // Créer un quiz Histoire/Science
        Quiz scienceQuiz = new Quiz(
            "quiz_science",
            "Histoire des sciences",
            "Un quiz pour tester vos connaissances sur les grands moments de l'histoire des sciences",
            null,
            "system",
            "Quiz Système"
        );
        scienceQuiz.setPlayCount(120);
        scienceQuiz.setRating(4.5);
        scienceQuiz.setCreatedAt(System.currentTimeMillis());
        
        // Ajouter les questions sur la technologie et l'informatique
        List<String> scienceQuestionIds = new ArrayList<>();
        for (Question q : demoQuestions) {
            if ("Technologie".equals(q.getCategory()) || "Informatique".equals(q.getCategory())) {
                scienceQuestionIds.add(q.getId());
            }
        }
        scienceQuiz.setQuestionIds(scienceQuestionIds);
        
        // Créer un quiz Espace
        Quiz spaceQuiz = new Quiz(
            "quiz_space",
            "Exploration spatiale",
            "Découvrez les missions qui ont marqué la conquête de l'espace",
            null,
            "system",
            "Quiz Système"
        );
        spaceQuiz.setPlayCount(85);
        spaceQuiz.setRating(4.2);
        spaceQuiz.setCreatedAt(System.currentTimeMillis() - 86400000); // Hier
        
        // Ajouter les questions sur l'espace
        List<String> spaceQuestionIds = new ArrayList<>();
        for (Question q : demoQuestions) {
            if ("Espace".equals(q.getCategory())) {
                spaceQuestionIds.add(q.getId());
            }
        }
        spaceQuiz.setQuestionIds(spaceQuestionIds);
        
        // Sauvegarder les quizzes
        final int[] savedCount = {0};
        final int totalQuizzes = 2;
        
        db.collection("quizzes").document(scienceQuiz.getId())
            .set(scienceQuiz.toMap())
            .addOnCompleteListener(task -> {
                savedCount[0]++;
                
                if (!task.isSuccessful()) {
                    Log.e(TAG, "Erreur lors de l'enregistrement du quiz Science", task.getException());
                }
                
                if (savedCount[0] >= totalQuizzes) {
                    listener.onSuccess();
                }
            });
            
        db.collection("quizzes").document(spaceQuiz.getId())
            .set(spaceQuiz.toMap())
            .addOnCompleteListener(task -> {
                savedCount[0]++;
                
                if (!task.isSuccessful()) {
                    Log.e(TAG, "Erreur lors de l'enregistrement du quiz Espace", task.getException());
                }
                
                if (savedCount[0] >= totalQuizzes) {
                    listener.onSuccess();
                }
            });
    }
    
    /**
     * Crée les questions de démonstration
     */
    private static List<Question> createDemoQuestions() {
        List<Question> questions = new ArrayList<>();
        
        // Question 1
        List<String> options1 = new ArrayList<>();
        options1.add("1969");
        options1.add("1971");
        options1.add("1975");
        options1.add("1965");
        questions.add(new Question(
            "q1",
            "En quelle année a été créé le premier microprocesseur Intel 4004?",
            null,
            null,
            options1,
            1,
            "Le premier microprocesseur Intel 4004 a été lancé en novembre 1971.",
            2,
            "Technologie",
            System.currentTimeMillis(),
            "system"
        ));
        
        // Question 2
        List<String> options2 = new ArrayList<>();
        options2.add("Mer de la Tranquillité");
        options2.add("Mer de la Sérénité");
        options2.add("Océan des Tempêtes");
        options2.add("Mer des Pluies");
        questions.add(new Question(
            "q2",
            "Sur quelle mer lunaire s'est posé Apollo 11?",
            null,
            null,
            options2,
            0,
            "Apollo 11 s'est posé sur la Mer de la Tranquillité (Mare Tranquillitatis) le 20 juillet 1969.",
            3,
            "Espace",
            System.currentTimeMillis(),
            "system"
        ));
        
        // Question 3
        List<String> options3 = new ArrayList<>();
        options3.add("Alan Turing");
        options3.add("John von Neumann");
        options3.add("Ada Lovelace");
        options3.add("Grace Hopper");
        questions.add(new Question(
            "q3",
            "Qui est considéré comme le père de l'informatique moderne?",
            null,
            null,
            options3,
            0,
            "Alan Turing est largement considéré comme le père de l'informatique moderne pour ses travaux sur la machine de Turing et le test de Turing.",
            2,
            "Informatique",
            System.currentTimeMillis(),
            "system"
        ));
        
        // Question 4
        List<String> options4 = new ArrayList<>();
        options4.add("Python");
        options4.add("Java");
        options4.add("JavaScript");
        options4.add("C++");
        questions.add(new Question(
            "q4",
            "Quel langage de programmation est le plus utilisé pour l'analyse de données et l'IA?",
            null,
            null,
            options4,
            0,
            "Python est le langage de prédilection pour l'analyse de données et l'IA grâce à ses bibliothèques comme TensorFlow, PyTorch, et scikit-learn.",
            1,
            "Informatique",
            System.currentTimeMillis(),
            "system"
        ));
        
        // Question 5
        List<String> options5 = new ArrayList<>();
        options5.add("Bitcoin");
        options5.add("Ethereum");
        options5.add("Litecoin");
        options5.add("Ripple");
        questions.add(new Question(
            "q5",
            "Quelle crypto-monnaie a introduit le concept de 'contrat intelligent'?",
            null,
            null,
            options5,
            1,
            "Ethereum a introduit le concept de 'contrat intelligent' (smart contract) qui permet d'exécuter des programmes sur la blockchain.",
            3,
            "Technologie",
            System.currentTimeMillis(),
            "system"
        ));
        
        return questions;
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