package net.spell_engine.network;

import com.google.common.collect.Iterables;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.Packet;
import net.minecraft.server.network.ServerPlayerConfigurationTask;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.spell_engine.SpellEngineMod;
import net.spell_engine.config.ServerConfig;
import net.spell_engine.internals.casting.SpellCastSyncHelper;
import net.spell_engine.internals.SpellHelper;
import net.spell_engine.internals.container.SpellAssignments;
import net.spell_engine.utils.TargetHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ServerNetwork {
    public static void initializeHandlers() {

        // Config stage

        PayloadTypeRegistry.configurationS2C().register(Packets.ConfigSync.PACKET_ID, Packets.ConfigSync.CODEC);
        PayloadTypeRegistry.configurationS2C().register(Packets.SpellRegistrySync.PACKET_ID, Packets.SpellRegistrySync.CODEC);
        PayloadTypeRegistry.configurationC2S().register(Packets.Ack.PACKET_ID, Packets.Ack.CODEC);

        ServerConfigurationConnectionEvents.CONFIGURE.register((handler, server) -> {
            // This if block is required! Otherwise the client gets stuck in connection screen
            // if the client cannot handle the packet.
            if (ServerConfigurationNetworking.canSend(handler, Packets.ConfigSync.ID)) {
                // System.out.println("Starting ConfigurationTask");
                handler.addTask(new ConfigurationTask(SpellEngineMod.config));
            } else {
                handler.disconnect(Text.literal("Network configuration task not supported: " + ConfigurationTask.name));
            }
        });
        ServerConfigurationConnectionEvents.CONFIGURE.register((handler, server) -> {
            if (ServerConfigurationNetworking.canSend(handler, Packets.SpellRegistrySync.ID)) {
                if (SpellAssignments.encoded.isEmpty()) {
                    throw new AssertionError("Spell registry is empty!");
                }
                // System.out.println("Starting WeaponRegistrySyncTask, chunks: " + WeaponRegistry.getEncodedRegistry().chunks().size());
                handler.addTask(new SpellRegistrySyncTask(SpellAssignments.encoded));
            } else {
                handler.disconnect(Text.literal("Network configuration task not supported: " + SpellRegistrySyncTask.name));
            }
        });
        ServerConfigurationNetworking.registerGlobalReceiver(Packets.Ack.PACKET_ID, (packet, context) -> {
            // Warning: if you do not call completeTask, the client gets stuck!
            if (packet.code().equals(ConfigurationTask.name)) {
                context.networkHandler().completeTask(ConfigurationTask.KEY);
            }
            if (packet.code().equals(SpellRegistrySyncTask.name)) {
                context.networkHandler().completeTask(SpellRegistrySyncTask.KEY);
            }
        });

        // Play stage

        PayloadTypeRegistry.playC2S().register(Packets.SpellCastSync.PACKET_ID, Packets.SpellCastSync.CODEC);
        PayloadTypeRegistry.playC2S().register(Packets.SpellRequest.PACKET_ID, Packets.SpellRequest.CODEC);
        PayloadTypeRegistry.playS2C().register(Packets.SpellCooldown.PACKET_ID, Packets.SpellCooldown.CODEC);
        PayloadTypeRegistry.playS2C().register(Packets.ParticleBatches.PACKET_ID, Packets.ParticleBatches.CODEC);
        PayloadTypeRegistry.playS2C().register(Packets.SpellAnimation.PACKET_ID, Packets.SpellAnimation.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(Packets.SpellCastSync.PACKET_ID, (packet, context) -> {
            var server = context.server();
            var player = context.player();

            ServerWorld world = Iterables.tryFind(server.getWorlds(), (element) -> element == player.getWorld())
                    .orNull();
            if (world == null || world.isClient) {
                return;
            }

            world.getServer().executeSync(() -> {
                if (packet.spellId() == null) {
                    SpellCastSyncHelper.clearCasting(player);
                } else {
                    SpellHelper.startCasting(player, packet.spellId(), packet.speed(), packet.length());
                }
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(Packets.SpellRequest.PACKET_ID, (packet, context) -> {
            var server = context.server();
            var player = context.player();

            ServerWorld world = Iterables.tryFind(server.getWorlds(), (element) -> element == player.getWorld())
                    .orNull();
            if (world == null || world.isClient) {
                return;
            }

            world.getServer().executeSync(() -> {
                List<Entity> targets = new ArrayList<>();
                for (var targetId: packet.targets()) {
                    // var entity = world.getEntityById(targetId);
                    var entity = world.getDragonPart(targetId); // Retrieves `getEntityById` + dragon parts :)
                    if (entity != null) {
                        targets.add(entity);
                    } else {
                        System.err.println("Spell Engine: Trying to perform spell " + packet.spellId().toString() + " Entity not found: " + targetId);
                    }
                }
                var target = new TargetHelper.SpellTargetResult(targets, packet.location());
                SpellHelper.performSpell(world, player, packet.spellId(), target, packet.action(), packet.progress());
            });
        });
    }

    public record ConfigurationTask(ServerConfig config) implements ServerPlayerConfigurationTask {
        public static final String name = SpellEngineMod.ID + ":" + "config";
        public static final Key KEY = new Key(name);

        @Override
        public Key getKey() {
            return KEY;
        }

        @Override
        public void sendPacket(Consumer<Packet<?>> sender) {
            var packet = new Packets.ConfigSync(this.config);
            sender.accept(ServerConfigurationNetworking.createS2CPacket(packet));
        }
    }

    public record SpellRegistrySyncTask(List<String> encodedChunks) implements ServerPlayerConfigurationTask {
        public static final String name = SpellEngineMod.ID + ":" + "spell_registry";
        public static final Key KEY = new Key(name);

        @Override
        public Key getKey() {
            return KEY;
        }

        @Override
        public void sendPacket(Consumer<Packet<?>> sender) {
            var packet = new Packets.SpellRegistrySync(encodedChunks);
            sender.accept(ServerConfigurationNetworking.createS2CPacket(packet));
        }
    }
}
