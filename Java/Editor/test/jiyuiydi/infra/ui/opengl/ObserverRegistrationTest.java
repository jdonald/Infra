package jiyuiydi.infra.ui.opengl;

import static org.junit.Assert.*;

import jiyuiydi.infra.*;
import jiyuiydi.infra.ui.Animator;
import jiyuiydi.infra.ui.opengl.ParentOfView;
import jiyuiydi.infra.ui.opengl.View;
import jiyuiydi.util.Observer;

import org.junit.Test;

public class ObserverRegistrationTest {

	@Test
	public void testObserverCount() {
		Box r = new Box(new Box("a", "b"), "c", new Box("d"));

		Box ab = r.get(0).asBox();
		Node a = ab.get(0);
		Node b = ab.get(1);
		Node c = r.get(1);
		Node d = r.get(2).asBox().get(0);

		ParentOfView pov = new ParentOfView() {
			public int indexOf(View v) { return 0; }
			public int count() { return 0; }
			public View get(int index) { return null; }
			public Animator getAnimator() { return null; }
			public void invalidateLayout() {}
			public void addObserver(Observer o) {}
			public void removeObserver(Observer o) {}
			public int getMaxX() { return 0; }
			public View replace(int index, View v) { return null; }
		};

		View v = View.createView(r, pov);

		assertEquals(1, ab.countObservers());
		assertEquals(1, a.countObservers());
		assertEquals(1, b.countObservers());
		assertEquals(1, c.countObservers());
		assertEquals(1, d.countObservers());

		v.destroy();
		assertEquals(0, ab.countObservers());
		assertEquals(0, a.countObservers());
		assertEquals(0, b.countObservers());
		assertEquals(0, c.countObservers());
		assertEquals(0, d.countObservers());

		//v.create();
		//View.recycleView(v);
		//View oldV = v;
		v = View.createView(r, pov);
		//assertEquals(v, oldV);
		assertEquals(1, ab.countObservers());
		assertEquals(1, a.countObservers());
		assertEquals(1, b.countObservers());
		assertEquals(1, c.countObservers());
		assertEquals(1, d.countObservers());

		//b.notifyMoving();
		ab.remove(1); // remove b
		View.destroyRecycledViewsOf(b);
		assertEquals(1, ab.countObservers());
		assertEquals(1, a.countObservers());
		assertEquals(0, b.countObservers());
		assertEquals(1, c.countObservers());
		assertEquals(1, d.countObservers());

		ab.insert(0, b);
		assertEquals(1, ab.countObservers());
		assertEquals(1, a.countObservers());
		assertEquals(1, b.countObservers());
		assertEquals(1, c.countObservers());
		assertEquals(1, d.countObservers());
	}

}
