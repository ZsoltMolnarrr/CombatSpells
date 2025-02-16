package net.spell_engine.network;

import com.google.gson.Gson;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.spell_engine.SpellEngineMod;
import net.spell_engine.api.spell.fx.ParticleBatch;
import net.spell_engine.config.ServerConfig;
import net.spell_engine.internals.SpellCooldownManager;
import net.spell_engine.internals.casting.SpellCast;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Packets {

    public record SpellCastSync(Identifier spellId, float speed, int length) implements CustomPayload {
        public static Identifier ID = Identifier.of(SpellEngineMod.ID, "cast_sync");
        public static final CustomPayload.Id<SpellCastSync> PACKET_ID = new CustomPayload.Id<>(ID);
        public static final PacketCodec<RegistryByteBuf, SpellCastSync> CODEC = PacketCodec.of(SpellCastSync::write, SpellCastSync::read);
        @Override
        public Id<? extends CustomPayload> getId() {
            return PACKET_ID;
        }

        public void write(RegistryByteBuf buffer) {
            if (spellId == null) {
                buffer.writeString("");
            } else {
                buffer.writeString(spellId.toString());
            }
            buffer.writeFloat(speed);
            buffer.writeInt(length);
        }

        public static SpellCastSync read(RegistryByteBuf buffer) {
            var string = buffer.readString();
            Identifier spellId = null;
            if (!string.isEmpty()) {
                spellId = Identifier.of(string);
            }
            var speed = buffer.readFloat();
            var length = buffer.readInt();
            return new SpellCastSync(spellId, speed, length);
        }
    }

    public record SpellRequest(SpellCast.Action action, Identifier spellId, float progress, int[] targets, @Nullable Vec3d location) implements CustomPayload {
        public static Identifier ID = Identifier.of(SpellEngineMod.ID, "release_request");
        public static final CustomPayload.Id<SpellRequest> PACKET_ID = new CustomPayload.Id<>(ID);
        public static final PacketCodec<RegistryByteBuf, SpellRequest> CODEC = PacketCodec.of(SpellRequest::write, SpellRequest::read);
        @Override
        public Id<? extends CustomPayload> getId() {
            return PACKET_ID;
        }

        public void write(RegistryByteBuf buffer) {
            buffer.writeEnumConstant(action);
            buffer.writeString(spellId.toString());
            buffer.writeFloat(progress);
            buffer.writeIntArray(targets);

            if (location != null) {
                buffer.writeBoolean(true);
                buffer.writeDouble(location.x);
                buffer.writeDouble(location.y);
                buffer.writeDouble(location.z);
            } else {
                buffer.writeBoolean(false);
            }
        }

        public static SpellRequest read(RegistryByteBuf buffer) {
            var action = buffer.readEnumConstant(SpellCast.Action.class);
            var spellId = Identifier.of(buffer.readString());
            var progress = buffer.readFloat();
            var targets = buffer.readIntArray();

            Vec3d location = null;
            var hasLocation = buffer.readBoolean();
            if (hasLocation) {
                var x = buffer.readDouble();
                var y = buffer.readDouble();
                var z = buffer.readDouble();
                location = new Vec3d(x, y, z);
            }
            return new SpellRequest(action, spellId, progress, targets, location);
        }
    }

    public record SpellCooldown(Identifier spellId, int duration) implements CustomPayload {
        public static Identifier ID = Identifier.of(SpellEngineMod.ID, "spell_cooldown");
        public static final CustomPayload.Id<SpellCooldown> PACKET_ID = new CustomPayload.Id<>(ID);
        public static final PacketCodec<RegistryByteBuf, SpellCooldown> CODEC = PacketCodec.of(SpellCooldown::write, SpellCooldown::read);
        @Override
        public Id<? extends CustomPayload> getId() {
            return PACKET_ID;
        }

        public void write(RegistryByteBuf buffer) {
            buffer.writeString(spellId.toString());
            buffer.writeInt(duration);
        }

        public static SpellCooldown read(RegistryByteBuf buffer) {
            var spellId = Identifier.of(buffer.readString());
            int duration = buffer.readInt();
            return new SpellCooldown(spellId, duration);
        }
    }

    public record SpellCooldownSync(int baseTick, Map<Identifier, SpellCooldownManager.Entry> cooldowns) implements CustomPayload {
        public static Identifier ID = Identifier.of(SpellEngineMod.ID, "cooldown_sync");
        public static final CustomPayload.Id<SpellCooldownSync> PACKET_ID = new CustomPayload.Id<>(ID);
        public static final PacketCodec<RegistryByteBuf, SpellCooldownSync> CODEC = PacketCodec.of(SpellCooldownSync::write, SpellCooldownSync::read);
        @Override
        public Id<? extends CustomPayload> getId() {
            return PACKET_ID;
        }

        public void write(RegistryByteBuf buffer) {
            buffer.writeInt(baseTick);
            buffer.writeInt(cooldowns.size());
            for (var entry: cooldowns.entrySet()) {
                buffer.writeString(entry.getKey().toString());
                buffer.writeInt(entry.getValue().startTick());
                buffer.writeInt(entry.getValue().endTick());
            }
        }

        public static SpellCooldownSync read(RegistryByteBuf buffer) {
            int baseTick = buffer.readInt();
            int size = buffer.readInt();
            var cooldowns = new HashMap<Identifier, SpellCooldownManager.Entry>();
            for (int i = 0; i < size; ++i) {
                var spellId = Identifier.of(buffer.readString());
                var startTick = buffer.readInt();
                var endTick = buffer.readInt();
                cooldowns.put(spellId, new SpellCooldownManager.Entry(startTick, endTick));
            }
            return new SpellCooldownSync(baseTick, cooldowns);
        }
    }

    public record SpellAnimation(int playerId, SpellCast.Animation type, String name, float speed) implements CustomPayload {
        public static Identifier ID = Identifier.of(SpellEngineMod.ID, "spell_animation");
        public static final CustomPayload.Id<SpellAnimation> PACKET_ID = new CustomPayload.Id<>(ID);
        public static final PacketCodec<RegistryByteBuf, SpellAnimation> CODEC = PacketCodec.of(SpellAnimation::write, SpellAnimation::read);
        @Override
        public Id<? extends CustomPayload> getId() {
            return PACKET_ID;
        }

        public void write(RegistryByteBuf buffer) {
            buffer.writeInt(playerId);
            buffer.writeInt(type.ordinal());
            buffer.writeString(name);
            buffer.writeFloat(speed);
        }

        public static SpellAnimation read(RegistryByteBuf buffer) {
            int playerId = buffer.readInt();
            var type = SpellCast.Animation.values()[buffer.readInt()];
            var name = buffer.readString();
            var speed = buffer.readFloat();
            return new SpellAnimation(playerId, type, name, speed);
        }
    }

    public record ParticleBatches(SourceType sourceType, float countMultiplier, List<Spawn> spawns) implements CustomPayload {
        public static Identifier ID = Identifier.of(SpellEngineMod.ID, "particle_effects");
        public static final CustomPayload.Id<ParticleBatches> PACKET_ID = new CustomPayload.Id<>(ID);
        public static final PacketCodec<RegistryByteBuf, ParticleBatches> CODEC = PacketCodec.of(ParticleBatches::write, ParticleBatches::read);
        @Override
        public Id<? extends CustomPayload> getId() {
            return PACKET_ID;
        }

        public enum SourceType { ENTITY, COORDINATE }
        public record Spawn(int sourceEntityId, float yaw, float pitch, Vec3d sourceLocation, ParticleBatch batch) { }


        public void write(RegistryByteBuf buffer) {
            buffer.writeInt(sourceType.ordinal());
            buffer.writeInt(spawns.size());
            for (var spawn: spawns) {
                buffer.writeInt(spawn.sourceEntityId);
                buffer.writeFloat(spawn.yaw);
                buffer.writeFloat(spawn.pitch);
                buffer.writeDouble(spawn.sourceLocation.x);
                buffer.writeDouble(spawn.sourceLocation.y);
                buffer.writeDouble(spawn.sourceLocation.z);
                write(spawn.batch, buffer, countMultiplier);
            }
        }

        private static void write(ParticleBatch batch, PacketByteBuf buffer, float countMultiplier) {
            buffer.writeString(batch.particle_id);
            buffer.writeInt(batch.shape.ordinal());
            buffer.writeInt(batch.origin.ordinal());
            buffer.writeInt(batch.rotation != null ? batch.rotation.ordinal() : -1);
            buffer.writeFloat(batch.roll);
            buffer.writeFloat(batch.roll_offset);
            buffer.writeFloat(batch.count * countMultiplier);
            buffer.writeFloat(batch.min_speed);
            buffer.writeFloat(batch.max_speed);
            buffer.writeFloat(batch.angle);
            buffer.writeFloat(batch.extent);
            buffer.writeFloat(batch.pre_spawn_travel);
            buffer.writeBoolean(batch.invert);
        }

        private static ParticleBatch readBatch(RegistryByteBuf buffer) {
            return new ParticleBatch(
                    buffer.readString(),
                    ParticleBatch.Shape.values()[buffer.readInt()],
                    ParticleBatch.Origin.values()[buffer.readInt()],
                    ParticleBatch.Rotation.from(buffer.readInt()),
                    buffer.readFloat(),
                    buffer.readFloat(),
                    buffer.readFloat(),
                    buffer.readFloat(),
                    buffer.readFloat(),
                    buffer.readFloat(),
                    buffer.readFloat(),
                    buffer.readFloat(),
                    buffer.readBoolean()
            );
        }

        public static ParticleBatches read(RegistryByteBuf buffer) {
            var sourceType = SourceType.values()[buffer.readInt()];
            var spawnCount = buffer.readInt();
            var spawns = new ArrayList<Spawn>();
            for (int i = 0; i < spawnCount; ++i) {
                spawns.add(new Spawn(
                        buffer.readInt(),
                        buffer.readFloat(),
                        buffer.readFloat(),
                        new Vec3d(buffer.readDouble(), buffer.readDouble(), buffer.readDouble()),
                        readBatch(buffer)
                ));
            }
            return new ParticleBatches(sourceType, 1, spawns);
        }
    }

    public record ConfigSync(ServerConfig config) implements CustomPayload {
        public static Identifier ID = Identifier.of(SpellEngineMod.ID, "config_sync");
        public static final CustomPayload.Id<ConfigSync> PACKET_ID = new CustomPayload.Id<>(ID);
        public static final PacketCodec<PacketByteBuf, ConfigSync> CODEC = PacketCodec.of(ConfigSync::write, ConfigSync::read);
        @Override
        public Id<? extends CustomPayload> getId() {
            return PACKET_ID;
        }

        private static final Gson gson = new Gson();

        public void write(PacketByteBuf buffer) {
            var json = gson.toJson(this.config);
            buffer.writeString(json);
        }

        public static ConfigSync read(PacketByteBuf buffer) {
            var gson = new Gson();
            var json = buffer.readString();
            var config = gson.fromJson(json, ServerConfig.class);
            return new ConfigSync(config);
        }
    }

    public record SpellRegistrySync(List<String> chunks) implements CustomPayload {
        public static Identifier ID = Identifier.of(SpellEngineMod.ID, "spell_registry_sync");
        public static final CustomPayload.Id<SpellRegistrySync> PACKET_ID = new CustomPayload.Id<>(ID);
        public static final PacketCodec<PacketByteBuf, SpellRegistrySync> CODEC = PacketCodec.of(SpellRegistrySync::write, SpellRegistrySync::read);

        private void write(PacketByteBuf buffer) {
            buffer.writeInt(chunks.size());
            for (var chunk: chunks) {
                buffer.writeString(chunk);
            }
        }

        private static SpellRegistrySync read(PacketByteBuf buffer) {
            var chunkCount = buffer.readInt();
            var chunks = new ArrayList<String>();
            for (int i = 0; i < chunkCount; ++i) {
                chunks.add(buffer.readString());
            }
            return new SpellRegistrySync(chunks);
        }

        @Override
        public Id<? extends CustomPayload> getId() {
            return PACKET_ID;
        }
    }

    public record Ack(String code) implements CustomPayload {
        public static Identifier ID = Identifier.of(SpellEngineMod.ID, "ack");
        public static final CustomPayload.Id<Ack> PACKET_ID = new CustomPayload.Id<>(ID);
        public static final PacketCodec<PacketByteBuf, Ack> CODEC = PacketCodec.of(Ack::write, Ack::read);

        public void write(PacketByteBuf buffer) {
            buffer.writeString(code);
        }

        public static Ack read(PacketByteBuf buffer) {
            var code = buffer.readString();
            return new Ack(code);
        }

        @Override
        public Id<? extends CustomPayload> getId() {
            return PACKET_ID;
        }
    }
}
