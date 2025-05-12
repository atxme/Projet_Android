package com.example.quiz.ui.quiz;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.quiz.R;
import com.example.quiz.util.UserStatsManager;
import com.google.firebase.auth.FirebaseAuth;

public class QuizResultsFragment extends Fragment {
    private static final String TAG = "QuizResultsFragment";
    
    private int score;
    private int totalQuestions;
    private UserStatsManager statsManager;
    private boolean statsUpdated = false;
    
    // UI components
    private TextView textResultsTitle;
    private ImageView imageTrophy;
    private TextView textFinalScore;
    private TextView textPercentage;
    private TextView textMessage;
    private Button buttonBackToHome;
    private Button buttonReplayQuiz;
    private ProgressBar progressUpdatingStats;
    private TextView textStatsStatus;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_quiz_results, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Initialiser le gestionnaire de statistiques
        statsManager = new UserStatsManager();
        
        // Récupérer les arguments
        if (getArguments() != null) {
            score = getArguments().getInt("score", 0);
            totalQuestions = getArguments().getInt("totalQuestions", 0);
        }
        
        // Initialiser les vues
        initViews(view);
        
        // Mettre à jour l'interface utilisateur
        updateUI();
        
        // Sauvegarder les statistiques
        updateUserStats();
        
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
        
        // Éléments UI pour le statut de mise à jour des statistiques
        progressUpdatingStats = view.findViewById(R.id.progress_updating_stats);
        textStatsStatus = view.findViewById(R.id.text_stats_status);
        
        // Cacher ces éléments par défaut s'ils existent
        if (progressUpdatingStats != null) {
            progressUpdatingStats.setVisibility(View.GONE);
        }
        
        if (textStatsStatus != null) {
            textStatsStatus.setVisibility(View.GONE);
        }
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
    
    /**
     * Met à jour les statistiques de l'utilisateur sur Firebase
     */
    private void updateUserStats() {
        // Éviter les mises à jour multiples si l'utilisateur revient sur cette page
        if (statsUpdated) return;
        
        if (statsManager.isUserLoggedIn()) {
            // Afficher l'indicateur de chargement si disponible
            if (progressUpdatingStats != null) {
                progressUpdatingStats.setVisibility(View.VISIBLE);
            }
            
            if (textStatsStatus != null) {
                textStatsStatus.setVisibility(View.VISIBLE);
                textStatsStatus.setText("Mise à jour des statistiques...");
            }
            
            // Mettre à jour les statistiques
            statsManager.updateStats(totalQuestions, score);
            statsUpdated = true;
            
            // Cacher l'indicateur après un délai
            if (getView() != null) {
                getView().postDelayed(() -> {
                    if (isAdded() && progressUpdatingStats != null) {
                        progressUpdatingStats.setVisibility(View.GONE);
                    }
                    
                    if (isAdded() && textStatsStatus != null) {
                        textStatsStatus.setText("Statistiques mises à jour");
                        
                        // Faire disparaître le message après quelques secondes
                        getView().postDelayed(() -> {
                            if (isAdded() && textStatsStatus != null) {
                                textStatsStatus.setVisibility(View.GONE);
                            }
                        }, 2000);
                    }
                }, 1000);
            }
            
            Log.d(TAG, "Statistiques mises à jour: " + totalQuestions + " questions, " + score + " correctes");
        } else {
            Log.d(TAG, "Utilisateur non connecté, statistiques non sauvegardées");
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