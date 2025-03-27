package com.example.quiz.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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
    private int correctOption;
    private int difficulty; // 1-5
    private String category;
    private long createdAt;
    private String createdBy;

    // Constructeur vide pour Firebase
    public Question() {
        options = new ArrayList<>();
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
    
    // Constructeur pour les données de démonstration
    public Question(String id, String text, String mediaUrl, String mediaType, 
                   List<String> options, int correctOption, String explanation,
                   int difficulty, String category, long createdAt, String createdBy) {
        this.id = id;
        this.text = text;
        this.mediaUrl = mediaUrl;
        this.mediaType = mediaType;
        this.options = options;
        this.correctOption = correctOption;
        this.explanation = explanation;
        this.difficulty = difficulty;
        this.category = category;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
        this.type = Type.SINGLE_CHOICE;  // Par défaut pour la démo
        this.correctAnswers = new ArrayList<>();
        if (options != null && correctOption >= 0 && correctOption < options.size()) {
            this.correctAnswers.add(options.get(correctOption));
        }
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

    // Méthode pour convertir un document Firestore en Question
    public static Question fromMap(Map<String, Object> map, String documentId) {
        Question question = new Question();
        question.id = documentId;
        question.text = (String) map.get("text");
        question.mediaUrl = (String) map.get("mediaUrl");
        question.mediaType = (String) map.get("mediaType");
        
        // Récupérer les options
        List<String> options = (List<String>) map.get("options");
        if (options != null) {
            question.options = options;
        }
        
        // Récupérer l'option correcte
        if (map.get("correctOption") instanceof Long) {
            question.correctOption = ((Long) map.get("correctOption")).intValue();
        } else if (map.get("correctOption") instanceof Integer) {
            question.correctOption = (Integer) map.get("correctOption");
        }
        
        question.explanation = (String) map.get("explanation");
        
        // Récupérer la difficulté
        if (map.get("difficulty") instanceof Long) {
            question.difficulty = ((Long) map.get("difficulty")).intValue();
        } else if (map.get("difficulty") instanceof Integer) {
            question.difficulty = (Integer) map.get("difficulty");
        }
        
        question.category = (String) map.get("category");
        
        // Récupérer la date de création
        if (map.get("createdAt") instanceof Long) {
            question.createdAt = (Long) map.get("createdAt");
        } else if (map.get("createdAt") instanceof java.util.Date) {
            question.createdAt = ((java.util.Date) map.get("createdAt")).getTime();
        }
        
        question.createdBy = (String) map.get("createdBy");
        
        return question;
    }

    // Méthode pour convertir Question en Map pour Firestore
    public Map<String, Object> toMap() {
        Map<String, Object> map = new java.util.HashMap<>();
        map.put("text", text);
        map.put("mediaUrl", mediaUrl);
        map.put("mediaType", mediaType);
        map.put("options", options);
        map.put("correctOption", correctOption);
        map.put("explanation", explanation);
        map.put("difficulty", difficulty);
        map.put("category", category);
        map.put("createdAt", createdAt);
        map.put("createdBy", createdBy);
        return map;
    }

    // Créer des données de démonstration
    public static List<Question> getDemoQuestions() {
        List<Question> questions = new ArrayList<>();
        
        // Question 1
        List<String> options1 = new ArrayList<>();
        options1.add("1969");
        options1.add("1971");
        options1.add("1975");
        options1.add("1965");
        questions.add(new Question(
            "q1",
            "En quelle année a été créé le premier microprocesseur Intel 4004?",
            null,
            null,
            options1,
            1,
            "Le premier microprocesseur Intel 4004 a été lancé en novembre 1971.",
            2,
            "Technologie",
            System.currentTimeMillis(),
            "system"
        ));
        
        // Question 2
        List<String> options2 = new ArrayList<>();
        options2.add("Mer de la Tranquillité");
        options2.add("Mer de la Sérénité");
        options2.add("Océan des Tempêtes");
        options2.add("Mer des Pluies");
        questions.add(new Question(
            "q2",
            "Sur quelle mer lunaire s'est posé Apollo 11?",
            null,
            null,
            options2,
            0,
            "Apollo 11 s'est posé sur la Mer de la Tranquillité (Mare Tranquillitatis) le 20 juillet 1969.",
            3,
            "Espace",
            System.currentTimeMillis(),
            "system"
        ));
        
        // Question 3
        List<String> options3 = new ArrayList<>();
        options3.add("Alan Turing");
        options3.add("John von Neumann");
        options3.add("Ada Lovelace");
        options3.add("Grace Hopper");
        questions.add(new Question(
            "q3",
            "Qui est considéré comme le père de l'informatique moderne?",
            null,
            null,
            options3,
            0,
            "Alan Turing est largement considéré comme le père de l'informatique moderne pour ses travaux sur la machine de Turing et le test de Turing.",
            2,
            "Informatique",
            System.currentTimeMillis(),
            "system"
        ));
        
        return questions;
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

    public int getCorrectOption() {
        return correctOption;
    }

    public void setCorrectOption(int correctOption) {
        this.correctOption = correctOption;
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

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
} 