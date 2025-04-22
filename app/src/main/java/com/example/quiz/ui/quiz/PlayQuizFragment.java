package com.example.quiz.ui.quiz;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.quiz.R;
import com.example.quiz.model.Question;
import com.example.quiz.model.Quiz;
import com.example.quiz.util.GameModeManager;
import com.example.quiz.util.MediaUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class PlayQuizFragment extends Fragment implements GameModeManager.GameModeListener {
    private static final String TAG = "PlayQuizFragment";
    
    private String quizId;
    private QuizViewModel viewModel;
    private GameModeManager gameModeManager;
    
    private TextView textQuizTitle;
    private ProgressBar progressBar;
    private TextView textProgress;
    private TextView textScore;
    private TextView textQuestionPrompt;
    private ImageView imageQuestion;
    private RadioGroup radioGroupOptions;
    private RadioButton[] radioOptions = new RadioButton[4];
    private TextView textExplanation;
    private Button buttonValidate;
    private Button buttonNext;
    private TextView textTimer;
    private TextView textGameMode;
    
    // Loading UI components
    private ConstraintLayout loadingContainer;
    private ConstraintLayout quizContentContainer;
    
    private boolean questionAnswered = false;
    private int timeRemaining = 0;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_play_quiz, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Initialiser le ViewModel en premier
        viewModel = new ViewModelProvider(this).get(QuizViewModel.class);
        
        // Initialiser le GameModeManager
        gameModeManager = new GameModeManager(this);
        
        // Récupérer l'id du quiz
        if (getArguments() != null) {
            quizId = getArguments().getString("quizId");
            // Récupérer le mode de jeu sélectionné, s'il est présent dans les arguments
            String gameModeStr = getArguments().getString("gameMode");
            if (gameModeStr != null && !gameModeStr.isEmpty()) {
                try {
                    // Convertir la chaîne en enum GameMode
                    Quiz.GameMode selectedGameMode = Quiz.GameMode.valueOf(gameModeStr);
                    // Stocker temporairement le mode sélectionné dans le ViewModel
                    viewModel.setSelectedGameMode(selectedGameMode);
                } catch (IllegalArgumentException e) {
                    Log.e(TAG, "Mode de jeu invalide: " + gameModeStr, e);
                }
            }
        }
        
        // Initialiser les vues
        initViews(view);
        
        // Configurer les observateurs
        setupObservers();
        
        // Configurer les écouteurs d'événements
        setupListeners();
        
        // Charger le quiz
        if (quizId != null && !quizId.isEmpty()) {
            viewModel.loadQuiz(quizId);
        } else {
            Toast.makeText(requireContext(), "ID de quiz manquant", Toast.LENGTH_SHORT).show();
            navigateBack();
        }
    }
    
    private void initViews(View view) {
        // Loading UI
        loadingContainer = view.findViewById(R.id.loadingContainer);
        quizContentContainer = view.findViewById(R.id.quizContentContainer);
        
        // Quiz content UI
        textQuizTitle = view.findViewById(R.id.textQuizTitle);
        progressBar = view.findViewById(R.id.progressBar);
        textProgress = view.findViewById(R.id.textProgress);
        textScore = view.findViewById(R.id.textScore);
        textQuestionPrompt = view.findViewById(R.id.textQuestionPrompt);
        imageQuestion = view.findViewById(R.id.imageQuestion);
        radioGroupOptions = view.findViewById(R.id.radioGroupOptions);
        radioOptions[0] = view.findViewById(R.id.radioOption1);
        radioOptions[1] = view.findViewById(R.id.radioOption2);
        radioOptions[2] = view.findViewById(R.id.radioOption3);
        radioOptions[3] = view.findViewById(R.id.radioOption4);
        textExplanation = view.findViewById(R.id.textExplanation);
        buttonValidate = view.findViewById(R.id.buttonValidate);
        buttonNext = view.findViewById(R.id.buttonNext);
        textTimer = view.findViewById(R.id.textTimer);
        textGameMode = view.findViewById(R.id.textGameMode);
        
        // Cacher le bouton suivant au début
        buttonNext.setVisibility(View.GONE);
        
        // Cacher le timer par défaut
        textTimer.setVisibility(View.GONE);
    }
    
    private void setupListeners() {
        buttonValidate.setOnClickListener(v -> validateAnswer());
        
        buttonNext.setOnClickListener(v -> {
            // Vérifier si c'est la dernière question
            if (viewModel.getCurrentQuestionIndex().getValue() < viewModel.getQuestions().getValue().size() - 1) {
                // Arrêter d'abord tout timer en cours
                gameModeManager.stopTimer();
                
                // Passer à la question suivante
                viewModel.nextQuestion();
                
                // Note: La réinitialisation du timer à 10s est maintenant gérée directement dans displayCurrentQuestion
                // qui sera appelée via l'observateur sur currentQuestionIndex
            } else {
                // Afficher les résultats
                showResults();
            }
        });
    }
    
    private void setupObservers() {
        // Observer le chargement
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            showLoading(isLoading);
        });
        
        // Observer les erreurs
        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), errorMessage -> {
            if (errorMessage != null && !errorMessage.isEmpty()) {
                Toast.makeText(getContext(), errorMessage, Toast.LENGTH_SHORT).show();
                showLoading(false);
            }
        });
        
        // Observer le quiz courant
        viewModel.getCurrentQuiz().observe(getViewLifecycleOwner(), quiz -> {
            if (quiz != null) {
                textQuizTitle.setText(quiz.getTitle());
                
                // Initialiser le mode de jeu
                Quiz.GameMode gameMode = viewModel.getSelectedGameMode();
                if (gameMode == null) {
                    gameMode = quiz.getGameMode();
                    if (gameMode == null) {
                        gameMode = Quiz.GameMode.STANDARD; // Mode par défaut
                    }
                }
                
                // Afficher le mode de jeu
                updateGameModeDisplay(gameMode);
                
                // Initialiser le GameModeManager avec le mode de jeu sélectionné
                gameModeManager.initializeGameMode(gameMode, quiz.getTimeLimit());
            }
        });
        
        // Observer la liste des questions
        viewModel.getQuestions().observe(getViewLifecycleOwner(), questions -> {
            if (questions != null && !questions.isEmpty()) {
                progressBar.setMax(questions.size());
                showLoading(false);
            }
        });
        
        // Observer l'index de la question courante
        viewModel.getCurrentQuestionIndex().observe(getViewLifecycleOwner(), index -> {
            if (index != null) {
                displayCurrentQuestion(index);
            }
        });
        
        // Observer le score
        viewModel.getScore().observe(getViewLifecycleOwner(), score -> {
            if (score != null) {
                textScore.setText(String.format("Score: %d", score));
            }
        });
        
        // Observer le temps restant
        viewModel.getTimeRemaining().observe(getViewLifecycleOwner(), time -> {
            if (time != null) {
                this.timeRemaining = time;
                if (gameModeManager.getCurrentGameMode() == Quiz.GameMode.TIMED) {
                    textTimer.setVisibility(View.VISIBLE);
                    textTimer.setText(String.format("Temps: %d s", time));
                }
            }
        });
    }
    
    private void updateGameModeDisplay(Quiz.GameMode gameMode) {
        if (textGameMode != null) {
            String modeName;
            switch (gameMode) {
                case TIMED:
                    modeName = "Mode: Contre la montre";
                    break;
                case SHUFFLE_OPTIONS:
                    modeName = "Mode: Réponses changeantes";
                    break;
                case STANDARD:
                default:
                    modeName = "Mode: Standard";
                    break;
            }
            textGameMode.setText(modeName);
            textGameMode.setVisibility(View.VISIBLE);
        }
    }
    
    private void showLoading(boolean isLoading) {
        if (isLoading) {
            loadingContainer.setVisibility(View.VISIBLE);
            quizContentContainer.setVisibility(View.GONE);
        } else {
            loadingContainer.setVisibility(View.GONE);
            quizContentContainer.setVisibility(View.VISIBLE);
        }
    }
    
    private void displayCurrentQuestion(int index) {
        List<Question> questions = viewModel.getQuestions().getValue();
        
        if (questions == null || questions.isEmpty() || index >= questions.size()) {
            return;
        }
        
        // Réinitialiser l'état
        questionAnswered = false;
        textExplanation.setVisibility(View.GONE);
        buttonValidate.setVisibility(View.VISIBLE);
        buttonNext.setVisibility(View.GONE);
        radioGroupOptions.clearCheck();
        
        // Réinitialiser le background de toutes les options
        for (RadioButton option : radioOptions) {
            option.setBackgroundResource(R.drawable.option_background);
            option.setEnabled(true);  // Réactiver les options pour la nouvelle question
        }
        
        // Obtenir la question courante
        Question question = questions.get(index);
        
        // Mettre à jour la progression
        progressBar.setProgress(index + 1);
        textProgress.setText(String.format("Question %d/%d", index + 1, questions.size()));
        
        // Afficher le texte de la question
        textQuestionPrompt.setText(question.getText());
        
        // Gérer l'image de la question si présente
        if (question.getImageUrl() != null && !question.getImageUrl().isEmpty()) {
            imageQuestion.setVisibility(View.VISIBLE);
            MediaUtils.loadImage(getContext(), question.getImageUrl(), imageQuestion);
        } else {
            imageQuestion.setVisibility(View.GONE);
        }
        
        // Afficher les options
        List<String> options = question.getOptions();
        if (options != null && options.size() > 0) {
            for (int i = 0; i < radioOptions.length; i++) {
                if (i < options.size()) {
                    radioOptions[i].setVisibility(View.VISIBLE);
                    radioOptions[i].setText(options.get(i));
                } else {
                    radioOptions[i].setVisibility(View.GONE);
                }
            }
        }
        
        // Réinitialiser et redémarrer le timer pour le mode contre la montre
        Quiz currentQuiz = viewModel.getCurrentQuiz().getValue();
        if ((currentQuiz != null && currentQuiz.getGameMode() == Quiz.GameMode.TIMED) || 
            gameModeManager.getCurrentGameMode() == Quiz.GameMode.TIMED) {
            // Mettre à jour l'affichage du temps restant dans le ViewModel
            viewModel.updateTimeRemaining(10);
            
            // Arrêter tout timer existant
            gameModeManager.stopTimer();
            
            // Pause pour s'assurer que l'arrêt est terminé
            new Handler().postDelayed(() -> {
                // Démarrer un nouveau timer de 10 secondes
                gameModeManager.initializeGameMode(Quiz.GameMode.TIMED, 10);
                
                // S'assurer que le timer est visible
                if (textTimer != null) {
                    textTimer.setVisibility(View.VISIBLE);
                    textTimer.setText("Temps: 10 s");
                }
            }, 100); // court délai pour assurer le redémarrage propre
        }
    }
    
    private void validateAnswer() {
        if (questionAnswered) return;
        
        Question currentQuestion = viewModel.getCurrentQuestion();
        if (currentQuestion == null) return;
        
        int selectedOptionId = radioGroupOptions.getCheckedRadioButtonId();
        
        if (selectedOptionId == -1) {
            Toast.makeText(getContext(), "Veuillez sélectionner une réponse", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Identifier l'index de l'option sélectionnée
        int selectedOptionIndex = -1;
        for (int i = 0; i < radioOptions.length; i++) {
            if (radioOptions[i].getId() == selectedOptionId) {
                selectedOptionIndex = i;
                break;
            }
        }
        
        // Arrêter le timer et le shuffling pour cette question
        gameModeManager.stopTimer();
        gameModeManager.stopShuffling();
        
        // Valider la réponse
        boolean isCorrect = (selectedOptionIndex == currentQuestion.getCorrectAnswerIndex());
        questionAnswered = true;
        
        // Calculer les points en fonction du mode de jeu
        int points = calculatePoints(isCorrect);
        
        // Mettre à jour l'interface
        if (isCorrect) {
            radioOptions[selectedOptionIndex].setBackgroundResource(R.drawable.option_correct_background);
            viewModel.updateScore(points); 
        } else {
            radioOptions[selectedOptionIndex].setBackgroundResource(R.drawable.option_incorrect_background);
            if (currentQuestion.getCorrectAnswerIndex() >= 0 && currentQuestion.getCorrectAnswerIndex() < radioOptions.length) {
                radioOptions[currentQuestion.getCorrectAnswerIndex()].setBackgroundResource(R.drawable.option_correct_background);
            }
            // Mettre à jour le score avec 0 points pour enregistrer la réponse incorrecte
            viewModel.updateScore(0);
        }
        
        // Désactiver toutes les options pour empêcher de changer de réponse
        for (RadioButton option : radioOptions) {
            option.setEnabled(false);
        }
        
        // Afficher l'explication
        textExplanation.setText(currentQuestion.getExplanation());
        textExplanation.setVisibility(View.VISIBLE);
        
        // Changer les boutons
        buttonValidate.setVisibility(View.GONE);
        
        // Vérifier si c'est la dernière question
        if (viewModel.getCurrentQuestionIndex().getValue() < viewModel.getQuestions().getValue().size() - 1) {
            buttonNext.setText("Question suivante");
        } else {
            buttonNext.setText("Voir les résultats");
        }
        
        buttonNext.setVisibility(View.VISIBLE);
    }
    
    /**
     * Calcule les points en fonction du mode de jeu actuel
     */
    private int calculatePoints(boolean isCorrect) {
        if (!isCorrect) return 0;
        
        Quiz currentQuiz = viewModel.getCurrentQuiz().getValue();
        if (currentQuiz == null) return 10; // Valeur par défaut
        
        Quiz.GameMode gameMode = currentQuiz.getGameMode();
        if (gameMode == null) gameMode = Quiz.GameMode.STANDARD;
        
        switch (gameMode) {
            case TIMED:
                // Points variables en fonction du temps restant
                return 10 + timeRemaining;
            case SHUFFLE_OPTIONS:
                // Points fixes pour le mode shuffle
                return 15;
            case STANDARD:
            default:
                // Points standards
                return 10;
        }
    }
    
    private void showResults() {
        gameModeManager.cleanup();
        viewModel.finishQuiz();
        
        // Créer un bundle avec les résultats pour le fragment de résultats
        Bundle bundle = new Bundle();
        bundle.putInt("score", viewModel.getScore().getValue());
        bundle.putInt("totalQuestions", viewModel.getQuestions().getValue().size());
        bundle.putString("quizId", quizId);
        
        // Naviguer vers l'écran de résultats
        try {
            NavController navController = Navigation.findNavController(requireView());
            navController.navigate(R.id.action_play_quiz_to_results, bundle);
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de la navigation vers les résultats", e);
            Toast.makeText(getContext(), "Erreur: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    @Override
    public void onTimeUpdated(int secondsRemaining) {
        if (isAdded() && getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                viewModel.updateTimeRemaining(secondsRemaining);
            });
        }
    }
    
    @Override
    public void onTimeUp() {
        if (isAdded() && getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                if (!questionAnswered) {
                    Toast.makeText(getContext(), "Temps écoulé !", Toast.LENGTH_SHORT).show();
                    
                    // Marquer la question comme répondue
                    questionAnswered = true;
                    
                    Question currentQuestion = viewModel.getCurrentQuestion();
                    if (currentQuestion != null) {
                        // Afficher la réponse correcte
                        int correctIndex = currentQuestion.getCorrectAnswerIndex();
                        if (correctIndex >= 0 && correctIndex < radioOptions.length) {
                            radioOptions[correctIndex].setBackgroundResource(R.drawable.option_correct_background);
                        }
                        
                        // Désactiver toutes les options
                        for (RadioButton option : radioOptions) {
                            option.setEnabled(false);
                        }
                        
                        // Afficher l'explication
                        textExplanation.setText(currentQuestion.getExplanation());
                        textExplanation.setVisibility(View.VISIBLE);
                        
                        // Cacher le bouton de validation
                        buttonValidate.setVisibility(View.GONE);
                        
                        // Enregistrer la réponse comme incorrecte
                        viewModel.updateScore(0);
                        
                        // Afficher le bouton pour continuer
                        if (viewModel.getCurrentQuestionIndex().getValue() < viewModel.getQuestions().getValue().size() - 1) {
                            buttonNext.setText("Question suivante");
                        } else {
                            buttonNext.setText("Voir les résultats");
                        }
                        buttonNext.setVisibility(View.VISIBLE);
                    }
                }
            });
        }
    }
    
    @Override
    public void onAnswerShuffled() {
        if (isAdded() && getActivity() != null && !questionAnswered) {
            getActivity().runOnUiThread(() -> {
                Question currentQuestion = viewModel.getCurrentQuestion();
                if (currentQuestion == null) return;
                
                // Obtenir les options actuelles et la réponse correcte
                List<String> options = new ArrayList<>(currentQuestion.getOptions());
                int correctIndex = currentQuestion.getCorrectAnswerIndex();
                String correctOption = options.get(correctIndex);
                
                // Mélanger les options
                Collections.shuffle(options);
                
                // Mettre à jour les RadioButtons
                for (int i = 0; i < radioOptions.length; i++) {
                    if (i < options.size()) {
                        radioOptions[i].setText(options.get(i));
                        
                        // Mettre à jour l'index de la réponse correcte
                        if (options.get(i).equals(correctOption)) {
                            currentQuestion.setCorrectAnswerIndex(i);
                        }
                    }
                }
            });
        }
    }
    
    @Override
    public void setAnswerEnabled(boolean enabled) {
        if (isAdded() && getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                for (RadioButton option : radioOptions) {
                    option.setEnabled(enabled);
                }
                buttonValidate.setEnabled(enabled);
            });
        }
    }
    
    /**
     * Méthode pour revenir à l'écran précédent
     */
    private void navigateBack() {
        try {
            Navigation.findNavController(requireView()).popBackStack();
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de la navigation retour", e);
        }
    }
    
    @Override
    public void onPause() {
        super.onPause();
        gameModeManager.pauseGameMode();
    }
    
    @Override
    public void onResume() {
        super.onResume();
        if (!questionAnswered) {
            gameModeManager.resumeGameMode(timeRemaining);
        }
    }
    
    @Override
    public void onDestroy() {
        gameModeManager.cleanup();
        super.onDestroy();
    }
} 