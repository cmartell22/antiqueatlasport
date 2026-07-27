package folk.sisby.roleplayers_atlas.gui;

import folk.sisby.roleplayers_atlas.MarkerTexture;
import folk.sisby.roleplayers_atlas.gui.core.BlinkingTextureComponent;

public class BlinkingMarkerComponent extends BlinkingTextureComponent implements MarkerModal.IMarkerTypeSelectListener {
	public void onSelectMarkerType(MarkerTexture markerTexture) {
		setTexture(markerTexture.id(), AtlasScreen.MARKER_SIZE, AtlasScreen.MARKER_SIZE);
	}
}
