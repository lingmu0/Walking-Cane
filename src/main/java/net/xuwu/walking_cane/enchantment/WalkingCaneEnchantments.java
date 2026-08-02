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
    private static final EnchantmentCategory ENDER_PEARL_SAVER_CATEGORY =
            EnchantmentCategory.create("ender_pearl_saver", item -> item instanceof WalkingCaneItem);

    public static final DeferredRegister<Enchantment> ENCHANTMENTS =
            DeferredRegister.create(ForgeRegistries.ENCHANTMENTS, WalkingCane.MOD_ID);

    public static final RegistryObject<Enchantment> COOLDOWN_STORAGE = ENCHANTMENTS.register(
            "cooldown_storage",
            () -> new CaneEnchantment(
                    Enchantment.Rarity.UNCOMMON,
                    COOLDOWN_STORAGE_CATEGORY,
                    stack -> true
            )
    );
    public static final RegistryObject<Enchantment> ENDER_PEARL_SAVER = ENCHANTMENTS.register(
            "ender_pearl_saver",
            () -> new CaneEnchantment(
                    Enchantment.Rarity.RARE,
                    ENDER_PEARL_SAVER_CATEGORY,
                    stack -> stack.getItem() instanceof WalkingCaneItem cane && cane.supportsEnderPearlSaver()
            )
    );

    public static int level(ItemStack stack, RegistryObject<Enchantment> enchantment) {
        return stack.isEmpty() ? 0 : EnchantmentHelper.getItemEnchantmentLevel(enchantment.get(), stack);
    }

    private static final class CaneEnchantment extends Enchantment {
        private final java.util.function.Predicate<ItemStack> supported;

        private CaneEnchantment(
                Rarity rarity,
                EnchantmentCategory category,
                java.util.function.Predicate<ItemStack> supported
        ) {
            super(rarity, category, new EquipmentSlot[]{
                    EquipmentSlot.MAINHAND,
                    EquipmentSlot.OFFHAND
            });
            this.supported = supported;
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
            return supported.test(stack);
        }
    }

    private WalkingCaneEnchantments() {
    }
}
