package net.xuwu.walking_cane.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.xuwu.walking_cane.network.DashMessage;
import net.xuwu.walking_cane.network.WalkingCaneNetwork;

public final class ClientDashSender {
    private ClientDashSender() {
    }

    public static void send(InteractionHand hand) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && Minecraft.getInstance().getConnection() != null) {
            WalkingCaneNetwork.CHANNEL.sendToServer(new DashMessage(
                    hand,
                    player.input.leftImpulse,
                    player.input.forwardImpulse
            ));
        }
    }
}
