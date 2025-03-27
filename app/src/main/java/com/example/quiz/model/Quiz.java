package com.example.quiz.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Quiz {
    public enum GameMode {
        STANDARD,         // Quiz standard avec points fixes
        TIMED,            // Contre la montre
        FASTEST,          // Le plus rapide gagne
        TOP_THREE,        // Les 3 meilleurs sur 4 gagnent des points
        REGRESSIVE,       // Points décroissants selon l'ordre de réponse
        SHARED,           // Le gagnant partage avec un autre joueur
        DOUBLE_OR_NOTHING, // Quitte ou double
        SURVIVAL,         // Mode survie avec 3 vies
        RANDOM_CHALLENGE, // Défi aléatoire
        MARATHON,         // Série de questions consécutives
        TRAP,             // Une réponse fait perdre des points
        MEMORY            // Question visible pendant un temps limité
    }

    private String id;
    private String title;
    private String description;
    private String category;
    private String subCategory;
    private String creatorId;
    private List<Question> questions;
    private GameMode gameMode;
    private int difficulty; // 1-5
    private long creationDate;
    private long lastModifiedDate;
    private boolean isPublic;
    private int playCount;
    private double averageRating;
    private String customRules; // Règles spécifiques pour le quiz
    private int timeLimit; // Temps total en secondes, 0 = pas de limite
    
    // Constructeur vide pour Firebase
    public Quiz() {
        questions = new ArrayList<>();
    }

    public Quiz(String id, String title, String description, String category, 
               String subCategory, String creatorId, GameMode gameMode, int difficulty) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.category = category;
        this.subCategory = subCategory;
        this.creatorId = creatorId;
        this.questions = new ArrayList<>();
        this.gameMode = gameMode;
        this.difficulty = difficulty;
        this.creationDate = System.currentTimeMillis();
        this.lastModifiedDate = this.creationDate;
        this.isPublic = true;
        this.playCount = 0;
        this.averageRating = 0.0;
        this.timeLimit = 0;
    }
    
    // Méthode pour mélanger les questions
    public void shuffleQuestions() {
        if (questions != null) {
            Collections.shuffle(questions);
        }
    }
    
    // Méthode pour mélanger les options de toutes les questions
    public void shuffleAllOptions() {
        if (questions != null) {
            for (Question question : questions) {
                question.shuffleOptions();
            }
        }
    }
    
    // Getters et Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getSubCategory() {
        return subCategory;
    }

    public void setSubCategory(String subCategory) {
        this.subCategory = subCategory;
    }

    public String getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(String creatorId) {
        this.creatorId = creatorId;
    }

    public List<Question> getQuestions() {
        return questions;
    }

    public void setQuestions(List<Question> questions) {
        this.questions = questions;
    }
    
    public void addQuestion(Question question) {
        if (this.questions == null) {
            this.questions = new ArrayList<>();
        }
        this.questions.add(question);
    }
    
    public void removeQuestion(String questionId) {
        if (questions != null) {
            questions.removeIf(q -> q.getId().equals(questionId));
        }
    }

    public GameMode getGameMode() {
        return gameMode;
    }

    public void setGameMode(GameMode gameMode) {
        this.gameMode = gameMode;
    }

    public int getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(int difficulty) {
        this.difficulty = difficulty;
    }

    public long getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(long creationDate) {
        this.creationDate = creationDate;
    }

    public long getLastModifiedDate() {
        return lastModifiedDate;
    }

    public void setLastModifiedDate(long lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public void setPublic(boolean isPublic) {
        this.isPublic = isPublic;
    }

    public int getPlayCount() {
        return playCount;
    }

    public void setPlayCount(int playCount) {
        this.playCount = playCount;
    }
    
    public void incrementPlayCount() {
        this.playCount++;
    }

    public double getAverageRating() {
        return averageRating;
    }

    public void setAverageRating(double averageRating) {
        this.averageRating = averageRating;
    }

    public String getCustomRules() {
        return customRules;
    }

    public void setCustomRules(String customRules) {
        this.customRules = customRules;
    }

    public int getTimeLimit() {
        return timeLimit;
    }

    public void setTimeLimit(int timeLimit) {
        this.timeLimit = timeLimit;
    }
} 