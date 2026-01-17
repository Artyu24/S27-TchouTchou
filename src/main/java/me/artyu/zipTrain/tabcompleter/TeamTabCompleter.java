package me.artyu.zipTrain.tabcompleter;

import me.artyu.zipTrain.manager.LockManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class TeamTabCompleter implements TabCompleter
{
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args)
    {
        if (args.length != 1)
            return Collections.emptyList();

        String input = args[0].toLowerCase(Locale.ROOT);
        String commandName = command.getName().toLowerCase(Locale.ROOT);

        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();

        return scoreboard.getTeams().stream()
                .map(Team::getName)
                .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(input))
                .filter(name ->
                {
                    boolean locked = LockManager.isTeamLocked(name);

                    if (commandName.equals("lockteam"))
                        return !locked;

                    if (commandName.equals("unlockteam"))
                        return locked;

                    return false;
                })
                .sorted()
                .collect(Collectors.toList());
    }
}
