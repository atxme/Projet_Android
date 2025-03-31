package com.example.quiz.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Modèle représentant une question dans un quiz
 */
public class Question {
    
    /**
     * Types de questions possibles
     */
    public enum Type {
        SINGLE_CHOICE,     // Choix unique
        MULTIPLE_CHOICE,   // Choix multiple
        FREE_TEXT,         // Texte libre
        FILL_IN_BLANKS,    // Texte à trous
        MATCHING           // Association
    }
    
    private String id;
    private String text;
    private String imageUrl;
    private String videoUrl;
    private List<String> options;
    private int correctAnswerIndex;
    private String explanation;
    private int difficulty; // 1-5
    private String category;
    private long createdAt;
    private String authorId;
    private Type type;
    
    /**
     * Constructeur vide requis pour Firestore
     */
    public Question() {
        options = new ArrayList<>();
        type = Type.SINGLE_CHOICE;
    }
    
    /**
     * Constructeur complet
     */
    public Question(String id, String text, String imageUrl, String videoUrl, 
            List<String> options, int correctAnswerIndex, String explanation, 
            int difficulty, String category, long createdAt, String authorId) {
        this.id = id;
        this.text = text;
        this.imageUrl = imageUrl;
        this.videoUrl = videoUrl;
        this.options = options != null ? options : new ArrayList<>();
        this.correctAnswerIndex = correctAnswerIndex;
        this.explanation = explanation;
        this.difficulty = difficulty;
        this.category = category;
        this.createdAt = createdAt;
        this.authorId = authorId;
        this.type = Type.SINGLE_CHOICE;
    }
    
    /**
     * Constructeur avec type spécifié
     */
    public Question(String id, String text, String imageUrl, String videoUrl, 
            List<String> options, int correctAnswerIndex, String explanation, 
            int difficulty, String category, long createdAt, String authorId, Type type) {
        this(id, text, imageUrl, videoUrl, options, correctAnswerIndex, explanation, 
                difficulty, category, createdAt, authorId);
        this.type = type;
    }
    
    /**
     * Crée une Question à partir d'un Map Firestore
     */
    public static Question fromMap(Map<String, Object> map, String id) {
        Question question = new Question();
        question.id = id;
        question.text = (String) map.get("text");
        question.imageUrl = (String) map.get("imageUrl");
        question.videoUrl = (String) map.get("videoUrl");
        question.options = (List<String>) map.get("options");
        
        if (map.get("correctAnswerIndex") instanceof Long) {
            question.correctAnswerIndex = ((Long) map.get("correctAnswerIndex")).intValue();
        } else if (map.get("correctAnswerIndex") instanceof Integer) {
            question.correctAnswerIndex = (Integer) map.get("correctAnswerIndex");
        }
        
        question.explanation = (String) map.get("explanation");
        
        if (map.get("difficulty") instanceof Long) {
            question.difficulty = ((Long) map.get("difficulty")).intValue();
        } else if (map.get("difficulty") instanceof Integer) {
            question.difficulty = (Integer) map.get("difficulty");
        }
        
        question.category = (String) map.get("category");
        
        if (map.get("createdAt") instanceof Long) {
            question.createdAt = (Long) map.get("createdAt");
        }
        
        question.authorId = (String) map.get("authorId");
        
        // Récupérer le type de question
        String typeStr = (String) map.get("type");
        if (typeStr != null) {
            try {
                question.type = Type.valueOf(typeStr);
            } catch (IllegalArgumentException e) {
                question.type = Type.SINGLE_CHOICE; // Type par défaut
            }
        } else {
            question.type = Type.SINGLE_CHOICE; // Type par défaut
        }
        
        return question;
    }
    
    /**
     * Convertit l'objet Question en Map pour Firestore
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("text", text);
        map.put("imageUrl", imageUrl);
        map.put("videoUrl", videoUrl);
        map.put("options", options);
        map.put("correctAnswerIndex", correctAnswerIndex);
        map.put("explanation", explanation);
        map.put("difficulty", difficulty);
        map.put("category", category);
        map.put("createdAt", createdAt);
        map.put("authorId", authorId);
        map.put("type", type != null ? type.name() : Type.SINGLE_CHOICE.name());
        return map;
    }
    
    /**
     * Mélange les options tout en conservant la correspondance avec la réponse correcte
     */
    public void shuffleOptions() {
        if (options != null && options.size() > 1) {
            String correctAnswer = options.get(correctAnswerIndex);
            Collections.shuffle(options);
            correctAnswerIndex = options.indexOf(correctAnswer);
        }
    }

    // Getters et Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }

    public List<String> getOptions() {
        return options;
    }

    public void setOptions(List<String> options) {
        this.options = options;
    }

    public int getCorrectAnswerIndex() {
        return correctAnswerIndex;
    }

    public void setCorrectAnswerIndex(int correctAnswerIndex) {
        this.correctAnswerIndex = correctAnswerIndex;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public int getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(int difficulty) {
        this.difficulty = difficulty;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public String getAuthorId() {
        return authorId;
    }

    public void setAuthorId(String authorId) {
        this.authorId = authorId;
    }
    
    public Type getType() {
        return type;
    }
    
    public void setType(Type type) {
        this.type = type;
    }
} 