package fr.tropimon.teamsaver.mixin.client;

import fr.tropimon.teamsaver.client.AutomationVisibility;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SoundSystem.class)
abstract class SoundSystemMixin {
    @Inject(method = "play(Lnet/minecraft/client/sound/SoundInstance;)V", at = @At("HEAD"), cancellable = true)
    private void tropimon$muteAutomatedPokemonTransfer(SoundInstance sound, CallbackInfo ci) {
        String path = sound.getId().getPath();
        if (AutomationVisibility.isAutomationActive()
                && "cobblemon".equals(sound.getId().getNamespace())
                && ("poke_ball.send_out".equals(path)
                || "poke_ball.shiny_send_out".equals(path)
                || "poke_ball.recall".equals(path))) {
            ci.cancel();
        }
    }
}
