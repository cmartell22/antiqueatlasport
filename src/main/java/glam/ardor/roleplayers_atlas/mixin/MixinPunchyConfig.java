package glam.ardor.roleplayers_atlas.mixin;

import glam.ardor.roleplayers_atlas.AtlasHoldMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Punchy! compat. Punchy cancels the whole vanilla first-person item pass at
// its HEAD and draws its own hands, so the held atlas never reaches the frame.
// Its supported escape hatch is the per-hand item blacklist: a blacklisted
// hand is left to the vanilla renderer, where all of our hooks live. While the
// atlas is out, report both hands as blacklisted.
@Pseudo
@Mixin(targets = "punchy.config.PunchyConfig", remap = false)
public class MixinPunchyConfig {
	@Inject(method = "isHandBlacklisted", at = @At("HEAD"), cancellable = true, require = 0)
	private static void roleplayers_atlas$vanillaHandsForAtlas(CallbackInfoReturnable<Boolean> cir) {
		if (AtlasHoldMode.isActive()) {
			cir.setReturnValue(true);
		}
	}
}
