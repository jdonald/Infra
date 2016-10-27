package jiyuiydi.infra;

import java.util.ArrayList;

public interface DelayedLoader {

	void anticiapate(Node n, Object memento);

	void unanticipate(Node n);

	boolean canLoad(Node n);

	void load(Node n);

	// static ////////////////////////////////////////////////////////////

	static final ArrayList<DelayedLoader> loaders = new ArrayList<>();

	static void registerLoader(DelayedLoader dl) { loaders.add(dl); }

	static void unregisterLoader(DelayedLoader dl) { loaders.remove(dl); }

	static boolean useLoader(Node n) {
		for(DelayedLoader dl : loaders)
			if(dl.canLoad(n)) {
				dl.load(n);
				n.setUnloaded(false);
				return true;
			}
		return false;
	}

}
