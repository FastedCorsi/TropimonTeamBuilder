package fr.tropimon.teamsaver.mixin.client;

import com.cobblemon.mod.common.client.render.pokeball.PokeBallRenderer;
import com.cobblemon.mod.common.entity.pokeball.EmptyPokeBallEntity;
import fr.tropimon.teamsaver.client.AutomationVisibility;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Hides the brief Poké Ball animation used by automated held-item transfers. */
@Mixin(value = PokeBallRenderer.class, remap = false)
abstract class PokeBallRendererMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true, remap = false)
    private void tropimon$hideAutomatedItemTransfer(EmptyPokeBallEntity pokeBall, float yaw, float tickDelta,
                                                     MatrixStack matrices, VertexConsumerProvider vertices,
                                                     int light, CallbackInfo ci) {
        if (AutomationVisibility.isAutomationActive()) ci.cancel();
    }
}
