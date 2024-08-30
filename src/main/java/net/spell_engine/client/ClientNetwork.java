package net.spell_engine.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.spell_engine.SpellEngineMod;
import net.spell_engine.client.animation.AnimatablePlayer;
import net.spell_engine.internals.casting.SpellCasterEntity;
import net.spell_engine.internals.SpellRegistry;
import net.spell_engine.network.Packets;
import net.spell_engine.network.ServerNetwork;
import net.spell_engine.particle.ParticleHelper;

public class ClientNetwork {
    public static void initializeHandlers() {
        PayloadTypeRegistry.configurationS2C().register(Packets.ConfigSync.PACKET_ID, Packets.ConfigSync.CODEC);
        ClientConfigurationNetworking.registerGlobalReceiver(Packets.ConfigSync.PACKET_ID, (packet, context) -> {
            SpellEngineMod.config = packet.config();
            context.responseSender().sendPacket(new Packets.Ack(ServerNetwork.ConfigurationTask.name));
        });
        
        PayloadTypeRegistry.configurationS2C().register(Packets.SpellRegistrySync.PACKET_ID, Packets.SpellRegistrySync.CODEC);
        ClientConfigurationNetworking.registerGlobalReceiver(Packets.SpellRegistrySync.PACKET_ID, (packet, context) -> {
            SpellRegistry.decodeContent(packet.chunks());
            context.responseSender().sendPacket(new Packets.Ack(ServerNetwork.SpellRegistrySyncTask.name));
        });


        PayloadTypeRegistry.playS2C().register(Packets.ParticleBatches.PACKET_ID, Packets.ParticleBatches.CODEC);
        ClientPlayNetworking.registerGlobalReceiver(Packets.ParticleBatches.PACKET_ID, (packet, context) -> {
            var client = context.client();
            var instructions = ParticleHelper.convertToInstructions(client.world, packet);
            client.execute(() -> {
                for(var instruction: instructions) {
                    instruction.perform(client.world);
                }
            });
        });

        PayloadTypeRegistry.playS2C().register(Packets.SpellAnimation.PACKET_ID, Packets.SpellAnimation.CODEC);
        ClientPlayNetworking.registerGlobalReceiver(Packets.SpellAnimation.PACKET_ID, (packet, context) -> {
            var client = context.client();
            client.execute(() -> {
                var entity = client.world.getEntityById(packet.playerId());
                if (entity instanceof PlayerEntity player) {
                    ((AnimatablePlayer)player).playSpellAnimation(packet.type(), packet.name(), packet.speed());
                }
            });
        });

        PayloadTypeRegistry.playS2C().register(Packets.SpellCooldown.PACKET_ID, Packets.SpellCooldown.CODEC);
        ClientPlayNetworking.registerGlobalReceiver(Packets.SpellCooldown.PACKET_ID, (packet, context) -> {
            var client = context.client();
            client.execute(() -> {
                ((SpellCasterEntity)client.player).getCooldownManager().set(packet.spellId(), packet.duration());
            });
        });
    }
}
