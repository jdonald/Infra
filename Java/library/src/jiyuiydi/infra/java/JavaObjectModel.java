package jiyuiydi.infra.java;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.HashMap;

import jiyuiydi.infra.*;
import jiyuiydi.infra.markupModels.Java;

public class JavaObjectModel extends Box {

	static final String nativePatchID_JavaObjectInstance = "Java Object instance";

	// instance //////////////////////////////////////////////////////////

	final Object model;
	final HashMap<Field, Node> fieldMap = new HashMap<>();

	public JavaObjectModel(Object o) { model = o; init(); }

	protected void init() {
		Class<?> cls = model.getClass();
		with(new UTF8(Java.langID),
				new Keyed(Java.propID_class, cls.getCanonicalName()),
				new Keyed(Java.propID_instance, System.identityHashCode(model))
				);
		Field[] fs = cls.getDeclaredFields();
		for(Field f : fs) {
			if(Modifier.isStatic(f.getModifiers())) continue;
			//if(!Modifier.isPublic(f.getModifiers())) continue;
			try {
				if(!Modifier.isPublic(f.getModifiers()))
					f.setAccessible(true);
				Object val = f.get(model);
				Node n;
				if(val == null || f.getType().isPrimitive() || f.getType() == String.class)
					n = Node.toNode(val);
				else
					n = new Patch(new Keyed(nativePatchID_JavaObjectInstance, new JavaObjectReferenceNode(val)));
				if(n == null) continue;
				n.with(Metadata.lang_ID, f.getName());
				fieldMap.put(f, n);
				add(n);
			} catch (IllegalArgumentException | IllegalAccessException e) { e.printStackTrace(); }
		}
		Method[] ms = cls.getDeclaredMethods();
		for(Method m : ms) {
			Box b = new Patch();
			b.with(Metadata.lang_ID, m.getName());
			for(Parameter p : m.getParameters()) {
				b.add(new UTF8(p.getName()));
			}
			add(b);
		}

		new Thread(new Runnable() {
				public void run() {
					//while(true) {
						try { Thread.sleep(5000); }
						catch (InterruptedException e) { e.printStackTrace(); }
						refresh();
					//}
				}
			}).start();
	}

	public void refresh() {
		fieldMap.forEach((f, n)->{
			try {
				Object val = f.get(model);
				if(n instanceof Int32) ((Int32) n).set((Integer) val);
				else
				if(n instanceof Float32) ((Float32) n).set((Float) val);
				else
				if(n instanceof UTF8) ((UTF8) n).set((String) val);
				else
				if(n instanceof Patch) {
					Patch p = (Patch) n;
					Keyed k = new Keyed(nativePatchID_JavaObjectInstance, new JavaObjectReferenceNode(val));
					if(p.count() > 0 && p.get(0).equals(k)) return;
					while(p.count() > 0) p.remove(0);
					p.add(k);
				}
			} catch (Exception e) { e.printStackTrace(); }
		});
	}

}
