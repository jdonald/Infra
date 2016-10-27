package jiyuiydi.util;

import static org.junit.Assert.*;

import java.lang.reflect.Field;
import java.util.List;

import org.junit.Test;

public class ReflectionTest {

	int a;
	List<String> s;
	List<?> q;
	
	@SuppressWarnings("rawtypes")
	List qq;
	
	List<List<String>> ls;
	List<? extends String> es;
	
	@Test
	public void testGenericTypeReflection() throws NoSuchFieldException, SecurityException {
		assertEquals(null,			fetch("a"));
		assertEquals(String.class,	fetch("s"));
		assertEquals(Object.class,	fetch("q"));
		assertEquals(Object.class,	fetch("qq"));
		assertEquals(List.class,	fetch("ls"));
		assertEquals(String.class,	fetch("es"));
	}
	
	Class<?> fetch(String name) throws NoSuchFieldException, SecurityException {
		return Reflection.getGenericParameterClass(getClass().getDeclaredField(name));
	}

	@Test
	public void testGetLoadedClasses() {
		class Z {}
		List<Class<?>> l = Reflection.getLoadedClasses();
		assertFalse(l.contains(Z.class));
		Z z = new Z();
		assertTrue(l.contains(Z.class));
	}

}
