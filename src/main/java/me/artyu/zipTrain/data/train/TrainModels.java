package me.artyu.zipTrain.data.train;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class TrainModels
{
    public static ItemStack locomotive() { return create(1001); } // 1 pour slow et 2 pour fast

    public static ItemStack wagon()
    {
        return create(1003);
    } // 3 pour vide et 4 pour plein

    public static ItemStack chest()
    {
        return create(1005);
    } // 5 classique

    private static ItemStack create(int cmd)
    {
        ItemStack item = new ItemStack(Material.CARROT_ON_A_STICK);
        ItemMeta meta = item.getItemMeta();
        meta.setCustomModelData(cmd);
        item.setItemMeta(meta);
        return item;
    }
}
