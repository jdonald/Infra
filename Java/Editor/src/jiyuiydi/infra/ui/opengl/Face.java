package jiyuiydi.infra.ui.opengl;

import java.util.List;

import javax.media.opengl.GLAutoDrawable;

import jiyuiydi.infra.ui.opengl.View.LayoutMode;

public interface Face {

	void doLayout(Editor context, LayoutMode mode);

	int getBaseline(Editor context, LayoutMode mode);

	void draw(GLAutoDrawable d, Editor context, LayoutMode mode);

}
