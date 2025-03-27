package com.example.quiz.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Question {
    public enum Type {
        MULTIPLE_CHOICE,
        SINGLE_CHOICE,
        FREE_TEXT,
        FILL_IN_BLANKS,
        MATCHING
    }

    private String id;
    private String text;
    private Type type;
    private List<String> options;
    private List<String> correctAnswers;
    private String explanation;
    private String mediaUrl;
    private String mediaType; // "image", "video", "audio"
    private int points;
    private int timeLimit; // en secondes, 0 = pas de limite

    // Constructeur vide pour Firebase
    public Question() {
    }

    public Question(String id, String text, Type type, List<String> options, 
                   List<String> correctAnswers, String explanation) {
        this.id = id;
        this.text = text;
        this.type = type;
        this.options = options;
        this.correctAnswers = correctAnswers;
        this.explanation = explanation;
        this.points = 10; // valeur par défaut
        this.timeLimit = 0; // pas de limite par défaut
    }

    // Méthode pour mélanger les options
    public void shuffleOptions() {
        if (options != null && type != Type.MATCHING) {
            List<String> shuffled = new ArrayList<>(options);
            Collections.shuffle(shuffled);
            this.options = shuffled;
        }
    }
    
    // Méthode pour vérifier une réponse
    public boolean checkAnswer(List<String> userAnswers) {
        if (userAnswers == null || correctAnswers == null) {
            return false;
        }
        
        if (type == Type.FILL_IN_BLANKS || type == Type.FREE_TEXT) {
            // Pour ces types, nous vérifions si la réponse est incluse (insensible à la casse)
            if (userAnswers.size() != 1 || correctAnswers.size() != 1) {
                return false;
            }
            return userAnswers.get(0).trim().equalsIgnoreCase(correctAnswers.get(0).trim());
        } else if (type == Type.MULTIPLE_CHOICE) {
            // Toutes les bonnes réponses doivent être sélectionnées
            if (userAnswers.size() != correctAnswers.size()) {
                return false;
            }
            return new ArrayList<>(userAnswers).containsAll(correctAnswers);
        } else if (type == Type.SINGLE_CHOICE) {
            // Une seule réponse correcte
            if (userAnswers.size() != 1 || correctAnswers.size() != 1) {
                return false;
            }
            return userAnswers.get(0).equals(correctAnswers.get(0));
        } else if (type == Type.MATCHING) {
            // Toutes les paires doivent correspondre
            if (userAnswers.size() != correctAnswers.size()) {
                return false;
            }
            for (int i = 0; i < userAnswers.size(); i++) {
                if (!userAnswers.get(i).equals(correctAnswers.get(i))) {
                    return false;
                }
            }
            return true;
        }
        
        return false;
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

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public List<String> getOptions() {
        return options;
    }

    public void setOptions(List<String> options) {
        this.options = options;
    }

    public List<String> getCorrectAnswers() {
        return correctAnswers;
    }

    public void setCorrectAnswers(List<String> correctAnswers) {
        this.correctAnswers = correctAnswers;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public String getMediaUrl() {
        return mediaUrl;
    }

    public void setMediaUrl(String mediaUrl) {
        this.mediaUrl = mediaUrl;
    }

    public String getMediaType() {
        return mediaType;
    }

    public void setMediaType(String mediaType) {
        this.mediaType = mediaType;
    }

    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        this.points = points;
    }

    public int getTimeLimit() {
        return timeLimit;
    }

    public void setTimeLimit(int timeLimit) {
        this.timeLimit = timeLimit;
    }
} 