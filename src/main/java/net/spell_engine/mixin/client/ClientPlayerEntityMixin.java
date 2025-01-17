package net.spell_engine.mixin.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.spell_engine.SpellEngineMod;
import net.spell_engine.api.effect.EntityActionsAllowed;
import net.spell_engine.api.spell.Spell;
import net.spell_engine.client.SpellEngineClient;
import net.spell_engine.client.input.SpellHotbar;
import net.spell_engine.internals.SpellHelper;
import net.spell_engine.internals.casting.SpellCast;
import net.spell_engine.internals.casting.SpellCasterClient;
import net.spell_engine.network.Packets;
import net.spell_engine.utils.TargetHelper;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

@Mixin(ClientPlayerEntity.class)
public abstract class ClientPlayerEntityMixin implements SpellCasterClient {
    @Shadow @Final public ClientPlayNetworkHandler networkHandler;
    private TargetHelper.SpellTargetResult spellTarget = TargetHelper.SpellTargetResult.empty();

    private ClientPlayerEntity player() {
        return (ClientPlayerEntity) ((Object) this);
    }

    private Entity firstTarget() {
        return spellTarget.entities().stream().findFirst().orElse(null);
    }

    @Override
    @Nullable public SpellCast.Process getSpellCastProcess() {
        return spellCastProcess;
    }

    @Override
    public Spell getCurrentSpell() {
        if (spellCastProcess != null) {
            return spellCastProcess.spell().value();
        }
        return null;
    }

    @Override
    public float getCurrentCastingSpeed() {
        if (spellCastProcess != null) {
            return spellCastProcess.speed();
        }
        return 1F;
    }

    public boolean isCastingSpell() {
        return spellCastProcess != null;
    }

    @Nullable private SpellCast.Process spellCastProcess;

    private void setSpellCastProcess(SpellCast.Process newValue, boolean sync) {
        var oldValue = spellCastProcess;
        spellCastProcess = newValue;
        if (sync && !Objects.equals(oldValue, newValue)) {
            Identifier id = null;
            float speed = 0;
            int length = 0;
            if (newValue != null) {
                id = newValue.spell().getKey().get().getValue();
                speed = newValue.speed();
                length = newValue.length();
            }
            ClientPlayNetworking.send(new Packets.SpellCastSync(id, speed, length));
        }
    }

    public SpellCast.Attempt startSpellCast(ItemStack itemStack, RegistryEntry<Spell> spellEntry) {
        var caster = player();
        if (caster.isSpectator()) {
            return SpellCast.Attempt.none();
        }
        if (spellEntry == null) {
            this.cancelSpellCast();
            return SpellCast.Attempt.none();
        }
        var spell = spellEntry.value();
        var spellId = spellEntry.getKey().get().getValue();
        if ((spellCastProcess != null && spellCastProcess.id().equals(spellId))
                || spell == null) {
            return SpellCast.Attempt.none();
        }
        if (EntityActionsAllowed.isImpaired(caster, EntityActionsAllowed.Player.CAST_SPELL, true)) {
            return SpellCast.Attempt.none();
        }
        var attempt = SpellHelper.attemptCasting(caster, itemStack, spellId);
        if (attempt.isSuccess()) {
            if (spellCastProcess != null) {
                // Cancel previous spell
                cancelSpellCast(false);
            }
            var instant = spell.cast.duration <= 0;
            if (instant) {
                // Release instant spell
                var process = new SpellCast.Process(spellEntry, itemStack.getItem(), 1, 0, caster.getWorld().getTime());
                this.setSpellCastProcess(process, false);
                this.updateSpellCast();
                applyInstantGlobalCooldown();
            } else {
                // Start casting
                var details = SpellHelper.getCastTimeDetails(caster, spell);
                setSpellCastProcess(new SpellCast.Process(spellEntry, itemStack.getItem(), details.speed(), details.length(), caster.getWorld().getTime()), true);
            }
        }
        return attempt;
    }

    private void applyInstantGlobalCooldown() {
        var duration = SpellEngineMod.config.spell_instant_cast_gcd;
        if (duration > 0) {
            for (var slot: SpellHotbar.INSTANCE.slots) {
                var spellEntry = slot.spell();
                if (spellEntry.value().cast != null && spellEntry.value().cast.duration <= 0) {
                    getCooldownManager().set(spellEntry.getKey().get().getValue(), duration, false);
                }
            }
        }
    }

    @Nullable public SpellCast.Progress getSpellCastProgress() {
        if (spellCastProcess != null) {
            var player = player();
            return spellCastProcess.progress(player.getWorld().getTime());
        }
        return null;
    }

    public void cancelSpellCast() {
        cancelSpellCast(true);
    }
    public void cancelSpellCast(boolean syncProcess) {
        var process = spellCastProcess;
        if (process != null) {
            if (SpellHelper.isChanneled(process.spell().value())) {
                var player = player();
                var progress = process.progress(player.getWorld().getTime());
                ClientPlayNetworking.send(new Packets.SpellRequest(SpellCast.Action.RELEASE, process.id(), progress.ratio(), new int[]{}, null));
            }
        }

        setSpellCastProcess(null, syncProcess);
        spellTarget = TargetHelper.SpellTargetResult.empty();
    }

