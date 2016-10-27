package jiyuiydi.infra;

import java.util.ArrayList;
import java.util.function.Consumer;

public class Box extends Node {

	protected final ArrayList<Node> children = new ArrayList<Node>();

	// Container /////////////////////////////////////////////////////////

	public Box() {}

	public Box(Object... os) { for(Object o : os) add(o); }

	public void foreach(Consumer<Node> c) { children.forEach(c); }

	public void add(Object... os) { for(Object o : os) insert(children.size(), toNode(o)); }
	public void insert(int index, Node n) { children.add(index, n); notifyInsertion(index, n); }
	public Node remove(int index) { Node n = children.remove(index); notifyRemoval(index, n); return n; }
	public void replace(int index, Object o) {
		if(index == count()) { add(o); return; }
		Node p = toNode(o);
		Node n = children.get(index);
		//n.notifyWillBeReplacedBy(p);
		children.set(index, p);
		//n.notifyReplacedBy(p);
		notifyReplacement(index, n, p);
		notifyRemoval(index, n);
		notifyInsertion(index, p);
	}

	public void replace(Node n, Object o) {
		for(int i = 0; i < children.size(); ++i)
			if(children.get(i) == n) {
				replace(i, o);
				break;
			}
	}

	public Keyed findKeyed(Object key) {
		for(int i = 0; i < count(); ++i)
			if(get(i) instanceof Keyed) {
				Keyed k = (Keyed) get(i);
				if(k.count() > 0 && k.get(0).equals(key))
					return k;
			}
		return null;
	}

	// Node //////////////////////////////////////////////////////////////

	@Override
	public int count() { return children.size(); }

	@Override
	public Node get(int index) { return children.get(index); }

	@Override
	protected Node shallowCopy() {
		Box c = (Box) super.shallowCopy();
		c.children.clear(); // Library patches initialize with children
		for(Node n : children)
			c.children.add(n);
		return c;
	}

	@Override
	public boolean blindEquals(Object o) {
		if(!(o instanceof Box)) return false;
		Box b = (Box) o;
		if(children.size() != b.children.size()) return false;
		for(int i = 0; i < children.size(); ++i)
			if(!children.get(i).equals(b.children.get(i)))
				return false;
		return true;
	}

	// Object ////////////////////////////////////////////////////////////

	@Override public int hashCode() { return children.hashCode(); }

	@Override
	public String toString() {
		final ArrayList<Node> backup = new ArrayList<>(children);
		children.clear(); children.add(new UTF8("\u221E")); // infinity

		StringBuilder sb = new StringBuilder();
		sb.append('[');
		for(Node n : backup) {
			sb.append(n.toString());
			sb.append(' ');
		}
		if(!backup.isEmpty())
			sb.delete(sb.length()-1, sb.length());
		sb.append(']');

		children.clear();
		children.addAll(backup);

		if(metadata != null) sb.append(metadata.toString());
		return sb.toString();
	}

}
