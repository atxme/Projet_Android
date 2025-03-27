package com.example.quiz.adapter;

import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.quiz.R;
import com.example.quiz.model.Quiz;
import com.example.quiz.util.MediaUtils;

import java.util.List;

public class QuizAdapter extends RecyclerView.Adapter<QuizAdapter.QuizViewHolder> {
    private List<Quiz> quizzes;
    private OnQuizClickListener listener;

    public interface OnQuizClickListener {
        void onQuizClick(Quiz quiz);
    }

    public QuizAdapter(List<Quiz> quizzes) {
        this.quizzes = quizzes;
    }
    
    public QuizAdapter(List<Quiz> quizzes, OnQuizClickListener listener) {
        this.quizzes = quizzes;
        this.listener = listener;
    }

    @NonNull
    @Override
    public QuizViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_quiz, parent, false);
        return new QuizViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull QuizViewHolder holder, int position) {
        Quiz quiz = quizzes.get(position);
        holder.bind(quiz);
    }

    @Override
    public int getItemCount() {
        return quizzes.size();
    }
    
    public void updateQuizzes(List<Quiz> newQuizzes) {
        this.quizzes.clear();
        this.quizzes.addAll(newQuizzes);
        notifyDataSetChanged();
    }

    public class QuizViewHolder extends RecyclerView.ViewHolder {
        private final ImageView imageQuiz;
        private final TextView textTitle;
        private final TextView textAuthor;
        private final TextView textPlayCount;
        private final RatingBar ratingBar;

        public QuizViewHolder(@NonNull View itemView) {
            super(itemView);
            
            imageQuiz = itemView.findViewById(R.id.imageQuiz);
            textTitle = itemView.findViewById(R.id.textTitle);
            textAuthor = itemView.findViewById(R.id.textAuthor);
            textPlayCount = itemView.findViewById(R.id.textPlayCount);
            ratingBar = itemView.findViewById(R.id.ratingBar);
            
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onQuizClick(quizzes.get(position));
                }
            });
        }

        public void bind(Quiz quiz) {
            textTitle.setText(quiz.getTitle());
            textAuthor.setText(quiz.getAuthorName());
            textPlayCount.setText(String.format("%d joués", quiz.getPlayCount()));
            ratingBar.setRating((float) quiz.getRating());
            
            // Charger l'image (de base64 si disponible)
            String imageUrl = quiz.getImageUrl();
            if (imageUrl != null && !imageUrl.isEmpty()) {
                // Vérifier si c'est du base64 ou un chemin
                if (imageUrl.startsWith("http")) {
                    // URL - à gérer avec une bibliothèque comme Glide
                    // Note: normalement nous n'utilisons pas Firebase Storage
                    imageQuiz.setImageResource(R.drawable.ic_quiz_placeholder);
                } else if (imageUrl.length() > 100) {
                    // Probablement du base64
                    Bitmap bitmap = MediaUtils.base64ToImage(imageUrl);
                    if (bitmap != null) {
                        imageQuiz.setImageBitmap(bitmap);
                    } else {
                        imageQuiz.setImageResource(R.drawable.ic_quiz_placeholder);
                    }
                } else {
                    // Probablement un chemin local
                    imageQuiz.setImageResource(R.drawable.ic_quiz_placeholder);
                    // Le chargement réel nécessiterait le contexte
                    // File file = MediaUtils.getMediaFile(itemView.getContext(), imageUrl);
                    // imageQuiz.setImageURI(Uri.fromFile(file));
                }
            } else {
                // Aucune image disponible
                imageQuiz.setImageResource(R.drawable.ic_quiz_placeholder);
            }
        }
    }
} 