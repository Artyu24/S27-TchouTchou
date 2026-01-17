package me.artyu.zipTrain.commands;

import me.artyu.zipTrain.manager.LockManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

public class LockCommand implements CommandExecutor
{
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        if (args.length != 1)
        {
            sender.sendMessage("/lockteam <team>");
            return true;
        }

        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        Team team = scoreboard.getTeam(args[0]);

        if (team == null)
        {
            sender.sendMessage("Team not found");
            return true;
        }

        LockManager.lockTeam(team.getName());

        for (String entry : team.getEntries())
        {
            Player player = Bukkit.getPlayer(entry);
            if (player == null)
                continue;

            fillInventoryWithBarriers(player);
            player.sendMessage("Your team has been locked");
        }

        sender.sendMessage("Team " + team.getName() + " locked");
        return true;
    }

    private void fillInventoryWithBarriers(Player player)
    {
        PlayerInventory inventory = player.getInventory();
        ItemStack barrier = new ItemStack(Material.BARRIER);

        // Hotbar (sauf slot 0)
        for (int i = 1; i <= 8; i++)
        {
            inventory.setItem(i, barrier);
        }

        // Inventaire principal
        for (int i = 9; i <= 35; i++)
        {
            inventory.setItem(i, barrier);
        }
    }
}
