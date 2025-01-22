package net.spell_engine.compat;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.BundleContentsComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.spell_engine.internals.Ammo;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class ContainerCompat {
    public static final ArrayList< Function<PlayerEntity, List<ItemStack>> > providers = new ArrayList<>();
    public static void addProvider(Function<PlayerEntity, List<ItemStack>> provider) {
        providers.add(provider);
    }

    @Nullable public static Ammo.Source findContainer(PlayerEntity player, Item item, int amount) {
        for (var provider : providers) {
            var stacks = provider.apply(player);
            for (var stack : stacks) {
                var found = find(stack, item, amount);
                if (found > 0) {
                    return new Ammo.Source(stack, found, true);
                }
            }
        }
        return null;
    }

    public static int find(ItemStack containerStack, Item consumedItem, int amount) {
        int found = 0;
        var bundle = containerStack.get(DataComponentTypes.BUNDLE_CONTENTS);
        if (bundle != null) {
            for (int i = 0; i < bundle.size(); i++) {
                var storedStack = bundle.get(i);
                if (storedStack.getItem() == consumedItem) {
                    found += storedStack.getCount();
                }
            }
        }
        return found;
    }

    public static int takeFrom(ItemStack containerStack, Item consumedItem, int amount) {
        int taken = 0;
        var bundle = containerStack.get(DataComponentTypes.BUNDLE_CONTENTS);
        var toDecreement = amount;
        if (bundle != null) {
            var putBack = new ArrayList<ItemStack>();
            for (int i = 0; i < bundle.size(); i++) {
                var storedStack = bundle.get(i);
                if (storedStack.getItem() == consumedItem) {
                    var decrementable = Math.min(storedStack.getCount(), toDecreement);
                    storedStack.decrement(decrementable);
                    toDecreement -= decrementable;
                    taken += decrementable;
                    if (!storedStack.isEmpty()) {
                        putBack.add(storedStack);
                    }
                }
            }
            var newBundle = new BundleContentsComponent.Builder(bundle).clear();
            for (var stackToAdd : putBack) {
                newBundle.add(stackToAdd);
            }
            containerStack.set(DataComponentTypes.BUNDLE_CONTENTS, newBundle.build());
        }
        return taken;
    }
}
