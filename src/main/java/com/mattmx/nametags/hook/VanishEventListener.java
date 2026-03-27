package com.mattmx.nametags.hook;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.mattmx.nametags.NameTags;
import com.mattmx.nametags.entity.NameTagEntity;
import de.myzelyam.api.vanish.PlayerHideEvent;
import de.myzelyam.api.vanish.PlayerShowEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

/**
 * Listens to PremiumVanish/SuperVanish events to properly show/hide nametags.
 */
public class VanishEventListener implements Listener {

  private final @NotNull NameTags plugin;

  public VanishEventListener(@NotNull NameTags plugin) {
    this.plugin = plugin;
  }

  /**
   * Injects the vanish event listener if a compatible vanish plugin is present.
   */
  public static void inject(@NotNull NameTags plugin) {
    if (!VanishHook.isVanishPluginPresent()) {
      return;
    }

    plugin.getLogger().info("PremiumVanish/SuperVanish detected, registering vanish event listener.");
    Bukkit.getPluginManager().registerEvents(new VanishEventListener(plugin), plugin);
  }

  /**
   * When a player vanishes, remove them as a viewer from all nametags they can't
   * see,
   * and remove all viewers from their nametag who can't see them.
   */
  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerHide(@NotNull PlayerHideEvent event) {
    Player vanishedPlayer = event.getPlayer();
    NameTagEntity vanishedTag = plugin.getEntityManager().getNameTagEntity(vanishedPlayer);

    if (vanishedTag == null)
      return;

    // Run one tick later so the vanish plugin has already updated per-viewer
    // visibility.
    Bukkit.getScheduler().runTask(plugin, () -> {
      boolean showSelf = plugin.getConfig().getBoolean("show-self", false);

      for (Player viewer : Bukkit.getOnlinePlayers()) {
        boolean isSelfHidden = viewer.equals(vanishedPlayer) && !showSelf;
        boolean canSeeVanishedPlayer = VanishHook.canSee(viewer, vanishedPlayer);

        if (!isSelfHidden && canSeeVanishedPlayer) {
          continue;
        }

        vanishedTag.getPassenger().removeViewer(viewer.getUniqueId());

        // Explicitly send destroy packet to avoid stale nametags on clients.
        WrapperPlayServerDestroyEntities destroyPacket = new WrapperPlayServerDestroyEntities(
            vanishedTag.getPassenger().getEntityId());
        PacketEvents.getAPI().getPlayerManager().sendPacket(viewer, destroyPacket);
      }
    });
  }

  /**
   * When a player reappears (un-vanishes), add viewers back to their nametag.
   */
  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerShow(@NotNull PlayerShowEvent event) {
    Player shownPlayer = event.getPlayer();
    NameTagEntity shownTag = plugin.getEntityManager().getNameTagEntity(shownPlayer);

    if (shownTag == null)
      return;

    // Don't show nametag if it's admin-disabled or player is invisible
    if (plugin.getEntityManager().isNameTagDisabled(shownPlayer.getUniqueId()))
      return;
    if (shownTag.isInvisible())
      return;

    boolean showSelf = plugin.getConfig().getBoolean("show-self", false);

    // Add all online players who can now see the player back as viewers
    for (Player viewer : Bukkit.getOnlinePlayers()) {
      if (viewer.equals(shownPlayer) && !showSelf)
        continue;
      if (!viewer.getWorld().equals(shownPlayer.getWorld()))
        continue;

      // After this event completes, viewers will be able to see the player
      // Re-add them as viewers of the nametag
      shownTag.updateLocation();
      shownTag.getPassenger().removeViewer(viewer.getUniqueId());
      shownTag.getPassenger().addViewer(viewer.getUniqueId());
      shownTag.sendPassengerPacket(viewer);
    }
  }
}
