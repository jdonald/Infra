package jiyuiydi.infra.ui.opengl;

import java.util.ArrayList;
import java.util.List;

import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;

import jiyuiydi.infra.*;
import jiyuiydi.infra.ui.opengl.View.LayoutMode;

public class View_Patch extends View_Box {

	// instance //////////////////////////////////////////////////////////

	Patch model;

	int partialEval;
	ArrayList<View> allViews = new ArrayList<>();
	View partialResultView;

	// View_Patch ////////////////////////////////////////////////////////

	public View_Patch(Patch m, ParentOfView parent) {
		super(m, parent);
		model = m;
		if(!(model instanceof LibraryPatch))
			layoutVertical = true;
		allViews.addAll(views);
	}

	public void setPartialEval(int breakPoint) {
		while(breakPoint < partialEval) {
			--partialEval;
			views.add(0, allViews.get(partialEval));
		}
		while(breakPoint > partialEval) {
			views.remove(0);
			++partialEval;
		}

		if(partialEval == 0) {
			partialResultView.destroy();
			partialResultView = null;
		} else {
//			Node partialOutput = getMomentOfTree().evaluatePartially(new int[]{partialEval});
//	
//			ArrayList<Node> models = new ArrayList<>();
//			if(partialResultView != null)
//				recycleViewTree(partialResultView, models);
//			partialResultView = createView(partialOutput, parent);
//			for(Node n : models)
//				destroyRecycledViewsOf(n);
		}

		invalidateLayout();
	}

	private void recycleViewTree(View v, List<Node> models) {
		models.add(v.getModel());
		recycleView(v);
		for(int i = 0; i < v.count(); ++i)
			recycleViewTree(v.get(i), models);
	}

	// View //////////////////////////////////////////////////////////////

	@Override
	int getBaseline(Editor context, LayoutMode mode) {
		if(partialResultView != null)
			return partialResultView.getBaseline(context, mode) + context.margin;
		return super.getBaseline(context, mode);
	}

	@Override
	void moveBy(int dx, int dy) {
		if(partialResultView != null)
			partialResultView.moveBy(dx, dy);
		super.moveBy(dx, dy);
	}

	@Override
	void doLayout(Editor context, LayoutMode mode) {
		super.doLayout(context, mode);

		if(partialEval > 0) {
			partialResultView.validateLayout(context, mode);
			partialResultView.moveTo(getMinX() + context.margin, getMinY() + context.margin);
			int topMargin = partialResultView.getHeight() + context.margin;
			int width = partialResultView.getWidth() + context.margin * 2;
			for(View v : views)
				v.moveBy(0, topMargin);
			sizeTo(Math.max(width, getWidth()), getHeight() + topMargin);
		}
	}

	@Override
	public void draw(GLAutoDrawable d, Editor context, LayoutMode mode) {
		if(model instanceof LibraryPatch) {
			GL2 gl = d.getGL().getGL2();
			gl.glColor3f(1f, 1f, 223/255f);
			drawRect(gl, liveX, liveY, liveX+liveW, liveY+liveH, true);
		}
		
		if(partialEval > 0) {
			int topMargin = partialResultView.getHeight() + context.margin;
			final GL2 gl = d.getGL().getGL2();
			gl.glColor4f(0f, 1f, .2f, .5f);
			drawRect(gl, liveX, liveY, getLiveMaxX(), liveY + topMargin, true);
//			for(int i = 0; i < partialEval && i < views.size(); ++i) {
//				View v = views.get(i);
//				drawRect(gl, v.getLiveMinX(), v.getLiveMinY(), v.getLiveMaxX(), v.getLiveMaxY(), true);
//			}
			partialResultView.draw(d, context, mode);
		}

		super.draw(d, context, mode);
	}

	@Override
	void drawBorder(GLAutoDrawable d, Editor context) {
		GL2 gl = d.getGL().getGL2();

		gl.glEnable(GL2.GL_LINE_STIPPLE);
		gl.glLineStipple(1, (short) 0xCCCC);
		super.drawBorder(d, context);
		gl.glDisable(GL2.GL_LINE_STIPPLE);
	}

	@Override
	public void update(UpdateMessage msg, Observable subject) {
		if(subject == model && partialEval > 0) {
			if(msg instanceof UpdateMessage.Inserted) {
				setPartialEval(0);
				super.update(msg, subject);
				setPartialEval(((UpdateMessage.Inserted<Node>) msg).index);
				return;
			}
			if(msg instanceof UpdateMessage.Removed) {
				setPartialEval(0);
				super.update(msg, subject);
				setPartialEval(((UpdateMessage.Removed<Node>) msg).index);
				return;
			}
		}

		super.update(msg, subject);
	}

}
