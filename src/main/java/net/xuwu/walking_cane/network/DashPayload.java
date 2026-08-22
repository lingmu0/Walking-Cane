package net.xuwu.walking_cane.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.xuwu.walking_cane.WalkingCane;
import net.xuwu.walking_cane.item.WalkingCaneItem;

public record DashPayload(
        InteractionHand hand,
        float strafe,
        float forward,
        boolean teleport,
        boolean forceDash
)
        implements CustomPacketPayload {
    public DashPayload(InteractionHand hand, float strafe, float forward) {
        this(hand, strafe, forward, false, false);
    }

    public static DashPayload teleport(InteractionHand hand) {
        return new DashPayload(hand, 0.0F, 0.0F, true, false);
    }

    public static final Type<DashPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(WalkingCane.MOD_ID, "dash")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, DashPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeEnum(payload.hand);
                buffer.writeFloat(payload.strafe);
                buffer.writeFloat(payload.forward);
                buffer.writeBoolean(payload.teleport);
                buffer.writeBoolean(payload.forceDash);
            },
            buffer -> new DashPayload(
                    buffer.readEnum(InteractionHand.class),
                    buffer.readFloat(),
                    buffer.readFloat(),
                    buffer.readBoolean(),
                    buffer.readBoolean()
            )
    );

    public static void handle(DashPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            if (payload.teleport && player.level() instanceof ServerLevel serverLevel) {
                if (player.getItemInHand(payload.hand).getItem() instanceof WalkingCaneItem cane
                        && cane.supportsEnderPearlSaver()) {
                    cane.tryTeleport(serverLevel, player, payload.hand);
                }
            } else {
                WalkingCaneItem.tryDash(
                        player,
                        payload.hand,
                        payload.strafe,
                        payload.forward,
                        payload.forceDash
                );
            }
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
