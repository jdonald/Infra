package jiyuiydi.infra;

import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import jiyuiydi.infra.java.*;
import jiyuiydi.util.Reflection;

public class Library {

	static public final Library core = new Library("jiyuiydi.infra");

	// instance //////////////////////////////////////////////////////////

	final Map<String, Class<? extends LibraryPatch>> mapID = new HashMap<>();

	@SuppressWarnings("unchecked")
	public Library(String packageName) {
		try {
			Class<?>[] cs = Reflection.getClasses(packageName);
			for(Class<?> c : cs)
				if(Reflection.isProperSubClassOf(c, LibraryPatch.class)) {
					Node id = Node.toNode(Reflection.getStaticValue(c, "ID"));
					mapID.put(id.toString(), (Class<? extends LibraryPatch>) c);
				}
		} catch(Exception e) {}
	}

	public LibraryPatch loadInstance(Node id) {
		try { return mapID.get(id.toString()).newInstance(); }
		catch(Exception e) { return null; }
	}

}

class CORE_LESS_THAN extends LibraryPatch {
	static final Node ID = new UTF8("<");
	public CORE_LESS_THAN() { add(new Symbol.Parameter(), "<", new Symbol.Parameter()); } // parameter template
	@Override public Node getOutput() {
		Node a1 = codePtr.evaluated();
		codePtr.sibling(2);
		Node a2 = codePtr.evaluated();
		if(a1 instanceof Int32 && a2 instanceof Int32) return Node.toNode(a1.asInt() < a2.asInt());
		if(a1 instanceof Int32) a1 = new Float32(a1.asInt());
		if(a2 instanceof Int32) a2 = new Float32(a2.asInt());
		if(a1 instanceof Float32 && a2 instanceof Float32) return Node.toNode(a1.asFloat() < a2.asFloat());
		return new Symbol.Error();
	}
}

class CORE_GREATER_THAN extends LibraryPatch {
	static final Node ID = new UTF8(">");
	public CORE_GREATER_THAN() { add(new Symbol.Parameter(), ">", new Symbol.Parameter()); } // parameter template
	@Override public Node getOutput() {
		Node a1 = codePtr.evaluated();
		codePtr.sibling(2);
		Node a2 = codePtr.evaluated();
		if(a1 instanceof Int32 && a2 instanceof Int32) return Node.toNode(a1.asInt() > a2.asInt());
		if(a1 instanceof Int32) a1 = new Float32(a1.asInt());
		if(a2 instanceof Int32) a2 = new Float32(a2.asInt());
		if(a1 instanceof Float32 && a2 instanceof Float32) return Node.toNode(a1.asFloat() > a2.asFloat());
		return new Symbol.Error();
	}
}

class CORE_ADDITION extends LibraryPatch {
	static final Node ID = new UTF8("+");
	public CORE_ADDITION() { add(new Symbol.Parameter(), "+", new Symbol.Parameter()); } // parameter template
	@Override public Node getOutput() {
		Node a1 = codePtr.evaluated();
		codePtr.sibling(2);
		Node a2 = codePtr.evaluated();
		if(a1 instanceof Int32 && a2 instanceof Int32) return new Int32(a1.asInt() + a2.asInt());
		if(a1 instanceof Int32) a1 = new Float32(a1.asInt());
		if(a2 instanceof Int32) a2 = new Float32(a2.asInt());
		if(a1 instanceof Float32 && a2 instanceof Float32) return new Float32(a1.asFloat() + a2.asFloat());
		return new Symbol.Error();
	}
}

class CORE_SUBTRACTION extends LibraryPatch {
	static final Node ID = new UTF8("-");
	public CORE_SUBTRACTION() { add(new Symbol.Parameter(), "-", new Symbol.Parameter()); } // parameter template
	@Override public Node getOutput() {
		Node a1 = codePtr.evaluated();
		codePtr.sibling(2);
		Node a2 = codePtr.evaluated();
		if(a1 instanceof Int32 && a2 instanceof Int32) return new Int32(a1.asInt() - a2.asInt());
		if(a1 instanceof Int32) a1 = new Float32(a1.asInt());
		if(a2 instanceof Int32) a2 = new Float32(a2.asInt());
		if(a1 instanceof Float32 && a2 instanceof Float32) return new Float32(a1.asFloat() - a2.asFloat());
		return new Symbol.Error();
	}
}

class CORE_MULTIPLICATION extends LibraryPatch {
	static final Node ID = new UTF8("*");
	public CORE_MULTIPLICATION() { add(new Symbol.Parameter(), "\u00D7", new Symbol.Parameter()); } // parameter template
	@Override public Node getOutput() {
		Node a1 = codePtr.evaluated();
		codePtr.sibling(2);
		Node a2 = codePtr.evaluated();
		if(a1 instanceof Int32 && a2 instanceof Int32) return new Int32(a1.asInt() * a2.asInt());
		if(a1 instanceof Int32) a1 = new Float32(a1.asInt());
		if(a2 instanceof Int32) a2 = new Float32(a2.asInt());
		if(a1 instanceof Float32 && a2 instanceof Float32) return new Float32(a1.asFloat() * a2.asFloat());
		return new Symbol.Error();
	}
}

class CORE_DIVISION extends LibraryPatch {
	static final Node ID = new UTF8("/");
	public CORE_DIVISION() { add(new Symbol.Parameter(), "/", new Symbol.Parameter()); } // parameter template
	@Override public Node getOutput() {
		Node a1 = codePtr.evaluated();
		codePtr.sibling(2);
		Node a2 = codePtr.evaluated();
		//if(a1 instanceof Int32 && a2 instanceof Int32) return new Int32(a1.asInt() / a2.asInt());
		if(a1 instanceof Int32) a1 = new Float32(a1.asInt());
		if(a2 instanceof Int32) a2 = new Float32(a2.asInt());
		if(a1 instanceof Float32 && a2 instanceof Float32) {
			float quotient = a1.asFloat() / a2.asFloat();
			if(quotient == Math.floor(quotient)) return new Int32((int) quotient);
			return new Float32(quotient);
		}
		return new Symbol.Error();
	}
}

class CORE_IF extends LibraryPatch {
	static final Node ID = new UTF8("if");
	public CORE_IF() { add("if", new Symbol.Parameter(), "then", new Symbol.Parameter(), "else", new Symbol.Parameter()); } // parameter template
	@Override public Node getOutput() {
		codePtr.sibling(1);
		Node condition = codePtr.evaluated();
		if(condition.getClass() == Symbol.True .class) { codePtr.sibling(2); return codePtr.focus; }
		if(condition.getClass() == Symbol.False.class) { codePtr.sibling(4); return codePtr.focus; }
		return new Symbol.Error();
	}
}

class CORE_JavaInstance extends LibraryPatch {
	static final Node ID = new UTF8("Java Object instance");
	public CORE_JavaInstance() { add(new Symbol.Parameter()); }
	@Override public Node getOutput() {
		if(count() >= 1 && get(0) instanceof JavaObjectReferenceNode) {
			Object o = ((JavaObjectReferenceNode) get(0)).o;
			return new JavaObjectModel(o);
		}
		return new Symbol.Error().with(Metadata.lang_patchOutput, "Bad live Java Object reference");
	}
}
