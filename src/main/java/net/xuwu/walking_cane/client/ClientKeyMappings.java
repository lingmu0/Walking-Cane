package net.xuwu.walking_cane.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.xuwu.walking_cane.WalkingCane;

/** Client key mappings for cane abilities. Both mappings are intentionally unbound by default. */
@EventBusSubscriber(
        modid = WalkingCane.MOD_ID,
        value = Dist.CLIENT,
        bus = EventBusSubscriber.Bus.MOD
)
public final class ClientKeyMappings {
    private static final int UNBOUND_KEY = InputConstants.UNKNOWN.getValue();

    public static final KeyMapping DASH = new KeyMapping(
            "key.walking_cane.dash",
            InputConstants.Type.KEYSYM,
            UNBOUND_KEY,
            "key.categories.walking_cane"
    );
    public static final KeyMapping TELEPORT = new KeyMapping(
            "key.walking_cane.teleport",
            InputConstants.Type.KEYSYM,
            UNBOUND_KEY,
            "key.categories.walking_cane"
    );

    private ClientKeyMappings() {
    }

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(DASH);
        event.register(TELEPORT);
    }

    public static boolean isDashKeyBound() {
        return DASH.getKey().getValue() != UNBOUND_KEY;
    }

    public static boolean isTeleportKeyBound() {
        return TELEPORT.getKey().getValue() != UNBOUND_KEY;
    }
}
