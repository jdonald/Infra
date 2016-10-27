package jiyuiydi.infra.ui.opengl;

import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;

import jiyuiydi.infra.ui.opengl.View.LayoutMode;

public class Face_Summary implements Face {

	View v;
	View_Box vb;

	public Face_Summary(View v) {
		this.v = v;
		if(v instanceof View_Box) vb = (View_Box) v;
	}

	@Override
	public void doLayout(Editor context, LayoutMode mode) {
		if(vb != null) {
			int x = vb.getMinX(), y = vb.getMinY();
			for(View v : vb.views) {
				v.moveTo(x, y);
				x += context.margin;
			}
			vb.sizeTo(x - vb.getMinX(), y - vb.getMinY() + context.margin * 2);
		} else {
			
		}
	}

	@Override
	public int getBaseline(Editor context, LayoutMode mode) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void draw(GLAutoDrawable d, Editor context, LayoutMode mode) {
		// TODO Auto-generated method stub
		if(vb != null) {
			final GL2 gl = d.getGL().getGL2();
			gl.glColor3f(0f, 0f, 0f);
			gl.glBegin(GL2.GL_POINTS);
			for(View v : vb.views) {
				gl.glVertex2f(v.liveX, v.liveY);
			}
			gl.glEnd();
		} else {
			
		}
	}

}
