package net.xuwu.walking_cane.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.xuwu.walking_cane.WalkingCane;
import net.xuwu.walking_cane.item.WalkingCaneItem;

public record DashPayload(InteractionHand hand, float strafe, float forward) implements CustomPacketPayload {
    public static final Type<DashPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(WalkingCane.MOD_ID, "dash")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, DashPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeEnum(payload.hand);
                buffer.writeFloat(payload.strafe);
                buffer.writeFloat(payload.forward);
            },
            buffer -> new DashPayload(
                    buffer.readEnum(InteractionHand.class),
                    buffer.readFloat(),
                    buffer.readFloat()
            )
    );

    public static void handle(DashPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            WalkingCaneItem.tryDash(player, payload.hand, payload.strafe, payload.forward);
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
