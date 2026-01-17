package me.artyu.zipTrain.listeners;

import me.artyu.zipTrain.manager.TeamFurnaceManager;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.FurnaceInventory;

public class TeamMinecartInteract  implements Listener
{
    @EventHandler
    public void onInteract(PlayerInteractEntityEvent event)
    {
        if (!(event.getRightClicked() instanceof Minecart))
            return;

        Minecart minecart = (Minecart) event.getRightClicked();

        if (!minecart.getScoreboardTags().contains("team_furnace"))
            return;

        Player player = event.getPlayer();

        FurnaceInventory furnace = TeamFurnaceManager.getFurnaceFor(player);
        if (furnace == null)
            return;

        event.setCancelled(true);
        player.openInventory(furnace);
    }
}
