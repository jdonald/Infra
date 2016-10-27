package jiyuiydi.infra.java;

import jiyuiydi.infra.Node;

public class JavaObjectReferenceNode extends Node {

	public Object o;

	public JavaObjectReferenceNode() {}

	public JavaObjectReferenceNode(Object o) { this.o = o; }

	// Node //////////////////////////////////////////////////////////////

	@Override
	protected Node shallowCopy() {
		JavaObjectReferenceNode jorn = (JavaObjectReferenceNode) super.shallowCopy();
		jorn.o = o;
		return jorn;
	}

	@Override
	public boolean blindEquals(Object o) {
		return o instanceof JavaObjectReferenceNode
				&& ((JavaObjectReferenceNode) o).o == this.o;
	}

}
