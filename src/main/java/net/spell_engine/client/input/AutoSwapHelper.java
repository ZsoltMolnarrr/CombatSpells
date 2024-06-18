package net.spell_engine.client.input;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.UseAction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.spell_engine.api.spell.SpellContainer;
import net.spell_engine.compat.TrinketsCompat;
import net.spell_engine.internals.SpellContainerHelper;
import org.jetbrains.annotations.Nullable;

public class AutoSwapHelper {
    public static boolean autoSwapForAttack() {
        var player = MinecraftClient.getInstance().player;
        if (player == null || player.isSpectator()) { return false; }
        var mainHand = player.getMainHandStack();
        var offHand = player.getInventory().offHand.get(0);

        if (!isWeapon(mainHand) && isWeapon(offHand)) {
            swapHeldItems();
            return true;
        } else {
            return false;
        }
    }

    public static boolean autoSwapForSpells() {
        var player = MinecraftClient.getInstance().player;
        if (player == null || player.isSpectator()) { return false; }
        var mainHand = player.getMainHandStack();
        var offHand = player.getInventory().offHand.get(0);

        if (mainHand.getUseAction() != UseAction.NONE) {
            return false;
        }

        var mainHandType = spellContentType(mainHand);
        var offHandType = spellContentType(offHand);
        var spellbookType = spellContentType(TrinketsCompat.getSpellBookStack(player));
        if (spellbookType != null) {
            if (mainHandType != spellbookType && offHandType == spellbookType) {
                swapHeldItems();
                return true;
            }
        }
        return false;
    }

    public static void swapHeldItems() {
        MinecraftClient
                .getInstance()
                .getNetworkHandler()
                .sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ORIGIN, Direction.DOWN));
    }

    public static boolean isWeapon(ItemStack itemStack) {
        return itemStack.getAttributeModifiers(EquipmentSlot.MAINHAND).containsKey(EntityAttributes.GENERIC_ATTACK_DAMAGE);
    }

    @Nullable
    private static SpellContainer.ContentType spellContentType(ItemStack itemStack) {
        var container = SpellContainerHelper.containerFromItemStack(itemStack);
        if (container != null) {
            return container.content;
        }
        return null;
    }
}
