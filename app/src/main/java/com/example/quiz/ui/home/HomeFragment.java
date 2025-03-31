package com.example.quiz.ui.home;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.quiz.R;
import com.example.quiz.adapter.CategoryAdapter;
import com.example.quiz.adapter.QuizAdapter;
import com.example.quiz.model.Quiz;
import com.example.quiz.model.Question;
import com.example.quiz.util.FirestoreUtils;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class HomeFragment extends Fragment {
    private static final String TAG = "HomeFragment";
    
    private RecyclerView recyclerViewPopular;
    private RecyclerView recyclerViewRecent;
    private ProgressBar progressBar;
    
    private QuizAdapter popularAdapter;
    private QuizAdapter recentAdapter;
    
    private List<Quiz> popularQuizzes = new ArrayList<>();
    private List<Quiz> recentQuizzes = new ArrayList<>();
    
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private boolean useFirestore = true; // Activé par défaut

    // Ajout des variables pour stocker les quiz complets 
    private List<Quiz> allPopularQuizzes;
    private List<Quiz> allRecentQuizzes;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Initialiser Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        
        // Configurer les RecyclerViews
        initViews(view);
        
        // Configurer le bouton pour actualiser les quiz
        Button refreshButton = view.findViewById(R.id.buttonRefreshQuizzes);
        if (refreshButton != null) {
            refreshButton.setOnClickListener(v -> {
                refreshData();
                Toast.makeText(getContext(), "Quiz actualisés", Toast.LENGTH_SHORT).show();
            });
        }
        
        // Charger les données
        loadData();
    }
    
    private void initViews(View view) {
        recyclerViewPopular = view.findViewById(R.id.recyclerViewPopular);
        recyclerViewRecent = view.findViewById(R.id.recyclerViewRecent);
        progressBar = view.findViewById(R.id.progressBar);
        
        // Configurer les RecyclerViews
        setupRecyclerViews(view);
    }
    
    private void setupRecyclerViews(View view) {
        // RecyclerView des quizzes populaires
        recyclerViewPopular.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        popularAdapter = new QuizAdapter(popularQuizzes, quiz -> {
            // Navigation vers la page de quiz sélectionné
            navigateToPlayQuiz(quiz);
        });
        recyclerViewPopular.setAdapter(popularAdapter);
        
        // RecyclerView des quizzes récents
        recyclerViewRecent.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        recentAdapter = new QuizAdapter(recentQuizzes, quiz -> {
            // Navigation vers la page de quiz sélectionné
            navigateToPlayQuiz(quiz);
        });
        recyclerViewRecent.setAdapter(recentAdapter);
    }
    
    private void navigateToPlayQuiz(Quiz quiz) {
        Log.d(TAG, "Quiz sélectionné: " + quiz.getTitle());
        
        try {
            NavController navController = Navigation.findNavController(requireView());
            Bundle args = new Bundle();
            args.putString("quizId", quiz.getId());
            navController.navigate(R.id.action_home_to_quiz_details, args);
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de la navigation: " + e.getMessage());
            Toast.makeText(getContext(), "Erreur de navigation: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    private void loadData() {
        // Afficher le loader
        progressBar.setVisibility(View.VISIBLE);
        
        // Charger les quizzes depuis Firestore
        loadPopularQuizzes();
        loadRecentQuizzes();
    }
    
    private void refreshData() {
        loadData();
    }
    
    private void loadPopularQuizzes() {
        try {
            db.collection("quizzes")
                .orderBy("playCount", Query.Direction.DESCENDING)
                .limit(10)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        popularQuizzes.clear();
                        for (DocumentSnapshot document : task.getResult()) {
                            try {
                                Quiz quiz = documentToQuiz(document);
                                if (quiz != null) {
                                    popularQuizzes.add(quiz);
                                    
                                    // Charger les questions pour ce quiz
                                    loadQuestionsForQuiz(quiz);
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Erreur lors de la conversion du document: " + e.getMessage());
                            }
                        }
                        
                        popularAdapter.notifyDataSetChanged();
                    } else {
                        Log.e(TAG, "Erreur lors du chargement des quiz populaires", task.getException());
                        Toast.makeText(getContext(), "Erreur lors du chargement des quiz populaires", Toast.LENGTH_SHORT).show();
                    }
                });
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors du chargement des quiz populaires: " + e.getMessage());
            Toast.makeText(getContext(), "Erreur: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            progressBar.setVisibility(View.GONE);
        }
    }
    
    private void loadRecentQuizzes() {
        try {
            db.collection("quizzes")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(10)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        recentQuizzes.clear();
                        for (DocumentSnapshot document : task.getResult()) {
                            try {
                                Quiz quiz = documentToQuiz(document);
                                if (quiz != null) {
                                    recentQuizzes.add(quiz);
                                    
                                    // Charger les questions pour ce quiz
                                    loadQuestionsForQuiz(quiz);
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Erreur lors de la conversion du document: " + e.getMessage());
                            }
                        }
                        
                        recentAdapter.notifyDataSetChanged();
                    } else {
                        Log.e(TAG, "Erreur lors du chargement des quiz récents", task.getException());
                        Toast.makeText(getContext(), "Erreur lors du chargement des quiz récents", Toast.LENGTH_SHORT).show();
                    }
                    
                    progressBar.setVisibility(View.GONE);
                });
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors du chargement des quiz récents: " + e.getMessage());
            Toast.makeText(getContext(), "Erreur: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            progressBar.setVisibility(View.GONE);
        }
    }
    
    private void loadQuestionsForQuiz(Quiz quiz) {
        if (quiz.getQuestionIds() == null || quiz.getQuestionIds().isEmpty()) {
            return;
        }
        
        FirestoreUtils.loadQuestionsById(quiz.getQuestionIds(), new FirestoreUtils.OnQuestionsLoadedListener() {
            @Override
            public void onQuestionsLoaded(List<Question> questions) {
                if (questions != null && !questions.isEmpty()) {
                    quiz.setQuestions(questions);
                    
                    // Utiliser la catégorie de la première question comme catégorie du quiz
                    if (quiz.getCategory() == null || quiz.getCategory().isEmpty()) {
                        quiz.setCategory(questions.get(0).getCategory());
                    }
                }
                
                // Rafraîchir l'adapter
                popularAdapter.notifyDataSetChanged();
                recentAdapter.notifyDataSetChanged();
            }
            
            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Erreur lors du chargement des questions: " + e.getMessage());
            }
        });
    }
    
    private Quiz documentToQuiz(DocumentSnapshot document) {
        String id = document.getId();
        String title = document.getString("title");
        String description = document.getString("description");
        String imageUrl = document.getString("imageUrl");
        String authorId = document.getString("authorId");
        
        Quiz quiz = new Quiz(id, title, description, imageUrl, authorId, document.getString("authorName"));
        
        // Récupérer le nombre de joueurs
        Number playCount = document.getLong("playCount");
        if (playCount != null) {
            quiz.setPlayCount(playCount.intValue());
        }
        
        // Récupérer la note
        Number rating = document.getDouble("rating");
        if (rating != null) {
            quiz.setRating(rating.doubleValue());
        }
        
        // Récupérer la date de création
        Number createdAt = document.getLong("createdAt");
        if (createdAt != null) {
            quiz.setCreatedAt(createdAt.longValue());
        }
        
        // Récupérer la catégorie
        String category = document.getString("category");
        if (category != null) {
            quiz.setCategory(category);
        }
        
        // Récupérer les IDs des questions
        List<String> questionIds = (List<String>) document.get("questionIds");
        if (questionIds != null) {
            quiz.setQuestionIds(questionIds);
        }
        
        return quiz;
    }
    
    private void updateQuizzesList(List<Quiz> quizzes) {
        // Mettre à jour les listes de quizzes avec ceux reçus
        popularQuizzes.clear();
        recentQuizzes.clear();
        
        if (quizzes != null && !quizzes.isEmpty()) {
            // Ajouter les quizzes aux deux listes (pour l'exemple)
            popularQuizzes.addAll(quizzes);
            recentQuizzes.addAll(quizzes);
            
            // Si la liste contient plus d'un quiz, mélanger l'ordre pour la liste des récents
            if (quizzes.size() > 1) {
                Collections.shuffle(recentQuizzes);
            }
        }
        
        // Notifier les adapters
        popularAdapter.notifyDataSetChanged();
        recentAdapter.notifyDataSetChanged();
        
        // Cacher le loader
        progressBar.setVisibility(View.GONE);
    }
} 