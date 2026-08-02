package net.xuwu.walking_cane.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.IItemDecorator;
import net.neoforged.neoforge.client.event.RegisterItemDecorationsEvent;
import net.xuwu.walking_cane.WalkingCane;
import net.xuwu.walking_cane.enchantment.WalkingCaneEnchantments;
import net.xuwu.walking_cane.item.CooldownStorageManager;

/** Renders current cooldown-storage charges over item icons. */
@EventBusSubscriber(
        modid = WalkingCane.MOD_ID,
        value = Dist.CLIENT,
        bus = EventBusSubscriber.Bus.MOD
)
public final class ClientItemDecorators {
    private static final IItemDecorator COOLDOWN_STORAGE_DECORATOR =
            ClientItemDecorators::renderCooldownStorage;

    private ClientItemDecorators() {
    }

    @SubscribeEvent
    public static void registerItemDecorations(RegisterItemDecorationsEvent event) {
        for (Item item : BuiltInRegistries.ITEM) {
            event.register(item, COOLDOWN_STORAGE_DECORATOR);
        }
    }

    private static boolean renderCooldownStorage(
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
                        WalkingCaneEnchantments.COOLDOWN_STORAGE
                ) <= 0) {
            return false;
        }

        int storedCharges = CooldownStorageManager.getStoredCharges(stack);
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
