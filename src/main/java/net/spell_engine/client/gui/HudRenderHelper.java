package net.spell_engine.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.spell_engine.SpellEngineMod;
import net.spell_engine.client.SpellEngineClient;
import net.spell_engine.client.input.SpellHotbar;
import net.spell_engine.client.util.Rect;
import net.spell_engine.client.util.SpellRender;
import net.spell_engine.client.util.TextureFile;
import net.spell_engine.config.HudConfig;
import net.spell_engine.internals.SpellHelper;
import net.spell_engine.internals.casting.SpellCasterClient;
import net.spell_engine.mixin.client.control.KeybindingAccessor;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class HudRenderHelper {

    public static void render(DrawContext context, float tickDelta) {
        render(context, tickDelta, false);
    }

    public static void render(DrawContext context, float tickDelta, boolean config) {
        var hudConfig = SpellEngineClient.hudConfig.value;
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;

        if ((player == null || player.isSpectator())
                && !config) {
            return;
        }

        var clientConfig = SpellEngineClient.config;

        var targetViewModel = TargetWidget.ViewModel.mock();
        boolean renderHotbar = true;
        var hotbarViewModel = SpellHotBarWidget.ViewModel.mock();
        var errorViewModel = ErrorMessageWidget.ViewModel.mock();
        CastBarWidget.ViewModel castBarViewModel = null;
        if (config) {
            castBarViewModel = CastBarWidget.ViewModel.mock();
        } else {
            targetViewModel = TargetWidget.ViewModel.from(player);
        }

        if (player != null) {
            var caster = (SpellCasterClient) player;

            if (SpellHotbar.INSTANCE.slots.isEmpty()) {
                hotbarViewModel = SpellHotBarWidget.ViewModel.empty;
            } else {
                var cooldownManager = caster.getCooldownManager();
                var spells = SpellHotbar.INSTANCE.slots.stream().map(slot -> {
                    var info = slot.spell();
                    var itemStack = slot.itemStack();
                    var useItem = itemStack != null;
                    var cooldownProgress = useItem
                            ? player.getItemCooldownManager().getCooldownProgress(itemStack.getItem(), tickDelta)
                            : cooldownManager.getCooldownProgress(Identifier.of(info.id().toString()), tickDelta);
                    return new SpellHotBarWidget.SpellViewModel(
                            useItem ? null : SpellRender.iconTexture(info.id()),
                            useItem ? itemStack : null,
                            cooldownProgress,
                            SpellHotBarWidget.KeyBindingViewModel.from(slot.getKeyBinding(client.options)),
                            slot.modifier() != null ? SpellHotBarWidget.KeyBindingViewModel.from(slot.modifier()) : null);
                }).collect(Collectors.toList());
                hotbarViewModel = new SpellHotBarWidget.ViewModel(spells);
            }
            renderHotbar = true;

            var spellCast = caster.getSpellCastProgress();
            if (spellCast != null) {
                castBarViewModel = new CastBarWidget.ViewModel(
                        spellCast.process().spell().school.color,
                        spellCast.ratio(),
                        spellCast.process().length(),
                        SpellRender.iconTexture(spellCast.process().id()),
                        true,
                        SpellHelper.isChanneled(spellCast.process().spell())
                );
            }

            if (!config) {
                var hudMessages = HudMessages.INSTANCE;
                var error = hudMessages.currentError();
                if (error != null && error.durationLeft > 0) {
                    errorViewModel = ErrorMessageWidget.ViewModel.from(error.message, error.durationLeft, error.fadeOut, tickDelta);
                } else {
                    errorViewModel = null;
                }
            }
        }

        var screenWidth = client.getWindow().getScaledWidth();
        var screenHeight = client.getWindow().getScaledHeight();
        var originPoint = hudConfig.castbar.base.origin.getPoint(screenWidth, screenHeight);
        var baseOffset = originPoint.add(hudConfig.castbar.base.offset);
        if (castBarViewModel != null) {
            CastBarWidget.render(context, tickDelta, hudConfig, baseOffset, castBarViewModel);
        }

        if (hudConfig.castbar.target.visible) {
            var targetOffset = baseOffset.add(hudConfig.castbar.target.offset);
            TargetWidget.render(context, tickDelta, targetOffset, targetViewModel);
        }

        if (renderHotbar || config) {
            if (config && (hotbarViewModel == null || hotbarViewModel.isEmpty())) {
                hotbarViewModel = SpellHotBarWidget.ViewModel.mock();
            }
            SpellHotBarWidget.render(context, screenWidth, screenHeight, hotbarViewModel);
        }

        if (errorViewModel != null) {
            ErrorMessageWidget.render(context, hudConfig, screenWidth, screenHeight, errorViewModel);
        }
    }

    public static class TargetWidget {
        public static void render(DrawContext context, float tickDelta, Vec2f starting, ViewModel viewModel) {
            MinecraftClient client = MinecraftClient.getInstance();
            var textRenderer = client.inGameHud.getTextRenderer();

            int textWidth = textRenderer.getWidth(viewModel.text);

            int x = (int) (starting.x - (textWidth / 2F));
            int y = (int) starting.y;
            int opacity = 255;

            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            context.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            context.fill(x - 2, y - 2, x + textWidth + 2, y + textRenderer.fontHeight + 2, client.options.getTextBackgroundColor(0));
            context.drawTextWithShadow(textRenderer, viewModel.text, x, y, 0xFFFFFF);
            RenderSystem.disableBlend();
            context.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        }

        public record ViewModel(String text) {
            public static ViewModel mock() {
                return new ViewModel("Target name");
            }

            public static ViewModel from(ClientPlayerEntity player) {
                var caster = (SpellCasterClient)player;
                var target = caster.getCurrentFirstTarget();
                var text = "";
                if (target != null
                        && (/* SpellEngineClient.config.showTargetNameWhenMultiple || */ caster.getCurrentTargets().size() == 1)) {
                    text = target.getName().getString();
                }
                return new ViewModel(text);
            }
        }
    }

    public static class CastBarWidget {
        public static Rect lastRendered;
        private static final float tailWidth = 5;
        public static final float minWidth = 2 * tailWidth;
        private static final int textureWidth = 182;
        private static final int textureHeight = 10;
        private static final int barHeight = textureHeight / 2;
        private static final Identifier CAST_BAR = Identifier.of(SpellEngineMod.ID, "textures/hud/castbar.png");
        private static final int spellIconSize = 16;

        public record ViewModel(int color, float progress, float castDuration, Identifier iconTexture, boolean allowTickDelta, boolean reverse) {
            public static ViewModel mock() {
                return new ViewModel(0xFF3300, 0.5F, 1, SpellRender.iconTexture(Identifier.of("spell_engine", "dummy_spell")), false, false);
            }
        }

        public static void render(DrawContext context, float tickDelta, HudConfig hudConfig, Vec2f starting, ViewModel viewModel) {
            var barWidth = hudConfig.castbar.width;
            var totalWidth = barWidth + minWidth;
            var totalHeight = barHeight;
            int x = (int) (starting.x - (totalWidth / 2));
            int y = (int) (starting.y - (totalHeight / 2));
            lastRendered = new Rect(new Vec2f(x,y), new Vec2f(x + totalWidth,y + totalHeight));

            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();

            float red = ((float) ((viewModel.color >> 16) & 0xFF)) / 255F;
            float green = ((float) ((viewModel.color >> 8) & 0xFF)) / 255F;
            float blue = ((float) (viewModel.color & 0xFF)) / 255F;

            context.setShaderColor(red, green, blue, 1F);

            renderBar(context, barWidth, true, 1, x, y);
            float partialProgress = 0;
            if (viewModel.allowTickDelta && viewModel.castDuration > 0) {
                partialProgress = tickDelta / viewModel.castDuration;
            }
            var progress = viewModel.reverse() ? (1F - viewModel.progress - partialProgress) : (viewModel.progress + partialProgress);
            renderBar(context, barWidth, false, progress, x, y);
            context.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

            if (hudConfig.castbar.icon.visible && viewModel.iconTexture != null) {
                x = (int) (starting.x + hudConfig.castbar.icon.offset.x);
                y = (int) (starting.y + hudConfig.castbar.icon.offset.y);

                context.drawTexture(viewModel.iconTexture, x, y, 0, 0, spellIconSize, spellIconSize, spellIconSize, spellIconSize);
            }

            RenderSystem.disableBlend();
        }

        private static void renderBar(DrawContext context, int barWidth, boolean isBackground, float progress, int x, int y) {
            var totalWidth = barWidth + minWidth;
            var centerWidth = totalWidth - minWidth;
            float leftRenderBegin = 0;
            float centerRenderBegin = tailWidth;
            float rightRenderBegin = totalWidth - tailWidth;

            renderBarPart(context, isBackground, PART.LEFT, progress, leftRenderBegin, tailWidth, x, y, totalWidth);
            renderBarPart(context, isBackground, PART.CENTER, progress, centerRenderBegin, centerRenderBegin + centerWidth, x, y, totalWidth);
            renderBarPart(context, isBackground, PART.RIGHT, progress, rightRenderBegin, totalWidth, x, y, totalWidth);
        }

        enum PART { LEFT, CENTER, RIGHT }
        private static void renderBarPart(DrawContext context, boolean isBackground, PART part, float progress, float renderBegin, float renderEnd, int x, int y, float totalWidth) {
            var u = 0;
            var partMaxWidth = renderEnd - renderBegin; //5
            var progressRange = (renderEnd - renderBegin) / totalWidth; //0.05
            var progressFloor = (renderBegin / totalWidth); // 0
            var adjustedProgress = Math.min(Math.max((progress - progressFloor), 0), progressRange) / progressRange;
            var width = Math.round(adjustedProgress * partMaxWidth);
            switch (part) {
                case LEFT -> {
                    u = 0;
                    // System.out.println(" partMaxWidth: " + partMaxWidth + " progressRange: " + progressRange + " progressFloor: " + progressFloor + " adjustedProgress: " + adjustedProgress + " width: " + width);
//                    context.setShaderColor(1.F, 0F, 0F, 0.5F);
                }
                case CENTER -> {
                    u = (int) tailWidth;
//                    context.setShaderColor(0.F, 1F, 0F, 0.5F);
                }
                case RIGHT -> {
                    u = (int) (textureWidth - tailWidth);
//                    context.setShaderColor(0.F, 0F, 1F, 0.5F);
                }
            }
            int v = isBackground ? 0 : barHeight;
            context.drawTexture(CAST_BAR, (int) (x + renderBegin), y, u, v, width, barHeight, textureWidth, textureHeight);
            // DrawableHelper.drawTexture(matrixStack, (int) (x + renderBegin), y, u, v, width, barHeight, textureWidth, textureHeight);
        }
    }

    public class SpellHotBarWidget {
        public static Rect lastRendered;
        private static final TextureFile HOTBAR = new TextureFile(Identifier.of("textures/gui/sprites/hud/hotbar.png"), 182, 22);
        private static final int slotHeight = 22;
        private static final int slotWidth = 20;

        private static final Map<Integer, String> customHudKeyLabels = Map.of(
                InputUtil.GLFW_KEY_LEFT_ALT, "Al",
                InputUtil.GLFW_KEY_RIGHT_ALT, "Al",
                InputUtil.GLFW_KEY_LEFT_SHIFT, "↑",
                InputUtil.GLFW_KEY_RIGHT_SHIFT, "↑"
        );

        public record KeyBindingViewModel(String label, @Nullable Drawable.Component drawable) {
            public static KeyBindingViewModel from(@Nullable KeyBinding keyBinding) {
                if (keyBinding == null) {
                    return new KeyBindingViewModel("", null);
                }
                var boundKey = ((KeybindingAccessor)keyBinding).getBoundKey();
                var key = boundKey.toString();
                var drawable = HudKeyVisuals.custom.get(key);
                if (drawable != null) {
                    return new KeyBindingViewModel("", drawable);
                }
                var customLabel = customHudKeyLabels.get(boundKey.getCode());
                if (customLabel != null) {
                    return new KeyBindingViewModel(customLabel, null);
                }
                var label = keyBinding.getBoundKeyLocalizedText()
                        .getString()
                        .toUpperCase(Locale.US);
                label = acronym(label, 3);
                return new KeyBindingViewModel(label, null);
            }

            public int width(TextRenderer textRenderer) {
                if (drawable != null) {
                    return drawable.draw().width();
                } else {
                    return textRenderer.getWidth(label);
                }
            }
        }

        private static String acronym(String phrase, int maxLength) {
            StringBuilder result = new StringBuilder();
            for (String token : phrase.split("\\s+")) {
                result.append(token.toUpperCase().charAt(0));
            }
            var resultString = result.toString();
            // Make the result at most 3 characters long
            if (resultString.length() > maxLength) {
                resultString = resultString.substring(0, maxLength);
            }
            return result.toString();
        }

        public record SpellViewModel(@Nullable Identifier iconId, @Nullable ItemStack itemStack, float cooldown, KeyBindingViewModel keybinding, @Nullable KeyBindingViewModel modifier) { }

        public record ViewModel(List<SpellViewModel> spells) {
            public static ViewModel mock() {
                return new ViewModel(
                        List.of(
                                new SpellViewModel(SpellRender.iconTexture(Identifier.of(SpellEngineMod.ID, "dummy_spell")), null, 0, new KeyBindingViewModel("1", null), null),
                                new SpellViewModel(SpellRender.iconTexture(Identifier.of(SpellEngineMod.ID, "dummy_spell")), null, 0, new KeyBindingViewModel("2", null), null),
                                new SpellViewModel(SpellRender.iconTexture(Identifier.of(SpellEngineMod.ID, "dummy_spell")), null, 0, new KeyBindingViewModel("3", null), null)
                        )
                );
            }

            public static final ViewModel empty = new ViewModel(List.of());

            public boolean isEmpty() {
                return spells.isEmpty();
            }
        }

        public static void render(DrawContext context, int screenWidth, int screenHeight, ViewModel viewModel) {
            var config = SpellEngineClient.hudConfig.value.hotbar;
            MinecraftClient client = MinecraftClient.getInstance();
            var textRenderer = client.inGameHud.getTextRenderer();
            if (viewModel.spells.isEmpty()) {
                return;
            }
            float estimatedWidth = slotWidth * viewModel.spells.size();
            float estimatedHeight = slotHeight;
            var origin = config.origin
                    .getPoint(screenWidth, screenHeight)
                    .add(config.offset)
                    .add(new Vec2f(estimatedWidth * (-0.5F), estimatedHeight * (-0.5F))); // Grow from center
            lastRendered = new Rect(origin, origin.add(new Vec2f(estimatedWidth, estimatedHeight)));

            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();

            // float barOpacity = (SpellEngineClient.config.indicateActiveHotbar && InputHelper.isLocked) ? 1F : 0.5F;
            float barOpacity = 1F;

            // Background
            context.setShaderColor(1.0f, 1.0f, 1.0f, barOpacity);
            context.drawTexture(HOTBAR.id(), (int) (origin.x), (int) (origin.y), 0, 0, slotWidth / 2, slotHeight, HOTBAR.width(), HOTBAR.height());
            int middleElements = viewModel.spells.size() - 1;
            for (int i = 0; i < middleElements; i++) {
                context.drawTexture(HOTBAR.id(), (int) (origin.x) + (slotWidth / 2) + (i * slotWidth), (int) (origin.y), slotWidth / 2, 0, slotWidth, slotHeight, HOTBAR.width(), HOTBAR.height());
            }
            context.drawTexture(HOTBAR.id(), (int) (origin.x) + (slotWidth / 2) + (middleElements * slotWidth), (int) (origin.y), 170, 0, (slotHeight / 2) + 1, slotHeight, HOTBAR.width(), HOTBAR.height());

            // Icons
            context.setShaderColor(1.0f, 1.0f, 1.0f, 1.0F);
            var iconsOffset = new Vec2f(3,3);
            int iconSize = 16;
            for (int i = 0; i < viewModel.spells.size(); i++) {
                var spell = viewModel.spells.get(i);
                int x = (int) (origin.x + iconsOffset.x) + ((slotWidth) * i);
                int y = (int) (origin.y + iconsOffset.y);


                RenderSystem.enableBlend();

                // Icon
                if (spell.iconId != null) {
                    context.drawTexture(spell.iconId, x, y, 0, 0, iconSize, iconSize, iconSize, iconSize);
                } else if (spell.itemStack != null) {
                    context.drawItem(spell.itemStack, x, y);
                }

                // Cooldown
                if (spell.cooldown > 0) {
                    renderCooldown(context, spell.cooldown, x, y);
                }

                // Keybinding
                if (spell.keybinding() != null) {
                    var keybindingX = x + (iconSize / 2);
                    var keybindingY = (int)origin.y + 2;
                    if (spell.modifier != null) {
                        keybindingX += 2; // Shifting to the right, because this will likely be the last
                        var spacing = 1;
                        var modifierWidth = spell.modifier().width(textRenderer);
                        var keybindingWidth = spell.keybinding().width(textRenderer);
                        var totalWidth = modifierWidth + keybindingWidth;

                        keybindingX -= (totalWidth / 2);
                        drawKeybinding(context, textRenderer, spell.modifier, keybindingX, keybindingY, Drawable.Anchor.LEADING, Drawable.Anchor.TRAILING);
                        keybindingX += modifierWidth + spacing;
                        drawKeybinding(context, textRenderer, spell.keybinding(), keybindingX, keybindingY, Drawable.Anchor.LEADING, Drawable.Anchor.TRAILING);
                    } else {
                        drawKeybinding(context, textRenderer, spell.keybinding(), keybindingX, keybindingY, Drawable.Anchor.CENTER, Drawable.Anchor.TRAILING);
                    }
                }
            }

            RenderSystem.disableBlend();
            context.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        }

        private static void drawKeybinding(DrawContext context, TextRenderer textRenderer, KeyBindingViewModel keybinding, int x, int y,
                                           Drawable.Anchor horizontalAnchor, Drawable.Anchor verticalAnchor) {
            if (keybinding.drawable != null) {
                keybinding.drawable.draw(context, x, y, horizontalAnchor, verticalAnchor);
            } else {
                var textLength = textRenderer.getWidth(keybinding.label);
                var xOffset = 0;
                switch (horizontalAnchor) {
                    case TRAILING -> xOffset = -textLength / 2;
                    case CENTER -> xOffset = 0;
                    case LEADING -> xOffset = textLength / 2;
                }
                x += xOffset;
                HudKeyVisuals.buttonLeading.draw(context, x - (textLength / 2), y, Drawable.Anchor.TRAILING, verticalAnchor);
                HudKeyVisuals.buttonCenter.drawFlexibleWidth(context, x - (textLength / 2), y, textLength, verticalAnchor);
                HudKeyVisuals.buttonTrailing.draw(context, x + (textLength / 2), y, Drawable.Anchor.LEADING, verticalAnchor);
                context.drawCenteredTextWithShadow(textRenderer, keybinding.label, x, y - 10, 0xFFFFFF);
            }
        }

        private static void renderCooldown(DrawContext context, float progress, int x, int y) {
            // Copied from DrawContext.drawItemInSlot
            var k = y + MathHelper.floor(16.0F * (1.0F - progress));
            var l = k + MathHelper.ceil(16.0F * progress);
            context.fill(RenderLayer.getGuiOverlay(), x, k, x + 16, l, Integer.MAX_VALUE);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
        }
    }

    public static class ErrorMessageWidget {
        public static Rect lastRendered;

        public record ViewModel(Text message, float opacity) {
            public static ViewModel mock() {
                return new ViewModel(Text.literal("Error Message!").formatted(Formatting.RED), 1F);
            }

            public static ViewModel from(Text message, int durationLeft, int fadeOut, float tickDelta) {
                float tick = ((float)durationLeft) - tickDelta;
                float opacity = tick > fadeOut ? 1F : (tick / fadeOut);
                return new ViewModel(message, opacity);
            }
        }

        public static void render(DrawContext context, HudConfig hudConfig, int screenWidth, int screenHeight, ViewModel viewModel) {
            int alpha = (int) (viewModel.opacity * 255);
            if (alpha < 10) { return; }
            // System.out.println("Rendering opacity: " + viewModel.opacity + " alpha: " + alpha);
            MinecraftClient client = MinecraftClient.getInstance();
            var textRenderer = client.inGameHud.getTextRenderer();
            int textWidth = textRenderer.getWidth(viewModel.message);
            int textHeight = textRenderer.fontHeight;
            var config = hudConfig.error_message;
            var origin = config.origin
                    .getPoint(screenWidth, screenHeight)
                    .add(config.offset);

            int x = (int) (origin.x - (textWidth / 2F));
            int y = (int) origin.y;
            lastRendered = new Rect(new Vec2f(x ,y), new Vec2f(x + textWidth,y + textHeight));
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            context.fill(x - 2, y - 2, x + textWidth + 2, y + textRenderer.fontHeight + 2, client.options.getTextBackgroundColor(0));
            context.drawTextWithShadow(textRenderer, viewModel.message(), x, y, 0xFFFFFF + (alpha << 24)); // color is ARGB
            RenderSystem.disableBlend();
        }
    }
}
