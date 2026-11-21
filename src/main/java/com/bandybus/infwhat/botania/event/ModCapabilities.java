package com.bandybus.infwhat.botania.event;

import com.bandybus.infwhat.botania.InfinityWhatBotania;
import com.bandybus.infwhat.botania.block.entity.GaiaBeaconBlockEntity;
import com.bandybus.infwhat.botania.block.entity.ManaBeaconBlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;
import vazkii.botania.api.BotaniaForgeCapabilities;
import vazkii.botania.api.mana.ManaReceiver;

/** Exposes our wireless devices as Botania mana receivers so wells can feed them directly. */
@Mod.EventBusSubscriber(modid = InfinityWhatBotania.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ModCapabilities {

    private static final ResourceLocation MANA_RECEIVER_ID =
            new ResourceLocation(InfinityWhatBotania.MOD_ID, "mana_receiver");

    private ModCapabilities() {
    }

    @SubscribeEvent
    public static void attach(AttachCapabilitiesEvent<BlockEntity> event) {
        if (event.getObject() instanceof ManaBeaconBlockEntity beacon) {
            event.addCapability(MANA_RECEIVER_ID, new Provider(beacon));
        } else if (event.getObject() instanceof GaiaBeaconBlockEntity gaiaBeacon) {
            event.addCapability(MANA_RECEIVER_ID, new Provider(gaiaBeacon));
        }
    }

    private record Provider(ManaReceiver receiver) implements ICapabilityProvider {

        @Override
        public <T> LazyOptional<T> getCapability(Capability<T> capability,
                                                 @Nullable Direction side) {
            if (capability == BotaniaForgeCapabilities.MANA_RECEIVER) {
                return LazyOptional.of(() -> receiver).cast();
            }
            return LazyOptional.empty();
        }
    }
}
