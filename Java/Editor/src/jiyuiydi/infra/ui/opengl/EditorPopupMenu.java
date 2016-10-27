package jiyuiydi.infra.ui.opengl;

import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;

import jiyuiydi.infra.Box;
import jiyuiydi.infra.Node;
import jiyuiydi.infra.Patch;
import jiyuiydi.infra.java.JavaObjectModel;
import jiyuiydi.util.Reflection;

public class EditorPopupMenu extends JPopupMenu {

	private static final long serialVersionUID = 1L;
	final Editor editor;

	public EditorPopupMenu(Editor editor) {
		this.editor = editor;

		add(makeClassMenu());

		add(new JSeparator());

		//add(new JMenuItem("Menu Item 1")); // dummies
		//add(new JMenuItem("Menu Item 2"));

		// Generate a Byte-encoding face for selected node
		add(new AbstractAction() {
			private static final long serialVersionUID = 1L;
			{ putValue(Action.NAME, "View Encoding"); }
			public void actionPerformed(ActionEvent e) {
				View v = editor.selections.getEnd();
				v.addFace(new Face_Encoding(v), true);
			}
		});

		add(new AbstractAction() {
			private static final long serialVersionUID = 1L;
			{ putValue(Action.NAME, "Collapse"); }
			public void actionPerformed(ActionEvent e) {
				for(View v : editor.selections.getViews())
					v.addFace(new Face_Summary(v), true);
			}
		});

		// An empty Patch object
		add(new AbstractAction() {
			private static final long serialVersionUID = 1L;
			{	putValue(Action.NAME, "Instance Test");
				putValue(Action.SHORT_DESCRIPTION, "Make a reference to a new Patch node.");
			}
			public void actionPerformed(ActionEvent e) {
				Node n = editor.rootView.getModel();
				if(n instanceof Box)
					((Box) n).add(new JavaObjectModel(new Patch()));
			}
		});

		add(new AbstractAction() {
			private static final long serialVersionUID = 1L;
			{ putValue(Action.NAME, "Reflect on View Instance"); }
			public void actionPerformed(ActionEvent e) {
				Node n = editor.rootView.getModel();
				if(n instanceof Box)
					((Box) n).add(new JavaObjectModel(editor.selections.getEnd()));
			}
		});

		add(new AbstractAction() {
			private static final long serialVersionUID = 1L;
			{ putValue(Action.NAME, "Inspect Editor"); }
			public void actionPerformed(ActionEvent e) {
				Node n = editor.rootView.getModel();
				if(n instanceof Box)
					((Box) n).add(new JavaObjectModel(editor));
			}
		});
	}

	JMenu makeClassMenu() {
		class PrefixMenu extends JMenu {
			private static final long serialVersionUID = 1L;
			Map<String, PrefixMenu> map = new HashMap<>();
			String prefix;
			int count = -1;

			PrefixMenu(String prefix) { super(prefix); this.prefix = prefix; }

			void finish() {
				count = getItemCount() - map.size(); // items that aren't sub-menus
				for(PrefixMenu pm : map.values()) {
					pm.finish();
					count += pm.count; // include descendants
				}
				setText(prefix + " (" + count + ")");
			}
		}
		List<Class<?>> classes = Reflection.getLoadedClasses();
		PrefixMenu classMenu = new PrefixMenu("Loaded Classes");

		for(Class<?> cls : classes) {
			Package p = cls.getPackage();
			String[] segs = p.getName().split("\\.");
			PrefixMenu m = classMenu; // start at root
			for(String s: segs) {
				PrefixMenu next = m.map.get(s);
				if(next == null) {
					next = new PrefixMenu(s);
					m.add(next);
					m.map.put(s, next);
				}
				m = next;
			}
		}
		for(Class<?> cls : classes) {
			Package p = cls.getPackage();
			String[] segs = p.getName().split("\\.");
			PrefixMenu m = classMenu; // start at root
			for(String s: segs) {
				PrefixMenu next = m.map.get(s);
				m = next;
			}
			m.add(new AbstractAction() {
				private static final long serialVersionUID = 1L;
				{ putValue(Action.NAME, classNameWithoutPackage(cls)); }
				public void actionPerformed(ActionEvent arg0) {
					Node n = editor.rootView.getModel();
					if(n instanceof Box)
						((Box) n).add(new JavaObjectModel(cls));
				}});
		}

		classMenu.finish();
		return classMenu;
	}
	
	private String classNameWithoutPackage(Class<?> cls) {
		String[] n = cls.getName().split("\\.");
		return n[n.length - 1];
	}

}
