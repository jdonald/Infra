package jiyuiydi.infra.ui;

import java.awt.event.ActionEvent;
import java.util.Iterator;
import java.util.LinkedList;
import javax.swing.Timer;

import jiyuiydi.util.VoidVoid;

public class Animator {

	static public interface Animation { boolean animate(); }

	VoidVoid callback;

	int fps = 24;
	final Timer timer = new Timer(1000 / fps, this::frameTick);
	private boolean ticking;

	final LinkedList<Animation> actives = new LinkedList<>();

	// Animator //////////////////////////////////////////////////////////

	public Animator(VoidVoid fn) { callback = fn; }

	public void register(Animation a) {
		synchronized(actives) {
			if(actives.contains(a)) return;
			if(actives.isEmpty()) timer.start();
			actives.add(a);
		}
	}

	public void unregister(Animation a) {
		synchronized(actives) {
			actives.remove(a);
		}
	}

	void frameTick(ActionEvent e) {
		if(ticking) return;
		ticking = true;
		synchronized(actives) {
			for(Iterator<Animation> i = actives.iterator(); i.hasNext();)
				if(i.next().animate()) i.remove();
			if(actives.isEmpty()) timer.stop();
		}
		callback.call();
		ticking = false;
	}

	public void postRedisplay() { callback.call(); }

}
