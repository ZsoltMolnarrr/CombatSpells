package net.spell_engine.client.gui;

import com.ibm.icu.text.DecimalFormat;
import net.fabricmc.fabric.mixin.client.keybinding.KeyBindingAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.spell_engine.SpellEngineMod;
import net.spell_engine.api.item.SpellEngineItemTags;
import net.spell_engine.api.spell.Spell;
import net.spell_engine.api.spell.registry.SpellRegistry;
import net.spell_engine.client.SpellEngineClient;
import net.spell_engine.client.input.Keybindings;
import net.spell_engine.internals.Ammo;
import net.spell_engine.internals.SpellHelper;
import net.spell_engine.api.spell.container.SpellContainerHelper;
import net.spell_power.api.SpellPower;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SpellTooltip {
    private static final String damageToken = "damage";
    private static final String healToken = "heal";
    private static final String rangeToken = "range";
    private static final String durationToken = "duration";
    private static final String itemToken = "item";
    private static final String effectDurationToken = "effect_duration";
    private static final String effectAmplifierToken = "effect_amplifier";
    private static final String impactRangeToken = "impact_range";
    private static final String teleportDistanceToken = "teleport_distance";
    private static final String countToken = "count";
    private static final String trigger_chance = "trigger_chance";
    public static String placeholder(String token) { return "{" + token + "}"; }

    public static void addSpellLines(ItemStack itemStack, TooltipType tooltipType, List<Text> lines) {
        var player = MinecraftClient.getInstance().player;
        if (player == null) {
            return;
        }
        int addSectionDivider = 0;
        var config = SpellEngineClient.config;
        var spellTextLines = new ArrayList<Text>();
        var container = SpellContainerHelper.containerFromItemStack(itemStack);
        if (container != null && container.isValid()) {
            if (container.is_proxy() && config.showSpellBookSuppportTooltip) {
                switch (container.content()) {
                    case MAGIC -> {
                        spellTextLines.add(Text.translatable("spell.tooltip.host.proxy.spell")
                                .formatted(Formatting.GRAY));
                    }
                    case ARCHERY -> {
                        spellTextLines.add(Text.translatable("spell.tooltip.host.proxy.archery")
                                .formatted(Formatting.GRAY));
                    }
                }
                addSectionDivider += 1;
            }

            boolean showListHeader = !itemStack.isIn(SpellEngineItemTags.SPELL_BOOK_MERGEABLE);
            int indentLevel = showListHeader ? 1 : 0;

            if (!container.spell_ids().isEmpty() && showListHeader) {
                String limit = "";
                if (container.max_spell_count() > 0) {
                    limit = I18n.translate("spell.tooltip.host.limit")
                            .replace(placeholder("current"), "" + container.spell_ids().size())
                            .replace(placeholder("max"), "" + container.max_spell_count());
                }

                var key = "spell.tooltip.host.list.spell";
                switch (container.content()) {
                    case MAGIC -> {
                        key = "spell.tooltip.host.list.spell";
                    }
                    case ARCHERY -> {
                        key = "spell.tooltip.host.list.archery";
                    }
                }
                spellTextLines.add(Text.translatable(key)
                        .append(Text.literal(" " + limit))
                        .formatted(Formatting.GRAY));
                addSectionDivider += 1;
            }
            var keybinding = Keybindings.bypass_spell_hotbar;
            var showDetails = config.alwaysShowFullTooltip
                    || (!keybinding.isUnbound() && InputUtil.isKeyPressed(
                    MinecraftClient.getInstance().getWindow().getHandle(),
                    ((KeyBindingAccessor) keybinding).fabric_getBoundKey().getCode())
            );
            for (int i = 0; i < container.spell_ids().size(); i++) {
                var spellId = Identifier.of(container.spell_ids().get(i));
                var info = spellEntry(spellId, player, itemStack, showDetails, indentLevel);
                if (!info.isEmpty()) {
                    if (i > 0 && showDetails) {
                        spellTextLines.add(Text.literal(" ")); // Separator: empty line
                    }
                    spellTextLines.addAll(info);
                }
            }
            if (!showDetails) {
                if (!keybinding.isUnbound() && container.spell_ids().size() > 0) {
                    spellTextLines.add(Text.translatable("spell.tooltip.hold_for_details",
                                    keybinding.getBoundKeyLocalizedText())
                            .formatted(Formatting.GRAY));
                }
                if (config.showSpellBindingTooltip
                        && container.pool() != null && !container.pool().isEmpty()
                        && container.spell_ids().isEmpty()) {
                    spellTextLines.add(Text.translatable("spell.tooltip.spell_binding_tip")
                            .formatted(Formatting.GRAY));
                }
            }
        }

        if (spellTextLines.isEmpty()) {
            return;
        }
        var found = 0;
        if (tooltipType.isAdvanced()) {
            var searchedStyle = Text.literal("x")
                    .formatted(Formatting.DARK_GRAY)
                    .getStyle(); // From: ItemStack.java, advanced tooltip section
            var reverseIndex = lines.size();
            for (var line : lines.reversed()) {
                --reverseIndex;
                var style = line.getStyle();
                if (style != null) {
                    var newFind = searchedStyle.getColor().equals(style.getColor());
                    if (found != 0 && !newFind) {
                        break;
                    } else {
                        found = reverseIndex;
                    }
                }
            }
        }
        if (found <= 0) {
            if (addSectionDivider > 0) {
                spellTextLines.addFirst(Text.literal(""));
            }
            lines.addAll(spellTextLines);
        } else {
            if (addSectionDivider > 0) {
                boolean hasPreceedignEmptyLine = false;
                if (found > 0) {
                    var previousLine = lines.get(found - 1).getString();
                    hasPreceedignEmptyLine = previousLine.isBlank();
                }
                if (!hasPreceedignEmptyLine) {
                    spellTextLines.addFirst(Text.literal(""));
                }
            }
            lines.addAll(found, spellTextLines);
        }
    }

    public static List<Text> spellEntry(Identifier spellId, PlayerEntity player, ItemStack itemStack, boolean details, int indentLevel) {
        var lines = new ArrayList<Text>();
        var world = MinecraftClient.getInstance().world;
        if (world == null) {
            return lines;
        }
        var spellEntry = SpellRegistry.from(world).getEntry(spellId);
        if (spellEntry.isEmpty()) {
            return lines;
        }
        var spell = spellEntry.get().value();
        var primaryPower = SpellPower.getSpellPower(spell.school, player);

        if (shouldShow(spell.tooltip.name, details)) {
            var color = Formatting.byName(spell.tooltip.name.color);
            var name = Text.translatable(spellTranslationKey(spellId))
                    .formatted(Formatting.BOLD)
                    .formatted(color);
            if (spell.group != null) {
                var group = spellGroup(spell.group);
                if (!group.isEmpty()) {
                    name.append(Text.literal(" " + group))
                            .formatted(color);
                }
            }
            lines.add(indentation(indentLevel)
                    .append(name));
            indentLevel += 1;
        }

        if (shouldShow(spell.tooltip.description, details)) {
            var color = Formatting.byName(spell.tooltip.description.color);
            var description = createDescription(spellId, player, itemStack, spell, primaryPower);
            lines.add(indentation(indentLevel)
                    .append(Text.translatable(description))
                    .formatted(color));
        }

        if (details) {
            if (SpellHelper.isInstant(spell)) {
                lines.add(indentation(indentLevel)
                        .append(Text.translatable("spell.tooltip.cast_instant"))
                        .formatted(Formatting.GOLD));
            } else {
                var castDuration = SpellHelper.getCastDuration(player, spell, itemStack);
                var castTimeKey = keyWithPlural("spell.tooltip.cast_time", castDuration);
                var castTime = I18n.translate(castTimeKey).replace(placeholder(durationToken), formattedNumber(castDuration));
                lines.add(indentation(indentLevel)
                        .append(Text.literal(castTime))
                        .formatted(Formatting.GOLD));
            }

            if (spell.range > 0 || spell.range_melee) {
                String rangeText;
                if (spell.range_melee) {
                    if (spell.range == 0) {
                        rangeText = I18n.translate("spell.tooltip.range.melee");
                    } else {
                        var key = spell.range > 0 ? "spell.tooltip.range.melee.plus" : "spell.tooltip.range.melee.minus";
                        var rangeKey = keyWithPlural(key, spell.range);
                        rangeText = I18n.translate(rangeKey).replace(placeholder(rangeToken), formattedNumber(Math.abs(spell.range))); // Abs to avoid "--1"
                    }
                } else {
                    var rangeKey = keyWithPlural("spell.tooltip.range", spell.range);
                    rangeText = I18n.translate(rangeKey).replace(placeholder(rangeToken), formattedNumber(spell.range));
                }
                lines.add(indentation(indentLevel)
                        .append(Text.literal(rangeText))
                        .formatted(Formatting.GOLD));
            }

            var cooldownDuration = SpellHelper.getCooldownDuration(player, spell, itemStack);
            if (cooldownDuration > 0) {
                String cooldown;
                if (spell.cost.cooldown.proportional) {
                    cooldown = I18n.translate("spell.tooltip.cooldown.proportional");
                } else {
                    var cooldownKey = keyWithPlural("spell.tooltip.cooldown", cooldownDuration);
                    cooldown = I18n.translate(cooldownKey).replace(placeholder(durationToken), formattedNumber(cooldownDuration));
                }
                lines.add(indentation(indentLevel)
                        .append(Text.literal(cooldown))
                        .formatted(Formatting.GOLD));
            }

            var showItemCost = true;
            var config = SpellEngineMod.config;
            if (config != null) {
                showItemCost = config.spell_cost_item_allowed;
            }
            if (showItemCost) {
                var ammoResult = Ammo.ammoForSpell(player, spell, itemStack);
                if (ammoResult.item() != null) {
                    var amount = spell.cost.item.amount;
                    var ammoKey = keyWithPlural("spell.tooltip.ammo", amount);
                    var itemName = I18n.translate(ammoResult.item().getTranslationKey());
                    var ammo = I18n.translate(ammoKey)
                            .replace(placeholder(itemToken), itemName)
                            .replace(placeholder(countToken), amount + "");
                    lines.add(indentation(indentLevel)
                            .append(Text.literal(ammo).formatted(ammoResult.satisfied() ? Formatting.GREEN : Formatting.RED)));
                }
            }
        }

        return lines;
    }

    public static boolean shouldShow(Spell.Tooltip.LineOptions options, boolean details) {
        return details ? options.show_in_details : options.show_in_compact;
    }

    private static String createDescription(Identifier spellId, PlayerEntity player, ItemStack itemStack, Spell spell, SpellPower.Result primaryPower) {
        var description = I18n.translate(spellKeyPrefix(spellId) + ".description");

        List<Spell.Trigger> triggers = new ArrayList<>();
        if (spell.passive != null) {
            triggers.add(spell.passive.trigger);
        }
        if (spell.deliver != null) {
            Spell.ProjectileData projectile = null;
            if (spell.deliver.projectile != null) {
                projectile = spell.deliver.projectile.projectile;
            }
            if (spell.deliver.meteor != null) {
                projectile = spell.deliver.meteor.projectile;
            }
            if (projectile != null) {
                if (projectile.perks.ricochet > 0) {
                    description = description.replace(placeholder("ricochet"), formattedNumber(projectile.perks.ricochet));
                }
                if (projectile.perks.bounce > 0) {
                    description = description.replace(placeholder("bounce"), formattedNumber(projectile.perks.bounce));
                }
                if (projectile.perks.pierce > 0) {
                    description = description.replace(placeholder("pierce"), formattedNumber(projectile.perks.pierce));
                }
                if (projectile.perks.chain_reaction_size > 0) {
                    description = description.replace(placeholder("chain_reaction_size"), formattedNumber(projectile.perks.chain_reaction_size));
                }
            }

            Spell.LaunchProperties launchProperties = null;
            if (spell.deliver.projectile != null) {
                launchProperties = spell.deliver.projectile.launch_properties;
            }
            if (spell.deliver.meteor != null) {
                launchProperties = spell.deliver.meteor.launch_properties;
            }
            if (launchProperties != null) {
                var extra_launch_count = launchProperties.extra_launch_count;
                if (extra_launch_count > 0) {
                    description = description.replace(placeholder("extra_launch"), formattedNumber(extra_launch_count));
                }
            }
            var cloud = spell.deliver.cloud;
            if (spell.deliver.clouds.length > 0) {
                cloud = spell.deliver.clouds[0];
            }
            if (cloud != null) {
                var cloud_duration = cloud.time_to_live_seconds;
                if (cloud_duration > 0) {
                    description = description.replace(placeholder("cloud_duration"), formattedNumber(cloud_duration));
                }
                var radius = cloud.volume.combinedRadius(primaryPower);
                description = description.replace(placeholder("cloud_radius"), formattedNumber(radius));
            }
            if (spell.deliver.stash_effect != null) {
                var stash = spell.deliver.stash_effect;
                triggers.add(stash.trigger);
            }
        }

        if (spell.impact != null) {
            var estimatedOutput = SpellHelper.estimate(spell, player, itemStack);
            for (var impact : spell.impact) {
                switch (impact.action.type) {
                    case DAMAGE -> {
                        description = replaceDamageTokens(description, damageToken, estimatedOutput.damage());
                    }
                    case HEAL -> {
                        description = replaceDamageTokens(description, healToken, estimatedOutput.heal());
                    }
                    case STATUS_EFFECT -> {
                        var statusEffect = impact.action.status_effect;
                        description = description.replace(placeholder(effectAmplifierToken), "" + (statusEffect.amplifier + 1));
                        description = description.replace(placeholder(effectDurationToken), formattedNumber(statusEffect.duration));
                    }
                    case TELEPORT -> {
                        var teleport = impact.action.teleport;
                        switch (teleport.mode) {
                            case FORWARD -> {
                                var forward = teleport.forward;
                                description = description.replace(placeholder(teleportDistanceToken), formattedNumber(forward.distance));
                            }
                        }
                    }
                }
            }
            var area_impact = spell.area_impact;
            if (area_impact != null) {
                var radius = area_impact.combinedRadius(primaryPower);
                description = description.replace(placeholder(impactRangeToken), formattedNumber(radius));
            }
        }

        var triggerChances = triggers.stream().map(trigger -> (int)(trigger.chance * 100) + "%").toList();
        description = replaceTokens(description, trigger_chance, triggerChances);

        var mutator = descriptionMutators.get(spellId);
        if (mutator != null) {
            var args = new DescriptionMutator.Args(description, player);
            description = mutator.mutate(args);
        }
        return description;
    }

    private static MutableText indentation(int level) {
        return Text.literal(level > 0 ? " ".repeat(level) : "");
    }

    private static String replaceDamageTokens(String text, String token, List<SpellHelper.EstimatedValue> values) {
        boolean indexTokens = values.size() > 1;
        for (int i = 0; i < values.size(); ++i) {
            var range = values.get(i);
            var actualToken = indexTokens ? placeholder(token + "_" + (i + 1)) : placeholder(token);
            text = text.replace(actualToken, formattedRange(range.min(), range.max()));
        }
        return text;
    }

    private static String replaceTokens(String text, String token, List<String> values) {
        boolean indexTokens = values.size() > 1;
        for (int i = 0; i < values.size(); ++i) {
            var actualToken = indexTokens ? placeholder(token + "_" + (i + 1)) : placeholder(token);
            text = text.replace(actualToken, values.get(i));
        }
        return text;
    }

    private static String formattedRange(double min, double max) {
        if (min == max) {
            return formattedNumber((float) min);
        }
        return formattedNumber((float) min) + " - " + formattedNumber((float) max);
    }

    private static String formattedNumber(float number) {
        DecimalFormat formatter = new DecimalFormat();
        formatter.setMaximumFractionDigits(1);
        return formatter.format(number);
    }

    private static String keyWithPlural(String key, float value) {
        if (value != 1) {
            return key + ".plural";
        }
        return key;
    }

    public static String spellTranslationKey(Identifier spellId) {
        return spellKeyPrefix(spellId) + ".name";
    }

    public static String spellKeyPrefix(Identifier spellId) {
        // For example: `spell.spell_engine.fireball`
        return "spell." + spellId.getNamespace() + "." + spellId.getPath();
    }

    public static String spellGroup(String group) {
        var key = "spell.group." + group;
        if (I18n.hasTranslation(key)) {
            return I18n.translate(key);
        } else {
            return "";
        }
    }

    public interface DescriptionMutator {
        record Args(String description, PlayerEntity player) { }
        String mutate(Args args);
    }

    private static final Map<Identifier, DescriptionMutator> descriptionMutators = new HashMap<>();

    public static void addDescriptionMutator(Identifier spellId, DescriptionMutator handler) {
        descriptionMutators.put(spellId, handler);
    }
}
