package me.artyu.zipTrain.manager;

import me.artyu.zipTrain.data.Train;
import org.bukkit.entity.Minecart;
import org.bukkit.scoreboard.Team;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TrainManager
{
    private final Map<Team, Train> trainsByTeam = new HashMap<>();

    public Train getTrain(Team team)
    {
        return trainsByTeam.get(team);
    }

    public void setTrain(Team team, Train train)
    {
        trainsByTeam.put(team, train);
    }

    public void tick()
    {
        trainsByTeam.values().removeIf(train -> !train.isValid());

        for (Train train : trainsByTeam.values())
            train.tick();
    }

    public Train getTrainContaining(Minecart minecart)
    {
        for (Train train : trainsByTeam.values())
        {
            if (train.contains(minecart))
                return train;
        }
        return null;
    }

    public void destroyTrain(Team team)
    {
        Train train = trainsByTeam.remove(team);
        if (train != null)
            train.destroy();
    }
}
