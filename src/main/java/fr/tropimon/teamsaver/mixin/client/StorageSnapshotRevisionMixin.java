package fr.tropimon.teamsaver.mixin.client;

import com.cobblemon.mod.common.client.net.storage.pc.SetPCBoxHandler;
import fr.tropimon.teamsaver.client.ClientDataRevision;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Bulk snapshots write the boxes directly, bypassing ClientPC.set. Observation only. */
@Mixin(value = SetPCBoxHandler.class, remap = false)
abstract class StorageSnapshotRevisionMixin {
    @Inject(method = "handle", at = @At("RETURN"))
    private void teamsaver$snapshotChanged(CallbackInfo ci) {
        ClientDataRevision.storageChanged();
    }
}
