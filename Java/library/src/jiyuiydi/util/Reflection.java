package jiyuiydi.util;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;

public class Reflection {

	static public Object getStaticValue(Class<?> cls, String fieldName) {
		try {
			Field f = cls.getDeclaredField(fieldName);
			if(Modifier.isStatic(f.getModifiers())) {
				f.setAccessible(true);
				return f.get(null);
			}
		} catch (Exception e) { e.printStackTrace(); }
		return null;
	}

	static public Class<?> getGenericParameterClass(Field f) {
		Type gt = f.getGenericType();
		if(gt instanceof Class) {
			if(((Class<?>) gt).isPrimitive()) return null;
			//TypeVariable<?>[] declaredParams = ((Class<?>) gt).getTypeParameters();
		}
		if(!(gt instanceof ParameterizedType)) return Object.class;
		Type[] args = ((ParameterizedType) gt).getActualTypeArguments();
		if(args[0] instanceof Class) return (Class<?>) args[0];
		if(args[0] instanceof ParameterizedType) {
			Type t = ((ParameterizedType) args[0]).getRawType();
			if(t instanceof Class) return (Class<?>) t;
		}
		if(args[0] instanceof WildcardType) {
			Type[] uBounds = ((WildcardType) args[0]).getUpperBounds();
			if(uBounds[0] instanceof Class) return (Class<?>) uBounds[0];
		}
		return null;
	}

	static public List<Class<?>> getLoadedClasses() {
		ClassLoader myCL = Thread.currentThread().getContextClassLoader();
		List<Class<?>> loadedClasses = new ArrayList<>();

		while (myCL != null) {
			Class<?> CL_class = myCL.getClass();
			while (CL_class != java.lang.ClassLoader.class)
				CL_class = CL_class.getSuperclass();
			try {
				Field classesField = CL_class.getDeclaredField("classes");
				classesField.setAccessible(true);

				@SuppressWarnings("unchecked")
				Vector<Class<?>> classes = (Vector<Class<?>>) classesField.get(myCL);
				loadedClasses.addAll(classes);
			} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) { e.printStackTrace(); }

			myCL = myCL.getParent();
		}

		return loadedClasses;
	}

	// Discover defined classes in a given package ///////////////////////

	static public Class<?>[] getClasses(Package p) throws ClassNotFoundException, IOException {
		return getClasses(p.getName());
	}

	static public Class<?>[] getClasses(String packageName) throws IOException, ClassNotFoundException {
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		assert classLoader != null;
		String path = packageName.replace('.', '/');
		Enumeration<URL> resources = classLoader.getResources(path);
		List<File> dirs = new ArrayList<File>();
		while (resources.hasMoreElements()) {
			URL resource = resources.nextElement();
			dirs.add(new File(Utilities.unescapeURL(resource.getFile())));
		}
		ArrayList<Class<?>> classes = new ArrayList<Class<?>>();
		for (File directory : dirs)
			classes.addAll(findClasses(directory, packageName));
		//for(Class<?> c : classes.toArray(new Class[classes.size()]))
		//	classes.addAll(findContainedClasses(c));
		return classes.toArray(new Class[classes.size()]);
	}

	static private List<Class<?>> findClasses(File directory, String packageName) throws ClassNotFoundException {
		List<Class<?>> classes = new ArrayList<Class<?>>();
		if (!directory.exists()) return classes;
		File[] files = directory.listFiles();
		for (File file : files) {
			if (file.isDirectory()) {
				assert !file.getName().contains(".");
				classes.addAll(findClasses(file, packageName + "." + file.getName()));
			} else if (file.getName().endsWith(".class")) {
				classes.add(Class.forName(packageName + '.' + file.getName().substring(0, file.getName().length() - 6)));
			}
		}
		return classes;
	}

	// Reason about Class relationships //////////////////////////////////

	static public boolean isProperSubClassOf(Class<?> subClassCandidate, Class<?> beingExtended) {
		try { subClassCandidate.asSubclass(beingExtended); }
		catch(ClassCastException e) { return false; }
		return !subClassCandidate.equals(beingExtended);
	}

	@SuppressWarnings("unchecked")
	static public <T> Class<? extends T>[] getAllProperSublassesOf(String packageName, Class<T> beingExtended) {
		ArrayList<Class<? extends T>> cs = new ArrayList<>();
		try {
			for(Class<?> cls : getClasses(packageName))
				if(isProperSubClassOf(cls, beingExtended))
					cs.add((Class<? extends T>) cls);
		} catch (ClassNotFoundException | IOException e) { e.printStackTrace(); }
		return cs.toArray(new Class[cs.size()]);
	}

}
