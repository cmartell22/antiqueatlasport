package folk.sisby.roleplayers_atlas.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;

/**
 * Screen lost its static hasShiftDown/hasControlDown/hasAltDown helpers in 1.21.9,
 * which now live on the input object handed to each callback. Half the places that
 * ask here are not callbacks at all - they are draws and tooltips deciding what to
 * show while a modifier is merely being held - so the window is polled directly,
 * exactly as the old helpers did.
 * <p>
 * The names are kept so that a static import restores the calls that used to be
 * inherited from Screen.
 */
public final class AtlasInput {
	private AtlasInput() {
	}

	private static boolean pressed(int leftKey, int rightKey) {
		var window = MinecraftClient.getInstance().getWindow();
		if (window == null) return false;
		return InputUtil.isKeyPressed(window, leftKey) || InputUtil.isKeyPressed(window, rightKey);
	}

	public static boolean hasShiftDown() {
		return pressed(InputUtil.GLFW_KEY_LEFT_SHIFT, InputUtil.GLFW_KEY_RIGHT_SHIFT);
	}

	public static boolean hasControlDown() {
		return pressed(InputUtil.GLFW_KEY_LEFT_CONTROL, InputUtil.GLFW_KEY_RIGHT_CONTROL);
	}

	public static boolean hasAltDown() {
		return pressed(InputUtil.GLFW_KEY_LEFT_ALT, InputUtil.GLFW_KEY_RIGHT_ALT);
	}
}
