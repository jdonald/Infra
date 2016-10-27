package jiyuiydi.infra.ui.opengl;

import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;

import jiyuiydi.infra.*;

public class View_Mutation extends View {

	static final int buttonWidth = 100, buttonHeight = 25;

	private MutationRequest model;
	private boolean depressed;

	public View_Mutation(MutationRequest m, ParentOfView parent) {
		super(m, parent);
		model = m;
	}

	public void setDepressed(boolean d) { depressed = d; }

	public void activate() {
		// Perform the action from our parent so we don't capture it as an edit to us as a Patch result
		if(parent instanceof EditActionCaptureContext)
			((EditActionCaptureContext) parent).performEdit(model.getEditAction());
	}

	@Override public MutationRequest getModel() { return model; }

	@Override
	void doLayout(Editor context, LayoutMode mode) {
		super.doLayout(context, mode);
		sizeTo(buttonWidth, buttonHeight);
	}

	@Override
	int getBaseline(Editor context, LayoutMode mode) {
		return buttonHeight;
	}

	@Override
	public void draw(GLAutoDrawable d, Editor context, LayoutMode mode) {
		final GL2 gl = d.getGL().getGL2();

		float left = liveX;
		float right = liveX + buttonWidth;
		float top = liveY;
		float bottom = liveY + buttonHeight;

		// Gradient
		gl.glBegin(GL2.GL_QUADS);
			if(!depressed) gl.glColor4f(.9f, .9f, .9f, 1f); // light on top
			else gl.glColor4f(.7f, .7f, .7f, 1f); // dark on top

			gl.glVertex2f(left +.5f, top +.5f);
			gl.glVertex2f(right +.5f, top +.5f);

			if(!depressed) gl.glColor4f(.7f, .7f, .7f, 1f); // dark on bottom
			else gl.glColor4f(.9f, .9f, .9f, 1f); // light on bottom

			gl.glVertex2f(right +.5f, bottom +.5f);
			gl.glVertex2f(left +.5f, bottom +.5f);
		gl.glEnd();

		// Dark Outline
		gl.glBegin(GL2.GL_LINE_STRIP);
			if(!depressed) gl.glColor4f(.6f, .6f, .6f, .7f);
			else gl.glColor4f(.2f, .2f, .2f, .7f); // even darker (in shade)
			gl.glVertex2f(left +.5f, bottom  +.5f);
			gl.glVertex2f(left +.5f, top +.5f);
			gl.glVertex2f(right +.5f, top +.5f);
			if(!depressed) gl.glColor4f(.2f, .2f, .2f, .7f); // even darker (in shade)
			else gl.glColor4f(.8f, .8f, .8f, 1f); // bright
			gl.glVertex2f(right +.5f, bottom +.5f);
			gl.glVertex2f(left +.5f, bottom +.5f);
		gl.glEnd();

		if(!depressed) {
			gl.glColor4f(.9f, .9f, .9f, .6f); // bright inner border
			drawRect(gl, left+1f, top+1f, right-1f, bottom-1f, false);
		}

		if(highlight) {
			gl.glColor4f(203/255f, 226/255f, 253/255f, .6f);
			drawRect(gl, left, top, right, bottom, true);
		} else if(hoverHighlight) {
			gl.glColor4f(203/255f, 226/255f, 253/255f, .3f);
			drawRect(gl, left+1f, top+1f, right-1f, bottom-1f, true);
		}
	}

}
