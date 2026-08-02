package net.xuwu.walking_cane.client;

import net.minecraft.world.InteractionHand;
import net.neoforged.neoforge.network.PacketDistributor;
import net.xuwu.walking_cane.network.CooldownUsePayload;

public final class ClientCooldownUseSender {
    private ClientCooldownUseSender() {
    }

    public static void send(InteractionHand hand) {
        PacketDistributor.sendToServer(new CooldownUsePayload(hand));
    }
}
