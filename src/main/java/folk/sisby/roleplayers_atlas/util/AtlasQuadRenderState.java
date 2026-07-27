package folk.sisby.roleplayers_atlas.util;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.TextureSetup;
import org.joml.Matrix3x2f;
import org.joml.Vector2f;

/**
 * A flat-coloured quad with free corners. The GUI's own fills are rectangles,
 * so a strip of them tears open wherever the shape it follows turns; this state
 * lets the corners be shared between neighbouring strips instead.
 */
public record AtlasQuadRenderState(Matrix3x2f pose, float[] xs, float[] ys, int color, ScreenRect bounds) implements SimpleGuiElementRenderState {
	public static AtlasQuadRenderState of(Matrix3x2f pose, float[] xs, float[] ys, int color) {
		float minX = Float.MAX_VALUE;
		float minY = Float.MAX_VALUE;
		float maxX = -Float.MAX_VALUE;
		float maxY = -Float.MAX_VALUE;
		Vector2f corner = new Vector2f();
		for (int i = 0; i < xs.length; i++) {
			pose.transformPosition(corner.set(xs[i], ys[i]));
			minX = Math.min(minX, corner.x);
			minY = Math.min(minY, corner.y);
			maxX = Math.max(maxX, corner.x);
			maxY = Math.max(maxY, corner.y);
		}
		int x = (int) Math.floor(minX);
		int y = (int) Math.floor(minY);
		ScreenRect bounds = new ScreenRect(x, y, (int) Math.ceil(maxX) - x + 1, (int) Math.ceil(maxY) - y + 1);
		return new AtlasQuadRenderState(pose, xs, ys, color, bounds);
	}

	@Override
	public void setupVertices(VertexConsumer vertices, float depth) {
		for (int i = 0; i < 4; i++) {
			vertices.vertex(this.pose, this.xs[i], this.ys[i], depth).color(this.color);
		}
	}

	@Override
	public RenderPipeline pipeline() {
		return RenderPipelines.GUI;
	}

	@Override
	public TextureSetup textureSetup() {
		return TextureSetup.empty();
	}

	@Override
	public ScreenRect scissorArea() {
		return null;
	}
}
