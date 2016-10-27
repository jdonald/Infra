package jiyuiydi.infra;

public class Int64 extends Quantity {

	long value;

	public Int64() {}
	public Int64(long v) { value = v; }

	@Override
	public Number get() { return value; }

	@Override
	public void set(Number v) {
		try { set((long) v); }
		catch(RuntimeException e) {}
	}

	public void set(long l) {
		if(l == value) return;
		value = l;
		notifyObservers();
	}

	@Override
	protected Node shallowCopy() {
		Int64 c = (Int64) super.shallowCopy();
		c.value = value;
		return c;
	}

	@Override
	public String toString() { return String.valueOf(value); }

}
