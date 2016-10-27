package jiyuiydi.infra.ui;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jiyuiydi.infra.Node;
import jiyuiydi.infra.UTF8;
import jiyuiydi.util.Utilities;

public class AutoCompleter {

	Set<Object> set = new HashSet<>();

	public AutoCompleter() {}

	public AutoCompleter(Class<?> c) {
		if(c.isEnum()) {
			Object[] ec = c.getEnumConstants();
			for(Object o : ec) set.add(o);
		}
	}

	public void add(Object o) { set.add(o); }

	public List<Object> getMatches(Node n) {
		List<Object> results = new ArrayList<>();

		for(Object o : set)
			if(matches(n, o)) results.add(o);

		return results;
	}

	static private boolean matches(Node n, Object o) {
		if(n == null) return true;
		if(n instanceof UTF8) return Utilities.sequentialContains(o.toString(), ((UTF8) n).get());
		return false;
	}

}
