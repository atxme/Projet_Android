package com.example.quiz.ui.create;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.quiz.R;
import com.example.quiz.model.Question;
import com.example.quiz.model.Quiz;
import com.example.quiz.util.FirestoreUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

public class CreateQuizFragment extends Fragment {
    private static final String TAG = "CreateQuizFragment";
    
    private EditText editTextTitle;
    private EditText editTextDescription;
    private EditText editTextCategory;
    private Button buttonAddQuestion;
    private Button buttonSaveQuiz;
    private RecyclerView recyclerViewQuestions;
    
    private List<Question> questions = new ArrayList<>();
    private QuestionAdapter adapter;
    
    private FirebaseAuth mAuth;
    private String quizId;
    private boolean isEditing = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_create_quiz, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Initialiser Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        
        // Référencer les vues
        editTextTitle = view.findViewById(R.id.editTextTitle);
        editTextDescription = view.findViewById(R.id.editTextDescription);
        editTextCategory = view.findViewById(R.id.editTextCategory);
        buttonAddQuestion = view.findViewById(R.id.buttonAddQuestion);
        buttonSaveQuiz = view.findViewById(R.id.buttonSaveQuiz);
        recyclerViewQuestions = view.findViewById(R.id.recyclerViewQuestions);
        
        // Configurer le RecyclerView
        recyclerViewQuestions.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new QuestionAdapter(questions, new QuestionAdapter.OnQuestionActionListener() {
            @Override
            public void onEditQuestion(int position) {
                // Ouvrir l'éditeur de question avec la question à modifier
                editQuestion(position);
            }

            @Override
            public void onDeleteQuestion(int position) {
                // Supprimer la question
                questions.remove(position);
                adapter.notifyDataSetChanged();
                updateQuestionCount();
            }
        });
        recyclerViewQuestions.setAdapter(adapter);
        
        // Vérifier si nous sommes en mode édition
        if (getArguments() != null && getArguments().containsKey("quizId")) {
            quizId = getArguments().getString("quizId");
            isEditing = true;
            
            // Charger le quiz existant
            loadQuiz(quizId);
        }
        
        // Configurer les listeners
        buttonAddQuestion.setOnClickListener(v -> addNewQuestion());
        
        buttonSaveQuiz.setOnClickListener(v -> saveQuiz());
        
