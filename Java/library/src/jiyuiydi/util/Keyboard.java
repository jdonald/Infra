package jiyuiydi.util;

public class Keyboard {

	static public final char
		backspace   =   8, // '\b'
		linefeed    =  10, // '\n'
		carriageRet =  13,
		escape      =  27,
		space       =  32,
		delete      = 127; // CTRL + Backspace

	public static boolean isPrintable(char c) {
		if(c < 32) return false;
		if(c == delete) return false;
		return true;
	}

}
