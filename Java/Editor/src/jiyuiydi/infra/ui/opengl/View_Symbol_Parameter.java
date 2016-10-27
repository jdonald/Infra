package jiyuiydi.infra.ui.opengl;

import javax.media.opengl.GLAutoDrawable;

import jiyuiydi.infra.Node;
import jiyuiydi.infra.Symbol;
import jiyuiydi.infra.ui.opengl.View.LayoutMode;

public class View_Symbol_Parameter extends View {

	Symbol.Parameter model;

	public View_Symbol_Parameter(Symbol.Parameter m, ParentOfView parent) {
		super(m, parent);
		model = m;
	}

	@Override public Symbol.Parameter getModel() { return model; }

	@Override
	int getBaseline(Editor context, LayoutMode mode) {
		return (int) (context.image_parameterField.getHeight() * .8f);
	}

	@Override
	void doLayout(Editor context, LayoutMode mode) {
		sizeTo(context.image_parameterField.getWidth(), context.image_parameterField.getHeight());
	}

	@Override
	public void draw(GLAutoDrawable d, Editor context, LayoutMode mode) {
		context.image_parameterField.draw(d, liveX, liveY);
	}

}
