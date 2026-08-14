package net.xuwu.walking_cane.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.xuwu.walking_cane.WalkingCane;
import net.xuwu.walking_cane.config.WalkingCaneConfig;
import net.xuwu.walking_cane.item.CooldownStorageManager;
import net.xuwu.walking_cane.item.WalkingCaneItem;
import org.lwjgl.glfw.GLFW;

/** Captures dash clicks that vanilla suppresses while an item cooldown is active. */
@Mod.EventBusSubscriber(
        modid = WalkingCane.MOD_ID,
        value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class ClientInputEvents {
    private ClientInputEvents() {
    }

    @SubscribeEvent
    public static void onMouseButton(InputEvent.MouseButton.Pre event) {
        if (event.getButton() != GLFW.GLFW_MOUSE_BUTTON_RIGHT
                || event.getAction() != InputConstants.PRESS
                || Minecraft.getInstance().screen != null) {
            return;
        }

        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }

        boolean teleport = player.isShiftKeyDown();
        InteractionHand hand = teleport
                ? findTeleportHand(player)
                : findCapturableHand(player);
        if (hand == null) {
            hand = findGenericHand(player);
            if (hand == null) {
                return;
            }
            ClientCooldownUseSender.send(hand);
            event.setCanceled(true);
            return;
        }

        if (teleport) {
            ClientTeleportSender.send(hand);
        } else {
            ClientDashSender.send(hand);
        }
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onInteractionKeyMappingTriggered(
            InputEvent.InteractionKeyMappingTriggered event
    ) {
        if (!event.isUseItem()) {
            return;
        }

        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }

        if (player.isShiftKeyDown()) {
            InteractionHand teleportHand = findTeleportHand(player);
            if (teleportHand != null) {
                ClientTeleportSender.send(teleportHand);
                event.setCanceled(true);
                return;
            }
            InteractionHand genericHand = findGenericHand(player);
            if (genericHand != null) {
                ClientCooldownUseSender.send(genericHand);
                event.setCanceled(true);
            }
            return;
        }

        InteractionHand hand = event.getHand();
        ItemStack stack = player.getItemInHand(hand);
        if (!(stack.getItem() instanceof WalkingCaneItem)) {
            if (isGenericCapturable(player, hand)) {
                ClientCooldownUseSender.send(hand);
                event.setCanceled(true);
            }
            return;
        }
        if (!WalkingCaneConfig.isHandEnabled(hand)) {
            return;
        }

        if (!(stack.getItem() instanceof WalkingCaneItem cane)
                || !cane.supportsDashStorage()
                || CooldownStorageManager.level(stack) <= 0
                || !player.getCooldowns().isOnCooldown(stack.getItem())
                || WalkingCaneItem.getStoredDashCharges(stack) <= 0) {
            return;
        }

        ClientDashSender.send(hand);
        event.setCanceled(true);
    }

    private static InteractionHand findCapturableHand(LocalPlayer player) {
        if (isCapturable(player, InteractionHand.MAIN_HAND)) {
            return InteractionHand.MAIN_HAND;
        }
        if (isCapturable(player, InteractionHand.OFF_HAND)) {
            return InteractionHand.OFF_HAND;
        }
        return null;
    }

    private static boolean isCapturable(LocalPlayer player, InteractionHand hand) {
        if (!WalkingCaneConfig.isHandEnabled(hand)) {
            return false;
        }

        ItemStack stack = player.getItemInHand(hand);
        return stack.getItem() instanceof WalkingCaneItem cane
                && cane.supportsDashStorage()
                && CooldownStorageManager.level(stack) > 0
                && player.getCooldowns().isOnCooldown(stack.getItem())
                && WalkingCaneItem.getStoredDashCharges(stack) > 0;
    }

    private static InteractionHand findTeleportHand(LocalPlayer player) {
        if (isTeleportCapturable(player, InteractionHand.MAIN_HAND)) {
            return InteractionHand.MAIN_HAND;
        }
        if (isTeleportCapturable(player, InteractionHand.OFF_HAND)) {
            return InteractionHand.OFF_HAND;
        }
        return null;
    }

    private static boolean isTeleportCapturable(LocalPlayer player, InteractionHand hand) {
        if (!WalkingCaneConfig.isHandEnabled(hand)) {
            return false;
        }

        ItemStack stack = player.getItemInHand(hand);
        return stack.getItem() instanceof WalkingCaneItem cane
                && cane.supportsEnderPearlSaver()
                && CooldownStorageManager.level(stack) > 0
                && player.getCooldowns().isOnCooldown(stack.getItem())
                && WalkingCaneItem.getStoredDashCharges(stack) > 0;
    }

    private static InteractionHand findGenericHand(LocalPlayer player) {
        if (isGenericCapturable(player, InteractionHand.MAIN_HAND)) {
            return InteractionHand.MAIN_HAND;
        }
        if (isGenericCapturable(player, InteractionHand.OFF_HAND)) {
            return InteractionHand.OFF_HAND;
        }
        return null;
    }

    private static boolean isGenericCapturable(LocalPlayer player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        int storageLevel = CooldownStorageManager.level(stack);
        return !stack.isEmpty()
                && !(stack.getItem() instanceof WalkingCaneItem)
                && storageLevel > 0
                && CooldownStorageManager.getStoredCharges(stack) > 0
                && player.getCooldowns().isOnCooldown(stack.getItem());
    }
}
