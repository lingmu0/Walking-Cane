package net.xuwu.walking_cane.client;

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
import net.xuwu.walking_cane.enchantment.WalkingCaneEnchantments;
import net.xuwu.walking_cane.item.WalkingCaneItem;

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

        InteractionHand hand = event.getHand();
        if (!WalkingCaneConfig.isHandEnabled(hand)) {
            return;
        }

        ItemStack stack = player.getItemInHand(hand);
        if (!(stack.getItem() instanceof WalkingCaneItem cane)
                || !cane.supportsDashStorage()
                || WalkingCaneEnchantments.level(
                        stack,
                        WalkingCaneEnchantments.DASH_STORAGE
                ) <= 0
                || !player.getCooldowns().isOnCooldown(stack.getItem())
                || (cane.supportsEnderPearlSaver() && player.isShiftKeyDown())) {
            return;
        }

        ClientDashSender.send(hand);
        event.setCanceled(true);
    }
}
