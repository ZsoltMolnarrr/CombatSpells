package net.spell_engine.mixin.entity;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;
import net.spell_engine.internals.container.SpellContainerHelper;
import net.spell_engine.internals.container.SpellContainerSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;

@Mixin(PlayerEntity.class)
public class PlayerSpellContainerMixin implements SpellContainerSource.Owner {
    private SpellContainerSource.Result currentSpellContainers = SpellContainerSource.Result.EMPTY;

    @Override
    public Map<String, List<SpellContainerSource.SourcedContainer>> spellContainerCache() {
        return spellContainerCache;
    }
    private Map<String, List<SpellContainerSource.SourcedContainer>> spellContainerCache = new LinkedHashMap<>();

    @Override
    public void setSpellContainers(SpellContainerSource.Result result) {
        currentSpellContainers = result;
    }

    @Override
    public SpellContainerSource.Result getSpellContainers() {
        return currentSpellContainers;
    }

    private ItemStack lastMainHandStack = ItemStack.EMPTY;
    public Map<String, Object> lastProviderStates = new HashMap<>();

    @Inject(method = "tick", at = @At("TAIL"))
    private void tick_TAIL_SpellEngine_SpellContainer(CallbackInfo ci) {
        var player = (PlayerEntity) (Object) this;

        // Special treatment for main hand stack
        // as it changes the most frequently
        var mainHandStack = player.getMainHandStack();
        if (mainHandStack != lastMainHandStack
                && !Objects.equals(
                        SpellContainerHelper.containerFromItemStack(mainHandStack),
                        SpellContainerHelper.containerFromItemStack(lastMainHandStack))
                ) {
            SpellContainerSource.setDirty(player, SpellContainerSource.MAIN_HAND);
        }
        for (var entry: SpellContainerSource.sources) {
            if (entry.checker() == null) { continue; }
            var currentState = entry.checker().current(player);
            if (!currentState.equals(lastProviderStates.get(entry.name()))){
                SpellContainerSource.setDirty(player, entry.name());
            }
            lastProviderStates.put(entry.name(), currentState);
        }

        SpellContainerSource.update(player);

        lastMainHandStack = mainHandStack;
    }
}
