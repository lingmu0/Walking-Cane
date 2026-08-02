package net.xuwu.walking_cane.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.xuwu.walking_cane.WalkingCane;
import net.xuwu.walking_cane.item.CooldownStorageManager;

/** Requests an extra use of an arbitrary item while its vanilla cooldown is active. */
public record CooldownUsePayload(InteractionHand hand) implements CustomPacketPayload {
    public static final Type<CooldownUsePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(WalkingCane.MOD_ID, "cooldown_use")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, CooldownUsePayload> STREAM_CODEC =
            StreamCodec.of(
                    (buffer, payload) -> buffer.writeEnum(payload.hand),
                    buffer -> new CooldownUsePayload(buffer.readEnum(InteractionHand.class))
            );

    public static void handle(CooldownUsePayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            CooldownStorageManager.tryUseDuringCooldown(player, payload.hand);
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
