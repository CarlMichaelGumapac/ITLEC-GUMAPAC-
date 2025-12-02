package com.rngym.myapplication;

import android.graphics.RectF;

public class PhysicsEngine {

    // maximum allowed velocity magnitude (px per frame)
    public static final float MAX_SPEED = 120f;
    public static final float SUBSTEP = 18f;

    // clamp velocity to MAX_SPEED
    public static void clampVelocity(Ball b) {
        float vx = b.vx, vy = b.vy;
        float mag = (float)Math.hypot(vx, vy);
        if (mag > MAX_SPEED) {
            float scale = MAX_SPEED / mag;
            b.vx *= scale;
            b.vy *= scale;
        }
    }

    public interface StepCallback {
        boolean onStep();
    }

    public static void moveWithSubsteps(Ball b, StepCallback callback) {
        float dx = b.vx;
        float dy = b.vy;
        float dist = (float)Math.hypot(dx, dy);
        int steps = Math.max(1, (int)Math.ceil(dist / SUBSTEP));
        float sx = dx / steps;
        float sy = dy / steps;
        for (int i = 0; i < steps; i++) {
            b.x += sx;
            b.y += sy;

            if (callback != null && !callback.onStep()) {

                break;
            }
        }
    }

    public static void moveWithSubsteps(Ball b) {
        moveWithSubsteps(b, null);
    }

    public static void reflectFromRectVertical(Ball b, float bounce) {
        b.vy = -Math.abs(b.vy) * bounce;
    }

    public static boolean intersects(Ball b, RectF r) {
        RectF br = new RectF(b.x, b.y, b.x + b.r, b.y + b.r);
        return RectF.intersects(br, r);
    }

    // detect contact on top surface (for cat)
    public static boolean contactTop(Ball b, RectF r) {
        boolean horiz = (b.x + b.r > r.left) && (b.x < r.right);
        boolean verticalTouch = (b.y + b.r >= r.top) && (b.y + b.r <= r.top + r.height()*0.5f);
        return horiz && verticalTouch;
    }
}
