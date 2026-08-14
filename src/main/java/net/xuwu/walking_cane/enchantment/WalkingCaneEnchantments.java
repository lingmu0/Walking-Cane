package net.xuwu.walking_cane.enchantment;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.xuwu.walking_cane.WalkingCane;
import net.xuwu.walking_cane.item.WalkingCaneItem;

/** Registered enchantments for the Forge 1.20.1 build. */
public final class WalkingCaneEnchantments {
    private static final EnchantmentCategory COOLDOWN_STORAGE_CATEGORY =
            EnchantmentCategory.create("cooldown_storage", item -> true);
    private static final EnchantmentCategory DISPLACEMENT_STORAGE_CATEGORY =
            EnchantmentCategory.create("displacement_storage", item -> item instanceof WalkingCaneItem);
    private static final EnchantmentCategory DISPLACEMENT_COOLDOWN_REDUCTION_CATEGORY =
            EnchantmentCategory.create("displacement_cooldown_reduction", item -> item instanceof WalkingCaneItem);
    private static final EnchantmentCategory ENDER_PEARL_SAVER_CATEGORY =
            EnchantmentCategory.create("ender_pearl_saver", item -> item instanceof WalkingCaneItem);

    public static final DeferredRegister<Enchantment> ENCHANTMENTS =
            DeferredRegister.create(ForgeRegistries.ENCHANTMENTS, WalkingCane.MOD_ID);

    public static final RegistryObject<Enchantment> COOLDOWN_STORAGE = ENCHANTMENTS.register(
            "cooldown_storage",
            () -> new CaneEnchantment(
                    Enchantment.Rarity.UNCOMMON,
                    COOLDOWN_STORAGE_CATEGORY,
                    stack -> true,
                    true,
                    true,
                    false
            )
    );
    public static final RegistryObject<Enchantment> DISPLACEMENT_STORAGE = ENCHANTMENTS.register(
            "displacement_storage",
            () -> new CaneEnchantment(
                    Enchantment.Rarity.UNCOMMON,
                    DISPLACEMENT_STORAGE_CATEGORY,
                    stack -> stack.getItem() instanceof WalkingCaneItem cane
                            && cane.supportsDisplacementStorage(),
                    true,
                    false,
                    true
            )
    );
    public static final RegistryObject<Enchantment> DISPLACEMENT_COOLDOWN_REDUCTION = ENCHANTMENTS.register(
            "displacement_cooldown_reduction",
            () -> new CaneEnchantment(
                    Enchantment.Rarity.RARE,
                    DISPLACEMENT_COOLDOWN_REDUCTION_CATEGORY,
                    stack -> stack.getItem() instanceof WalkingCaneItem,
                    false,
                    false,
                    false
            )
    );
    public static final RegistryObject<Enchantment> ENDER_PEARL_SAVER = ENCHANTMENTS.register(
            "ender_pearl_saver",
            () -> new CaneEnchantment(
                    Enchantment.Rarity.RARE,
                    ENDER_PEARL_SAVER_CATEGORY,
                    stack -> stack.getItem() instanceof WalkingCaneItem cane && cane.supportsEnderPearlSaver(),
                    false,
                    false,
                    true
            )
    );

    public static int level(ItemStack stack, RegistryObject<Enchantment> enchantment) {
        return stack.isEmpty() ? 0 : EnchantmentHelper.getItemEnchantmentLevel(enchantment.get(), stack);
    }

    private static final class CaneEnchantment extends Enchantment {
        private final java.util.function.Predicate<ItemStack> supported;
        private final boolean storageEnchantment;
        private final boolean treasureOnly;
        private final boolean availableAtEnchantingTable;

        private CaneEnchantment(
                Rarity rarity,
                EnchantmentCategory category,
                java.util.function.Predicate<ItemStack> supported,
                boolean storageEnchantment,
                boolean treasureOnly,
                boolean availableAtEnchantingTable
        ) {
            super(rarity, category, new EquipmentSlot[]{
                    EquipmentSlot.MAINHAND,
                    EquipmentSlot.OFFHAND
            });
            this.supported = supported;
            this.storageEnchantment = storageEnchantment;
            this.treasureOnly = treasureOnly;
            this.availableAtEnchantingTable = availableAtEnchantingTable;
        }

        @Override
        public int getMaxLevel() {
            return 3;
        }

        @Override
        public int getMinCost(int level) {
            return 1 + (level - 1) * 10;
        }

        @Override
        public int getMaxCost(int level) {
            return getMinCost(level) + 10;
        }

        @Override
        public boolean canEnchant(ItemStack stack) {
            return supported.test(stack);
        }

        @Override
        public boolean canApplyAtEnchantingTable(ItemStack stack) {
            return availableAtEnchantingTable && !treasureOnly && supported.test(stack);
        }

        @Override
        protected boolean checkCompatibility(Enchantment other) {
            if (storageEnchantment
                    && other instanceof CaneEnchantment otherEnchantment
                    && otherEnchantment.storageEnchantment) {
                return false;
            }
            return super.checkCompatibility(other);
        }

        @Override
        public boolean isTreasureOnly() {
            return treasureOnly;
        }

        @Override
        public boolean isTradeable() {
            return !treasureOnly;
        }

        @Override
        public boolean isDiscoverable() {
            return !treasureOnly;
        }
    }

    private WalkingCaneEnchantments() {
    }
}
