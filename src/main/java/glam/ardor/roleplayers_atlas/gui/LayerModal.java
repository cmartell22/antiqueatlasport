package glam.ardor.roleplayers_atlas.gui;

import net.minecraft.client.gui.Click;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.input.CharInput;
import glam.ardor.roleplayers_atlas.RoleplayersAtlas;
import net.minecraft.client.gui.Click;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.input.CharInput;
import glam.ardor.roleplayers_atlas.AtlasComponents;
import net.minecraft.client.gui.Click;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.input.CharInput;
import glam.ardor.roleplayers_atlas.MarkerLayers;
import net.minecraft.client.gui.Click;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.input.CharInput;
import glam.ardor.roleplayers_atlas.WorldAtlasData;
import net.minecraft.client.gui.Click;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.input.CharInput;
import glam.ardor.roleplayers_atlas.gui.core.Component;
import net.minecraft.client.gui.Click;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.input.CharInput;
import glam.ardor.roleplayers_atlas.gui.core.ScrollBoxComponent;
import net.minecraft.client.gui.Click;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.input.CharInput;
import glam.ardor.roleplayers_atlas.gui.core.ToggleButtonRadioGroup;
import net.minecraft.client.gui.Click;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.input.CharInput;
import glam.ardor.roleplayers_atlas.util.ColorUtil;
import net.minecraft.client.gui.Click;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.input.CharInput;
import folk.sisby.surveyor.WorldSummary;
import net.minecraft.client.gui.Click;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.input.CharInput;
import folk.sisby.surveyor.landmark.Landmark;
import net.minecraft.client.gui.Click;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Click;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.Click;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.Click;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.input.CharInput;
import net.minecraft.text.Text;
import net.minecraft.client.gui.Click;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.input.CharInput;
import net.minecraft.util.DyeColor;

import net.minecraft.client.gui.Click;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.input.CharInput;
import java.util.ArrayList;
import net.minecraft.client.gui.Click;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.input.CharInput;
import java.util.LinkedHashMap;
import net.minecraft.client.gui.Click;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.input.CharInput;
import java.util.List;
import net.minecraft.client.gui.Click;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.input.CharInput;
import java.util.Map;

/**
 * Creation/editing dialog for a player-defined marker layer: pick a name and
 * a color. Deleting a layer moves its landmarks back to the personal layer.
 */
public class LayerModal extends Component {
	private static final int BTN_W = 120;
	private static final int SP = 8;
	private static final int TYPE_SPACING = 1;

	private WorldSummary summary;
	private WorldAtlasData worldAtlasData;
	private MarkerLayers.MapLayer editing;

	private DyeColor selectedColor = DyeColor.WHITE;
	private TextFieldWidget nameField;
	private ButtonWidget btnDone;
	private ButtonWidget btnDelete;
	private ButtonWidget btnDeleteAll;
	private ButtonWidget btnCancel;
	private ScrollBoxComponent colorScrollBox;
	private ToggleButtonRadioGroup<TexturePreviewButton<DyeColor>> colorRadioGroup;
	private final Map<DyeColor, TexturePreviewButton<DyeColor>> colorButtons = new LinkedHashMap<>();
	private String pendingName = "";

	void setData(WorldSummary summary, WorldAtlasData worldAtlasData, MarkerLayers.MapLayer editing) {
		this.summary = summary;
		this.worldAtlasData = worldAtlasData;
		this.editing = editing;
		this.pendingName = editing == null ? "" : editing.name();
		this.selectedColor = DyeColor.WHITE;
		if (editing != null) {
			for (DyeColor color : DyeColor.values()) {
				if (color.getEntityColor() == editing.color()) selectedColor = color;
			}
		}
	}

	@Override
	public void init() {
		removeAllChildren();
		super.init();

		nameField = new TextFieldWidget(MinecraftClient.getInstance().textRenderer, (this.width - 200) / 2, this.height / 2 - 38, 200, 18, Text.translatable("gui.roleplayers_atlas.layerModal.name"));
		nameField.setEditable(true);
		nameField.setFocusUnlocked(true);
		nameField.setFocused(true);
		nameField.setPlaceholder(Text.translatable("gui.roleplayers_atlas.layerModal.name"));
		nameField.setText(pendingName);

		colorScrollBox = new ScrollBoxComponent(false, (TexturePreviewButton.FRAME_SIZE + TYPE_SPACING));
		this.addChild(colorScrollBox);
		int colorsOnScreen = Math.min(DyeColor.values().length, 7);
		int colorScrollWidth = colorsOnScreen * (TexturePreviewButton.FRAME_SIZE + TYPE_SPACING) - TYPE_SPACING;
		colorScrollBox.getViewport().setSize(colorScrollWidth, TexturePreviewButton.FRAME_SIZE + TYPE_SPACING);
		colorScrollBox.setGuiCoords((this.width - colorScrollWidth) / 2, this.height / 2 - 10);

		colorRadioGroup = new ToggleButtonRadioGroup<>();
		colorRadioGroup.addListener(button -> selectedColor = button.getValue());
		colorButtons.clear();
		int colorContentX = 0;
		for (DyeColor color : DyeColor.values()) {
			TexturePreviewButton<DyeColor> colorGui = new TexturePreviewButton<>(color, BookmarkButton.TEXTURE_LEFT, BookmarkButton.WIDTH, BookmarkButton.HEIGHT, BookmarkButton.HEIGHT, ColorUtil.componentsFromRgb(color.getFireworkColor()));
			colorButtons.put(color, colorGui);
			colorRadioGroup.addButton(colorGui);
			colorScrollBox.getViewport().addContent(colorGui).setRelativeX(colorContentX);
			colorContentX += TexturePreviewButton.FRAME_SIZE + TYPE_SPACING;
		}
		colorRadioGroup.setSelectedButton(colorButtons.get(selectedColor));

		int bottomY = this.height / 2 + 34;
		addDrawableChild(btnDone = ButtonWidget.builder(Text.translatable("gui.done"), button -> doSave())
			.dimensions(this.width / 2 - BTN_W - SP / 2, bottomY, BTN_W, 20).build());
		addDrawableChild(btnCancel = ButtonWidget.builder(Text.translatable("gui.cancel"), button -> closeChild())
			.dimensions(this.width / 2 + SP / 2, bottomY, BTN_W, 20).build());
		int deleteY = bottomY + 24;
		addDrawableChild(btnDelete = ButtonWidget.builder(Text.translatable("gui.roleplayers_atlas.layerModal.delete"), button -> doDelete(false))
			.dimensions(this.width / 2 - BTN_W - SP / 2, deleteY, BTN_W, 20).build());
		addDrawableChild(btnDeleteAll = ButtonWidget.builder(Text.translatable("gui.roleplayers_atlas.layerModal.deleteAll"), button -> doDelete(true))
			.dimensions(this.width / 2 + SP / 2, deleteY, BTN_W, 20).build());
	}

