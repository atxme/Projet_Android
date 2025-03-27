package com.example.quiz.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Player {
    private String id;
    private String username;
    private String email;
    private String photoUrl;
    private long creationDate;
    private long lastLoginDate;
    private int totalQuizPlayed;
    private int totalQuizWon;
    private int totalPoints;
    private int level;
    private List<String> badges;
    private Map<String, Integer> categoryScores; // Scores par catégorie
    private List<String> favoriteQuizzes;
    private List<String> completedQuizzes;
    private Map<String, Object> settings; // Préférences utilisateur

    // Constructeur vide pour Firebase
    public Player() {
        badges = new ArrayList<>();
        categoryScores = new HashMap<>();
        favoriteQuizzes = new ArrayList<>();
        completedQuizzes = new ArrayList<>();
        settings = new HashMap<>();
    }

    public Player(String id, String username, String email) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.creationDate = System.currentTimeMillis();
        this.lastLoginDate = this.creationDate;
        this.totalQuizPlayed = 0;
        this.totalQuizWon = 0;
        this.totalPoints = 0;
        this.level = 1;
        this.badges = new ArrayList<>();
        this.categoryScores = new HashMap<>();
        this.favoriteQuizzes = new ArrayList<>();
        this.completedQuizzes = new ArrayList<>();
        this.settings = new HashMap<>();
        
        // Paramètres par défaut
        this.settings.put("notifications", true);
        this.settings.put("sound", true);
        this.settings.put("vibration", true);
        this.settings.put("darkMode", false);
    }
    
    // Méthode pour ajouter des points
    public void addPoints(int points, String category) {
        this.totalPoints += points;
        
        // Mise à jour des scores par catégorie
        if (category != null && !category.isEmpty()) {
            int currentCategoryScore = categoryScores.getOrDefault(category, 0);
            categoryScores.put(category, currentCategoryScore + points);
        }
        
        // Mise à jour du niveau (1 niveau tous les 1000 points)
        this.level = (this.totalPoints / 1000) + 1;
    }
    
    // Méthode pour ajouter un badge
    public void addBadge(String badge) {
        if (!badges.contains(badge)) {
            badges.add(badge);
        }
    }
    
    // Méthode pour ajouter un quiz aux favoris
    public void addFavoriteQuiz(String quizId) {
        if (!favoriteQuizzes.contains(quizId)) {
            favoriteQuizzes.add(quizId);
        }
    }
    
    // Méthode pour retirer un quiz des favoris
    public void removeFavoriteQuiz(String quizId) {
        favoriteQuizzes.remove(quizId);
    }
    
    // Méthode pour marquer un quiz comme complété
    public void addCompletedQuiz(String quizId) {
        if (!completedQuizzes.contains(quizId)) {
            completedQuizzes.add(quizId);
        }
    }
    
    // Méthode pour enregistrer une victoire
    public void addWin() {
        this.totalQuizWon++;
    }
    
    // Méthode pour enregistrer une partie jouée
    public void addQuizPlayed() {
        this.totalQuizPlayed++;
    }

    // Getters et Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public long getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(long creationDate) {
        this.creationDate = creationDate;
    }

    public long getLastLoginDate() {
        return lastLoginDate;
    }

    public void setLastLoginDate(long lastLoginDate) {
        this.lastLoginDate = lastLoginDate;
    }

    public int getTotalQuizPlayed() {
        return totalQuizPlayed;
    }

    public void setTotalQuizPlayed(int totalQuizPlayed) {
        this.totalQuizPlayed = totalQuizPlayed;
    }

    public int getTotalQuizWon() {
        return totalQuizWon;
    }

    public void setTotalQuizWon(int totalQuizWon) {
        this.totalQuizWon = totalQuizWon;
    }

    public int getTotalPoints() {
        return totalPoints;
    }

    public void setTotalPoints(int totalPoints) {
        this.totalPoints = totalPoints;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public List<String> getBadges() {
        return badges;
    }

    public void setBadges(List<String> badges) {
        this.badges = badges;
    }

    public Map<String, Integer> getCategoryScores() {
        return categoryScores;
    }

    public void setCategoryScores(Map<String, Integer> categoryScores) {
        this.categoryScores = categoryScores;
    }

    public List<String> getFavoriteQuizzes() {
        return favoriteQuizzes;
    }

    public void setFavoriteQuizzes(List<String> favoriteQuizzes) {
        this.favoriteQuizzes = favoriteQuizzes;
    }

    public List<String> getCompletedQuizzes() {
        return completedQuizzes;
    }

    public void setCompletedQuizzes(List<String> completedQuizzes) {
        this.completedQuizzes = completedQuizzes;
    }

    public Map<String, Object> getSettings() {
        return settings;
    }

    public void setSettings(Map<String, Object> settings) {
        this.settings = settings;
    }
    
    // Méthode pour mettre à jour un paramètre spécifique
    public void updateSetting(String key, Object value) {
        if (settings == null) {
            settings = new HashMap<>();
        }
        settings.put(key, value);
    }
} 