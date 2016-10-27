package jiyuiydi.infra.ui.opengl;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.util.ArrayList;

import javax.media.opengl.GLAutoDrawable;

import com.jogamp.opengl.util.awt.TextRenderer;

public class BatchTextRenderer {

	static public interface TextRenderingClient {
		void drawText(GLAutoDrawable d, int fontIndex, TextRenderer tr, FontMetrics fm, Component c);
	}

	static private class RenderSet {
		Font font;
		TextRenderer renderer;
		Color color = Color.black;
		final ArrayList<TextRenderingClient> queue = new ArrayList<>();

		RenderSet(Font f) { setFont(f); }

		void setFont(Font f) {
			font = f;
			renderer = new TextRenderer(f, true, false);
		}
	}

	// instance //////////////////////////////////////////////////////////

	final Graphics graphics;
	final ArrayList<RenderSet> sets = new ArrayList<>();

	public BatchTextRenderer(Graphics g) { graphics = g; }

	public int registerFont(Font f) {
		int index = sets.indexOf(f);
		if(index > -1) return index;
		sets.add(new RenderSet(f));
		return sets.size() - 1;
	}

	public void scaleFonts(float scaleFactor) {
		for(RenderSet rs : sets) {
			rs.setFont(rs.font.deriveFont(rs.font.getSize2D() * scaleFactor));
		}
	}

	public FontMetrics getMetrics(int fontIndex) { return graphics.getFontMetrics(sets.get(fontIndex).font); }

	public void requestTextRendering(int fontIndex, TextRenderingClient rr) {
		sets.get(fontIndex).queue.add(rr);
	}

	public void render(GLAutoDrawable d, Component c) {
		int fontIndex = 0;
		for(RenderSet s : sets) {
			s.renderer.setColor(s.color);
			s.renderer.beginRendering(c.getWidth(), c.getHeight());
			//s.renderer.begin3DRendering();
			final FontMetrics fm = c.getFontMetrics(s.font);
			for(TextRenderingClient rr : s.queue)
				rr.drawText(d, fontIndex, s.renderer, fm, c);
			s.renderer.endRendering();
			//s.renderer.end3DRendering();
			s.queue.clear();
			++fontIndex;
		}
	}
	
}
