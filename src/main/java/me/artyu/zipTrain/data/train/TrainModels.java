package me.artyu.zipTrain.data.train;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

public class TrainModels
{
    private static final String NAMESPACE = "survisland";

    public static ItemStack locomotive()
    {
        return create("locomotive_slow");
    }

    public static ItemStack wagon()
    {
        return create("craft_wagon_empty");
    }

    public static ItemStack chest()
    {
        return create("chest_wagon");
    }

    private static ItemStack create(String itemModelId)
    {
        ItemStack item = new ItemStack(Material.CARROT_ON_A_STICK);
        ItemMeta meta = item.getItemMeta();

        meta.setItemModel(new NamespacedKey(NAMESPACE, itemModelId));
        item.setItemMeta(meta);

        return item;
    }

    public static NamespacedKey model(String id)
    {
        return new NamespacedKey(NAMESPACE, id);
    }
}
