package me.artyu.zipTrain.data;

import me.artyu.zipTrain.data.train.TrainModels;
import me.artyu.zipTrain.data.train.VisualWagon;
import me.artyu.zipTrain.manager.TeamFurnaceManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public class Train
{
    private static class PathSample
    {
        final Vector position;
        final Vector direction;
        final float yaw;

        PathSample(Location location, Vector direction)
        {
            this.position = location.toVector();
            this.direction = direction.clone();
            this.yaw = location.getYaw();
        }
    }

    private final List<PathSample> pathBuffer = new ArrayList<>();
    private final int maxPathSamples = 300;

    private int ticksSinceSpawn = 0;
    public int getTicksSinceSpawn() {return ticksSinceSpawn;}

    private final List<Minecart> carts = new ArrayList<>();
    private final double spacing = 1.75;
    private double speed = 0.02;
    private double lowSpeed = 0.02;
    private double highSpeed = 0.2;
    private boolean isLow = true;
    private Vector lastDirection = new Vector(0, 0, 0);

    private final List<VisualWagon> visuals = new ArrayList<>();
    private final VisualWagon locomotiveVisual;
    private final VisualWagon wagonVisual;
    private boolean wagonRailsVariant = false;

    private final Team team;

    public boolean isValid()
    {
        for (Minecart cart : carts)
        {
            if (cart == null || !cart.isValid())
            {
                destroy();
                return false;
            }
        }
        return true;
    }

    public Team getTeam() {return team;}

    public boolean IsLow()
    {
        return isLow;
    }

    public Train(Team team, Minecart engine, Minecart middle, Minecart chest, double xVal, double zVal)
    {
        this.team = team;

        carts.add(engine);
        carts.add(middle);
        carts.add(chest);

        Vector initialDir = new Vector(xVal, 0, zVal);

        locomotiveVisual = new VisualWagon(engine, TrainModels.locomotive(), new Vector(0.0, 1.6, 0.64), initialDir);
        wagonVisual = new VisualWagon(middle, TrainModels.wagon(), new Vector(0.0, 1.6, -0.12), initialDir);
        wagonRailsVariant = false;

        visuals.add(locomotiveVisual);
        visuals.add(wagonVisual);
        visuals.add(new VisualWagon(chest, TrainModels.chest(), new Vector(0.0, 1.6, -0.12), initialDir));

        lastDirection = new Vector(xVal, 0, zVal);

        warmupPathBuffer(xVal, zVal);
    }

    private void warmupPathBuffer(double xVal, double zVal)
    {
        Location loc = carts.get(0).getLocation().clone();

        // Direction 4-way à partir du spawn (stable)
        Vector dir = new Vector(xVal, 0, zVal);
        if (dir.lengthSquared() < 1e-6)
            dir = lastDirection.clone();

        dir.setY(0);

        boolean alongX = Math.abs(dir.getX()) >= Math.abs(dir.getZ());
        if (alongX)
            dir = new Vector(Math.signum(dir.getX() == 0 ? 1 : dir.getX()), 0, 0);
        else
            dir = new Vector(0, 0, Math.signum(dir.getZ() == 0 ? 1 : dir.getZ()));

        // Centre du bloc rail (axe latéral verrouillé)
        double centerX = loc.getBlockX() + 0.5;
        double centerZ = loc.getBlockZ() + 0.5;

        if (alongX)
            loc.setZ(centerZ);
        else
            loc.setX(centerX);

        float yaw = loc.getYaw();

        pathBuffer.clear();

        for (int i = maxPathSamples - 1; i >= 0; i--)
        {
            Location fake = loc.clone().subtract(dir.clone().multiply(i * speed));

            // verrouille l'axe latéral sur toute la queue
            if (alongX)
                fake.setZ(centerZ);
            else
                fake.setX(centerX);

            fake.setYaw(yaw);
            pathBuffer.add(new PathSample(fake, dir));
        }

        // Place immédiatement les autres carts sur ce buffer
        for (int i = 1; i < carts.size(); i++)
        {
            Minecart cart = carts.get(i);
            PathResult result = getInterpolatedLocation(i * spacing, cart.getLocation());
            if (result == null)
                continue;

            cart.teleport(result.location);
            cart.setGravity(false);
            cart.setVelocity(result.direction.clone().multiply(speed));
        }
    }

    public void tick()
    {
        if(ticksSinceSpawn <= 20)
            ticksSinceSpawn++;

        Minecart front = carts.get(0);

    /* =========================
       1) LEADER : vitesse fixe, pentes autorisées
       ========================= */

        Vector velocity = front.getVelocity().clone();

        if (velocity.lengthSquared() < 0.0001)
            velocity = lastDirection.clone();

        velocity.normalize().multiply(speed);
        front.setVelocity(velocity);
        front.setGravity(false);

        lastDirection = velocity.clone();

    /* =========================
       2) ENREGISTREMENT DU PATH
       ========================= */

        Vector dir = front.getVelocity().clone();
        if (dir.lengthSquared() < 0.0001)
            dir = lastDirection.clone();

        dir.normalize();

        pathBuffer.add(new PathSample(front.getLocation(), dir));
        if (pathBuffer.size() > maxPathSamples)
            pathBuffer.remove(0);

    /* =========================
       3) WAGONS : path replay interpolé
       ========================= */

        for (int i = 1; i < carts.size(); i++)
        {
            Minecart cart = carts.get(i);
            double targetDistance = i * spacing;

            PathResult result = getInterpolatedLocation(targetDistance, cart.getLocation());
            if (result == null)
                continue;

            cart.teleport(result.location);
            cart.setGravity(false);

            // 🔥 FAKE velocity visuelle
            cart.setVelocity(result.direction.clone().multiply(speed));
        }

    /* =========================
       4) VISUELS
       ========================= */

        boolean hasRails = TeamFurnaceManager.hasRailsInResult(team);
        if (hasRails != wagonRailsVariant)
        {
            wagonRailsVariant = hasRails;


            wagonVisual.setItemModel(TrainModels.model(hasRails ? "craftwagonfull" : "craftwagonempty"));
        }

        for (VisualWagon visual : visuals)
            visual.tick();
    }

    private static class PathResult
    {
        final Location location;
        final Vector direction;

        PathResult(Location location, Vector direction)
        {
            this.location = location;
            this.direction = direction;
        }
    }

    private PathResult getInterpolatedLocation(double distance, Location fallback)
    {
        double traveled = 0;

        for (int i = pathBuffer.size() - 1; i > 0; i--)
        {
            PathSample a = pathBuffer.get(i);
            PathSample b = pathBuffer.get(i - 1);

            double segment = a.position.distance(b.position);

            if (traveled + segment >= distance)
            {
                double t = (distance - traveled) / segment;

                Vector pos = b.position.clone().multiply(1.0 - t)
                        .add(a.position.clone().multiply(t));

                Vector dir = b.direction.clone().multiply(1.0 - t)
                        .add(a.direction.clone().multiply(t))
                        .normalize();

                float yaw = lerpYaw(b.yaw, a.yaw, (float) t);

                Location loc = fallback.clone();
                loc.set(pos.getX(), pos.getY(), pos.getZ());
                loc.setYaw(yaw);

                return new PathResult(loc, dir);
            }

            traveled += segment;
        }

        return null;
    }

    private float lerpYaw(float a, float b, float t)
    {
        float delta = ((b - a + 540f) % 360f) - 180f;
        return a + delta * t;
    }

    public boolean contains(Minecart minecart)
    {
        return carts.contains(minecart);
    }

    public void fastState(Vector initialDirection)
    {
        if(!isLow)
            return;

        isLow = false;
        speed = highSpeed;

        locomotiveVisual.setItemModel(TrainModels.model("locomotivefast"));

        modifySpeed(initialDirection);
    }

    public void lowState(Vector initialDirection)
    {
        if(isLow)
            return;

        isLow = true;
        speed = lowSpeed;

        locomotiveVisual.setItemModel(TrainModels.model("locomotiveslow"));

        modifySpeed(initialDirection);
    }

    private void modifySpeed(Vector initialDirection)
    {
        Vector dir = (initialDirection != null) ? initialDirection.clone() : (lastDirection != null ? lastDirection.clone() : null);
        if (dir == null)
            return;

        dir.setY(0);

        if (!Double.isFinite(dir.getX()) || !Double.isFinite(dir.getZ()) || dir.lengthSquared() < 1e-8) {
            // fallback sur lastDirection
            if (lastDirection == null)
                return;

            dir = lastDirection.clone();
            dir.setY(0);

            if (!Double.isFinite(dir.getX()) || !Double.isFinite(dir.getZ()) || dir.lengthSquared() < 1e-8)
                return;
        }

        dir.normalize();

        if (!Double.isFinite(dir.getX()) || !Double.isFinite(dir.getY()) || !Double.isFinite(dir.getZ()))
            return;

        lastDirection = dir.clone();

        if (!Double.isFinite(speed) || speed <= 0)
            return;

        Vector velocity = dir.multiply(speed);

        if (!Double.isFinite(velocity.getX()) || !Double.isFinite(velocity.getY()) || !Double.isFinite(velocity.getZ()))
            return;

        carts.get(0).setVelocity(velocity);
    }

    public void destroy()
    {
        sendMessageToTeam("§c⚠ Train détruit : déraillement détecté.");

        Location furnaceDropLoc = null;
        if (carts.size() >= 2 && carts.get(1) != null && carts.get(1).getWorld() != null)
            furnaceDropLoc = carts.get(1).getLocation().clone();

        Location chestDropLoc = null;
        if (carts.size() >= 3 && carts.get(2) != null && carts.get(2).getWorld() != null)
            chestDropLoc = carts.get(2).getLocation().clone();

        if (furnaceDropLoc != null)
        {
            World world = furnaceDropLoc.getWorld();
            for (ItemStack it : TeamFurnaceManager.popAllFurnaceItems(team))
                world.dropItemNaturally(furnaceDropLoc, it);
        }

        if (chestDropLoc != null && carts.size() >= 3)
        {
            Minecart chestCart = carts.get(2);
            if (chestCart instanceof InventoryHolder holder)
            {
                Inventory inv = holder.getInventory();
                for (ItemStack it : inv.getContents())
                {
                    if (it == null || it.getType().isAir())
                        continue;

                    chestDropLoc.getWorld().dropItemNaturally(chestDropLoc, it.clone());
                }
                inv.clear();
            }
        }

        for (VisualWagon visual : visuals)
            visual.remove();
        visuals.clear();

        for (Minecart cart : carts)
        {
            if (cart != null && !cart.isDead())
                cart.remove();
        }

        carts.clear();
    }

    private void sendMessageToTeam(String message)
    {
        for (String entry : team.getEntries())
        {
            Player player = Bukkit.getPlayer(entry);
            if (player == null)
                continue;

            player.sendMessage(message);
        }
    }
}
