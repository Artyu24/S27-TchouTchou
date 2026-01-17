package me.artyu.zipTrain.manager;

import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class LockManager
{
    private static final Set<String> LOCKED_TEAMS = new HashSet<>();

    public static boolean isLocked(Player player)
    {
        Team team = player.getScoreboard().getEntryTeam(player.getName());
        if (team == null)
            return false;

        return LOCKED_TEAMS.contains(team.getName());
    }

    public static void lockTeam(String teamName)
    {
        LOCKED_TEAMS.add(teamName);
    }

    public static void unlockTeam(String teamName)
    {
        LOCKED_TEAMS.remove(teamName);
    }

    public static boolean isTeamLocked(String teamName)
    {
        return LOCKED_TEAMS.contains(teamName);
    }

    public static Set<String> getLockedTeams()
    {
        return Collections.unmodifiableSet(LOCKED_TEAMS);
    }
}
