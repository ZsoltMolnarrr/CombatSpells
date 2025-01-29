package net.spell_engine.mixin.entity;

import net.minecraft.entity.player.PlayerEntity;
import net.spell_engine.internals.container.SpellContainerSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public class PlayerSpellContainerMixin implements SpellContainerSource.Owner {
    private SpellContainerSource.Result currentSpellContainers = SpellContainerSource.Result.EMPTY;

    @Override
    public void setSpellContainers(SpellContainerSource.Result result) {
        currentSpellContainers = result;
    }

    @Override
    public SpellContainerSource.Result getSpellContainers() {
        return currentSpellContainers;
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void tick_TAIL_SpellEngine_SpellContainer(CallbackInfo ci) {
        var player = (PlayerEntity) (Object) this;
        SpellContainerSource.update(player);
    }
}
