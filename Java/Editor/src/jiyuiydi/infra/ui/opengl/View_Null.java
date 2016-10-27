package jiyuiydi.infra.ui.opengl;

import javax.media.opengl.GLAutoDrawable;

import jiyuiydi.infra.Node;
import jiyuiydi.infra.Symbol;
import jiyuiydi.infra.UTF8;
import jiyuiydi.infra.ui.opengl.View.LayoutMode;

public class View_Null extends View {

	final Symbol.Null model;

	public View_Null(Symbol.Null sym, ParentOfView parent) {
		super(sym, parent);
		model = sym;
	}

	@Override
	public Node getModel() { return model; }

	@Override
	void doLayout(Editor context, LayoutMode mode) {
		super.doLayout(context, mode);

		if(metadata != null) {
			metadata.validateLayout(context, LayoutMode.METADATA);
			metadata.moveTo((int) goalX, (int) (goalY - metadata.goalH));
		}
	}

	@Override
	public void draw(GLAutoDrawable d, Editor context, LayoutMode mode) {
		drawBackground(d, context);
		context.batchTextRenderer.requestTextRendering(context.fontIndex_specialChars, this);

		if(metadata != null) metadata.draw(d, context, LayoutMode.METADATA);
	}

}
