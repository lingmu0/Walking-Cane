package net.xuwu.walking_cane.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.neoforged.neoforge.network.PacketDistributor;
import net.xuwu.walking_cane.network.DashPayload;

public final class ClientDashSender {
    private ClientDashSender() {
    }

    public static void send(InteractionHand hand) {
        send(hand, false);
    }

    public static void sendFromKeyMapping(InteractionHand hand) {
        send(hand, true);
    }

    private static void send(InteractionHand hand, boolean forceDash) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && Minecraft.getInstance().getConnection() != null) {
            PacketDistributor.sendToServer(new DashPayload(
                    hand,
                    player.input.leftImpulse,
                    player.input.forwardImpulse,
                    false,
                    forceDash
            ));
        }
    }
}
