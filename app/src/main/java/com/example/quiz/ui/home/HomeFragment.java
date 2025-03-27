package com.example.quiz.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;

import com.example.quiz.R;
import com.example.quiz.databinding.FragmentHomeBinding;
import com.example.quiz.model.Quiz;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private QuizAdapter recentQuizAdapter;
    private QuizAdapter popularQuizAdapter;
    private QuizAdapter myQuizAdapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Configuration des RecyclerViews
        setupRecyclerViews();

        // Charger les données de démonstration
        loadDemoQuizzes();

        // Configurer le FAB
        FloatingActionButton fab = binding.fabCreateGame;
        fab.setOnClickListener(v -> {
            // Navigation vers l'écran de configuration de jeu
            NavController navController = Navigation.findNavController(view);
            navController.navigate(R.id.action_home_to_gameSetup);
        });
    }

    private void setupRecyclerViews() {
        // Adapter pour les quiz récents
        recentQuizAdapter = new QuizAdapter(new ArrayList<>(), quiz -> {
            // Navigation vers les détails du quiz
            NavController navController = Navigation.findNavController(requireView());
            Bundle args = new Bundle();
            args.putString("quizId", quiz.getId());
            navController.navigate(R.id.action_home_to_quizDetails, args);
        });

        // Adapter pour les quiz populaires
        popularQuizAdapter = new QuizAdapter(new ArrayList<>(), quiz -> {
            // Navigation vers les détails du quiz
            NavController navController = Navigation.findNavController(requireView());
            Bundle args = new Bundle();
            args.putString("quizId", quiz.getId());
            navController.navigate(R.id.action_home_to_quizDetails, args);
        });

        // Adapter pour mes quiz
        myQuizAdapter = new QuizAdapter(new ArrayList<>(), quiz -> {
            // Navigation vers les détails du quiz
            NavController navController = Navigation.findNavController(requireView());
            Bundle args = new Bundle();
            args.putString("quizId", quiz.getId());
            navController.navigate(R.id.action_home_to_quizDetails, args);
        });

        // Configurer les RecyclerViews
        RecyclerView recentQuizzesRecycler = binding.recyclerRecentQuizzes;
        recentQuizzesRecycler.setAdapter(recentQuizAdapter);

        RecyclerView popularQuizzesRecycler = binding.recyclerPopularQuizzes;
        popularQuizzesRecycler.setAdapter(popularQuizAdapter);

        RecyclerView myQuizzesRecycler = binding.recyclerMyQuizzes;
        myQuizzesRecycler.setAdapter(myQuizAdapter);
    }

    private void loadDemoQuizzes() {
        // Données de démonstration pour les quiz récents
        List<Quiz> recentQuizzes = createDemoQuizzes("Quiz récent", 5);
        recentQuizAdapter.updateQuizzes(recentQuizzes);

        // Données de démonstration pour les quiz populaires
        List<Quiz> popularQuizzes = createDemoQuizzes("Quiz populaire", 5);
        popularQuizAdapter.updateQuizzes(popularQuizzes);

        // Données de démonstration pour mes quiz
        List<Quiz> myQuizzes = createDemoQuizzes("Mon quiz", 5);
        myQuizAdapter.updateQuizzes(myQuizzes);
    }
    
    private List<Quiz> createDemoQuizzes(String prefix, int count) {
        List<Quiz> quizzes = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            Quiz quiz = new Quiz(
                "quiz_" + i,
                prefix + " " + i,
                "Description du " + prefix.toLowerCase() + " " + i,
                "Science",
                "Biologie",
                "user_1",
                Quiz.GameMode.STANDARD,
                3
            );
            quizzes.add(quiz);
        }
        return quizzes;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
} 