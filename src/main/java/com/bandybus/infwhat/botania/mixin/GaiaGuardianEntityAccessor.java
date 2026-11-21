package com.bandybus.infwhat.botania.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import vazkii.botania.common.entity.GaiaGuardianEntity;

import java.util.List;
import java.util.UUID;

/**
 * Exposes the guardian's attacker list so the Gaia Beacon can credit nearby players.
 *
 * <p>Botania itself awards the kill trigger and the no-armor advancement to every entry in this
 * list, so adding nearby players before the forced death keeps all of that logic in Botania.
 */
@Mixin(GaiaGuardianEntity.class)
public interface GaiaGuardianEntityAccessor {

    @Accessor(value = "playersWhoAttacked", remap = false)
    List<UUID> infwhat$playersWhoAttacked();

    /**
     * Botania returns an empty loot table while this counter is positive (the minion spawn
     * phase, 900 ticks), so the beacon clears it before the finishing hit.
     */
    @Accessor(value = "mobSpawnTicks", remap = false)
    void infwhat$setMobSpawnTicks(int ticks);
}
