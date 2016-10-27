package jiyuiydi.util;

import static org.junit.Assert.*;

import java.math.BigInteger;
import java.nio.ByteBuffer;

import org.junit.Test;

public class VarUintTest {

	@Test
	public void testOctet() {
		assertEquals(0, VarUint.octet(""));
		assertEquals(1, VarUint.octet("1"));
		assertEquals(2, VarUint.octet("10"));
		assertEquals(2, VarUint.octet("1 0"));
	}

	@Test
	public void testTable() {
		assertEquals((byte) 0x00, VarUint.table[0].prefix);
		assertEquals((byte) 0x80, VarUint.table[1].prefix);
		assertEquals((byte) 0xC0, VarUint.table[2].prefix);
		assertEquals((byte) 0xE0, VarUint.table[3].prefix);
		assertEquals((byte) 0xE8, VarUint.table[4].prefix);

		assertEquals((long) Math.pow(2,  7)-1, VarUint.table[0].maxVal);
		assertEquals((long) Math.pow(2, 14)-1, VarUint.table[1].maxVal);
		assertEquals((long) Math.pow(2, 21)-1, VarUint.table[2].maxVal);
		assertEquals((long) Math.pow(2, 27)-1, VarUint.table[3].maxVal);
		assertEquals((long) Math.pow(2, 35)-1, VarUint.table[4].maxVal);
		assertEquals((long) Math.pow(2, 59)-1, VarUint.table[5].maxVal);
		assertEquals((long) Math.pow(2, 40)-1, VarUint.table[6].maxVal);
	}

	@Test
	public void testEncodingLengthForQuantity() {
		assertEquals(1, VarUint.getEncodingLengthForQuantity(0));
		assertEquals(1, VarUint.getEncodingLengthForQuantity(1));
		assertEquals(1, VarUint.getEncodingLengthForQuantity(127));
		assertEquals(2, VarUint.getEncodingLengthForQuantity(128));
		assertEquals(6, VarUint.getEncodingLengthForQuantity(34_359_738_368L));
	}

	@Test
	public void testEncodingLengthForBigQuantity() {
		BigInteger b64 = new BigInteger("18446744073709551615");
		BigInteger b65 = new BigInteger("18446744073709551616");
		assertEquals(64, b64.bitLength());
		assertEquals(65, b65.bitLength());
		assertEquals( 8+1, VarUint.getEncodingLengthForQuantity(b64));
		assertEquals(16+1, VarUint.getEncodingLengthForQuantity(b65));
	}

	@Test
	public void testRoundTrip() {
		testRoundTripRange(             0L,         17_000L); // 1 - 3 byte
		testRoundTripRange(     2_097_000L,      2_098_000L); // 2 - 3 byte
		testRoundTripRange(   134_217_000L,    134_218_000L); // 3 - 4 byte
		testRoundTripRange( 4_294_967_296L,  4_304_218_000L); //     5 byte (33 bit)
		testRoundTripRange(34_359_738_368L, 34_359_738_368L); //     6 byte
		testRoundTripRange(34_359_738_300L, 34_359_738_400L); // 5 - 6 byte
	}

	private void testRoundTripRange(long lo, long hi) {
		for(long i = lo; i <= hi; ++i) {
			BufferedBuffer bb = BufferedBuffer.allocate(VarUint.getEncodingLengthForQuantity(i));
			VarUint.encode(i, bb);
			if(bb.remaining() != 0)
				System.out.println(i);
			assertEquals(0, bb.remaining());
			bb.position(0L);
			assertEquals(i, VarUint.decode(bb));
			assertEquals(0, bb.remaining());
		}
	}

	@Test
	public void testRoundTripBig() {
		BigInteger b64 = new BigInteger("18446744073709551615");
		BigInteger b65 = new BigInteger("18446744073709551616");
		testRoundTripBigValue(b64);
		testRoundTripBigValue(b65);
	}

	private void testRoundTripBigValue(BigInteger bi) {
		BufferedBuffer bb = BufferedBuffer.allocate(VarUint.getEncodingLengthForQuantity(bi));
		VarUint.encode(bi, bb);
		assertEquals(0, bb.remaining());
		bb.position(0);
		assertEquals(bi, VarUint.decodeBig(bb));
		assertEquals(0, bb.remaining());
	}

}
