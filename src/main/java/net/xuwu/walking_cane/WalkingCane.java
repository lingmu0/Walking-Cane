package net.xuwu.walking_cane;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.xuwu.walking_cane.event.CaneAttributeEvents;
import net.xuwu.walking_cane.event.CaneInteractionEvents;
import net.xuwu.walking_cane.item.WalkingCaneItem;
import net.xuwu.walking_cane.network.DashPayload;
import net.xuwu.walking_cane.network.CooldownUsePayload;

@Mod(WalkingCane.MOD_ID)
public final class WalkingCane {
    public static final String MOD_ID = "walking_cane";

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MOD_ID);

    public static final DeferredItem<WalkingCaneItem> WOODEN_CANE = registerCane(
            "wooden_cane", 0.15, 59, 0, 0, false
    );
    public static final DeferredItem<WalkingCaneItem> IRON_CANE = registerCane(
            "iron_cane", 0.30, 250, 0, 0, false
    );
    public static final DeferredItem<WalkingCaneItem> DIAMOND_CANE = registerCane(
            "diamond_cane", 0.50, 1561, 2.5, 40, false
    );
    public static final DeferredItem<WalkingCaneItem> ENDER_CANE = registerCane(
            "ender_cane", 0.50, 1561, 2.5, 40, true
    );
    public static final DeferredItem<WalkingCaneItem> NETHERITE_CANE = registerCane(
            "netherite_cane", 0.80, 2031, 2.5, 30, false
    );

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> WALKING_CANES_TAB =
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

    public WalkingCane(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
        CREATIVE_TABS.register(modEventBus);
        modEventBus.addListener(WalkingCane::registerPayloads);
        NeoForge.EVENT_BUS.addListener(CaneAttributeEvents::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(CaneInteractionEvents::onRightClickItem);
    }

    private static DeferredItem<WalkingCaneItem> registerCane(
            String name,
            double speedBonus,
            int durability,
            double dashStrength,
            int dashCooldownTicks,
            boolean canTeleport
    ) {
        return ITEMS.register(name, () -> new WalkingCaneItem(
                new Item.Properties()
                        .durability(durability)
                        .attributes(WalkingCaneItem.createAttributes(speedBonus)),
                speedBonus,
                dashStrength,
                dashCooldownTicks,
                canTeleport
        ));
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("3")
                .playToServer(DashPayload.TYPE, DashPayload.STREAM_CODEC, DashPayload::handle)
                .playToServer(
                        CooldownUsePayload.TYPE,
                        CooldownUsePayload.STREAM_CODEC,
                        CooldownUsePayload::handle
                );
    }
}
