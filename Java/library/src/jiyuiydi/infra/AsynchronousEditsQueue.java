package jiyuiydi.infra;

import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;

import jiyuiydi.util.Observer;

public class AsynchronousEditsQueue extends Observer.Observable {

	static public final AsynchronousEditsQueue instance = new AsynchronousEditsQueue();

	private final ConcurrentLinkedQueue<EditAction> queue = new ConcurrentLinkedQueue<>();

	synchronized public void add(EditAction e) {
		queue.add(e);
		notifyObservers();
	}

	synchronized public void applyEdits(Node rootFilter) {
		for(Iterator<EditAction> i = queue.iterator(); i.hasNext();) {
			EditAction ea = i.next();
			if(true) { // TODO: filter based on if rootFilter owns the Node being modified by EditAction ea
				ea.redo();
				i.remove();
			}
		}
	}

}
