package net.spell_engine.spellbinding;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.context.LootContextParameter;
import net.minecraft.loot.function.ConditionalLootFunction;
import net.minecraft.loot.function.LootFunctionType;
import net.minecraft.loot.provider.number.LootNumberProvider;
import net.minecraft.loot.provider.number.LootNumberProviderTypes;
import net.minecraft.util.Identifier;
import net.spell_engine.SpellEngineMod;
import net.spell_engine.api.spell.SpellDataComponents;
import net.spell_engine.api.spell.registry.SpellRegistry;
import net.spell_engine.api.spell.registry.SpellTags;
import net.spell_engine.api.spell.container.SpellContainerHelper;
import net.spell_engine.item.ScrollItem;
import net.spell_engine.item.SpellEngineItems;

import java.util.List;
import java.util.Set;

public class SpellBindRandomlyLootFunction extends ConditionalLootFunction {
    public static final String NAME = "spell_bind_randomly";
    public static final Identifier ID = Identifier.of(SpellEngineMod.ID, NAME);

    public static final MapCodec<SpellBindRandomlyLootFunction> CODEC = RecordCodecBuilder.mapCodec(
            instance -> addConditionsField(instance)
                    .<LootNumberProvider, Boolean>and(
                            instance.group(
                                    LootNumberProviderTypes.CODEC.fieldOf("tier").forGetter(function -> function.tier),
                                    Codec.BOOL.fieldOf("add").orElse(false).forGetter(function -> function.add)
                            )
                    )
                    .apply(instance, SpellBindRandomlyLootFunction::new)
    );
    public static final LootFunctionType<SpellBindRandomlyLootFunction> TYPE = new LootFunctionType<SpellBindRandomlyLootFunction>(CODEC);

    private final LootNumberProvider tier;
    private final boolean add;

    private SpellBindRandomlyLootFunction(List<LootCondition> conditions, LootNumberProvider tier, boolean add) {
        super(conditions);
        this.tier = tier;
        this.add = add;
    }

    @Override
    public LootFunctionType<SpellBindRandomlyLootFunction> getType() {
        return TYPE;
    }

    @Override
    public Set<LootContextParameter<?>> getRequiredParameters() {
        return this.tier.getRequiredParameters();
    }

    @Override
    public ItemStack process(ItemStack stack, LootContext context) {
        final var selectedTier = this.tier.nextInt(context);
        var spells = SpellRegistry.stream(context.getWorld())
                .filter(entry -> entry.value().tier == selectedTier
                        && (entry.value().active != null && entry.value().active.scroll != null)
                        && entry.isIn(SpellTags.TREASURE)
                )
                .toList();
        if (spells.size() > 0) {
            var spell = spells.get(context.getRandom().nextInt(spells.size()));
            var retryAttempts = 3;
            if (stack.getItem() == SpellEngineItems.SCROLL.get()) {
                var success = ScrollItem.applySpell(stack, spell, true);
                while (retryAttempts-- > 0 && !success) {
                    spell = spells.get(context.getRandom().nextInt(spells.size()));
                    success = ScrollItem.applySpell(stack, spell, true);
                }
                if (!success) {
                    return ItemStack.EMPTY;
                }
            } else {
                var isValid = SpellContainerHelper.isSpellValidForItem(stack.getItem(), spell);
                while (retryAttempts-- > 0 && !isValid) {
                    spell = spells.get(context.getRandom().nextInt(spells.size()));
                    isValid = SpellContainerHelper.isSpellValidForItem(stack.getItem(), spell);
                }
                if (isValid) {
                    var container = SpellContainerHelper.create(spell, stack.getItem());
                    stack.set(SpellDataComponents.SPELL_CONTAINER, container);
                }
            }
        } else {
            if (stack.getItem() == SpellEngineItems.SCROLL.get()) {
                return ItemStack.EMPTY;
            }
        }

        return stack;
    }

    public static ConditionalLootFunction.Builder<?> builder(LootNumberProvider tier) {
        return builder(conditions -> new SpellBindRandomlyLootFunction(conditions, tier, false));
    }

    public static ConditionalLootFunction.Builder<?> builder(LootNumberProvider tier, boolean add) {
        return builder(conditions -> new SpellBindRandomlyLootFunction(conditions, tier, add));
    }
}

