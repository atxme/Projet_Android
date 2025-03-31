package com.example.quiz.ui.create;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.quiz.R;
import com.example.quiz.model.Question;

import java.util.List;

public class QuestionAdapter extends RecyclerView.Adapter<QuestionAdapter.QuestionViewHolder> {
    
    private List<Question> questions;
    private OnQuestionActionListener listener;
    
    public interface OnQuestionActionListener {
        void onEditQuestion(int position);
        void onDeleteQuestion(int position);
    }
    
    public QuestionAdapter(List<Question> questions, OnQuestionActionListener listener) {
        this.questions = questions;
        this.listener = listener;
    }

    @NonNull
    @Override
    public QuestionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_question, parent, false);
        return new QuestionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull QuestionViewHolder holder, int position) {
        Question question = questions.get(position);
        holder.bind(question, position);
    }

    @Override
    public int getItemCount() {
        return questions.size();
    }
    
    class QuestionViewHolder extends RecyclerView.ViewHolder {
        private TextView textQuestionText;
        private TextView textQuestionType;
        private TextView textAnswerCount;
        private ImageButton buttonEditQuestion;
        private ImageButton buttonDeleteQuestion;
        
        public QuestionViewHolder(@NonNull View itemView) {
            super(itemView);
            textQuestionText = itemView.findViewById(R.id.textQuestionText);
            textQuestionType = itemView.findViewById(R.id.textQuestionType);
            textAnswerCount = itemView.findViewById(R.id.textAnswerCount);
            buttonEditQuestion = itemView.findViewById(R.id.buttonEditQuestion);
            buttonDeleteQuestion = itemView.findViewById(R.id.buttonDeleteQuestion);
        }
        
        public void bind(Question question, int position) {
            textQuestionText.setText(question.getText());
            
            // Définir le type de question
            String questionType = getQuestionTypeText(question.getType());
            textQuestionType.setText(questionType);
            
            // Afficher le nombre de réponses
            int optionsCount = question.getOptions() != null ? question.getOptions().size() : 0;
            textAnswerCount.setText(String.format("%d réponses", optionsCount));
            
            // Gérer les clics sur les boutons
            buttonEditQuestion.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onEditQuestion(position);
                }
            });
            
            buttonDeleteQuestion.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDeleteQuestion(position);
                }
            });
        }
        
        private String getQuestionTypeText(Question.Type type) {
            if (type == null) {
                return "Type inconnu";
            }
            
            switch (type) {
                case SINGLE_CHOICE:
                    return "Choix unique";
                case MULTIPLE_CHOICE:
                    return "Choix multiple";
                case FREE_TEXT:
                    return "Texte libre";
                case FILL_IN_BLANKS:
                    return "Texte à trous";
                case MATCHING:
                    return "Association";
                default:
                    return "Type inconnu";
            }
        }
    }
} 