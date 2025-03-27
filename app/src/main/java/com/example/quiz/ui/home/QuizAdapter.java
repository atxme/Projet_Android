package com.example.quiz.ui.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.quiz.R;
import com.example.quiz.model.Quiz;

import java.util.List;

public class QuizAdapter extends RecyclerView.Adapter<QuizAdapter.QuizViewHolder> {

    private List<Quiz> quizzes;
    private final OnQuizClickListener listener;

    public interface OnQuizClickListener {
        void onQuizClick(Quiz quiz);
    }

    public QuizAdapter(List<Quiz> quizzes, OnQuizClickListener listener) {
        this.quizzes = quizzes;
        this.listener = listener;
    }

    public void updateQuizzes(List<Quiz> newQuizzes) {
        this.quizzes = newQuizzes;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public QuizViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_quiz_card, parent, false);
        return new QuizViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull QuizViewHolder holder, int position) {
        Quiz quiz = quizzes.get(position);
        holder.bind(quiz, listener);
    }

    @Override
    public int getItemCount() {
        return quizzes.size();
    }

    static class QuizViewHolder extends RecyclerView.ViewHolder {
        private final ImageView imageQuiz;
        private final TextView textTitle;
        private final TextView textCategory;
        private final TextView textQuestions;
        private final TextView textDifficulty;

        public QuizViewHolder(@NonNull View itemView) {
            super(itemView);
            imageQuiz = itemView.findViewById(R.id.image_quiz);
            textTitle = itemView.findViewById(R.id.text_quiz_title);
            textCategory = itemView.findViewById(R.id.text_quiz_category);
            textQuestions = itemView.findViewById(R.id.text_quiz_questions);
            textDifficulty = itemView.findViewById(R.id.text_quiz_difficulty);
        }

        public void bind(Quiz quiz, OnQuizClickListener listener) {
            textTitle.setText(quiz.getTitle());
            textCategory.setText(quiz.getCategory());
            
            int questionCount = quiz.getQuestions() != null ? quiz.getQuestions().size() : 0;
            textQuestions.setText(questionCount + " questions");
            
            textDifficulty.setText("Difficulté: " + quiz.getDifficulty() + "/5");

            // Charger l'image du quiz si disponible (à implémenter)
            // Pour l'instant on utilise une image par défaut
            Glide.with(itemView.getContext())
                .load(R.drawable.default_quiz_image)
                .centerCrop()
                .into(imageQuiz);

            // Gestionnaire de clic
            itemView.setOnClickListener(v -> listener.onQuizClick(quiz));
        }
    }
} 