	private void doSave() {
		String name = nameField.getText().trim();
		if (name.isBlank()) return;
		String id = editing != null ? editing.id() : "layer_" + Integer.toHexString((name + System.nanoTime()).hashCode());
		MarkerLayers.put(new MarkerLayers.MapLayer(id, name, selectedColor.getEntityColor()));
		closeChild();
	}

	private void doDelete(boolean withMarkers) {
		if (editing == null) {
			closeChild();
			return;
		}
		if (summary != null && summary.landmarks() != null && worldAtlasData != null) {
			List<Landmark> affected = new ArrayList<>();
			for (Landmark landmark : worldAtlasData.getEditableLandmarks().keySet()) {
				if (editing.id().equals(RoleplayersAtlas.layerOf(landmark))) affected.add(landmark);
			}
			for (Landmark landmark : affected) {
				summary.landmarks().remove(landmark.owner(), landmark.id());
				if (withMarkers) {
					// Deleted together with the layer.
					RoleplayersAtlas.trackedMarkers.remove(RoleplayersAtlas.trackKey(landmark));
				} else {
					// Landmarks of the deleted layer fall back to the personal layer.
					summary.landmarks().put(WorldAtlasData.copyLandmarkWith(landmark, landmark.id(), copy -> copy.set(AtlasComponents.LAYER, MarkerLayers.DEFAULT_ID)));
				}
			}
			if (withMarkers) glam.ardor.roleplayers_atlas.TrackedMarkersStore.save();
		}
		MarkerLayers.remove(editing.id());
		closeChild();
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float partialTick) {
		context.fill(0, 0, this.width, this.height, 0x66000000);
		drawCentered(context, Text.translatable(editing == null ? "gui.roleplayers_atlas.layerModal.titleNew" : "gui.roleplayers_atlas.layerModal.titleEdit"), this.height / 2 - 58, 0xDDDDDD, true);
		btnDone.active = !nameField.getText().isBlank();
		btnDelete.visible = editing != null;
		btnDeleteAll.visible = editing != null;
		nameField.render(context, mouseX, mouseY, partialTick);
		btnDone.render(context, mouseX, mouseY, partialTick);
		if (btnDelete.visible) btnDelete.render(context, mouseX, mouseY, partialTick);
		if (btnDeleteAll.visible) btnDeleteAll.render(context, mouseX, mouseY, partialTick);
		btnCancel.render(context, mouseX, mouseY, partialTick);
		context.fillGradient(colorScrollBox.getGuiX() + 1, colorScrollBox.getGuiY() + 1,
			colorScrollBox.getGuiX() + colorScrollBox.getWidth(),
			colorScrollBox.getGuiY() + colorScrollBox.getHeight(),
			0x88101010, 0x99101010);
		super.render(context, mouseX, mouseY, partialTick);
	}

	@Override
	public boolean mouseClicked(Click click, boolean doubled) {
		double mouseX = click.x(), mouseY = click.y();
		int button = click.button();
		return super.mouseClicked(click, doubled) || nameField.mouseClicked(click, doubled);
	}

	@Override
	public boolean keyPressed(KeyInput input) {
		int keyCode = input.key(), scanCode = input.scancode(), modifiers = input.modifiers();
		if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
			closeChild();
			return true;
		}
		if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER || keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_KP_ENTER) {
			if (btnDone.active) btnDone.onPress(input);
			return true;
		}
		return super.keyPressed(input) || nameField.keyPressed(input);
	}

	@Override
	public boolean charTyped(CharInput input) {
		char chr = (char) input.codepoint();
		int modifiers = input.modifiers();
		return super.charTyped(input) || nameField.charTyped(input);
	}

	@Override
	public void closeChild() {
		super.closeChild();
		if (colorScrollBox != null) colorScrollBox.closeChild();
	}
}
