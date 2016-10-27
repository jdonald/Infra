package jiyuiydi.infra.OS;

import java.io.File;

import jiyuiydi.infra.Metadata;
import jiyuiydi.infra.UTF8;

public class OSFile extends UTF8 {
	OSFile(File f) {
		//with(new UTF8("OS"), "file");
		with(Metadata.lang_ID, f.getName());
		//add(f.getName());
		//Bytes fs = new Bytes();
		//Node fs = new UTF8();
		//DirectoryLoader.instance.anticiapate(fs, f);
		//fs.setUnloaded(true);
		//add(fs);
		OSLoader.instance.anticiapate(this, f);
		setUnloaded(true);
	}
}
