package com.example.quiz.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GameSession {
    private String id;
    private String quizId;
    private long startTime;
    private long endTime;
    private List<String> playerIds;
    private Map<String, Integer> playerScores; // joueurId -> score
    private Map<String, List<Long>> playerResponseTimes; // joueurId -> liste des temps de réponse
    private Map<String, Integer> playerLives; // joueurId -> nombre de vies (pour le mode survie)
    private Map<String, List<Boolean>> playerAnswers; // joueurId -> liste des réponses (correctes ou non)
    private int currentQuestionIndex;
    private Quiz.GameMode gameMode;
    private boolean isActive;
    private String winnerId;
    private Map<String, Object> gameRules; // Règles spécifiques pour la session

    // Constructeur vide pour Firebase
    public GameSession() {
        playerIds = new ArrayList<>();
        playerScores = new HashMap<>();
        playerResponseTimes = new HashMap<>();
        playerLives = new HashMap<>();
        playerAnswers = new HashMap<>();
        gameRules = new HashMap<>();
    }

    public GameSession(String id, String quizId, List<String> playerIds, Quiz.GameMode gameMode) {
        this.id = id;
        this.quizId = quizId;
        this.startTime = System.currentTimeMillis();
        this.playerIds = new ArrayList<>(playerIds);
        this.playerScores = new HashMap<>();
        this.playerResponseTimes = new HashMap<>();
        this.playerLives = new HashMap<>();
        this.playerAnswers = new HashMap<>();
        this.currentQuestionIndex = 0;
        this.gameMode = gameMode;
        this.isActive = true;
        this.gameRules = new HashMap<>();
        
        // Initialisation des scores et des vies pour chaque joueur
        for (String playerId : playerIds) {
            playerScores.put(playerId, 0);
            playerResponseTimes.put(playerId, new ArrayList<>());
            playerLives.put(playerId, gameMode == Quiz.GameMode.SURVIVAL ? 3 : 0);
            playerAnswers.put(playerId, new ArrayList<>());
        }
    }
    
    // Méthode pour mettre à jour le score d'un joueur
    public void updatePlayerScore(String playerId, int additionalPoints) {
        if (playerScores.containsKey(playerId)) {
            int currentScore = playerScores.get(playerId);
            playerScores.put(playerId, currentScore + additionalPoints);
        }
    }
    
    // Méthode pour enregistrer le temps de réponse d'un joueur
    public void recordResponseTime(String playerId, long responseTimeMs) {
        if (playerResponseTimes.containsKey(playerId)) {
            List<Long> times = playerResponseTimes.get(playerId);
            times.add(responseTimeMs);
            playerResponseTimes.put(playerId, times);
        }
    }
    
    // Méthode pour enregistrer une réponse d'un joueur
    public void recordAnswer(String playerId, boolean isCorrect) {
        if (playerAnswers.containsKey(playerId)) {
            List<Boolean> answers = playerAnswers.get(playerId);
            answers.add(isCorrect);
            playerAnswers.put(playerId, answers);
        }
        
        // Si mode survie et réponse incorrecte, décrémenter une vie
        if (gameMode == Quiz.GameMode.SURVIVAL && !isCorrect) {
            if (playerLives.containsKey(playerId)) {
                int lives = playerLives.get(playerId);
                if (lives > 0) {
                    playerLives.put(playerId, lives - 1);
                }
            }
        }
    }
    
    // Méthode pour passer à la question suivante
    public void nextQuestion() {
        currentQuestionIndex++;
    }
    
    // Méthode pour terminer la session
    public void endSession() {
        this.endTime = System.currentTimeMillis();
        this.isActive = false;
        
        // Déterminer le gagnant (celui avec le score le plus élevé)
        String currentWinner = null;
        int highestScore = -1;
        
        for (Map.Entry<String, Integer> entry : playerScores.entrySet()) {
            if (entry.getValue() > highestScore) {
                highestScore = entry.getValue();
                currentWinner = entry.getKey();
            }
        }
        
        this.winnerId = currentWinner;
    }
    
    // Méthode pour vérifier si un joueur est éliminé (mode survie)
    public boolean isPlayerEliminated(String playerId) {
        if (gameMode != Quiz.GameMode.SURVIVAL) {
            return false;
        }
        
        return playerLives.getOrDefault(playerId, 0) <= 0;
    }
    
    // Méthode pour obtenir le temps de jeu en millisecondes
    public long getGameDuration() {
        if (endTime == 0) {
            return System.currentTimeMillis() - startTime;
        } else {
            return endTime - startTime;
        }
    }
    
    // Méthode pour obtenir le temps de réponse moyen d'un joueur
    public double getAverageResponseTime(String playerId) {
        if (!playerResponseTimes.containsKey(playerId) || playerResponseTimes.get(playerId).isEmpty()) {
            return 0;
        }
        
        List<Long> times = playerResponseTimes.get(playerId);
        long sum = 0;
        for (Long time : times) {
            sum += time;
        }
        
        return (double) sum / times.size();
    }
    
    // Méthode pour obtenir le taux de bonnes réponses d'un joueur
    public double getCorrectAnswerRate(String playerId) {
        if (!playerAnswers.containsKey(playerId) || playerAnswers.get(playerId).isEmpty()) {
            return 0;
        }
        
        List<Boolean> answers = playerAnswers.get(playerId);
        int correctCount = 0;
        for (Boolean correct : answers) {
            if (correct) {
                correctCount++;
            }
        }
        
        return (double) correctCount / answers.size();
    }

    // Getters et Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getQuizId() {
        return quizId;
    }

    public void setQuizId(String quizId) {
        this.quizId = quizId;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public List<String> getPlayerIds() {
        return playerIds;
    }

    public void setPlayerIds(List<String> playerIds) {
        this.playerIds = playerIds;
    }

    public Map<String, Integer> getPlayerScores() {
        return playerScores;
    }

    public void setPlayerScores(Map<String, Integer> playerScores) {
        this.playerScores = playerScores;
    }

    public Map<String, List<Long>> getPlayerResponseTimes() {
        return playerResponseTimes;
    }

    public void setPlayerResponseTimes(Map<String, List<Long>> playerResponseTimes) {
        this.playerResponseTimes = playerResponseTimes;
    }

    public Map<String, Integer> getPlayerLives() {
        return playerLives;
    }

    public void setPlayerLives(Map<String, Integer> playerLives) {
        this.playerLives = playerLives;
    }

    public Map<String, List<Boolean>> getPlayerAnswers() {
        return playerAnswers;
    }

    public void setPlayerAnswers(Map<String, List<Boolean>> playerAnswers) {
        this.playerAnswers = playerAnswers;
    }

    public int getCurrentQuestionIndex() {
        return currentQuestionIndex;
    }

    public void setCurrentQuestionIndex(int currentQuestionIndex) {
        this.currentQuestionIndex = currentQuestionIndex;
    }

    public Quiz.GameMode getGameMode() {
        return gameMode;
    }

    public void setGameMode(Quiz.GameMode gameMode) {
        this.gameMode = gameMode;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public String getWinnerId() {
        return winnerId;
    }

    public void setWinnerId(String winnerId) {
        this.winnerId = winnerId;
    }

    public Map<String, Object> getGameRules() {
        return gameRules;
    }

    public void setGameRules(Map<String, Object> gameRules) {
        this.gameRules = gameRules;
    }
    
    // Méthode pour ajouter une règle spécifique
    public void addGameRule(String key, Object value) {
        if (gameRules == null) {
            gameRules = new HashMap<>();
        }
        gameRules.put(key, value);
    }
} 