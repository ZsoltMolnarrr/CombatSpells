package net.spell_engine.api.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.spell_engine.client.render.CustomModelRegistry;
import net.spell_engine.mixin.client.render.ItemRendererAccessor;

import java.util.List;

public class CustomModels {
    public static void registerModelIds(List<Identifier> ids) {
        CustomModelRegistry.modelIds.addAll(ids);
    }

    public static void render(RenderLayer renderLayer, ItemRenderer itemRenderer, Identifier modelId,
                              MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int seed) {
        var model = MinecraftClient.getInstance().getBakedModelManager().getModel(modelId);
        if (model == null) {
            var stack = Registries.ITEM.get(modelId).getDefaultStack();
            if (!stack.isEmpty()) {
                model = itemRenderer.getModel(stack, null, null, seed);
            }
        }
        renderModel(renderLayer, (ItemRendererAccessor) itemRenderer, matrices, vertexConsumers, light, model);
    }

    public static void renderModel(RenderLayer renderLayer, ItemRendererAccessor itemRenderer,
                                    MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, BakedModel model) {
        var buffer = vertexConsumers.getBuffer(renderLayer);
        matrices.translate(-0.5, -0.5, -0.5);
        itemRenderer.SpellEngine_renderBakedItemModel(model, ItemStack.EMPTY, light, OverlayTexture.DEFAULT_UV, matrices, buffer);
    }
}
