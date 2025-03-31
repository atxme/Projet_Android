package com.example.quiz.ui.quiz;

import android.os.Bundle;
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
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.quiz.R;
import com.example.quiz.model.Question;
import com.example.quiz.model.Quiz;
import com.example.quiz.util.MediaUtils;

import java.util.ArrayList;
import java.util.List;

public class PlayQuizFragment extends Fragment {
    private static final String TAG = "PlayQuizFragment";
    
    private String quizId;
    private QuizViewModel viewModel;
    
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
    
    private boolean questionAnswered = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_play_quiz, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Initialiser le ViewModel
        viewModel = new ViewModelProvider(this).get(QuizViewModel.class);
        
        // Récupérer l'ID du quiz passé en argument
        if (getArguments() != null) {
            quizId = getArguments().getString("quizId");
        }
        
        // Initialiser les vues
        initViews(view);
        
        // Configurer les listeners
        setupListeners();
        
        // Observer les changements dans le ViewModel
        setupObservers();
        
        // Charger le quiz
        if (quizId != null && !quizId.isEmpty()) {
            viewModel.loadQuiz(quizId);
        } else {
            Toast.makeText(getContext(), "ID de quiz invalide", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void initViews(View view) {
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
        
        // Cacher le bouton suivant au début
        buttonNext.setVisibility(View.GONE);
    }
    
    private void setupListeners() {
        buttonValidate.setOnClickListener(v -> validateAnswer());
        
        buttonNext.setOnClickListener(v -> {
            // Vérifier si c'est la dernière question
            if (viewModel.getCurrentQuestionIndex().getValue() < viewModel.getQuestions().getValue().size() - 1) {
                // Passer à la question suivante
                viewModel.nextQuestion();
            } else {
                // Afficher les résultats
                showResults();
            }
        });
    }
    
    private void setupObservers() {
        // Observer le chargement
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            if (isLoading) {
                progressBar.setVisibility(View.VISIBLE);
            } else {
                progressBar.setVisibility(View.VISIBLE); // Garder visible pour montrer la progression
            }
        });
        
        // Observer les erreurs
        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), errorMessage -> {
            if (errorMessage != null && !errorMessage.isEmpty()) {
                Toast.makeText(getContext(), errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
        
        // Observer le quiz courant
        viewModel.getCurrentQuiz().observe(getViewLifecycleOwner(), quiz -> {
            if (quiz != null) {
                textQuizTitle.setText(quiz.getTitle());
            }
        });
        
        // Observer la liste des questions
        viewModel.getQuestions().observe(getViewLifecycleOwner(), questions -> {
            if (questions != null && !questions.isEmpty()) {
                progressBar.setMax(questions.size());
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
            option.setBackgroundResource(android.R.drawable.btn_radio);
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
        
        // Valider la réponse
        boolean isCorrect = (selectedOptionIndex == currentQuestion.getCorrectAnswerIndex());
        questionAnswered = true;
        
        // Mettre à jour l'interface
        if (isCorrect) {
            radioOptions[selectedOptionIndex].setBackgroundResource(R.drawable.option_correct_background);
            viewModel.updateScore(10); // 10 points par bonne réponse
        } else {
            radioOptions[selectedOptionIndex].setBackgroundResource(R.drawable.option_incorrect_background);
            if (currentQuestion.getCorrectAnswerIndex() >= 0 && currentQuestion.getCorrectAnswerIndex() < radioOptions.length) {
                radioOptions[currentQuestion.getCorrectAnswerIndex()].setBackgroundResource(R.drawable.option_correct_background);
            }
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
    
    private void showResults() {
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
} 