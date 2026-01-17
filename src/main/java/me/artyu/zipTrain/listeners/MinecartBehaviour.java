package me.artyu.zipTrain.listeners;

import me.artyu.zipTrain.data.Train;
import me.artyu.zipTrain.manager.TrainManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Rail;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.bukkit.entity.minecart.PoweredMinecart;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.vehicle.VehicleBlockCollisionEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.vehicle.VehicleEntityCollisionEvent;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MinecartBehaviour implements Listener
{
    private final TrainManager trainManager;

    public MinecartBehaviour(TrainManager trainManager)
    {
        this.trainManager = trainManager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onVehicleEntityCollision(VehicleEntityCollisionEvent event)
    {
        if (!(event.getVehicle() instanceof Minecart minecart))
            return;

        if (!minecart.getScoreboardTags().contains("no_collision"))
            return;

        // Plus de push / blocage par entités
        event.setCancelled(true);
    }

    @EventHandler
    public void onVehicleMove(VehicleMoveEvent event)
    {
        if (!(event.getVehicle() instanceof Minecart cart))
            return;

        Train train = trainManager.getTrainContaining(cart);
        if (train == null)
            return;

        if (train.getTicksSinceSpawn() > 20 && !isOnRail(cart))
            trainManager.destroyTrain(train.getTeam());
    }

    @EventHandler(ignoreCancelled = true)
    public void onVehicleEnter(VehicleEnterEvent event)
    {
        if (!(event.getVehicle() instanceof Minecart minecart))
            return;

        if (!minecart.getScoreboardTags().contains("no_collision"))
            return;

        event.setCancelled(true);
    }

    public boolean isOnRail(Minecart cart)
    {
        Location loc = cart.getLocation();

        // Liste des positions à tester autour du minecart
        int[][] offsets = {
                { 0, -1, 0 }, // dessous
                { 0,  0, 0 }, // même bloc
                { 0, -2, 0 }  // pente descendante
        };

        for (int[] o : offsets)
        {
            if (loc.clone().add(o[0], o[1], o[2]).getBlock().getBlockData() instanceof Rail)
                return true;
        }

        return false;
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEntityEvent event)
    {
        if (!(event.getRightClicked() instanceof PoweredMinecart minecart))
            return;

        if (!minecart.getScoreboardTags().contains("train_engine"))
            return;

        event.setCancelled(true);

        Player player = event.getPlayer();

        Train train = trainManager.getTrainContaining(minecart);
        if (train == null)
            return;

        Team playerTeam = player.getScoreboard().getEntryTeam(player.getName());
        if (playerTeam == null || !playerTeam.equals(train.getTeam()))
        {
            player.sendActionBar(Component.text("This is not your team's train", NamedTextColor.RED));
            return;
        }

        Vector dir = minecart.getVelocity().normalize();

        if (!train.IsLow())
        {
            train.lowState(dir);

            player.sendActionBar(Component.text("Train ralentit", NamedTextColor.BLUE));
        }
        else
        {
            train.fastState(dir);

            player.sendActionBar(Component.text("Train accéléré", NamedTextColor.YELLOW));
        }
    }
}
