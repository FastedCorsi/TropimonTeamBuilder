package fr.tropimon.teamsaver.mixin.client;

import com.cobblemon.mod.common.client.net.pokemon.update.PokemonUpdatePacketHandler;
import fr.tropimon.teamsaver.client.ClientDataRevision;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = PokemonUpdatePacketHandler.class, remap = false)
abstract class PokemonRevisionMixin {
    @Inject(method = "handle", at = @At("RETURN"))
    private void teamsaver$pokemonChanged(CallbackInfo ci) {
        ClientDataRevision.pokemonChanged();
    }
}
