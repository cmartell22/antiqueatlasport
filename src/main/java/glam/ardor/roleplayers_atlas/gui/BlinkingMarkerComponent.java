package glam.ardor.roleplayers_atlas.gui;

import glam.ardor.roleplayers_atlas.MarkerTexture;
import glam.ardor.roleplayers_atlas.gui.core.BlinkingTextureComponent;

public class BlinkingMarkerComponent extends BlinkingTextureComponent implements MarkerModal.IMarkerTypeSelectListener {
	public void onSelectMarkerType(MarkerTexture markerTexture) {
		setTexture(markerTexture.id(), AtlasScreen.MARKER_SIZE, AtlasScreen.MARKER_SIZE);
	}
}
