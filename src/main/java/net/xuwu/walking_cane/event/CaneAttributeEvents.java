package net.xuwu.walking_cane.event;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.xuwu.walking_cane.config.WalkingCaneConfig;
import net.xuwu.walking_cane.item.CooldownStorageManager;
import net.xuwu.walking_cane.item.WalkingCaneItem;

import java.util.List;

public final class CaneAttributeEvents {
    private CaneAttributeEvents() {
    }

    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            WalkingCaneItem.refreshBothHandAttributes(event.player);
            if (event.player instanceof ServerPlayer player) {
                CooldownStorageManager.tick(player);
            }
        }
    }

    public static void onItemTooltip(ItemTooltipEvent event) {
        if (WalkingCaneConfig.HAND_MODE != WalkingCaneConfig.HandMode.BOTH
                || !(event.getItemStack().getItem() instanceof WalkingCaneItem)) {
            return;
        }

        List<Component> tooltip = event.getToolTip();
        for (int index = 0; index < tooltip.size(); index++) {
            Component line = tooltip.get(index);
            if (line.getContents() instanceof TranslatableContents contents
                    && "item.modifiers.mainhand".equals(contents.getKey())) {
                tooltip.set(
                        index,
                        Component.translatable("tooltip.walking_cane.hand")
                                .withStyle(ChatFormatting.GRAY)
                );
            }
        }
    }
}
