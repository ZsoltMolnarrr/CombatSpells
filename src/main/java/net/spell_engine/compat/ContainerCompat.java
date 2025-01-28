package net.spell_engine.compat;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class ContainerCompat {
    public static final ArrayList< Function<PlayerEntity, List<ItemStack>> > providers = new ArrayList<>();
    public static void addProvider(Function<PlayerEntity, List<ItemStack>> provider) {
        providers.add(provider);
    }
}
