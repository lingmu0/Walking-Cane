package net.xuwu.walking_cane.enchantment;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.xuwu.walking_cane.WalkingCane;

/** Resource keys and lookup helpers for the data-driven 1.21.1 enchantments. */
public final class WalkingCaneEnchantments {
    public static final ResourceKey<Enchantment> COOLDOWN_STORAGE = key("cooldown_storage");
    public static final ResourceKey<Enchantment> DISPLACEMENT_STORAGE = key("displacement_storage");
    public static final ResourceKey<Enchantment> DISPLACEMENT_COOLDOWN_REDUCTION = key("displacement_cooldown_reduction");
    public static final ResourceKey<Enchantment> ENDER_PEARL_SAVER = key("ender_pearl_saver");

    public static int level(LivingEntity entity, ItemStack stack, ResourceKey<Enchantment> key) {
        if (stack.isEmpty()) {
            return 0;
        }
        Holder<Enchantment> enchantment = entity.registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(key);
        return EnchantmentHelper.getItemEnchantmentLevel(enchantment, stack);
    }

    private static ResourceKey<Enchantment> key(String id) {
        return ResourceKey.create(
                Registries.ENCHANTMENT,
                ResourceLocation.fromNamespaceAndPath(WalkingCane.MOD_ID, id)
        );
    }

    private WalkingCaneEnchantments() {
    }
}
