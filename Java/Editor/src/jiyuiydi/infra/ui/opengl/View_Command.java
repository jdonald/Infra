package jiyuiydi.infra.ui.opengl;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;

import jiyuiydi.infra.Box;
import jiyuiydi.infra.Int32;
import jiyuiydi.infra.Keyed;
import jiyuiydi.infra.Node;
import jiyuiydi.infra.Opcode;
import jiyuiydi.infra.UTF8;
import jiyuiydi.infra.ui.opengl.View.LayoutMode;
import jiyuiydi.util.Observer.UpdateMessage;

public class View_Command extends View_Keyed {

	public View_Command(Keyed m, ParentOfView parent) {
		super(m, parent);
	}

	@Override
	void doLayout(Editor context, LayoutMode mode) {
		super.doLayout(context, mode);
		if(views.size() < 2)
			sizeTo((int) (goalW + context.margin * 3f), (int) goalH);
	}

	@Override
	public void draw(GLAutoDrawable d, Editor context, LayoutMode mode) {
		final GL2 gl = d.getGL().getGL2();

		drawBackground(d, context);
		for(View v : views) v.draw(d, context, mode);

		float m = context.margin;
		float l = liveX, r = liveX + liveW, t = liveY + m, b = liveY + liveH - m;
		if(views.size() > 0) {
			l = views.get(0).getLiveMaxX() + m;
		}
		if(views.size() > 1) {
			View v2 = views.get(1);
			View vn = views.get(views.size()-1);
			l = v2.getLiveMinX() - context.itemSpacing/2;
			r = vn.getLiveMaxX() + context.itemSpacing/2;
			t = v2.getLiveMinY();
			b = v2.getLiveMaxY();
		}

		gl.glLineWidth(context.lineThickness);
		gl.glColor4f(0f, .3f, .7f, .5f);
		gl.glBegin(GL.GL_LINE_STRIP);
			gl.glVertex2f(l+m, t-m);
			gl.glVertex2f(l  , t  );
			gl.glVertex2f(l  , b  );
			gl.glVertex2f(l+m, b+m);
		gl.glEnd();
		gl.glBegin(GL.GL_LINE_STRIP);
			gl.glVertex2f(r-m, t-m);
			gl.glVertex2f(r  , t  );
			gl.glVertex2f(r  , b  );
			gl.glVertex2f(r-m, b+m);
		gl.glEnd();
	}

	@Override
	public void update(UpdateMessage msg, Observable subject) {
		if(subject == model) {
			if(msg instanceof UpdateMessage.Initialize) {
				Keyed sub = (Keyed) subject;
				for(int i = 0; i < sub.count(); ++i) {
					if(i == 0 && sub.get(i) instanceof Int32) {
						views.add(new View_UTF8(
								new UTF8(Opcode.values()[sub.get(i).asInt()].getName()),
								this
								));
						continue;
					}
					views.add(createView(sub.get(i), this));
					invalidateLayout();
				}
				return;
			}
		}
		super.update(msg, subject);
	}
	
}
