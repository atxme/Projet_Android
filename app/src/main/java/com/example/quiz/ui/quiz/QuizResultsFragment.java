package com.example.quiz.ui.quiz;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.quiz.R;

public class QuizResultsFragment extends Fragment {
    private static final String TAG = "QuizResultsFragment";
    
    private int score;
    private int totalQuestions;
    
    // UI components
    private TextView textResultsTitle;
    private ImageView imageTrophy;
    private TextView textFinalScore;
    private TextView textPercentage;
    private TextView textMessage;
    private Button buttonBackToHome;
    private Button buttonReplayQuiz;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_quiz_results, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Récupérer les arguments
        if (getArguments() != null) {
            score = getArguments().getInt("score", 0);
            totalQuestions = getArguments().getInt("totalQuestions", 0);
        }
        
        // Initialiser les vues
        initViews(view);
        
        // Mettre à jour l'interface utilisateur
        updateUI();
        
        // Configurer les boutons
        setupButtons();
    }
    
    private void initViews(View view) {
        textResultsTitle = view.findViewById(R.id.textResultsTitle);
        imageTrophy = view.findViewById(R.id.imageTrophy);
        textFinalScore = view.findViewById(R.id.textFinalScore);
        textPercentage = view.findViewById(R.id.textPercentage);
        textMessage = view.findViewById(R.id.textMessage);
        buttonBackToHome = view.findViewById(R.id.buttonBackToHome);
        buttonReplayQuiz = view.findViewById(R.id.buttonReplayQuiz);
    }
    
    private void updateUI() {
        // Afficher le score
        textFinalScore.setText(String.format("Score: %d/%d", score, totalQuestions));
        
        // Calculer et afficher le pourcentage
        int percentage = totalQuestions > 0 ? (score * 100) / totalQuestions : 0;
        textPercentage.setText(String.format("%d%%", percentage));
        
        // Définir le message en fonction du score
        if (percentage >= 80) {
            textMessage.setText("Félicitations! Vous avez un excellent score!");
        } else if (percentage >= 60) {
            textMessage.setText("Bien joué! Votre score est bon.");
        } else if (percentage >= 40) {
            textMessage.setText("Pas mal! Vous pouvez vous améliorer.");
        } else {
            textMessage.setText("Continuez à vous entraîner pour améliorer votre score.");
        }
    }
    
    private void setupButtons() {
        // Bouton pour retourner à l'accueil
        buttonBackToHome.setOnClickListener(v -> {
            try {
                NavController navController = Navigation.findNavController(requireView());
                navController.navigate(R.id.action_results_to_home);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        
        // Bouton pour rejouer le quiz
        buttonReplayQuiz.setOnClickListener(v -> {
            // Navigation arrière vers le PlayQuizFragment
            try {
                requireActivity().onBackPressed();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
} 