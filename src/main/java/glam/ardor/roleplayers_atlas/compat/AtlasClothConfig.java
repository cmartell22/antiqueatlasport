package glam.ardor.roleplayers_atlas.compat;

import glam.ardor.roleplayers_atlas.AtlasConfig;
import glam.ardor.roleplayers_atlas.RoleplayersAtlas;
import glam.ardor.roleplayers_atlas.WorldAtlasData;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.Arrays;

/**
 * The settings screen, built with Cloth Config: a searchable list with a reset
 * button on every row and the usual cancel / save buttons.
 * <p>
 * Cloth is optional, so nothing outside this class may name it — the mod has to
 * keep working for players who don't have it. {@link AtlasModMenu} checks first
 * and only then calls in here, which is what keeps the class from being loaded
 * at all when Cloth is absent.
 * <p>
 * Entries write to the plain config fields; the file is only touched once, when
 * the player saves. That is what makes cancelling actually cancel.
 */
public final class AtlasClothConfig {
	private AtlasClothConfig() {
	}

	private static Text label(String key) {
		return Text.translatable("config.roleplayers_atlas." + key);
	}

	private static Text tip(String key) {
		return Text.translatable("config.roleplayers_atlas." + key + ".tooltip");
	}

	public static Screen create(Screen parent) {
		AtlasConfig config = RoleplayersAtlas.CONFIG;
		// Anything that changes what a tile should look like has to redraw the
		// land already on the map, or the setting appears to do nothing at all.
		AtlasConfig.FallbackHandling wasFallback = config.fallbackFailHandling;
		int[] wasElevations = {config.elevationLow, config.elevationMid, config.elevationHigh, config.elevationPeak};
		ConfigBuilder builder = ConfigBuilder.create()
			.setParentScreen(parent)
			.setTitle(Text.translatable("gui.roleplayers_atlas.config"))
			.setSavingRunnable(() -> {
				config.saveFields();
				boolean elevationsChanged = wasElevations[0] != config.elevationLow
					|| wasElevations[1] != config.elevationMid
					|| wasElevations[2] != config.elevationHigh
					|| wasElevations[3] != config.elevationPeak;
				if (config.fallbackFailHandling != wasFallback || elevationsChanged) WorldAtlasData.retileAll();
			});
		ConfigEntryBuilder e = builder.entryBuilder();
		// One category, so Cloth hides the tab bar and shows a plain list.
		ConfigCategory c = builder.getOrCreateCategory(Text.translatable("gui.roleplayers_atlas.config"));

		c.addEntry(e.startBooleanToggle(label("fullscreen"), config.fullscreen)
			.setDefaultValue(true)
			.setTooltip(tip("fullscreen"))
			.setSaveConsumer(v -> config.fullscreen = v)
			.build());
		c.addEntry(e.startBooleanToggle(label("keepZoom"), config.keepZoom)
			.setDefaultValue(false)
			.setTooltip(tip("keepZoom"))
			.setSaveConsumer(v -> config.keepZoom = v)
			.build());
		c.addEntry(e.startBooleanToggle(label("keepOffset"), config.keepOffset)
			.setDefaultValue(false)
			.setTooltip(tip("keepOffset"))
			.setSaveConsumer(v -> config.keepOffset = v)
			.build());
		c.addEntry(e.startEnumSelector(label("emptyHandling"), AtlasConfig.EmptyHandling.class, config.emptyHandling)
			.setDefaultValue(AtlasConfig.EmptyHandling.EMPTY)
			.setEnumNameProvider(v -> Text.translatable("gui.roleplayers_atlas.config.emptyHandling." + v.name().toLowerCase()))
			.setTooltip(tip("emptyHandling"))
			.setSaveConsumer(v -> config.emptyHandling = v)
			.build());

		c.addEntry(e.startBooleanToggle(label("showHints"), config.showHints)
			.setDefaultValue(true)
			.setTooltip(tip("showHints"))
			.setSaveConsumer(v -> config.showHints = v)
			.build());
		c.addEntry(e.startBooleanToggle(label("stabilizeHeldMap"), config.stabilizeHeldMap)
			.setDefaultValue(false)
			.setTooltip(tip("stabilizeHeldMap"))
			.setSaveConsumer(v -> config.stabilizeHeldMap = v)
			.build());
		c.addEntry(e.startEnumSelector(label("quickMark"), AtlasConfig.QuickMark.class, config.quickMark)
			.setDefaultValue(AtlasConfig.QuickMark.LOOKING)
			.setEnumNameProvider(v -> Text.translatable("gui.roleplayers_atlas.config.quickMark." + v.name().toLowerCase()))
			.setTooltip(tip("quickMark"))
			.setSaveConsumer(v -> config.quickMark = v)
			.build());
		// Whatever icons the resource pack actually ships, read at the moment the
		// screen opens rather than listed here and left to rot.
		java.util.List<String> icons = glam.ardor.roleplayers_atlas.QuickMark.icons();
		if (!icons.isEmpty()) {
			c.addEntry(e.startSelector(label("quickMarkIcon"), icons.toArray(new String[0]),
					icons.contains(config.quickMarkIcon) ? config.quickMarkIcon : icons.getFirst())
				.setDefaultValue("red_x_small")
				.setNameProvider(v -> Text.literal(org.apache.commons.lang3.text.WordUtils.capitalizeFully(v.replace('_', ' '))))
				.setTooltip(tip("quickMarkIcon"))
				.setSaveConsumer(v -> config.quickMarkIcon = v)
				.build());
		}
		c.addEntry(e.startIntSlider(label("quickMarkRange"), config.quickMarkRange, 16, 512)
			.setDefaultValue(256)
			.setTextGetter(v -> Text.translatable("config.roleplayers_atlas.blocks", v))
			.setTooltip(tip("quickMarkRange"))
			.setSaveConsumer(v -> config.quickMarkRange = v)
			.build());
		c.addEntry(e.startEnumSelector(label("markerSort"), AtlasConfig.MarkerSort.class, config.markerSort)
			.setDefaultValue(AtlasConfig.MarkerSort.KIND)
			.setEnumNameProvider(v -> Text.translatable("gui.roleplayers_atlas.sort." + v.name().toLowerCase()))
			.setTooltip(tip("markerSort"))
			.setSaveConsumer(v -> config.markerSort = v)
			.build());

		c.addEntry(e.startBooleanToggle(label("zoneTitles"), config.zoneTitles)
			.setDefaultValue(true)
			.setTooltip(tip("zoneTitles"))
			.setSaveConsumer(v -> config.zoneTitles = v)
			.build());
		c.addEntry(e.startBooleanToggle(label("zoneTitleSound"), config.zoneTitleSound)
			.setDefaultValue(false)
			.setTooltip(tip("zoneTitleSound"))
			.setSaveConsumer(v -> config.zoneTitleSound = v)
			.build());
		c.addEntry(e.startIntSlider(label("zoneTitleRadius"), config.zoneTitleRadius, 4, 256)
			.setDefaultValue(32)
			.setTextGetter(v -> Text.translatable("config.roleplayers_atlas.blocks", v))
			.setTooltip(tip("zoneTitleRadius"))
			.setSaveConsumer(v -> config.zoneTitleRadius = v)
			.build());

		c.addEntry(e.startBooleanToggle(label("deathMarkers"), config.deathMarkers)
			.setDefaultValue(true)
			.setTooltip(tip("deathMarkers"))
			.setSaveConsumer(v -> config.deathMarkers = v)
			.build());
		c.addEntry(e.startBooleanToggle(label("hideDeathsByDefault"), config.hideDeathsByDefault)
			.setDefaultValue(false)
			.setTooltip(tip("hideDeathsByDefault"))
			.setSaveConsumer(v -> config.hideDeathsByDefault = v)
			.build());
		c.addEntry(e.startEnumSelector(label("graveStyle"), AtlasConfig.GraveStyle.class, config.graveStyle)
			.setDefaultValue(AtlasConfig.GraveStyle.EUPHEMISMS)
			.setEnumNameProvider(v -> Text.translatable("gui.roleplayers_atlas.config.graveStyle." + v.name().toLowerCase()))
			.setTooltip(tip("graveStyle"))
			.setSaveConsumer(v -> config.graveStyle = v)
			.build());

		c.addEntry(e.startIntSlider(label("guideArrowOpacity"), config.guideArrowOpacity, 10, 100)
			.setDefaultValue(100)
			.setTextGetter(v -> Text.translatable("config.roleplayers_atlas.percent", v))
			.setTooltip(tip("guideArrowOpacity"))
			.setSaveConsumer(v -> config.guideArrowOpacity = v)
			.build());
		c.addEntry(e.startBooleanToggle(label("clearTrackingOnArrival"), config.clearTrackingOnArrival)
			.setDefaultValue(true)
			.setTooltip(tip("clearTrackingOnArrival"))
			.setSaveConsumer(v -> config.clearTrackingOnArrival = v)
			.build());

		// Only whether the hearth exists at all. Its icon, colour and opacity are
		// restyled from the atlas itself, like any other mark.
		c.addEntry(e.startBooleanToggle(label("spawnMarker"), config.spawnMarker)
			.setDefaultValue(true)
			.setTooltip(tip("spawnMarker"))
			.setSaveConsumer(v -> config.spawnMarker = v)
			.build());
		c.addEntry(e.startBooleanToggle(label("hearthFollowsBeds"), config.hearthFollowsBeds)
			.setDefaultValue(false)
			.setTooltip(tip("hearthFollowsBeds"))
			.setSaveConsumer(v -> config.hearthFollowsBeds = v)
			.build());

		c.addEntry(e.startIntField(label("elevationLow"), config.elevationLow)
			.setDefaultValue(10).setMin(-64).setMax(320)
			.setTooltip(tip("elevationLow"))
			.setSaveConsumer(v -> config.elevationLow = v)
			.build());
		c.addEntry(e.startIntField(label("elevationMid"), config.elevationMid)
			.setDefaultValue(20).setMin(-64).setMax(320)
			.setTooltip(tip("elevationMid"))
			.setSaveConsumer(v -> config.elevationMid = v)
			.build());
		c.addEntry(e.startIntField(label("elevationHigh"), config.elevationHigh)
			.setDefaultValue(50).setMin(-64).setMax(320)
			.setTooltip(tip("elevationHigh"))
			.setSaveConsumer(v -> config.elevationHigh = v)
			.build());
		c.addEntry(e.startIntField(label("elevationPeak"), config.elevationPeak)
			.setDefaultValue(90).setMin(-64).setMax(320)
			.setTooltip(tip("elevationPeak"))
			.setSaveConsumer(v -> config.elevationPeak = v)
			.build());

		c.addEntry(e.startIntSlider(label("mapScale"), config.mapScale, -2, 10)
			.setDefaultValue(0)
			.setTooltip(tip("mapScale"))
			.setSaveConsumer(v -> config.mapScale = v)
			.build());
		c.addEntry(e.startIntSlider(label("maxTileChunks"), config.maxTileChunks, 0, 6)
			.setDefaultValue(5)
			.setTooltip(tip("maxTileChunks"))
			.setSaveConsumer(v -> config.maxTileChunks = v)
			.build());
		c.addEntry(e.startIntSlider(label("maxTilePixels"), config.maxTilePixels, 0, 3)
			.setDefaultValue(1)
			.setTooltip(tip("maxTilePixels"))
			.setSaveConsumer(v -> config.maxTilePixels = v)
			.build());
		c.addEntry(e.startIntField(label("chunkTickLimit"), config.chunkTickLimit)
			.setDefaultValue(100)
			.setMin(1)
			.setMax(4096)
			.setTooltip(tip("chunkTickLimit"))
			.setSaveConsumer(v -> config.chunkTickLimit = v)
			.build());
		// CRASH is a resource pack author's tool — it stops the game dead on a
		// biome with no texture. Left out of the list so a player can't pick it
		// by accident; the config file still takes it.
		AtlasConfig.FallbackHandling[] fallbacks = Arrays.stream(AtlasConfig.FallbackHandling.values())
			.filter(v -> v != AtlasConfig.FallbackHandling.CRASH)
			.toArray(AtlasConfig.FallbackHandling[]::new);
		AtlasConfig.FallbackHandling fallback = config.fallbackFailHandling == AtlasConfig.FallbackHandling.CRASH
			? AtlasConfig.FallbackHandling.MISSING
			: config.fallbackFailHandling;
		c.addEntry(e.startSelector(label("fallbackFailHandling"), fallbacks, fallback)
			.setDefaultValue(AtlasConfig.FallbackHandling.MISSING)
			.setNameProvider(v -> Text.translatable("gui.roleplayers_atlas.config.fallbackFailHandling." + v.name().toLowerCase()))
			.setTooltip(tip("fallbackFailHandling"))
			.setSaveConsumer(v -> config.fallbackFailHandling = v)
			.build());

		return builder.build();
	}
}
