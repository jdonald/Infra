package jiyuiydi.infra.OS;

import java.io.File;

import jiyuiydi.infra.Box;
import jiyuiydi.infra.Metadata;

public class OSDirectory extends Box {

	OSDirectory(File d) {
		//with(new Box("OS", "directory"));
		//with(new Box("path"), d.getPath());
		String name = d.getName();
		if(name.isEmpty()) {
			name = d.toString(); // may pull string from drive label
			if(name.contains(File.separator))
				name = name.replaceAll("\\"+File.separator, "");
		}
		with(Metadata.lang_ID, name);
		OSLoader.instance.anticiapate(this, d);

		setUnloaded(true);
		File[] files = d.listFiles();
		if(files != null)
			setUnloaded(files.length);
	}

}
