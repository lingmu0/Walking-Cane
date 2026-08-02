package net.xuwu.walking_cane.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.network.NetworkEvent;
import net.xuwu.walking_cane.item.CooldownStorageManager;

import java.util.function.Supplier;

/** Requests an extra use of an arbitrary item while its vanilla cooldown is active. */
public record CooldownUseMessage(InteractionHand hand) {
    public static void encode(CooldownUseMessage message, FriendlyByteBuf buffer) {
        buffer.writeEnum(message.hand);
    }

    public static CooldownUseMessage decode(FriendlyByteBuf buffer) {
        return new CooldownUseMessage(buffer.readEnum(InteractionHand.class));
    }

    public static void handle(
            CooldownUseMessage message,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer player = context.getSender();
        if (player != null) {
            CooldownStorageManager.tryUseDuringCooldown(player, message.hand);
        }
        context.setPacketHandled(true);
    }
}
