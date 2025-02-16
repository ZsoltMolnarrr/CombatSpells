package net.spell_engine.internals;

import com.google.common.collect.Maps;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.spell_engine.network.Packets;

import java.util.Iterator;
import java.util.Map;

public class SpellCooldownManager {
    public record Entry(int startTick, int endTick) {
        int timeLeft(int currentTick) {
            return Math.max(0, endTick - currentTick);
        }
    }
    private final Map<Identifier, Entry> entries = Maps.newHashMap();
    private int tick;

    private final PlayerEntity owner;

    public SpellCooldownManager(PlayerEntity owner) {
        this.owner = owner;
    }

    public boolean isCoolingDown(Identifier spell) {
        return this.getCooldownProgress(spell, 0.0f) > 0.0f;
    }

    public float getCooldownProgress(Identifier spell, float tickDelta) {
        SpellCooldownManager.Entry entry = this.entries.get(spell);
        if (entry != null) {
            float f = entry.endTick - entry.startTick;
            float g = (float)entry.endTick - ((float)this.tick + tickDelta);
            return MathHelper.clamp(g / f, 0.0f, 1.0f);
        }
        return 0.0f;
    }

    public void update() {
        ++this.tick;
        if (!this.entries.isEmpty()) {
            Iterator<Map.Entry<Identifier, Entry>> iterator = this.entries.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<Identifier, SpellCooldownManager.Entry> entry = iterator.next();
                if (entry.getValue().endTick > this.tick) continue;
                iterator.remove();
                this.cooldownCleared(entry.getKey());
            }
        }
    }

    public void set(Identifier spell, int duration, boolean force) {
        if (force
                || !this.entries.containsKey(spell)
                || (this.entries.get(spell).timeLeft(tick) < duration)
        ) {
            this.entries.put(spell, new SpellCooldownManager.Entry(this.tick, this.tick + duration));
            this.cooldownSet(spell, duration);
        }
    }

    public void set(Identifier spell, int duration) {
        this.set(spell, duration, true);
    }

    public void remove(Identifier spell) {
        this.entries.remove(spell);
        this.cooldownCleared(spell);
    }

    protected void cooldownSet(Identifier spell, int duration) {
        if (owner instanceof ServerPlayerEntity serverPlayer) {
            ServerPlayNetworking.send(serverPlayer, new Packets.SpellCooldown(spell, duration));
        }
    }

    protected void cooldownCleared(Identifier spell) {
        if (owner instanceof ServerPlayerEntity serverPlayer) {
            ServerPlayNetworking.send(serverPlayer, new Packets.SpellCooldown(spell, 0));
        }
    }

    private static final String NBT_KEY = "spell_engine_cooldowns";
    public void writeCustomDataToNbt(NbtCompound nbt) {
        NbtCompound cooldowns = new NbtCompound();
        for (var entry: entries.entrySet()) {
            var spell = entry.getKey();
            var cooldown = entry.getValue();
            NbtCompound cooldownData = new NbtCompound();
            cooldownData.putInt("start", cooldown.startTick - tick);
            cooldownData.putInt("end", cooldown.endTick - tick);
            cooldowns.put(spell.toString(), cooldownData);
        }
        nbt.put(NBT_KEY, cooldowns);
    }

    public void readCustomDataFromNbt(NbtCompound nbt) {
        NbtCompound cooldowns = nbt.getCompound(NBT_KEY);
        for (var key: cooldowns.getKeys()) {
            var spell = Identifier.of(key);
            var cooldownData = cooldowns.getCompound(key);
            var start = cooldownData.getInt("start");
            var end = cooldownData.getInt("end");
            entries.put(spell, new Entry(start, end));
        }
    }

    public void pushSync() {
        if (owner instanceof ServerPlayerEntity serverPlayer) {
            ServerPlayNetworking.send(serverPlayer, new Packets.SpellCooldownSync(this.tick, Map.copyOf(this.entries)));
        }
    }

    public void acceptSync(int baseTick, Map<Identifier, Entry> cooldowns) {
        this.tick = baseTick;
        this.entries.clear();
        this.entries.putAll(cooldowns);
    }
}