    private void updateSpellCast() {
        var process = spellCastProcess;
        if (process != null) {
            var player = player();
            if (!player().isAlive()
                    || player.getMainHandStack().getItem() != process.item()
                    || getCooldownManager().isCoolingDown(process.id())
                    || EntityActionsAllowed.isImpaired(player, EntityActionsAllowed.Player.CAST_SPELL, true)
            ) {
                cancelSpellCast();
                return;
            }
            var spell = process.spell().value();

            spellTarget = findTargets(spell);

            var spellCastTicks = process.spellCastTicksSoFar(player.getWorld().getTime());
            if (SpellHelper.isChanneled(spell)) {
                // Is channel tick due?
                var offset = Math.round(spell.cast.channel_ticks * 0.5F);
                var currentTick = spellCastTicks + offset;
                var isDue = currentTick >= spell.cast.channel_ticks
                        && (currentTick % spell.cast.channel_ticks) == 0;
                if (isDue) {
                    // Channel spell
                    releaseSpellCast(process, SpellCast.Action.CHANNEL);
                }
            } else {
                var isFinished = spellCastTicks >= process.length();
                if (isFinished) {
                    // Release spell
                    releaseSpellCast(process, SpellCast.Action.RELEASE);
                }
            }
        } else {
            spellTarget = TargetHelper.SpellTargetResult.empty();
        }
    }

    private void releaseSpellCast(SpellCast.Process process, SpellCast.Action action) {
        var spellId = process.id();
        var spell = process.spell().value();
        var player = player();
        var progress = process.progress(player.getWorld().getTime());
        var release = spell.release.target;
        var targets = spellTarget.entities();
        var location = spellTarget.location();
        int[] targetIDs = new int[]{};
        switch (release.type) {
            case PROJECTILE, CURSOR, METEOR -> {
                var firstTarget = firstTarget();
                if (firstTarget != null) {
                    targetIDs = new int[]{ firstTarget.getId() };
                }
            }
            case AREA, BEAM -> {
                targetIDs = new int[targets.size()];
                int i = 0;
                for (var target : targets) {
                    targetIDs[i] = target.getId();
                    i += 1;
                }
            }
            case SELF, SHOOT_ARROW -> {
            }
        }
        ClientPlayNetworking.send(new Packets.SpellRequest(action, spellId, progress.ratio(), targetIDs, location));
        switch (action) {
            case CHANNEL -> {
                if (progress.ratio() >= 1) {
                    cancelSpellCast();
                }
            }
            case RELEASE -> {
                cancelSpellCast();
            }
        }
    }

    public List<Entity> getCurrentTargets() {
        var targets = spellTarget.entities();
        if (targets == null) {
            return List.of();
        }
        return targets;
    }

    public Entity getCurrentFirstTarget() {
        return firstTarget();
    }

    private int findSlot(PlayerEntity player, ItemStack stack) {
        for(int i = 0; i < player.getInventory().size(); ++i) {
            ItemStack itemStack = player.getInventory().getStack(i);
            if (stack == itemStack) {
                return i;
            }
        }
        return -1;
    }

    private TargetHelper.SpellTargetResult findTargets(Spell currentSpell) {
        var caster = player();
        var previousTargets = spellTarget.entities();
        List<Entity> targets = List.of();
        Vec3d location = null;
        if (currentSpell == null || currentSpell.impact == null) {
            return new TargetHelper.SpellTargetResult(targets, location);
        }
        boolean fallbackToPreviousTargets = false;
        var targetingMode = SpellHelper.selectionTargetingMode(currentSpell);
        var targetType = currentSpell.release.target.type;
        var range = SpellHelper.getRange(caster, currentSpell) * player().getScale();

        Predicate<Entity> selectionPredicate = (target) -> {
            boolean intentAllows = false;
            for (var impact: currentSpell.impact) {
                var intent = SpellHelper.intent(impact.action);
                var newValue = impact.action.apply_to_caster
                        ? target == caster
                        : TargetHelper.actionAllowed(targetingMode, intent, caster, target);
                intentAllows = intentAllows || newValue;
            }
            return !SpellEngineClient.config.filterInvalidTargets || intentAllows;
        };
        switch (targetType) {
            case AREA -> {
                targets = TargetHelper.targetsFromArea(caster, range, currentSpell.release.target.area, selectionPredicate);
                var area = currentSpell.release.target.area;
                if (area != null && area.include_caster) {
                    targets.add(caster);
                }
            }
            case BEAM -> {
                targets = TargetHelper.targetsFromRaycast(caster, range, selectionPredicate);
            }
            case CURSOR, PROJECTILE, METEOR -> {
                fallbackToPreviousTargets = targetType != Spell.Release.Target.Type.PROJECTILE; // All of these except `PROJECTILE`
                var target = TargetHelper.targetFromRaycast(caster, range, selectionPredicate);
                if (target != null) {
                    targets = List.of(target);
                } else {
                    targets = List.of();
                }
            }
            case SELF, CLOUD, SHOOT_ARROW -> {
                // Nothing to do
            }
        }
        if (fallbackToPreviousTargets && SpellEngineClient.config.stickyTarget
                && targets.isEmpty()) {
            targets = previousTargets.stream()
                    .filter(entity -> {
                        return TargetHelper.isInLineOfSight(caster, entity) && !entity.isRemoved();
                    })
                    .toList();
        }

        var cursor = currentSpell.release.target.cursor;
        if (cursor != null) {
            if (cursor.use_caster_as_fallback && targets.isEmpty()) {
                targets = List.of(caster);
            }
        }

        if (targetType == Spell.Release.Target.Type.METEOR
                && targets.isEmpty()
                && currentSpell.release.target.meteor != null
                && !currentSpell.release.target.meteor.requires_entity) {
            location = TargetHelper.locationFromRayCast(caster, range);
        }

        return new TargetHelper.SpellTargetResult(targets, location);
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void tick_TAIL_SpellEngine(CallbackInfo ci) {
        updateSpellCast();
        var player = player();
        if (isBeaming()) {
            networkHandler.sendPacket(new PlayerMoveC2SPacket.Full(
                    player.getX(), player.getY(), player.getZ(),
                    player.getYaw(), player.getPitch(),
                    player.isOnGround())
            );
        }
    }
}