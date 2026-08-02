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
 * Stores cooldown charges per player and Item, matching ItemCooldowns' item-keyed behavior.
 * The custom data mirrored to matching stacks is only used to synchronize the HUD number.
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
        return WalkingCaneEnchantments.level(stack, WalkingCaneEnchantments.COOLDOWN_STORAGE);
    }

    /** Reads the synchronized display value from a stack on either logical side. */
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
                && state.charges() > 0
                && player.getCooldowns().isOnCooldown(stack.getItem());
    }

    /** Returns the shared state for an item and imports legacy per-stack data once. */
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
        state.maxCharges = Math.max(state.maxCharges, storageLevel);
        if (!state.initialized) {
            state.importFromStacks(player, stack);
            state.initialized = true;
        }
        state.maxCharges = Math.max(state.maxCharges, storageLevel);
        state.charges = Math.min(Math.max(state.charges, 0), state.maxCharges);
        sync(player, state);
        return state;
    }

    /** Mirrors one shared Item state to every matching stack for client HUD rendering. */
    static void sync(ServerPlayer player, StorageState state) {
        for (ItemStack stack : matchingStacks(player, state.item, null)) {
            if (state.charges < 0) {
                state.charges = 0;
            }
            stack.getOrCreateTag().putInt(STORAGE_TAG, state.charges);
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
            int storageLevel = levels.getOrDefault(item, state.maxCharges);
            if (storageLevel <= 0) {
                if (!player.getCooldowns().isOnCooldown(item)) {
                    playerStates.remove(item);
                }
                continue;
            }

            if (representative != null) {
                state(player, representative, storageLevel);
            }
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
            if (state.charges < state.maxCharges) {
                state.charges++;
            }
            state.activeCooldown = false;
            sync(player, state);
        }
    }

    public static boolean tryUseDuringCooldown(ServerPlayer player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.isEmpty() || stack.getItem() instanceof WalkingCaneItem) {
            return false;
        }

        StorageState state = state(player, stack, level(stack));
        if (state == null
                || state.charges <= 0
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
        state.charges--;
        state.activeCooldown = true;
        sync(player, state);
        return true;
    }

    static List<ItemStack> matchingStacks(ServerPlayer player, Item item, ItemStack anchor) {
        List<ItemStack> result = new ArrayList<>();
        for (ItemStack stack : allInventoryStacks(player)) {
            if (stack.getItem() == item && !result.contains(stack)) {
                result.add(stack);
            }
        }
        if (anchor != null && anchor.getItem() == item && !result.contains(anchor)) {
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
        private int maxCharges;
        private int charges;
        private boolean initialized;
        private boolean activeCooldown;
        private int cooldownType;

        private StorageState(Item item) {
            this.item = item;
        }

        public int maxCharges() {
            return maxCharges;
        }

        public int charges() {
            return charges;
        }

        public void setCharges(int charges) {
            this.charges = Math.max(0, Math.min(charges, maxCharges));
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

        private void importFromStacks(ServerPlayer player, ItemStack anchor) {
            int importedCharges = -1;
            for (ItemStack stack : matchingStacks(player, item, anchor)) {
                if (!stack.hasTag()) {
                    continue;
                }
                var tag = stack.getTag();
                if (tag.contains(STORAGE_TAG)) {
                    importedCharges = Math.max(importedCharges, tag.getInt(STORAGE_TAG));
                }
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
            charges = importedCharges < 0 ? maxCharges : Math.min(importedCharges, maxCharges);
        }
    }
}
