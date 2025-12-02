package com.rngym.myapplication;

import android.graphics.RectF;
import java.util.Random;

public class BlackHoleSystem {

    public static class BlackHole {
        public float x, y, r;
        public long spawnMs;
        public long durationMs;
        public float rotation = 0f;  // Current rotation angle
        public float pullStrength;  // Dynamic pull strength based on distance

        public BlackHole(float x, float y, float r, long s, long d, float pullStrength){
            this.x=x; this.y=y; this.r=r; spawnMs=s; durationMs=d; this.pullStrength=pullStrength;
        }

        public RectF rect() {
            return new RectF(x-r, y-r, x+r, y+r);
        }
    }

    private BlackHole hole = null;
    private final Random rnd = new Random();

    // Configurable values
    public long minDuration = 5000L;
    public long maxDuration = 9000L;
    public long cooldownMs = 9000L;
    private long lastGone = 0L;

    // Pull strength parameters
    public float basePullStrength = 0.32f;
    public float maxPullStrength = 0.16f;
    public float minPullDistance = 100f;  // Minimum distance for pull effect
    public float maxPullDistance = 300f;  // Maximum distance for pull effect
    public float corePullRadius = 0.8f;   // Radius of the core (no pull inside)
    public float edgePullFactor = 2.0f;   // Extra pull at the edge of the black hole

    public BlackHole get() {
        return hole;
    }

    public void update(long now, int screenW, int screenH) {

        if (now - GameState.get().getGameStartTime() < GameConfig.EARLY_GAME_GRACE_PERIOD_MS) {
            return;
        }

        if (hole != null) {
            // Update rotation for visual effect
            hole.rotation += GameConfig.BLACKHOLE_ROTATION_SPEED;

            // Check if black hole should expire
            if (now - hole.spawnMs >= hole.durationMs) {
                hole = null;
                lastGone = now;
            }
        } else if (now - lastGone >= cooldownMs) {
            spawn(now, screenW, screenH);
        }
    }

    public void primeSpawnTimer(long now) {
        this.lastGone = now;
    }

    public void spawn(long now, int sw, int sh) {
        if (sw <= 0 || sh <= 0) return;

        // Calculate radius based on screen size
        float minR = GameConfig.BLACKHOLE_MIN_RADIUS;
        float maxR = GameConfig.BLACKHOLE_MAX_RADIUS;
        float r = minR + rnd.nextFloat() * (maxR - minR);

        // Calculate safe spawn area (avoid edges and cat area)
        float margin = r * 2f;
        float catAreaTop = sh * 0.7f;

        // Random position within safe area
        float x = margin + rnd.nextFloat() * (sw - margin * 2);
        float y = margin + rnd.nextFloat() * (catAreaTop - margin * 2);

        // Calculate duration
        long minD = Math.min(minDuration, maxDuration);
        long maxD = Math.max(minDuration, maxDuration);
        int durRange = (int)Math.max(1L, maxD - minD);
        long dur = minD + rnd.nextInt(durRange);

        // Create the black hole with dynamic pull strength
        float pullStrength = basePullStrength + rnd.nextFloat() * (maxPullStrength - basePullStrength);
        hole = new BlackHole(x, y, r, now, dur, pullStrength);

        // Reset cooldown
        lastGone = now;
    }


    public void clearBlackHole() {
        hole = null;
        lastGone = 0L; // Reset spawn timer
    }

    public void applyPull(Ball b) {
        if (hole == null) return;

        // Get upgrade multiplier for black hole pull
        float pullMultiplier = 1.0f;
        if (GameState.get().hasUpgrade("blackhole_pull_plus")) {
            pullMultiplier = GameConfig.UPGRADE_BLACKHOLE_PULL_MULTIPLIER;
        }

        // Calculate vector from ball to hole center
        float bx = b.centerX();
        float by = b.centerY();
        float dx = hole.x - bx;
        float dy = hole.y - by;

        float dist2 = dx*dx + dy*dy;
        float maxPullDist = hole.r * 8f;

        // Skip if too far away or inside the core
        if (dist2 > maxPullDist * maxPullDist || dist2 < hole.r * hole.r) return;

        float dist = (float)Math.sqrt(dist2);
        dx /= dist; // Normalize direction vector
        dy /= dist;

        float normalizedDist = (dist - hole.r) / (maxPullDist - hole.r);
        normalizedDist = Math.max(0f, Math.min(1f, normalizedDist));
        float strengthFactor = 1.0f - normalizedDist;
        strengthFactor *= strengthFactor; // Square for falloff

        // Apply pull force towards the hole
        float pullStrength = hole.pullStrength * strengthFactor * pullMultiplier;
        b.vx += dx * pullStrength;
        b.vy += dy * pullStrength;

        if (dist < hole.r * 2.0f) {
            float tangentX = -dy;
            float tangentY = dx;
            float orbitalStrength = pullStrength * 0.3f; // Weaker orbital force

            // Apply the orbital velocity
            b.vx += tangentX * orbitalStrength;
            b.vy += tangentY * orbitalStrength;
        }
    }
}