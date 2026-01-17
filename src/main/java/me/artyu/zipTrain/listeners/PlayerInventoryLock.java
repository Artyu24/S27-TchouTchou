package me.artyu.zipTrain.listeners;

import me.artyu.zipTrain.manager.LockManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public class PlayerInventoryLock implements Listener
{
    @EventHandler
    public void onInventoryClickEvent(InventoryClickEvent event)
    {
        if (event.getClickedInventory() == null)
            return;

        Player player = (Player) event.getWhoClicked();

        if (!LockManager.isLocked(player))
            return;

        if (event.getSlot() == 40)
        {
            event.setCancelled(true);
            return;
        }

        ItemStack current = event.getCurrentItem();
        ItemStack cursor = event.getCursor();

        if ((current != null && current.getType() == Material.BARRIER) || (cursor != null && cursor.getType() == Material.BARRIER))
        {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryShiftClick(InventoryClickEvent event)
    {
        Player player = (Player) event.getWhoClicked();

        if (!LockManager.isLocked(player))
            return;

        if (!event.isShiftClick())
            return;

        ItemStack item = event.getCurrentItem();

        if (item == null)
            return;

        if (item.getType() == Material.BARRIER)
        {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event)
    {
        Player player = (Player) event.getWhoClicked();

        if (!LockManager.isLocked(player))
            return;

        ItemStack item = event.getOldCursor();

        if (item == null)
            return;

        if (item.getType() == Material.BARRIER)
        {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onItemDrop(PlayerDropItemEvent event)
    {
        if (!LockManager.isLocked(event.getPlayer()))
            return;

        if (event.getItemDrop().getItemStack().getType() == Material.BARRIER)
        {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event)
    {
        if (!LockManager.isLocked(event.getPlayer()))
            return;

        if (event.getItemInHand().getType() == Material.BARRIER)
        {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event)
    {
        if (!LockManager.isLocked(event.getPlayer()))
            return;

        if (event.getItem() == null)
            return;

        if (event.getItem().getType() == Material.BARRIER)
        {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onSwapHandItems(PlayerSwapHandItemsEvent event)
    {
        if (!LockManager.isLocked(event.getPlayer()))
            return;

        event.setCancelled(true);
    }
}
