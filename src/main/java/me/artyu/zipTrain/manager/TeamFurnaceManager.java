package me.artyu.zipTrain.manager;

import io.papermc.paper.block.BlockPredicate;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemAdventurePredicate;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.TypedKey;
import io.papermc.paper.registry.keys.BlockTypeKeys;
import io.papermc.paper.registry.set.RegistryKeySet;
import io.papermc.paper.registry.set.RegistrySet;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.BlockType;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.FurnaceInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.view.FurnaceView;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Team;

import java.util.*;

public class TeamFurnaceManager
{
    private static final Map<String, FurnaceData> FURNACES = new HashMap<>();

    // Même durée pour tous (ticks). 200 = 10s
    private static final int COOK_TIME_TICKS = 20;

    // 3 recettes (fuel + input -> rails)
    private static final List<Recipe> RECIPES = List.of(
            new Recipe(Material.OAK_WOOD, Material.ANDESITE, List.of(Material.GRASS_BLOCK, Material.ANDESITE, Material.OAK_WOOD, Material.LIME_CONCRETE)),
            new Recipe(Material.SPRUCE_WOOD, Material.CALCITE, List.of(Material.SNOW_BLOCK, Material.WHITE_CONCRETE_POWDER, Material.SPRUCE_WOOD, Material.CALCITE, Material.WHITE_WOOL, Material.STRIPPED_PALE_OAK_WOOD, Material.LIGHT_BLUE_CONCRETE)),
            new Recipe(Material.STRIPPED_MANGROVE_WOOD, Material.BLACKSTONE, List.of(Material.CRIMSON_NYLIUM, Material.BLACKSTONE, Material.NETHER_BRICKS, Material.RED_CONCRETE, Material.RED_STAINED_GLASS, Material.ORANGE_STAINED_GLASS, Material.YELLOW_STAINED_GLASS))
    );

    private static BukkitTask _task;

    public static void init(JavaPlugin plugin)
    {
        if (_task != null)
            return;

        _task = Bukkit.getScheduler().runTaskTimer(plugin, TeamFurnaceManager::tickAll, 1L, 1L);
    }

    public static void shutdown()
    {
        if (_task != null)
            _task.cancel();

        _task = null;
        FURNACES.clear();
    }

    public static FurnaceInventory getFurnaceFor(Player player)
    {
        Team team = player.getScoreboard().getEntryTeam(player.getName());
        if (team == null)
            return null;

        FurnaceData data = FURNACES.computeIfAbsent(team.getName(), name ->
        {
            Inventory inv = Bukkit.createInventory(null, InventoryType.FURNACE, "Team Furnace");
            return new FurnaceData((FurnaceInventory) inv);
        });

        return data._inv;
    }

    private static void tickAll()
    {
        for (FurnaceData data : FURNACES.values())
            tickOne(data);
    }

    private static void tickOne(FurnaceData data)
    {
        FurnaceInventory inv = data._inv;

        ItemStack input = inv.getSmelting(); // slot 0
        ItemStack fuel = inv.getFuel();      // slot 1
        ItemStack result = inv.getResult();  // slot 2

        Recipe recipe = findRecipe(fuel, input);
        if (recipe == null)
        {
            data._progress = 0;
            updateFurnaceView(inv, 0, COOK_TIME_TICKS, false);
            return;
        }

        ItemStack out = makeRails(2, recipe._allowedBases); // adapte si ton Recipe a encore un seul Material
        if (!canInsert(result, out))
        {
            // On “garde” la flèche à son état actuel (ou 0 si tu préfères)
            updateFurnaceView(inv, data._progress, COOK_TIME_TICKS, true);
            return;
        }

        data._progress++;
        updateFurnaceView(inv, data._progress, COOK_TIME_TICKS, true);

        if (data._progress < COOK_TIME_TICKS)
            return;

        data._progress = 0;

        // Option B : consommation manuelle 1 fuel + 1 input
        consumeOne(inv, 0); // input
        consumeOne(inv, 1); // fuel

        inv.setResult(merge(result, out));

        // Reset visuel après craft
        updateFurnaceView(inv, 0, COOK_TIME_TICKS, false);
    }

    private static void updateFurnaceView(FurnaceInventory inv, int progress, int total, boolean burning)
    {
        if (progress < 0) progress = 0;
        if (total <= 0) total = 1;
        if (progress > total) progress = total;

        for (HumanEntity viewer : inv.getViewers())
        {
            if (!(viewer.getOpenInventory() instanceof FurnaceView view))
                continue;

            // Flèche (progression)
            view.setCookTime(progress, total);

            // Flamme (visuel fuel)
            if (burning)
                view.setBurnTime(progress, total);   // flamme "allumée"
            else
                view.setBurnTime(0, 1);   // flamme "éteinte"
        }
    }

