package me.artyu.zipTrain.commands;

import me.artyu.zipTrain.data.Train;
import me.artyu.zipTrain.manager.TrainManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.*;
import org.bukkit.entity.minecart.PoweredMinecart;
import org.bukkit.entity.minecart.StorageMinecart;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.Vector;

import java.util.List;

import static org.apache.logging.log4j.LogManager.getLogger;

public class SpawnTrainCommand  implements CommandExecutor
{
    private final TrainManager trainManager;

    public SpawnTrainCommand(TrainManager trainManager)
    {
        this.trainManager = trainManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        if (args.length != 5)
        {
            sender.sendMessage("Usage: /spawntrain <player> <x> <y> <z> <n/s/e/w>");
            return true;
        }

        Player player = resolveTargetPlayer(sender, args[0]);
        if (player == null)
        {
            sender.sendMessage("Player not found : " + args[0]);
            getLogger().info("Player not found : " + args[0]);
            return true;
        }

        double x, y, z;

        try
        {
            x = Double.parseDouble(args[1]);
            y = Double.parseDouble(args[2]);
            z = Double.parseDouble(args[3]);
        }
        catch (NumberFormatException e)
        {
            sender.sendMessage("Coordinates must be numbers.");
            getLogger().info("Coordinates must be numbers.");
            return true;
        }

        if(!args[4].equalsIgnoreCase("n") && !args[4].equalsIgnoreCase("s") && !args[4].equalsIgnoreCase("w") && !args[4].equalsIgnoreCase("e"))
        {
            getLogger().info(args[4]);
            sender.sendMessage("Usage: /spawntrain <player> <x> <y> <z> <n/s/e/w>");
            return true;
        }

        World world = player.getWorld();

        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        Team team = scoreboard.getEntryTeam(player.getName());
        if (team == null)
        {
            player.sendMessage("§cYou are not in a team.");
            getLogger().info("§cYou are not in a team.");
            return true;
        }

        Train existingTrain = trainManager.getTrain(team);
        if (existingTrain != null)
            trainManager.destroyTrain(team);

        Location base = new Location(world, x, y, z);

        double xAdd = 0;
        double zAdd = 0;
        switch (args[4])
        {
            case "n":
            case "N":
                zAdd = 1.5;
                break;
            case "s":
            case "S":
                zAdd = -1.5;
                break;
            case "e":
            case "E":
                xAdd = -1.5;
                break;
            case "w":
            case "W":
                xAdd = 1.5;
                break;
            default:
                break;
        }

        Location locoLoc = base.clone().add(0, 0, 0);
        Location cartLoc = base.clone().add(xAdd, 0, zAdd);
        Location chestLoc = base.clone().add(xAdd * 2, 0, zAdd * 2);

        player.sendMessage(locoLoc.toString());
        player.sendMessage(cartLoc.toString());
        player.sendMessage(chestLoc.toString());

        PoweredMinecart furnace = world.spawn(locoLoc, PoweredMinecart.class);
        Minecart cart = world.spawn(cartLoc, Minecart.class);
        StorageMinecart chest = world.spawn(chestLoc, StorageMinecart.class);

        Vector furnaceDir = new Vector(xAdd * -1, 0, zAdd * -1);
        furnace.setVelocity(furnaceDir.multiply(0.2));
        cart.setVelocity(furnaceDir.multiply(0.2));
        chest.setVelocity(furnaceDir.multiply(0.2));
        furnace.setFuel(32000);

        setupMinecart(furnace, 1001);
        setupMinecart(cart, 1002);
        setupMinecart(chest, 1003);

        // Tags spécifiques
        furnace.addScoreboardTag("train_engine");
        cart.addScoreboardTag("team_furnace");
        chest.addScoreboardTag("train_storage");

        Train train = new Train(team, furnace, cart, chest, xAdd * -1, zAdd * -1);
        trainManager.setTrain(team, train);

        player.sendMessage("Train spawned and linked");

        return true;
    }

    private void setupMinecart(Minecart minecart, int cmd)
    {
        minecart.setInvulnerable(true);
        minecart.setSilent(true);
        minecart.setSlowWhenEmpty(false);
        minecart.addScoreboardTag("train_cart");
        minecart.addScoreboardTag("no_collision");

        ItemMeta meta = minecart.getPickItemStack().getItemMeta();
        meta.setCustomModelData(cmd);
        minecart.getPickItemStack().setItemMeta(meta);
    }

    private Player resolveTargetPlayer(CommandSender sender, String targetArg)
    {
        List<Entity> entities;

        try
        {
            entities = Bukkit.selectEntities(sender, targetArg);
        }
        catch (IllegalArgumentException e)
        {
            return null;
        }

        for (Entity entity : entities)
        {
            if (entity instanceof Player player)
                return player;
        }

        return null;
    }
}