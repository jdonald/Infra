package jiyuiydi.infra.ui;

import java.awt.event.KeyEvent;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import jiyuiydi.util.Keyboard;

public abstract class KeyBinding<T> {

	public boolean matchesPressed(KeyEvent e) { return false; }
	public boolean matchesTyped  (KeyEvent e) { return false; }
	public abstract void execute(T instance);

	// modules ///////////////////////////////////////////////////////////

	static public enum SoftkeyMode { PLAIN, SHIFT, CTRL, META, ALT, CTRL_SHIFT }

	static public class Hotkey<T> extends KeyBinding<T> {
		SoftkeyMode mode;
		int code;
		char ch;
		Consumer<T> fn;

		public Hotkey(SoftkeyMode m, char letter, Consumer<T> fn) { mode = m; this.fn = fn; ch = Character.toLowerCase(letter); }
		public Hotkey(SoftkeyMode m, int keyCode, Consumer<T> fn) { mode = m; this.fn = fn; code = keyCode; }

		boolean matchesModifiers(KeyEvent e) {
			int numSoftKeys = 0;
			if(e.isShiftDown  ()) ++numSoftKeys;
			if(e.isControlDown()) ++numSoftKeys;
			if(e.isMetaDown   ()) ++numSoftKeys;
			if(e.isAltDown    ()) ++numSoftKeys;

			switch(mode) {
			case PLAIN: if(numSoftKeys != 0)                       return false; break;
			case SHIFT: if(numSoftKeys != 1 || !e.isShiftDown  ()) return false; break;
			case CTRL:  if(numSoftKeys != 1 || !e.isControlDown()) return false; break;
			case META:  if(numSoftKeys != 1 || !e.isMetaDown   ()) return false; break;
			case ALT :  if(numSoftKeys != 1 || !e.isAltDown    ()) return false; break;
			case CTRL_SHIFT: if(numSoftKeys != 2 || (!e.isShiftDown() && !e.isControlDown())) return false; break;
			}
			return true;
		}

		boolean matches(KeyEvent e) {
			if(!matchesModifiers(e)) return false;

			if(ch > 0) {
				char keyCh = e.getKeyChar();
				if(e.isControlDown()) keyCh = translateCharFromControlKeyCombo(keyCh);
				return Character.toLowerCase(keyCh) == ch;
			}
			return e.getKeyCode() == code;
		}

		static private char translateCharFromControlKeyCombo(char ch) {
			if(ch == Keyboard.delete) return Keyboard.backspace;
			return (char) (ch + 'a' - 1);
		}

		@Override public boolean matchesPressed(KeyEvent e) { if(ch != 0) return false; return matches(e); }
		@Override public boolean matchesTyped  (KeyEvent e) { if(ch == 0) return false; return matches(e); }
		@Override public void execute(T instance) { fn.accept(instance); }
	}

	static public class Printables<T> extends KeyBinding<T> {
		private BiConsumer<T, Character> fn;
		char lastMatched;

		public Printables(BiConsumer<T, Character> fn) { this.fn = fn; }

		@Override public boolean matchesTyped(KeyEvent e) {
			if(e.isControlDown() || e.isMetaDown() || e.isAltDown()) return false;
			if(e.getKeyChar() == ' ') return false;
			if(!Keyboard.isPrintable(e.getKeyChar())) return false;
			lastMatched = e.getKeyChar();
			return true;
		}

		@Override public void execute(T instance) { fn.accept(instance, lastMatched); }
	}

}
