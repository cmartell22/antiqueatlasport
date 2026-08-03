package glam.ardor.roleplayers_atlas.gui;

import glam.ardor.roleplayers_atlas.RoleplayersAtlas;
import glam.ardor.roleplayers_atlas.AtlasConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

/**
 * Simple settings screen (opened from ModMenu): the most useful client
 * options, saved straight into the TOML config.
 */
public class AtlasConfigScreen extends Screen {
	private final Screen parent;

	public AtlasConfigScreen(Screen parent) {
		super(Text.translatable("gui.roleplayers_atlas.config"));
		this.parent = parent;
	}

	private static Text onOff(String key, boolean value) {
		return Text.translatable(key, Text.translatable(value ? "gui.roleplayers_atlas.marker.zoneTitle.on" : "gui.roleplayers_atlas.marker.zoneTitle.off"));
	}

	private ButtonWidget toggle(String langKey, String configKey, java.util.function.Supplier<Boolean> getter, java.util.function.Consumer<Boolean> setter, int x, int y) {
		return ButtonWidget.builder(onOff(langKey, getter.get()), button -> {
			boolean value = !getter.get();
			setter.accept(value);
			RoleplayersAtlas.CONFIG.setAndSave(configKey, value);
			button.setMessage(onOff(langKey, value));
		}).dimensions(x, y, 150, 20).build();
	}

