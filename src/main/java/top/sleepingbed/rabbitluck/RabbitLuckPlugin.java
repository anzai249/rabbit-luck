package top.sleepingbed.rabbitluck;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class RabbitLuckPlugin extends JavaPlugin {

    private LuckListener listener;

    @Override
    public void onEnable() {
        listener = new LuckListener(this);
        getServer().getPluginManager().registerEvents(listener, this);

        // If reloaded while players are online, sync their effect state.
        Bukkit.getOnlinePlayers().forEach(listener::refreshLuckFor);

        getLogger().info("RabbitLuck enabled.");
    }

    @Override
    public void onDisable() {
        // Clean up any effects we applied so we don't leave stale state behind.
        if (listener != null) {
            Bukkit.getOnlinePlayers().forEach(listener::clearIfApplied);
        }
        getLogger().info("RabbitLuck disabled.");
    }
}