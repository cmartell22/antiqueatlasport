package folk.sisby.roleplayers_atlas.util;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.TextureSetup;
import org.joml.Matrix3x2f;

/**
 * A single gui render-state element carrying many textured quads: thousands of
 * map subtiles become one draw instead of one gui element each, which is what
 * keeps close zoom levels smooth.
 */
public final class BatchedGuiQuads implements SimpleGuiElementRenderState {
	private final RenderPipeline pipeline;
	private final TextureSetup textureSetup;
	private final Matrix3x2f pose;
	private final ScreenRect scissorArea;
	/** Packed per quad: x1, y1, x2, y2, u1, v1, u2, v2. */
	private final float[] quads;
	private final int[] colors;
	private final int count;
	private final ScreenRect bounds;

	public BatchedGuiQuads(RenderPipeline pipeline, TextureSetup textureSetup, Matrix3x2f pose, ScreenRect scissorArea, float[] quads, int[] colors, int count, ScreenRect bounds) {
		this.pipeline = pipeline;
		this.textureSetup = textureSetup;
		this.pose = pose;
		this.scissorArea = scissorArea;
		this.quads = quads;
		this.colors = colors;
		this.count = count;
		this.bounds = bounds;
	}

	@Override
	public void setupVertices(VertexConsumer vertices, float depth) {
		for (int i = 0; i < count; i++) {
			int q = i * 8;
			float x1 = quads[q], y1 = quads[q + 1], x2 = quads[q + 2], y2 = quads[q + 3];
			float u1 = quads[q + 4], v1 = quads[q + 5], u2 = quads[q + 6], v2 = quads[q + 7];
			int color = colors[i];
			vertices.vertex(pose, x1, y1, depth).texture(u1, v1).color(color);
			vertices.vertex(pose, x1, y2, depth).texture(u1, v2).color(color);
			vertices.vertex(pose, x2, y2, depth).texture(u2, v2).color(color);
			vertices.vertex(pose, x2, y1, depth).texture(u2, v1).color(color);
		}
	}

	@Override
	public RenderPipeline pipeline() {
		return pipeline;
	}

	@Override
	public TextureSetup textureSetup() {
		return textureSetup;
	}

	@Override
	public ScreenRect scissorArea() {
		return scissorArea;
	}

	@Override
	public ScreenRect bounds() {
		return bounds;
	}
}
