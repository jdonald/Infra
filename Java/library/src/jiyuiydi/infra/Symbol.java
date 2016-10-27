package jiyuiydi.infra;

public abstract class Symbol extends Node {

	static public final class False     extends Symbol { public String getName() { return "false"; } }
	static public final class True      extends Symbol { public String getName() { return "true" ; } }
	static public final class Void      extends Symbol { public String getName() { return "void" ; } }
	static public final class Null      extends Symbol { public String getName() { return "NULL" ; } }
	static public final class Any       extends Symbol { public String getName() { return "*"    ; } }
	static public final class Parameter extends Symbol { public String getName() { return "_"    ; } }
	static public final class Error     extends Symbol { public String getName() { return "error"; } }
	static public final class Constant  extends Symbol { public String getName() { return "#"    ; } }

	public abstract String getName();

	@Override
	public boolean blindEquals(Object o) { return this.getClass() == o.getClass(); }

	@Override
	public String toString() { return getName() + (metadata == null ? "" : metadata.toString()); }

}
