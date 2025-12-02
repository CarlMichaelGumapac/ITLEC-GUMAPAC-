package com.rngym.myapplication;

/**
 * GameConfig - Centralized configuration for WallPAWng game
 * All magic numbers collected here for easy balancing
 */
public class GameConfig {


    public static final long EARLY_GAME_GRACE_PERIOD_MS = 8000L; // 8 seconds

    // ==================== CAT CONFIGURATION ====================
    public static final float CAT_WIDTH_FRACTION = 0.30f;  // 30% of screen width
    public static final float CAT_MAX_WIDTH_FRACTION = 0.5f;  // Maximum 50% of screen
    public static final float CAT_HEIGHT_PX = 96f;
    public static final float CAT_SPEED = 20f;  // Pixels per frame

    // ==================== BALL CONFIGURATION ====================
    public static final float BALL_SIZE_PERCENT = 0.07f;  // 7% of screen width
    public static final float STARTING_VY = 20f;  // Initial vertical velocity
    public static final float MIN_VX = 10f;  // Minimum horizontal velocity
    public static final float MAX_VX = 20f;  // Maximum horizontal velocity

    // ==================== BOX SPAWNER CONFIGURATION ====================
    public static final long BOX_SPAWN_COOLDOWN = 8000L;  // 8 seconds between spawns
    public static final int MAX_BOXES = 5;  // Maximum boxes on screen
    public static final float BOX_MIN_WIDTH = 60f;
    public static final float BOX_MAX_WIDTH = 140f;
    public static final float BOX_HEIGHT = 100f;
    public static final int BOX_MIN_HP = 1;
    public static final int BOX_MAX_HP = 4;

    // Box reward scaling
    public static final int BOX_XP_PER_HP = 2;  // XP = HP × 2
    public static final int BOX_SCORE_PER_HP = 5;  // Score = HP × 5

    // ==================== BUMPER CONFIGURATION ====================
    public static final int MAX_BUMPERS = 3;
    public static final float BUMPER_MIN_WIDTH_FRAC = 0.10f;  // 10% of screen
    public static final float BUMPER_MAX_WIDTH_FRAC = 0.30f;  // 30% of screen
    public static final float BUMPER_HEIGHT = 20f;
    public static final long BUMPER_MIN_LIFE = 8000L;  // 8 seconds
    public static final long BUMPER_MAX_LIFE = 22000L;  // 22 seconds

    // ==================== PORTAL CONFIGURATION ====================
    public static final long PORTAL_MIN_DURATION = 6000L;  // 6 seconds
    public static final long PORTAL_MAX_DURATION = 11000L;  // 11 seconds
    public static final long PORTAL_COOLDOWN = 7000L;  // 7 seconds between portal pairs
    public static final float PORTAL_WIDTH = 120f;
    public static final float PORTAL_HEIGHT = 140f;

    // ==================== BLACK HOLE CONFIGURATION ====================
    public static final long BLACKHOLE_MIN_DURATION = 5000L;  // 5 seconds
    public static final long BLACKHOLE_MAX_DURATION = 9000L;  // 9 seconds
    public static final long BLACKHOLE_COOLDOWN = 9000L;  // 9 seconds cooldown
    public static final float BLACKHOLE_PULL_STRENGTH = 0.16f;
    public static final float BLACKHOLE_MIN_RADIUS = 28f;
    public static final float BLACKHOLE_MAX_RADIUS = 64f;
    public static final float BLACKHOLE_ROTATION_SPEED = 0.1f;  // Rotation speed for visual effect
    public static final float UPGRADE_BLACKHOLE_PULL_MULTIPLIER = 1.5f;  // Upgrade multiplier for black hole pull

