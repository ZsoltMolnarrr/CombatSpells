package net.spell_engine.api.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.*;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.util.Identifier;

public class CustomLayers extends RenderLayer {
    public CustomLayers(String name, VertexFormat vertexFormat, VertexFormat.DrawMode drawMode, int expectedBufferSize, boolean hasCrumbling, boolean translucent, Runnable startAction, Runnable endAction) {
        super(name, vertexFormat, drawMode, expectedBufferSize, hasCrumbling, translucent, startAction, endAction);
    }

    public static RenderLayer beam(Identifier texture, boolean cull, boolean transparent) {
        MultiPhaseParameters multiPhaseParameters = MultiPhaseParameters.builder()
                .program(BEACON_BEAM_PROGRAM)
                .cull(cull ? ENABLE_CULLING : DISABLE_CULLING)
                .texture(new RenderPhase.Texture(texture, false, false))
                .transparency(transparent ? BEAM_TRANSPARENCY : NO_TRANSPARENCY)
                .writeMaskState(transparent ? ALL_MASK : ALL_MASK)
                .build(false);
        return RenderLayer.of("spell_beam",
                VertexFormats.POSITION_COLOR_TEXTURE_LIGHT_NORMAL,
                VertexFormat.DrawMode.QUADS,
                256,
                false,
                true,
                multiPhaseParameters);
    }

    protected static final Transparency BEAM_TRANSPARENCY = new Transparency("beam_transparency", () -> {
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
    }, () -> {
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
    });

//    @Deprecated
//    public static RenderLayer projectile(Identifier texture, boolean translucent) {
//        return projectile(texture, translucent, true);
//    }
//
//    @Deprecated
//    public static RenderLayer projectile(Identifier texture, boolean translucent, boolean emissive) {
//        MultiPhaseParameters multiPhaseParameters = MultiPhaseParameters.builder()
//                .program(emissive ? ENTITY_TRANSLUCENT_EMISSIVE_PROGRAM : ENTITY_TRANSLUCENT_PROGRAM)
//                .texture(new RenderPhase.Texture((Identifier)texture, false, false))
//                .transparency(translucent ? TRANSLUCENT_TRANSPARENCY : NO_TRANSPARENCY)
//                .cull(DISABLE_CULLING)
//                .writeMaskState(translucent ? COLOR_MASK : ALL_MASK)
//                .overlay(ENABLE_OVERLAY_COLOR)
//                .build(false);
//        return RenderLayer.of("entity_translucent_emissive", VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, VertexFormat.DrawMode.QUADS, 256, true, true, multiPhaseParameters);
//    }



    public static RenderLayer spellEffect(LightEmission lightEmission, boolean translucent) {
        return spellObject(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE, lightEmission, translucent);
    }

    public static RenderLayer projectile(LightEmission lightEmission) {
        return spellObject(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE, lightEmission, false);
    }

    public static RenderLayer create(Identifier texture, RenderPhase.ShaderProgram shaderProgram, RenderPhase.Transparency transparency,
                                     RenderPhase.Cull culling, RenderPhase.WriteMaskState writeMask, RenderPhase.Overlay overlay,
                                     Target target, boolean affectsOutline) {
        MultiPhaseParameters multiPhaseParameters = MultiPhaseParameters.builder()
                .program(shaderProgram)
                .texture(new RenderPhase.Texture(texture, false, false))
                .transparency(transparency)
                .cull(culling)
                .writeMaskState(writeMask)
                .overlay(overlay)
                .target(target)
                .build(affectsOutline);
        return RenderLayer.of("entity_translucent_emissive", VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, VertexFormat.DrawMode.QUADS, 256, true, true, multiPhaseParameters);
    }

    public static RenderLayer spellObject(LightEmission lightEmission) {
        switch (lightEmission) {
            case RADIATE:
                return spellObject(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE, lightEmission, false);
            case GLOW:
                return spellObject(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE, lightEmission, false);
            case NONE:
                break;
        }
        return RenderLayer.getEntityTranslucent(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE);
    }

    public static RenderLayer spellObject(Identifier texture, LightEmission lightEmission, boolean translucent) {
        RenderPhase.ShaderProgram shaderProgram = switch (lightEmission) {
            case RADIATE -> ENTITY_TRANSLUCENT_EMISSIVE_PROGRAM;
            case GLOW -> BEACON_BEAM_PROGRAM;
            case NONE -> ENTITY_TRANSLUCENT_PROGRAM;
        };
        MultiPhaseParameters multiPhaseParameters = MultiPhaseParameters.builder()
                .program(shaderProgram)
                .texture(new RenderPhase.Texture(texture, false, false))
                .transparency(translucent ? TRANSLUCENT_TRANSPARENCY : NO_TRANSPARENCY)
                .cull(DISABLE_CULLING)
                .writeMaskState(translucent ? COLOR_MASK : ALL_MASK)
                .overlay(ENABLE_OVERLAY_COLOR)
                .target(PARTICLES_TARGET)
                .build(false);
        return RenderLayer.of("entity_translucent_emissive", VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, VertexFormat.DrawMode.QUADS, 256, true, true, multiPhaseParameters);
    }

    public static RenderLayer create(String name, VertexFormat vertexFormat, VertexFormat.DrawMode drawMode, int expectedBufferSize, boolean hasCrumbling, boolean translucent, MultiPhaseParameters phases) {
        return RenderLayer.of(name, vertexFormat, drawMode, expectedBufferSize, hasCrumbling, translucent, phases);
    }
}