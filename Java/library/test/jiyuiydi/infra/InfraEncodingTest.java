package jiyuiydi.infra;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;

import org.junit.Test;

import jiyuiydi.infra.Box;
import jiyuiydi.infra.HeaderTree;
import jiyuiydi.infra.Int32;
import jiyuiydi.infra.Keyed;
import jiyuiydi.infra.Metadata;
import jiyuiydi.infra.Node;
import jiyuiydi.infra.UTF8;
import jiyuiydi.infra.markupModels.Encoding;
import jiyuiydi.util.BufferedBuffer;
import jiyuiydi.util.Reflection;
import jiyuiydi.util.Utilities;

public class InfraEncodingTest {

	@Test
	public void testHeaderTreeTotal() {
		Node tree = new UTF8("hi");
		assertEquals(1+2, new HeaderTree(tree).getTotal());

		tree = new Int32(7);
		assertEquals(1, new HeaderTree(tree).getTotal());

		tree = new Int32(70);
		assertEquals(2, new HeaderTree(tree).getTotal());

		tree = new Box(new UTF8("fish"), new Box(new Keyed(1, "red"), new Keyed(2, true)));
		int byteCount = 2+(5 + 1+(1+(1+4) + 1+(1+1))); // 17
		assertEquals(byteCount, new HeaderTree(tree).getTotal());

		tree.with(Metadata.lang_ID, new UTF8("Seuss"));
		int mdByteCount = 1 + 1+(3 + 6); // meta(keyed('ID', 'Seuss'))
		assertEquals(byteCount + mdByteCount, new HeaderTree(tree).getTotal());
	}

	@Test
	public void testRoundTrip() {
		testRoundTripTree(new UTF8(""));
		testRoundTripTree(new UTF8("hi"));
		testRoundTripTree(new Int32(0));
		testRoundTripTree(new Int32(1));
		testRoundTripTree(new Int32(-1));
		testRoundTripTree(new Int32(127));
		testRoundTripTree(new Int32(128));
		testRoundTripTree(new Box());
		testRoundTripTree(new Box(1, 2));
		testRoundTripTree(new Box(new Keyed(1, "red"), new Keyed(2, "blue")));

		// Test symbol class encoding and decoding (should automatically scale as Symbol classes are added)
		Class<? extends Symbol>[] symbolClasses = Reflection.getAllProperSublassesOf(getClass().getPackage().getName(), Symbol.class);
		assertTrue(symbolClasses.length >= 8); // There were 8 symbol classes at the time of writing this test
		for(Class<? extends Symbol> sc : symbolClasses)
			try { testRoundTripTree(sc.newInstance()); }
			catch(InstantiationException | IllegalAccessException e) { e.printStackTrace(); }

		// Test metadata
		testRoundTripTree(new UTF8("").with(Metadata.lang_ID, "a"));
		testRoundTripTree(new UTF8("").with(Metadata.lang_ID, "a").with(Metadata.lang_version, 1));

		// Test metadata-metadata
		Node a = new UTF8("").with(Metadata.lang_ID, "a");
		a.getMetadata().with(Metadata.lang_version, 2);
		assertEquals("`' w/(ID:a) w/(version:2)", a.toString());
		testRoundTripTree(a);
	}

	private void testRoundTripTree(Node n) {
		BufferedBuffer bb = InfraEncoding.encoded(n);
		Node n2 = InfraEncoding.decode(bb);
		assertTrue(n.equalsWithMetadata(n2));
	}

	@Test
	public void testSaveLoad() throws IOException {
		Node tree = new Box(new Keyed(1, "red"), new Keyed(2, "blue"));

		File temp = File.createTempFile("testSaveLoad", ".infra");
		try {
			Path path = FileSystems.getDefault().getPath(temp.getPath());
			InfraEncoding.save(tree, path);
			Node tree2 = InfraEncoding.load(path);
			assertEquals(tree, tree2);
		} finally { temp.delete(); }
	}

	@Test
	public void testStreamingSave() throws IOException {
		Node tree = new Box(new Keyed(1, "red"), new Keyed(2, "blue"));
		File temp = File.createTempFile("testSaveLoad", "infra");
		Path path = Utilities.getPath(temp);
		int streamChunkSize = 9; // minimumStreamChunkSize
		InfraEncoding.save(tree, path, streamChunkSize);
		Node tree2 = InfraEncoding.load(path);
		assertEquals(tree, tree2);
	}

	@Test
	public void testFreePadding() {
		Node tree = Encoding.padTo(new UTF8("hi"), 5);
		assertEquals("hi w/(encoding:[`pad to':5])", tree.toString());
		BufferedBuffer bb = InfraEncoding.encoded(tree);
		while(bb.remaining() > 0)
			System.out.println((char)(bb.read1unsigned()));
		assertEquals(5, bb.remaining());
	}

//	@Test
//	public void testByteArray() {
//		Node type = new UTF8().with(Metadata.lang_encodingHints, new Keyed("pad to", 4));
//		Node n = new ByteArray(new byte[] {'g', 'o', 'o', 'd'});
//		n.with(Metadata.lang_type, type);
//		assertTrue(false);
//	}

}
