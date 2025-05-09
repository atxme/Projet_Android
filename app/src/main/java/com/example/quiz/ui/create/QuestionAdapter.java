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
    
    public interface OnQuestionActionListener {
        void onEditQuestion(int position);
        void onDeleteQuestion(int position);
    }
    
    private final List<Question> questions;
    private final OnQuestionActionListener listener;
    
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
        private final TextView textQuestionText;
        private final TextView textQuestionType;
        private final ImageButton buttonEdit;
        private final ImageButton buttonDelete;
        
        public QuestionViewHolder(@NonNull View itemView) {
            super(itemView);
            textQuestionText = itemView.findViewById(R.id.textQuestionText);
            textQuestionType = itemView.findViewById(R.id.textQuestionType);
            buttonEdit = itemView.findViewById(R.id.buttonEdit);
            buttonDelete = itemView.findViewById(R.id.buttonDelete);
        }
        
        public void bind(Question question, int position) {
            // Afficher le texte de la question (tronqué si nécessaire)
            String questionText = question.getText();
            if (questionText.length() > 100) {
                questionText = questionText.substring(0, 97) + "...";
            }
            textQuestionText.setText(questionText);
            
            // Afficher le type de question
            Question.Type type = question.getType();
            switch (type) {
                case SINGLE_CHOICE:
                    textQuestionType.setText("Choix unique");
                    break;
                case MULTIPLE_CHOICE:
                    textQuestionType.setText("Choix multiple");
                    break;
                case FREE_TEXT:
                    textQuestionType.setText("Texte libre");
                    break;
                case FILL_IN_BLANKS:
                    textQuestionType.setText("À trous");
                    break;
                case MATCHING:
                    textQuestionType.setText("Association");
                    break;
                default:
                    textQuestionType.setText("Type inconnu");
                    break;
            }
            
            // Configurer les boutons
            buttonEdit.setOnClickListener(v -> {
                if (listener != null) {
                    int adapterPosition = getAdapterPosition();
                    if (adapterPosition != RecyclerView.NO_POSITION) {
                        listener.onEditQuestion(adapterPosition);
                    }
                }
            });
            
            buttonDelete.setOnClickListener(v -> {
                if (listener != null) {
                    int adapterPosition = getAdapterPosition();
                    if (adapterPosition != RecyclerView.NO_POSITION) {
                        listener.onDeleteQuestion(adapterPosition);
                    }
                }
            });
        }
    }
} 