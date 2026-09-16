package fr.tropimon.teamsaver.mixin.client;

import com.cobblemon.mod.common.api.moves.Moves;
import fr.tropimon.teamsaver.client.ClientDataRevision;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Moves.class, remap = false)
abstract class MovesRevisionMixin {
    @Inject(method = "receiveSyncPacket$common", at = @At("RETURN"))
    private void teamsaver$catalogueChanged(CallbackInfo ci) {
        ClientDataRevision.catalogueChanged();
    }
}
