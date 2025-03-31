package com.example.quiz.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.quiz.R;

import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder> {

    private List<String> categories;
    private final OnCategoryClickListener listener;

    public interface OnCategoryClickListener {
        void onCategoryClick(String category);
    }

    public CategoryAdapter(List<String> categories, OnCategoryClickListener listener) {
        this.categories = categories;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_category, parent, false);
        return new CategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        String category = categories.get(position);
        holder.bind(category, listener);
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    public static class CategoryViewHolder extends RecyclerView.ViewHolder {
        private final TextView textCategory;
        private final CardView cardView;

        public CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            textCategory = itemView.findViewById(R.id.textCategory);
            cardView = itemView.findViewById(R.id.cardCategory);
        }

        public void bind(final String category, final OnCategoryClickListener listener) {
            textCategory.setText(category);

            // Définir la couleur de fond en fonction de la catégorie
            int colorRes;
            switch (category.toLowerCase()) {
                case "sciences":
                    colorRes = android.R.color.holo_blue_light;
                    break;
                case "technologie":
                    colorRes = android.R.color.holo_green_light;
                    break;
                case "histoire":
                    colorRes = android.R.color.holo_orange_light;
                    break;
                case "géographie":
                    colorRes = android.R.color.holo_purple;
                    break;
                case "cinéma":
                    colorRes = android.R.color.holo_red_light;
                    break;
                default:
                    colorRes = android.R.color.darker_gray;
                    break;
            }
            cardView.setCardBackgroundColor(itemView.getContext().getColor(colorRes));

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onCategoryClick(category);
                }
            });
        }
    }
} 