        // Mettre à jour le compteur de questions
        updateQuestionCount();
    }
    
    private void loadQuiz(String quizId) {
        FirestoreUtils.loadQuiz(quizId, new FirestoreUtils.OnQuizLoadedListener() {
            @Override
            public void onQuizLoaded(Quiz quiz) {
                if (getActivity() == null) return;
                
                // Remplir les champs avec les données du quiz
                editTextTitle.setText(quiz.getTitle());
                editTextDescription.setText(quiz.getDescription());
                editTextCategory.setText(quiz.getCategory());
                
                // Charger les questions
                FirestoreUtils.loadQuestionsForQuiz(quiz, new FirestoreUtils.OnQuestionsLoadedListener() {
                    @Override
                    public void onQuestionsLoaded(List<Question> loadedQuestions) {
                        questions.clear();
                        questions.addAll(loadedQuestions);
                        adapter.notifyDataSetChanged();
                        updateQuestionCount();
                    }
                    
                    @Override
                    public void onError(Exception e) {
                        Log.e(TAG, "Erreur lors du chargement des questions", e);
                        Toast.makeText(getContext(), "Erreur lors du chargement des questions: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
            
            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Erreur lors du chargement du quiz", e);
                Toast.makeText(getContext(), "Erreur lors du chargement du quiz: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void addNewQuestion() {
        // Naviguer vers le fragment de création de question
        NavController navController = Navigation.findNavController(requireView());
        Bundle args = new Bundle();
        args.putBoolean("isNewQuestion", true);
        navController.navigate(R.id.action_create_quiz_to_create_question, args);
    }
    
    private void editQuestion(int position) {
        // Naviguer vers le fragment de création de question avec la question à éditer
        Question question = questions.get(position);
        NavController navController = Navigation.findNavController(requireView());
        Bundle args = new Bundle();
        args.putBoolean("isNewQuestion", false);
        args.putString("questionId", question.getId());
        // Vous devrez implémenter une méthode pour sérialiser/désérialiser la question
        // ou la stocker temporairement dans un singleton ou un ViewModel partagé
        navController.navigate(R.id.action_create_quiz_to_create_question, args);
    }
    
    private void saveQuiz() {
        String title = editTextTitle.getText().toString().trim();
        String description = editTextDescription.getText().toString().trim();
        String category = editTextCategory.getText().toString().trim();
        
        // Validation des champs
        if (TextUtils.isEmpty(title)) {
            editTextTitle.setError("Le titre est requis");
            editTextTitle.requestFocus();
            return;
        }
        
        if (TextUtils.isEmpty(description)) {
            editTextDescription.setError("La description est requise");
            editTextDescription.requestFocus();
            return;
        }
        
        if (questions.isEmpty()) {
            Toast.makeText(getContext(), "Ajoutez au moins une question", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Récupérer l'utilisateur actuellement connecté
        FirebaseUser currentUser = mAuth.getCurrentUser();
        String authorId = currentUser != null ? currentUser.getUid() : "offline_user";
        String authorName = currentUser != null ? currentUser.getDisplayName() : "Utilisateur Hors Ligne";
        
        if (TextUtils.isEmpty(authorName)) {
            authorName = "Utilisateur " + authorId.substring(0, 5);
        }
        
        // Créer ou mettre à jour le quiz
        Quiz quiz;
        if (isEditing && quizId != null) {
            // Mettre à jour un quiz existant
            quiz = new Quiz(quizId, title, description, null, authorId, authorName);
        } else {
            // Créer un nouveau quiz
            quiz = new Quiz("", title, description, null, authorId, authorName);
        }
        
        quiz.setCategory(category);
        quiz.setQuestions(new ArrayList<>(questions));
        
        // Liste des IDs de questions
        List<String> questionIds = new ArrayList<>();
        for (Question q : questions) {
            questionIds.add(q.getId());
        }
        quiz.setQuestionIds(questionIds);
        
        // Sauvegarder d'abord toutes les questions
        saveQuestions(quiz);
    }
    
    private void saveQuestions(Quiz quiz) {
        final int[] savedCount = {0};
        final boolean[] hasError = {false};
        final Exception[] lastError = {null};
        
        if (questions.isEmpty()) {
            // Pas de questions à sauvegarder, enregistrer le quiz directement
            saveQuizToFirestore(quiz);
            return;
        }
        
        // Sauvegarder chaque question
        for (Question question : questions) {
            FirestoreUtils.addQuestion(question, new FirestoreUtils.OnOperationCompleteListener() {
                @Override
                public void onSuccess() {
                    savedCount[0]++;
                    
                    // Vérifier si toutes les questions ont été sauvegardées
                    if (savedCount[0] >= questions.size()) {
                        if (!hasError[0]) {
                            // Toutes les questions ont été sauvegardées, enregistrer le quiz
                            saveQuizToFirestore(quiz);
                        } else {
                            // Il y a eu une erreur lors de l'enregistrement des questions
                            if (getContext() != null) {
                                Toast.makeText(getContext(), 
                                    "Erreur lors de l'enregistrement des questions: " + 
                                    (lastError[0] != null ? lastError[0].getMessage() : "Erreur inconnue"), 
                                    Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                }
                
                @Override
                public void onError(Exception e) {
                    savedCount[0]++;
                    hasError[0] = true;
                    lastError[0] = e;
                    
                    Log.e(TAG, "Erreur lors de l'enregistrement de la question", e);
                    
                    // Vérifier si toutes les questions ont été traitées
                    if (savedCount[0] >= questions.size() && getContext() != null) {
                        Toast.makeText(getContext(), 
                            "Erreur lors de l'enregistrement des questions: " + e.getMessage(), 
                            Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }
    
    private void saveQuizToFirestore(Quiz quiz) {
        FirestoreUtils.createQuiz(quiz, new FirestoreUtils.OnQuizCreatedListener() {
            @Override
            public void onSuccess(String quizId) {
                if (getContext() == null) return;
                
                Toast.makeText(getContext(), "Quiz enregistré avec succès", Toast.LENGTH_SHORT).show();
                
                // Naviguer vers l'écran d'accueil
                NavController navController = Navigation.findNavController(requireView());
                navController.navigate(R.id.action_create_quiz_to_home);
            }
            
            @Override
            public void onError(Exception e) {
                if (getContext() == null) return;
                
                Log.e(TAG, "Erreur lors de l'enregistrement du quiz", e);
                Toast.makeText(getContext(), "Erreur lors de l'enregistrement du quiz: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void updateQuestionCount() {
        buttonAddQuestion.setText(String.format("Ajouter une question (%d)", questions.size()));
    }
} 