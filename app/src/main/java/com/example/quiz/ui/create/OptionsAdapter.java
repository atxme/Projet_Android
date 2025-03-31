package com.example.quiz.ui.create;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioButton;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.quiz.R;

import java.util.List;

/**
 * Adaptateur pour gérer les options lors de la création d'une question
 */
public class OptionsAdapter extends RecyclerView.Adapter<OptionsAdapter.OptionViewHolder> {
    
    private List<String> options;
    private int correctAnswerIndex = 0;
    private OnOptionSelectedListener onOptionSelectedListener;
    private OnOptionRemovedListener onOptionRemovedListener;
    
    /**
     * Interface pour gérer la sélection d'une option comme réponse correcte
     */
    public interface OnOptionSelectedListener {
        void onOptionSelected(int position);
    }
    
    /**
     * Interface pour gérer la suppression d'une option
     */
    public interface OnOptionRemovedListener {
        void onOptionRemoved(int position);
    }
    
    /**
     * Constructeur
     * @param options Liste des options
     * @param onOptionSelectedListener Listener pour la sélection d'une option
     * @param onOptionRemovedListener Listener pour la suppression d'une option
     */
    public OptionsAdapter(List<String> options, OnOptionSelectedListener onOptionSelectedListener,
                         OnOptionRemovedListener onOptionRemovedListener) {
        this.options = options;
        this.onOptionSelectedListener = onOptionSelectedListener;
        this.onOptionRemovedListener = onOptionRemovedListener;
    }
    
    /**
     * Définir l'index de la réponse correcte
     * @param index Index de la réponse correcte
     */
    public void setCorrectAnswerIndex(int index) {
        this.correctAnswerIndex = index;
    }

    @NonNull
    @Override
    public OptionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_option, parent, false);
        return new OptionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OptionViewHolder holder, int position) {
        String option = options.get(position);
        holder.bind(option, position);
    }

    @Override
    public int getItemCount() {
        return options.size();
    }
    
    /**
     * ViewHolder pour représenter une option
     */
    class OptionViewHolder extends RecyclerView.ViewHolder {
        private EditText editTextOption;
        private RadioButton radioButtonCorrect;
        private ImageButton buttonRemoveOption;
        
        public OptionViewHolder(@NonNull View itemView) {
            super(itemView);
            editTextOption = itemView.findViewById(R.id.editTextOption);
            radioButtonCorrect = itemView.findViewById(R.id.radioButtonCorrect);
            buttonRemoveOption = itemView.findViewById(R.id.buttonRemoveOption);
        }
        
        /**
         * Lier les données à la vue
         * @param option Texte de l'option
         * @param position Position dans la liste
         */
        public void bind(String option, final int position) {
            // Définir le texte de l'option
            editTextOption.setText(option);
            
            // Ajouter un TextWatcher pour mettre à jour la liste des options
            editTextOption.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}
                
                @Override
                public void afterTextChanged(Editable s) {
                    options.set(position, s.toString());
                }
            });
            
            // Vérifier si cette option est la réponse correcte
            radioButtonCorrect.setChecked(position == correctAnswerIndex);
            
            // Configurer les listeners
            radioButtonCorrect.setOnClickListener(v -> {
                if (onOptionSelectedListener != null) {
                    onOptionSelectedListener.onOptionSelected(position);
                }
            });
            
            buttonRemoveOption.setOnClickListener(v -> {
                if (onOptionRemovedListener != null) {
                    onOptionRemovedListener.onOptionRemoved(position);
                }
            });
            
            // Cacher le bouton de suppression si c'est la seule option
            buttonRemoveOption.setVisibility(options.size() > 1 ? View.VISIBLE : View.GONE);
        }
    }
} 