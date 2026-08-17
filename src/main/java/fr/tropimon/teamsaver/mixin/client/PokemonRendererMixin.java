package fr.tropimon.teamsaver.mixin.client;

import com.cobblemon.mod.common.client.render.pokemon.PokemonRenderer;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import fr.tropimon.teamsaver.client.AutomationVisibility;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = PokemonRenderer.class, remap = false)
abstract class PokemonRendererMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true, remap = false)
    private void tropimon$hideAutomatedItemTransfer(PokemonEntity pokemon, float yaw, float tickDelta,
                                                     MatrixStack matrices, VertexConsumerProvider vertices,
                                                     int light, CallbackInfo ci) {
        if (AutomationVisibility.isHidden(pokemon.getUuid())
                || AutomationVisibility.isHidden(pokemon.getPokemon().getUuid())) {
            ci.cancel();
        }
    }
}
