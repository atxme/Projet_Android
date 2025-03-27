package com.example.quiz.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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
    private String imageUrl; // Base64 ou chemin local
    private String authorId;
    private String authorName;
    private int playCount;
    private double rating;
    private long createdAt;
    private long updatedAt;
    private List<String> questionIds; // IDs des questions dans Firestore
    private List<Question> questions; // Questions chargées
    private boolean isPublic;
    private String category;
    private String difficulty; // "Facile", "Moyen", "Difficile"
    private int timeLimit; // en secondes, 0 = pas de limite
    private GameMode gameMode;
    private int difficultyLevel; // 1-5
    private String customRules; // Règles spécifiques pour le quiz
    
    // Constructeur vide requis pour Firestore
    public Quiz() {
        questionIds = new ArrayList<>();
        questions = new ArrayList<>();
        playCount = 0;
        rating = 0.0;
        createdAt = System.currentTimeMillis();
        updatedAt = System.currentTimeMillis();
        isPublic = true;
    }

    // Constructeur pour créer un quiz avec les informations de base
    public Quiz(String id, String title, String description, String imageUrl, String authorId, String authorName) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.imageUrl = imageUrl;
        this.authorId = authorId;
        this.authorName = authorName;
        this.questionIds = new ArrayList<>();
        this.questions = new ArrayList<>();
        this.playCount = 0;
        this.rating = 0.0;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
        this.isPublic = true;
        this.category = "";
        this.difficulty = "Moyen";
        this.timeLimit = 0;
    }

    // Méthode pour convertir un document Firestore en Quiz
    public static Quiz fromMap(Map<String, Object> map, String documentId) {
        Quiz quiz = new Quiz();
        quiz.id = documentId;
        quiz.title = (String) map.get("title");
        quiz.description = (String) map.get("description");
        quiz.imageUrl = (String) map.get("imageUrl");
        quiz.authorId = (String) map.get("authorId");
        quiz.authorName = (String) map.get("authorName");
        
        // Récupérer la liste des IDs de questions
        List<String> questionIds = (List<String>) map.get("questionIds");
        if (questionIds != null) {
            quiz.questionIds = questionIds;
        }
        
        // Récupérer le nombre de parties
        if (map.get("playCount") instanceof Long) {
            quiz.playCount = ((Long) map.get("playCount")).intValue();
        } else if (map.get("playCount") instanceof Integer) {
            quiz.playCount = (Integer) map.get("playCount");
        }
        
        // Récupérer la note
        if (map.get("rating") instanceof Double) {
            quiz.rating = (Double) map.get("rating");
        }
        
        // Récupérer les dates
        if (map.get("createdAt") instanceof Long) {
            quiz.createdAt = (Long) map.get("createdAt");
        } else if (map.get("createdAt") instanceof java.util.Date) {
            quiz.createdAt = ((java.util.Date) map.get("createdAt")).getTime();
        }
        
        if (map.get("updatedAt") instanceof Long) {
            quiz.updatedAt = (Long) map.get("updatedAt");
        } else if (map.get("updatedAt") instanceof java.util.Date) {
            quiz.updatedAt = ((java.util.Date) map.get("updatedAt")).getTime();
        }
        
        // Récupérer si le quiz est public
        if (map.get("isPublic") instanceof Boolean) {
            quiz.isPublic = (Boolean) map.get("isPublic");
        }
        
        quiz.category = (String) map.get("category");
        quiz.difficulty = (String) map.get("difficulty");
        
        // Récupérer le temps limite
        if (map.get("timeLimit") instanceof Long) {
            quiz.timeLimit = ((Long) map.get("timeLimit")).intValue();
        } else if (map.get("timeLimit") instanceof Integer) {
            quiz.timeLimit = (Integer) map.get("timeLimit");
        }
        
        return quiz;
    }

    // Méthode pour convertir Quiz en Map pour Firestore
    public Map<String, Object> toMap() {
        Map<String, Object> map = new java.util.HashMap<>();
        map.put("title", title);
        map.put("description", description);
        map.put("imageUrl", imageUrl);
        map.put("authorId", authorId);
        map.put("authorName", authorName);
        map.put("questionIds", questionIds);
        map.put("playCount", playCount);
        map.put("rating", rating);
        map.put("createdAt", createdAt);
        map.put("updatedAt", updatedAt);
        map.put("isPublic", isPublic);
        map.put("category", category);
        map.put("difficulty", difficulty);
        map.put("timeLimit", timeLimit);
        return map;
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
    
    // Ajouter une question au quiz
    public void addQuestion(Question question) {
        if (question.getId() != null && !question.getId().isEmpty()) {
            if (!questionIds.contains(question.getId())) {
                questionIds.add(question.getId());
            }
            
            // Vérifier si la question existe déjà dans la liste
            boolean exists = false;
            for (Question q : questions) {
                if (q.getId().equals(question.getId())) {
                    exists = true;
                    break;
                }
            }
            
            if (!exists) {
                questions.add(question);
            }
        }
    }
    
    // Supprimer une question du quiz
    public void removeQuestion(String questionId) {
        questionIds.remove(questionId);
        
        // Supprimer également la question de la liste des questions chargées
        List<Question> toRemove = new ArrayList<>();
        for (Question q : questions) {
            if (q.getId().equals(questionId)) {
                toRemove.add(q);
            }
        }
        
        questions.removeAll(toRemove);
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

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getAuthorId() {
        return authorId;
    }

    public void setAuthorId(String authorId) {
        this.authorId = authorId;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public List<String> getQuestionIds() {
        return questionIds;
    }

    public void setQuestionIds(List<String> questionIds) {
        this.questionIds = questionIds;
    }

    public List<Question> getQuestions() {
        return questions;
    }

    public void setQuestions(List<Question> questions) {
        this.questions = questions;
    }

    public int getPlayCount() {
        return playCount;
    }

    public void setPlayCount(int playCount) {
        this.playCount = playCount;
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public void setPublic(boolean isPublic) {
        this.isPublic = isPublic;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }

    public int getTimeLimit() {
        return timeLimit;
    }

    public void setTimeLimit(int timeLimit) {
        this.timeLimit = timeLimit;
    }

    public GameMode getGameMode() {
        return gameMode;
    }

    public void setGameMode(GameMode gameMode) {
        this.gameMode = gameMode;
    }

    public int getDifficultyLevel() {
        return difficultyLevel;
    }

    public void setDifficultyLevel(int difficultyLevel) {
        this.difficultyLevel = difficultyLevel;
    }

    public String getCustomRules() {
        return customRules;
    }

    public void setCustomRules(String customRules) {
        this.customRules = customRules;
    }
} 