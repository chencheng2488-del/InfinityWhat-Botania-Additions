package com.bandybus.infwhat.botania.container;

import com.bandybus.infwhat.botania.InfinityWhatBotania;
import net.minecraft.inventory.container.ContainerType;
import net.minecraftforge.common.extensions.IForgeContainerType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

/** Container type registration. */
public final class ModContainers {

    public static final DeferredRegister<ContainerType<?>> CONTAINERS =
            DeferredRegister.create(ForgeRegistries.CONTAINERS, InfinityWhatBotania.MOD_ID);

    public static final RegistryObject<ContainerType<ManaDeviceContainer>> DEVICE =
            CONTAINERS.register("mana_device",
                    () -> IForgeContainerType.create(ManaDeviceContainer::new));

    public static final RegistryObject<ContainerType<AcceleratorContainer>> ACCELERATOR =
            CONTAINERS.register("accelerator",
                    () -> IForgeContainerType.create(AcceleratorContainer::new));

    private ModContainers() {
    }

    public static void register(IEventBus eventBus) {
        CONTAINERS.register(eventBus);
    }
}
