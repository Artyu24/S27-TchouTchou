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

public class UnlockCommand implements CommandExecutor
{
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        if (args.length != 1)
        {
            sender.sendMessage("/unlockteam <team>");
            return true;
        }

        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        Team team = scoreboard.getTeam(args[0]);

        if (team == null)
        {
            sender.sendMessage("Team not found");
            return true;
        }

        LockManager.unlockTeam(team.getName());

        for (String entry : team.getEntries())
        {
            Player player = Bukkit.getPlayer(entry);
            if (player == null)
                continue;

            removeBarriers(player);
            player.getInventory().setItemInOffHand(null);
            player.sendMessage("Your team has been unlocked");
        }

        sender.sendMessage("Team " + team.getName() + " unlocked");
        return true;
    }

    private void removeBarriers(Player player)
    {
        PlayerInventory inventory = player.getInventory();

        for (int i = 0; i < inventory.getSize(); i++)
        {
            ItemStack item = inventory.getItem(i);

            if (item == null)
                continue;

            if (item.getType() == Material.BARRIER)
            {
                inventory.setItem(i, null);
            }
        }
    }
}


