package glam.ardor.roleplayers_atlas;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.util.InputUtil;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;


public class AtlasKeybindings {
	/**
	 * Categories stopped being bare translation keys in 1.21.9. The label is now
	 * derived from the id as {@code key.category.<namespace>.<path>}, which is the
	 * key the language files carry.
	 */
	private static final KeyBinding.Category CATEGORY = KeyBinding.Category.create(Identifier.of(RoleplayersAtlas.ID, "atlas"));

	public static final KeyBinding ATLAS_KEYMAPPING = new KeyBinding("key.roleplayers_atlas.open", InputUtil.Type.KEYSYM, 77, CATEGORY);
	/** N by default, and rebindable in the vanilla controls screen like any other. */
	public static final KeyBinding QUICK_MARK_KEYMAPPING = new KeyBinding("key.roleplayers_atlas.quickMark", InputUtil.Type.KEYSYM, 78, CATEGORY);

	public static void init() {
		KeyMappingHelper.registerKeyMapping(ATLAS_KEYMAPPING);
		KeyMappingHelper.registerKeyMapping(QUICK_MARK_KEYMAPPING);
		ClientTickEvents.END_CLIENT_TICK.register(AtlasKeybindings::onClientTick);
	}

	public static void onClientTick(MinecraftClient client) {
		while (QUICK_MARK_KEYMAPPING.wasPressed()) {
			if (client.player != null) QuickMark.place(client);
		}
		while (ATLAS_KEYMAPPING.wasPressed()) {
			if (client.player == null) continue;
			if (AtlasHoldMode.isClosing()) {
				// Pressing the key while the book is being put away draws it again.
				AtlasHoldMode.activate();
			} else if (AtlasHoldMode.isActive()) {
				// Second press: put the book away and open the full atlas screen.
				AtlasHoldMode.deactivate();
				RoleplayersAtlas.openAtlasScreen();
			} else {
				// First press: draw the book into the player's hands.
				
				AtlasHoldMode.activate();
				client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.ITEM_BOOK_PAGE_TURN, 1.0F));
			}
		}
	}
}