    private static Recipe findRecipe(ItemStack fuel, ItemStack input)
    {
        if (fuel == null || fuel.getType() == Material.AIR) return null;
        if (input == null || input.getType() == Material.AIR) return null;

        Material fuelType = fuel.getType();
        Material inputType = input.getType();

        for (Recipe r : RECIPES)
        {
            if (r._fuel == fuelType && r._input == inputType)
                return r;
        }

        return null;
    }

    private static ItemStack makeRails(int amount, List<Material> allowedBases)
    {
        ItemStack rails = new ItemStack(Material.RAIL, amount);

        // 1) Material -> TypedKey<BlockType>
        List<TypedKey<BlockType>> keys = new ArrayList<>();
        for (Material m : allowedBases)
        {
            if (m == null || !m.isBlock())
                continue;

            String id = m.getKey().toString();

            keys.add(TypedKey.create(RegistryKey.BLOCK, id));
        }

        // fallback si liste vide
        if (keys.isEmpty())
            keys.add(TypedKey.create(RegistryKey.BLOCK, "minecraft:stone"));

        // 2) On fabrique le keyset "can_place_on" avec tous les blocs
        RegistryKeySet<BlockType> allowedBlocks = RegistrySet.keySet(
                RegistryKey.BLOCK,
                keys.toArray(new TypedKey[0])
        );

        // 3) Un seul predicate qui accepte n'importe lequel des blocs listés
        BlockPredicate predicate = BlockPredicate.predicate()
                .blocks((RegistryKeySet) allowedBlocks)
                .build();

        ItemAdventurePredicate canPlaceOn = ItemAdventurePredicate.itemAdventurePredicate(List.of(predicate));

        // 4) On applique le composant vanilla Adventure
        rails.setData(DataComponentTypes.CAN_PLACE_ON, canPlaceOn);

        return rails;
    }

    private static void consumeOne(FurnaceInventory inv, int slot)
    {
        ItemStack it = inv.getItem(slot);
        if (it == null || it.getType() == Material.AIR) return;

        int a = it.getAmount();
        if (a <= 1) inv.setItem(slot, null);
        else
        {
            it.setAmount(a - 1);
            inv.setItem(slot, it);
        }
    }

    private static boolean canInsert(ItemStack existing, ItemStack toAdd)
    {
        if (existing == null || existing.getType() == Material.AIR)
            return true;

        if (existing.getType() != toAdd.getType())
            return false;

        return existing.getAmount() + toAdd.getAmount() <= existing.getMaxStackSize();
    }

    private static ItemStack merge(ItemStack existing, ItemStack add)
    {
        if (existing == null || existing.getType() == Material.AIR)
            return add;

        ItemStack copy = existing.clone();
        copy.setAmount(existing.getAmount() + add.getAmount());
        return copy;
    }

    public static List<ItemStack> popAllFurnaceItems(Team team)
    {
        if (team == null)
            return Collections.emptyList();

        FurnaceData data = FURNACES.get(team.getName());
        if (data == null || data._inv == null)
            return Collections.emptyList();

        FurnaceInventory inv = data._inv;

        for (HumanEntity viewer : new ArrayList<>(inv.getViewers())){
            viewer.closeInventory();
            viewer.sendMessage("§cLe train a été détruit, le four est hors service.");
        }

        List<ItemStack> drops = new ArrayList<>();
        for (ItemStack it : inv.getContents())
        {
            if (it == null || it.getType().isAir())
                continue;

            drops.add(it.clone()); // garde exactement la même data
        }

        inv.clear();
        data._progress = 0;
        updateFurnaceView(inv, 0, COOK_TIME_TICKS, false);

        return drops;
    }

    public static boolean hasRailsInResult(Team team)
    {
        if (team == null)
            return false;

        FurnaceData data = FURNACES.get(team.getName());
        if (data == null || data._inv == null)
            return false;

        ItemStack result = data._inv.getResult(); // slot 2
        return result != null && result.getType() == Material.RAIL && result.getAmount() > 0;
    }

    private static final class FurnaceData
    {
        private final FurnaceInventory _inv;
        private int _progress;

        private FurnaceData(FurnaceInventory inv)
        {
            _inv = inv;
        }
    }

    private static final class Recipe
    {
        private final Material _fuel;
        private final Material _input;
        private final List<Material> _allowedBases;

        private Recipe(Material fuel, Material input, List<Material> allowedBase)
        {
            _fuel = fuel;
            _input = input;
            _allowedBases = allowedBase;
        }
    }
}
