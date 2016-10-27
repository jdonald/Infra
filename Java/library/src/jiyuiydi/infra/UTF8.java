package jiyuiydi.infra;

import java.nio.ByteBuffer;

public class UTF8 extends Node {

	String value;

	public UTF8() { value = ""; }
	public UTF8(String s) { value = s; }

	public String get() { return value; }

	public void set(String s) { value = s; notifyObservers(); }

	public void insert(int index, String s) {
		StringBuilder sb = new StringBuilder(value);
		sb.insert(index, s);
		value = sb.toString();
		notifyInsertion(index, s);
	}

	public void remove(int index, int length) {
		StringBuilder sb = new StringBuilder(value);
		String s = sb.substring(index, index + length);
		sb.delete(index, index + length);
		value = sb.toString();
		notifyRemoval(index, s);
	}

	// Node //////////////////////////////////////////////////////////////

	@Override
	protected Node shallowCopy() {
		UTF8 c = (UTF8) super.shallowCopy();
		c.value = value;
		return c;
	}

	@Override
	public boolean blindEquals(Object o) {
		if(o instanceof UTF8) return ((UTF8) o).value.equals(value);
		return false;
	}

	// Object ////////////////////////////////////////////////////////////

	@Override public int hashCode() { return value.hashCode(); }

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		if(value.isEmpty() || value.contains(" ")) {
			sb.append('`');
			sb.append(value);
			sb.append('\'');
		} else sb.append(value);
		if(metadata != null)
			sb.append(metadata.toString());
		return sb.toString();
	}

}
