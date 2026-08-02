package net.xuwu.walking_cane.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeMod;
import net.xuwu.walking_cane.client.ClientDashSender;
import net.xuwu.walking_cane.config.WalkingCaneConfig;
import net.xuwu.walking_cane.enchantment.WalkingCaneEnchantments;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class WalkingCaneItem extends Item {
    private static final UUID MOVEMENT_SPEED_MODIFIER_ID =
            UUID.fromString("f29f1dd3-a6d3-4ed4-bfe1-6d81d8d8631e");
    private static final UUID SWIM_SPEED_MODIFIER_ID =
            UUID.fromString("f2f31c60-b2b8-45b4-8f56-c4051bc7a6e2");
    private static final UUID STEP_HEIGHT_MODIFIER_ID =
            UUID.fromString("b5d81e27-3579-42bd-a02a-4759df07b99f");
    private static final int TELEPORT_COOLDOWN_TICKS = 200;
    private static final String DASH_STORAGE_TAG = "walking_cane.dash_storage";
    private static final String COOLDOWN_TYPE_TAG = "walking_cane.cooldown_type";
    private static final int COOLDOWN_TYPE_DASH = 1;
    private static final int COOLDOWN_TYPE_TELEPORT = 2;

    private final Multimap<Attribute, AttributeModifier> heldModifiers;
    private final double speedBonus;
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
        this.speedBonus = speedBonus;
        this.dashStrength = dashStrength;
        this.dashCooldownTicks = dashCooldownTicks;
        this.canTeleport = canTeleport;
        this.heldModifiers = createAttributes(speedBonus);
    }

    private static Multimap<Attribute, AttributeModifier> createAttributes(double speedBonus) {
        ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
        builder.put(Attributes.MOVEMENT_SPEED, new AttributeModifier(
                MOVEMENT_SPEED_MODIFIER_ID,
                "Walking cane movement speed",
                speedBonus,
                AttributeModifier.Operation.MULTIPLY_BASE
        ));
        builder.put(ForgeMod.SWIM_SPEED.get(), new AttributeModifier(
                SWIM_SPEED_MODIFIER_ID,
                "Walking cane swim speed",
                speedBonus,
                AttributeModifier.Operation.MULTIPLY_BASE
        ));
        builder.put(ForgeMod.STEP_HEIGHT_ADDITION.get(), new AttributeModifier(
                STEP_HEIGHT_MODIFIER_ID,
                "Walking cane step height",
                1.0,
                AttributeModifier.Operation.ADDITION
        ));
        return builder.build();
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        return switch (WalkingCaneConfig.HAND_MODE) {
            case MAIN_HAND -> slot == EquipmentSlot.MAINHAND
                    ? heldModifiers
                    : super.getDefaultAttributeModifiers(slot);
            case OFF_HAND -> slot == EquipmentSlot.OFFHAND
                    ? heldModifiers
                    : super.getDefaultAttributeModifiers(slot);
            case BOTH -> slot == EquipmentSlot.MAINHAND
                    ? heldModifiers
                    : super.getDefaultAttributeModifiers(slot);
        };
    }

    public static void refreshBothHandAttributes(Player player) {
        if (WalkingCaneConfig.HAND_MODE != WalkingCaneConfig.HandMode.BOTH) {
            return;
        }

        WalkingCaneItem activeCane = getStrongerCane(
                player.getMainHandItem(),
                player.getOffhandItem()
        );
        if (activeCane == null) {
            removeModifier(player.getAttribute(Attributes.MOVEMENT_SPEED), MOVEMENT_SPEED_MODIFIER_ID);
            removeModifier(player.getAttribute(ForgeMod.SWIM_SPEED.get()), SWIM_SPEED_MODIFIER_ID);
            removeModifier(player.getAttribute(ForgeMod.STEP_HEIGHT_ADDITION.get()), STEP_HEIGHT_MODIFIER_ID);
            return;
        }

        updateModifier(
                player.getAttribute(Attributes.MOVEMENT_SPEED),
                new AttributeModifier(
                        MOVEMENT_SPEED_MODIFIER_ID,
                        "Walking cane movement speed",
                        activeCane.speedBonus,
                        AttributeModifier.Operation.MULTIPLY_BASE
                )
        );
        updateModifier(
                player.getAttribute(ForgeMod.SWIM_SPEED.get()),
                new AttributeModifier(
                        SWIM_SPEED_MODIFIER_ID,
                        "Walking cane swim speed",
                        activeCane.speedBonus,
                        AttributeModifier.Operation.MULTIPLY_BASE
                )
        );
        updateModifier(
                player.getAttribute(ForgeMod.STEP_HEIGHT_ADDITION.get()),
                new AttributeModifier(
                        STEP_HEIGHT_MODIFIER_ID,
                        "Walking cane step height",
                        1.0,
                        AttributeModifier.Operation.ADDITION
                )
        );
    }

    private static WalkingCaneItem getStrongerCane(ItemStack first, ItemStack second) {
        WalkingCaneItem firstCane = first.getItem() instanceof WalkingCaneItem cane ? cane : null;
        WalkingCaneItem secondCane = second.getItem() instanceof WalkingCaneItem cane ? cane : null;
        if (firstCane == null) {
            return secondCane;
        }
        if (secondCane == null) {
            return firstCane;
        }
        return firstCane.speedBonus >= secondCane.speedBonus ? firstCane : secondCane;
    }

    private static void updateModifier(
            AttributeInstance attribute,
            AttributeModifier modifier
    ) {
        if (attribute == null) {
            return;
        }

        AttributeModifier current = attribute.getModifier(modifier.getId());
        if (current == null
                || Double.compare(current.getAmount(), modifier.getAmount()) != 0
                || current.getOperation() != modifier.getOperation()) {
            attribute.removeModifier(modifier.getId());
            attribute.addTransientModifier(modifier);
        }
    }

    private static void removeModifier(AttributeInstance attribute, UUID modifierId) {
        if (attribute != null) {
            attribute.removeModifier(modifierId);
        }
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
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        if (level.isClientSide || !(entity instanceof ServerPlayer player) || !canDash()) {
            return;
        }

        int storageLevel = WalkingCaneEnchantments.level(
                stack,
                WalkingCaneEnchantments.DASH_STORAGE
        );
        if (storageLevel <= 0) {
            return;
        }

        if (!hasStoredDashCharges(stack)) {
            setStoredDashCharges(stack, storageLevel);
            return;
        }

        int storedCharges = Math.min(getStoredDashCharges(stack), storageLevel);
        if (storedCharges != getStoredDashCharges(stack)) {
            setStoredDashCharges(stack, storedCharges);
        }
        if (storedCharges < storageLevel && !player.getCooldowns().isOnCooldown(this)) {
            int replenished = storedCharges + 1;
            setStoredDashCharges(stack, replenished);
            if (replenished < storageLevel) {
                int cooldownType = getCooldownType(stack);
                if (cooldownType != COOLDOWN_TYPE_TELEPORT) {
                    cooldownType = COOLDOWN_TYPE_DASH;
                    setCooldownType(stack, cooldownType);
                }
                int cooldownTicks = cooldownType == COOLDOWN_TYPE_TELEPORT
                        ? TELEPORT_COOLDOWN_TICKS
                        : dashCooldownTicks;
                player.getCooldowns().addCooldown(this, cooldownTicks);
            } else {
                setCooldownType(stack, 0);
            }
        }
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            @Nullable Level level,
            List<Component> tooltip,
            TooltipFlag flag
    ) {
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

    public static void tryDash(
            ServerPlayer player,
            InteractionHand hand,
            float rawStrafe,
            float rawForward
    ) {
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

        int storageLevel = WalkingCaneEnchantments.level(
                stack,
                WalkingCaneEnchantments.DASH_STORAGE
        );
        if (storageLevel > 0 && !hasStoredDashCharges(stack)) {
            setStoredDashCharges(stack, storageLevel);
        }

        boolean onCooldown = player.getCooldowns().isOnCooldown(cane);
        int storedCharges = getStoredDashCharges(stack);
        if (onCooldown && storedCharges <= 0) {
            return;
        }

        if (storedCharges > 0) {
            setStoredDashCharges(stack, storedCharges - 1);
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

        if (!onCooldown) {
            setCooldownType(stack, COOLDOWN_TYPE_DASH);
            player.getCooldowns().addCooldown(cane, cane.dashCooldownTicks);
        }
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

        ItemStack caneStack = player.getItemInHand(caneHand);
        InteractionHand consumableHand = caneHand == InteractionHand.MAIN_HAND
                ? InteractionHand.OFF_HAND
                : InteractionHand.MAIN_HAND;
        ItemStack consumables = player.getItemInHand(consumableHand);
        int consumableCount = WalkingCaneConfig.isTeleportConsumable(consumables)
                ? consumables.getCount()
                : 0;
        int savedConsumables = WalkingCaneEnchantments.level(
                caneStack,
                WalkingCaneEnchantments.ENDER_PEARL_SAVER
        );
        int consumedCount = Math.max(0, consumableCount - savedConsumables);
        double maxDistance = WalkingCaneConfig.TELEPORT_BASE_DISTANCE
                + consumableCount * WalkingCaneConfig.TELEPORT_DISTANCE_PER_PEARL;
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

        player.teleportTo(
                level,
                destination.x,
                destination.y,
                destination.z,
                player.getYRot(),
                player.getXRot()
        );
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
            consumables.shrink(consumedCount);
        }
        setCooldownType(caneStack, COOLDOWN_TYPE_TELEPORT);
        player.getCooldowns().addCooldown(this, TELEPORT_COOLDOWN_TICKS);
        player.awardStat(Stats.ITEM_USED.get(this));
    }

    private boolean canDash() {
        return dashCooldownTicks > 0
                && dashStrength > 0.0
                && WalkingCaneConfig.DASH_STRENGTH > 0.0;
    }

    public boolean supportsDashStorage() {
        return canDash();
    }

    public boolean supportsEnderPearlSaver() {
        return canTeleport;
    }

    public static int getStoredDashCharges(ItemStack stack) {
        return stack.hasTag() ? stack.getTag().getInt(DASH_STORAGE_TAG) : 0;
    }

    private static boolean hasStoredDashCharges(ItemStack stack) {
        return stack.hasTag() && stack.getTag().contains(DASH_STORAGE_TAG);
    }

    private static int getCooldownType(ItemStack stack) {
        return stack.hasTag() ? stack.getTag().getInt(COOLDOWN_TYPE_TAG) : 0;
    }

    private static void setCooldownType(ItemStack stack, int type) {
        if (type <= 0) {
            if (stack.hasTag()) {
                stack.getTag().remove(COOLDOWN_TYPE_TAG);
            }
        } else {
            stack.getOrCreateTag().putInt(COOLDOWN_TYPE_TAG, type);
        }
    }

    private static void setStoredDashCharges(ItemStack stack, int value) {
        stack.getOrCreateTag().putInt(DASH_STORAGE_TAG, Math.max(0, value));
    }

    private static void damageCane(ItemStack stack, ServerPlayer player, InteractionHand hand) {
        if (!player.getAbilities().instabuild) {
            stack.hurtAndBreak(1, player, brokenPlayer -> brokenPlayer.broadcastBreakEvent(hand));
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
