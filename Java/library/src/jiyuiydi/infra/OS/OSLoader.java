package jiyuiydi.infra.OS;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.IdentityHashMap;
import java.util.Map;

import jiyuiydi.infra.Box;
import jiyuiydi.infra.DelayedLoader;
import jiyuiydi.infra.Node;
import jiyuiydi.infra.UTF8;

public class OSLoader implements DelayedLoader {

	static public final OSLoader instance = new OSLoader();

	Map<Node, File> pendingDirectories = new IdentityHashMap<>();

	private OSLoader() { DelayedLoader.registerLoader(this); }

	@Override
	public void anticiapate(Node n, Object memento) {
		if(!(memento instanceof File)) throw new RuntimeException("Memento must be a java.io.File object.");
		pendingDirectories.put(n, (File) memento);
	}

	@Override public void unanticipate(Node n) { pendingDirectories.remove(n); }

	@Override public boolean canLoad(Node n) { return pendingDirectories.containsKey(n); }

	@Override
	public void load(Node n) {
		File source = pendingDirectories.remove(n);
		if(source == null) throw new RuntimeException("Loading this node was not anticipated.");

		if(source.isDirectory() && source.canRead()) {
			if(!(n instanceof Box)) throw new RuntimeException("Node must be a Box instance.");
			File[] fl = source.listFiles();
			if(fl != null) {
				for(File f : fl)
					if(f.isDirectory()) ((Box) n).add(new OSDirectory(f));
				for(File f : source.listFiles()) {
					if(f.isFile() && f.canExecute())
						((Box) n).add(new OSExecutable(f));
					else
					if(f.isFile())
						((Box) n).add(new OSFile(f));
				}
			}
		}
		else
		if(source.isFile()) {
			//if(!(n instanceof Bytes)) throw new RuntimeException("Node must be a Bytes instance.");
			//Bytes b = (Bytes) n;
			try(FileInputStream in = new FileInputStream(source)) {
				byte[] data = new byte[in.available()];
				in.read(data, 0, data.length);
				//b.set(data);
				((UTF8) n).set(new String(data));
			}
			catch(IOException e) { e.printStackTrace(); }
		}
	}

}
