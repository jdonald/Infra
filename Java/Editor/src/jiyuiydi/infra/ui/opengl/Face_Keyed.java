package jiyuiydi.infra.ui.opengl;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;

import jiyuiydi.infra.ui.opengl.View.LayoutMode;

public class Face_Keyed extends Face_Box {

	public Face_Keyed(View_Keyed v) { super(v); }

	@Override
	public void draw(GLAutoDrawable d, Editor context, LayoutMode mode) {
		final GL2 gl = d.getGL().getGL2();

//		if(vb.views.size() > 2) { // highlight excess elements in red as warning
//			gl.glColor4f(203/255f, 0/255f, 0/255f, .7f);
//			vb.drawRect(gl, vb.views.get(2).liveX - context.itemSpacing/2, vb.liveY, vb.liveX+vb.liveW, vb.liveY+vb.liveH, true);
//		}
		//super.draw(d, context);

		vb.drawBackground(d, context);

		float scaleFactor = 1f;
		if(mode == LayoutMode.METADATA) scaleFactor = .5f;

		if(vb.views.size() > 2) {
			//vb.drawBorder(d, context);

			// Draw commas
			for(int i = 1; i < vb.views.size() - 1; ++i) {
				View v = vb.views.get(i);
				float x = v.liveX + v.liveW, y = v.liveY + v.getBaseline(context, mode) * .95f;
				gl.glPointSize(context.lineThickness * 2f * scaleFactor);
				gl.glColor4f(0f, .3f, .7f, .5f);
				gl.glBegin(GL.GL_LINES);
					gl.glVertex2f(x + 8 * scaleFactor, y);
					gl.glVertex2f(x + 4 * scaleFactor, y + 8 * scaleFactor);
					gl.glVertex2f(x + 6 * scaleFactor, y);
					gl.glVertex2f(x + 4 * scaleFactor, y + 8 * scaleFactor);
				gl.glEnd();
			}
		}

//		if(views.size() > 0) { // highlight key
//			View v0 = views.get(0);
//			gl.glColor4f(255/255f, 255/255f, 0/255f, .3f);
//			drawRect(gl, v0.getLiveMinX(), v0.getLiveMinY(), v0.getLiveMaxX(), v0.getLiveMaxY(), true);
//		}

		for(View v : vb.views) v.draw(d, context, mode);

		if(vb.views.size() > 1) { // Draw colon
			View v0 = vb.views.get(0);
			View v1 = vb.views.get(1);
			float l = v0.getLiveMaxX();
			float r = v1.getLiveMinX();
			float xc = (l + r) * .5f;
			float yc = v0.liveY + v0.liveH * .5f;

//			gl.glLineWidth(context.lineThickness);
//			gl.glColor4f(0f, .3f, .7f, .5f);
//			gl.glBegin(GL.GL_LINE_STRIP);
//				gl.glVertex2f(getLiveMinX(), y);
//				gl.glVertex2f(l  , y);
//				gl.glVertex2f(c  , liveY + context.itemSpacing  );
//				gl.glVertex2f(r, liveY);
//				gl.glVertex2f(getLiveMaxX(), liveY);
//			gl.glEnd();

			gl.glPointSize(context.lineThickness * 2f * scaleFactor);
			gl.glColor4f(0f, .3f, .7f, .5f);
			gl.glBegin(GL.GL_POINTS);
				gl.glVertex2f(xc , yc - v0.liveH * .10f);
				gl.glVertex2f(xc , yc + v0.liveH * .10f);
			gl.glEnd();
		}

		if(vb.metadata != null) vb.metadata.draw(d, context, LayoutMode.METADATA);
	}

	@Override
	public void doLayout(Editor context, LayoutMode mode) {
		// TODO Auto-generated method stub
		super.doLayout(context, mode);

		if(vb.layoutVertical && vb.views.size() > 2) {
			View k = vb.views.get(0);
			View v1 = vb.views.get(1);

			int dx = (int) (k.goalW + context.itemSpacing);
			int dy = (int) (k.goalY + k.getBaseline(context, mode) - (v1.goalY + v1.getBaseline(context, mode)));

			for(int i = 1; i < vb.views.size(); ++i)
				vb.views.get(i).moveBy(dx, dy);
			vb.sizeTo(vb.getWidth() + dx, vb.getHeight() + dy);
		}
	}

}
