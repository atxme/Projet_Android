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
import com.google.android.material.chip.Chip;

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
        private final ImageView difficultyStar1;
        private final ImageView difficultyStar2;
        private final ImageView difficultyStar3;
        private final ImageView difficultyStar4;
        private final ImageView difficultyStar5;

        public QuizViewHolder(@NonNull View itemView) {
            super(itemView);
            imageQuiz = itemView.findViewById(R.id.image_quiz);
            textTitle = itemView.findViewById(R.id.text_quiz_title);
            textCategory = itemView.findViewById(R.id.text_quiz_category);
            textQuestions = itemView.findViewById(R.id.text_quiz_questions);
            difficultyStar1 = itemView.findViewById(R.id.difficulty_star_1);
            difficultyStar2 = itemView.findViewById(R.id.difficulty_star_2);
            difficultyStar3 = itemView.findViewById(R.id.difficulty_star_3);
            difficultyStar4 = itemView.findViewById(R.id.difficulty_star_4);
            difficultyStar5 = itemView.findViewById(R.id.difficulty_star_5);
        }

        public void bind(Quiz quiz, OnQuizClickListener listener) {
            textTitle.setText(quiz.getTitle());
            textCategory.setText(quiz.getCategory());
            
            // Badge de catégorie
            Chip chipCategory = itemView.findViewById(R.id.chip_category);
            if (chipCategory != null) {
                // Limiter la longueur du texte de la catégorie
                String categoryText = quiz.getCategory();
                if (categoryText != null && categoryText.length() > 12) {
                    categoryText = categoryText.substring(0, 10) + "...";
                }
                chipCategory.setText(categoryText);
                
                // Définir la couleur du chip en fonction de la catégorie
                chipCategory.setChipBackgroundColorResource(getCategoryColorResource(quiz.getCategory()));
                
                // S'assurer que le chip est visible
                chipCategory.setVisibility(View.VISIBLE);
            }
            
            int questionCount = quiz.getQuestions() != null ? quiz.getQuestions().size() : 0;
            textQuestions.setText(questionCount + " questions");
            
            // Conversion de la difficulté
            int difficulty;
            if (quiz.getDifficultyLevel() > 0) {
                // Utiliser la valeur de difficultyLevel si elle est définie
                difficulty = quiz.getDifficultyLevel();
            } else {
                // Sinon, faire une conversion approximative basée sur le texte
                String difficultyText = quiz.getDifficulty();
                if (difficultyText == null || difficultyText.isEmpty()) {
                    difficulty = 3; // Valeur par défaut: moyen
                } else if (difficultyText.equalsIgnoreCase("Facile")) {
                    difficulty = 1;
                } else if (difficultyText.equalsIgnoreCase("Moyen")) {
                    difficulty = 3;
                } else if (difficultyText.equalsIgnoreCase("Difficile")) {
                    difficulty = 5;
                } else {
                    // Essayer de parser une valeur numérique si présente (ex: "4/5")
                    try {
                        if (difficultyText.contains("/")) {
                            difficulty = Integer.parseInt(difficultyText.split("/")[0]);
                        } else {
                            difficulty = Integer.parseInt(difficultyText);
                        }
                    } catch (NumberFormatException e) {
                        difficulty = 3; // Valeur par défaut en cas d'erreur
                    }
                }
            }
            
            // Limiter la difficulté entre 1 et 5
            difficulty = Math.max(1, Math.min(5, difficulty));
            
            updateDifficultyStars(difficulty);

            // Charger l'image du quiz si disponible (à implémenter)
            // Pour l'instant on utilise une image par défaut
            if (quiz.getImageUrl() != null && !quiz.getImageUrl().isEmpty()) {
                // Si une URL d'image est disponible, la charger avec Glide
                Glide.with(itemView.getContext())
                    .load(quiz.getImageUrl())
                    .placeholder(R.drawable.default_quiz_image)
                    .error(R.drawable.default_quiz_image)
                    .centerCrop()
                    .into(imageQuiz);
            } else {
                // Sinon, utiliser une image de fond basée sur la catégorie
                int placeholderColor = getCategoryColor(quiz.getCategory());
                imageQuiz.setBackgroundColor(placeholderColor);
                
                // Charger l'image par défaut
                Glide.with(itemView.getContext())
                    .load(R.drawable.default_quiz_image)
                    .centerCrop()
                    .into(imageQuiz);
            }

            // Gestionnaire de clic
            itemView.setOnClickListener(v -> listener.onQuizClick(quiz));
        }
        
        private void updateDifficultyStars(int difficulty) {
            int colorActive = itemView.getContext().getResources().getColor(R.color.purple_500);
            int colorInactive = itemView.getContext().getResources().getColor(android.R.color.darker_gray);
            
            difficultyStar1.setColorFilter(difficulty >= 1 ? colorActive : colorInactive);
            difficultyStar2.setColorFilter(difficulty >= 2 ? colorActive : colorInactive);
            difficultyStar3.setColorFilter(difficulty >= 3 ? colorActive : colorInactive);
            difficultyStar4.setColorFilter(difficulty >= 4 ? colorActive : colorInactive);
            difficultyStar5.setColorFilter(difficulty >= 5 ? colorActive : colorInactive);
        }

        /**
         * Retourne une couleur en fonction de la catégorie du quiz
         */
        private int getCategoryColor(String category) {
            if (category == null || category.isEmpty()) {
                return itemView.getContext().getResources().getColor(R.color.purple_200);
            }
            
            // Couleurs différentes selon les catégories
            category = category.toLowerCase();
            
            if (category.contains("techno") || category.contains("science")) {
                return itemView.getContext().getResources().getColor(R.color.category_tech);
            } else if (category.contains("histoire") || category.contains("géographie")) {
                return itemView.getContext().getResources().getColor(R.color.category_history);
            } else if (category.contains("cinéma") || category.contains("film") || category.contains("série")) {
                return itemView.getContext().getResources().getColor(R.color.category_cinema);
            } else if (category.contains("sport") || category.contains("jeux") || category.contains("loisir")) {
                return itemView.getContext().getResources().getColor(R.color.category_sports);
            } else if (category.contains("musique") || category.contains("art")) {
                return itemView.getContext().getResources().getColor(R.color.category_music);
            } else if (category.contains("culture") || category.contains("pop")) {
                return itemView.getContext().getResources().getColor(R.color.category_culture);
            } else {
                return itemView.getContext().getResources().getColor(R.color.category_other);
            }
        }

        /**
         * Retourne une couleur en fonction de la catégorie du quiz
         */
        private int getCategoryColorResource(String category) {
            if (category == null || category.isEmpty()) {
                return R.color.purple_200;
            }
            
            // Couleurs différentes selon les catégories
            category = category.toLowerCase();
            
            if (category.contains("techno") || category.contains("science")) {
                return R.color.category_tech;
            } else if (category.contains("histoire") || category.contains("géographie")) {
                return R.color.category_history;
            } else if (category.contains("cinéma") || category.contains("film") || category.contains("série")) {
                return R.color.category_cinema;
            } else if (category.contains("sport") || category.contains("jeux") || category.contains("loisir")) {
                return R.color.category_sports;
            } else if (category.contains("musique") || category.contains("art")) {
                return R.color.category_music;
            } else if (category.contains("culture") || category.contains("pop")) {
                return R.color.category_culture;
            } else {
                return R.color.category_other;
            }
        }
    }
} 