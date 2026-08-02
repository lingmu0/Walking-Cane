package net.xuwu.walking_cane.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.neoforged.neoforge.network.PacketDistributor;
import net.xuwu.walking_cane.network.DashPayload;

public final class ClientTeleportSender {
    private ClientTeleportSender() {
    }

    public static void send(InteractionHand hand) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && Minecraft.getInstance().getConnection() != null) {
            PacketDistributor.sendToServer(DashPayload.teleport(hand));
        }
    }
}
