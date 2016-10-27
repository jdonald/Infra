package jiyuiydi.util;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.function.Supplier;

import org.junit.Test;

public class BufferedBufferTest {

	@Test
	public void testSmallDirect() {
		BufferedBuffer bb = BufferedBuffer.allocate(8);
		assertEquals(BufferedBuffer_SmallDirect.class, bb.getClass());

		/*1-byte*/ bb.write ((byte)    255); confirm(bb, 1, bb::read1unsigned,           255);
		/*2-byte*/ bb.write ((short)   256); confirm(bb, 2, bb::read2        , (short)   256);
		/*3-byte*/ bb.write3(0x01_40_00_00); confirm(bb, 3, bb::read3        , 0x00_40_00_00);
		/*4-byte*/ bb.write(12345);          confirm(bb, 4, bb::read4        ,         12345);
		/*8-byte*/ bb.write(12345L);         confirm(bb, 8, bb::read8        ,        12345L);
		/*float */ bb.write(1.1f);           confirm(bb, 4, bb::read4floating,          1.1f);
		/*double*/ bb.write(1.1);            confirm(bb, 8, bb::read8floating,           1.1);

		ByteBuffer chunk = bb.read(4);
		assertEquals(4, chunk.remaining());
	}

	private <T> void confirm(BufferedBuffer bb, int byteSize, Supplier<T> readFunc, T answer) {
		assertEquals(byteSize, bb.position());
		bb.seek(-byteSize);
		assertEquals(answer, readFunc.get());
		assertEquals(byteSize, bb.position());
		bb.position(0);
	}

	@Test
	public void testPaging() {
		// TODO
	}

	@Test
	public void testFile() throws IOException {
		File temp = File.createTempFile("BufferedBufferTest", ".tmp");
		BufferedBuffer bb;

		bb = BufferedBuffer.fileWrite(Utilities.getPath(temp));
		try {
			assertEquals(BufferedBuffer_File.class, bb.getClass());
			((BufferedBuffer_File) bb).setChunkSize(10);
			bb.write ((byte)    255);
			bb.write ((short)   256);
			bb.write3(0x01_40_00_00);
			bb.write(12345);
			bb.write(12345L);
			bb.write(1.1f);
			bb.write(1.1);
		} finally { bb.finish(); }

		bb = BufferedBuffer.fileRead(Utilities.getPath(temp));
		try {
			assertEquals(BufferedBuffer_File.class, bb.getClass());
			((BufferedBuffer_File) bb).setChunkSize(10);
			assertEquals(          255, bb.read1unsigned());
			assertEquals((short)   256, bb.read2());
			assertEquals(0x00_40_00_00, bb.read3());
			assertEquals(        12345, bb.read4());
			assertEquals(       12345L, bb.read8());
			assertEquals(         1.1f, bb.read4floating(), 0f);
			assertEquals(          1.1, bb.read8floating(), 0.0);
		} finally { bb.finish(); }
	}

}
