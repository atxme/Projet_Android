package com.example.quiz.ui.create;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.quiz.R;
import com.example.quiz.model.Quiz;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

public class CreateQuizFragment extends Fragment {
    private static final String TAG = "CreateQuizFragment";
    
    private CreateQuizViewModel viewModel;
    private EditText editTextTitle;
    private EditText editTextDescription;
    private EditText editTextTimeLimit;
    private Spinner spinnerGameMode;
    private Button buttonSave;
    private Button buttonAddQuestions;
    private Switch switchPublish;
    
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
        
        // Initialiser le ViewModel
        viewModel = new ViewModelProvider(this).get(CreateQuizViewModel.class);
        
        // Initialiser les vues
        initViews(view);
        
        // Récupérer l'ID du quiz s'il s'agit d'une édition
        if (getArguments() != null && getArguments().containsKey("quizId")) {
            quizId = getArguments().getString("quizId");
            isEditing = true;
            
            // Charger le quiz existant
            viewModel.loadQuiz(quizId);
        }
        
        // Observer les changements du ViewModel
        setupObservers();
    }
    
    private void initViews(View view) {
        mAuth = FirebaseAuth.getInstance();
        
        // Référencer les vues
        editTextTitle = view.findViewById(R.id.editTextTitle);
        editTextDescription = view.findViewById(R.id.editTextDescription);
        editTextTimeLimit = view.findViewById(R.id.editTextTimeLimit);
        spinnerGameMode = view.findViewById(R.id.spinnerGameMode);
        buttonSave = view.findViewById(R.id.buttonSave);
        buttonAddQuestions = view.findViewById(R.id.buttonAddQuestions);
        switchPublish = view.findViewById(R.id.switchPublish);
        
        // Configurer le Spinner pour les modes de jeu
        setupGameModeSpinner();
        
        // Configurer les listeners
        buttonSave.setOnClickListener(v -> saveQuiz());
        buttonAddQuestions.setOnClickListener(v -> {
            // Enregistrer d'abord le quiz, puis naviguer vers l'écran d'ajout de questions
            String quizId = viewModel.getCurrentQuizId();
            if (quizId != null && !quizId.isEmpty()) {
                navigateToQuestionManager(quizId);
            } else {
                saveQuiz(() -> {
                    String newQuizId = viewModel.getCurrentQuizId();
                    navigateToQuestionManager(newQuizId);
                });
            }
        });
    }
    
    private void setupGameModeSpinner() {
        // Créer une liste des modes de jeu
        List<String> gameModeNames = new ArrayList<>();
        gameModeNames.add("Standard"); // Mode standard
        gameModeNames.add("Contre la montre"); // Mode timed
        gameModeNames.add("Réponses changeantes"); // Mode shuffle_options
        
        // Créer un adapter pour le spinner
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                gameModeNames
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerGameMode.setAdapter(adapter);
    }
    
    private void setupObservers() {
        // Observer le quiz en cours d'édition
        viewModel.getCurrentQuiz().observe(getViewLifecycleOwner(), quiz -> {
            if (quiz != null) {
                editTextTitle.setText(quiz.getTitle());
                editTextDescription.setText(quiz.getDescription());
                editTextTimeLimit.setText(String.valueOf(quiz.getTimeLimit()));
                switchPublish.setChecked(quiz.isPublished());
                
                // Mettre à jour le spinner avec le mode de jeu du quiz
                Quiz.GameMode gameMode = quiz.getGameMode();
                if (gameMode != null) {
                    int position = 0; // Position par défaut (STANDARD)
                    
                    switch (gameMode) {
                        case TIMED:
                            position = 1;
                            break;
                        case SHUFFLE_OPTIONS:
                            position = 2;
                            break;
                    }
                    
                    spinnerGameMode.setSelection(position);
                }
            }
        });
        
        // Observer les messages d'erreur
        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), errorMessage -> {
            if (errorMessage != null && !errorMessage.isEmpty()) {
                Toast.makeText(getContext(), errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
        
        // Observer le succès de sauvegarde
        viewModel.getSaveSuccess().observe(getViewLifecycleOwner(), success -> {
            if (success) {
                Toast.makeText(getContext(), "Quiz enregistré avec succès", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void saveQuiz() {
        saveQuiz(null);
    }
    
    private void saveQuiz(Runnable onSuccess) {
        String title = editTextTitle.getText().toString().trim();
        String description = editTextDescription.getText().toString().trim();
        String timeLimitStr = editTextTimeLimit.getText().toString().trim();
        boolean isPublished = switchPublish.isChecked();
        
        // Validation
        if (title.isEmpty()) {
            Toast.makeText(getContext(), "Veuillez saisir un titre", Toast.LENGTH_SHORT).show();
            return;
        }
        
        int timeLimit = 0;
        if (!timeLimitStr.isEmpty()) {
            try {
                timeLimit = Integer.parseInt(timeLimitStr);
            } catch (NumberFormatException e) {
                Toast.makeText(getContext(), "Limite de temps invalide", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        
        // Récupérer le mode de jeu sélectionné
        Quiz.GameMode gameMode = Quiz.GameMode.STANDARD; // Mode par défaut
        int selectedPosition = spinnerGameMode.getSelectedItemPosition();
        
        switch (selectedPosition) {
            case 1:
                gameMode = Quiz.GameMode.TIMED;
                
                // Si le mode contre la montre est sélectionné, mais aucune limite de temps n'est définie
                if (timeLimit <= 0) {
                    timeLimit = 10; // Valeur par défaut pour le mode contre la montre (10 secondes)
                    editTextTimeLimit.setText(String.valueOf(timeLimit));
                }
                break;
            case 2:
                gameMode = Quiz.GameMode.SHUFFLE_OPTIONS;
                break;
            case 0:
            default:
                gameMode = Quiz.GameMode.STANDARD;
                break;
        }
        
        // Sauvegarder le quiz
        viewModel.saveQuiz(title, description, timeLimit, gameMode, isPublished, onSuccess);
    }
    
    private void navigateToQuestionManager(String quizId) {
        if (quizId == null || quizId.isEmpty()) {
            Toast.makeText(getContext(), "Erreur: ID de quiz invalide", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Créer le bundle d'arguments
        Bundle args = new Bundle();
        args.putString("quizId", quizId);
        
        // Naviguer vers l'écran de gestion des questions
        try {
            NavController navController = Navigation.findNavController(requireView());
            navController.navigate(R.id.action_to_question_manager, args);
        } catch (Exception e) {
            Toast.makeText(getContext(), "Erreur: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
} 