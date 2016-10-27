package jiyuiydi.infra;

import java.nio.BufferUnderflowException;

import jiyuiydi.util.BufferedBuffer;
import jiyuiydi.util.VarUint;

public class Header {

	static public Header make(Node n) {
		Class<? extends Node> nc = n.getClass(); // Exact class match also allows sorting these checks by frequency

		if(nc == Int32.class) { // The most common leaf in high-efficiency use cases?
			int v = ((Int32) n).value;
			if(v == 0) return Header.nibble0;
			if(v == 1) return Header.nibble1;
			if(v >= 0 && v <= 14) return new Header.Nibble((byte) v);
			if(v >= -128 && v <= 127) return Header.int1;
			if(v >= -32768 && v <= 32767) return new Header(Type.INTEGER, 2);
			return Header.int4;
		}
		if(nc == Float32.class) return Header.float4;
		if(nc == UTF8   .class) return new Header(Type.UTF8, ((UTF8) n).value.getBytes().length);
		if(nc == Int64  .class) return Header.int8;
		if(nc == Float64.class) return Header.float8;

		// Containers must have their content length filled in later
		if(nc == Box.class     ) return new Header(Type.LIST );
		if(nc == Keyed.class   ) return new Header(Type.KEYED);
		if(nc == Command.class ) return new Header(Type.KEYED);
		if(nc == Patch.class   ) return new Header(Type.PATCH);
		if(nc == Metadata.class) return new Header(Type.META );

		if(n instanceof jiyuiydi.infra.Symbol) return new Symbol((jiyuiydi.infra.Symbol) n);
		return new Header(Type.FREE);
	}

	static final Header nibble0 = new Header.Nibble((byte) 0); // frequently occurring headers
	static final Header nibble1 = new Header.Nibble((byte) 1);
	static final Header int1   = new Header(Type.INTEGER , 1);
	static final Header int4   = new Header(Type.INTEGER , 4);
	static final Header float4 = new Header(Type.FLOATING, 4);
	static final Header int8   = new Header(Type.INTEGER , 8);
	static final Header float8 = new Header(Type.FLOATING, 8);

	static public Header decode(BufferedBuffer bb) {
		try {
			if(bb.remaining() == 0) return null;
			long startPosition = bb.position();
			int b = bb.read1unsigned();
			Type t = Type.values()[b >>> 4];
			if(t.ordinal() >= 13) { // SYMBOL, BITS, or NIBBLE
				if(t == Type.NIBBLE) return new Nibble((byte) (b & 0x0F));
				if(t == Type.SYMBOL) return new Symbol(Type.Symbol.values()[b & 0x0F]);
				if(t == Type.BITS)   return new Bits(((byte) (b & 0x0F)), bb);
			}
			Header h = new Header();
			h.type = t;
			h.contentByteLength = b & 0x0F;
			if(h.contentByteLength == 15) {
				h.contentByteLength = VarUint.decode(bb);
				if(h.contentByteLength < 0) { bb.position(startPosition); return null; }
			}
			h.headerByteSize = (int) (bb.position() - startPosition);
			return h;
		}
		catch(BufferUnderflowException e) {}
		return null;
	}

	// instance //////////////////////////////////////////////////////////

	Type type;
	int headerByteSize;
	long contentByteLength;

	protected Header() {}

	public Header(Type t) { type = t; headerByteSize = 1; }

	public Header(Type t, long contentByteLength) { type = t; setContentByteLength(contentByteLength); }

	public int getHeaderByteLength() { return headerByteSize; }
	public long getContentByteLength() { return contentByteLength; }

	public void setContentByteLength(long cbl) {
		if(type.ordinal() >= 13) return; // not SYMBOL, BITS, or NIBBLE (contentByteLength stays 0)
		contentByteLength = cbl;
		if(contentByteLength < 15) headerByteSize = 1;
		else                       headerByteSize = 1 + VarUint.getEncodingLengthForQuantity(contentByteLength);
	}

	public long getSegmentByteLength() { return headerByteSize + contentByteLength; }

	public Node constructNode() {
		switch(type) {
			case BYTES: return new Bytes();
			case UTF8: return new UTF8();
			case INTEGER:
				if(contentByteLength <= 4) return new Int32();
				if(contentByteLength <= 8) return new Int64();
				return null;
			case FLOATING:
				if(contentByteLength <= 4) return new Float32();
				if(contentByteLength <= 8) return new Float64();
				return null;

			case BITS: break;
			case NIBBLE: return new Int32(((Nibble) this).immediate);
			case SYMBOL: return InfraEncoding.decode(((Symbol) this).sym);

			case META: return new Metadata();
			case CONTINUATION: // pass-through to LIST
			case LIST: return new Box();
			case KEYED: return new Keyed();
			case PATCH: return new Patch();

			case FREE: break;
			case UNALLOCATED_A: break;
			case UNALLOCATED_B: break;
			case UNALLOCATED_C: break;
		}
		return null;
	}

	@Override public String toString() { return headerByteSize + "-byte " + type.toString() + " header. body length: " + contentByteLength + "."; }

	public boolean encode(BufferedBuffer bb) {
		if(bb.remaining() < headerByteSize) return false;
		if(contentByteLength < 15) {
			bb.write((byte) ((type.ordinal() << 4) | contentByteLength));
		} else {
			bb.write((byte) ((type.ordinal() << 4) | 0x0F));
			VarUint.encode(contentByteLength, bb);
		}
		return true;
	}

	// modules ///////////////////////////////////////////////////////////

	static class Nibble extends Header {
		byte immediate;
		public Nibble(byte val0to15) { super(Type.NIBBLE); immediate = val0to15; }
		@Override public boolean encode(BufferedBuffer bb) {
			if(bb.remaining() < 1) return false;
			bb.write((byte) ((Type.NIBBLE.ordinal() << 4) | immediate));
			return true;
		}
	}

	static class Symbol extends Header {

		Type.Symbol sym;

		public Symbol(Type.Symbol s) { super(Type.SYMBOL); sym = s; }

		public Symbol(jiyuiydi.infra.Symbol n) {
			super(Type.SYMBOL);
			Class<? extends jiyuiydi.infra.Symbol> nc = n.getClass();
			/**/ if(nc == jiyuiydi.infra.Symbol.False    .class) sym = Type.Symbol.FALSE;
			else if(nc == jiyuiydi.infra.Symbol.True     .class) sym = Type.Symbol.TRUE;
			else if(nc == jiyuiydi.infra.Symbol.Void     .class) sym = Type.Symbol.VOID;
			else if(nc == jiyuiydi.infra.Symbol.Null     .class) sym = Type.Symbol.NULL;
			else if(nc == jiyuiydi.infra.Symbol.Any      .class) sym = Type.Symbol.ANY;
			else if(nc == jiyuiydi.infra.Symbol.Parameter.class) sym = Type.Symbol.PARAMETER;
			else if(nc == jiyuiydi.infra.Symbol.Error    .class) sym = Type.Symbol.ERROR;
			else if(nc == jiyuiydi.infra.Symbol.Constant .class) sym = Type.Symbol.CONSTANT;
		}

		@Override public boolean encode(BufferedBuffer bb) {
			if(bb.remaining() < 1) return false;
			bb.write((byte) ((Type.SYMBOL.ordinal() << 4) | sym.ordinal()));
			return true;
		}

	}

	static class Bits extends Header {
		public Bits(byte immediate, BufferedBuffer rest) {
			super(Type.BITS);
			// TODO
		}
		@Override public boolean encode(BufferedBuffer bb) {
			// TODO
			return false;
		}
	}

}
