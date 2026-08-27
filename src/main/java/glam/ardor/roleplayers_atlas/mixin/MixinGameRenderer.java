package glam.ardor.roleplayers_atlas.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import glam.ardor.roleplayers_atlas.AtlasHoldMode;
import glam.ardor.roleplayers_atlas.RoleplayersAtlas;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(GameRenderer.class)
public class MixinGameRenderer {
	// Stabilization: with the option on, the walk bob is kept out of the hand
	// pass while the atlas is up, so the book reads steady against the moving
	// world. Only this pass is skipped - the world keeps bobbing as configured,
	// and the hurt tilt still comes through.
	@WrapOperation(method = "renderHand", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/GameRenderer;bobView(Lnet/minecraft/client/render/state/CameraRenderState;Lnet/minecraft/client/util/math/MatrixStack;)V"))
	private void roleplayers_atlas$steadyAtlas(GameRenderer renderer, CameraRenderState cameraState, MatrixStack matrices, Operation<Void> original) {
		if (RoleplayersAtlas.CONFIG.stabilizeHeldMap && AtlasHoldMode.isActive()) return;
		original.call(renderer, cameraState, matrices);
	}
}
