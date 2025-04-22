package com.example.quiz.ui.create;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.quiz.R;
import com.example.quiz.model.Question;
import com.example.quiz.model.Quiz;
import com.example.quiz.util.FirestoreUtils;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class QuestionManagerFragment extends Fragment {
    private static final String TAG = "QuestionManagerFragment";

    private String quizId;
    private Quiz currentQuiz;
    private List<Question> questions = new ArrayList<>();
    private RecyclerView recyclerViewQuestions;
    private TextView textQuizTitle;
    private TextView textQuestionCount;
    private Button buttonBack;
    private FloatingActionButton fabAddQuestion;
    private QuestionAdapter adapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate a layout for this fragment
        return inflater.inflate(R.layout.fragment_question_manager, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Récupérer l'ID du quiz
        if (getArguments() != null) {
            quizId = getArguments().getString("quizId");
        }

        // Initialiser les vues
        initViews(view);

        // Charger le quiz et ses questions
        if (quizId != null && !quizId.isEmpty()) {
            loadQuizData();
        } else {
            Toast.makeText(getContext(), "ID de quiz invalide", Toast.LENGTH_SHORT).show();
            navigateBack();
        }
    }

    private void initViews(View view) {
        recyclerViewQuestions = view.findViewById(R.id.recyclerViewQuestions);
        textQuizTitle = view.findViewById(R.id.textQuizTitle);
        textQuestionCount = view.findViewById(R.id.textQuestionCount);
        buttonBack = view.findViewById(R.id.buttonBack);
        fabAddQuestion = view.findViewById(R.id.fabAddQuestion);

        // Configurer le RecyclerView
        recyclerViewQuestions.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new QuestionAdapter(questions, new QuestionAdapter.OnQuestionActionListener() {
            @Override
            public void onEditQuestion(int position) {
                editQuestion(position);
            }

            @Override
            public void onDeleteQuestion(int position) {
                deleteQuestion(position);
            }
        });
        recyclerViewQuestions.setAdapter(adapter);

        // Configurer les écouteurs de clics
        buttonBack.setOnClickListener(v -> navigateBack());
        fabAddQuestion.setOnClickListener(v -> createNewQuestion());
    }

    private void loadQuizData() {
        FirestoreUtils.loadQuiz(quizId, new FirestoreUtils.OnQuizLoadedListener() {
            @Override
            public void onQuizLoaded(Quiz quiz) {
                if (getActivity() == null) return;

                currentQuiz = quiz;
                textQuizTitle.setText(quiz.getTitle());

                // Charger les questions du quiz
                loadQuestions(quiz);
            }

            @Override
            public void onError(Exception e) {
                if (getActivity() == null) return;

                Log.e(TAG, "Erreur lors du chargement du quiz", e);
                Toast.makeText(getContext(), "Erreur: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                navigateBack();
            }
        });
    }

    private void loadQuestions(Quiz quiz) {
        FirestoreUtils.loadQuestionsForQuiz(quiz, new FirestoreUtils.OnQuestionsLoadedListener() {
            @Override
            public void onQuestionsLoaded(List<Question> loadedQuestions) {
                if (getActivity() == null) return;

                questions.clear();
                questions.addAll(loadedQuestions);
                adapter.notifyDataSetChanged();
                updateQuestionCount();
            }

            @Override
            public void onError(Exception e) {
                if (getActivity() == null) return;

                Log.e(TAG, "Erreur lors du chargement des questions", e);
                Toast.makeText(getContext(), "Erreur: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateQuestionCount() {
        textQuestionCount.setText(getString(R.string.question_count, questions.size()));
    }

    private void createNewQuestion() {
        // Naviguer vers le fragment de création de question
        NavController navController = Navigation.findNavController(requireView());
        Bundle args = new Bundle();
        args.putBoolean("isNewQuestion", true);
        args.putString("quizId", quizId);
        navController.navigate(R.id.action_question_manager_to_create_question, args);
    }

    private void editQuestion(int position) {
        // Naviguer vers le fragment d'édition de question
        Question question = questions.get(position);
        NavController navController = Navigation.findNavController(requireView());
        Bundle args = new Bundle();
        args.putBoolean("isNewQuestion", false);
        args.putString("questionId", question.getId());
        args.putString("quizId", quizId);
        navController.navigate(R.id.action_question_manager_to_create_question, args);
    }

    private void deleteQuestion(int position) {
        Question questionToDelete = questions.get(position);
        
        // Supprimer la question de Firestore
        FirestoreUtils.deleteQuestion(questionToDelete.getId(), quizId, new FirestoreUtils.OnOperationCompleteListener() {
            @Override
            public void onSuccess() {
                if (getActivity() == null) return;
                
                // Supprimer localement
                questions.remove(position);
                adapter.notifyDataSetChanged();
                updateQuestionCount();
                Toast.makeText(getContext(), "Question supprimée", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(Exception e) {
                if (getActivity() == null) return;
                
                Log.e(TAG, "Erreur lors de la suppression de la question", e);
                Toast.makeText(getContext(), "Erreur: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void navigateBack() {
        if (getActivity() != null) {
            getActivity().onBackPressed();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Recharger les questions à la reprise pour refléter les modifications
        if (quizId != null && !quizId.isEmpty() && currentQuiz != null) {
            loadQuestions(currentQuiz);
        }
    }
} 