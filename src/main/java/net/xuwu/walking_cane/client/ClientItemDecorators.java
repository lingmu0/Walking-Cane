package net.xuwu.walking_cane.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.IItemDecorator;
import net.minecraftforge.client.event.RegisterItemDecorationsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.xuwu.walking_cane.WalkingCane;
import net.xuwu.walking_cane.enchantment.WalkingCaneEnchantments;
import net.xuwu.walking_cane.item.WalkingCaneItem;

/** Renders the current stored dash charges in the top-right of cane icons. */
@Mod.EventBusSubscriber(
        modid = WalkingCane.MOD_ID,
        value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.MOD
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
                        stack,
                        WalkingCaneEnchantments.DASH_STORAGE
                ) <= 0) {
            return false;
        }

        int storedCharges = WalkingCaneItem.getStoredDashCharges(stack);
        if (storedCharges <= 0) {
            return false;
        }

        String count = Integer.toString(storedCharges);
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.0F, 0.0F, 250.0F);
        guiGraphics.drawString(font, count, xOffset + 1, yOffset + 1, 0xFFFFFFFF, true);
        guiGraphics.pose().popPose();
        return false;
    }
}
