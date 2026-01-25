package me.artyu.zipTrain;

import me.artyu.zipTrain.commands.LockCommand;
import me.artyu.zipTrain.commands.SpawnTrainCommand;
import me.artyu.zipTrain.commands.UnlockCommand;
import me.artyu.zipTrain.data.train.TrainModels;
import me.artyu.zipTrain.listeners.MinecartBehaviour;
import me.artyu.zipTrain.listeners.PlayerInventoryLock;
import me.artyu.zipTrain.listeners.TeamMinecartInteract;
import me.artyu.zipTrain.manager.TeamFurnaceManager;
import me.artyu.zipTrain.manager.TrainManager;
import me.artyu.zipTrain.tabcompleter.TeamTabCompleter;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public final class ZipTrain extends JavaPlugin {

    @Override
    public void onEnable() {
        System.out.println("Tchou Tchou Plugin Enable");

        getServer().getPluginManager().registerEvents(new PlayerInventoryLock(), this);
        TeamFurnaceManager.init(this);
        getServer().getPluginManager().registerEvents(new TeamMinecartInteract(), this);

        getCommand("tchoutchou_lockteam").setExecutor(new LockCommand());
        getCommand("tchoutchou_unlockteam").setExecutor(new UnlockCommand());
        TeamTabCompleter teamCompleter = new TeamTabCompleter();
        getCommand("tchoutchou_lockteam").setTabCompleter(teamCompleter);
        getCommand("tchoutchou_unlockteam").setTabCompleter(teamCompleter);

        TrainBehaviour();
    }

    private void TrainBehaviour()
    {
        TrainManager trainManager = new TrainManager();

        new BukkitRunnable()
        {
            @Override
            public void run()
            {
                trainManager.tick();
            }
        }.runTaskTimer(this, 1L, 1L);

        getCommand("tchoutchou_spawntrain").setExecutor(new SpawnTrainCommand(trainManager));
        getServer().getPluginManager().registerEvents(new MinecartBehaviour(trainManager), this);
    }

    @Override
    public void onDisable() {
        TeamFurnaceManager.shutdown();

        System.out.println("Tchou Tchou Plugin Disable");

    }
}
