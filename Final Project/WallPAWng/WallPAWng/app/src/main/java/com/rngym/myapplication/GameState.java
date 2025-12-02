package com.rngym.myapplication;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

public class GameState {
    private static GameState instance;

    // === CORE GAME STATE ===
    private int score = 0;
    private int highScore = 0;
    private float stress = 0f;
    private float maxStress = 100f;
    private boolean paused = false;
    private int level = 1;
    private int xp = 0;

    // === COMBO SYSTEM ===
    private int combo = 0;
    private int maxCombo = 0;
    private long lastCatchTime = 0L;

    // === RUNTIME FLAGS ===
    private boolean ballMoving = false;
    private long gameStartTime = 0L;

    // === PORTAL / BLACKHOLE TIMERS ===
    private long portalCooldown = 60000L;
    private long nextPortalTime = 0L;
    private long blackHoleCooldown = 30000L;

    // === PERSISTENCE ===
    private SharedPreferences prefs;
    private static final String PREFS = "game_prefs";
    private static final String KEY_HS = "highscore";
    private static final String KEY_MAX_COMBO = "max_combo_ever";
    private static final String KEY_UPGRADES = "upgrades";

    // === UPGRADES ===
    private final Set<String> upgrades = new HashSet<>();

    private GameState() {}

    public static synchronized GameState get() {
        if (instance == null) instance = new GameState();
        return instance;
    }

    // === LOAD SAVE DATA ===
    public synchronized void init(Context ctx) {
        if (prefs == null) {
            prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            highScore = prefs.getInt(KEY_HS, 0);
            maxCombo = prefs.getInt(KEY_MAX_COMBO, 0);

            Set<String> savedUpgrades = prefs.getStringSet(KEY_UPGRADES, new HashSet<>());
            upgrades.addAll(savedUpgrades);
        }
    }

    // ==================== SCORE ====================

    public synchronized void addScore(int v) {
        score += v;
        if (score < 0) {
            score = 0;
        }
    }

    public synchronized int getScore() {
        return score;
    }

    public synchronized void resetScore() {
        score = 0;
    }

    // ==================== HIGHSCORE ====================

    public synchronized int getHighScore() {
        return highScore;
    }

    public synchronized void maybeUpdateHighScore() {
        if (score > highScore) {
            highScore = score;
            if (prefs != null) {
                prefs.edit().putInt(KEY_HS, highScore).apply();
            }
        }
    }

    // ==================== STRESS ====================

    public synchronized void addStress(float s) {
        stress += s;
        clampStress(0f, maxStress);
    }

    public synchronized void setStress(float s) {
        stress = s;
        clampStress(0f, maxStress);
    }

    public synchronized float getStress() {
        return stress;
    }

    public synchronized float getMaxStress() {
        return maxStress;
    }

    public synchronized void addMaxStress(float amount) {
        maxStress += amount;
        if (maxStress > 200f) maxStress = 200f;
    }

    private void clampStress(float min, float max) {
        if (stress < min) stress = min;
        if (stress > max) stress = max;
    }

    // ==================== PAUSE ====================

    public synchronized void setPaused(boolean p) {
        paused = p;
    }

    public synchronized boolean isPaused() {
        return paused;
    }

    // ==================== XP / LEVEL ====================

    public synchronized void addXP(int v) {
        xp += v;
    }

    public synchronized int getXP() {
        return xp;
    }

    public synchronized int getLevel() {
        return level;
    }

    public synchronized void levelUp() {
        level++;
        xp = 0;
    }

    // ==================== COMBO SYSTEM ====================


    public synchronized void registerCatch() {
        long now = System.currentTimeMillis();


        if (lastCatchTime > 0 && (now - lastCatchTime) > GameConfig.COMBO_TIMEOUT_MS) {
            combo = 0; // Reset if timeout
        }

        // Increment combo
        combo++;
        lastCatchTime = now;

        // Update max combo
        if (combo > maxCombo) {
            maxCombo = combo;

            // Save all-time max
            if (prefs != null && combo > prefs.getInt(KEY_MAX_COMBO, 0)) {
                prefs.edit().putInt(KEY_MAX_COMBO, combo).apply();
            }
        }
    }

    public synchronized void registerMiss() {
        combo = 0;
        lastCatchTime = 0L;
    }


    public synchronized int getCombo() {
        return combo;
    }

    public synchronized int getMaxCombo() {
        return maxCombo;
    }

    public synchronized float getComboMultiplier() {
        if (combo >= 20) return 3.0f;
        if (combo >= 10) return 2.0f;
        if (combo >= 5) return 1.5f;
        return 1.0f;
    }

    public synchronized long getTimeSinceLastCatch() {
        if (lastCatchTime == 0) return Long.MAX_VALUE;
        return System.currentTimeMillis() - lastCatchTime;
    }

    public synchronized boolean isComboExpiring() {
        if (combo == 0) return false;
        long timeSince = getTimeSinceLastCatch();
        return timeSince > (GameConfig.COMBO_TIMEOUT_MS * 0.7f);
    }

    // ==================== UPGRADES ====================

    public synchronized void unlockUpgrade(String name) {
        if (name == null || name.isEmpty()) return;
        upgrades.add(name);

        if (prefs != null) {
            prefs.edit().putStringSet(KEY_UPGRADES, new HashSet<>(upgrades)).apply();
        }
    }

    public synchronized boolean hasUpgrade(String name) {
        return upgrades.contains(name);
    }

    public synchronized void removeUpgrade(String name) {
        upgrades.remove(name);
        if (prefs != null) {
            prefs.edit().putStringSet(KEY_UPGRADES, new HashSet<>(upgrades)).apply();
        }
    }

    public synchronized void clearUpgrades() {
        upgrades.clear();
        if (prefs != null) {
            prefs.edit().putStringSet(KEY_UPGRADES, new HashSet<>()).apply();
        }
    }

    // ==================== RUNTIME FLAGS ====================

    public synchronized boolean isBallMoving() {
        return ballMoving;
    }

    public synchronized void setBallMoving(boolean moving) {
        ballMoving = moving;
    }

    // ==================== TIMERS ====================

    public synchronized long getPortalCooldown() {
        return portalCooldown;
    }

    public synchronized void setPortalCooldown(long cooldown) {
        portalCooldown = cooldown;
    }

    public synchronized long getNextPortalTime() {
        return nextPortalTime;
    }

    public synchronized void setNextPortalTime(long time) {
        nextPortalTime = time;
    }

    public synchronized long getBlackHoleCooldown() {
        return blackHoleCooldown;
    }

    public synchronized void setBlackHoleCooldown(long cooldown) {
        blackHoleCooldown = cooldown;
    }

    // ==================== RESET ====================

    public synchronized void resetRun() {
        score = 0;
        stress = 0f;
        level = 1;
        xp = 0;
        ballMoving = false;
        combo = 0;
        maxCombo = 0;
        lastCatchTime = 0L;

        gameStartTime = System.currentTimeMillis();
    }

    public synchronized long getGameStartTime() {
        return gameStartTime;
    }

    public synchronized void resetAll() {
        resetRun();
        highScore = 0;
        maxStress = 100f;
        clearUpgrades();
        if (prefs != null) {
            prefs.edit().clear().apply();
        }
    }
}