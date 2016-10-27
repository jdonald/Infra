package jiyuiydi.infra.ui.opengl;

import jiyuiydi.infra.ui.Animator;
import jiyuiydi.util.Observer;

public interface ParentOfView {

	int count();
	View get(int index);
	int indexOf(View v);

	Animator getAnimator();
	void invalidateLayout();
	int getMaxX();

	void addObserver(Observer o);
	void removeObserver(Observer o);

	View replace(int index, View v);

}
