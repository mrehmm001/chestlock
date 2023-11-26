package chestlock.chestlock;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.*;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.DoubleChestInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.plugin.Plugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

import java.util.HashMap;

import static org.bukkit.Bukkit.getServer;

public class LockChestEvent implements Listener {
    private Plugin plugin;
    LockChestEvent(ChestLock plugin){
        this.plugin = plugin;
    }
    @EventHandler
    public void onLockChest(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Boolean isSign = isSign(event.getBlock().getType());
        Boolean isChest = event.getBlockAgainst().getType().equals(Material.CHEST);
        System.out.println(isSign+" "+isChest);
        if(!(isSign && isChest)) return;
        Sign sign = (Sign) event.getBlock().getState();
        Chest chest = (Chest) event.getBlockAgainst().getState();
        //Check if chest isnt already locked
        if(this.configHasChest(chest)){
            String name = this.getChestFromConfig(chest);
            if(!name.equals(player.getDisplayName())){
                event.setCancelled(true);
                player.sendMessage("This chest has already been locked by "+name);
                return;
            }else{
                event.setCancelled(true);
                player.sendMessage("You have already locked this chest!");
                return;
            }

        }

        sign.setEditable(false);
        event.setCancelled(true);

        getServer().getScheduler().runTaskLater(this.plugin, () -> {
            event.getBlock().setType(Material.OAK_WALL_SIGN);
            sign.setLine(0, "[LOCKED]");
            sign.setLine(1, player.getDisplayName());
            sign.update();
            String name = player.getDisplayName();
            if(chest.getInventory() instanceof DoubleChestInventory){
                System.out.println("Is double chest");
                DoubleChest dbChest = (DoubleChest) chest.getInventory().getHolder();
                Chest leftChest = (Chest) dbChest.getLeftSide();
                Chest rightChest = (Chest) dbChest.getRightSide();
                this.putChestInConfig(leftChest, name);
                this.putChestInConfig(rightChest, name);
            }else{
                System.out.println("Is single chest");
                this.plugin.getConfig().set(chest.getLocation().toString(), name);
            }
        }, 1L);
    }

    @EventHandler
    public void onChestOpen(InventoryOpenEvent event) {
        Player player = (Player) event.getPlayer();
        if(event.getInventory() instanceof DoubleChestInventory) {
            DoubleChest dbChest = (DoubleChest) event.getInventory().getHolder();
            Chest leftChest = (Chest) dbChest.getLeftSide();
            if (this.configHasChest(leftChest) && !(this.getChestFromConfig(leftChest).equals(player.getDisplayName()))) {
                event.setCancelled(true);
                String name = this.getChestFromConfig(leftChest);
                player.sendMessage("You cannot access " + name + "'s chest!");
                return;
            }
        }
        if(event.getInventory().getHolder() instanceof Chest){
            Chest chest = (Chest) event.getInventory().getHolder();
            if (this.configHasChest(chest) && !(this.getChestFromConfig(chest).equals(player.getDisplayName()))) {
                event.setCancelled(true);
                String name = this.getChestFromConfig(chest);
                player.sendMessage("You cannot access " + name + "'s chest!");
                return;
            }
        }
    }

    private void putChestInConfig(Chest chest, String name){
        this.plugin.getConfig().set(chest.getLocation().toString(), name);
        this.plugin.saveConfig();
    }

    private String getChestFromConfig(Chest chest){
        return this.plugin.getConfig().get(chest.getLocation().toString()).toString();
    }

    private boolean configHasChest(Chest chest){
        return this.plugin.getConfig().contains(chest.getLocation().toString());
    }

    private void removeChestFromConfig(Chest chest){
        this.plugin.getConfig().set(chest.getLocation().toString(), null);
        this.plugin.saveConfig();
    }


    @EventHandler
    public void onChestBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        if(isSign(block.getType())){
            Sign sign = (Sign) block.getState();

            if(sign.getLine(0).equals("[LOCKED]") && !(sign.getLine(1).equals(player.getDisplayName()))){
                event.setCancelled(true);
                String name = sign.getLine(1);
                player.sendMessage("You cannot break "+name+"'s chest!");
                return;
            }else if(sign.getLine(0).equals("[LOCKED]") && sign.getLine(1).equals(player.getDisplayName())){
                BlockData data = block.getBlockData();
                if (data instanceof Directional)
                {
                    Directional directional = (Directional)data;
                    Block blockBehind = block.getRelative(directional.getFacing().getOppositeFace());

                    if(blockBehind.getType().equals(Material.CHEST)){
                        Chest chest = (Chest) blockBehind.getState();
                        if(chest.getInventory() instanceof DoubleChestInventory){
                            System.out.println("Is double chest");
                            DoubleChest dbChest = (DoubleChest) chest.getInventory().getHolder();
                            Chest leftChest = (Chest) dbChest.getLeftSide();
                            Chest rightChest = (Chest) dbChest.getRightSide();
                            this.removeChestFromConfig(leftChest);
                            this.removeChestFromConfig(rightChest);
                        }else{
                            System.out.println("Is single chest");
                            this.removeChestFromConfig(chest);
                        }
                        player.sendMessage("Lock removed");
                    }
                }

            }
        }
        if(block.getType().equals(Material.CHEST)){
            Chest chest = (Chest) block.getState();
            if(this.configHasChest(chest) && !(this.getChestFromConfig(chest).equals(player.getDisplayName()))){
                event.setCancelled(true);
                String name = this.getChestFromConfig(chest);
                player.sendMessage("You cannot break "+name+"'s chest!");
                return;
            }else{
                if(chest.getInventory() instanceof DoubleChestInventory){
                    System.out.println("Is double chest");
                    DoubleChest dbChest = (DoubleChest) chest.getInventory().getHolder();
                    Chest leftChest = (Chest) dbChest.getLeftSide();
                    Chest rightChest = (Chest) dbChest.getRightSide();
                    this.removeChestFromConfig(leftChest);
                    this.removeChestFromConfig(rightChest);
                }else{
                    System.out.println("Is single chest");
                    this.removeChestFromConfig(chest);
                }
            }
        }
    }

    public boolean isSign(Material block){
        if(block.equals(Material.SPRUCE_WALL_SIGN) || block.equals(Material.ACACIA_WALL_SIGN) ||
                block.equals(Material.BAMBOO_WALL_SIGN) || block.equals(Material.BIRCH_WALL_SIGN)
            || block.equals(Material.OAK_WALL_SIGN) || block.equals(Material.JUNGLE_WALL_SIGN)){
            return true;
        }
        return false;
    }
}