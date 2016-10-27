package jiyuiydi.infra.ui.opengl;

import java.awt.Component;
import java.awt.FontMetrics;
import java.awt.geom.Rectangle2D;

import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;

import com.jogamp.opengl.util.awt.TextRenderer;

import jiyuiydi.infra.UTF8;
import jiyuiydi.infra.ui.opengl.BatchTextRenderer.TextRenderingClient;
import jiyuiydi.infra.ui.opengl.View.LayoutMode;

public class Face_String implements Face, TextRenderingClient {

	static final String emptyQuote = "\u201C\u201D";

	View_UTF8 v;

	public Face_String(View_UTF8 v) { this.v = v; }

	@Override
	public void doLayout(Editor context, LayoutMode mode) {
		if(v.isSubstructureOpen) {
			int maxBaseline = 0, maxHeight = 0;
			for(View_Char vc : v.expandedChars) {
				vc.validateLayout(context, mode);
				maxBaseline = Math.max(maxBaseline, vc.getBaseline(context, mode));
				maxHeight = Math.max(maxHeight, vc.getHeight());
			}
			int x = v.internalPadding_left;
			int y = v.getMinY();
			int maxX = x;
			for(View_Char vc : v.expandedChars) {
				vc.moveTo(v.getMinX() + x, y);
				if(vc.model == '\n') {
					x = v.internalPadding_left;
					y += maxHeight;
				} else {
					x += vc.getWidth();
					maxX = Math.max(maxX, x);
				}
			}
			v.sizeTo(maxX, y - v.getMinY() + maxHeight);
		} else {
			String s = v.text.get().isEmpty() ? emptyQuote : v.text.get();
			final FontMetrics fm = context.batchTextRenderer.getMetrics(mode == LayoutMode.METADATA ? context.fontIndex_smallText : context.fontIndex_defaultText);
			Rectangle2D bounds = fm.getStringBounds(s, context.canvas.getGraphics());
			int width = Math.max(context.margin, (int) bounds.getWidth());
			v.sizeTo(v.internalPadding_left + width, fm.getHeight());
		}

		if(v.metadata != null) {
			v.metadata.validateLayout(context, LayoutMode.METADATA);
			v.metadata.moveTo((int) v.goalX, (int) (v.goalY - v.metadata.goalH));
		}
	}

	@Override
	public int getBaseline(Editor context, LayoutMode mode) {
		if(v.isSubstructureOpen && !v.expandedChars.isEmpty())
			return v.expandedChars.get(0).getBaseline(context, mode);
		final int fontIndex = v.text.get().isEmpty()
				? mode == LayoutMode.METADATA ? context.fontIndex_smallSpecialChars : context.fontIndex_specialChars
				: mode == LayoutMode.METADATA ? context.fontIndex_smallText         : context.fontIndex_defaultText;
		return context.batchTextRenderer.getMetrics(fontIndex).getAscent();
	}

	@Override
	public void draw(GLAutoDrawable d, Editor context, LayoutMode mode) {
		v.drawBackground(d, context);
		if(v.isSubstructureOpen) {
			for(View_Char vc : v.expandedChars)
				vc.draw(d, context, mode);
		} else {
			final int fontIndex = v.text.get().isEmpty()
					? mode == LayoutMode.METADATA ? context.fontIndex_smallSpecialChars : context.fontIndex_specialChars
					: mode == LayoutMode.METADATA ? context.fontIndex_smallText         : context.fontIndex_defaultText;
			context.batchTextRenderer.requestTextRendering(fontIndex, this);

			String s = ((UTF8)v.getModel()).get();
			if(s.contains(" ")) {// Draw spaces
				FontMetrics fm = context.batchTextRenderer.getMetrics(fontIndex);
				int spaceWidth = fm.charWidth(' ');
				float halfSW = spaceWidth * .5f;
				float fracSW = spaceWidth * .13f;
				final GL2 gl = d.getGL().getGL2();
				float x = v.liveX, y = v.liveY + fm.getAscent()*.6f;
				String[] words = s.split(" ");
				gl.glColor4f(0f, 0f, 0f, .3f);
				gl.glLineWidth(1f);
				for(int i = 0; i < words.length - 1; ++i) {
					x += fm.stringWidth(words[i]);
					gl.glBegin(GL2.GL_LINES);
						gl.glVertex2f(x+halfSW-fracSW, y);
						gl.glVertex2f(x+halfSW+fracSW, y);
					gl.glEnd();
					x += spaceWidth;
				}
			}
		}

		if(v.metadata != null) v.metadata.draw(d, context, LayoutMode.METADATA);
	}

	@Override
	public void drawText(GLAutoDrawable d, int fontIndex, TextRenderer tr, FontMetrics fm, Component c) {
		//if(parent instanceof View) {
			//View p = (View) parent;
			//final GL2 gl = d.getGL().getGL2();
			//gl.glScissor((int) p.liveX, c.getHeight() - (int) (p.liveY+p.liveH), (int) p.liveW, (int) p.liveH);
			//gl.glEnable(GL.GL_SCISSOR_TEST);
			//tr.draw(text.get(), (int) liveX, c.getHeight() - ((int) liveY + fm.getAscent()));
			//tr.draw3D(model.get(), (int) liveX, (int) liveY + fm.getAscent(), 0, 1f);
			//gl.glDisable(GL.GL_SCISSOR_TEST);
			//return;
		//}

		String s = v.text.get().isEmpty() ? emptyQuote : v.text.get();
		tr.draw(s, (int) v.liveX + v.internalPadding_left, c.getHeight() - ((int) v.liveY + fm.getAscent()));
		//tr.draw3D(model.get(), (int) liveX, (int) liveY + fm.getAscent(), 0, 1f);
	}

}
