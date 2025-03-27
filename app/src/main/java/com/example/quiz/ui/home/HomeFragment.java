package com.example.quiz.ui.home;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.quiz.R;
import com.example.quiz.adapter.QuizAdapter;
import com.example.quiz.model.Question;
import com.example.quiz.model.Quiz;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HomeFragment extends Fragment {
    private static final String TAG = "HomeFragment";
    
    private RecyclerView recyclerViewRecent;
    private RecyclerView recyclerViewPopular;
    private RecyclerView recyclerViewYours;
    
    private QuizAdapter recentAdapter;
    private QuizAdapter popularAdapter;
    private QuizAdapter yourAdapter;
    
    private List<Quiz> recentQuizzes = new ArrayList<>();
    private List<Quiz> popularQuizzes = new ArrayList<>();
    private List<Quiz> yourQuizzes = new ArrayList<>();
    
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

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
        setupRecyclerViews(view);
        
        // Charger les données
        loadData();
    }
    
    private void setupRecyclerViews(View view) {
        // RecyclerView des quizzes récents
        recyclerViewRecent = view.findViewById(R.id.recyclerViewRecent);
        recyclerViewRecent.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        recentAdapter = new QuizAdapter(recentQuizzes);
        recyclerViewRecent.setAdapter(recentAdapter);
        
        // RecyclerView des quizzes populaires
        recyclerViewPopular = view.findViewById(R.id.recyclerViewPopular);
        recyclerViewPopular.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        popularAdapter = new QuizAdapter(popularQuizzes);
        recyclerViewPopular.setAdapter(popularAdapter);
        
        // RecyclerView de vos quizzes
        recyclerViewYours = view.findViewById(R.id.recyclerViewYours);
        recyclerViewYours.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        yourAdapter = new QuizAdapter(yourQuizzes);
        recyclerViewYours.setAdapter(yourAdapter);
    }
    
    private void loadData() {
        // Vérifier si l'utilisateur est connecté
        FirebaseUser currentUser = mAuth.getCurrentUser();
        String userId = currentUser != null ? currentUser.getUid() : null;
        
        // Charger les quizzes récents
        loadRecentQuizzes();
        
        // Charger les quizzes populaires
        loadPopularQuizzes();
        
        // Charger vos quizzes si l'utilisateur est connecté
        if (userId != null) {
            loadYourQuizzes(userId);
        } else {
            // Utilisateur non connecté, afficher des données de démo
            yourQuizzes.clear();
            yourQuizzes.add(createDemoQuiz("Connectez-vous pour voir vos quizzes", "Créez un compte pour enregistrer vos quizzes et suivre votre progression"));
            yourAdapter.notifyDataSetChanged();
        }
    }
    
    private void loadRecentQuizzes() {
        db.collection("quizzes")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(10)
            .get()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    recentQuizzes.clear();
                    for (DocumentSnapshot document : task.getResult()) {
                        Quiz quiz = documentToQuiz(document);
                        if (quiz != null) {
                            recentQuizzes.add(quiz);
                        }
                    }
                    
                    // Si aucun quiz n'est trouvé, afficher des données de démo
                    if (recentQuizzes.isEmpty()) {
                        recentQuizzes.addAll(createDemoQuizzes());
                    }
                    
                    recentAdapter.notifyDataSetChanged();
                } else {
                    Log.w(TAG, "Erreur lors du chargement des quizzes récents", task.getException());
                    // En cas d'erreur, afficher des données de démo
                    recentQuizzes.clear();
                    recentQuizzes.addAll(createDemoQuizzes());
                    recentAdapter.notifyDataSetChanged();
                }
            });
    }
    
    private void loadPopularQuizzes() {
        db.collection("quizzes")
            .orderBy("playCount", Query.Direction.DESCENDING)
            .limit(10)
            .get()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    popularQuizzes.clear();
                    for (DocumentSnapshot document : task.getResult()) {
                        Quiz quiz = documentToQuiz(document);
                        if (quiz != null) {
                            popularQuizzes.add(quiz);
                        }
                    }
                    
                    // Si aucun quiz n'est trouvé, afficher des données de démo
                    if (popularQuizzes.isEmpty()) {
                        popularQuizzes.addAll(createDemoQuizzes());
                    }
                    
                    popularAdapter.notifyDataSetChanged();
                } else {
                    Log.w(TAG, "Erreur lors du chargement des quizzes populaires", task.getException());
                    // En cas d'erreur, afficher des données de démo
                    popularQuizzes.clear();
                    popularQuizzes.addAll(createDemoQuizzes());
                    popularAdapter.notifyDataSetChanged();
                }
            });
    }
    
    private void loadYourQuizzes(String userId) {
        db.collection("quizzes")
            .whereEqualTo("authorId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    yourQuizzes.clear();
                    for (DocumentSnapshot document : task.getResult()) {
                        Quiz quiz = documentToQuiz(document);
                        if (quiz != null) {
                            yourQuizzes.add(quiz);
                        }
                    }
                    
                    // Si aucun quiz n'est trouvé, afficher un message
                    if (yourQuizzes.isEmpty()) {
                        yourQuizzes.add(createDemoQuiz("Vous n'avez pas encore créé de quiz", "Appuyez sur le bouton + pour créer votre premier quiz"));
                    }
                    
                    yourAdapter.notifyDataSetChanged();
                } else {
                    Log.w(TAG, "Erreur lors du chargement de vos quizzes", task.getException());
                    // En cas d'erreur, afficher un message
                    yourQuizzes.clear();
                    yourQuizzes.add(createDemoQuiz("Erreur de chargement", "Impossible de charger vos quizzes pour le moment"));
                    yourAdapter.notifyDataSetChanged();
                }
            });
    }
    
    private Quiz documentToQuiz(DocumentSnapshot document) {
        try {
            String id = document.getId();
            String title = document.getString("title");
            String description = document.getString("description");
            String imageUrl = document.getString("imageUrl");
            String authorId = document.getString("authorId");
            String authorName = document.getString("authorName");
            long playCount = document.getLong("playCount") != null ? document.getLong("playCount") : 0;
            double rating = document.getDouble("rating") != null ? document.getDouble("rating") : 0.0;
            long createdAt = document.getLong("createdAt") != null ? document.getLong("createdAt") : System.currentTimeMillis();
            
            Quiz quiz = new Quiz(id, title, description, imageUrl, authorId, authorName);
            quiz.setPlayCount((int) playCount);
            quiz.setRating(rating);
            quiz.setCreatedAt(createdAt);
            
            return quiz;
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de la conversion du document en quiz", e);
            return null;
        }
    }
    
    private List<Quiz> createDemoQuizzes() {
        List<Quiz> demoQuizzes = new ArrayList<>();
        
        Quiz quiz1 = new Quiz(
            "demo1",
            "Histoire des sciences",
            "Un quiz pour tester vos connaissances sur les grands moments de l'histoire des sciences",
            null,
            "system",
            "Quiz Système"
        );
        quiz1.setPlayCount(120);
        quiz1.setRating(4.5);
        demoQuizzes.add(quiz1);
        
        Quiz quiz2 = new Quiz(
            "demo2",
            "Exploration spatiale",
            "Découvrez les missions qui ont marqué la conquête de l'espace",
            null,
            "system",
            "Quiz Système"
        );
        quiz2.setPlayCount(85);
        quiz2.setRating(4.2);
        demoQuizzes.add(quiz2);
        
        Quiz quiz3 = new Quiz(
            "demo3",
            "Informatique et programmation",
            "Les bases de l'informatique et de la programmation moderne",
            null,
            "system",
            "Quiz Système"
        );
        quiz3.setPlayCount(210);
        quiz3.setRating(4.7);
        demoQuizzes.add(quiz3);
        
        return demoQuizzes;
    }
    
    private Quiz createDemoQuiz(String title, String description) {
        return new Quiz(
            "demo",
            title,
            description,
            null,
            "system",
            "Quiz Système"
        );
    }
} 