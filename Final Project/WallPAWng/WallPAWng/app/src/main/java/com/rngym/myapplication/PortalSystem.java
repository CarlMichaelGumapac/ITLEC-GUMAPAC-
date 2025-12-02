package com.rngym.myapplication;

import android.graphics.RectF;
import java.util.Random;

public class PortalSystem {

    public static class Portal {
        public RectF rect;
        public long spawnMs;
        public long durationMs;

        public Portal(RectF r, long s, long d) {
            rect = r;
            spawnMs = s;
            durationMs = d;
        }
    }

    private Portal pA = null;
    private Portal pB = null;
    private GameView gameView; // Reference to GameView for collision checking

    private final Random rnd = new Random();

    public long minDuration = 6000L;   // 6 sec
    public long maxDuration = 11000L;  // 11 sec
    public long cooldownMs = 7000L;    // after disappearance before respawn
    private long lastGone = 0L;

    private boolean hasTeleported = false;

    // Constructor with GameView reference
    public PortalSystem(GameView gameView) {
        this.gameView = gameView;
    }

    public Portal getA() { return pA; }
    public Portal getB() { return pB; }

    public void update(long now) {

        if (now - GameState.get().getGameStartTime() < GameConfig.EARLY_GAME_GRACE_PERIOD_MS) {
            return;
        }

        if (pA != null) {
            if (now - pA.spawnMs >= pA.durationMs) {
                // Despawn portals
                pA = pB = null;
                hasTeleported = false;  // reset one-teleport rule
                lastGone = now;
            }
        }
        if (pA == null && now - lastGone > cooldownMs) {
            spawn(now);
        }
    }

    public void primeSpawnTimer(long now) {
        this.lastGone = now;
    }

    private void spawn(long now) {
        float w = GameConfig.PORTAL_WIDTH;
        float h = GameConfig.PORTAL_HEIGHT;

        // Get screen dimensions from GameView
        float screenW = gameView.screenW;
        float screenH = gameView.screenH;

        // Get portal duration upgrade multiplier
        float durationMultiplier = 1.0f;
        if (GameState.get().hasUpgrade("portal_duration_up")) {
            durationMultiplier = 1.0f + (GameConfig.UPGRADE_PORTAL_DURATION_INCREASE / (float)(maxDuration - minDuration));
        }

        for (int attempt = 0; attempt < 50; attempt++) {
            // Random positions for first portal
            float ax = 80 + rnd.nextInt((int)(screenW - w - 160));
            float ay = 200 + rnd.nextInt((int)(screenH - h - 400));

            RectF portalA = new RectF(ax, ay, ax + w, ay + h);

            // Random positions for second portal
            float bx = 80 + rnd.nextInt((int)(screenW - w - 160));
            float by = 200 + rnd.nextInt((int)(screenH - h - 400));

            RectF portalB = new RectF(bx, by, bx + w, by + h);

            // Check if positions are safe
            if (arePortalPositionsSafe(portalA, portalB)) {
                long range = maxDuration - minDuration;
                long dur = (long)((minDuration + range * rnd.nextFloat()) * durationMultiplier);

                pA = new Portal(portalA, now, dur);
                pB = new Portal(portalB, now, dur);

                hasTeleported = false;
                return; // Success, exit loop
            }
        }
    }

    public void clearPortals() {
        pA = null;
        pB = null;
        hasTeleported = false;
        lastGone = 0L; // Reset spawn timer
    }

    private boolean arePortalPositionsSafe(RectF portalA, RectF portalB) {
        // Check if portals overlap with each other
        if (RectF.intersects(portalA, portalB)) {
            return false;
        }

        // Check if portals overlap with boxes
        for (GameView.Box box : gameView.boxes) {
            if (RectF.intersects(portalA, box.rect) || RectF.intersects(portalB, box.rect)) {
                return false;
            }
        }

        // Check if portals overlap with bumpers
        for (Bumper bumper : gameView.bumperSystem.getBumpers()) {
            if (bumper != null && bumper.rect != null) {
                if (RectF.intersects(portalA, bumper.rect) || RectF.intersects(portalB, bumper.rect)) {
                    return false;
                }
            }
        }

        // Check if portals overlap with black hole
        BlackHoleSystem.BlackHole blackHole = gameView.blackHoleSystem.get();
        if (blackHole != null) {
            RectF blackHoleRect = blackHole.rect();
            if (RectF.intersects(portalA, blackHoleRect) || RectF.intersects(portalB, blackHoleRect)) {
                return false;
            }
        }

        // Check if portals overlap with cat area
        RectF catRect = new RectF(gameView.catX, gameView.catY,
                gameView.catX + gameView.catW, gameView.catY + gameView.catH);
        if (RectF.intersects(portalA, catRect) || RectF.intersects(portalB, catRect)) {
            return false;
        }

        // Check if portals are too close to screen edges
        float margin = 50f;
        if (portalA.left < margin || portalA.right > gameView.screenW - margin ||
                portalB.left < margin || portalB.right > gameView.screenW - margin ||
                portalA.top < margin || portalA.bottom > gameView.screenH - margin ||
                portalB.top < margin || portalB.bottom > gameView.screenH - margin) {
            return false;
        }

        return true; // Positions are safe
    }

    public Portal whichPortal(RectF ballRect, long ballLastTP, long cooldown, long now) {
        if (pA == null) return null;
        if (hasTeleported) return null;

        // Ball cooldown check
        if (now - ballLastTP < cooldown) return null;

        if (RectF.intersects(ballRect, pA.rect)) {
            hasTeleported = true;
            return pA;
        }
        if (RectF.intersects(ballRect, pB.rect)) {
            hasTeleported = true;
            return pB;
        }

        return null;
    }

    public Portal getLinked(Portal src) {
        if (src == null) return null;
        return (src == pA) ? pB : pA;
    }
}