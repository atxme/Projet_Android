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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.quiz.R;
import com.example.quiz.model.Quiz;
import com.example.quiz.util.FirestoreUtils;
import com.example.quiz.util.MediaUtils;

public class QuizDetailsFragment extends Fragment {
    private static final String TAG = "QuizDetailsFragment";
    
    private String quizId;
    private Quiz quiz;
    
    // UI components
    private ImageView imageQuiz;
    private TextView textQuizTitle;
    private TextView textQuizAuthor;
    private TextView textQuizCategory;
    private TextView textQuizRating;
    private TextView textQuestionCount;
    private TextView textPlayCount;
    private TextView textQuizDescription;
    private Button buttonStartQuiz;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_quiz_details, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Récupérer l'ID du quiz depuis les arguments
        if (getArguments() != null) {
            quizId = getArguments().getString("quizId");
            if (quizId == null || quizId.isEmpty()) {
                Toast.makeText(requireContext(), "ID de quiz invalide", Toast.LENGTH_SHORT).show();
                navigateBack();
                return;
            }
        } else {
            Toast.makeText(requireContext(), "Aucun quiz spécifié", Toast.LENGTH_SHORT).show();
            navigateBack();
            return;
        }
        
        // Initialiser les vues
        initViews(view);
        
        // Charger les détails du quiz
        loadQuizDetails();
    }
    
    private void initViews(View view) {
        imageQuiz = view.findViewById(R.id.imageQuiz);
        textQuizTitle = view.findViewById(R.id.textQuizTitle);
        textQuizAuthor = view.findViewById(R.id.textQuizAuthor);
        textQuizCategory = view.findViewById(R.id.textQuizCategory);
        textQuizRating = view.findViewById(R.id.textQuizRating);
        textQuestionCount = view.findViewById(R.id.textQuestionCount);
        textPlayCount = view.findViewById(R.id.textPlayCount);
        textQuizDescription = view.findViewById(R.id.textQuizDescription);
        buttonStartQuiz = view.findViewById(R.id.buttonStartQuiz);
        
        buttonStartQuiz.setOnClickListener(v -> startQuiz());
    }
    
    private void loadQuizDetails() {
        FirestoreUtils.loadQuiz(quizId, new FirestoreUtils.OnQuizLoadedListener() {
            @Override
            public void onQuizLoaded(Quiz loadedQuiz) {
                quiz = loadedQuiz;
                updateUI();
            }
            
            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Erreur lors du chargement du quiz", e);
                Toast.makeText(requireContext(), 
                        "Erreur lors du chargement du quiz: " + e.getMessage(), 
                        Toast.LENGTH_SHORT).show();
                navigateBack();
            }
        });
    }
    
    private void updateUI() {
        if (quiz == null) return;
        
        // Mettre à jour les détails du quiz
        textQuizTitle.setText(quiz.getTitle());
        textQuizAuthor.setText("Par: " + quiz.getAuthorName());
        
        // Afficher la catégorie si disponible
        if (quiz.getCategory() != null && !quiz.getCategory().isEmpty()) {
            textQuizCategory.setVisibility(View.VISIBLE);
            textQuizCategory.setText("Catégorie: " + quiz.getCategory());
        } else {
            textQuizCategory.setVisibility(View.GONE);
        }
        
        // Mettre à jour les statistiques
        textQuizRating.setText(String.format("%.1f / 5", quiz.getRating()));
        
        int questionCount = quiz.getQuestionIds() != null ? quiz.getQuestionIds().size() : 0;
        textQuestionCount.setText(String.valueOf(questionCount));
        
        textPlayCount.setText(String.valueOf(quiz.getPlayCount()));
        
        // Description
        textQuizDescription.setText(quiz.getDescription());
        
        // Image du quiz (si disponible)
        if (quiz.getImageUrl() != null && !quiz.getImageUrl().isEmpty()) {
            // Convertir l'image Base64 en Bitmap si nécessaire
            if (quiz.getImageUrl().startsWith("data:image") || 
                    quiz.getImageUrl().startsWith("/9j/")) {
                imageQuiz.setImageBitmap(MediaUtils.base64ToImage(quiz.getImageUrl()));
            } else {
                // Charger l'image depuis une URL (à implémenter)
                // Pour l'instant, utiliser une image par défaut
                imageQuiz.setImageResource(R.drawable.default_quiz_image);
            }
        } else {
            // Image par défaut
            imageQuiz.setImageResource(R.drawable.default_quiz_image);
        }
        
        // Activer le bouton de démarrage seulement si le quiz a des questions
        buttonStartQuiz.setEnabled(questionCount > 0);
        if (questionCount == 0) {
            buttonStartQuiz.setText("Aucune question disponible");
        }
    }
    
    private void startQuiz() {
        try {
            // Naviguer vers le fragment de jeu
            NavController navController = Navigation.findNavController(requireView());
            Bundle args = new Bundle();
            args.putString("quizId", quizId);
            navController.navigate(R.id.action_quiz_details_to_play_quiz, args);
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de la navigation vers le quiz", e);
            Toast.makeText(requireContext(), 
                    "Erreur: " + e.getMessage(), 
                    Toast.LENGTH_SHORT).show();
        }
    }
    
    private void navigateBack() {
        try {
            Navigation.findNavController(requireView()).popBackStack();
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de la navigation retour", e);
        }
    }
} 