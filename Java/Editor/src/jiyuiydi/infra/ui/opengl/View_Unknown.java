package jiyuiydi.infra.ui.opengl;

import java.awt.Component;
import java.awt.FontMetrics;
import java.awt.geom.Rectangle2D;

import javax.media.opengl.GLAutoDrawable;

import com.jogamp.opengl.util.awt.TextRenderer;

import jiyuiydi.infra.*;

public class View_Unknown extends View {

	final Node model;
	String s;

	public View_Unknown(Node n, ParentOfView parent) {
		super(n, parent);
		model = n;
		s = model.toString();
	}

	@Override
	public Node getModel() { return model; }

	@Override
	void doLayout(Editor context, LayoutMode mode) {
		super.doLayout(context, mode);

		final FontMetrics fm = context.batchTextRenderer.getMetrics(mode == LayoutMode.METADATA ? context.fontIndex_smallText : context.fontIndex_defaultText);
		Rectangle2D bounds = fm.getStringBounds(s, context.canvas.getGraphics());
		int width = Math.max(context.margin, (int) bounds.getWidth());
		sizeTo(internalPadding_left + width, fm.getHeight());

		if(metadata != null) {
			metadata.validateLayout(context, LayoutMode.METADATA);
			metadata.moveTo((int) goalX, (int) (goalY - metadata.goalH));
		}
	}

	@Override
	int getBaseline(Editor context, LayoutMode mode) {
		final int fontIndex = mode == LayoutMode.METADATA ? context.fontIndex_smallText : context.fontIndex_defaultText;
		return context.batchTextRenderer.getMetrics(fontIndex).getAscent();
	}

	@Override
	public void draw(GLAutoDrawable d, Editor context, LayoutMode mode) {
		drawBackground(d, context);

		final int fontIndex = mode == LayoutMode.METADATA ? context.fontIndex_smallText : context.fontIndex_defaultText;
		context.batchTextRenderer.requestTextRendering(fontIndex, this);

		if(metadata != null) metadata.draw(d, context, LayoutMode.METADATA);
	}

	@Override
	public void drawText(GLAutoDrawable d, int fontIndex, TextRenderer tr, FontMetrics fm, Component c) {
		tr.draw(s, (int) liveX + internalPadding_left, c.getHeight() - ((int) liveY + fm.getAscent()));
	}

}
