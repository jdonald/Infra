package jiyuiydi.infra;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;

import jiyuiydi.util.BufferedBuffer;

enum Type {
	FREE, BYTES, UTF8, INTEGER, FLOATING,        // Leaves      [ 0 -  4]
	LIST, KEYED, PATCH, META, CONTINUATION,      // Containers  [ 5 -  9]
	UNALLOCATED_A, UNALLOCATED_B, UNALLOCATED_C, // Unallocated [10 - 12]
	BITS, NIBBLE, SYMBOL,                        // Special     [13 - 15]
	;
	static enum Symbol {
		FALSE, TRUE, VOID, NULL, ANY,
		PARAMETER, ERROR, CONSTANT,
	}
	boolean isContainer() {
		switch(this) {
			case LIST: case KEYED: case PATCH: case CONTINUATION: case META: return true;
			default: return false;
		}
	}
}

class HeaderTree {

	Header me;
	ArrayList<HeaderTree> subTrees;
	HeaderTree metadataTree;

	HeaderTree(Node n) {
		me = Header.make(n);
		if(n.metadata != null) {
			metadataTree = new HeaderTree(n.metadata);
			if(me.type.isContainer())
				me.setContentByteLength(me.contentByteLength + metadataTree.getTotal());
		}
		for(int i = 0; i < n.count(); ++i)
			add(new HeaderTree(n.get(i)));
	}

	void add(HeaderTree ht) {
		if(subTrees == null) subTrees = new ArrayList<>();
		subTrees.add(ht);
		me.setContentByteLength(me.contentByteLength + ht.getTotal());
	}

	long getTotal() {
		return me.getSegmentByteLength()
				+
				(metadataTree == null || me.type.isContainer() // if container, metadata segment length is already included as a content child
					? 0
					: metadataTree.getTotal());
		}

}

//class HeaderTreeLive extends HeaderTree {
//
//	HeaderTreeLive parent;
//	BufferedBuffer storage;
//	long storagePosition;
//	int trailingFreeSpace;
//
//	public HeaderTreeLive() {
//		
//	}
//
//}

public abstract class InfraEncoding {

	static public BufferedBuffer encoded(Node n) {
		HeaderTree ht = new HeaderTree(n);
		BufferedBuffer bb = BufferedBuffer.allocate((int) ht.getTotal());
		encode(n, bb, ht);
		bb.position(0L);
		return bb;
	}

	static public boolean encode(Node n, BufferedBuffer bb, HeaderTree ht) {
		//if(bb.remaining() < ht.getTotal()) return false;

		ht.me.encode(bb); // write header

		// write content body
		Class<? extends Node> nc = n.getClass();
		if(nc == Int32.class) {
			int v = ((Int32) n).value;
			if(ht.me.contentByteLength == 4) bb.write (        v);
			if(ht.me.contentByteLength == 1) bb.write ((byte)  v);
			if(ht.me.contentByteLength == 2) bb.write ((short) v);
			if(ht.me.contentByteLength == 3) bb.write3(        v);
		}
		else if(nc == Float32.class) {
			float v = ((Float32) n).value;
			if(ht.me.contentByteLength == 4) bb.write(v);
		}
		else if(nc == UTF8.class) bb.write(((UTF8) n).value);

		if(n.metadata != null) // write metadata after header. if container, this will be included in content size as the first child
			encode(n.metadata, bb, ht.metadataTree);

		for(int i = 0; i < n.count(); ++i)
			encode(n.get(i), bb, ht.subTrees.get(i));

		return true;
	}

	static public Node decode(BufferedBuffer bb) {
		long origin = bb.position();
		Header h;
		do {
			h = Header.decode(bb);
			if(h == null) return null;
		} while(h.type == Type.FREE);
		if(bb.remaining() < h.contentByteLength) { bb.position(origin); return null; }

		Node node = null;

		switch(h.type) { // Leaves
			case BYTES: node = new Bytes(bb, h.contentByteLength); break;
			case UTF8: node = new UTF8(bb.readString((int) h.contentByteLength)); break;
			case INTEGER:
				if(h.contentByteLength == 4) { node = new Int32(bb.read4()); break; }
				if(h.contentByteLength == 1) { node = new Int32(bb.read1()); break; }
				if(h.contentByteLength == 2) { node = new Int32(bb.read2()); break; }
				if(h.contentByteLength == 3) { node = new Int32(bb.read3()); break; }
				break;
			case FLOATING: if(h.contentByteLength == 4) node = new Float32(bb.read4floating()); break;
			case BITS: break;
			case NIBBLE: node = new Int32(((Header.Nibble) h).immediate); break;
			case SYMBOL: node = decode(((Header.Symbol) h).sym); break;
			default: break; // try containers next
		}

		if(node == null) { // Containers
			Box container = null;
			switch(h.type) {
				case META:         container = new Metadata(); break;
				case CONTINUATION: container = new Box     (); break;
				case LIST:         container = new Box     (); break;
				case KEYED:        container = new Keyed   (); break;
				case PATCH:        container = new Patch   (); break;
				default: return null;
			}
			long endContainer = bb.position() + (int) h.contentByteLength;
			Node previous = container;
			while(bb.position() < endContainer) {
				Node child = decode(bb);
				if(child == null) return node; // bad data
				if(child instanceof Metadata) previous.metadata = (Metadata) child;
				else                          container.add(child);
				previous = child;
			}
			node = container;
		}

		if(bb.remaining() > 0) { // Check for trailing metadata
			byte peek = bb.read1();
			bb.seek(-1); // restore position
			if((peek & 0xF0) == (Type.META.ordinal() << 4)) {
				node.metadata = (Metadata) decode(bb);
			}
		}

		return node;
	}

