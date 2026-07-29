package net.xuwu.walking_cane.event;

import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.xuwu.walking_cane.item.WalkingCaneItem;

public final class CaneAttributeEvents {
    private CaneAttributeEvents() {
    }

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        WalkingCaneItem.refreshBothHandAttributes(event.getEntity());
    }
}
