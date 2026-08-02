package net.xuwu.walking_cane.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.network.NetworkEvent;
import net.xuwu.walking_cane.item.WalkingCaneItem;

import java.util.function.Supplier;

public record DashMessage(InteractionHand hand, float strafe, float forward, boolean teleport) {
    public DashMessage(InteractionHand hand, float strafe, float forward) {
        this(hand, strafe, forward, false);
    }

    public static DashMessage teleport(InteractionHand hand) {
        return new DashMessage(hand, 0.0F, 0.0F, true);
    }

    public static void encode(DashMessage message, FriendlyByteBuf buffer) {
        buffer.writeEnum(message.hand);
        buffer.writeFloat(message.strafe);
        buffer.writeFloat(message.forward);
        buffer.writeBoolean(message.teleport);
    }

    public static DashMessage decode(FriendlyByteBuf buffer) {
        return new DashMessage(
                buffer.readEnum(InteractionHand.class),
                buffer.readFloat(),
                buffer.readFloat(),
                buffer.readBoolean()
        );
    }

    public static void handle(DashMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
        ServerPlayer player = contextSupplier.get().getSender();
        if (player != null) {
            if (message.teleport && player.level() instanceof ServerLevel serverLevel) {
                if (player.getItemInHand(message.hand).getItem() instanceof WalkingCaneItem cane
                        && cane.supportsEnderPearlSaver()) {
                    cane.tryTeleport(serverLevel, player, message.hand);
                }
            } else {
                WalkingCaneItem.tryDash(
                        player,
                        message.hand,
                        message.strafe,
                        message.forward
                );
            }
        }
    }
}
