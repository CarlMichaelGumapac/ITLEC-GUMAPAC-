package com.rngym.myapplication;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class UpgradeManager {
    public static class Choice {
        public final String key;
        public final String title;
        public final String desc;
        public int iconRes;

        public Choice(String k, String t, String d) {
            key=k;
            title=t;
            desc=d;
        }
    }

    private final Random rnd = new Random();

    public Choice[] generate(int n) {
        List<Choice> pool = new ArrayList<>();

        // === OFFENSIVE / SCORING ===
        pool.add(new Choice("score_x2","Score ×2","Doubles score per catch."));
        pool.add(new Choice("combo_plus1","Combo Master","Increases combo XP bonus by 0.5 per stack."));
        pool.add(new Choice("multi_full_burst","Multiball","Spawns 2 extra full-sized yarn balls."));
        pool.add(new Choice("box_reward_up","Riches","Boxes give 50% more XP and score on break."));
        pool.add(new Choice("blackhole_box_destroyer","Singularity","Black holes now instantly destroy boxes they touch."));

        // === DEFENSE / UTILITY ===
        pool.add(new Choice("max_stress_plus20","Zen Master","Increases max stress by 20 (cap 200)."));
        pool.add(new Choice("stress_reducer","Calm Cat","Catching a ball reduces 5 extra stress."));
        pool.add(new Choice("cat_width_plus","Wide Load","Cat is 12% wider (capped at 50% screen)."));
        pool.add(new Choice("extra_yarn","+1 Yarn","Spawns an extra yarn ball at the cat immediately."));
        pool.add(new Choice("portal_freq_plus","Portal Network","Portals appear 50% more frequently."));
        pool.add(new Choice("cat_reflect","Save","Once per level, the cat can reflect a missed ball back into play."));

        // === SPEED ===
        pool.add(new Choice("vy_plus","Velocity Up","Balls move 10% faster vertically."));
        pool.add(new Choice("vx_plus","Velocity Up","Balls move 10% faster horizontally."));

        Choice[] out = new Choice[n];
        for (int i=0; i<n; i++) {
            if (pool.isEmpty()) break;
            int index = rnd.nextInt(pool.size());
            out[i] = pool.remove(index);
        }
        return out;
    }
}