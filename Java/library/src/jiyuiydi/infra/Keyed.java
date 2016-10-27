package jiyuiydi.infra;

public class Keyed extends Box {

	public Keyed() {}

	public Keyed(Object... os) { super(os); }

	public Keyed(Object key, Object value) {
		children.add(toNode(key));
		children.add(toNode(value));
	}

	// Node //////////////////////////////////////////////////////////////

//	@Override
//	public Node get(int index) {
//		if(index >= children.size()) return nil;
//		return super.get(index);
//	}

	// Object ////////////////////////////////////////////////////////////

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		if(count() >= 1) sb.append(get(0).toString());
		sb.append(':');
		if(count() >= 2) sb.append(get(1).toString());
		if(children.size() > 2) sb.append('+');
		return sb.toString();
	}

}
