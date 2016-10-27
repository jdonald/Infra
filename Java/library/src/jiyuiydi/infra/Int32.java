package jiyuiydi.infra;

public class Int32 extends Quantity {

	int value;

	public Int32() {}
	public Int32(int v) { value = v; }

	// Quantity //////////////////////////////////////////////////////////

	@Override public Number get() { return value; }

	@Override public void set(Number v) {
		try { set((int) v); }
		catch(RuntimeException e) {}
	}

	public int getInt() { return value; }

	public void set(int v) {
		if(value == v) return;
		value = v;
		notifyObservers();
	}

	// Node //////////////////////////////////////////////////////////////

	@Override
	protected Node shallowCopy() {
		Int32 c = (Int32) super.shallowCopy();
		c.value = value;
		return c;
	}

	// Object ////////////////////////////////////////////////////////////

	@Override
	public String toString() { return String.valueOf(value); }

}
