package net.spell_engine.client.render;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.spell_engine.api.render.CustomLayers;
import net.spell_engine.api.render.LightEmission;
import net.spell_engine.api.spell.Spell;
import net.spell_engine.client.beam.BeamEmitterEntity;
import net.spell_engine.internals.Beam;
import net.spell_engine.internals.SpellHelper;
import net.spell_engine.internals.casting.SpellCasterEntity;
import net.spell_engine.utils.TargetHelper;

public class BeamRenderer extends RenderLayer {
    public static void setup() {
        WorldRenderEvents.AFTER_TRANSLUCENT.register(context -> {
            VertexConsumerProvider.Immediate vcProvider = MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers();
            renderAllInWorld(context, context.matrixStack(), vcProvider, context.camera(), LightmapTextureManager.MAX_LIGHT_COORDINATE, context.tickCounter().getTickDelta(true));
        });
    }

    public static void renderAllInWorld(WorldRenderContext context, MatrixStack matrices, VertexConsumerProvider.Immediate vertexConsumers, Camera camera, int light, float delta) {
        var focusedEntity = context.camera().getFocusedEntity();
        if (focusedEntity == null) {
            return;
        }

        var renderDistance = MinecraftClient.getInstance().options.getViewDistance().getValue() * 24; // 24 = 16 * 1.5F
        var squaredRenderDistance = renderDistance * renderDistance;
        var players = context.world().getPlayers()
                .stream().filter(player ->
                        player.squaredDistanceTo(focusedEntity) < squaredRenderDistance
                && ((SpellCasterEntity)player).getBeam() != null)
                .toList();
        if (players.isEmpty()) {
            return;
        }

        matrices.push();
        Vec3d camPos = camera.getPos();
        matrices.translate(-camPos.x, -camPos.y, -camPos.z);
        for (var livingEntity : players) {
            var launchHeight = SpellHelper.launchHeight(livingEntity);
            var offset = new Vec3d(0.0, launchHeight, SpellHelper.launchPointOffsetDefault);
            SpellCasterEntity caster = (SpellCasterEntity)livingEntity;
            matrices.push();
            var pos = new Vec3d(livingEntity.prevX, livingEntity.prevY, livingEntity.prevZ)
                    .lerp(livingEntity.getPos(), delta);
            matrices.translate(pos.x, pos.y, pos.z);

            Vec3d from = livingEntity.getPos().add(0, launchHeight, 0);
            var lookVector = Vec3d.ZERO;
            if (livingEntity == MinecraftClient.getInstance().player) {
                // No lerp for local player
                lookVector = Vec3d.fromPolar(livingEntity.getPitch(), livingEntity.getYaw());
            } else {
                lookVector = Vec3d.fromPolar(livingEntity.prevPitch, livingEntity.prevYaw);
                lookVector = lookVector.lerp(Vec3d.fromPolar(livingEntity.getPitch(), livingEntity.getYaw()), delta);
            }
            lookVector = lookVector.normalize();
            var beamPosition = TargetHelper.castBeam(livingEntity, lookVector, 32);
            lookVector = lookVector.multiply(beamPosition.length());
            Vec3d to = from.add(lookVector);

            var beamAppearance = caster.getBeam();
            renderBeamFromPlayer(matrices, vertexConsumers, beamAppearance,
                    from, to, offset, livingEntity.getWorld().getTime(), delta);
            ((BeamEmitterEntity)livingEntity).setLastRenderedBeam(new Beam.Rendered(beamPosition, beamAppearance));
            matrices.pop();
        }
        vertexConsumers.draw();
        matrices.pop();
    }

