package com.mattmx.nametags.utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * Entity-thread helpers over Paper's region schedulers.
 * <p>
 * On Folia every entity is ticked by the thread that owns its region, so work that reads an
 * entity's live state (tracking, passengers, effects) or mutates our per-entity tag has to run
 * there. On Paper and Purpur the same calls resolve to the main thread, so one code path serves
 * both: {@link Bukkit#isOwnedByCurrentRegion(Entity)} is "on the main thread" and an entity
 * scheduler task is a main-thread task on the next tick.
 */
public final class Schedulers {

    private Schedulers() {
    }

    /**
     * Runs {@code task} right away when the current thread owns {@code entity}, otherwise on the
     * entity's next tick. Returns false when the entity has already been removed and the task
     * will never run.
     */
    public static boolean runOn(@NotNull Plugin plugin, @NotNull Entity entity, @NotNull Runnable task) {
        if (Bukkit.isOwnedByCurrentRegion(entity)) {
            task.run();
            return true;
        }
        return nextTick(plugin, entity, task);
    }

    /**
     * Runs {@code task} on the entity's next tick, on the thread that owns it. Returns false when
     * the entity has already been removed and the task will never run.
     */
    public static boolean nextTick(@NotNull Plugin plugin, @NotNull Entity entity, @NotNull Runnable task) {
        return later(plugin, entity, task, 1L);
    }

    /**
     * Runs {@code task} after {@code delayTicks} of the entity's ticks, on the thread that owns
     * it. Returns false when the entity has already been removed and the task will never run.
     */
    public static boolean later(@NotNull Plugin plugin, @NotNull Entity entity, @NotNull Runnable task, long delayTicks) {
        return entity.getScheduler().runDelayed(plugin, (scheduled) -> task.run(), null, Math.max(1L, delayTicks)) != null;
    }
}
