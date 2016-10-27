package jiyuiydi.util;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;
import java.util.UUID;

public class Utilities {

	static public Path getPath(File f) { return FileSystems.getDefault().getPath(f.getPath()); }

	static public int min(int n, int... ns) {
		int min = n;
		for(int c : ns) if(c < n) min = c;
		return min;
	}

	static public int max(int n, int... ns) {
		int max = n;
		for(int c : ns) if(c > n) max = c;
		return max;
	}

	static public long min(long n, long... ns) {
		long min = n;
		for(long c : ns) if(c < n) min = c;
		return min;
	}

	static public long max(long n, long... ns) {
		long max = n;
		for(long c : ns) if(c > n) max = c;
		return max;
	}

	static public int clamp(int val, int min, int max) {
		return Math.max(min, Math.min(max, val));
	}

	static public int hexChar2int(char ch) {
		if(ch >= 'a' && ch <= 'f') return ch - 'a' + 10;
		if(ch >= 'A' && ch <= 'F') return ch - 'A' + 10;
		if(ch >= '0' && ch <= '9') return ch - '0';
		return 0;
	}

	static public String byteToHex(int b) {
		String s = "";
		final int n1 = b >>> 4, n2 = b & 0x0F;
		if(n1 > 9) s += (char)('A'+n1-10);
		else       s += n1;
		if(n2 > 9) s += (char)('A'+n2-10);
		else       s += n2;
		return s;
	}

	static public String unescapeURL(String url) {
		if(url == null) return "";
		url = url.replace('+', ' '); //replace + first so plus characters can exist in urls
		char[] chars = new char[url.length()];
		url.getChars(0, url.length(), chars, 0);

		int i = -1, offset = 0, hex;
		while(true) {
			i = url.indexOf('%', i+1);
			if(i == -1) return new String(chars, 0, url.length()-offset);

			hex  = Utilities.hexChar2int(chars[i+1])*16;
			hex += Utilities.hexChar2int(chars[i+2])* 1;
			chars[i-offset] = (char) hex;
			offset += 2;

			int n = url.indexOf('%', i+3);
			if(n == -1) n = url.length();
			for(int c = i-offset+3; c < n-offset; ++c) chars[c] = chars[c+offset];
		}
	}

	static public UUID generateUUIDv5(String url) {
		try {
			byte[] b = MessageDigest.getInstance("SHA-1").digest(url.getBytes());
			ByteBuffer bb = ByteBuffer.wrap(b);
			//                                              5                             5
			long high = (bb.getLong() | 0x00_00_00_00_00_00_50_00L) & 0xFf_Ff_Ff_Ff_Ff_Ff_5f_FfL;
			//                            1xxx                          x0xx
			long low  = (bb.getLong() | 0x80_00_00_00_00_00_00_00L) & 0xBf_Ff_Ff_Ff_Ff_Ff_Ff_FfL;
			return new UUID(high, low);
		} catch(NoSuchAlgorithmException e) { return null; }
	}

	static public <T> Iterable<T> iter(Iterator<T> i) {
		return () -> i;
	}

	/**
	 * Contains non-contiguous substring. Only order matters. As if .* were inserted between each character and used as a regular expression.
	 * @param a Source material
	 * @param b Target pattern
	 * @return True if a sequentially contains b
	 */
	public static boolean sequentialContains(String a, String b) {
		int aIndex = 0, bIndex = 0;
		while(aIndex < a.length() && bIndex < b.length())
			if(		Character.toLowerCase(a.charAt(aIndex++))
					==
					Character.toLowerCase(b.charAt(bIndex)))
				++bIndex;
		return bIndex == b.length();
	}

}
