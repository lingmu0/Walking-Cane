package net.xuwu.walking_cane;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.xuwu.walking_cane.event.CaneInteractionEvents;
import net.xuwu.walking_cane.item.WalkingCaneItem;
import net.xuwu.walking_cane.network.WalkingCaneNetwork;

@Mod(WalkingCane.MOD_ID)
public final class WalkingCane {
    public static final String MOD_ID = "walking_cane";

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MOD_ID);

    public static final RegistryObject<WalkingCaneItem> WOODEN_CANE = registerCane(
            "wooden_cane", 0.15, 59, 0.0, 0, false
    );
    public static final RegistryObject<WalkingCaneItem> IRON_CANE = registerCane(
            "iron_cane", 0.30, 250, 0.0, 0, false
    );
    public static final RegistryObject<WalkingCaneItem> DIAMOND_CANE = registerCane(
            "diamond_cane", 0.50, 1561, 2.5, 40, false
    );
    public static final RegistryObject<WalkingCaneItem> ENDER_CANE = registerCane(
            "ender_cane", 0.50, 1561, 2.5, 40, true
    );
    public static final RegistryObject<WalkingCaneItem> NETHERITE_CANE = registerCane(
            "netherite_cane", 0.80, 2031, 2.5, 30, false
    );

    public static final RegistryObject<CreativeModeTab> WALKING_CANES_TAB =
            CREATIVE_TABS.register("walking_canes", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.walking_cane.walking_canes"))
                    .withTabsBefore(CreativeModeTabs.COMBAT)
                    .icon(() -> DIAMOND_CANE.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(WOODEN_CANE.get());
                        output.accept(IRON_CANE.get());
                        output.accept(DIAMOND_CANE.get());
                        output.accept(ENDER_CANE.get());
                        output.accept(NETHERITE_CANE.get());
                    })
                    .build());

    public WalkingCane() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        ITEMS.register(modEventBus);
        CREATIVE_TABS.register(modEventBus);

        WalkingCaneNetwork.register();
        MinecraftForge.EVENT_BUS.addListener(CaneInteractionEvents::onRightClickItem);
    }

    private static RegistryObject<WalkingCaneItem> registerCane(
            String name,
            double speedBonus,
            int durability,
            double dashStrength,
            int dashCooldownTicks,
            boolean canTeleport
    ) {
        return ITEMS.register(name, () -> new WalkingCaneItem(
                new Item.Properties().durability(durability),
                speedBonus,
                dashStrength,
                dashCooldownTicks,
                canTeleport
        ));
    }
}
