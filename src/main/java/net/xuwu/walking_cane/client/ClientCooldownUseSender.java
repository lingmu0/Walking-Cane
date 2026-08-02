package net.xuwu.walking_cane.client;

import net.minecraft.world.InteractionHand;
import net.xuwu.walking_cane.network.CooldownUseMessage;
import net.xuwu.walking_cane.network.WalkingCaneNetwork;

public final class ClientCooldownUseSender {
    private ClientCooldownUseSender() {
    }

    public static void send(InteractionHand hand) {
        WalkingCaneNetwork.CHANNEL.sendToServer(new CooldownUseMessage(hand));
    }
}
