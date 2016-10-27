package jiyuiydi.infra;

public enum Opcode {

	STOP    ("stop"),		// 0

	UP      ("parent"),
	DOWN    ("child"),
	LEFT    ("left"),
	RIGHT   ("right"),
	META    ("metadata"),	// 5

	ROOT    ("root"),
	ID      ("find ID"),
	UID     ("find UID"),

	WRITE   ("write"),
	INSERT  ("insert"),		// 10
	ADD     ("add to list"),
	MOVE_TO ("move to"),
	MOVE_BY ("move by"),

	VALUE   ("value"),
	CLONE   ("clone"),		// 15
	COUNT   ("count"),

	DOWN_NOEVAL("down w/o eval"),
	WRITE_CLONE("write clone"),
	EVAL    ("eval"),		// 20

	SAVE    ("save"),
	CHOICE	("choice"),				// like 'child' but indicates that siblings are alternatives (drop-down menu)

	;

	String name;

	Opcode() {}

	Opcode(String prettyName) { name = prettyName; }

	public String getName() { return name == null ? toString() : name; }

	@Override public String toString() { return getName(); }

}
