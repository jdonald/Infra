package jiyuiydi.infra.ui.opengl;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;

import jiyuiydi.infra.Box;
import jiyuiydi.infra.Keyed;
import jiyuiydi.infra.Node;
import jiyuiydi.infra.ui.opengl.View.LayoutMode;

public class View_Keyed extends View_Box {

	public View_Keyed(Keyed n, ParentOfView parent) {
		super(n, parent);
		addFace(new Face_Keyed(this), true);
	}

}
