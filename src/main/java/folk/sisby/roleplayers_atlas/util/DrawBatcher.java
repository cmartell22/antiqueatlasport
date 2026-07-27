package folk.sisby.roleplayers_atlas.util;

import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.texture.TextureSetup;
import net.minecraft.util.Identifier;
import org.joml.Matrix3x2f;
import org.joml.Matrix4f;

import java.lang.reflect.Method;

public class DrawBatcher implements AutoCloseable {

	protected final AtlasPainter painter;
	protected final Identifier texture;
	protected final Matrix4f matrix4f;
	protected final VertexConsumer vertexConsumer;
	protected final float textureWidth;
	protected final float textureHeight;
	protected final int light;

	// Gui mode: quads are accumulated and submitted as one render-state element.
	protected FloatArrayList guiQuads;
	protected IntArrayList guiColors;
	protected float guiMinX, guiMinY, guiMaxX, guiMaxY;

	public static boolean areWeShadersRightNow() {
		try {
			Class<?> apiClass = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
			Method instanceMethod = apiClass.getDeclaredMethod("getInstance");
			Method inUseMethod = apiClass.getDeclaredMethod("isShaderPackInUse");
			Object apiInstance = instanceMethod.invoke(null);
			return (boolean) inUseMethod.invoke(apiInstance);
		} catch (Exception e) {
			return false;
		}
	}

	public static void drawSingle(AtlasPainter painter, Identifier texture, int textureWidth, int textureHeight, int light, int x, int y, float z, int width, int height, int u, int v, int regionWidth, int regionHeight, int argb, boolean drawingTransparent) {
		try (DrawBatcher batcher = new DrawBatcher(painter, texture, textureWidth, textureHeight, light, drawingTransparent)) {
			batcher.add(x, y, z, width, height, u, v, regionWidth, regionHeight, argb);
		}
	}

	public DrawBatcher(AtlasPainter painter, Identifier texture, int textureWidth, int textureHeight, int light, boolean drawingTransparent) {
		this.painter = painter;
		this.texture = texture;
		if (painter.isGui()) {
			this.matrix4f = null;
			this.vertexConsumer = null;
		} else {
			VertexConsumerProvider vertexConsumers = painter.vertexConsumers();
			if (areWeShadersRightNow()) {
				if (drawingTransparent) {
					this.vertexConsumer = vertexConsumers.getBuffer(RenderLayer.getEntityNoOutline(texture));
				} else {
					this.vertexConsumer = vertexConsumers.getBuffer(RenderLayer.getEntitySolid(texture));
				}
			} else {
				this.vertexConsumer = vertexConsumers.getBuffer(RenderLayer.getText(texture));
			}
			this.matrix4f = painter.matrices().peek().getPositionMatrix();
		}
		this.textureWidth = textureWidth;
		this.textureHeight = textureHeight;
		this.light = light;
	}

	public void add(int x, int y, float z, int width, int height, int u, int v, int regionWidth, int regionHeight, int argb) {
		if (painter.isGui()) {
			if (guiQuads == null) {
				guiQuads = new FloatArrayList();
				guiColors = new IntArrayList();
				guiMinX = Float.MAX_VALUE;
				guiMinY = Float.MAX_VALUE;
				guiMaxX = -Float.MAX_VALUE;
				guiMaxY = -Float.MAX_VALUE;
			}
			float x1 = x, y1 = y, x2 = x + width, y2 = y + height;
			guiQuads.add(x1);
			guiQuads.add(y1);
			guiQuads.add(x2);
			guiQuads.add(y2);
			guiQuads.add((u + 0.0F) / textureWidth);
			guiQuads.add((v + 0.0F) / textureHeight);
			guiQuads.add((u + (float) regionWidth) / textureWidth);
			guiQuads.add((v + (float) regionHeight) / textureHeight);
			guiColors.add(argb);
			guiMinX = Math.min(guiMinX, Math.min(x1, x2));
			guiMinY = Math.min(guiMinY, Math.min(y1, y2));
			guiMaxX = Math.max(guiMaxX, Math.max(x1, x2));
			guiMaxY = Math.max(guiMaxY, Math.max(y1, y2));
			return;
		}
		this.innerAdd(x, x + width, y, y + height, z,
			(u + 0.0F) / textureWidth,
			(u + (float) regionWidth) / textureWidth,
			(v + 0.0F) / textureHeight,
			(v + (float) regionHeight) / textureHeight,
			argb
		);
	}

	protected void innerAdd(float x1, float x2, float y1, float y2, float z, float u1, float u2, float v1, float v2, int argb) {
		vertexConsumer.vertex(matrix4f, x1, y1, z).color(argb).texture(u1, v1).overlay(0).light(light).normal(0, 0, 0);
		vertexConsumer.vertex(matrix4f, x1, y2, z).color(argb).texture(u1, v2).overlay(0).light(light).normal(0, 0, 0);
		vertexConsumer.vertex(matrix4f, x2, y2, z).color(argb).texture(u2, v2).overlay(0).light(light).normal(0, 0, 0);
		vertexConsumer.vertex(matrix4f, x2, y1, z).color(argb).texture(u2, v1).overlay(0).light(light).normal(0, 0, 0);
	}

	@Override
	public void close() {
		if (painter.isGui() && guiQuads != null && !guiQuads.isEmpty()) {
			DrawContext context = painter.context();
			Matrix3x2f pose = new Matrix3x2f(context.getMatrices());
			ScreenRect scissor = context.scissorStack.peekLast();
			TextureSetup textureSetup = TextureSetup.withoutGlTexture(MinecraftClient.getInstance().getTextureManager().getTexture(texture).getGlTextureView());
			ScreenRect bounds = new ScreenRect((int) Math.floor(guiMinX), (int) Math.floor(guiMinY), (int) Math.ceil(guiMaxX - guiMinX), (int) Math.ceil(guiMaxY - guiMinY)).transformEachVertex(pose);
			if (scissor != null) bounds = bounds.intersection(scissor);
			if (bounds != null) {
				context.state.addSimpleElement(new BatchedGuiQuads(RenderPipelines.GUI_TEXTURED, textureSetup, pose, scissor, guiQuads.toFloatArray(), guiColors.toIntArray(), guiColors.size(), bounds));
			}
			guiQuads = null;
			guiColors = null;
		}
	}
}
