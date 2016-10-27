package jiyuiydi.infra.ui.opengl;

import java.awt.Component;
import java.awt.FontMetrics;
import java.awt.geom.Rectangle2D;

import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;

import com.jogamp.opengl.util.awt.TextRenderer;

import jiyuiydi.infra.*;

public class View_Symbol extends View {

	Symbol model;

	public View_Symbol(Symbol s, ParentOfView p) {
		super(new UTF8(s.getName()), p);
		model = s;
		model.addObserver(this);
		if(model.hasMetadata())
			metadata = createView(model.getMetadata(), this);
	}

	@Override public Symbol getModel() { return model; }

	@Override public void destroy() { model.removeObserver(this); super.destroy(); }

	@Override
	void doLayout(Editor context, LayoutMode mode) {
		super.doLayout(context, mode);

		final int fontIndex = mode == LayoutMode.METADATA ? context.fontIndex_smallText : context.fontIndex_defaultTextBoldItalic;
		final FontMetrics fm = context.batchTextRenderer.getMetrics(fontIndex);
		Rectangle2D bounds = fm.getStringBounds(model.getName(), context.canvas.getGraphics());
		sizeTo(internalPadding_left + (int) bounds.getWidth(), fm.getHeight());

		if(metadata != null) {
			metadata.validateLayout(context, LayoutMode.METADATA);
			metadata.moveTo((int) goalX, (int) (goalY - metadata.goalH));
		}
	}

	@Override
	int getBaseline(Editor context, LayoutMode mode) {
		final int fontIndex = mode == LayoutMode.METADATA ? context.fontIndex_smallText : context.fontIndex_defaultTextBoldItalic;
		return context.batchTextRenderer.getMetrics(fontIndex).getAscent();
	}

	@Override
	public void draw(GLAutoDrawable d, Editor context, LayoutMode mode) {
		drawBackground(d, context);
		if(isPatchResultMode()) {
			final GL2 gl = d.getGL().getGL2();
			gl.glColor4f(.7f, .3f, 0f, .1f);
			drawRect(gl, internalPadding_left + liveX, liveY, liveX+liveW, liveY+liveH, true);
		}

		final int fontIndex = mode == LayoutMode.METADATA ? context.fontIndex_smallText : context.fontIndex_defaultTextBoldItalic;
		context.batchTextRenderer.requestTextRendering(fontIndex, this);

		if(metadata != null) metadata.draw(d, context, LayoutMode.METADATA);
	}

	@Override
	public void drawText(GLAutoDrawable d, int fontIndex, TextRenderer tr, FontMetrics fm, Component c) {
		tr.draw(model.getName(), (int) liveX + internalPadding_left, c.getHeight() - ((int) liveY + fm.getAscent()));
	}

}
