package com.legacyvisualfix.combat;

import java.util.ArrayDeque;
import java.util.Deque;

/** Per-thread observations only. Never mutates entity state. */
public final class DamageTransactions {

    private final Deque<Scope> scopes = new ArrayDeque<>();

    public Scope begin(Object target, float health, float absorbed) {
        Scope scope = new Scope(target, health, absorbed);
        scopes.push(scope);
        return scope;
    }

    public boolean active() {
        return !scopes.isEmpty();
    }

    public Result finish(Scope scope, float health, float absorbed, boolean completed) {
        if (scopes.peek() != scope) throw new IllegalStateException("Unbalanced damage observation");
        scopes.pop();
        float rawHealth = loss(scope.health, health);
        float rawAbsorbed = loss(scope.absorbed, absorbed);
        // Only the nearest matching ancestor receives this subtree's total.
        for (Scope parent : scopes) {
            if (parent.target == scope.target) {
                parent.childHealth += rawHealth;
                parent.childAbsorbed += rawAbsorbed;
                break;
            }
        }
        return completed
            ? new Result(Math.max(0, rawHealth - scope.childHealth), Math.max(0, rawAbsorbed - scope.childAbsorbed))
            : new Result(0, 0);
    }

    private static float loss(float before, float after) {
        if (!Float.isFinite(before) || !Float.isFinite(after)) return 0;
        return Math.max(0, Math.max(0, before) - Math.max(0, after));
    }

    public static final class Scope {

        private final Object target;
        private final float health, absorbed;
        private float childHealth, childAbsorbed;

        private Scope(Object target, float health, float absorbed) {
            this.target = target;
            this.health = health;
            this.absorbed = absorbed;
        }
    }

    public static final class Result {

        public final float health, absorbed;

        private Result(float health, float absorbed) {
            this.health = health;
            this.absorbed = absorbed;
        }

        public boolean hasDamage() {
            return Float.isFinite(health) && Float.isFinite(absorbed) && (health > 0 || absorbed > 0);
        }
    }
}
