package chestlock.chestlock;

import org.bukkit.block.Chest;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.HashMap;
public final class ChestLock extends JavaPlugin {
    @Override
    public void onEnable() {
        System.out.println("Chest lock is working!");
        getServer().getPluginManager().registerEvents(new LockChestEvent(this), this);
    }

    @Override
    public void onDisable() {
        saveConfig();
    }
}
