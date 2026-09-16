package fr.tropimon.teamsaver.mixin.client;

import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import fr.tropimon.teamsaver.client.ClientDataRevision;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = PokemonSpecies.class, remap = false)
abstract class SpeciesRevisionMixin {
    @Inject(method = "reload(Ljava/util/Map;)V", at = @At("RETURN"))
    private void teamsaver$catalogueChanged(CallbackInfo ci) {
        ClientDataRevision.catalogueChanged();
    }
}
