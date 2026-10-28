package com.agristep.ui;

import javax.swing.JComponent;
import javax.swing.Timer;
import java.awt.GraphicsEnvironment;
import java.awt.event.HierarchyEvent;

/**
 * Small animations: hover fades, press effects, screen fade-ins, drifting clouds.
 * Motion is skipped (everything jumps straight to its final state) when the window isn't showing, in tests,
 * and when the person turns it off with {@code -Dplantwise.reduceMotion=true} or the environment variable
 * {@code PLANTWISE_REDUCE_MOTION=1}.
 */
final class Motion {
    private static final long T0 = System.nanoTime();

    private Motion() {}

    static boolean enabled() {
        if (GraphicsEnvironment.isHeadless()) return false;
        if (Boolean.getBoolean("plantwise.reduceMotion")) return false;
        return !"1".equals(System.getenv("PLANTWISE_REDUCE_MOTION"));
    }

    /** Seconds since start, used to drift clouds and rain; 0 when motion is off so pictures stay still. */
    static float seconds() {
        return enabled() ? (System.nanoTime() - T0) / 1_000_000_000f : 0f;
    }

    /** Repaints a component about 20 times a second, but only while it is on screen. */
    static void ambient(JComponent c) {
        Timer timer = new Timer(50, e -> c.repaint());
        c.addHierarchyListener(e -> {
            if ((e.getChangeFlags() & (HierarchyEvent.SHOWING_CHANGED | HierarchyEvent.DISPLAYABILITY_CHANGED)) == 0) return;
            if (enabled() && c.isShowing()) timer.start();
            else timer.stop();
        });
    }

    /** A number that eases from one value to another (for hover glow, press, fade-in). */
    static final class Tween {
        private final JComponent owner;
        private final int millis;
        private final Timer timer;
        private float value;
        private float from;
        private float target;
        private long started;

        Tween(JComponent owner, int millis) {
            this.owner = owner;
            this.millis = Math.max(1, millis);
            this.timer = new Timer(16, e -> tick());
        }

        float value() { return value; }

        /** Eases to {@code to}; jumps there at once when motion is off or the component isn't showing. */
        void to(float to) {
            if (to == target && (value == to || timer.isRunning())) return;
            target = to;
            if (!enabled() || !owner.isShowing()) {
                timer.stop();
                value = to;
                owner.repaint();
                return;
            }
            from = value;
            started = System.nanoTime();
            timer.start();
        }

        /** Sets the value without animating. */
        void jump(float v) {
            timer.stop();
            value = v;
            target = v;
        }

        private void tick() {
            float t = Math.min(1f, (System.nanoTime() - started) / (millis * 1_000_000f));
            float eased = 1f - (1f - t) * (1f - t) * (1f - t);   // ease out
            value = from + (target - from) * eased;
            owner.repaint();
            if (t >= 1f) {
                value = target;
                timer.stop();
            }
        }
    }
}
