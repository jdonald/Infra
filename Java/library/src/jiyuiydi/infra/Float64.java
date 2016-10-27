package jiyuiydi.infra;

public class Float64 extends Quantity {

	double value;

	public Float64() {}
	public Float64(double d) { value = d; }

	@Override
	public Number get() { return value; }

	@Override
	public void set(Number v) {
		try { set((double) v); }
		catch(RuntimeException e) {}
	}

	public void set(double f) {
		if(f == value) return;
		value = f;
		notifyObservers();
	}

	@Override
	protected Node shallowCopy() {
		Float64 c = (Float64) super.shallowCopy();
		c.value = value;
		return c;
	}

	@Override
	public String toString() { return String.valueOf(value); }

}
