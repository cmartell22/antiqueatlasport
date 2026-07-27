package folk.sisby.roleplayers_atlas.compat;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import folk.sisby.roleplayers_atlas.gui.AtlasConfigScreen;
import net.fabricmc.loader.api.FabricLoader;

/**
 * ModMenu integration: opens the atlas settings.
 * <p>
 * Cloth Config gives the better screen — searchable, with a reset on every row —
 * but it is only suggested, not required, so the plain screen stays behind as a
 * fallback. The check happens inside the factory rather than up here, so the
 * Cloth-facing class is never loaded on a client that doesn't have it.
 */
public class AtlasModMenu implements ModMenuApi {
	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return parent -> FabricLoader.getInstance().isModLoaded("cloth-config")
			? AtlasClothConfig.create(parent)
			: new AtlasConfigScreen(parent);
	}
}
