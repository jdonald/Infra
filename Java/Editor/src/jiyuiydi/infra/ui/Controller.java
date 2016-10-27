package jiyuiydi.infra.ui;

import java.awt.Component;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import jiyuiydi.util.Reflection;

public class Controller<T> implements KeyListener, MouseListener, MouseMotionListener, MouseWheelListener {

	T bindingsTarget;
	final ArrayList<KeyBinding<T>> bindings = new ArrayList<>();

	public Controller(Component c) {
		c.addKeyListener(this);
		c.addMouseListener(this);
		c.addMouseMotionListener(this);
		c.addMouseWheelListener(this);
	}

	public void loadKeyBindings(T objectWithStaticKeyBindings) {
		bindingsTarget = objectWithStaticKeyBindings;
		bindings.clear();
		for(Field f : bindingsTarget.getClass().getDeclaredFields()) {
			if(KeyBinding.class.isAssignableFrom(f.getType())) {
				f.setAccessible(true);
				try {
					bindings.add((KeyBinding<T>) f.get(bindingsTarget));
				} catch(Exception e) {}
				continue;
			}
			if(Collection.class.isAssignableFrom(f.getType())) {
				Class<?> cls = Reflection.getGenericParameterClass(f);
				if(KeyBinding.class.isAssignableFrom(cls)) {
					f.setAccessible(true);
					try {
						Collection<KeyBinding<?>> c = (Collection<KeyBinding<?>>) f.get(objectWithStaticKeyBindings);
						Iterator<KeyBinding<?>> i = c.iterator();
						while(i.hasNext())
							bindings.add((KeyBinding<T>) i.next());
					} catch (Exception e) {}
				}
				continue;
			}
		}
	}

	// KeyListener ///////////////////////////////////////////////////////

	@Override public void keyPressed (KeyEvent e) {
		//System.out.println(e);
		for(KeyBinding<T> k : bindings)
			if(k.matchesPressed(e)) {
				e.consume();
				k.execute(bindingsTarget);
				break;
			}
	}

	@Override public void keyReleased(KeyEvent e) {}

	@Override public void keyTyped   (KeyEvent e) {
		for(KeyBinding<T> k : bindings) if(k.matchesTyped(e)) { e.consume(); k.execute(bindingsTarget); }
	}

	// MouseListener 
	@Override public void mouseClicked (MouseEvent e) {}
	@Override public void mouseEntered (MouseEvent e) {}
	@Override public void mouseExited  (MouseEvent e) {}
	@Override public void mousePressed (MouseEvent e) {}
	@Override public void mouseReleased(MouseEvent e) {}

	// MouseMotionListener
	@Override public void mouseDragged(MouseEvent e) {}
	@Override public void mouseMoved  (MouseEvent e) {}

	// MouseWheelListener
	@Override public void mouseWheelMoved(MouseWheelEvent e) {}

}
