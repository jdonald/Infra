package jiyuiydi.infra;

public class Command extends Keyed {

	public Command() {}
	public Command(Opcode op) { super(op); }
	public Command(Opcode op, Object param) { super(op.ordinal(), param); }
	public Command(Object... os) { super(os); }

	@Override
	public String toString() {
		if(count() == 0) return "NoOp()";
		StringBuilder sb = new StringBuilder();
		if(children.get(0) instanceof Int32)
			sb.append(Opcode.values()[children.get(0).asInt()].toString());
		else
			sb.append(children.get(0).toString());
		sb.append('(');
		for(int i = 1; i < count(); ++i) {
			sb.append(children.get(i).toString());
			if(i < count()-1) sb.append(",");
		}
		sb.append(')');
		return sb.toString();
	}

}
