package net.xuwu.walking_cane.event;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.xuwu.walking_cane.WalkingCane;
import net.xuwu.walking_cane.config.WalkingCaneConfig;
import net.xuwu.walking_cane.item.WalkingCaneItem;

public final class CaneInteractionEvents {
    private CaneInteractionEvents() {
    }

    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        InteractionHand consumableHand = event.getHand();
        InteractionHand caneHand = consumableHand == InteractionHand.MAIN_HAND
                ? InteractionHand.OFF_HAND
                : InteractionHand.MAIN_HAND;

        if (!player.isShiftKeyDown()
                || !WalkingCaneConfig.isTeleportConsumable(
                        player.getItemInHand(consumableHand)
                )
                || !player.getItemInHand(caneHand).is(WalkingCane.ENDER_CANE.get())
                || !WalkingCaneConfig.isHandEnabled(caneHand)) {
            return;
        }

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (event.getLevel() instanceof ServerLevel serverLevel
                && player instanceof ServerPlayer serverPlayer
                && player.getItemInHand(caneHand).getItem() instanceof WalkingCaneItem cane) {
            cane.tryTeleport(serverLevel, serverPlayer, caneHand);
        }
    }
}
