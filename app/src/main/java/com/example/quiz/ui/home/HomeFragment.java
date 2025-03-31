package com.example.quiz.ui.home;

import android.os.Bundle;
import android.os.Handler;
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
        FirestoreUtils.loadAllQuizzes(new FirestoreUtils.OnQuizzesLoadedListener() {
            @Override
            public void onQuizzesLoaded(List<Quiz> quizzes) {
                Log.d(TAG, "Quizzes chargés depuis Firestore: " + quizzes.size());
                
                if (quizzes != null && !quizzes.isEmpty()) {
                    // Utiliser tous les quizzes, qu'ils soient publiés ou non
                    List<Quiz> allQuizzes = new ArrayList<>(quizzes);
                    
                    // Log des quizzes récupérés
                    for (Quiz quiz : allQuizzes) {
                        Log.d(TAG, "Quiz: " + quiz.getTitle() + ", publié: " + quiz.isPublished() + ", ID: " + quiz.getId());
                    }
                    
                    // Trier par popularité et par date
                    List<Quiz> sortedByPopularity = new ArrayList<>(allQuizzes);
                    List<Quiz> sortedByDate = new ArrayList<>(allQuizzes);
                    
                    // Trier par nombre de parties
                    Collections.sort(sortedByPopularity, (q1, q2) -> 
                        Integer.compare(q2.getPlayCount(), q1.getPlayCount())
                    );
                    
                    // Trier par date de création (plus récent d'abord)
                    Collections.sort(sortedByDate, (q1, q2) -> 
                        Long.compare(q2.getCreatedAt(), q1.getCreatedAt())
                    );
                    
                    // Mettre à jour les listes
                    popularQuizzes.clear();
                    recentQuizzes.clear();
                    
                    // Limiter à 10 quizzes par catégorie (ou moins si pas assez de quizzes)
                    int popularLimit = Math.min(sortedByPopularity.size(), 10);
                    int recentLimit = Math.min(sortedByDate.size(), 10);
                    
                    popularQuizzes.addAll(sortedByPopularity.subList(0, popularLimit));
                    recentQuizzes.addAll(sortedByDate.subList(0, recentLimit));
                    
                    Log.d(TAG, "Quizzes populaires ajoutés: " + popularQuizzes.size());
                    Log.d(TAG, "Quizzes récents ajoutés: " + recentQuizzes.size());
                    
                    // Charger les questions pour chaque quiz
                    for (Quiz quiz : popularQuizzes) {
                        loadQuestionsForQuiz(quiz);
                    }
                    
                    for (Quiz quiz : recentQuizzes) {
                        if (!popularQuizzes.contains(quiz)) {
                            loadQuestionsForQuiz(quiz);
                        }
                    }
                    
                    // Mettre à jour les adapters
                    popularAdapter.notifyDataSetChanged();
                    recentAdapter.notifyDataSetChanged();
                } else {
                    Log.w(TAG, "Aucun quiz trouvé dans Firestore");
                    Toast.makeText(getContext(), "Création d'un quiz de démonstration...", Toast.LENGTH_SHORT).show();
                    createDemoQuiz();
                }
                
                progressBar.setVisibility(View.GONE);
            }
            
            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Erreur lors du chargement des quizzes: " + e.getMessage(), e);
                Toast.makeText(getContext(), "Erreur lors du chargement des quizzes: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(View.GONE);
            }
        });
    }
    
    private void refreshData() {
        loadData();
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
    
    /**
     * Créer un quiz de démonstration si aucun n'existe
     */
    private void createDemoQuiz() {
        // Créer quelques questions
        List<Question> demoQuestions = new ArrayList<>();
        
        // Question 1
        List<String> options1 = new ArrayList<>();
        options1.add("1969");
        options1.add("1971");
        options1.add("1975");
        options1.add("1965");
        
        Question q1 = new Question(
            "",
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
        );
        
        // Question 2
        List<String> options2 = new ArrayList<>();
        options2.add("Mer de la Tranquillité");
        options2.add("Mer de la Sérénité");
        options2.add("Océan des Tempêtes");
        options2.add("Mer des Pluies");
        
        Question q2 = new Question(
            "",
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
        );
        
        // Sauvegarder les questions
        FirestoreUtils.addQuestion(q1, new FirestoreUtils.OnOperationCompleteListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Question 1 ajoutée avec succès, ID: " + q1.getId());
                
                FirestoreUtils.addQuestion(q2, new FirestoreUtils.OnOperationCompleteListener() {
                    @Override
                    public void onSuccess() {
                        Log.d(TAG, "Question 2 ajoutée avec succès, ID: " + q2.getId());
                        
                        // Créer le quiz avec les questions
                        createQuizWithQuestions(Arrays.asList(q1.getId(), q2.getId()));
                    }
                    
                    @Override
                    public void onError(Exception e) {
                        Log.e(TAG, "Erreur lors de l'ajout de la question 2", e);
                    }
                });
            }
            
            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Erreur lors de l'ajout de la question 1", e);
            }
        });
    }
    
    /**
     * Créer un quiz avec les questions fournies
     */
    private void createQuizWithQuestions(List<String> questionIds) {
        Quiz demoQuiz = new Quiz(
            "quiz_demo",
            "Quiz de démonstration",
            "Un quiz pour tester l'application",
            null,
            "system",
            "Quiz Système"
        );
        demoQuiz.setQuestionIds(questionIds);
        demoQuiz.setPlayCount(10);
        demoQuiz.setRating(4.5);
        demoQuiz.setCategory("Technologie");
        demoQuiz.setDifficulty("Moyen");
        demoQuiz.setPublished(true);
        
        FirestoreUtils.createQuiz(demoQuiz, new FirestoreUtils.OnQuizCreatedListener() {
            @Override
            public void onSuccess(String quizId) {
                Log.d(TAG, "Quiz de démonstration créé avec succès, ID: " + quizId);
                Toast.makeText(getContext(), "Quiz de démonstration créé, actualisez la page", Toast.LENGTH_LONG).show();
                
                // Rafraîchir les données après 2 secondes
                new Handler().postDelayed(() -> refreshData(), 2000);
            }
            
            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Erreur lors de la création du quiz de démonstration", e);
            }
        });
    }
} 