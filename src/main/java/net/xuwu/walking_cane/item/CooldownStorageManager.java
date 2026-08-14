package net.xuwu.walking_cane.item;

import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.ForgeHooks;
import net.xuwu.walking_cane.WalkingCane;
import net.xuwu.walking_cane.enchantment.WalkingCaneEnchantments;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Keeps cooldown state per player and Item, like ItemCooldowns. Charge values remain on
 * individual stacks so different enchantment levels retain different storage limits.
 */
public final class CooldownStorageManager {
    static final int COOLDOWN_TYPE_NONE = 0;
    static final int COOLDOWN_TYPE_DASH = 1;
    static final int COOLDOWN_TYPE_TELEPORT = 2;

    private static final String STORAGE_TAG = WalkingCane.MOD_ID + ".cooldown_storage";
    private static final String ACTIVE_TAG = WalkingCane.MOD_ID + ".cooldown_storage_active";
    private static final String COOLDOWN_TYPE_TAG = WalkingCane.MOD_ID + ".cooldown_type";
    private static final String COOLDOWN_QUEUE_TAG = WalkingCane.MOD_ID + ".cooldown_queue";

    private static final Map<ServerPlayer, Map<Item, StorageState>> STATES = new WeakHashMap<>();

    private CooldownStorageManager() {
    }

    public static int level(ItemStack stack) {
        int cooldownStorageLevel = WalkingCaneEnchantments.level(
                stack,
                WalkingCaneEnchantments.COOLDOWN_STORAGE
        );
        if (stack.getItem() instanceof WalkingCaneItem cane && cane.supportsDisplacementStorage()) {
            return Math.max(
                    cooldownStorageLevel,
                    WalkingCaneEnchantments.level(stack, WalkingCaneEnchantments.DISPLACEMENT_STORAGE)
            );
        }
        return cooldownStorageLevel;
    }

    static int displacementCooldownReductionLevel(ServerPlayer player, Item item) {
        int level = 0;
        for (ItemStack stack : matchingStacks(player, item, null)) {
            level = Math.max(
                    level,
                    WalkingCaneEnchantments.level(
                            stack,
                            WalkingCaneEnchantments.DISPLACEMENT_COOLDOWN_REDUCTION
                    )
            );
        }
        return level;
    }

    /** Reads the stack-local charge value on either logical side. */
    public static int getStoredCharges(ItemStack stack) {
        return stack.hasTag() ? stack.getTag().getInt(STORAGE_TAG) : 0;
    }

    public static boolean isCapturable(ServerPlayer player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.isEmpty() || stack.getItem() instanceof WalkingCaneItem) {
            return false;
        }

