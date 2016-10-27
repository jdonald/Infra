package jiyuiydi.infra;

import static org.junit.Assert.*;

import java.io.IOException;

import jiyuiydi.infra.CORE_GREATER_THAN;
import jiyuiydi.infra.CORE_LESS_THAN;
import jiyuiydi.infra.Int32;
import jiyuiydi.infra.Library;
import jiyuiydi.infra.Node;
import jiyuiydi.infra.Patch;
import jiyuiydi.infra.Symbol;
import jiyuiydi.util.Reflection;

import org.junit.Test;

public class LibraryTest {

	@Test
	public void testPatchSubclassFinding() throws ClassNotFoundException, IOException {
		assertEquals("<", Reflection.getStaticValue(CORE_LESS_THAN.class   , "ID").toString());
		assertEquals(">", Reflection.getStaticValue(CORE_GREATER_THAN.class, "ID").toString());
	}

	@Test
	public void testGreaterThan() {
		Patch p = Library.core.loadInstance(Node.toNode(">"));
		p.replace(0, 5); p.replace(2, 9); // 5 > 9
		assertEquals(Symbol.False.class, p.getResult().getClass());
		p.replace(0, -3); p.replace(2, -12); // -3 > -12
		assertEquals(Symbol.True.class, p.getResult().getClass());
	}

	@Test
	public void testLessThan() {
		Patch p = Library.core.loadInstance(Node.toNode("<"));
		p.replace(0, 5); p.replace(2, 9); // 5 < 9
		assertEquals(Symbol.True.class, p.getResult().getClass());
		p.replace(0, -3); p.replace(2, -12); // -3 < -12
		assertEquals(Symbol.False.class, p.getResult().getClass());
	}

	@Test
	public void testDivision() {
		Patch p = Library.core.loadInstance(Node.toNode("/"));
		p.replace(0, 5); p.replace(2, 2); // {5 / 2}
		assertEquals("2.5", p.getResult().toString()); // int -> float

		p.replace(0, 4.4f); p.replace(2, 2.2f); // {4 / 2}
		assertEquals("{4.4 / 2.2}", p.toString());
		assertEquals("2", p.getResult().toString());
		assertEquals(Int32.class, p.getResult().getClass()); // flaot -> int
	}

}
