package jiyuiydi.infra;

import java.nio.ByteBuffer;

public class Float32 extends Quantity {

	float value;

	public Float32() {}
	public Float32(float f) { value = f; }

	// Quantity //////////////////////////////////////////////////////////

	@Override public Number get() { return value; }

	@Override public void set(Number v) {
		try { set((float) v); }
		catch(RuntimeException e) {}
	}

	public void set(float f) {
		if(f == value) return;
		value = f;
		notifyObservers();
	}

	// Node //////////////////////////////////////////////////////////////

	@Override
	protected Node shallowCopy() {
		Float32 c = (Float32) super.shallowCopy();
		c.value = value;
		return c;
	}

	// Object ////////////////////////////////////////////////////////////

	@Override
	public String toString() { return String.valueOf(value); }

}
