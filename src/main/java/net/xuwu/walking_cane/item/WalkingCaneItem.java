package net.xuwu.walking_cane.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.xuwu.walking_cane.WalkingCane;
import net.xuwu.walking_cane.client.ClientDashSender;
import net.xuwu.walking_cane.config.WalkingCaneConfig;

import java.util.List;
import java.util.Locale;

public final class WalkingCaneItem extends Item {
    private static final int TELEPORT_COOLDOWN_TICKS = 200;

    private final int speedPercent;
    private final double dashStrength;
    private final int dashCooldownTicks;
    private final boolean canTeleport;

    public WalkingCaneItem(
            Properties properties,
            double speedBonus,
            double dashStrength,
            int dashCooldownTicks,
            boolean canTeleport
    ) {
        super(properties);
        this.speedPercent = Mth.floor(speedBonus * 100.0 + 0.5);
        this.dashStrength = dashStrength;
        this.dashCooldownTicks = dashCooldownTicks;
        this.canTeleport = canTeleport;
    }

    public static ItemAttributeModifiers createAttributes(double speedBonus) {
        ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder();
        if (WalkingCaneConfig.HAND_MODE != WalkingCaneConfig.HandMode.OFF_HAND) {
            addAttributes(builder, speedBonus, EquipmentSlotGroup.MAINHAND, "mainhand");
        }
        if (WalkingCaneConfig.HAND_MODE != WalkingCaneConfig.HandMode.MAIN_HAND) {
            addAttributes(builder, speedBonus, EquipmentSlotGroup.OFFHAND, "offhand");
        }
        return builder.build();
    }

    private static void addAttributes(
            ItemAttributeModifiers.Builder builder,
            double speedBonus,
            EquipmentSlotGroup slot,
            String slotName
    ) {
        AttributeModifier speedModifier = new AttributeModifier(
                ResourceLocation.fromNamespaceAndPath(
                        WalkingCane.MOD_ID,
                        "held_speed_bonus_" + slotName
                ),
                speedBonus,
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE
        );
        AttributeModifier stepHeightModifier = new AttributeModifier(
                ResourceLocation.fromNamespaceAndPath(
                        WalkingCane.MOD_ID,
                        "held_step_height_bonus_" + slotName
                ),
                1.0,
                AttributeModifier.Operation.ADD_VALUE
        );

        builder.add(Attributes.MOVEMENT_SPEED, speedModifier, slot);
        builder.add(NeoForgeMod.SWIM_SPEED, speedModifier, slot);
        builder.add(Attributes.STEP_HEIGHT, stepHeightModifier, slot);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!WalkingCaneConfig.isHandEnabled(hand)) {
            return InteractionResultHolder.pass(stack);
        }

        if (canTeleport && player.isShiftKeyDown()) {
            if (level instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer) {
                tryTeleport(serverLevel, serverPlayer, hand);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }

        if (canDash()) {
            if (level.isClientSide()) {
                ClientDashSender.send(hand);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }

        return InteractionResultHolder.pass(stack);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            List<Component> tooltip,
            TooltipFlag flag
    ) {
//        tooltip.add(Component.translatable("tooltip.walking_cane.speed", speedPercent)
//                .withStyle(ChatFormatting.AQUA));
//        tooltip.add(Component.translatable("tooltip.walking_cane.step_height")
//                .withStyle(ChatFormatting.AQUA));

        if (canDash()) {
            tooltip.add(Component.translatable(
                    "tooltip.walking_cane.dash",
                    formatSeconds(dashCooldownTicks)
            ).withStyle(ChatFormatting.GRAY));
        }

        if (canTeleport) {
            tooltip.add(Component.translatable("tooltip.walking_cane.teleport")
                    .withStyle(ChatFormatting.DARK_PURPLE));
            tooltip.add(Component.translatable(
                    "tooltip.walking_cane.teleport_range",
                    formatNumber(WalkingCaneConfig.TELEPORT_BASE_DISTANCE),
                    formatNumber(WalkingCaneConfig.TELEPORT_DISTANCE_PER_PEARL)
            )
                    .withStyle(ChatFormatting.GRAY));
        }
    }

    public static void tryDash(ServerPlayer player, InteractionHand hand, float rawStrafe, float rawForward) {
        if (!WalkingCaneConfig.isHandEnabled(hand)) {
            return;
        }

        ItemStack stack = player.getItemInHand(hand);
        if (!(stack.getItem() instanceof WalkingCaneItem cane) || !cane.canDash()) {
            return;
        }

        if (cane.canTeleport && player.isShiftKeyDown()) {
            return;
        }

        if (player.getCooldowns().isOnCooldown(cane)) {
            return;
        }

        float strafe = Mth.clamp(rawStrafe, -1.0F, 1.0F);
        float forward = Mth.clamp(rawForward, -1.0F, 1.0F);
        Vec3 direction;

        if (strafe * strafe + forward * forward > 1.0E-4F) {
            double yaw = Math.toRadians(player.getYRot());
            Vec3 lookDirection = player.getLookAngle().normalize();
            Vec3 horizontalForward = new Vec3(-Math.sin(yaw), 0.0, Math.cos(yaw));
            Vec3 rightDirection = new Vec3(Math.cos(yaw), 0.0, Math.sin(yaw));
            Vec3 forwardDirection = forward > 0.0F ? lookDirection : horizontalForward;
            direction = forwardDirection.scale(forward)
                    .add(rightDirection.scale(strafe))
                    .normalize();
        } else {
            direction = player.getLookAngle().normalize();
        }

        Vec3 dashVelocity = direction.scale(WalkingCaneConfig.DASH_STRENGTH);
        if (Math.abs(direction.y) < 1.0E-4) {
            double verticalVelocity = player.onGround()
                    ? 0.18
                    : player.getDeltaMovement().y;
            dashVelocity = new Vec3(dashVelocity.x, verticalVelocity, dashVelocity.z);
        }

        player.setDeltaMovement(dashVelocity);
        player.hasImpulse = true;
        player.hurtMarked = true;
        player.resetFallDistance();

        player.getCooldowns().addCooldown(cane, cane.dashCooldownTicks);
        damageCane(stack, player, hand);
        player.awardStat(Stats.ITEM_USED.get(cane));
        player.level().playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP,
                SoundSource.PLAYERS,
                0.8F,
                1.25F
        );
    }

