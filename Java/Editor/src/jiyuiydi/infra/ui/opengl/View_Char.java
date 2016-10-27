package jiyuiydi.infra.ui.opengl;

import java.awt.Component;
import java.awt.FontMetrics;
import java.awt.geom.Rectangle2D;

import javax.media.opengl.GLAutoDrawable;

import com.jogamp.opengl.util.awt.TextRenderer;

import jiyuiydi.infra.Node;

public class View_Char extends View {

	char model;

	public View_Char(char ch, ParentOfView parent) {
		super(null, parent);
		model = ch;
	}

	@Override public Node getModel() {
		return null;
	}

	@Override
	int getBaseline(Editor context, LayoutMode mode) {
		return context.batchTextRenderer.getMetrics(context.fontIndex_defaultMonospaced).getAscent();
	}

	@Override
	void doLayout(Editor context, LayoutMode mode) {
		final FontMetrics fm = context.batchTextRenderer.getMetrics(context.fontIndex_defaultMonospaced);
		int width;
		if(model == '\t')
			width = (int) (fm.getHeight() * 1.5);
		else {
			String s = String.valueOf(model);
			Rectangle2D bounds = fm.getStringBounds(s, context.canvas.getGraphics());
			width = Math.max(context.margin, (int) bounds.getWidth());
		}
		sizeTo(width, fm.getHeight());
	}

	@Override
	public void draw(GLAutoDrawable d, Editor context, LayoutMode mode) {
		drawBackground(d, context);
		int fontIndex = context.fontIndex_defaultMonospaced;
		context.batchTextRenderer.requestTextRendering(fontIndex, this);
	}

	@Override
	public void drawText(GLAutoDrawable d, int fontIndex, TextRenderer tr, FontMetrics fm, Component c) {
		tr.draw(String.valueOf(model), (int) liveX, c.getHeight() - (int) (liveY + fm.getAscent()));
	}

}
