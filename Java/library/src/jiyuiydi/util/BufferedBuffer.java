package jiyuiydi.util;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public abstract class BufferedBuffer {

	static final int maxArraySize = Integer.MAX_VALUE - 8; // 8 is largest JVM object header size across implementations

	static public BufferedBuffer wrap(ByteBuffer bb) { return new BufferedBuffer_SmallDirect(bb); }

	static public BufferedBuffer allocate(long capacity) {
		return (capacity <= maxArraySize)
				? new BufferedBuffer_SmallDirect((int) capacity)
				: new BufferedBuffer_Paging(capacity);
	}

	static public BufferedBuffer fileRead(Path path) throws IOException {
		final FileChannel fc = FileChannel.open(path, StandardOpenOption.READ);
		final int bufSize = (int) Math.min(BufferedBuffer_File.defaultStreamChunkSize, fc.size());
		return new BufferedBuffer_File(fc, bufSize);
	}

	static public BufferedBuffer fileWrite(Path path) throws IOException {
		return fileWrite(path, BufferedBuffer_File.defaultStreamChunkSize);
	}

	static public BufferedBuffer fileWrite(Path path, int chunkSize) throws IOException {
		final FileChannel fc = FileChannel.open(path, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
		chunkSize = Math.max(BufferedBuffer_File.minimumStreamChunkSize, chunkSize);
		BufferedBuffer_File bb = new BufferedBuffer_File(fc, chunkSize);
		bb.writeMode = true;
		return bb;
	}

	// instance //////////////////////////////////////////////////////////

	public abstract void finish   ();
	public abstract long remaining();
	public abstract long position ();
	public abstract void position (long newPosition);
	public          void seek     (long addPosition) { position(position() + addPosition); }

	public abstract byte       read1();
	public abstract short      read2();
	public          int        read3() { return (read2() << 8) /*pads with sign bit*/ + read1(); }
	public abstract int        read4();
	public abstract long       read8();
	public          int        read1unsigned() { return read1() & 0xFF; }
	public abstract float      read4floating();
	public abstract double     read8floating();
	public abstract ByteBuffer read(int byteCount);
	public abstract String     readString(int length);

	public abstract void write (byte       i);
	public abstract void write (short      i);
	public          void write3(int        i) { write((short) (i >>> 8)); write((byte) i); }
	public abstract void write (int        i);
	public abstract void write (long       i);
	public abstract void write (float      f);
	public abstract void write (double     f);
	public          void write (String     s) { write(ByteBuffer.wrap(s.getBytes())); }
	public abstract void write (ByteBuffer b);

	public void writeZero(int length) {
		while(length >= 8) { write(0L); length -= 8; }
		while(length >= 4) { write(0 ); length -= 4; }
		while(length-- > 0) write((byte) 0);
	}

}

class BufferedBuffer_SmallDirect extends BufferedBuffer {

	final ByteBuffer bb;

	BufferedBuffer_SmallDirect(int capacity) { bb = ByteBuffer.allocate(capacity); }
	public BufferedBuffer_SmallDirect(ByteBuffer b) { bb = b; }

	@Override public void finish   ()                 { bb.limit(bb.position()); }
	@Override public long remaining()                 { return bb.remaining(); }
	@Override public long position ()                 { return bb.position(); }
	@Override public void position (long newPosition) { bb.position((int) newPosition); }

	@Override public byte       read1()         { return bb.get      (); }
	@Override public short      read2()         { return bb.getShort (); }
	@Override public int        read4()         { return bb.getInt   (); }
	@Override public long       read8()         { return bb.getLong  (); }
	@Override public int        read1unsigned() { return bb.get      () & 0xFF; }
	@Override public float      read4floating() { return bb.getFloat (); }
	@Override public double     read8floating() { return bb.getDouble(); }
	@Override public ByteBuffer read(int n) {
		//ByteBuffer slice = bb.asReadOnlyBuffer();
		//slice.limit(slice.position() + n);
		ByteBuffer slice = bb.slice();
		slice.limit(n);
		seek(n);
		return slice;
	}
	@Override public String readString(int length) { seek(length); return new String(bb.array(), bb.position() - length, length); }

	@Override public void write(byte   i)     { bb.put(i); }
	@Override public void write(short  i)     { bb.putShort(i); }
	@Override public void write(int    i)     { bb.putInt(i); }
	@Override public void write(long   i)     { bb.putLong(i); }
	@Override public void write(float  f)     { bb.putFloat(f); }
	@Override public void write(double f)     { bb.putDouble(f); }
	@Override public void write(ByteBuffer b) { bb.put(b); }

}

class BufferedBuffer_Paging extends BufferedBuffer {

	static final int defaultPageSize = 64 * 1024; // 64KB

	final int pageSize;
	byte[][] page;
	int currentPageIndex;
	ByteBuffer currentPage, writeCache;

	BufferedBuffer_Paging(long initialCapacity) { pageSize = defaultPageSize; }
	BufferedBuffer_Paging() { pageSize = defaultPageSize; }

	@Override public void finish   ()                 {}					//TODO
	@Override public long remaining()                 { return 0; }			//TODO
	@Override public long position ()                 { return 0; }			//TODO
	@Override public void position (long newPosition) {}					//TODO

	@Override public byte       read1()         { return 0; }				//TODO
	@Override public short      read2()         { return 0; }				//TODO
	@Override public int        read3()         { return 0; }				//TODO
	@Override public int        read4()         { return 0; }				//TODO
	@Override public long       read8()         { return 0; }				//TODO
	@Override public float      read4floating() { return 0; }				//TODO
	@Override public double     read8floating() { return 0; }				//TODO
	@Override public ByteBuffer read(int n)     { return null; }			//TODO
	@Override public String     readString(int length) { return null; }		//TODO

	@Override public void write (byte   i) {}								//TODO
	@Override public void write (short  i) {}								//TODO
	@Override public void write3(int    i) {}								//TODO
	@Override public void write (int    i) {}								//TODO
	@Override public void write (long   i) {}								//TODO
	@Override public void write (float  f) {}								//TODO
	@Override public void write (double f) {}								//TODO
	@Override public void write (ByteBuffer b) {}							//TODO

}

class BufferedBuffer_File extends BufferedBuffer implements AutoCloseable {

	static final int minimumStreamChunkSize = 1+8;
	static final int defaultStreamChunkSize = 64 * 1024;

	// instance //////////////////////////////////////////////////////////

	private FileChannel fc;
	private ByteBuffer bb;
	boolean writeMode;

	public BufferedBuffer_File(FileChannel fc) { this(fc, defaultStreamChunkSize); }

	public BufferedBuffer_File(FileChannel fc, int streamChunkSize) {
		this.fc = fc;
		setChunkSize(streamChunkSize);
	}

	public void setChunkSize(int size) {
		bb = ByteBuffer.allocate(Math.max(size, minimumStreamChunkSize));
		bb.limit(0); // no byte values read yet buffered
	}

	@Override public long remaining() {
		if(writeMode) return Long.MAX_VALUE;
		try { return fc.size() - position(); }
		catch(IOException e) { return 0; }
	}

	@Override public long position() {
		try {
			if(writeMode) return fc.position() + bb.position();
			return fc.position() - bb.remaining();
		}
		catch(IOException e) { return 0; }
	}

	@Override public void position(long newPosition) {
		try {
			if(writeMode) {
				flush();
			} else {
				long shift = newPosition - position();
				if(shift > 0 && +shift <= bb.remaining()
				|| shift < 0 && -shift <= bb.position()) {
					bb.position(bb.position() + (int) shift);
				}
				else {
					bb.limit(0);
					fc.position(newPosition);
				}
			}

		}
		catch(IOException e) {}
	}

	@Override public ByteBuffer read(int byteCount) {
		prepareRead(byteCount);
		if(bb.remaining() < byteCount) return null; // uh-oh
		//ByteBuffer slice = bb.asReadOnlyBuffer();
		ByteBuffer slice = bb.slice();
		slice.limit(byteCount);
		return slice;
	}

	@Override public int    read1unsigned() { prepareRead(1); return bb.get() & 0xFF; }
	@Override public byte   read1 ()        { prepareRead(1); return bb.get(); }
	@Override public short  read2()         { prepareRead(2); return bb.getShort(); }
	@Override public int    read3()         { prepareRead(3); return (bb.getShort() << 8) /*pads with sign bit*/ + bb.get(); }
	@Override public int    read4()         { prepareRead(4); return bb.getInt(); }
	@Override public long   read8()         { prepareRead(8); return bb.getLong(); }
	@Override public float  read4floating() { prepareRead(4); return bb.getFloat(); }
	@Override public double read8floating() { prepareRead(8); return bb.getDouble(); }

	@Override public String readString(int length) {
		prepareRead(length);
		seek(length);
		return new String(bb.array(), bb.position() - length, length);
	}

	@Override public void write (ByteBuffer b) {
		if(writeMode) flush();
		try { fc.write(b); } catch(IOException e) {} // write directly to save making yet another copy
	}

	@Override public void write (byte   i) { prepareWrite(1); bb.put((byte) i); }
	@Override public void write (short  i) { prepareWrite(2); bb.putShort((short) i); }
	@Override public void write3(int    i) { prepareWrite(3); bb.putShort((short) (i >>> 8)); bb.put((byte) i); }
	@Override public void write (int    i) { prepareWrite(4); bb.putInt(i); }
	@Override public void write (long   i) { prepareWrite(8); bb.putLong(i); }
	@Override public void write (float  f) { prepareWrite(4); bb.putFloat(f); }
	@Override public void write (double f) { prepareWrite(8); bb.putDouble(f); }

	@Override public void finish() {
		if(writeMode) flush(); // flush the remainder of the buffer
		try { fc.close(); }
		catch(IOException e) { e.printStackTrace(); }
	}

	protected void prepareRead(int byteCount) {
		if(writeMode) { flush(); writeMode = false; } // leaves remaining() == 0
		if(bb.remaining() >= byteCount) return; // good to go
		// move the remainder to the front and load data up to capacity
		int shift = bb.position(), len = bb.remaining();
		bb.clear(); // limit = capacity
		byte[] b = bb.array();
		System.arraycopy(b, shift, b, 0, len);
		bb.position(len);
		try { fc.read(bb); } catch(IOException e) {}
		bb.limit(bb.position()); // prepare array section, set position/limit
		bb.position(0);
	}

	protected void prepareWrite(int byteCount) {
		if(!writeMode) bb.limit(0); // safely leave read mode
		if(bb.remaining() >= byteCount) return; // good to go
		if(writeMode) flush(); // flush what has been buffered so far
		writeMode = true;
		bb.clear(); // position = 0, limit = capacity (space ready to store writes)
	}

	protected void flush() { // assuming writeMode == true
		bb.limit(bb.position()); // bb.position is the number of bytes waiting to be written
		bb.position(0);
		try { fc.write(bb); } catch(IOException e) {} // leaves remaining() == 0
		bb.limit(0);
	}

	// AutoClosable //////////////////////////////////////////////////////

	@Override public void close() throws Exception { finish(); }

}
