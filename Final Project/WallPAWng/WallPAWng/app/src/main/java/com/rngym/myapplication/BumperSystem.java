package com.rngym.myapplication;

import android.graphics.RectF;
import android.util.Log;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class BumperSystem {
    private final List<Bumper> bumpers = new ArrayList<>();
    private final Random rnd = new Random();


    private long lastGone = 0L;
    private long cooldownMs = 5000L; // 5 second cooldown after all bumpers expire
    private float screenW = 0;
    private float screenH = 0;

    // Editable parameters (tweakable)
    public int maxBumpers = 3;
    public float minWidthFrac = 0.10f;
    public float maxWidthFrac = 0.30f;
    public float heightPx = 20f;

    public List<Bumper> getBumpers() { return bumpers; }

    // regenerate bumpers randomly avoiding the catRect area
    public void regenerate(float screenW, float screenH, RectF catRect) {

        this.screenW = screenW;
        this.screenH = screenH;

        bumpers.clear();

        if (screenW <= 0 || screenH <= 0) {
            return;
        }

        int count = 1;

        RectF inflatedCatRect = new RectF(catRect);
        inflatedCatRect.inset(-80f, -80f);

        for (int i = 0; i < count; i++) {
            boolean spawned = false;
            int attempts = 0;
            while (!spawned && attempts < 100) {
                float w = screenW * (minWidthFrac + rnd.nextFloat() * (maxWidthFrac - minWidthFrac));
                float h = heightPx;
                float left = rnd.nextFloat() * (screenW - w);
                float top = rnd.nextFloat() * (screenH - h);
                RectF newBumperRect = new RectF(left, top, left + w, top + h);

                if (RectF.intersects(newBumperRect, inflatedCatRect)) {
                    attempts++;
                    continue;
                }

                boolean rotates = rnd.nextFloat() < 0.25f;
                float angle = 0f;
                float bounce = 1.2f;
                long life = 30000L;

                bumpers.add(new Bumper(newBumperRect, angle, rotates, bounce, System.currentTimeMillis(), life));

                spawned = true;
            }
        }
    }

    public void update(long nowMs) {

        if (nowMs - GameState.get().getGameStartTime() < GameConfig.EARLY_GAME_GRACE_PERIOD_MS) {
            return;
        }

        // Remove any expired bumpers
        Iterator<Bumper> it = bumpers.iterator();
        while (it.hasNext()) {
            Bumper b = it.next();
            if (nowMs - b.spawnAtMs > b.lifeMs) {
                it.remove();
                Log.d("BumperSystem", "A bumper expired.");
            }
        }

        if (nowMs - lastGone > 15000L) { // 15 seconds
            trySpawnOneBumper(nowMs);
            lastGone = nowMs; // Reset the timer
        }
    }

    public void primeSpawnTimer(long now) {
        this.lastGone = now;
    }

    public void clearAllBumpers() {
        bumpers.clear();
        lastGone = 0L; // Reset spawn timer
    }

    private void trySpawnOneBumper(long nowMs) {
        if (screenW <= 0 || screenH <= 0) return;

        int attempts = 0;
        while (attempts < 50) {
            float w = screenW * (minWidthFrac + rnd.nextFloat() * (maxWidthFrac - minWidthFrac));
            float h = heightPx;
            float left = rnd.nextFloat() * (screenW - w);
            float top = rnd.nextFloat() * (screenH - h);
            RectF newBumperRect = new RectF(left, top, left + w, top + h);

            RectF catRect = new RectF(screenW * 0.3f, screenH * 0.8f, screenW * 0.7f, screenH * 0.95f);
            if (RectF.intersects(newBumperRect, catRect)) {
                attempts++;
                continue;
            }


            boolean rotates = rnd.nextFloat() < 0.25f;
            float angle = 0f;
            float bounce = 1.2f;
            long life = 20000L + rnd.nextInt(10000); // 20-30 seconds life

            bumpers.add(new Bumper(newBumperRect, angle, rotates, bounce, nowMs, life));
            return;
        }
    }

    public void spawn(long now) {
        if (screenW <= 0 || screenH <= 0) {
            return;
        }

            float w = 100f;
            float h = 20f;
            float left = screenW / 2f - w / 2f;
            float top = screenH / 2f - h / 2f;
            RectF bumperRect = new RectF(left, top, left + w, top + h);

            long life = 30000L; // 30 seconds
            bumpers.add(new Bumper(bumperRect, 0f, false, 1.2f, now, life));
            lastGone = now; // Reset cooldown timer
    }

    public Bumper firstIntersecting(android.graphics.RectF ballRect) {
        for (Bumper b : bumpers) {
            if (b == null || b.rect == null) continue;
            if (RectF.intersects(ballRect, b.rect)) return b;
        }
        return null;
    }
}