    private static void renderBeamFromPlayer(MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider,
                                             Spell.Release.Target.Beam beam,
                                             Vec3d from, Vec3d to, Vec3d offset, long time, float tickDelta) {
        var absoluteTime = (float)Math.floorMod(time, 40) + tickDelta;

        matrixStack.push();
        matrixStack.translate(0, offset.y, 0);

        Vec3d beamVector = to.subtract(from);
        float length = (float)beamVector.length();

        // Perform some rotation
        beamVector = beamVector.normalize();
        float n = (float)Math.acos(beamVector.y);
        float o = (float)Math.atan2(beamVector.z, beamVector.x);
        matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((1.5707964F - o) * 57.295776F));
        matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(n * 57.295776F));
        matrixStack.translate(0, offset.z, 0); // At this point everything is so rotated, we need to translate along y to move along z

        matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(absoluteTime * 2.25F - 45.0F));

        var texture = Identifier.of(beam.texture_id);
        var color = beam.color_rgba;
        var red = (color >> 24) & 255;
        var green = (color >> 16) & 255;
        var blue = (color >> 8) & 255;
        var alpha = color & 255;
        // System.out.println("Beam color " + " red:" + red + " green:" + green + " blue:" + blue + " alpha:" + alpha);
        BeamRenderer.renderBeam(matrixStack, vertexConsumerProvider,
                texture, time, tickDelta, beam.flow, true,
                (int)red, (int)green, (int)blue, (int)alpha,
                0, length, beam.width);

        matrixStack.pop();
    }

    public BeamRenderer(String name, VertexFormat vertexFormat, VertexFormat.DrawMode drawMode, int expectedBufferSize, boolean hasCrumbling, boolean translucent, Runnable startAction, Runnable endAction) {
        super(name, vertexFormat, drawMode, expectedBufferSize, hasCrumbling, translucent, startAction, endAction);
    }

    public static void renderBeam(MatrixStack matrices, VertexConsumerProvider vertexConsumers,
                                  Identifier texture, long time, float tickDelta, float direction, boolean center,
                                  int red, int green, int blue, int alpha,
                                  float yOffset, float height, float width) {
        matrices.push();

        float shift = (float)Math.floorMod(time, 40) + tickDelta;
        float offset = MathHelper.fractionalPart(shift * 0.2f - (float)MathHelper.floor(shift * 0.1f)) * (- direction);

//        var innerRenderLayer = CustomLayers.beam(texture, true, true); //alpha < 250);
        var outerRenderLayer = CustomLayers.beam(texture, false, true);
        var innerRenderLayer = CustomLayers.spellObject(texture, LightEmission.RADIATE, false); //alpha < 250);
//        var outerRenderLayer = CustomLayers.spellObject(texture, LightEmission.GLOW, true); //alpha < 250);

        var originalWidth = width;
        if (center) {
            renderBeamLayer(matrices, vertexConsumers.getBuffer(innerRenderLayer),
                    red, green, blue, alpha,
                    yOffset, height,
                    0.0f, width, width, 0.0f, -width, 0.0f, 0.0f, -width,
                    0.0f, 1f, height, offset);
        }

        width = originalWidth * 1.5F;
        renderBeamLayer(matrices, vertexConsumers.getBuffer(outerRenderLayer),
                red, green, blue, (int) (alpha * 0.75),
                yOffset, height,
                0.0f, width, width, 0.0f, -width, 0.0f, 0.0f, -width,
                0.0f, 1.0f, height, offset * 0.9F);

        width = originalWidth * 2F;
        renderBeamLayer(matrices, vertexConsumers.getBuffer(outerRenderLayer),
                red, green, blue, alpha / 3,
                yOffset, height,
                0.0f, width, width, 0.0f, -width, 0.0f, 0.0f, -width,
                0.0f, 1.0f, height, offset * 0.8F);
        matrices.pop();
    }

    private static void renderBeamLayer(MatrixStack matrices, VertexConsumer vertices,
                                        int red, int green, int blue, int alpha,
                                        float yOffset, float height,
                                        float x1, float z1, float x2, float z2, float x3, float z3, float x4,
                                        float z4, float u1, float u2, float v1, float v2) {
        MatrixStack.Entry matrix = matrices.peek();
        renderBeamFace(matrix, vertices, red, green, blue, alpha, yOffset, height, x1, z1, x2, z2, u1, u2, v1, v2);
        renderBeamFace(matrix, vertices, red, green, blue, alpha, yOffset, height, x4, z4, x3, z3, u1, u2, v1, v2);
        renderBeamFace(matrix, vertices, red, green, blue, alpha, yOffset, height, x2, z2, x4, z4, u1, u2, v1, v2);
        renderBeamFace(matrix, vertices, red, green, blue, alpha, yOffset, height, x3, z3, x1, z1, u1, u2, v1, v2);
    }


    private static void renderBeamFace(MatrixStack.Entry matrix, VertexConsumer vertices, int red, int green, int blue, int alpha, float yOffset, float height, float x1, float z1, float x2, float z2, float u1, float u2, float v1, float v2) {
        renderBeamVertex(matrix, vertices, red, green, blue, alpha, height, x1, z1, u2, v1);
        renderBeamVertex(matrix, vertices, red, green, blue, alpha, yOffset, x1, z1, u2, v2);
        renderBeamVertex(matrix, vertices, red, green, blue, alpha, yOffset, x2, z2, u1, v2);
        renderBeamVertex(matrix, vertices, red, green, blue, alpha, height, x2, z2, u1, v1);
    }

    /**
     * @param v the top-most coordinate of the texture region
     * @param u the left-most coordinate of the texture region
     */
    private static void renderBeamVertex(MatrixStack.Entry matrix, VertexConsumer vertices, int red, int green, int blue, int alpha, float y, float x, float z, float u, float v) {
        vertices.vertex(matrix, x, y, z)
                .color(red, green, blue, alpha)
                .texture(u, v)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(LightmapTextureManager.MAX_LIGHT_COORDINATE)
                .normal(matrix, 0.0F, 1.0F, 0.0F);
    }
}