        StorageState state = state(player, stack, level(stack));
        return state != null
                && getStoredCharges(stack) > 0
                && player.getCooldowns().isOnCooldown(stack.getItem());
    }

    /** Returns shared cooldown state and initializes/clamps every matching stack independently. */
    static StorageState state(ServerPlayer player, ItemStack stack, int storageLevel) {
        if (stack.isEmpty() || storageLevel <= 0) {
            return null;
        }

        Item item = stack.getItem();
        Map<Item, StorageState> playerStates = STATES.computeIfAbsent(
                player,
                ignored -> new HashMap<>()
        );
        StorageState state = playerStates.computeIfAbsent(item, StorageState::new);
        initializeStacks(player, item, stack);
        if (!state.initialized) {
            state.importCooldownData(player, stack);
            state.initialized = true;
        }
        sync(player, state);
        return state;
    }

    private static void initializeStacks(ServerPlayer player, Item item, ItemStack anchor) {
        for (ItemStack stack : matchingStacks(player, item, anchor)) {
            int stackLevel = level(stack);
            if (stackLevel <= 0) {
                continue;
            }

            int charges = getStoredCharges(stack);
            if (!hasStoredCharges(stack)) {
                charges = stackLevel;
            }
            charges = Math.min(Math.max(charges, 0), stackLevel);
            stack.getOrCreateTag().putInt(STORAGE_TAG, charges);
        }
    }

    private static boolean hasStoredCharges(ItemStack stack) {
        return stack.hasTag() && stack.getTag().contains(STORAGE_TAG);
    }

    /** Mirrors only shared cooldown metadata; charge counts stay stack-local. */
    static void sync(ServerPlayer player, StorageState state) {
        for (ItemStack stack : matchingStacks(player, state.item, null)) {
            if (level(stack) <= 0) {
                continue;
            }
            if (state.activeCooldown) {
                stack.getOrCreateTag().putBoolean(ACTIVE_TAG, true);
            } else if (stack.hasTag()) {
                stack.getTag().remove(ACTIVE_TAG);
            }
            if (state.cooldownType == COOLDOWN_TYPE_NONE) {
                if (stack.hasTag()) {
                    stack.getTag().remove(COOLDOWN_TYPE_TAG);
                }
            } else {
                stack.getOrCreateTag().putInt(COOLDOWN_TYPE_TAG, state.cooldownType);
            }
            if (state.cooldownQueue.isEmpty()) {
                if (stack.hasTag()) {
                    stack.getTag().remove(COOLDOWN_QUEUE_TAG);
                }
            } else {
                ListTag queue = new ListTag();
                state.cooldownQueue.forEach(type -> queue.add(IntTag.valueOf(type)));
                stack.getOrCreateTag().put(COOLDOWN_QUEUE_TAG, queue);
            }
        }
    }

    /** Decrements every matching enchanted stack that has a charge available. */
    static void consumeCharges(ServerPlayer player, Item item) {
        for (ItemStack stack : matchingStacks(player, item, null)) {
            int stackLevel = level(stack);
            if (stackLevel <= 0) {
                continue;
            }
            int charges = getStoredCharges(stack);
            if (!hasStoredCharges(stack)) {
                charges = stackLevel;
            }
            if (charges > 0) {
                stack.getOrCreateTag().putInt(STORAGE_TAG, charges - 1);
            }
        }
    }

    /** Restores one charge on every matching stack, respecting each stack's own cap. */
    static void replenishCharges(ServerPlayer player, Item item) {
        for (ItemStack stack : matchingStacks(player, item, null)) {
            int stackLevel = level(stack);
            if (stackLevel <= 0) {
                continue;
            }
            int charges = hasStoredCharges(stack) ? getStoredCharges(stack) : stackLevel;
            stack.getOrCreateTag().putInt(STORAGE_TAG, Math.min(charges + 1, stackLevel));
        }
    }

    static boolean hasReplenishableCharges(ServerPlayer player, Item item) {
        for (ItemStack stack : matchingStacks(player, item, null)) {
            int stackLevel = level(stack);
            if (stackLevel > 0 && getStoredCharges(stack) < stackLevel) {
                return true;
            }
        }
        return false;
    }

    public static void tick(ServerPlayer player) {
        Map<Item, ItemStack> representatives = new HashMap<>();
        Map<Item, Integer> levels = new HashMap<>();
        for (ItemStack stack : allInventoryStacks(player)) {
            int storageLevel = level(stack);
            if (storageLevel <= 0) {
                continue;
            }
            representatives.putIfAbsent(stack.getItem(), stack);
            levels.merge(stack.getItem(), storageLevel, Math::max);
        }

        Map<Item, StorageState> playerStates = STATES.get(player);
        if (playerStates == null) {
            playerStates = new HashMap<>();
        }
        for (Map.Entry<Item, StorageState> entry : new ArrayList<>(playerStates.entrySet())) {
            Item item = entry.getKey();
            StorageState state = entry.getValue();
            ItemStack representative = representatives.get(item);
            if (!levels.containsKey(item)) {
                if (player.getCooldowns().isOnCooldown(item)) {
                    if (item instanceof WalkingCaneItem cane) {
                        cane.tickSharedCooldown(player, state);
                    } else {
                        tickGeneric(player, state);
                    }
                } else {
                    playerStates.remove(item);
                }
                continue;
            }

            state(player, representative, levels.get(item));
            if (item instanceof WalkingCaneItem cane) {
                cane.tickSharedCooldown(player, state);
            } else {
                tickGeneric(player, state);
            }
        }

        for (Map.Entry<Item, ItemStack> entry : representatives.entrySet()) {
            if (playerStates.containsKey(entry.getKey())) {
                continue;
            }
            StorageState state = state(player, entry.getValue(), levels.get(entry.getKey()));
            if (entry.getKey() instanceof WalkingCaneItem cane) {
                cane.tickSharedCooldown(player, state);
            } else {
                tickGeneric(player, state);
            }
        }
    }

    private static void tickGeneric(ServerPlayer player, StorageState state) {
        if (player.getCooldowns().isOnCooldown(state.item)) {
            state.activeCooldown = true;
            sync(player, state);
            return;
        }

        if (state.activeCooldown) {
            replenishCharges(player, state.item);
            state.activeCooldown = false;
            sync(player, state);
        }
    }

    public static boolean tryUseDuringCooldown(ServerPlayer player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.isEmpty() || stack.getItem() instanceof WalkingCaneItem) {
            return false;
        }

        int storageLevel = level(stack);
        StorageState state = state(player, stack, storageLevel);
        if (state == null
                || getStoredCharges(stack) <= 0
                || !player.getCooldowns().isOnCooldown(stack.getItem())) {
            return false;
        }

        InteractionResult eventResult = ForgeHooks.onItemRightClick(player, hand);
        if (eventResult != null) {
            return false;
        }

        Level level = player.level();
        InteractionResultHolder<ItemStack> result = stack.use(level, player, hand);
        if (!result.getResult().consumesAction()) {
            return false;
        }

        if (result.getObject() != stack) {
            player.setItemInHand(hand, result.getObject());
        }
        consumeCharges(player, stack.getItem());
        state.activeCooldown = true;
        sync(player, state);
        return true;
    }

    static List<ItemStack> matchingStacks(ServerPlayer player, Item item, ItemStack anchor) {
        List<ItemStack> result = new ArrayList<>();
        boolean anchorFound = false;
        for (ItemStack stack : allInventoryStacks(player)) {
            if (stack.getItem() == item) {
                result.add(stack);
                anchorFound |= stack == anchor;
            }
        }
        if (anchor != null && anchor.getItem() == item && !anchorFound) {
            result.add(anchor);
        }
        return result;
    }

    private static List<ItemStack> allInventoryStacks(ServerPlayer player) {
        List<ItemStack> result = new ArrayList<>();
        result.addAll(player.getInventory().items);
        result.addAll(player.getInventory().armor);
        result.addAll(player.getInventory().offhand);
        return result;
    }

    public static final class StorageState {
        private final Item item;
        private final ArrayDeque<Integer> cooldownQueue = new ArrayDeque<>();
        private boolean initialized;
        private boolean activeCooldown;
        private int cooldownType;

        private StorageState(Item item) {
            this.item = item;
        }

        public boolean activeCooldown() {
            return activeCooldown;
        }

        public void setActiveCooldown(boolean activeCooldown) {
            this.activeCooldown = activeCooldown;
        }

        public int cooldownType() {
            return cooldownType;
        }

        public void setCooldownType(int cooldownType) {
            this.cooldownType = cooldownType;
        }

        public void enqueueCooldown(int type) {
            if (type != COOLDOWN_TYPE_NONE) {
                cooldownQueue.add(type);
            }
        }

        public int pollCooldown() {
            return cooldownQueue.isEmpty() ? COOLDOWN_TYPE_NONE : cooldownQueue.removeFirst();
        }

        private void importCooldownData(ServerPlayer player, ItemStack anchor) {
            for (ItemStack stack : matchingStacks(player, item, anchor)) {
                if (level(stack) <= 0 || !stack.hasTag()) {
                    continue;
                }
                var tag = stack.getTag();
                activeCooldown |= tag.getBoolean(ACTIVE_TAG);
                if (cooldownType == COOLDOWN_TYPE_NONE && tag.contains(COOLDOWN_TYPE_TAG)) {
                    cooldownType = tag.getInt(COOLDOWN_TYPE_TAG);
                }
                if (cooldownQueue.isEmpty() && tag.contains(COOLDOWN_QUEUE_TAG, Tag.TAG_LIST)) {
                    ListTag queue = tag.getList(COOLDOWN_QUEUE_TAG, Tag.TAG_INT);
                    for (int index = 0; index < queue.size(); index++) {
                        cooldownQueue.add(queue.getInt(index));
                    }
                }
            }
        }
    }
}