	@Override
	protected void init() {
		int left = this.width / 2 - 155;
		int right = this.width / 2 + 5;
		int y = this.height / 2 - 96;

		addDrawableChild(toggle("gui.roleplayers_atlas.config.deathMarkers", "deathMarkers", () -> RoleplayersAtlas.CONFIG.deathMarkers, v -> RoleplayersAtlas.CONFIG.deathMarkers = v, left, y));
		addDrawableChild(toggle("gui.roleplayers_atlas.config.zoneTitles", "zoneTitles", () -> RoleplayersAtlas.CONFIG.zoneTitles, v -> RoleplayersAtlas.CONFIG.zoneTitles = v, right, y));
		y += 24;
		addDrawableChild(new RadiusSlider(left, y));
		addDrawableChild(ButtonWidget.builder(graveStyleText(), button -> {
			AtlasConfig.GraveStyle[] styles = AtlasConfig.GraveStyle.values();
			RoleplayersAtlas.CONFIG.graveStyle = styles[(RoleplayersAtlas.CONFIG.graveStyle.ordinal() + 1) % styles.length];
			RoleplayersAtlas.CONFIG.setAndSave("graveStyle", RoleplayersAtlas.CONFIG.graveStyle);
			button.setMessage(graveStyleText());
		}).dimensions(right, y, 150, 20).build());
		y += 24;
		addDrawableChild(toggle("gui.roleplayers_atlas.config.fullscreen", "fullscreen", () -> RoleplayersAtlas.CONFIG.fullscreen, v -> RoleplayersAtlas.CONFIG.fullscreen = v, left, y));
		addDrawableChild(toggle("gui.roleplayers_atlas.config.keepZoom", "keepZoom", () -> RoleplayersAtlas.CONFIG.keepZoom, v -> RoleplayersAtlas.CONFIG.keepZoom = v, right, y));
		y += 24;
		addDrawableChild(toggle("gui.roleplayers_atlas.config.keepOffset", "keepOffset", () -> RoleplayersAtlas.CONFIG.keepOffset, v -> RoleplayersAtlas.CONFIG.keepOffset = v, left, y));
		addDrawableChild(toggle("gui.roleplayers_atlas.config.hideDeathsByDefault", "hideDeathsByDefault", () -> RoleplayersAtlas.CONFIG.hideDeathsByDefault, v -> RoleplayersAtlas.CONFIG.hideDeathsByDefault = v, right, y));
		y += 24;
		addDrawableChild(toggle("gui.roleplayers_atlas.config.zoneTitleSound", "zoneTitleSound", () -> RoleplayersAtlas.CONFIG.zoneTitleSound, v -> RoleplayersAtlas.CONFIG.zoneTitleSound = v, left, y));
		addDrawableChild(toggle("gui.roleplayers_atlas.config.clearTrackingOnArrival", "clearTrackingOnArrival", () -> RoleplayersAtlas.CONFIG.clearTrackingOnArrival, v -> RoleplayersAtlas.CONFIG.clearTrackingOnArrival = v, right, y));
		y += 24;
		addDrawableChild(toggle("gui.roleplayers_atlas.config.spawnMarker", "spawnMarker", () -> RoleplayersAtlas.CONFIG.spawnMarker, v -> RoleplayersAtlas.CONFIG.spawnMarker = v, left, y));
		addDrawableChild(toggle("gui.roleplayers_atlas.config.showHints", "showHints", () -> RoleplayersAtlas.CONFIG.showHints, v -> RoleplayersAtlas.CONFIG.showHints = v, right, y));
		y += 24;
		addDrawableChild(new ArrowOpacitySlider(left, y));
		addDrawableChild(ButtonWidget.builder(emptyHandlingText(), button -> {
			AtlasConfig.EmptyHandling[] modes = AtlasConfig.EmptyHandling.values();
			RoleplayersAtlas.CONFIG.emptyHandling = modes[(RoleplayersAtlas.CONFIG.emptyHandling.ordinal() + 1) % modes.length];
			RoleplayersAtlas.CONFIG.setAndSave("emptyHandling", RoleplayersAtlas.CONFIG.emptyHandling);
			button.setMessage(emptyHandlingText());
		}).dimensions(right, y, 150, 20).build());
		y += 24;
		// The quick-mark key and how the bookmark list is ordered. Kept in step
		// with the Cloth screen: without Cloth these were unreachable entirely.
		addDrawableChild(ButtonWidget.builder(quickMarkText(), button -> {
			AtlasConfig.QuickMark[] modes = AtlasConfig.QuickMark.values();
			RoleplayersAtlas.CONFIG.quickMark = modes[(RoleplayersAtlas.CONFIG.quickMark.ordinal() + 1) % modes.length];
			RoleplayersAtlas.CONFIG.setAndSave("quickMark", RoleplayersAtlas.CONFIG.quickMark);
			button.setMessage(quickMarkText());
		}).dimensions(left, y, 150, 20).build());
		addDrawableChild(ButtonWidget.builder(quickMarkIconText(), button -> {
			java.util.List<String> icons = glam.ardor.roleplayers_atlas.QuickMark.icons();
			if (icons.isEmpty()) return;
			int at = icons.indexOf(RoleplayersAtlas.CONFIG.quickMarkIcon);
			RoleplayersAtlas.CONFIG.quickMarkIcon = icons.get((at + 1) % icons.size());
			RoleplayersAtlas.CONFIG.setAndSave("quickMarkIcon", RoleplayersAtlas.CONFIG.quickMarkIcon);
			button.setMessage(quickMarkIconText());
		}).dimensions(right, y, 150, 20).build());
		y += 24;
		addDrawableChild(new QuickMarkRangeSlider(left, y));
		addDrawableChild(ButtonWidget.builder(markerSortText(), button -> {
			AtlasConfig.MarkerSort[] orders = AtlasConfig.MarkerSort.values();
			RoleplayersAtlas.CONFIG.markerSort = orders[(RoleplayersAtlas.CONFIG.markerSort.ordinal() + 1) % orders.length];
			RoleplayersAtlas.CONFIG.setAndSave("markerSort", RoleplayersAtlas.CONFIG.markerSort);
			button.setMessage(markerSortText());
		}).dimensions(right, y, 150, 20).build());
		y += 24;
		addDrawableChild(toggle("gui.roleplayers_atlas.config.stabilizeHeldMap", "stabilizeHeldMap", () -> RoleplayersAtlas.CONFIG.stabilizeHeldMap, v -> RoleplayersAtlas.CONFIG.stabilizeHeldMap = v, left, y));

		// Two clicks: undoing every setting at once is easy to hit by accident.
		addDrawableChild(ButtonWidget.builder(Text.translatable("gui.roleplayers_atlas.config.reset"), button -> {
			if (!resetArmed) {
				resetArmed = true;
				button.setMessage(Text.translatable("gui.roleplayers_atlas.config.resetConfirm"));
				return;
			}
			RoleplayersAtlas.CONFIG.resetToDefaults();
			resetArmed = false;
			clearAndInit();
		}).dimensions(this.width / 2 - 155, this.height / 2 + 152, 150, 20).build());
		addDrawableChild(ButtonWidget.builder(Text.translatable("gui.done"), button -> close())
			.dimensions(this.width / 2 + 5, this.height / 2 + 152, 150, 20).build());
	}