	static public Node decode(Type.Symbol sym) {
		switch(sym) { // Symbol classes must be instanced so they can have unique metadata
			default:
			case FALSE:     return new Symbol.False    ();
			case TRUE:      return new Symbol.True     ();
			case VOID:      return new Symbol.Void     ();
			case NULL:      return new Symbol.Null     ();
			case ANY:       return new Symbol.Any      ();
			case PARAMETER: return new Symbol.Parameter();
			case ERROR:     return new Symbol.Error    (); // especially likely to have metadata
			case CONSTANT:  return new Symbol.Constant (); // especially likely to have metadata
		}
	}

	static public void save(Node n, Path path) throws IOException { save(n, path, 64*1024); }

	static public void save(Node n, Path path, int chunkSize) throws IOException {
		HeaderTree ht = new HeaderTree(n);
		chunkSize = (int) Math.min(ht.getTotal(), chunkSize);
		BufferedBuffer bb = BufferedBuffer.fileWrite(path, chunkSize);
		encode(n, bb, ht);
		bb.finish();
	}

	static public Node load(Path path) throws IOException {
		BufferedBuffer bb = BufferedBuffer.fileRead(path);
		Node tree = decode(bb);
		bb.finish();
		return tree;
	}

//	static public void save(Node n, Path path) throws IOException {
//		FileChannel fc = FileChannel.open(path, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
//		BufferedBuffer bb = encoded(n);
//		fc.write(bb.read((int) bb.remaining())); // write all at once
//		fc.close();
//	}
//
//	static public Node load(Path path) throws IOException {
//		FileChannel fc = FileChannel.open(path, StandardOpenOption.READ);
//		ByteBuffer bb = ByteBuffer.allocate((int) fc.size()); // read all at once
//		fc.read(bb);
//		fc.close();
//		bb.clear();
//		return decode(BufferedBuffer.wrap(bb));
//	}
//
//	static final int minimumChunkSize = 1+8; // header for a 64bit content-length-value
//	static int defaultStreamChunkSize = 1024 * 1024;
//
//	static public void streamingSave(Node n, Path path) throws IOException { streamingSave(n, path, defaultStreamChunkSize); }
//
//	static public void streamingSave(Node n, Path path, int streamChunkSize) throws IOException {
//		FileChannel fc = FileChannel.open(path, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
//		HeaderTree ht = new HeaderTree(n);
//		BufferedBuffer bb = BufferedBuffer.allocate(streamChunkSize < ht.getTotal() ? streamChunkSize : (int) ht.getTotal());
//		streamingSave(n, ht, bb, fc, streamChunkSize);
//		fc.close();
//	}
//
//	static private void streamingSave(Node n, HeaderTree ht, BufferedBuffer bb, FileChannel fc, int streamChunkSize) throws IOException {
//		if(ht.getTotal() <= streamChunkSize) {
//			encode(n, bb, ht);                           bb.position(0L);
//			fc.write(bb.read((int) bb.remaining()));
//			bb.position(0L);
//			return;
//		}
//		if(n.count() == 0) { // huge leaf?
//			BufferedBuffer bb8 = encoded(n);
//			fc.write(bb8.read((int) bb8.remaining()));
//			return;
//		}
//		ht.me.encode(bb); bb.position(0L);
//		fc.write(bb.read((int) bb.remaining()));     bb.position(0L);
//		for(int i = 0; i < n.count(); ++i)
//			streamingSave(n.get(i), ht.subTrees.get(i), bb, fc, streamChunkSize);
//		if(n.metadata != null)
//			streamingSave(n.metadata, ht.metadataTree, bb, fc, streamChunkSize);
//	}

}
