package net.xuwu.walking_cane.item;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.ForgeHooks;
import net.xuwu.walking_cane.WalkingCane;
import net.xuwu.walking_cane.enchantment.WalkingCaneEnchantments;

/** Generic cooldown-storage behavior for non-cane items. */
public final class CooldownStorageManager {
    private static final String STORAGE_TAG = WalkingCane.MOD_ID + ".cooldown_storage";
    private static final String ACTIVE_TAG = WalkingCane.MOD_ID + ".cooldown_storage_active";

    private CooldownStorageManager() {
    }

    public static int level(ItemStack stack) {
        return WalkingCaneEnchantments.level(stack, WalkingCaneEnchantments.COOLDOWN_STORAGE);
    }

    public static int getStoredCharges(ItemStack stack) {
        return stack.hasTag() ? stack.getTag().getInt(STORAGE_TAG) : 0;
    }

    private static boolean hasStoredCharges(ItemStack stack) {
        return stack.hasTag() && stack.getTag().contains(STORAGE_TAG);
    }

    private static void setStoredCharges(ItemStack stack, int value) {
        stack.getOrCreateTag().putInt(STORAGE_TAG, Math.max(0, value));
    }

    private static boolean hasActiveCooldown(ItemStack stack) {
        return stack.hasTag() && stack.getTag().getBoolean(ACTIVE_TAG);
    }

    private static void setActiveCooldown(ItemStack stack, boolean active) {
        if (active) {
            stack.getOrCreateTag().putBoolean(ACTIVE_TAG, true);
        } else if (stack.hasTag()) {
            stack.getTag().remove(ACTIVE_TAG);
        }
    }

    public static boolean isCapturable(ServerPlayer player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        return !stack.isEmpty()
                && !(stack.getItem() instanceof WalkingCaneItem)
                && level(stack) > 0
                && getStoredCharges(stack) > 0
                && player.getCooldowns().isOnCooldown(stack.getItem());
    }

    public static void tick(ServerPlayer player) {
        tickStack(player, player.getMainHandItem());
        tickStack(player, player.getOffhandItem());
    }

    private static void tickStack(ServerPlayer player, ItemStack stack) {
        if (stack.isEmpty() || stack.getItem() instanceof WalkingCaneItem) {
            return;
        }

        int storageLevel = level(stack);
        if (storageLevel <= 0) {
            return;
        }

        if (!hasStoredCharges(stack)) {
            setStoredCharges(stack, storageLevel);
            return;
        }

        int stored = Math.min(getStoredCharges(stack), storageLevel);
        if (stored != getStoredCharges(stack)) {
            setStoredCharges(stack, stored);
        }

        if (player.getCooldowns().isOnCooldown(stack.getItem())) {
            setActiveCooldown(stack, true);
            return;
        }

        if (hasActiveCooldown(stack)) {
            if (stored < storageLevel) {
                setStoredCharges(stack, stored + 1);
            }
            setActiveCooldown(stack, false);
        }
    }

    public static boolean tryUseDuringCooldown(ServerPlayer player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!isCapturable(player, hand)) {
            return false;
        }

        InteractionResult eventResult = ForgeHooks.onItemRightClick(player, hand);
        if (eventResult != null) {
            return false;
        }

        Level level = player.level();
        InteractionResultHolder<ItemStack> result = stack.use(level, player, hand);
        if (!result.getResult().consumesAction()) {
            return false;
        }

        setStoredCharges(stack, getStoredCharges(stack) - 1);
        setActiveCooldown(stack, true);
        if (result.getObject() != stack) {
            player.setItemInHand(hand, result.getObject());
        }
        return true;
    }
}