    // ==================== PHYSICS CONFIGURATION ====================
    public static final float GRAVITY = 0f;  // No gravity
    public static final float BOUNCE_DAMPING = 0.98f;  // Energy loss on bounce
    public static final float WALL_BOUNCE_DAMPING = 0.98f;
    public static final float MAX_SPEED = 120f;  // Maximum velocity magnitude
    public static final float SUBSTEP_DISTANCE = 18f;  // Physics sub-step size

    public static final float MIN_SPEED_AFTER_COLLISION = 16.0f; // Minimum speed after any collision

    // ==================== STRESS CONFIGURATION ====================
    public static final float STRESS_ON_MISS = 20f;  // Stress added when ball is missed
    public static final float STRESS_ON_CATCH = -5f;  // Stress reduced on catch
    public static final float STRESS_DECAY_RATE = 0.01f;  // Passive decay per frame
    public static final float INITIAL_MAX_STRESS = 100f;
    public static final float ABSOLUTE_MAX_STRESS = 200f;  // Hard cap

    // ==================== XP & LEVELING CONFIGURATION ====================
    public static final int BASE_XP_PER_CATCH = 2;
    public static final float XP_SCALING_FACTOR = 1.15f;  // XP needed grows by 15% per level
    public static final int BASE_XP_FOR_LEVEL_2 = 10;

    // ==================== COMBO SYSTEM ====================
    public static final long COMBO_TIMEOUT_MS = 8000; // 8 seconds to maintain combo
    public static final float COMBO_XP_MULTIPLIER = 0.25f; // Bonus XP per combo stack
    public static final float COMBO_REWARD_MULTIPLIER = 0.1f; // XP awarded per combo count on break
    public static final float MAX_COMBO_MULTIPLIER = 2.0f; // Maximum combo multiplier
    public static final int COMBO_POPUP_DURATION = 1000; // 1 second

    public static int xpForLevel(int level) {
        double xp = BASE_XP_FOR_LEVEL_2 * level * Math.pow(XP_SCALING_FACTOR, Math.max(0, level - 1));
        return Math.max(1, (int)Math.round(xp));
    }

    // ==================== SCORE CONFIGURATION ====================
    public static final int SCORE_PER_CATCH = 1;
    public static final int SCORE_PENALTY_ON_MISS = -1;

    // ==================== UPGRADE MULTIPLIERS ====================
    public static final float UPGRADE_CAT_WIDTH_INCREASE = 1.12f;  // 12% increase
    public static final float UPGRADE_SPEED_INCREASE = 1.10f;  // 10% increase
    public static final int UPGRADE_MAX_STRESS_INCREASE = 20;
    public static final float UPGRADE_BOX_REWARD_MULTIPLIER = 1.5f;  // 50% bonus
    public static final long UPGRADE_PORTAL_DURATION_INCREASE = 2500L;  // +2.5 seconds

    // ==================== UI CONFIGURATION ====================
    public static final float POPUP_TEXT_SIZE = 36f;
    public static final long POPUP_LIFETIME = 1000L;  // 1.0 seconds
    public static final float POPUP_RISE_SPEED = -40f;  // Negative = upward
    public static final float POPUP_SCALE_MAX = 1.25f;  // Scale up to 125%

    public static final float HUD_TEXT_SIZE = 18f;
    public static final float HUD_SMALL_TEXT_SIZE = 16f;

    // ==================== TIMING CONFIGURATION ====================
    public static final int TARGET_FPS = 60;
    public static final long FRAME_TIME_MS = 16;  // ~60 FPS

    // ==================== DIFFICULTY SCALING ====================

    public static long getAdjustedBoxSpawnCooldown(int level) {
        long adjusted = BOX_SPAWN_COOLDOWN - (level * 200L);
        return Math.max(2000L, adjusted);  // Minimum 2 seconds
    }

    public static int getMaxBoxesForLevel(int level) {
        return Math.min(MAX_BOXES + (level / 5), 8);  // Cap at 8 boxes
    }

    public static int getBoxHPForLevel(int level, int baseHP) {
        return baseHP + (level / 3);
    }
}