package net.spell_engine.spellbinding;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.spell_engine.SpellEngineMod;
import net.spell_engine.client.gui.SpellTooltip;
import net.spell_engine.client.util.SpellRender;
import net.spell_engine.internals.SpellContainerHelper;
import net.spell_engine.internals.SpellRegistry;

import java.util.ArrayList;
import java.util.List;

@Environment(value= EnvType.CLIENT)
public class SpellBindingScreen extends HandledScreen<SpellBindingScreenHandler> {
    private static final Identifier TEXTURE = new Identifier(SpellEngineMod.ID, "textures/gui/" + SpellBinding.name + ".png");

    private ItemStack stack;

    public SpellBindingScreen(SpellBindingScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.stack = ItemStack.EMPTY;
    }

    protected void init() {
        super.init();
    }

    public void handledScreenTick() {
        super.handledScreenTick();
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        delta = this.client.getTickDelta();
        this.renderBackground(matrices);
        super.render(matrices, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(matrices, mouseX, mouseY);
        var player = MinecraftClient.getInstance().player;
        var lapisCount = handler.getLapisCount();
        var itemStack = handler.getStacks().get(0);
        for (var button: buttonViewModels) {
            if (button.spell != null && button.mouseOver(mouseX, mouseY)) {
                ArrayList<Text> tooltip = Lists.newArrayList();
                switch (button.bindingState.state) {
                    case ALREADY_APPLIED -> {
                        tooltip.add(Text.translatable("gui.spell_engine.spell_binding.already_bound")
                                .formatted(Formatting.GRAY));
                    }
                    case NO_MORE_SLOT -> {
                        tooltip.add(Text.translatable("gui.spell_engine.spell_binding.no_more_slots")
                                .formatted(Formatting.GRAY));
                    }
                    case APPLICABLE -> {
                        if (button.bindingState.readyToApply(player, lapisCount)) {
                            tooltip.add(Text.translatable("gui.spell_engine.spell_binding.available")
                                    .formatted(Formatting.GREEN));
                        } else {
                            if (!button.bindingState.requirements.metRequiredLevel(player)) {
                                tooltip.add(Text.translatable("gui.spell_engine.spell_binding.level_req_fail",
                                                button.bindingState.requirements.requiredLevel())
                                        .formatted(Formatting.RED));
                            } else {
                                var lapisCost = button.bindingState.requirements.lapisCost();
                                var hasEnoughLapis = button.bindingState.requirements.hasEnoughLapis(lapisCount);
                                MutableText lapis = lapisCost == 1 ? Text.translatable("container.enchant.lapis.one") : Text.translatable("container.enchant.lapis.many", lapisCost);
                                tooltip.add(lapis.formatted(hasEnoughLapis ? Formatting.GRAY : Formatting.RED));

                                var levelCost = button.bindingState.requirements.levelCost();
                                var hasEnoughLevels = button.bindingState.requirements.hasEnoughLevelsToSpend(player);
                                MutableText levels = levelCost == 1 ? Text.translatable("container.enchant.level.one") : Text.translatable("container.enchant.level.many", levelCost);
                                tooltip.add(levels.formatted(hasEnoughLevels ? Formatting.GRAY : Formatting.RED));
                            }
                        }
                    }
                    case INVALID -> {
                        continue;
                    }
                }
                tooltip.add(Text.literal(" "));
                tooltip.addAll(SpellTooltip.spellInfo(button.spell.id(), player, itemStack));
                this.renderTooltip(matrices, tooltip, mouseX, mouseY);
                break;
            }
        }
    }

    @Override
    protected void drawBackground(MatrixStack matrices, float delta, int mouseX, int mouseY) {
        DiffuseLighting.disableGuiDepthLighting();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.setShaderTexture(0, TEXTURE);
        int originX = (this.width - this.backgroundWidth) / 2;
        int originY = (this.height - this.backgroundHeight) / 2;
        this.drawTexture(matrices, originX, originY, 0, 0, this.backgroundWidth, this.backgroundHeight);
        DiffuseLighting.enableGuiDepthLighting();
        this.updateButtons(originX, originY);
        this.drawButtons(matrices, mouseX, mouseY);
    }

    private List<ButtonViewModel> buttonViewModels = List.of();

    private static final int BUTTONS_ORIGIN_X = 60;
    private static final int BUTTONS_ORIGIN_Y = 14;

    private void updateButtons(int originX, int originY) {
        var buttons = new ArrayList<ButtonViewModel>();
        var itemStack = handler.getStacks().get(0);
        var lapisCount = handler.getLapisCount();
        var player = MinecraftClient.getInstance().player;
        var container = SpellContainerHelper.containerFromItemStack(itemStack);
        if (container == null) {
            buttonViewModels = buttons;
            return;
        }

        for (int i = 0; i < SpellBindingScreenHandler.MAXIMUM_SPELL_COUNT; i++) {
            var rawId = handler.spellId[i];
            var cost = handler.spellCost[i];
            var requirement = handler.spellLevelRequirement[i];
            var spellId = SpellRegistry.fromRawId(rawId);
            if (spellId.isEmpty()) { continue; }
            var id = spellId.get();
            var spell = new SpellInfo(
                    id,
                    SpellRender.iconTexture(id),
                    Text.translatable(SpellTooltip.spellTranslationKey(id)),
                    cost, requirement);
            SpellBinding.State bindingState = SpellBinding.State.of(id, itemStack, cost, cost, requirement);
            boolean isEnabled = bindingState.readyToApply(player, lapisCount);
            var button = new ButtonViewModel(
                    originX + BUTTONS_ORIGIN_X, originY + BUTTONS_ORIGIN_Y + (buttons.size() * BUTTON_HEIGHT),
                    BUTTON_WIDTH, BUTTON_HEIGHT,
                    isEnabled, spell, bindingState);
            buttons.add(button);
        }
        buttonViewModels = buttons;
    }

    private void drawButtons(MatrixStack matrices, int mouseX, int mouseY) {
        for(var button: buttonViewModels) {
            var state = button.mouseOver(mouseX, mouseY) ? ButtonState.HOVER : ButtonState.NORMAL;
            drawSpellButton(matrices, button, state);
        }
    }

    enum ButtonState { NORMAL, HOVER }


    record SpellInfo(Identifier id, Identifier icon, Text name, int cost, int requirement) { }
    record ButtonViewModel(int x, int y, int width, int height, boolean isEnabled, SpellInfo spell, SpellBinding.State bindingState) {
        public boolean mouseOver(int mouseX, int mouseY) {
            return (mouseX > x && mouseX < x + width) && (mouseY > y && mouseY < y + height);
        }
    }

    private static final int BUTTON_TEXTURE_U = 0;
    private static final int BUTTON_TEXTURE_V = 166;
    private static final int BUTTON_WIDTH = 108;
    private static final int BUTTON_HEIGHT = 19;
    private static final int SPELL_ICON_SIZE = 16;
    private static final int SPELL_ICON_INDENT = (int) Math.ceil((BUTTON_HEIGHT - SPELL_ICON_SIZE) / 2.0);
    private void drawSpellButton(MatrixStack matrices, ButtonViewModel viewModel, ButtonState state) {
        int u = BUTTON_TEXTURE_U;
        int v = BUTTON_TEXTURE_V;
        if (viewModel.isEnabled) {
            switch (state) {
                case NORMAL -> {
                    v += 0;
                }
                case HOVER -> {
                    v += viewModel.height * 2;
                }
            }
        } else {
            v += viewModel.height;
        }
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, TEXTURE);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        drawTexture(matrices, viewModel.x, viewModel.y, u, v, viewModel.width, viewModel.height);
        if (viewModel.spell != null) {
            boolean isUnlocked = viewModel.bindingState.state == SpellBinding.State.ApplyState.ALREADY_APPLIED || viewModel.isEnabled;
            var spell = viewModel.spell;
            textRenderer.drawWithShadow(matrices, spell.name,
                    viewModel.x + viewModel.height, viewModel.y + SPELL_ICON_INDENT, isUnlocked ? 0xFFFFFF : 0x808080);
            if (spell.icon != null) {

                RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, isUnlocked ? 1F : 0.5f);
                RenderSystem.enableBlend();
                RenderSystem.setShaderTexture(0, spell.icon);
                // int x, int y, int u, int v, int width, int height
                DrawableHelper.drawTexture(matrices,
                        viewModel.x + SPELL_ICON_INDENT,
                        viewModel.y + SPELL_ICON_INDENT,
                        0, 0,
                        SPELL_ICON_SIZE, SPELL_ICON_SIZE, SPELL_ICON_SIZE, SPELL_ICON_SIZE);
            }
        }
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();
    }
}
