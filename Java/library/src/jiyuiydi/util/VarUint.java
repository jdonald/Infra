package jiyuiydi.util;

import java.math.BigInteger;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

/*
 * Implementation of
 * J. M. Dlugosz’ Variable-Length Integer Encoding - Revision 2.
 * http://www.dlugosz.com/ZIP2/VLI.html
 */
public class VarUint {

	static class Row {
		final byte prefix; final int byteCount, dataBits; final long maxVal;
		Row(byte p, int c, int b, long m) { prefix = p; byteCount = c; dataBits = b; maxVal = m; }
	}
	static final Row[] table = {
			//             prefix-bits bytes data-bits           max-value
			new Row(octet("0__ __ ___"),  1,   7,                     127L), // 0
			new Row(octet("10_ __ ___"),  2,  14,                  16_383L), // 1
			new Row(octet("110 __ ___"),  3,  21,               2_097_151L), // 2
			new Row(octet("111 00 ___"),  4,  27,             134_217_727L), // 3
			new Row(octet("111 01 ___"),  5,  35,          34_359_738_367L), // 4
			new Row(octet("111 10 ___"),  8,  59, 576_460_752_303_423_487L), // 5
			new Row(octet("111 11 000"),  6,  40,       1_099_511_627_775L), // 6
			new Row(octet("111 11 001"),  9,  64,                       0L), // 7
			new Row(octet("111 11 010"), 17, 128,                       0L), // 8
			new Row(octet("111 11 111"), -1,  -1,                       0L), // 9
	};
	static final int[] ordering_maxValue = {0, 1, 2, 3, 4, 6, 5, 7, 8, 9};

	static public int getEncodingLengthForQuantity(long v) {
		if(v < 0) return table[7].byteCount; // 64bit data
		for(int i = 0; i < ordering_maxValue.length; ++i)
			if(v <= table[ordering_maxValue[i]].maxVal)
				return table[ordering_maxValue[i]].byteCount;
		return -1;
	}

	static public int getEncodingLengthForQuantity(BigInteger bi) {
		int b = bi.bitLength();
		if(b < 64) return getEncodingLengthForQuantity(bi.longValue());
		if(b <= table[7].dataBits) return table[7].byteCount;
		if(b <= table[8].dataBits) return table[8].byteCount;
		return (int) Math.ceil(b/8.0) + 2; // one for prefix and one for byte indicating length
	}

	static public void encode(long val, BufferedBuffer bb) {
		// 9-bytes (byte+long) - This comes first because longs cannot be unsigned in Java
		if(val < 0) {                bb.write(          table[7].prefix                     );
		                             bb.write(                                   val        ); return; }
		// 1-byte
		if(val <= table[0].maxVal) { bb.write((byte ) ( table[0].prefix       +  val       )); return; }
		// 2-byte (short)
		if(val <= table[1].maxVal) { bb.write((short) ((table[1].prefix << 8) +  val       )); return; }
		// 3-byte (byte+short)
		if(val <= table[2].maxVal) { bb.write((byte ) ( table[2].prefix       + (val >> 16)));
		                             bb.write((short)                            val        ); return; }
		// 4-byte (int)
		if(val <= table[3].maxVal) { bb.write((int)   ((table[3].prefix << 24)+  val       )); return; }
		// 5-byte (byte+int)
		if(val <= table[4].maxVal) { bb.write((byte)  ( table[4].prefix       + (val >> 32)));
		                             bb.write((int)                              val        ); return; }
		// 6-byte (short+int)
		if(val <= table[6].maxVal) { bb.write((short) ((table[6].prefix << 8) + (val >> 32)));
		                             bb.write((int)                              val        ); return; }
		// 8-byte (long)
		if(val <= table[5].maxVal) { bb.write(        ( table[5].prefix << 56)+  val        ); return; }
	}

	static public void encode(BigInteger bi, BufferedBuffer bb) {
		if(bi.compareTo(BigInteger.ONE) < 0) bi = BigInteger.ZERO;
		int b = bi.bitLength();
		if(b < 64) { encode(bi.longValue(), bb); return; }
		byte[] bytes = bi.toByteArray(); // big-endian
		int dataBytes;
		/**/ if(b <= table[7].dataBits) { bb.write(table[7].prefix); dataBytes = 8; }
		else if(b <= table[8].dataBits) { bb.write(table[8].prefix); dataBytes = 16; }
		else                            bb.write((byte) (dataBytes = bytes.length));
		ByteBuffer biBB = ByteBuffer.wrap(bytes);
		while(biBB.get() == 0); // skip possible zero byte due to space for the sign bit
		biBB.position(biBB.position() - 1);
		bb.writeZero(dataBytes - biBB.remaining());
		bb.write(biBB);
		//int firstNonZero = 0; while(bytes[firstNonZero] == 0) ++firstNonZero; // trim sign bit
		//while(bytes.length - firstNonZero < dataBytes--) bb.put((byte) 0);
		//bb.write(bytes, firstNonZero, bytes.length - firstNonZero);
	}

	static public long decode(BufferedBuffer bb) {
		byte prefix = bb.read1();
		if(prefix >= table[0].prefix)
			return assemble(bb, prefix, table[0]);
		for(int i = 1; i <= 6; ++i)
			if(prefix < table[i+1].prefix)
				return assemble(bb, prefix, table[i]);
		bb.position(bb.position() - 1); // reset position (didn't read)
		return -1;
	}
	
	static public BigInteger decodeBig(BufferedBuffer bb) {
		byte prefix = bb.read1();
		int dataBytes;
		/**/ if(prefix == table[7].prefix) dataBytes = table[7].byteCount - 1;
		else if(prefix == table[8].prefix) dataBytes = table[8].byteCount - 1;
		else if(prefix == table[9].prefix) dataBytes = bb.read4();
		else {
			bb.position(bb.position() - 1);
			long val = decode(bb);
			return new BigInteger(String.valueOf(val));
		}
		byte[] bytes = new byte[dataBytes + 1]; // leave space to ensure positive interpretation
		ByteBuffer buf = bb.read(dataBytes);
		buf.get(bytes, 1, dataBytes);
		return new BigInteger(bytes);
	}

	static private long assemble(BufferedBuffer bb, byte first, Row r) {
		long val = first - r.prefix; // first byte portion
		for(int i = 1; i < r.byteCount; ++i) { // rest of whole bytes
			val <<= 8;
			val += bb.read1unsigned();
		}
		return val;
	}

	static byte octet(String bits) {
		byte b = 0; int place = 0, l = bits.length();
		for(int i = 0; i < l; ++i) {
			char ch = bits.charAt(l-i-1);
			if(ch == '1') b += Math.pow(2, place);
			if(ch != ' ') ++place;
		}
		return b;
	}

}
