package me.artyu.zipTrain.data.train;

import org.bukkit.Location;
import org.bukkit.block.data.Rail;
import org.bukkit.entity.Display;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Minecart;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;

import static org.apache.logging.log4j.LogManager.getLogger;

public class VisualWagon
{
    private final Minecart minecart;
    private final ItemDisplay display;

    private final Vector localOffset;
    private final Vector localScale;
    private final float modelYawOffsetDeg;

    private final Vector fallbackDir;
    private boolean firstSnapDone = false;
    private final int normalTeleportDuration = 4;

    public VisualWagon(Minecart minecart, ItemStack model, Vector localOffset, Vector fallbackDir)
    {
        this(minecart, model, localOffset, new Vector(3.5, 3.5, 3.5), 90f, fallbackDir);
    }

    public VisualWagon(Minecart minecart, ItemStack model, Vector localOffset, Vector localScale, float modelYawOffsetDeg, Vector fallbackDir)
    {
        this.minecart = minecart;
        this.localOffset = localOffset.clone();
        this.localScale = localScale.clone();
        this.modelYawOffsetDeg = modelYawOffsetDeg;
        this.fallbackDir = fallbackDir == null ? new Vector(0, 0, 1) : fallbackDir.clone();

        this.display = (ItemDisplay) minecart.getWorld().spawnEntity(
                minecart.getLocation(),
                EntityType.ITEM_DISPLAY
        );

        display.setItemStack(model);
        display.setBillboard(Display.Billboard.FIXED);
        display.setBrightness(new Display.Brightness(15, 15));
        display.setTeleportDuration(0);
        display.setInterpolationDelay(0);

        applyBaseTransform();
        minecart.setInvisible(true);

        snapNow();
    }

    private void snapNow()
    {
        Location base = minecart.getLocation();
        display.teleport(base);

        Vector v = minecart.getVelocity();
        if (v.lengthSquared() < 0.000001)
            v = fallbackDir.clone();

        v.setY(0);
        if (v.lengthSquared() < 0.000001)
            v = new Vector(0, 0, 1);

        v.normalize();

        float yaw = (float) Math.toDegrees(Math.atan2(-v.getX(), v.getZ()));
        display.setRotation(yaw, 0f);

        firstSnapDone = true;
        display.setTeleportDuration(normalTeleportDuration);
    }

    private void applyBaseTransform()
    {
        Transformation t = display.getTransformation();

        t.getTranslation().set(
                (float) localOffset.getX(),
                (float) localOffset.getY(),
                (float) localOffset.getZ()
        );

        t.getScale().set(
                (float) localScale.getX(),
                (float) localScale.getY(),
                (float) localScale.getZ()
        );

        // Correction d'orientation du modèle (ex: ton +90°)
        t.getLeftRotation().set(new Quaternionf().rotateY((float) Math.toRadians(-modelYawOffsetDeg)));

        display.setTransformation(t);
    }

    public void tick()
    {
        if (!minecart.isValid())
        {
            display.remove();
            return;
        }

        if (!firstSnapDone)
            snapNow();

        Location base = minecart.getLocation();
        display.teleport(base);

        var v = minecart.getVelocity();
        if (v.lengthSquared() < 0.000001)
        {
            display.setRotation(base.getYaw(), display.getPitch());
            return;
        }

        v = v.clone().normalize();

        float yaw = (float) Math.toDegrees(Math.atan2(-v.getX(), v.getZ()));
        float pitch = (float) -Math.toDegrees(Math.asin(v.getY())); // signe inversé pour Bukkit

        Vector railDir = onRailAscending(minecart);
        if(railDir != null)
        {
            railDir.setY(0).normalize();
            Vector horizontalMove = v.clone().setY(0).normalize();

            double dot = railDir.dot(horizontalMove);
            if (dot > 0)
                pitch = -45f; // montée
            else
                pitch = 45f; // descente
        }

        display.setRotation(yaw, pitch);
    }

    public void remove()
    {
        display.remove();
    }

    public Vector onRailAscending(Minecart cart)
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
            if (loc.clone().add(o[0], o[1], o[2]).getBlock().getBlockData() instanceof Rail rail)
            {
                Rail.Shape shape = rail.getShape();

                if (shape == Rail.Shape.ASCENDING_NORTH || shape == Rail.Shape.ASCENDING_SOUTH || shape == Rail.Shape.ASCENDING_EAST  || shape == Rail.Shape.ASCENDING_WEST)
                    return getRailAscendingDirection(shape);
                else
                    return null;
            }
        }

        return null;
    }

    private Vector getRailAscendingDirection(Rail.Shape shape)
    {
        return switch (shape)
        {
            case ASCENDING_NORTH -> new Vector(0, 1, -1);
            case ASCENDING_SOUTH -> new Vector(0, 1, 1);
            case ASCENDING_EAST  -> new Vector(1, 1, 0);
            case ASCENDING_WEST  -> new Vector(-1, 1, 0);
            default -> null;
        };
    }

    public void setCustomModelData(int customModelData)
    {
        ItemStack current = display.getItemStack();
        if (current == null)
            return;

        ItemStack updated = current.clone();
        ItemMeta meta = updated.getItemMeta();
        if (meta == null)
            return;

        Integer existing = meta.hasCustomModelData() ? meta.getCustomModelData() : null;
        if (existing != null && existing == customModelData)
            return;

        meta.setCustomModelData(customModelData);
        updated.setItemMeta(meta);
        display.setItemStack(updated);
    }
}
