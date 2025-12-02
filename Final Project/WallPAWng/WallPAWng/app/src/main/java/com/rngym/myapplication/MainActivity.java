package com.rngym.myapplication;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;

import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.cardview.widget.CardView;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private GameView gameView;
    private View titleOverlay, settingsOverlay, upgradeOverlay, gameOverOverlay;
    private Button btnPlay, btnSettings, btnCredits;
    private Button btnSettingsBack;
    private CheckBox checkVibration;
    private SeekBar seekSound;
    private CardView card1, card2, card3;
    private TextView title1, title2, title3, desc1, desc2, desc3;
    private TextView tvLevel, tvScore, tvCombo;
    private LinearLayout expBarContainer, stressBarContainer;
    private TextView tvExpText, tvStressText, tvExpLabel, tvStressLabel;
    private TextView tvFinalScore;
    private Button btnRestart;

    private SharedPreferences prefs;
    private static final String PREFS_NAME = "game_prefs";
    private static final String KEY_VIBRATION = "vibration_enabled";
    private static final String KEY_SOUND_VOLUME = "sound_volume";

    private Handler hudUpdateHandler = new Handler();
    private Runnable hudUpdateRunnable;

    private Bitmap expSegmentBitmap;
    private Bitmap stressSegmentBitmap;
    private static final int MAX_SEGMENTS = 10;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        GameState.get().init(this);
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        loadProgressSegmentImages();

        if (!initializeViews()) {
            Toast.makeText(this, "Critical error: UI elements missing", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        setupListeners();
        loadSettings();
        startHUDUpdateLoop();
        GameState.get().setPaused(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (gameView != null && !GameState.get().isPaused()) {
            gameView.start();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (gameView != null) {
            gameView.stop();
        }
        saveSettings();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (gameView != null) {
            gameView.stop();
        }
        if (hudUpdateHandler != null && hudUpdateRunnable != null) {
            hudUpdateHandler.removeCallbacks(hudUpdateRunnable);
        }
    }

    private void loadProgressSegmentImages() {
        expSegmentBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.progress_exp);
        stressSegmentBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.progress_stress);
    }

    private boolean initializeViews() {
        gameView = findViewById(R.id.gameView);
        if (gameView == null) return false;

        titleOverlay = findViewById(R.id.titleOverlay);
        settingsOverlay = findViewById(R.id.settingsOverlay);
        upgradeOverlay = findViewById(R.id.upgradeOverlay);
        gameOverOverlay = findViewById(R.id.gameOverOverlay);

        if (titleOverlay == null || settingsOverlay == null || upgradeOverlay == null || gameOverOverlay == null) return false;

        btnPlay = findViewById(R.id.btnPlay);
        btnSettings = findViewById(R.id.btnSettings);
        btnCredits = findViewById(R.id.btnCredits);
        if (btnPlay == null) return false;

        btnSettingsBack = findViewById(R.id.btnSettingsBack);
        checkVibration = findViewById(R.id.checkVibration);
        seekSound = findViewById(R.id.seekSound);

        card1 = findViewById(R.id.upgradeCard1);
        card2 = findViewById(R.id.upgradeCard2);
        card3 = findViewById(R.id.upgradeCard3);
        title1 = findViewById(R.id.upgradeTitle1);
        title2 = findViewById(R.id.upgradeTitle2);
        title3 = findViewById(R.id.upgradeTitle3);
        desc1 = findViewById(R.id.upgradeDesc1);
        desc2 = findViewById(R.id.upgradeDesc2);
        desc3 = findViewById(R.id.upgradeDesc3);

        if (card1 == null || card2 == null || card3 == null) return false;

        tvLevel = findViewById(R.id.tvLevel);
        tvScore = findViewById(R.id.tvScore);
        tvCombo = findViewById(R.id.tvCombo);

        expBarContainer = findViewById(R.id.expBarContainer);
        stressBarContainer = findViewById(R.id.stressBarContainer);
        tvExpText = findViewById(R.id.tvExpText);
        tvStressText = findViewById(R.id.tvStressText);
        tvExpLabel = findViewById(R.id.tvExpLabel);
        tvStressLabel = findViewById(R.id.tvStressLabel);

        tvFinalScore = findViewById(R.id.tvFinalScore);
        btnRestart = findViewById(R.id.btnRestart);

        // if (btnRestart == null || tvFinalScore == null) return false;
        //
        //        return true;

        return btnRestart != null && tvFinalScore != null;
    }

    private void updateProgressBars() {
        GameState gs = GameState.get();
        updateExpBar(gs);
        updateStressBar(gs);
    }

    private void updateExpBar(GameState gs) {
        if (expBarContainer == null || expSegmentBitmap == null) return;

        int currentXP = gs.getXP();
        int level = gs.getLevel();
        int xpForNextLevel = GameConfig.xpForLevel(level);
        float xpPercent = Math.min(1f, Math.max(0f, (float) currentXP / xpForNextLevel));

        int segmentsToShow = Math.round(xpPercent * MAX_SEGMENTS);

        expBarContainer.removeAllViews();

        for (int i = 0; i < MAX_SEGMENTS; i++) {
            ImageView segment = new ImageView(this);
            segment.setLayoutParams(new LinearLayout.LayoutParams(40, 30));

            if (i < segmentsToShow) {
                segment.setImageBitmap(expSegmentBitmap);
                segment.setAlpha(1.0f);
            } else {
                segment.setImageBitmap(expSegmentBitmap);
                segment.setAlpha(0.2f);
            }

            expBarContainer.addView(segment, 0);
        }

        if (tvExpText != null) {
            tvExpText.setText(String.format("%d/%d", currentXP, xpForNextLevel));
        }
    }

    private void updateStressBar(GameState gs) {
        if (stressBarContainer == null || stressSegmentBitmap == null) return;

        float currentStress = gs.getStress();
        float maxStress = gs.getMaxStress();
        float stressPercent = Math.min(1f, Math.max(0f, currentStress / maxStress));

        int segmentsToShow = Math.round(stressPercent * MAX_SEGMENTS);

        stressBarContainer.removeAllViews();

        for (int i = 0; i < MAX_SEGMENTS; i++) {
            ImageView segment = new ImageView(this);
            segment.setLayoutParams(new LinearLayout.LayoutParams(40, 30));

            if (i < segmentsToShow) {
                segment.setImageBitmap(stressSegmentBitmap);
                segment.setAlpha(1.0f);
            } else {
                segment.setImageBitmap(stressSegmentBitmap);
                segment.setAlpha(0.2f);
            }

            stressBarContainer.addView(segment, 0);
        }

        if (tvStressText != null) {
            tvStressText.setText(String.format("%.0f/%.0f", currentStress, maxStress));
        }

//        if (currentStress >= maxStress && !gs.isPaused()) {
//            android.util.Log.e("MainActivity", "GAME OVER: Stress maxed!");
//            triggerGameOver();
//        }
    }

    private void setupListeners() {
        btnPlay.setOnClickListener(v -> startNewGame());

        if (btnSettings != null) {
            btnSettings.setOnClickListener(v -> openSettings());
        }

        if (btnCredits != null) {
            btnCredits.setOnClickListener(v -> showCredits());
        }

        if (btnSettingsBack != null) {
            btnSettingsBack.setOnClickListener(v -> closeSettings());
        }

        btnRestart.setOnClickListener(v -> restartGame());

        gameView.setLevelUpListener((level, xp, choices) -> runOnUiThread(() -> {
            if (choices == null || choices.length == 0) {
                GameState.get().setPaused(false);
                return;
            }
            showUpgradeOverlay(choices);
        }));

        gameView.setGameOverListener((finalScore, highScore) -> runOnUiThread(() -> {
            showGameOver(finalScore);
        }));
    }

    private void startNewGame() {
        titleOverlay.setVisibility(View.GONE);
        gameOverOverlay.setVisibility(View.GONE);

        GameState.get().resetRun();
        GameState.get().setPaused(false);

        gameView.resetGameOverFlag();

        gameView.spawnInitialBall();
        gameView.setStateToPlaying();
        gameView.start();
    }

    private void restartGame() {

        gameOverOverlay.setVisibility(View.GONE);

        GameState.get().resetRun();
        GameState.get().setPaused(false);

        gameView.clearGameObjects();
        gameView.resetGameOverFlag();


        gameView.spawnInitialBall();
        gameView.setStateToPlaying();
        gameView.start();
    }

    private void showGameOver(int finalScore) {
        // GameState.get().maybeUpdateHighScore();

        if (tvFinalScore != null) {
            String scoreText = "Final Score: " + finalScore + "\n" +
                    "High Score: " + GameState.get().getHighScore() + "\n" +
                    "Max Combo: " + GameState.get().getMaxCombo();
            tvFinalScore.setText(scoreText);
        }

        runOnUiThread(() -> {
            if (titleOverlay != null) titleOverlay.setVisibility(View.GONE);
            if (upgradeOverlay != null) upgradeOverlay.setVisibility(View.GONE);
            if (settingsOverlay != null) settingsOverlay.setVisibility(View.GONE);

            // Show game over overlay
            if (gameOverOverlay != null) {
                gameOverOverlay.setVisibility(View.VISIBLE);
                gameOverOverlay.bringToFront();
            }
        });

        GameState.get().setPaused(true);
    }

    private void triggerGameOver() {
        if (GameState.get().isPaused()) return;

        GameState.get().setPaused(true);
        int finalScore = GameState.get().getScore();
        int highScore = GameState.get().getHighScore();

        runOnUiThread(() -> showGameOver(finalScore));
    }

    private void showUpgradeOverlay(UpgradeManager.Choice[] choices) {
        if (choices.length > 0) bindUpgradeCard(card1, title1, desc1, choices[0]);
        if (choices.length > 1) bindUpgradeCard(card2, title2, desc2, choices[1]);
        if (choices.length > 2) bindUpgradeCard(card3, title3, desc3, choices[2]);

        upgradeOverlay.setVisibility(View.VISIBLE);
    }

    private void bindUpgradeCard(CardView card, TextView titleView, TextView descView, UpgradeManager.Choice choice) {
        if (card == null || choice == null) return;

        if (titleView != null) {
            titleView.setText(choice.title != null ? choice.title : "Upgrade");
            titleView.setTextColor(Color.WHITE);
        }

        if (descView != null) {
            descView.setText(choice.desc != null ? choice.desc : "");
            descView.setTextColor(Color.WHITE);
        }

        card.setOnClickListener(v -> {
            gameView.applyUpgradeDirect(choice.key);
            upgradeOverlay.setVisibility(View.GONE);
            GameState.get().setPaused(false);
            gameView.start();
        });
    }

    private void openSettings() {
        settingsOverlay.setVisibility(View.VISIBLE);
    }

    private void closeSettings() {
        saveSettings();
        settingsOverlay.setVisibility(View.GONE);
    }

    private void loadSettings() {
        if (checkVibration != null) {
            checkVibration.setChecked(prefs.getBoolean(KEY_VIBRATION, true));
        }
        if (seekSound != null) {
            seekSound.setProgress(prefs.getInt(KEY_SOUND_VOLUME, 80));
        }
    }

    private void saveSettings() {
        SharedPreferences.Editor editor = prefs.edit();
        if (checkVibration != null) {
            editor.putBoolean(KEY_VIBRATION, checkVibration.isChecked());
        }
        if (seekSound != null) {
            editor.putInt(KEY_SOUND_VOLUME, seekSound.getProgress());
        }
        editor.apply();
    }

    private void showCredits() {
        Toast.makeText(this, "WallPAWng\nDeveloped by: Group 1\nVersion: 2.0", Toast.LENGTH_LONG).show();
    }

    private void startHUDUpdateLoop() {
        hudUpdateRunnable = new Runnable() {
            @Override
            public void run() {
                updateHUD();
                hudUpdateHandler.postDelayed(this, 100);
            }
        };
        hudUpdateHandler.post(hudUpdateRunnable);
    }

    private void updateHUD() {
        GameState gs = GameState.get();

        if (tvLevel != null) {
            tvLevel.setText("LVL " + gs.getLevel());
            tvLevel.setAlpha(0.3f);
        }

        if (tvScore != null) {
            tvScore.setText(String.valueOf(gs.getScore()));
            tvScore.setAlpha(0.3f);
        }

        if (tvCombo != null) {
            int combo = gs.getCombo();

            if (combo > 0) {
                float multiplier = gs.getComboMultiplier();
                String comboText = (multiplier > 1.0f) ?
                        "x" + combo + " (" + String.format("%.1f", multiplier) + "x)" :
                        "x" + combo;

                tvCombo.setText(comboText);
                tvCombo.setVisibility(View.VISIBLE);

                if (combo >= 20) tvCombo.setTextColor(0xFFFF00FF);
                else if (combo >= 10) tvCombo.setTextColor(0xFFFF6B6B);
                else if (combo >= 5) tvCombo.setTextColor(0xFFFFD700);
                else tvCombo.setTextColor(0xFFFFFFFF);

                if (gs.isComboExpiring()) {
                    long timeSince = gs.getTimeSinceLastCatch();
                    float alpha = 0.5f + 0.5f * (float)Math.sin(timeSince * 0.01f);
                    tvCombo.setAlpha(alpha);
                } else {
                    tvCombo.setAlpha(0.3f);
                }
            } else {
                tvCombo.setVisibility(View.INVISIBLE);
            }
        }

        updateProgressBars();
    }
}