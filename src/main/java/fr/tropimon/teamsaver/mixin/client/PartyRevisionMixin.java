package fr.tropimon.teamsaver.mixin.client;

import com.cobblemon.mod.common.client.storage.ClientParty;
import fr.tropimon.teamsaver.client.ClientDataRevision;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ClientParty.class, remap = false)
abstract class PartyRevisionMixin {
    @Inject(method = "set(Lcom/cobblemon/mod/common/api/storage/party/PartyPosition;Lcom/cobblemon/mod/common/pokemon/Pokemon;)V", at = @At("RETURN"))
    private void teamsaver$partyChanged(CallbackInfo ci) {
        ClientDataRevision.storageChanged();
    }
}
