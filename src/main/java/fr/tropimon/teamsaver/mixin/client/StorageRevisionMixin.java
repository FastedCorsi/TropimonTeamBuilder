package fr.tropimon.teamsaver.mixin.client;

import com.cobblemon.mod.common.client.storage.ClientPC;
import fr.tropimon.teamsaver.client.ClientDataRevision;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ClientPC.class, remap = false)
abstract class StorageRevisionMixin {
    @Inject(method = "set(Lcom/cobblemon/mod/common/api/storage/pc/PCPosition;Lcom/cobblemon/mod/common/pokemon/Pokemon;)V", at = @At("RETURN"))
    private void teamsaver$storageChanged(CallbackInfo ci) {
        ClientDataRevision.storageChanged();
    }
}
