package jiyuiydi.infra;

import java.util.Collection;

import jiyuiydi.util.Observer.Observable;

public abstract class Node extends Observable {

	static public final int INDEX_METADATA = -1;
	static public final int INDEX_VIRTUALCHILD = -2;
	static public final int INDEX_ERROR = -3;
	static public final int UNLOADED_UNKNOWN = -1;

	static public Node toNode(Object o) {
		if (o instanceof Node)
			return (Node) o;
		if (o instanceof Enum)
			return new Int32(((Enum<?>) o).ordinal());
		if (o instanceof Collection) {
			Box b = new Box();
			for (Object i : (Collection<?>) o)
				b.add(i);
			return b;
		}

		if (o == null)
			return new Symbol.Null();
		if (o == Boolean.TRUE)
			return new Symbol.True();
		if (o == Boolean.FALSE)
			return new Symbol.False();

		final Class<?> oc = o.getClass();
		if (oc == Byte.class)
			return new Int32((int) o);
		if (oc == Short.class)
			return new Int32((int) o);
		if (oc == Integer.class)
			return new Int32((int) o);
		if (oc == Long.class)
			return new Int64((long) o);
		if (oc == Float.class)
			return new Float32((float) o);
		if (oc == Double.class)
			return new Float64((double) o);
		if (oc == Character.class)
			return new UTF8(String.valueOf((char) o));
		if (oc == String.class)
			return new UTF8((String) o);

		return null;
	}

	// instance //////////////////////////////////////////////////////////

	Metadata metadata;
	int numChildrenUnloaded;

	// Node //////////////////////////////////////////////////////////////

	protected Node() {}

	public boolean isUnloaded() { return numChildrenUnloaded != 0; }
	public int getNumUnloaded() { return numChildrenUnloaded; }
	public void setUnloaded(boolean unloaded) { numChildrenUnloaded = unloaded ? -1 : 0; }
	public void setUnloaded(int numChildrenUnloaded) { this.numChildrenUnloaded = numChildrenUnloaded; }

	public int count() { return 0; }

	public Node get(int index) { return null; }

	public int indexOf(Node findChild) {
		for(int i = 0; i < count(); ++i)
			if(get(i) == findChild) return i;
		return INDEX_ERROR;
	}

	public boolean hasMetadata() { return metadata != null; }

	public Metadata getMetadata() { return metadata; }

	public Keyed getMetadataChannel(Node lang) {
		if (metadata == null)
			return null;
		return metadata.getOrNull(lang);
	}

	public Node with(Node lang, Object... md) {
		if (metadata == null)
			metadata = new Metadata();
		if(lang == null) {
			for(Object d : md)
				metadata.add(d);
		} else {
			for(Object d : md)
				metadata.add(lang, d);
		}
		return this;
	}

	public boolean isContainer() {
		return count() >= 0;
	}

	public int asInt() {
		if (this instanceof Int32)
			return ((Int32) this).value;
		try {
			if (this instanceof UTF8)
				return Integer.parseInt(((UTF8) this).value);
		} catch (Exception e) {
		}
		return 0;
	}

	public float asFloat() {
		if (this instanceof Float32)
			return ((Float32) this).value;
		if (this instanceof Int32)
			return ((Int32) this).value;
		try {
			return Float.parseFloat(toString());
		} catch (NumberFormatException e) {
		}
		return 0;
	}

	public Box asBox() {
		if (this instanceof Box)
			return (Box) this;
		else
			return new Box(this);
	}

	protected Node shallowCopy() {
		try {
			Node c = getClass().newInstance();
			c.metadata = metadata;
			return c;
		} catch (InstantiationException | IllegalAccessException e) {
			e.printStackTrace();
			return null;
		}
	}

	protected Node deepCopy() {
		Node n = shallowCopy();
		if (n.count() == 0)
			return n;
		Box b = (Box) n;
		for (int i = 0; i < n.count(); ++i) {
			b.replace(i, b.get(i).deepCopy());
		}
		return n;
	}

	public boolean blindEquals(Object o) {
		return this == o;
	}

	// Object ////////////////////////////////////////////////////////////

	@Override
	public boolean equals(Object obj) {
		if (obj == this)
			return true;
		if (getClass() == obj.getClass())
			return blindEquals(obj);
		final Node n = toNode(obj);
		return this.blindEquals(n) && n.blindEquals(this);
	}

	public boolean equalsWithMetadata(Node n) {
		if (n == this)
			return true;
		if (metadata == null && n.metadata == null)
			return equals(n); // stop infinite recursion
		Metadata myMD = metadata == null ? new Metadata() : metadata; // upgrade
																		// nulls
																		// for
																		// empty
																		// Box
																		// comparison
		Metadata nsMD = n.metadata == null ? new Metadata() : n.metadata;
		return myMD.equalsWithMetadata(nsMD) && equals(n);
	}

}
