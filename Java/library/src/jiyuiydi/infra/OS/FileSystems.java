package jiyuiydi.infra.OS;

import java.io.File;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import jiyuiydi.infra.*;

public class FileSystems extends LibraryPatch {

	static public final Node ID = new UTF8("file");

	public FileSystems() {
		FileSystem fs = java.nio.file.FileSystems.getDefault();
		for(Path p : fs.getRootDirectories())
			add(new OSDirectory(p.toFile()));
	}

	public FileSystems(File root) {
		add(new OSDirectory(root));
	}

	@Override
	public Node getOutput() { return null; }

}
