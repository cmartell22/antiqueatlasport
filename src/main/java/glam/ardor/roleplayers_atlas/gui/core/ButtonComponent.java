package glam.ardor.roleplayers_atlas.gui.core;

import net.minecraft.client.gui.Click;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.gui.Click;
import net.minecraft.sound.SoundEvent;
import net.minecraft.client.gui.Click;
import net.minecraft.sound.SoundEvents;

import net.minecraft.client.gui.Click;
import java.util.ArrayList;
import net.minecraft.client.gui.Click;
import java.util.List;

/**
 * A GuiComponent that can act like a button.
 */
@SuppressWarnings("rawtypes")
public class ButtonComponent extends Component {
	protected final List<IButtonListener> listeners = new ArrayList<>();

	protected SoundEvent clickSound = SoundEvents.UI_BUTTON_CLICK.value();

	@Override
	public boolean mouseClicked(Click click, boolean doubled) {
		double x = click.x(), y = click.y();
		int mouseButton = click.button();
		if (!isClipped && mouseButton == 0 && isMouseOver(x, y)) {
			onClick();
			return true;
		}

		return super.mouseClicked(click, doubled);
	}

	/**
	 * Called when the user left-clicks on this component.
	 */
	@SuppressWarnings("unchecked")
	public void onClick() {
		if (clickSound != null) {
			MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.ui(clickSound, 1.0F));
		}

		for (IButtonListener listener : listeners) {
			listener.onClick(this);
		}
	}

	public void addListener(IButtonListener listener) {
		listeners.add(listener);
	}
}
