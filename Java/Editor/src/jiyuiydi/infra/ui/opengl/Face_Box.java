package jiyuiydi.infra.ui.opengl;

import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;

import jiyuiydi.infra.Metadata;
import jiyuiydi.infra.ui.opengl.View.LayoutMode;

public class Face_Box implements Face {

	static final float UNLOADED_DOTSPACING = 4f;

	final View_Box vb;
	int baseline;

	public Face_Box(View_Box vb) { this.vb = vb; }

	@Override
	public void doLayout(Editor context, LayoutMode mode) {
		int margin = mode == LayoutMode.METADATA ? 0 : context.margin;
		int startX = vb.internalPadding_left + margin;
		int x = startX, y = margin, maxX = x, maxY = y;

		if(vb.metadata != null) {
			vb.metadata.validateLayout(context, LayoutMode.METADATA);
			vb.metadata.moveTo(vb.getMinX(), vb.getMinY());
			y += vb.metadata.getHeight();
			maxY = y;
			vb.internalMargin_top = vb.metadata.getHeight();
		}

		int lineAscent = margin;
		int line = 1;

		int firstInRow = 0;
		for(int i = 0; i < vb.views.size(); ++i) {
			View v = vb.views.get(i);
			v.validateLayout(context, mode);
			lineAscent = Math.max(lineAscent, v.getBaseline(context, mode));
			maxX = Math.max(maxX, x + v.getWidth());
			maxY = Math.max(maxY, y + v.getHeight());
			x += v.getWidth() + context.itemSpacing;

			if(/*x > ? ||*/ vb.layoutVertical) {
				// Adjust recent line of views
				x = startX;
				for(int ii = firstInRow; ii <= i; ++ii) {
					v = vb.views.get(ii);
					int vAscent = v.getBaseline(context, mode);
					v.moveTo(vb.getMinX() + x, vb.getMinY() + y - vAscent + lineAscent);
					x += v.getWidth() + context.itemSpacing;
				}
				if(line == 1) baseline = lineAscent + y;

				x = startX;
				y = maxY + margin;
				firstInRow = i + 1;
				lineAscent = margin;
				++line;
			}
		}

		// Adjust recent line of views
		x = startX;
		for(int i = firstInRow; i < vb.views.size(); ++i) {
			View v = vb.views.get(i);
			int vAscent = v.getBaseline(context, mode);
			v.moveTo(vb.getMinX() + x, vb.getMinY() + y - vAscent + lineAscent);
			x += v.getWidth() + context.itemSpacing;
		}
		if(line == 1) baseline = lineAscent + y;

		if(vb.model.isUnloaded()) {
			float inc = UNLOADED_DOTSPACING;
			int count = vb.model.getNumUnloaded();
			if(count < 0) { count = 3; inc = context.margin; }
			float width = inc * (count);
			maxX = Math.max(maxX, (int) width); // cancel the margin that will be added
		}

		if(vb.metadata != null) {
			//vb.metadata.validateLayout(context, LayoutMode.METADATA);
			//vb.metadata.moveTo((int) vb.goalX, (int) (vb.goalY - vb.metadata.goalH));
			if(vb.metadata.getWidth() > maxX) {
				vb.internalMargin_right = vb.metadata.getWidth() - maxX;
				maxX = vb.metadata.getWidth();
			}
		}

		vb.sizeTo(maxX + margin, maxY + margin);
	}

	@Override
	public int getBaseline(Editor context, LayoutMode mode) { return baseline; }

	@Override
	public void draw(GLAutoDrawable d, Editor context, LayoutMode mode) {
		if(vb.parent instanceof View && !(vb.model instanceof Metadata))
			vb.drawBorder(d, context);

		if(vb.model.isUnloaded()) {
			final GL2 gl = d.getGL().getGL2();
			float inc = UNLOADED_DOTSPACING;
			float x = vb.liveX + inc, y = vb.liveY + vb.liveH - (vb.liveH-vb.internalMargin_top) * .5f;
			int count = vb.model.getNumUnloaded();
			if(count < 0) { count = 3; inc = context.margin; }

			gl.glColor4f(.7f, .3f, 0f, .6f);
			gl.glBegin(GL2.GL_POINTS);
			for(int i = 0; i < count; ++i) {
				gl.glVertex2f(x, y);
				x += inc;
			}
			gl.glEnd();
		} else {
			for(View v : vb.views)
				v.draw(d, context, mode);
		}

		if(vb.metadata != null) vb.metadata.draw(d, context, LayoutMode.METADATA);
	}

}
