package jiyuiydi.infra;

import static org.junit.Assert.*;

import org.junit.Test;

import jiyuiydi.infra.Box;
import jiyuiydi.infra.Spine;

public class SpineTest {

	@Test
	public void testUpDown() {
		Spine s = new Spine(new Box(1, 2, 3, new Box("a", "b", "c")));
		assertEquals(false, s.up());
		assertEquals("[1 2 3 [a b c]]", s.get().toString());
		s.down(3);
		assertEquals("[a b c]", s.get().toString());
		s.down(1);
		assertEquals("b", s.get().toString());
		assertEquals(false, s.down(0));
		s.up(2);
		assertEquals("[1 2 3 [a b c]]", s.get().toString());
	}

	@Test
	public void testNextSegment() {
		Spine a = new Spine(new Box(1, new Box(2, new Box(3, new Box(4)))));
		a.down(1,1,1,0);
		assertEquals("4", a.get().toString());
		Spine b = new Spine(a);
		assertEquals("4", b.get().toString());
		b.up(2);
		assertEquals("[3 [4]]", b.get().toString());
	}

	@Test
	public void testDownEvals() {
		Spine a = new Spine(Examples.helloWorldPatch());
		assertEquals("{value([Hello world!])}", a.get().toString());
		assertEquals("[Hello world!]", a.evaluated().toString());
		assertEquals(true, a.down(0));
		assertEquals("Hello", a.get().toString());
	}

	@Test
	public void testIndexInParent() {
		Spine s = new Spine(new Box(1, 2, 3, new Box("a", "b", "c")));
		assertEquals(null, s.getParent());
		assertEquals(0, s.indexInParent());
		s.down(3, 2);
		assertEquals(2, s.indexInParent());
		s.up();
		assertEquals(3, s.indexInParent());

		Spine s2 = new Spine(s);
		assertEquals(3, s2.indexInParent());
	}

	@Test
	public void testFork() {
		Spine a = new Spine(new Box(1, new Box(2, new Box(3, new Box(4)))));
		a.down(1,1,1);
		assertEquals("[4]", a.get().toString());

		Spine b = a.fork();
		assertEquals("[4]", a.get().toString());
		assertEquals("[4]", b.get().toString());

		a.down(0);
		assertEquals( "4" , a.get().toString());
		assertEquals("[4]", b.get().toString());
		b.down(0);
		assertEquals("4", a.get().toString());
		assertEquals("4", b.get().toString());
		a.up(); b.up();

		a.up();
		assertEquals("[3 [4]]", a.get().toString());
		assertEquals(  "[4]" , b.get().toString());
		b.up();
		assertEquals("[3 [4]]", a.get().toString());
		assertEquals("[3 [4]]", b.get().toString());
	}

}
