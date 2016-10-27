package jiyuiydi.infra.ui;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;

import jiyuiydi.infra.Opcode;
import jiyuiydi.infra.UTF8;
import jiyuiydi.infra.ui.AutoCompleter;

public class AutoCompleterTest {

	@Test
	public void testStrings() {
		AutoCompleter ac = new AutoCompleter(Opcode.class);
		assertEquals(Opcode.values().length, ac.set.size());

		List<Object> r = ac.getMatches(new UTF8("VAL"));
		assertEquals(1, r.size());
		assertEquals(Opcode.VALUE, r.get(0));
	}
	
}
