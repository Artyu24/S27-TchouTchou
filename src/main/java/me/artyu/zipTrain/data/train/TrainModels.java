package me.artyu.zipTrain.data.train;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

public class TrainModels
{
    private static JavaPlugin plugin;

    public static void init(JavaPlugin pluginInstance)
    {
        plugin = pluginInstance;
    }

    public static ItemStack locomotive()
    {
        return create("locomotiveslow");
    }

    public static ItemStack wagon()
    {
        return create("craftwagonempty");
    }

    public static ItemStack chest()
    {
        return create("chestwagon");
    }

    private static ItemStack create(String itemModelId)
    {
        ItemStack item = new ItemStack(Material.CARROT_ON_A_STICK);
        ItemMeta meta = item.getItemMeta();

        meta.setItemModel(new NamespacedKey(plugin, itemModelId));
        item.setItemMeta(meta);

        return item;
    }

    public static NamespacedKey model(String id)
    {
        return new NamespacedKey(plugin, id);
    }
}
