package com.example.quiz.util;

import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.widget.RadioButton;

import com.example.quiz.model.Quiz;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Gestionnaire des modes de jeu pour le quiz.
 */
public class GameModeManager {

    public interface GameModeListener {
        void onTimeUpdated(int secondsRemaining);
        void onTimeUp();
        void onAnswerShuffled();
        void setAnswerEnabled(boolean enabled);
    }

    private final GameModeListener listener;
    private CountDownTimer countDownTimer;
    private Handler shuffleHandler;
    private Runnable shuffleRunnable;
    private boolean isTimerRunning = false;
    private Quiz.GameMode currentGameMode;

    public GameModeManager(GameModeListener listener) {
        this.listener = listener;
        this.shuffleHandler = new Handler(Looper.getMainLooper());
    }

    /**
     * Initialise le mode de jeu spécifié.
     */
    public void initializeGameMode(Quiz.GameMode gameMode, int timeLimit) {
        this.currentGameMode = gameMode;

        // Arrêter toute activité en cours
        stopTimer();
        stopShuffling();

        switch (gameMode) {
            case TIMED:
                // Mode contre la montre
                startTimer(timeLimit);
                break;
            case SHUFFLE_OPTIONS:
                // Mode où les réponses changent d'ordre
                startShuffling();
                break;
            case STANDARD:
            default:
                // Mode standard, aucune action spéciale
                break;
        }
    }

    /**
     * Démarre un timer pour le mode contre la montre.
     */
    private void startTimer(int seconds) {
        if (seconds <= 0) seconds = 60; // Valeur par défaut
        isTimerRunning = true;

        countDownTimer = new CountDownTimer(seconds * 1000L, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                int secondsRemaining = (int) (millisUntilFinished / 1000);
                listener.onTimeUpdated(secondsRemaining);
            }

            @Override
            public void onFinish() {
                isTimerRunning = false;
                listener.onTimeUp();
            }
        }.start();
    }

    /**
     * Arrête le timer en cours.
     */
    public void stopTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
            isTimerRunning = false;
        }
    }

    /**
     * Démarre le mode de mélange des options.
     */
    private void startShuffling() {
        shuffleRunnable = new Runnable() {
            @Override
            public void run() {
                listener.onAnswerShuffled();
                shuffleHandler.postDelayed(this, 2000); // Toutes les 2 secondes
            }
        };
        shuffleHandler.postDelayed(shuffleRunnable, 2000);
    }

    /**
     * Arrête le mélange des options.
     */
    public void stopShuffling() {
        if (shuffleHandler != null && shuffleRunnable != null) {
            shuffleHandler.removeCallbacks(shuffleRunnable);
        }
    }

    /**
     * Pause tous les timers et effets actifs.
     */
    public void pauseGameMode() {
        if (isTimerRunning) {
            stopTimer();
        }
        stopShuffling();
    }

    /**
     * Reprend les timers et effets actifs.
     */
    public void resumeGameMode(int remainingSeconds) {
        if (currentGameMode == Quiz.GameMode.TIMED) {
            startTimer(remainingSeconds);
        } else if (currentGameMode == Quiz.GameMode.SHUFFLE_OPTIONS) {
            startShuffling();
        }
    }

    /**
     * Libère les ressources utilisées par le GameModeManager.
     */
    public void cleanup() {
        stopTimer();
        stopShuffling();
    }

    /**
     * Vérifie si le timer est en cours d'exécution.
     */
    public boolean isTimerRunning() {
        return isTimerRunning;
    }

    /**
     * Retourne le mode de jeu courant.
     */
    public Quiz.GameMode getCurrentGameMode() {
        return currentGameMode;
    }
} 