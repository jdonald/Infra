package jiyuiydi.infra;

public abstract class Quantity extends Node {

	public abstract Number get();

	public abstract void set(Number v);

	@Override
	public boolean blindEquals(Object o) {
		if(o instanceof Quantity)
			return ((Quantity) o).get().equals(get());
		return false;
	}

}
