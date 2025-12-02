package com.rngym.myapplication;

public class Ball {
    // Position & physics
    public float x, y, r;
    public float vx, vy;

    // Portal cooldown
    public long lastTeleportedAt = 0;
    public long teleportCooldown = 900; // ms

    // Collision cooldown
    public long lastCollisionTimeMs = 0;
    public static final long COLLISION_COOLDOWN_MS = 100;

    // Small ball properties
    public boolean isSmall = false;
    public long spawnTime = 0;
    public static final long SMALL_BALL_LIFETIME = 10000L; // 10 seconds

    /**
     * Constructor for normal balls
     */
    public Ball(float x, float y, float r, float vx, float vy) {
        this.x = x;
        this.y = y;
        this.r = r;
        this.vx = vx;
        this.vy = vy;
        this.isSmall = false;
        this.spawnTime = System.currentTimeMillis();
    }

    /**
     * Constructor with small ball flag
     */
    public Ball(float x, float y, float r, float vx, float vy, boolean isSmall) {
        this(x, y, r, vx, vy);
        this.isSmall = isSmall;
    }

    public float centerX() {
        return x + r * 0.5f;
    }

    public float centerY() {
        return y + r * 0.5f;
    }


    public void setCenter(float cx, float cy) {
        this.x = cx - r * 0.5f;
        this.y = cy - r * 0.5f;
    }

    public boolean isMoving() {
        return Math.abs(vx) > 0.1f || Math.abs(vy) > 0.1f;
    }

    public boolean isExpired() {
        if (!isSmall) return false; // Normal balls never expire
        return System.currentTimeMillis() - spawnTime > SMALL_BALL_LIFETIME;
    }

}