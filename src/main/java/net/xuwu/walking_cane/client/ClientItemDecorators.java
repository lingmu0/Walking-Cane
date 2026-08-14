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
import net.minecraftforge.registries.ForgeRegistries;
import net.xuwu.walking_cane.WalkingCane;
import net.xuwu.walking_cane.item.CooldownStorageManager;

/** Renders current cooldown-storage charges over item icons. */
@Mod.EventBusSubscriber(
        modid = WalkingCane.MOD_ID,
        value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.MOD
)
public final class ClientItemDecorators {
    private static final IItemDecorator COOLDOWN_STORAGE_DECORATOR =
            ClientItemDecorators::renderCooldownStorage;

    private ClientItemDecorators() {
    }

    @SubscribeEvent
    public static void registerItemDecorations(RegisterItemDecorationsEvent event) {
        ForgeRegistries.ITEMS.getValues()
                .forEach(item -> event.register(item, COOLDOWN_STORAGE_DECORATOR));
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
                || CooldownStorageManager.level(stack) <= 0) {
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
