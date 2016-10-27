package jiyuiydi.infra.ui.opengl;

import java.awt.Component;
import java.awt.FontMetrics;
import java.awt.geom.Rectangle2D;

import javax.media.opengl.GLAutoDrawable;

import com.jogamp.opengl.util.awt.TextRenderer;

import jiyuiydi.infra.ui.opengl.BatchTextRenderer.TextRenderingClient;
import jiyuiydi.infra.ui.opengl.View.LayoutMode;

public class Face_Quantity implements Face, TextRenderingClient {

	View_Quantity v;

	public Face_Quantity(View_Quantity v) { this.v = v; }

	@Override
	public void doLayout(Editor context, LayoutMode mode) {
		final int fontIndex = mode == LayoutMode.METADATA ? context.fontIndex_smallText : context.fontIndex_defaultQuantity;
		final FontMetrics fm = context.batchTextRenderer.getMetrics(fontIndex);
		Rectangle2D bounds = fm.getStringBounds(v.prettyPrint, context.canvas.getGraphics());
		v.sizeTo(v.internalPadding_left + (int) bounds.getWidth(), fm.getHeight());

		if(v.metadata != null) {
			v.metadata.validateLayout(context, LayoutMode.METADATA);
			v.metadata.moveTo((int) v.goalX, (int) (v.goalY - v.metadata.goalH));
		}
	}

	@Override
	public int getBaseline(Editor context, LayoutMode mode) {
		final int fontIndex = mode == LayoutMode.METADATA ? context.fontIndex_smallText : context.fontIndex_defaultQuantity;
		return context.batchTextRenderer.getMetrics(fontIndex).getAscent();
	}

	@Override
	public void draw(GLAutoDrawable d, Editor context, LayoutMode mode) {
		v.drawBackground(d, context);
		final int fontIndex = mode == LayoutMode.METADATA ? context.fontIndex_smallText : context.fontIndex_defaultQuantity;
		context.batchTextRenderer.requestTextRendering(fontIndex, this);

		if(v.metadata != null) v.metadata.draw(d, context, LayoutMode.METADATA);
	}

	@Override
	public void drawText(GLAutoDrawable d, int fontIndex, TextRenderer tr, FontMetrics fm, Component c) {
		tr.draw(v.prettyPrint, (int) v.liveX + v.internalPadding_left, c.getHeight() - ((int) v.liveY + fm.getAscent()));
	}

}
