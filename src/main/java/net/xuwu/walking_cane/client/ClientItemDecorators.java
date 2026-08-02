package net.xuwu.walking_cane.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.IItemDecorator;
import net.neoforged.neoforge.client.event.RegisterItemDecorationsEvent;
import net.xuwu.walking_cane.WalkingCane;
import net.xuwu.walking_cane.enchantment.WalkingCaneEnchantments;
import net.xuwu.walking_cane.item.WalkingCaneItem;

/** Renders the current stored dash charges in the top-right of cane icons. */
@EventBusSubscriber(
        modid = WalkingCane.MOD_ID,
        value = Dist.CLIENT,
        bus = EventBusSubscriber.Bus.MOD
)
public final class ClientItemDecorators {
    private static final IItemDecorator DASH_STORAGE_DECORATOR =
            ClientItemDecorators::renderDashStorage;

    private ClientItemDecorators() {
    }

    @SubscribeEvent
    public static void registerItemDecorations(RegisterItemDecorationsEvent event) {
        event.register(WalkingCane.DIAMOND_CANE.get(), DASH_STORAGE_DECORATOR);
        event.register(WalkingCane.ENDER_CANE.get(), DASH_STORAGE_DECORATOR);
        event.register(WalkingCane.NETHERITE_CANE.get(), DASH_STORAGE_DECORATOR);
    }

    private static boolean renderDashStorage(
            GuiGraphics guiGraphics,
            Font font,
            ItemStack stack,
            int xOffset,
            int yOffset
    ) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null
                || WalkingCaneEnchantments.level(
                        minecraft.player,
                        stack,
                        WalkingCaneEnchantments.DASH_STORAGE
                ) <= 0) {
            return false;
        }

        String count = Integer.toString(WalkingCaneItem.getStoredDashCharges(stack));
        guiGraphics.drawString(
                font,
                count,
                xOffset + 17 - font.width(count),
                yOffset + 1,
                0xFFFFFFFF,
                true
        );
        return false;
    }
}
