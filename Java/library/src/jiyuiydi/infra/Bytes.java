package jiyuiydi.infra;

import java.nio.ByteBuffer;
import java.util.ArrayList;

import jiyuiydi.util.BufferedBuffer;

public class Bytes extends Node {

	//final ArrayList<byte[]> segments = new ArrayList<>();
	byte[] data;

	public Bytes() { data = new byte[0]; }

	public Bytes(BufferedBuffer bb, long contentByteLength) {
		if(contentByteLength > Integer.MAX_VALUE) {
			// TODO Auto-generated constructor stub
			bb.seek(contentByteLength);
		} else {
			data = new byte[(int) contentByteLength];
			ByteBuffer seg = bb.read((int) contentByteLength);
			System.arraycopy(seg.array(), seg.position(), data, 0, seg.remaining());
		}
	}

	public void set(byte[] data) { this.data = data; notifyObservers(); }

}