    public void tryTeleport(ServerLevel level, ServerPlayer player, InteractionHand caneHand) {
        if (!WalkingCaneConfig.isHandEnabled(caneHand)) {
            return;
        }

        if (player.getCooldowns().isOnCooldown(this)) {
            return;
        }

        InteractionHand pearlHand = caneHand == InteractionHand.MAIN_HAND
                ? InteractionHand.OFF_HAND
                : InteractionHand.MAIN_HAND;
        ItemStack pearls = player.getItemInHand(pearlHand);
        int pearlCount = pearls.is(Items.ENDER_PEARL) ? pearls.getCount() : 0;
        double maxDistance = WalkingCaneConfig.TELEPORT_BASE_DISTANCE
                + pearlCount * WalkingCaneConfig.TELEPORT_DISTANCE_PER_PEARL;
        Vec3 look = player.getLookAngle().normalize();
        Vec3 target = player.position().add(look.scale(maxDistance));
        BlockPos targetBlock = BlockPos.containing(target);

        // Heightmaps are not reliable until the destination chunk has been loaded or generated.
        level.getChunkAt(targetBlock);
        BlockPos surface = level.getHeightmapPos(
                Heightmap.Types.WORLD_SURFACE,
                targetBlock
        );
        Vec3 destination = new Vec3(target.x, surface.getY(), target.z);

        Vec3 origin = player.position();
        level.sendParticles(
                ParticleTypes.PORTAL,
                origin.x,
                origin.y + player.getBbHeight() * 0.5,
                origin.z,
                48,
                0.35,
                0.75,
                0.35,
                0.1
        );
        level.playSound(
                null,
                origin.x,
                origin.y,
                origin.z,
                SoundEvents.ENDERMAN_TELEPORT,
                SoundSource.PLAYERS,
                1.0F,
                1.0F
        );

        player.teleportTo(level, destination.x, destination.y, destination.z, player.getYRot(), player.getXRot());
        player.setDeltaMovement(Vec3.ZERO);
        player.resetFallDistance();

        level.sendParticles(
                ParticleTypes.PORTAL,
                destination.x,
                destination.y + player.getBbHeight() * 0.5,
                destination.z,
                48,
                0.35,
                0.75,
                0.35,
                0.1
        );
        level.playSound(
                null,
                destination.x,
                destination.y,
                destination.z,
                SoundEvents.ENDERMAN_TELEPORT,
                SoundSource.PLAYERS,
                1.0F,
                1.0F
        );

        if (!player.getAbilities().instabuild) {
            pearls.shrink(pearlCount);
        }
        player.getCooldowns().addCooldown(this, TELEPORT_COOLDOWN_TICKS);
        player.awardStat(Stats.ITEM_USED.get(this));
    }

    private boolean canDash() {
        return dashCooldownTicks > 0
                && dashStrength > 0.0
                && WalkingCaneConfig.DASH_STRENGTH > 0.0;
    }

    private static void damageCane(ItemStack stack, ServerPlayer player, InteractionHand hand) {
        if (!player.getAbilities().instabuild) {
            EquipmentSlot slot = hand == InteractionHand.MAIN_HAND
                    ? EquipmentSlot.MAINHAND
                    : EquipmentSlot.OFFHAND;
            stack.hurtAndBreak(1, player, slot);
        }
    }

    private static String formatSeconds(int ticks) {
        double seconds = ticks / 20.0;
        if (seconds == Math.rint(seconds)) {
            return Integer.toString((int) seconds);
        }
        return String.format(Locale.ROOT, "%.1f", seconds);
    }

    private static String formatNumber(double value) {
        if (value == Math.rint(value)) {
            return Long.toString((long) value);
        }
        return String.format(Locale.ROOT, "%.2f", value)
                .replaceAll("0+$", "")
                .replaceAll("\\.$", "");
    }
}
