package net.xuwu.walking_cane.event;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.xuwu.walking_cane.WalkingCane;

public final class CaneInteractionEvents {
    private CaneInteractionEvents() {
    }

    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        if (event.getHand() == InteractionHand.OFF_HAND
                && player.isShiftKeyDown()
                && player.getOffhandItem().is(Items.ENDER_PEARL)
                && player.getMainHandItem().is(WalkingCane.ENDER_CANE.get())
                && player.getCooldowns().isOnCooldown(WalkingCane.ENDER_CANE.get())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }
}
