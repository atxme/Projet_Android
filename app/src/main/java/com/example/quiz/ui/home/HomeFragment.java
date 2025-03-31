package com.example.quiz.ui.home;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.quiz.R;
import com.example.quiz.adapter.QuizAdapter;
import com.example.quiz.model.Quiz;
import com.example.quiz.util.FirestoreUtils;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
    private boolean useFirestore = true; // Activé par défaut

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
        
        // Configurer le bouton pour créer un quiz
        FloatingActionButton fab = view.findViewById(R.id.fabCreateQuiz);
        if (fab != null) {
            fab.setOnClickListener(v -> {
                // Naviguer vers l'écran de création de quiz
                try {
                    NavController navController = Navigation.findNavController(view);
                    navController.navigate(R.id.action_home_to_create_quiz);
                } catch (Exception e) {
                    Log.e(TAG, "Erreur de navigation: " + e.getMessage());
                    Toast.makeText(getContext(), "Navigation non disponible", Toast.LENGTH_SHORT).show();
                }
            });
        }
        
        // Ajouter un bouton pour synchroniser les questions avec Firestore (pour démo)
        Button syncButton = view.findViewById(R.id.buttonSyncQuestions);
        if (syncButton != null) {
            syncButton.setOnClickListener(v -> {
                syncDemoQuestionsToFirestore();
            });
        }
        
        // Charger les données
        loadData();
    }
    
    private void setupRecyclerViews(View view) {
        // RecyclerView des quizzes récents
        recyclerViewRecent = view.findViewById(R.id.recyclerViewRecent);
        recyclerViewRecent.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        recentAdapter = new QuizAdapter(recentQuizzes, quiz -> {
            // Gestion du clic sur un quiz
            Log.d(TAG, "Quiz sélectionné: " + quiz.getTitle());
            
            // Naviguer vers le détail du quiz (si l'action existe)
            try {
                NavController navController = Navigation.findNavController(requireView());
                Bundle args = new Bundle();
                args.putString("quizId", quiz.getId());
                // navController.navigate(R.id.action_home_to_quiz_details, args);
                // Pour l'instant, juste afficher un message
                Toast.makeText(getContext(), "Quiz sélectionné: " + quiz.getTitle(), Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Log.e(TAG, "Erreur lors de la navigation: " + e.getMessage());
            }
        });
        recyclerViewRecent.setAdapter(recentAdapter);
        
        // RecyclerView des quizzes populaires
        recyclerViewPopular = view.findViewById(R.id.recyclerViewPopular);
        recyclerViewPopular.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        popularAdapter = new QuizAdapter(popularQuizzes, quiz -> {
            // Gestion du clic sur un quiz
            Log.d(TAG, "Quiz sélectionné: " + quiz.getTitle());
            Toast.makeText(getContext(), "Quiz sélectionné: " + quiz.getTitle(), Toast.LENGTH_SHORT).show();
        });
        recyclerViewPopular.setAdapter(popularAdapter);
        
        // RecyclerView de vos quizzes
        recyclerViewYours = view.findViewById(R.id.recyclerViewYours);
        recyclerViewYours.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        yourAdapter = new QuizAdapter(yourQuizzes, quiz -> {
            // Gestion du clic sur un quiz
            Log.d(TAG, "Quiz sélectionné: " + quiz.getTitle());
            Toast.makeText(getContext(), "Quiz sélectionné: " + quiz.getTitle(), Toast.LENGTH_SHORT).show();
        });
        recyclerViewYours.setAdapter(yourAdapter);
    }
    
    private void loadData() {
        // Vérifier si on utilise Firestore ou les données locales
        if (useFirestore) {
            // Charger depuis Firestore avec fallback sur données locales
            loadFirestoreData();
        } else {
            // Utiliser uniquement les données locales
            loadLocalData();
        }
    }
    
    private void loadLocalData() {
        // Charger les quizzes récents
        recentQuizzes.clear();
        recentQuizzes.addAll(createDemoQuizzes());
        recentAdapter.notifyDataSetChanged();
        
        // Charger les quizzes populaires (mêmes données mais ordre différent)
        popularQuizzes.clear();
        List<Quiz> demoPop = createDemoQuizzes();
        Collections.reverse(demoPop); // Inverser l'ordre pour simuler un tri différent
        popularQuizzes.addAll(demoPop);
        popularAdapter.notifyDataSetChanged();
        
        // Charger vos quizzes
        yourQuizzes.clear();
        yourQuizzes.add(createDemoQuiz(
            "Créez votre premier quiz", 
            "Appuyez sur le bouton + pour commencer à créer vos propres quiz"));
        yourAdapter.notifyDataSetChanged();
    }
    
    private void loadFirestoreData() {
        // Vérifier si l'utilisateur est connecté
        FirebaseUser currentUser = mAuth.getCurrentUser();
        String userId = currentUser != null ? currentUser.getUid() : null;
        
        // Charger les quizzes récents avec timeout pour éviter de bloquer l'UI
        loadRecentQuizzes();
        
        // Charger les quizzes populaires avec timeout
        loadPopularQuizzes();
        
        // Charger vos quizzes si l'utilisateur est connecté
        if (userId != null) {
            loadYourQuizzes(userId);
        } else {
            // Utilisateur non connecté, afficher des données de démo
            yourQuizzes.clear();
            yourQuizzes.add(createDemoQuiz("Connectez-vous pour voir vos quizzes", 
                "Créez un compte pour enregistrer vos quizzes et suivre votre progression"));
            yourAdapter.notifyDataSetChanged();
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
        } catch (Exception e) {
            Log.e(TAG, "Exception lors du chargement des quizzes récents: " + e.getMessage());
            // En cas d'erreur, afficher des données de démo
            recentQuizzes.clear();
            recentQuizzes.addAll(createDemoQuizzes());
            recentAdapter.notifyDataSetChanged();
        }
    }
    
    private void loadQuestionsForQuiz(Quiz quiz) {
        FirestoreUtils.loadQuestionsForQuiz(quiz, new FirestoreUtils.OnQuestionsLoadedListener() {
            @Override
            public void onQuestionsLoaded(List<com.example.quiz.model.Question> questions) {
                // Mettre à jour l'adapter pour refléter le nombre de questions chargées
                recentAdapter.notifyDataSetChanged();
                popularAdapter.notifyDataSetChanged();
                yourAdapter.notifyDataSetChanged();
            }
            
            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Erreur lors du chargement des questions pour le quiz " + quiz.getId(), e);
            }
        });
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
                        
                        // Si aucun quiz n'est trouvé, afficher des données de démo
                        if (popularQuizzes.isEmpty()) {
                            popularQuizzes.addAll(createDemoQuizzes());
                            // Inverser l'ordre pour avoir des données différentes des quizzes récents
                            Collections.reverse(popularQuizzes);
                        }
                        
                        popularAdapter.notifyDataSetChanged();
                    } else {
                        Log.w(TAG, "Erreur lors du chargement des quizzes populaires", task.getException());
                        // En cas d'erreur, afficher des données de démo
                        popularQuizzes.clear();
                        List<Quiz> demoQuizzes = createDemoQuizzes();
                        Collections.reverse(demoQuizzes);
                        popularQuizzes.addAll(demoQuizzes);
                        popularAdapter.notifyDataSetChanged();
                    }
                });
        } catch (Exception e) {
            Log.e(TAG, "Exception lors du chargement des quizzes populaires: " + e.getMessage());
            // En cas d'erreur, afficher des données de démo
            popularQuizzes.clear();
            List<Quiz> demoQuizzes = createDemoQuizzes();
            Collections.reverse(demoQuizzes);
            popularQuizzes.addAll(demoQuizzes);
            popularAdapter.notifyDataSetChanged();
        }
    }
    
    private void loadYourQuizzes(String userId) {
        try {
            db.collection("quizzes")
                .whereEqualTo("authorId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        yourQuizzes.clear();
                        for (DocumentSnapshot document : task.getResult()) {
                            try {
                                Quiz quiz = documentToQuiz(document);
                                if (quiz != null) {
                                    yourQuizzes.add(quiz);
                                    
                                    // Charger les questions pour ce quiz
                                    loadQuestionsForQuiz(quiz);
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Erreur lors de la conversion du document: " + e.getMessage());
                            }
                        }
                        
                        // Si aucun quiz n'est trouvé, afficher un message
                        if (yourQuizzes.isEmpty()) {
                            yourQuizzes.add(createDemoQuiz("Vous n'avez pas encore créé de quiz", 
                                "Appuyez sur le bouton + pour créer votre premier quiz"));
                        }
                        
                        yourAdapter.notifyDataSetChanged();
                    } else {
                        Log.w(TAG, "Erreur lors du chargement de vos quizzes", task.getException());
                        // En cas d'erreur, afficher un message
                        yourQuizzes.clear();
                        yourQuizzes.add(createDemoQuiz("Erreur de chargement", 
                            "Impossible de charger vos quizzes pour le moment"));
                        yourAdapter.notifyDataSetChanged();
                    }
                });
        } catch (Exception e) {
            Log.e(TAG, "Exception lors du chargement de vos quizzes: " + e.getMessage());
            // En cas d'erreur, afficher un message
            yourQuizzes.clear();
            yourQuizzes.add(createDemoQuiz("Erreur de chargement", 
                "Impossible de charger vos quizzes pour le moment"));
            yourAdapter.notifyDataSetChanged();
        }
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
            
            // Récupérer la liste des IDs de questions
            List<String> questionIds = (List<String>) document.get("questionIds");
            if (questionIds != null) {
                quiz.setQuestionIds(questionIds);
            }
            
            return quiz;
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de la conversion du document en quiz", e);
            return null;
        }
    }
    
    private void syncDemoQuestionsToFirestore() {
        if (getContext() == null) return;
        
        Toast.makeText(getContext(), "Synchronisation des questions de démonstration...", Toast.LENGTH_SHORT).show();
        
        FirestoreUtils.saveDemoQuestionsToFirestore(new FirestoreUtils.OnOperationCompleteListener() {
            @Override
            public void onSuccess() {
                if (getContext() == null) return;
                Toast.makeText(getContext(), "Questions de démonstration synchronisées avec succès!", Toast.LENGTH_SHORT).show();
                
                // Recharger les données
                loadData();
            }
            
            @Override
            public void onError(Exception e) {
                if (getContext() == null) return;
                Toast.makeText(getContext(), "Erreur lors de la synchronisation: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Erreur lors de la synchronisation des questions de démo", e);
            }
        });
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
        
        Quiz quiz4 = new Quiz(
            "demo4",
            "Sciences naturelles",
            "Testez vos connaissances sur la faune et la flore",
            null,
            "system",
            "Quiz Système"
        );
        quiz4.setPlayCount(98);
        quiz4.setRating(4.3);
        demoQuizzes.add(quiz4);
        
        Quiz quiz5 = new Quiz(
            "demo5",
            "Géographie mondiale",
            "Pays, capitales, fleuves et montagnes du monde",
            null,
            "system",
            "Quiz Système"
        );
        quiz5.setPlayCount(156);
        quiz5.setRating(4.6);
        demoQuizzes.add(quiz5);
        
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