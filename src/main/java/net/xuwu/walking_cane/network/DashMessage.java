package net.xuwu.walking_cane.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.network.NetworkEvent;
import net.xuwu.walking_cane.item.WalkingCaneItem;

import java.util.function.Supplier;

public record DashMessage(InteractionHand hand, float strafe, float forward) {
    public static void encode(DashMessage message, FriendlyByteBuf buffer) {
        buffer.writeEnum(message.hand);
        buffer.writeFloat(message.strafe);
        buffer.writeFloat(message.forward);
    }

    public static DashMessage decode(FriendlyByteBuf buffer) {
        return new DashMessage(
                buffer.readEnum(InteractionHand.class),
                buffer.readFloat(),
                buffer.readFloat()
        );
    }

    public static void handle(DashMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
        ServerPlayer player = contextSupplier.get().getSender();
        if (player != null) {
            WalkingCaneItem.tryDash(
                    player,
                    message.hand,
                    message.strafe,
                    message.forward
            );
        }
    }
}