	private boolean resetArmed = false;

	private Text emptyHandlingText() {
		return Text.translatable("gui.roleplayers_atlas.config.emptyHandling", Text.translatable("gui.roleplayers_atlas.config.emptyHandling." + RoleplayersAtlas.CONFIG.emptyHandling.toString().toLowerCase()));
	}

	private Text quickMarkText() {
		return Text.translatable("gui.roleplayers_atlas.config.quickMarkShort", Text.translatable("gui.roleplayers_atlas.config.quickMark." + RoleplayersAtlas.CONFIG.quickMark.toString().toLowerCase()));
	}

	private Text quickMarkIconText() {
		return Text.translatable("gui.roleplayers_atlas.config.quickMarkIconShort", org.apache.commons.lang3.text.WordUtils.capitalizeFully(RoleplayersAtlas.CONFIG.quickMarkIcon.replace('_', ' ')));
	}

	private Text markerSortText() {
		return Text.translatable("gui.roleplayers_atlas.sort", Text.translatable("gui.roleplayers_atlas.sort." + RoleplayersAtlas.CONFIG.markerSort.toString().toLowerCase()));
	}

	private class QuickMarkRangeSlider extends SliderWidget {
		QuickMarkRangeSlider(int x, int y) {
			super(x, y, 150, 20, Text.empty(), (RoleplayersAtlas.CONFIG.quickMarkRange - 16) / 496.0);
			updateMessage();
		}

		@Override
		protected void updateMessage() {
			setMessage(Text.translatable("gui.roleplayers_atlas.config.quickMarkRangeShort", RoleplayersAtlas.CONFIG.quickMarkRange));
		}

		@Override
		protected void applyValue() {
			RoleplayersAtlas.CONFIG.quickMarkRange = MathHelper.clamp((int) Math.round(16 + value * 496), 16, 512);
			RoleplayersAtlas.CONFIG.setAndSave("quickMarkRange", RoleplayersAtlas.CONFIG.quickMarkRange);
			updateMessage();
		}
	}

	private Text graveStyleText() {
		return Text.translatable("gui.roleplayers_atlas.config.graveStyle", Text.translatable("gui.roleplayers_atlas.config.graveStyle." + RoleplayersAtlas.CONFIG.graveStyle.toString().toLowerCase()));
	}

	private class ArrowOpacitySlider extends SliderWidget {
		ArrowOpacitySlider(int x, int y) {
			super(x, y, 150, 20, Text.empty(), (RoleplayersAtlas.CONFIG.guideArrowOpacity - 10) / 90.0);
			updateMessage();
		}

		@Override
		protected void updateMessage() {
			setMessage(Text.translatable("gui.roleplayers_atlas.config.guideArrowOpacity", RoleplayersAtlas.CONFIG.guideArrowOpacity));
		}

		@Override
		protected void applyValue() {
			RoleplayersAtlas.CONFIG.guideArrowOpacity = MathHelper.clamp((int) Math.round(10 + value * 90), 10, 100);
			RoleplayersAtlas.CONFIG.setAndSave("guideArrowOpacity", RoleplayersAtlas.CONFIG.guideArrowOpacity);
			updateMessage();
		}
	}

	private class RadiusSlider extends SliderWidget {
		RadiusSlider(int x, int y) {
			super(x, y, 150, 20, Text.empty(), (RoleplayersAtlas.CONFIG.zoneTitleRadius - 4) / 252.0);
			updateMessage();
		}

		@Override
		protected void updateMessage() {
			setMessage(Text.translatable("gui.roleplayers_atlas.config.zoneTitleRadius", RoleplayersAtlas.CONFIG.zoneTitleRadius));
		}

		@Override
		protected void applyValue() {
			RoleplayersAtlas.CONFIG.zoneTitleRadius = MathHelper.clamp((int) Math.round(4 + value * 252), 4, 256);
			RoleplayersAtlas.CONFIG.setAndSave("zoneTitleRadius", RoleplayersAtlas.CONFIG.zoneTitleRadius);
			updateMessage();
		}
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		super.render(context, mouseX, mouseY, delta);
		context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, this.height / 2 - 120, 0xFFFFFFFF);
	}

	@Override
	public void close() {
		this.client.setScreen(parent);
	}
}
