package net.spell_engine.api.item.trinket;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public class SpellBookItem extends Item implements ISpellBookItem {
    public static final Identifier EQUIP_SOUND_ID = Identifier.of("spell_engine", "spellbook_equip");
    public static final SoundEvent EQUIP_SOUND = SoundEvent.of(EQUIP_SOUND_ID);

    private final Identifier poolId;
    public SpellBookItem(Identifier poolId, Settings settings) {
        super(settings);
        this.poolId = poolId;
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return false;
    }

    @Override
    public Identifier getPoolId() {
        return poolId;
    }
}
