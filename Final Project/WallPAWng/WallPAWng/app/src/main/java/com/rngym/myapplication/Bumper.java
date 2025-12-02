package com.rngym.myapplication;

import android.graphics.RectF;

public class Bumper {
    public RectF rect;
    public float angleDeg;
    public boolean rotates;
    public float bounce;
    public long spawnAtMs;
    public long lifeMs;

    public Bumper(RectF rect, float angleDeg, boolean rotates, float bounce, long spawnAtMs, long lifeMs) {
        this.rect = rect;
        this.angleDeg = angleDeg;
        this.rotates = rotates;
        this.bounce = bounce;
        this.spawnAtMs = spawnAtMs;
        this.lifeMs = lifeMs;
    }
}
