package com.example.quiz.ui.create;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
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
import com.example.quiz.util.FirestoreUtils;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

public class CreateQuestionFragment extends Fragment {
    private static final String TAG = "CreateQuestionFragment";
    
    private TextInputEditText editTextQuestionText;
    private EditText editTextExplanation;
    private EditText editTextCategory;
    private RadioGroup radioGroupType;
    private RadioButton radioButtonSingleChoice;
    private Button buttonAddOption;
    private Button buttonSaveQuestion;
    private RecyclerView recyclerViewOptions;
    
    private OptionsAdapter optionsAdapter;
    private List<String> options = new ArrayList<>();
    private int correctAnswerIndex = 0;
    
    private boolean isNewQuestion = true;
    private String questionId = null;
    private Question currentQuestion = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_create_question, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Référencer les vues
        editTextQuestionText = view.findViewById(R.id.editTextQuestionText);
        editTextExplanation = view.findViewById(R.id.editTextExplanation);
        editTextCategory = view.findViewById(R.id.editTextCategory);
        radioGroupType = view.findViewById(R.id.radioGroupType);
        radioButtonSingleChoice = view.findViewById(R.id.radioButtonSingleChoice);
        buttonAddOption = view.findViewById(R.id.buttonAddOption);
        buttonSaveQuestion = view.findViewById(R.id.buttonSaveQuestion);
        recyclerViewOptions = view.findViewById(R.id.recyclerViewOptions);
        
        // Par défaut, sélectionner "Choix unique"
        radioButtonSingleChoice.setChecked(true);
        
        // Configurer le RecyclerView pour les options
        recyclerViewOptions.setLayoutManager(new LinearLayoutManager(getContext()));
        optionsAdapter = new OptionsAdapter(options, position -> {
            correctAnswerIndex = position;
            optionsAdapter.setCorrectAnswerIndex(correctAnswerIndex);
            optionsAdapter.notifyDataSetChanged();
        }, position -> {
            // Supprimer l'option
            options.remove(position);
            if (correctAnswerIndex >= position && correctAnswerIndex > 0) {
                correctAnswerIndex--;
            }
            optionsAdapter.setCorrectAnswerIndex(correctAnswerIndex);
            optionsAdapter.notifyDataSetChanged();
        });
        recyclerViewOptions.setAdapter(optionsAdapter);
        
        // Récupérer les arguments
        if (getArguments() != null) {
            isNewQuestion = getArguments().getBoolean("isNewQuestion", true);
            questionId = getArguments().getString("questionId");
            
            if (!isNewQuestion && questionId != null) {
                // Charger la question existante
                loadQuestion(questionId);
            }
        }
        
        // Ajouter une option par défaut s'il s'agit d'une nouvelle question
        if (isNewQuestion && options.isEmpty()) {
            options.add("");
            optionsAdapter.notifyDataSetChanged();
        }
        
        // Configurer les listeners
        buttonAddOption.setOnClickListener(v -> {
            options.add("");
            optionsAdapter.notifyDataSetChanged();
        });
        
        buttonSaveQuestion.setOnClickListener(v -> saveQuestion());
    }
    
    private void loadQuestion(String questionId) {
        // TODO: Implémenter le chargement d'une question existante depuis Firestore
        // Pour l'instant, nous utilisons un objet vide
        currentQuestion = new Question();
        currentQuestion.setId(questionId);
        
        // Remplir l'interface avec les données de la question
        if (currentQuestion != null) {
            editTextQuestionText.setText(currentQuestion.getText());
            editTextExplanation.setText(currentQuestion.getExplanation());
            editTextCategory.setText(currentQuestion.getCategory());
            
            // Définir le type de question
            if (currentQuestion.getType() == Question.Type.SINGLE_CHOICE) {
                radioButtonSingleChoice.setChecked(true);
            } else {
                // Gérer d'autres types si nécessaire
            }
            
            // Remplir les options
            if (currentQuestion.getOptions() != null) {
                options.clear();
                options.addAll(currentQuestion.getOptions());
                correctAnswerIndex = currentQuestion.getCorrectAnswerIndex();
                optionsAdapter.setCorrectAnswerIndex(correctAnswerIndex);
                optionsAdapter.notifyDataSetChanged();
            }
        }
    }
    
    private void saveQuestion() {
        // Valider les entrées
        String questionText = editTextQuestionText.getText().toString().trim();
        String explanation = editTextExplanation.getText().toString().trim();
        String category = editTextCategory.getText().toString().trim();
        
        if (TextUtils.isEmpty(questionText)) {
            editTextQuestionText.setError("Le texte de la question est requis");
            editTextQuestionText.requestFocus();
            return;
        }
        
        if (options.size() < 2) {
            Toast.makeText(getContext(), "Ajoutez au moins deux options", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Vérifier que toutes les options ont du texte
        for (int i = 0; i < options.size(); i++) {
            if (TextUtils.isEmpty(options.get(i))) {
                Toast.makeText(getContext(), "Toutes les options doivent avoir du texte", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        
        // Créer ou mettre à jour l'objet Question
        Question question;
        if (!isNewQuestion && currentQuestion != null) {
            question = currentQuestion;
        } else {
            question = new Question();
            question.setId(FirebaseUtils.generateUniqueId());
        }
        
        question.setText(questionText);
        question.setExplanation(explanation);
        question.setCategory(category);
        question.setType(Question.Type.SINGLE_CHOICE); // Par défaut pour l'instant
        question.setOptions(new ArrayList<>(options));
        question.setCorrectAnswerIndex(correctAnswerIndex);
        question.setDifficulty(2); // Difficulté moyenne par défaut
        question.setAuthorId(FirebaseAuth.getInstance().getCurrentUser() != null ? 
                FirebaseAuth.getInstance().getCurrentUser().getUid() : "offline_user");
        question.setCreatedAt(System.currentTimeMillis());
        
        // Sauvegarder la question dans Firestore
        FirestoreUtils.addQuestion(question, new FirestoreUtils.OnOperationCompleteListener() {
            @Override
            public void onSuccess() {
                // Retourner au fragment de création de quiz
                Toast.makeText(getContext(), "Question sauvegardée avec succès", Toast.LENGTH_SHORT).show();
                NavController navController = Navigation.findNavController(requireView());
                navController.navigate(R.id.action_create_question_to_create_quiz);
            }
            
            @Override
            public void onError(Exception e) {
                Toast.makeText(getContext(), "Erreur lors de la sauvegarde de la question: " + e.getMessage(), 
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    // Classe utilitaire pour générer un ID unique pour les questions en mode hors ligne
    private static class FirebaseUtils {
        public static String generateUniqueId() {
            return "offline_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 1000);
        }
    }
} 