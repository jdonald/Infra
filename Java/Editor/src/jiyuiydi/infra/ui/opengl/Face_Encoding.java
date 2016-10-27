package jiyuiydi.infra.ui.opengl;

import java.awt.Component;
import java.awt.FontMetrics;
import java.awt.geom.Rectangle2D;

import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;

import com.jogamp.opengl.util.awt.TextRenderer;

import jiyuiydi.infra.*;
import jiyuiydi.infra.ui.opengl.BatchTextRenderer.TextRenderingClient;
import jiyuiydi.infra.ui.opengl.View.LayoutMode;
import jiyuiydi.util.BufferedBuffer;
import jiyuiydi.util.Utilities;

public class Face_Encoding implements Face, TextRenderingClient {

	View v;
	String hex = "";
	Header h;

	public Face_Encoding(View v) { this.v = v; }

	@Override
	public void doLayout(Editor context, LayoutMode mode) {
		BufferedBuffer bb = InfraEncoding.encoded(v.getModel());
		long byteCount = bb.remaining();
		StringBuilder sb = new StringBuilder();
		for(long i = 0; i < byteCount; ++i) {
			final int b = bb.read1unsigned();
			sb.append(Utilities.byteToHex(b));
		}
		hex = sb.toString();

		bb.position(0);
		h = Header.decode(bb);

		final FontMetrics fm = context.batchTextRenderer.getMetrics(context.fontIndex_defaultMonospaced);
		Rectangle2D bounds = fm.getStringBounds(hex, context.canvas.getGraphics());
		int width = Math.max(context.margin, (int) bounds.getWidth());
		v.sizeTo(v.internalPadding_left + width, fm.getHeight());
	}

	@Override
	public int getBaseline(Editor context, LayoutMode mode) {
		return context.batchTextRenderer.getMetrics(context.fontIndex_defaultMonospaced).getAscent();
	}

	@Override
	public void draw(GLAutoDrawable d, Editor context, LayoutMode mode) {
		context.batchTextRenderer.requestTextRendering(context.fontIndex_defaultMonospaced, this);
		final GL2 gl = d.getGL().getGL2();
		float bW = v.getWidth() / (float)hex.length() * 2f;

		// Highlight header byte
		gl.glColor4f(0f, .6f, .3f, .2f);
		View.drawRect(gl, v.getLiveMinX(), v.getLiveMinY(), v.getLiveMinX() + bW, v.getLiveMaxY(), true);

		long b = h.getHeaderByteLength();
		if(b > 1) {
			gl.glColor4f(.6f, 0f, .3f, .2f);
			View.drawRect(gl, v.getLiveMinX() + bW, v.getLiveMinY(), v.getLiveMinX() + b*bW, v.getLiveMaxY(), true);
		}

		gl.glColor4f(0f, 0f, 0f, .05f);
		for(b += 1; b < h.getSegmentByteLength(); b += 2)
			View.drawRect(gl, v.getLiveMinX() + b*bW, v.getLiveMinY(), v.getLiveMinX() + b*bW + bW, v.getLiveMaxY(), true);

	}

	@Override
	public void drawText(GLAutoDrawable d, int fontIndex, TextRenderer tr, FontMetrics fm, Component c) {
		tr.draw(hex, (int) v.liveX + v.internalPadding_left, c.getHeight() - ((int) v.liveY + fm.getAscent()));
	}

}
