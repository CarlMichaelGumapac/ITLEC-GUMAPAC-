package com.rngym.myapplication;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.MotionEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class GameView extends SurfaceView implements Runnable {

    // === CORE ===
    private final SurfaceHolder holder;
    private Thread thread;
    private volatile boolean running = false;

    // Screen dimensions
    float screenW = 0;
    float screenH = 0;

    // === GAME OBJECTS ===
    public final List<Ball> balls = new ArrayList<>();
    public final List<Box> boxes = new ArrayList<>();

    // === CAT ===
    float catX;
    float catY;
    float catW;
    float catH;

    // === SUBSYSTEMS ===
    final BumperSystem bumperSystem = new BumperSystem();
    private final PortalSystem portalSystem = new PortalSystem(this);
    final BlackHoleSystem blackHoleSystem = new BlackHoleSystem();
    private final UpgradeManager upgradeManager = new UpgradeManager();
    private final GameState gs = GameState.get();

    private boolean gracePeriodJustEnded = false;
    boolean gameOverTriggered = false;

    // === CONFIGURATION ===

    // +++ Cat +++
    public float catWidthFrac = GameConfig.CAT_WIDTH_FRACTION;
    public float catHeightPx = GameConfig.CAT_HEIGHT_PX;

    // +++ Ball +++
    public float ballSizePercent = GameConfig.BALL_SIZE_PERCENT;
    public float startingVY = GameConfig.STARTING_VY;
    public float minVX = GameConfig.MIN_VX;
    public float maxVX = GameConfig.MAX_VX;

    // +++ Box spawning +++
    private float boxMinWidth = GameConfig.BOX_MIN_WIDTH;
    private float boxMaxWidth = GameConfig.BOX_MAX_WIDTH;
    private float boxHeight = GameConfig.BOX_HEIGHT;
    private int boxMinHP = GameConfig.BOX_MIN_HP;
    private int boxMaxHP = GameConfig.BOX_MAX_HP;
    private long lastBoxSpawn = 0L;
    private long boxSpawnCooldown = GameConfig.BOX_SPAWN_COOLDOWN;
    private int maxBoxes = GameConfig.MAX_BOXES;

    // === RENDERING ===

    // +++ Paints +++
    private final Paint pBall = new Paint();
    private final Paint pCat = new Paint();
    private final Paint pBumper = new Paint();
    private final Paint pBox = new Paint();
    private final Paint pPopup = new Paint();
    private final Paint pPortal = new Paint();
    private final Paint pBlackHole = new Paint();
    private final Paint pComboText = new Paint();

    // +++ Sprites +++
    private Bitmap catBitmap = null;
    private Bitmap yarnBitmap = null;
    private Bitmap portalBitmap = null;
    private Bitmap blackHoleBitmap = null;

    // +++ Cached scaled sprites +++
    private Bitmap cachedCatBitmap = null;
    private Bitmap cachedYarnBitmap = null;
    private float cachedCatW = 0f, cachedCatH = 0f;
    private float cachedYarnSize = 0f;

    // +++ Popups +++
    private static class Popup {
        String txt;
        float x, y, vy;
        long born, life;
        float scale;
        Paint p;

        Popup(String t, float x, float y, long born, long life, Paint p) {
            this.txt = t;
            this.x = x;
            this.y = y;
            this.born = born;
            this.life = life;
            this.p = p;
            this.vy = GameConfig.POPUP_RISE_SPEED;
            this.scale = 1f;
        }
    }
    private final List<Popup> popups = new ArrayList<>();

    // === INPUT ===
    private boolean movingLeft = false;
    private boolean movingRight = false;

    // === STATE ===
    public enum State { TITLE, PLAYING, PAUSED }
    private State state = State.TITLE;

    // === LISTENERS ===
    public interface LevelUpListener {
        void onLevelUp(int level, int xp, UpgradeManager.Choice[] choices);
    }

    public interface GameOverListener {
        void onGameOver(int finalScore, int highScore);
    }

    private LevelUpListener levelUpListener;
    private GameOverListener gameOverListener;

    // === AUDIO ===
    private SoundPool soundPool;
    private int soundHit;
    private int soundMiss;
    private MediaPlayer backgroundMusicPlayer;
    private MediaPlayer titleMusicPlayer;

    // === UTILITIES ===
    private final Random rnd = new Random();

    // === BOX CLASS ===
    public static class Box {
        RectF rect;
        int hp;
        int xpReward;
        int scoreReward;

        Box(RectF r, int hp, int xp, int sc) {
            rect = r;
            this.hp = hp;
            this.xpReward = xp;
            this.scoreReward = sc;
        }
    }

    // === CONSTRUCTOR ===

    public GameView(Context ctx, AttributeSet attrs) {
        super(ctx, attrs);
        holder = getHolder();
        initPaints();
        initAudio();
        loadSprites();
    }

    // === PAINT OBJECTS ===
    private void initPaints() {
        // Ball paint
        pBall.setColor(0xFFD6D6D6);

        // Cat paint
        pCat.setColor(0xFFFFFF66);

        // Bumper paint
        pBumper.setColor(0xFFB480E0);

        // Box paint
        pBox.setColor(0xFFC88C78);

        // Popup paint
        pPopup.setColor(0xFFFFFFFF);
        pPopup.setTextSize(GameConfig.POPUP_TEXT_SIZE);
        pPopup.setTextAlign(Paint.Align.CENTER);

        // Portal paint
        pPortal.setColor(0xFF00FFFF);
        pPortal.setStyle(Paint.Style.STROKE);
        pPortal.setStrokeWidth(4f);

        // Black hole paint
        pBlackHole.setColor(0xFF111111);

        // Combo text paint
        pComboText.setColor(Color.YELLOW);
        pComboText.setTextSize(32f);
        pComboText.setTextAlign(Paint.Align.CENTER);
        pComboText.setTypeface(Typeface.DEFAULT_BOLD);
        pComboText.setShadowLayer(2.0f, 1.0f, 1.0f, Color.BLACK);
    }

    // === INITIALIZE AUDIO SYSTEM ===
    private void initAudio() {
        AudioAttributes attrs = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        soundPool = new SoundPool.Builder()
                .setMaxStreams(6)
                .setAudioAttributes(attrs)
                .build();

        //  === SOUND EFFECTS ===
        try {
            soundHit = soundPool.load(getContext(), R.raw.meow_hit, 1);
            soundMiss = soundPool.load(getContext(), R.raw.meow_miss, 1);
        } catch (Exception e) {
            android.util.Log.e("GameView", "Failed to load sound effects: " + e.getMessage());
        }

        // === BACKGROUND MUSIC ===
        try {
            backgroundMusicPlayer = MediaPlayer.create(getContext(), R.raw.background_music);
            if (backgroundMusicPlayer != null) {
                backgroundMusicPlayer.setLooping(true);
                backgroundMusicPlayer.setVolume(0.5f, 0.5f);
            }
        } catch (Exception e) {
            android.util.Log.w("GameView", "Background music not found: " + e.getMessage());
        }

        // === TITLE MUSIC ===
        try {
            titleMusicPlayer = MediaPlayer.create(getContext(), R.raw.title_music);
            if (titleMusicPlayer != null) {
                titleMusicPlayer.setLooping(true);
                titleMusicPlayer.setVolume(0.5f, 0.5f);
            }
        } catch (Exception e) {
            android.util.Log.w("GameView", "Title music not found: " + e.getMessage());
        }
    }

    private void loadSprites() {

            catBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.cat_sprite);
            yarnBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.yarnball_red);
            portalBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.portal_blue);
            blackHoleBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.black_hole);

    }

    // === LIFECYCLE ===

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        screenW = w;
        screenH = h;

        // CAT POSITION
        catW = Math.min(screenW * catWidthFrac, screenW * GameConfig.CAT_MAX_WIDTH_FRACTION);
        catH = catHeightPx;
        catX = (screenW - catW) / 2f;
        catY = screenH - catH - 120f;

        // CACHE SPRITES
        updateCachedCatBitmap();
        updateCachedYarnBitmap();

        // INITIAL BUMPERS
        RectF catSafeZone = new RectF(
                catX - 120f,
                catY - 120f,
                catX + catW + 120f,
                catY + catH + 120f
        );
        bumperSystem.regenerate(screenW, screenH, catSafeZone);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        // Cleanup cached bitmaps
        if (cachedCatBitmap != null && cachedCatBitmap != catBitmap) {
            cachedCatBitmap.recycle();
            cachedCatBitmap = null;
        }

        if (cachedYarnBitmap != null && cachedYarnBitmap != yarnBitmap) {
            cachedYarnBitmap.recycle();
            cachedYarnBitmap = null;
        }

        // Release media players
        if (backgroundMusicPlayer != null) {
            backgroundMusicPlayer.release();
            backgroundMusicPlayer = null;
        }

        if (titleMusicPlayer != null) {
            titleMusicPlayer.release();
            titleMusicPlayer = null;
        }
    }

    // === BITMAP CACHING ===


    private void updateCachedCatBitmap() {
        if (catBitmap == null) {
            cachedCatBitmap = null;
            return;
        }

        if (cachedCatBitmap != null && cachedCatW == catW && cachedCatH == catH) {
            return;
        }

        try {
            if (cachedCatBitmap != null && cachedCatBitmap != catBitmap) {
                cachedCatBitmap.recycle();
            }

            Bitmap result = Bitmap.createBitmap((int)catW, (int)catH, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(result);

            // Draw the scaled bitmap centered
            Rect srcRect = new Rect(0, 0, catBitmap.getWidth(), catBitmap.getHeight());
            Rect dstRect = new Rect(0, 0, (int)catW, (int)catH);
            canvas.drawBitmap(catBitmap, srcRect, dstRect, null);

            cachedCatBitmap = result;
            cachedCatW = catW;
            cachedCatH = catH;
        } catch (Exception e) {
            cachedCatBitmap = null;
        }
    }

    private void updateCachedYarnBitmap() {
        if (yarnBitmap == null) {
            cachedYarnBitmap = null;
            return;
        }

        float targetSize = Math.max(10f, screenW * ballSizePercent);

        if (cachedYarnBitmap != null && Math.abs(cachedYarnSize - targetSize) < 2f) {
            return;
        }

        try {
            if (cachedYarnBitmap != null && cachedYarnBitmap != yarnBitmap) {
                cachedYarnBitmap.recycle();
            }

            int size = (int)targetSize;
            cachedYarnBitmap = Bitmap.createScaledBitmap(
                    yarnBitmap,
                    size,
                    size,
                    true
            );
            cachedYarnSize = targetSize;

        } catch (Exception e) {
            cachedYarnBitmap = null;
        }
    }

    // === INPUT HANDLING ===

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        float x = ev.getX();
        float y = ev.getY();
        int action = ev.getActionMasked();

        if (state == State.TITLE || state == State.PAUSED) {
            return true;
        }

        // Game touch controls
        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE) {
            movingLeft = x < screenW / 2f;
            movingRight = !movingLeft;

            // Ensure balls start moving on first touch
            for (Ball b : balls) {
                if (!b.isMoving()) {
                    b.vx = randomVX();
                    b.vy = Math.abs(startingVY);
                }
            }
        } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            movingLeft = movingRight = false;
        }

        return true;
    }

    // === GAME LOOP ===

    @Override
    public void run() {
        long last = System.currentTimeMillis();

        while (running) {
            long now = System.currentTimeMillis();
            float dt = (now - last) / 1000f;
            last = now;

            // Update subsystems
            portalSystem.update(now);
            blackHoleSystem.update(now, (int)screenW, (int)screenH);
            bumperSystem.update(now);

            // Update gameplay
            if (state == State.PLAYING && !gs.isPaused()) {
                updateGameplay(now, dt);
            }

            // Render frame
            render();

            // Target 60 FPS
            try {
                Thread.sleep(GameConfig.FRAME_TIME_MS);
            } catch (InterruptedException e) {
                // Ignore
            }
        }
    }

    private void updateGameplay(long now, float dt) {

        if (!gracePeriodJustEnded && (now - gs.getGameStartTime() >= GameConfig.EARLY_GAME_GRACE_PERIOD_MS)) {
            gracePeriodJustEnded = true;

            lastBoxSpawn = now;
            bumperSystem.primeSpawnTimer(now);
            portalSystem.primeSpawnTimer(now);
            blackHoleSystem.primeSpawnTimer(now);
        }

        // Move cat
        updateCatMovement();

        // Update balls
        updateBalls(now);

        // Remove expired small balls
        removeExpiredSmallBalls();

        // Update spawners
        updateBoxSpawner(now);

        // Update popups
        updatePopups();

        float currentStress = GameState.get().getStress();
        float newStress = Math.max(0f, currentStress - GameConfig.STRESS_DECAY_RATE);
        GameState.get().setStress(newStress);

        checkLevelUp();

    }

    private void updateCatMovement() {
        float catSpeed = GameConfig.CAT_SPEED;

        if (movingLeft) {
            catX -= catSpeed;
        }
        if (movingRight) {
            catX += catSpeed;
        }

        // Clamp to screen bounds
        if (catX < 0) catX = 0;
        if (catX + catW > screenW) catX = screenW - catW;
    }

    private void enforceMinimumSpeed(Ball ball) {
        float currentSpeed = (float) Math.sqrt(ball.vx * ball.vx + ball.vy * ball.vy);
        if (currentSpeed < GameConfig.MIN_SPEED_AFTER_COLLISION) {
            float scale = GameConfig.MIN_SPEED_AFTER_COLLISION / (currentSpeed + 0.001f); // Add small value to prevent division by zero
            ball.vx *= scale;
            ball.vy *= scale;
        }
    }

    private void updateBalls(long now) {
        List<Ball> snapshot = new ArrayList<>(balls);

        for (Ball ball : snapshot) {
            float totalDx = ball.vx;
            float totalDy = ball.vy;

            // SUBSTEPS CALCULATION
            int steps = Math.max(1, (int) Math.ceil(Math.hypot(totalDx, totalDy) / GameConfig.SUBSTEP_DISTANCE));
            float stepDx = totalDx / steps;
            float stepDy = totalDy / steps;

            for (int step = 0; step < steps; step++) {
                ball.x += stepDx;
                ball.y += stepDy;

                // Check for collisions after each small move
                handleWallCollisions(ball);
                handleBumperCollisions(ball);
                handleBoxCollisions(ball);
                handleCatCollision(ball);
                handleBottomMiss(ball);

                blackHoleSystem.applyPull(ball);
            }

            // Apply portal teleportation after all movement for the frame is done
            handlePortalTeleport(ball, now);

            // Ensure ball is moving
            if (!ball.isMoving()) {
                ball.vx = randomVX();
                ball.vy = Math.abs(startingVY);
            }

            enforceMinimumSpeed(ball);

            // Clamp velocity to max speed
            PhysicsEngine.clampVelocity(ball);
        }
    }

    private void handleWallCollisions(Ball ball) {
        // Left wall
        if (ball.x <= 0) {
            ball.x = 0;
            ball.vx = Math.abs(ball.vx) * GameConfig.WALL_BOUNCE_DAMPING;
        }

        // Right wall
        if (ball.x + ball.r >= screenW) {
            ball.x = screenW - ball.r;
            ball.vx = -Math.abs(ball.vx) * GameConfig.WALL_BOUNCE_DAMPING;
        }

        // Top wall (ceiling)
        if (ball.y <= 80f) {
            ball.y = 80f;
            ball.vy = Math.abs(ball.vy) * GameConfig.WALL_BOUNCE_DAMPING;
        }
    }

    private void handleBumperCollisions(Ball ball) {
        long now = System.currentTimeMillis();

        if (now - ball.lastCollisionTimeMs < Ball.COLLISION_COOLDOWN_MS) {
            return;
        }

        RectF ballRect = new RectF(ball.x, ball.y, ball.x + ball.r, ball.y + ball.r);
        Bumper hit = bumperSystem.firstIntersecting(ballRect);

        if (hit != null) {
            ball.lastCollisionTimeMs = now;

            // Calculate the center of the ball and bumper
//            float ballCenterX = ball.centerX();
//            float ballCenterY = ball.centerY();
//            float bumperCenterX = hit.rect.centerX();
//            float bumperCenterY = hit.rect.centerY();

            // Calculate overlap on each axis
            float overlapLeft = ballRect.right - hit.rect.left;
            float overlapRight = hit.rect.right - ballRect.left;
            float overlapTop = ballRect.bottom - hit.rect.top;
            float overlapBottom = hit.rect.bottom - ballRect.top;

            // Find the smallest overlap to determine the best direction to push the ball
            float minOverlap = Math.min(Math.min(overlapLeft, overlapRight), Math.min(overlapTop, overlapBottom));

            // Teleport ball to safety based on the smallest overlap
            if (minOverlap == overlapLeft) {
                // Ball hit the left side, push it left
                ball.x = hit.rect.left - ball.r - 1f;
                ball.vx = -Math.abs(ball.vx) * hit.bounce;
            } else if (minOverlap == overlapRight) {

                // Ball hit the right side, push it right
                ball.x = hit.rect.right + 1f;
                ball.vx = Math.abs(ball.vx) * hit.bounce;
            } else if (minOverlap == overlapTop) {

                // Ball hit the top, push it up
                ball.y = hit.rect.top - ball.r - 1f;
                ball.vy = -Math.abs(ball.vy) * hit.bounce;
            } else {

                // Ball hit the bottom, push it down
                ball.y = hit.rect.bottom + 1f;
                ball.vy = Math.abs(ball.vy) * hit.bounce;
            }

            // Add a small random kick to prevent predictable bouncing
            ball.vx += (rnd.nextFloat() - 0.5f) * 2f;

            // Ensure minimum speed and clamp velocity
            enforceMinimumSpeed(ball);
            PhysicsEngine.clampVelocity(ball);


            // Reward
            gs.addScore(1);
            spawnPopup("+1", ball.centerX(), ball.centerY());
        }
    }

    private void handlePortalTeleport(Ball ball, long now) {
        RectF ballRect = new RectF(ball.x, ball.y, ball.x + ball.r, ball.y + ball.r);

        PortalSystem.Portal hitPortal = portalSystem.whichPortal(
                ballRect,
                ball.lastTeleportedAt,
                ball.teleportCooldown,
                now
        );

        if (hitPortal != null) {
            PortalSystem.Portal dest = portalSystem.getLinked(hitPortal);

            if (dest != null) {
                ball.lastTeleportedAt = now;
                float cx = dest.rect.centerX();
                float cy = dest.rect.centerY();
                ball.setCenter(cx, cy);
            }
        }
    }

    private void handleBoxCollisions(Ball ball) {
        RectF ballRect = new RectF(ball.x, ball.y, ball.x + ball.r, ball.y + ball.r);
        Iterator<Box> it = boxes.iterator();

        while (it.hasNext()) {
            Box box = it.next();

            if (RectF.intersects(ballRect, box.rect)) {

                // Calculate overlap on each axis
                float overlapLeft = ballRect.right - box.rect.left;
                float overlapRight = box.rect.right - ballRect.left;
                float overlapTop = ballRect.bottom - box.rect.top;
                float overlapBottom = box.rect.bottom - ballRect.top;

                // Find the smallest overlap and push the ball out in that direction
                float minOverlapX = Math.min(overlapLeft, overlapRight);
                float minOverlapY = Math.min(overlapTop, overlapBottom);

                if (minOverlapX < minOverlapY) {
                    // Push horizontally
                    if (overlapLeft < overlapRight) {
                        ball.x -= minOverlapX + 1f; // Push left
                    } else {
                        ball.x += minOverlapX + 1f; // Push right
                    }
                } else {
                    // Push vertically
                    if (overlapTop < overlapBottom) {
                        ball.y -= minOverlapY + 1f; // Push up
                    } else {
                        ball.y += minOverlapY + 1f; // Push down
                    }
                }

                // Bounce ball
                ball.vy = -ball.vy * GameConfig.BOUNCE_DAMPING;
                PhysicsEngine.clampVelocity(ball);

                // Damage the box
                box.hp -= 1;

                if (box.hp <= 0) {
                    // Box destroyed -> apply rewards
                    gs.addScore(box.scoreReward);
                    gs.addXP(box.xpReward);

                    spawnPopup("+" + box.xpReward + " XP", box.rect.centerX(), box.rect.centerY());
                    spawnPopup("+" + box.scoreReward, box.rect.centerX(), box.rect.centerY() + 20f);

                    it.remove();
                } else {
                    // Box still has HP
                    spawnPopup(String.valueOf(box.hp), box.rect.centerX(), box.rect.centerY());
                }

                return;
            }
        }
    }

    private void handleCatCollision(Ball ball) {
        RectF catRect = new RectF(catX, catY, catX + catW, catY + catH);

        // Only catch if ball is moving downward
        if (ball.vy > 0 && PhysicsEngine.contactTop(ball, catRect)) {
            // Calculate bounce angle based on hit position
            float catCenter = catX + catW / 2f;
            float hitPos = ball.centerX() - catCenter;
            float norm = hitPos / (catW / 2f);

            ball.vx += norm * 4f;
            ball.vy = -Math.abs(ball.vy) - 1.2f;
            PhysicsEngine.clampVelocity(ball);

            // Register catch for combo system
            gs.registerCatch();

            // Get combo multiplier
            float comboMult = gs.getComboMultiplier();
            int comboCount = gs.getCombo();

            // Calculate base rewards
            int baseXP = GameConfig.BASE_XP_PER_CATCH;
            int baseScore = GameConfig.SCORE_PER_CATCH;

            // Apply upgrade bonuses
            if (gs.hasUpgrade("score_x2")) {
                baseScore *= 2;
            }

            // Apply combo multiplier
            int finalXP = (int)(baseXP * comboMult);
            int finalScore = (int)(baseScore * comboMult);

            // Grant rewards
            gs.addXP(finalXP);
            gs.addScore(finalScore);

            // Reduce stress
            float stressReduction = GameConfig.STRESS_ON_CATCH;
            if (gs.hasUpgrade("stress_reducer")) {
                stressReduction -= 5f;
            }
            gs.addStress(stressReduction);

            // Play hit sound
            if (soundHit > 0) {
                soundPool.play(soundHit, 1.0f, 1.0f, 0, 0, 1.0f);
            }

            // Show popups with combo info
            if (comboCount > 1) {
                spawnPopup("+" + finalXP + " XP (x" + comboCount + ")", ball.centerX(), ball.centerY());
            } else {
                spawnPopup("+" + finalXP + " XP", ball.centerX(), ball.centerY());
            }

            spawnPopup("+" + finalScore, ball.centerX(), ball.centerY() + 40f);

            // Show special combo milestone popups
            if (comboCount == 5) {
                spawnPopup("5 COMBO! 1.5x MULTIPLIER!", screenW / 2f, screenH * 0.3f);
            } else if (comboCount == 10) {
                spawnPopup("10 COMBO! 2x MULTIPLIER!", screenW / 2f, screenH * 0.3f);
            } else if (comboCount == 20) {
                spawnPopup("20 COMBO! 3x MULTIPLIER!", screenW / 2f, screenH * 0.3f);
            }
            checkLevelUp();
        }
    }

    private void handleBottomMiss(Ball ball) {
        if (ball.y > screenH + 200f) {
            // Check for cat_reflect upgrade
            if (gs.hasUpgrade("cat_reflect")) {
                gs.removeUpgrade("cat_reflect");
                ball.vy = -Math.abs(startingVY) * 1.5f;
                spawnPopup("SAVED!", ball.centerX(), screenH / 2f);
                return;
            }

            // Break combo on miss
            int lostCombo = gs.getCombo();
            gs.registerMiss();

            // Show combo lost message if there was a combo
            if (lostCombo >= 5) {
                spawnPopup("COMBO LOST! (" + lostCombo + ")", screenW / 2f, screenH * 0.4f);
            }

            // Add stress
            gs.addStress(GameConfig.STRESS_ON_MISS);
            gs.addScore(GameConfig.SCORE_PENALTY_ON_MISS);

            checkGameOver();

            // Play miss sound
            if (soundMiss > 0) {
                soundPool.play(soundMiss, 1.0f, 1.0f, 0, 0, 1.0f);
            }

            // Respawn ball at cat position
            ball.x = catX + catW / 2f - ball.r / 2f;
            ball.y = catY - ball.r - 8f;
            ball.vx = randomVX();
            ball.vy = -Math.abs(startingVY);
        }
    }

    private void removeExpiredSmallBalls() {
        balls.removeIf(Ball::isExpired);
    }

    public void resetGameOverFlag() {
        this.gameOverTriggered = false;
    }

    private void checkGameOver() {
        // If game over has already been triggered, do nothing
        if (gameOverTriggered) {
            return;
        }

        float currentStress = gs.getStress();
        float maxStress = gs.getMaxStress();

        // --- DIAGNOSTIC LOGGING --- DEV NOTES: WENT THRU AN 4 HOUR DEBUGGING BECAUSE OF STRESS AND GAMEOVER FEATURE
        android.util.Log.e("GameView_GameOver", "--- DIAGNOSTIC CHECK ---");
        android.util.Log.e("GameView_GameOver", "currentStress (float): " + currentStress);
        android.util.Log.e("GameView_GameOver", "maxStress (float): " + maxStress);

        // Explicitly cast to double for comparison to avoid any float weirdness
        double currentStressD = (double) currentStress;
        double maxStressD = (double) maxStress;

        android.util.Log.e("GameView_GameOver", "currentStress (double): " + currentStressD);
        android.util.Log.e("GameView_GameOver", "maxStress (double): " + maxStressD);
        android.util.Log.e("GameView_GameOver", "Listener is null: " + (gameOverListener == null));
        android.util.Log.e("GameView_GameOver", "State is: " + state);
        android.util.Log.e("GameView_GameOver", "gameOverTriggered is: " + gameOverTriggered);
        // --- END DIAGNOSTIC LOGGING --- DEV NOTES: 5 CUPS OF COFFEE AND A DREAM

        if (currentStressD > (maxStressD - 0.01)) {
            android.util.Log.e("GameView_GameOver", "!!! CONDITION MET !!! ENTERING GAME OVER BLOCK.");

            gameOverTriggered = true; // Set flag to true immediately

            // Update high score first
            gs.maybeUpdateHighScore();

            // Stop the game immediately
            gs.setPaused(true);
            state = State.PAUSED;

            // Call listener if it exists
            if (gameOverListener != null) {
                int finalScore = gs.getScore();
                int highScore = gs.getHighScore();
                gameOverListener.onGameOver(finalScore, highScore);
            } else {
                android.util.Log.e("GameView_GameOver", "CRITICAL: gameOverListener is NULL!");
            }
        } else {
            android.util.Log.e("GameView_GameOver", "!!! CONDITION NOT MET !!!");
        }
    }

    private void checkLevelUp() {
        int currentLevel = gs.getLevel();
        int currentXp = gs.getXP();
        int xpNeeded = GameConfig.xpForLevel(currentLevel);

        if (currentXp >= xpNeeded) {

            // Pause the game first
            gs.setPaused(true);

            // Calculate the carry-over XP before leveling up
            int carryOverXp = currentXp - xpNeeded;

            // Manually level up the player
            gs.levelUp(); // This increments level and resets XP to 0

            // Add back the carry-over XP
            gs.addXP(carryOverXp);

            // Generate and show upgrade choices
            UpgradeManager.Choice[] choices = upgradeManager.generate(3);

            if (levelUpListener != null) {
                levelUpListener.onLevelUp(gs.getLevel(), gs.getXP(), choices);
            } else {
                // Resume the game if the listener is missing to prevent a soft-lock
                gs.setPaused(false);
            }
            // Check for another level up in case carry-over XP is enough
            checkLevelUp();
        }
    }

    // === BOX SPAWNER ===

    private void updateBoxSpawner(long now) {

        if (now - gs.getGameStartTime() < GameConfig.EARLY_GAME_GRACE_PERIOD_MS) {
            return;
        }

        long adjustedCooldown = GameConfig.getAdjustedBoxSpawnCooldown(gs.getLevel());
        int adjustedMaxBoxes = GameConfig.getMaxBoxesForLevel(gs.getLevel());

        if (now - lastBoxSpawn < adjustedCooldown) return;
        if (boxes.size() >= adjustedMaxBoxes) return;

        if (trySpawnBox(now)) {
            lastBoxSpawn = now;
        }
    }

    private boolean trySpawnBox(long now) {
        if (screenW <= 0 || screenH <= 0) return false;

        // Randomize properties
        float width = boxMinWidth + rnd.nextFloat() * (boxMaxWidth - boxMinWidth);
        int hp = boxMinHP + rnd.nextInt(boxMaxHP - boxMinHP + 1);

        // Scale HP with level
        hp = GameConfig.getBoxHPForLevel(gs.getLevel(), hp);

        // Calculate rewards
        int xpReward = hp * GameConfig.BOX_XP_PER_HP + gs.getLevel();
        int scoreReward = hp * GameConfig.BOX_SCORE_PER_HP + gs.getLevel() * 2;

        // Try to find safe spawn position
        for (int attempt = 0; attempt < 20; attempt++) {
            float left = 40f + rnd.nextFloat() * (screenW - width - 80f);
            float top = screenH * 0.15f + rnd.nextFloat() * (screenH * 0.40f);

            RectF newBoxRect = new RectF(left, top, left + width, top + boxHeight);

            if (isValidBoxPosition(newBoxRect)) {
                boxes.add(new Box(newBoxRect, hp, xpReward, scoreReward));
                return true;
            }
        }

        return false;
    }

    private boolean isValidBoxPosition(RectF newBox) {
        // Check existing boxes
        for (Box existing : boxes) {
            if (RectF.intersects(newBox, existing.rect)) {
                return false;
            }
        }

        // Check bumpers
        for (Bumper bumper : bumperSystem.getBumpers()) {
            if (bumper != null && bumper.rect != null) {
                if (RectF.intersects(newBox, bumper.rect)) {
                    return false;
                }
            }
        }

        // Check cat zone
        RectF catZone = new RectF(
                catX - 60f,
                catY - 100f,
                catX + catW + 60f,
                catY + catH + 60f
        );
        if (RectF.intersects(newBox, catZone)) {
            return false;
        }

        // Check balls
        for (Ball ball : balls) {
            RectF ballRect = new RectF(ball.x, ball.y, ball.x + ball.r, ball.y + ball.r);
            if (RectF.intersects(newBox, ballRect)) {
                return false;
            }
        }

        return true;
    }

    // === POPUPS ===

    private void spawnPopup(String txt, float x, float y) {
        Paint pp = new Paint(pPopup);
        pp.setTextSize(GameConfig.POPUP_TEXT_SIZE);
        pp.setTextAlign(Paint.Align.CENTER);
        popups.add(new Popup(txt, x, y, System.currentTimeMillis(), GameConfig.POPUP_LIFETIME, pp));
    }

    private void updatePopups() {
        Iterator<Popup> it = popups.iterator();
        long now = System.currentTimeMillis();

        while (it.hasNext()) {
            Popup p = it.next();
            float age = now - p.born;

            if (age > p.life) {
                it.remove();
                continue;
            }

            float lifePct = age / (float)p.life;
            p.y += p.vy * (1f - lifePct * 0.5f);
            p.scale = 1f + (GameConfig.POPUP_SCALE_MAX - 1f) * (1f - lifePct);
            p.p.setAlpha((int)(255 * (1f - lifePct)));
        }
    }

    // === RENDERING ===

    private void render() {
        if (!holder.getSurface().isValid()) return;

        Canvas c = holder.lockCanvas();
        if (c == null) return;

        // Clear screen
        c.drawColor(0xFF000000);

        // Draw game elements
        if (state == State.TITLE) {
            drawTitleScreen(c);
        } else {
            drawGameArea(c);
        }

        // Draw popups last (always on top)
        drawPopups(c);

        holder.unlockCanvasAndPost(c);
    }

    private void drawTitleScreen(Canvas c) {
        // Draw title text
        Paint titlePaint = new Paint();
        titlePaint.setColor(Color.WHITE);
        titlePaint.setTextSize(48f);
        titlePaint.setTextAlign(Paint.Align.CENTER);
        titlePaint.setTypeface(Typeface.DEFAULT_BOLD);

        c.drawText("WallPAWng", screenW / 2f, screenH * 0.3f, titlePaint);

        // Draw play hint
        Paint hintPaint = new Paint();
        hintPaint.setColor(Color.LTGRAY);
        hintPaint.setTextSize(18f);
        hintPaint.setTextAlign(Paint.Align.CENTER);

        c.drawText("Tap to Start", screenW / 2f, screenH * 0.5f, hintPaint);
    }

   // === GAME ELEMENTS ===
    private void drawGameArea(Canvas c) {
        // 1. Draw portals (behind everything else)
        drawPortals(c);

        // 2. Draw black hole
        drawBlackHole(c);

        // 3. Draw bumpers
        drawBumpers(c);

        // 4. Draw boxes
        drawBoxes(c);

        // 5. Draw balls
        drawBalls(c);

        // 6. Draw cat (on top of balls)
        drawCat(c);
    }

    private void drawPortals(Canvas c) {
        PortalSystem.Portal pA = portalSystem.getA();
        PortalSystem.Portal pB = portalSystem.getB();

        if (pA == null || pB == null) return;

        long now = System.currentTimeMillis();
        long age = now - pA.spawnMs;
        float agePct = age / (float)pA.durationMs;

        // Pulse animation
        float pulse = 1f + 0.15f * (float)Math.sin(age * 0.005f);

        int alpha = 255;
        if (agePct < 0.2f) {
            alpha = (int)(255 * (agePct / 0.2f));
        } else if (agePct > 0.8f) {
            alpha = (int)(255 * ((1f - agePct) / 0.2f));
        }

        // Draw both portals
        drawSinglePortal(c, pA, pulse, alpha, 0xFF00FFFF); // Cyan
        drawSinglePortal(c, pB, pulse, alpha, 0xFFFF00FF); // Magenta
    }

    private void drawSinglePortal(Canvas c, PortalSystem.Portal portal, float pulse, int alpha, int color) {
        float cx = portal.rect.centerX();
        float cy = portal.rect.centerY();
        float w = portal.rect.width() * pulse;
        float h = portal.rect.height() * pulse;

        // Use sprite if available
        if (portalBitmap != null) {
            Paint p = new Paint();
            p.setAlpha(alpha);

            c.save();
            c.translate(cx, cy);
            c.scale(pulse, pulse);
            c.drawBitmap(
                    portalBitmap,
                    -w / 2f,
                    -h / 2f,
                    p
            );
            c.restore();
        } else {
            Paint p = new Paint();
            p.setAlpha(alpha);
            RadialGradient gradient = new RadialGradient(
                    cx, cy,
                    Math.max(w, h) / 2f,
                    new int[]{color, Color.TRANSPARENT},
                    new float[]{0.3f, 1f},
                    Shader.TileMode.CLAMP
            );
            p.setShader(gradient);
            c.drawOval(
                    cx - w / 2f,
                    cy - h / 2f,
                    cx + w / 2f,
                    cy + h / 2f,
                    p
            );
            p.setShader(null);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(4f * pulse);
            p.setColor(color);
            p.setAlpha(alpha);
            c.drawOval(
                    cx - w / 2f * 0.7f,
                    cy - h / 2f * 0.7f,
                    cx + w / 2f * 0.7f,
                    cy + h / 2f * 0.7f,
                    p
            );
        }
    }

    private void drawBlackHole(Canvas c) {
        BlackHoleSystem.BlackHole bh = blackHoleSystem.get();
        if (bh == null) return;

        long now = System.currentTimeMillis();
        long age = now - bh.spawnMs;
        float agePct = age / (float)bh.durationMs;

        // Rotation animation
        float rotation = (age * GameConfig.BLACKHOLE_ROTATION_SPEED) % 360f;

        // Pulse animation
        float pulse = 1f + 0.1f * (float)Math.sin(age * 0.003f);

        int alpha = 255;
        if (agePct < 0.15f) {
            alpha = (int)(255 * (agePct / 0.15f));
        } else if (agePct > 0.85f) {
            alpha = (int)(255 * ((1f - agePct) / 0.15f));
        }

        float r = bh.r * pulse;

        // Use sprite if available
        if (blackHoleBitmap != null) {
            Paint p = new Paint();
            p.setAlpha(alpha);

            c.save();
            c.translate(bh.x, bh.y);
            c.rotate(rotation);
            c.scale(pulse, pulse);
            c.drawBitmap(
                    blackHoleBitmap,
                    -r,
                    -r,
                    p
            );
            c.restore();
        } else {
            Paint p = new Paint();
            p.setAlpha(alpha);

            RadialGradient outerGradient = new RadialGradient(
                    bh.x, bh.y,
                    r * 1.5f,
                    new int[]{0xFF4A0080, Color.TRANSPARENT},
                    new float[]{0f, 1f},
                    Shader.TileMode.CLAMP
            );
            p.setShader(outerGradient);
            c.drawCircle(bh.x, bh.y, r * 1.5f, p);

            p.setShader(null);
            p.setStyle(Paint.Style.FILL);
            p.setColor(0xFF000000);
            p.setAlpha(alpha);
            c.drawCircle(bh.x, bh.y, r * 0.6f, p);

            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(3f);
            p.setColor(0xFF8B00FF);
            p.setAlpha(alpha);
            c.drawCircle(bh.x, bh.y, r * 0.9f, p);

            c.save();
            c.rotate(rotation, bh.x, bh.y);
            for (int i = 0; i < 8; i++) {
                float angle = i * 45f;
                float rad = (float)Math.toRadians(angle);
                float startX = bh.x + (float)Math.cos(rad) * r * 1.2f;
                float startY = bh.y + (float)Math.sin(rad) * r * 1.2f;
                float endX = bh.x + (float)Math.cos(rad) * r * 0.9f;
                float endY = bh.y + (float)Math.sin(rad) * r * 0.9f;

                p.setStrokeWidth(2f);
                p.setAlpha(alpha / 2);
                c.drawLine(startX, startY, endX, endY, p);
            }
            c.restore();
        }
    }

    private void drawBumpers(Canvas c) {
        // Log the number of bumpers being drawn for debugging
        if (bumperSystem != null && bumperSystem.getBumpers() != null) {
            android.util.Log.d("GameView", "Drawing " + bumperSystem.getBumpers().size() + " bumpers.");
        }

        for (Bumper bumper : bumperSystem.getBumpers()) {
            if (bumper == null || bumper.rect == null) continue;

            c.save();

            // Rotate if needed
            if (bumper.rotates) {
                long age = System.currentTimeMillis() - bumper.spawnAtMs;
                float rotation = (age * 0.05f) % 360f;
                c.rotate(rotation, bumper.rect.centerX(), bumper.rect.centerY());
            }

            c.drawRect(bumper.rect, pBumper);
            c.restore();
        }
    }

    private void drawBoxes(Canvas c) {
        Paint pBoxDynamic = new Paint(pBox);
        Paint pText = new Paint();
        pText.setColor(Color.WHITE);
        pText.setTextSize(18f);
        pText.setTextAlign(Paint.Align.CENTER);
        pText.setStyle(Paint.Style.FILL);

        for (Box box : boxes) {
            // Color based on HP
            pBoxDynamic.setColor(getBoxColor(box.hp));
            c.drawRect(box.rect, pBoxDynamic);

            // Draw HP text
            c.drawText(
                    String.valueOf(box.hp),
                    box.rect.centerX(),
                    box.rect.centerY() + 6f,
                    pText
            );
        }
    }

    private int getBoxColor(int hp) {
        if (hp >= 5) return 0xFFFF6B6B;      // Strong red
        else if (hp >= 3) return 0xFFC88C78; // Medium brown
        else return 0xFFE8B896;              // Light tan
    }

    private void drawBalls(Canvas c) {
        for (Ball ball : balls) {
            if (cachedYarnBitmap != null) {
                float targetSize = ball.r;

                if (Math.abs(cachedYarnSize - targetSize) < 2f) {
                    c.drawBitmap(cachedYarnBitmap, ball.x, ball.y, pBall);
                } else {
                    // Different size (small balls) - scale on demand
                    Bitmap scaled = Bitmap.createScaledBitmap(
                            yarnBitmap,
                            (int)ball.r,
                            (int)ball.r,
                            true
                    );
                    c.drawBitmap(scaled, ball.x, ball.y, pBall);
                }
            } else {
                // No sprite - draw circle
                c.drawOval(ball.x, ball.y, ball.x + ball.r, ball.y + ball.r, pBall);
            }

            // Draw indicator for small balls
            if (ball.isSmall) {
                Paint pSmall = new Paint();
                pSmall.setColor(0x88FFFF00); // Semi-transparent yellow
                pSmall.setStyle(Paint.Style.STROKE);
                pSmall.setStrokeWidth(2f);
                c.drawCircle(ball.centerX(), ball.centerY(), ball.r / 2f, pSmall);
            }
        }
    }

    private void drawCat(Canvas c) {
        if (cachedCatBitmap != null) {
            c.drawBitmap(cachedCatBitmap, catX, catY, pCat);
        } else if (catBitmap != null) {
            // Cache not ready - try to create it
            updateCachedCatBitmap();
            if (cachedCatBitmap != null) {
                c.drawBitmap(cachedCatBitmap, catX, catY, pCat);
            } else {
                // Caching failed - draw rectangle
                c.drawRect(catX, catY, catX + catW, catY + catH, pCat);
            }
        } else {
            // No sprite - draw rectangle
            c.drawRect(catX, catY, catX + catW, catY + catH, pCat);
        }
    }

    private void drawPopups(Canvas c) {
        for (Popup p : popups) {
            c.save();
            c.translate(p.x, p.y);
            c.scale(p.scale, p.scale);
            c.drawText(p.txt, 0, 0, p.p);
            c.restore();
        }
    }

    // === PUBLIC API ===

    public void start() {
        if (thread == null || !thread.isAlive()) {
            running = true;
            thread = new Thread(this);
            thread.start();
        }
    }

    public void stop() {
        running = false;
        try {
            if (thread != null) {
                thread.join();
            }
        } catch (InterruptedException e) {
            // Ignore
        }
    }

    public void setStateToPlaying() {
        state = State.PLAYING;

        // Start background music
        if (backgroundMusicPlayer != null && !backgroundMusicPlayer.isPlaying()) {
            backgroundMusicPlayer.start();
        }

        // Stop title music
        if (titleMusicPlayer != null && titleMusicPlayer.isPlaying()) {
            titleMusicPlayer.pause();
            titleMusicPlayer.seekTo(0);
        }
    }

    public void clearGameObjects() {
        // Clear all lists of objects
        balls.clear();
        boxes.clear();
        popups.clear();

        // Reset subsystems
        bumperSystem.clearAllBumpers();
        portalSystem.clearPortals();
        blackHoleSystem.clearBlackHole();
    }

    public void setStateToTitle() {
        state = State.TITLE;

        // Start title music
        if (titleMusicPlayer != null && !titleMusicPlayer.isPlaying()) {
            titleMusicPlayer.start();
        }

        // Stop background music
        if (backgroundMusicPlayer != null && backgroundMusicPlayer.isPlaying()) {
            backgroundMusicPlayer.pause();
            backgroundMusicPlayer.seekTo(0);
        }
    }

    public void spawnInitialBall() {
        balls.clear();
        float r = Math.max(10f, screenW * ballSizePercent);
        float x = (screenW - r) / 2f;
        float y = (screenH - r) / 2f;
        balls.add(new Ball(x, y, r, randomVX(), startingVY));
    }

    public void setLevelUpListener(LevelUpListener listener) {
        this.levelUpListener = listener;
    }

    public void setGameOverListener(GameOverListener listener) {
        this.gameOverListener = listener;
    }

    public void applyUpgradeDirect(String key) {
        if (key == null || key.isEmpty()) return;

        gs.unlockUpgrade(key);

        switch (key) {
            case "score_x2":
                spawnPopup("Score Doubled!", screenW / 2f, screenH / 2f);
                break;

            case "combo_plus1":
                spawnPopup("Combo XP Increased!", screenW / 2f, screenH / 2f);
                break;

            case "multi_full_burst":
                spawnExtraYarn(); // Spawns one extra
                spawnExtraYarn(); // Spawns a second one
                spawnPopup("+2 Full Yarn Balls!", screenW / 2f, screenH / 2f);
                break;

            case "box_reward_up":
                spawnPopup("Box Rewards Up!", screenW / 2f, screenH / 2f);
                break;

            case "blackhole_box_destroyer":
                spawnPopup("Black Holes Destroy Boxes!", screenW / 2f, screenH / 2f);
                break;

            case "max_stress_plus20":
                gs.addMaxStress(GameConfig.UPGRADE_MAX_STRESS_INCREASE);
                spawnPopup("Max Stress +20!", screenW / 2f, screenH / 2f);
                break;

            case "stress_reducer":
                spawnPopup("Catches Reduce More Stress!", screenW / 2f, screenH / 2f);
                break;

            case "cat_width_plus":
                catW *= GameConfig.UPGRADE_CAT_WIDTH_INCREASE;
                catW = Math.min(catW, screenW * GameConfig.CAT_MAX_WIDTH_FRACTION);
                catX = (screenW - catW) / 2f;
                updateCachedCatBitmap();
                spawnPopup("Cat Wider!", screenW / 2f, screenH / 2f);
                break;

            case "extra_yarn":
                spawnExtraYarn();
                spawnPopup("+1 Yarn Ball!", screenW / 2f, screenH / 2f);
                break;

            case "portal_freq_plus":
                spawnPopup("Portals More Frequent!", screenW / 2f, screenH / 2f);
                break;

            case "cat_reflect":
                spawnPopup("Cat Can Save a Miss!", screenW / 2f, screenH / 2f);
                break;

            case "vy_plus":
                for (Ball b : balls) b.vy *= GameConfig.UPGRADE_SPEED_INCREASE;
                startingVY *= GameConfig.UPGRADE_SPEED_INCREASE;
                spawnPopup("Vertical Speed Up!", screenW / 2f, screenH / 2f);
                break;

            case "vx_plus":
                for (Ball b : balls) b.vx *= GameConfig.UPGRADE_SPEED_INCREASE;
                minVX *= GameConfig.UPGRADE_SPEED_INCREASE;
                maxVX *= GameConfig.UPGRADE_SPEED_INCREASE;
                spawnPopup("Horizontal Speed Up!", screenW / 2f, screenH / 2f);
                break;

            default:
                spawnPopup("Upgrade Applied!", screenW / 2f, screenH / 2f);
                break;
        }
    }

    private void spawnExtraYarn() {
        float r = Math.max(10f, screenW * ballSizePercent);
        Ball newBall = new Ball(
                catX + catW / 2f - r / 2f,
                catY - r - 4f,
                r,
                randomVX(),
                -Math.abs(startingVY),
                false
        );
        balls.add(newBall);
    }

    private float randomVX() {
        float v = minVX + rnd.nextFloat() * (maxVX - minVX);
        return rnd.nextBoolean() ? v : -v;
    }
}