package top.sleepingbed.rabbitluck;

import io.papermc.paper.event.player.PlayerInventorySlotChangeEvent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.bukkit.potion.PotionEffect.INFINITE_DURATION;

public final class LuckListener implements Listener {

    private final JavaPlugin plugin;
    private final Set<UUID> appliedByPlugin = ConcurrentHashMap.newKeySet();

    public LuckListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    // Public helpers for plugin lifecycle.
    public void refreshLuckFor(Player player) {
        if (hasRabbitFoot(player)) {
            applyLuck(player);
        } else {
            removeLuckIfApplied(player);
        }
    }

    public void clearIfApplied(Player player) {
        if (appliedByPlugin.remove(player.getUniqueId())) {
            player.removePotionEffect(PotionEffectType.LUCK);
        }
    }

    // Core logic
    private static boolean hasRabbitFoot(Player player) {
        return player.getInventory().contains(Material.RABBIT_FOOT);
    }

    private void applyLuck(Player player) {
        UUID id = player.getUniqueId();
        if (appliedByPlugin.contains(id)) {
            return; // already applied by us
        }
        // Very long duration; we remove it explicitly when needed.
        PotionEffect effect = new PotionEffect(
                PotionEffectType.LUCK,
                INFINITE_DURATION,
                0,                 // amplifier (Luck I)
                false,             // ambient
                false,              // particles
                true               // icon
        );
        player.addPotionEffect(effect);
        appliedByPlugin.add(id);
    }

    private void removeLuckIfApplied(Player player) {
        if (appliedByPlugin.remove(player.getUniqueId())) {
            player.removePotionEffect(PotionEffectType.LUCK);
        }
    }

    private void scheduleRefresh(Player player) {
        // Run next tick so inventory state is finalized.
        Bukkit.getScheduler().runTask(plugin, () -> refreshLuckFor(player));
    }

    // Events

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        scheduleRefresh(event.getPlayer());
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        scheduleRefresh(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        clearIfApplied(event.getPlayer());
    }

    // Paper-specific: fires on any change to a player inventory slot.
    @EventHandler
    public void onSlotChange(PlayerInventorySlotChangeEvent event) {
        ItemStack oldItem = event.getOldItemStack();
        ItemStack newItem = event.getNewItemStack();
        boolean involvesRabbitFoot =
                (oldItem != null && oldItem.getType() == Material.RABBIT_FOOT) ||
                        (newItem != null && newItem.getType() == Material.RABBIT_FOOT);

        if (involvesRabbitFoot) {
            scheduleRefresh(event.getPlayer());
        }
    }

    // Extra robustness for some flows (pickups/drops/clicks/drags)
    @EventHandler
    public void onPickup(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (event.getItem().getItemStack().getType() == Material.RABBIT_FOOT) {
                scheduleRefresh(player);
            }
        }
    }

    @EventHandler
    public void onDrop(org.bukkit.event.player.PlayerDropItemEvent event) {
        if (event.getItemDrop().getItemStack().getType() == Material.RABBIT_FOOT) {
            scheduleRefresh(event.getPlayer());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            ItemStack cursor = event.getCursor();
            ItemStack current = event.getCurrentItem();
            if ((cursor != null && cursor.getType() == Material.RABBIT_FOOT) ||
                    (current != null && current.getType() == Material.RABBIT_FOOT)) {
                scheduleRefresh(player);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            ItemStack before = event.getOldCursor();
            if (before != null && before.getType() == Material.RABBIT_FOOT) {
                scheduleRefresh(player);
            }
        }
    }

    @EventHandler
    public void onSwap(PlayerSwapHandItemsEvent event) {
        ItemStack main = event.getMainHandItem();
        ItemStack off = event.getOffHandItem();
        if ((main != null && main.getType() == Material.RABBIT_FOOT) ||
                (off != null && off.getType() == Material.RABBIT_FOOT)) {
            scheduleRefresh(event.getPlayer());
        }
    }
}