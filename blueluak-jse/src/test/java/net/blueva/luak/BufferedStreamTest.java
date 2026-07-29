/******************************************************************************
 *  ____  _            _                _  __
 * | __ )| |_   _  ___| |   _   _  __ _| |/ /
 * |  _ \| | | | |/ _ \ |  | | | |/ _` | ' /
 * | |_) | | |_| |  __/ |__| |_| | (_| | . \
 * |____/|_|\__,_|\___|_____\__,_|\__,_|_|\_\
 *
 *  BlueLuaK
 *  https://github.com/BluevaDevelopment/BlueLuaK
 *
 *  Based on LuaJ (https://luaj.org)
 *  Original work Copyright (c) 2009 Luaj.org
 *  Modifications Copyright (c) 2026 Blueva Development
 *
 *  SPDX-License-Identifier: MIT
 ******************************************************************************/
package net.blueva.luak;

import java.io.ByteArrayInputStream;

import junit.framework.TestCase;

import net.blueva.luak.Globals.BufferedStream;


public class BufferedStreamTest extends TestCase {

	public BufferedStreamTest() {}
	
	private BufferedStream NewBufferedStream(int buflen, String contents) {
		return new BufferedStream(buflen, new ByteArrayInputStream(contents.getBytes()));
	}
	
	protected void setUp() throws Exception {
		super.setUp();
	}
	
	public void testReadEmptyStream() throws java.io.IOException {
		BufferedStream bs = NewBufferedStream(4, "");
		assertEquals(-1, bs.read());
		assertEquals(-1, bs.read(new byte[10]));
		assertEquals(-1, bs.read(new byte[10], 0, 10));
	}
	
	public void testReadByte() throws java.io.IOException {
		BufferedStream bs = NewBufferedStream(2, "abc");
		assertEquals('a', bs.read());
		assertEquals('b', bs.read());
		assertEquals('c', bs.read());
		assertEquals(-1, bs.read());
	}
	
	public void testReadByteArray() throws java.io.IOException {
		byte[] array = new byte[3];
		BufferedStream bs = NewBufferedStream(4, "abcdef");
		assertEquals(3, bs.read(array));
		assertEquals("abc", new String(array));
		assertEquals(1, bs.read(array));
		assertEquals("d", new String(array, 0, 1));
		assertEquals(2, bs.read(array));
		assertEquals("ef", new String(array, 0, 2));
		assertEquals(-1, bs.read());
	}
	
	public void testReadByteArrayOffsetLength() throws java.io.IOException {
		byte[] array = new byte[10];
		BufferedStream bs = NewBufferedStream(8, "abcdefghijklmn");
		assertEquals(4, bs.read(array, 0, 4));
		assertEquals("abcd", new String(array, 0, 4));
		assertEquals(4, bs.read(array, 2, 8));
		assertEquals("efgh", new String(array, 2, 4));
		assertEquals(6, bs.read(array, 0, 10));
		assertEquals("ijklmn", new String(array, 0, 6));
		assertEquals(-1, bs.read());
	}
	
	public void testMarkOffsetBeginningOfStream() throws java.io.IOException {
		byte[] array = new byte[4];
		BufferedStream bs = NewBufferedStream(8, "abcdefghijkl");
		assertEquals(true, bs.markSupported());
		bs.mark(4);
		assertEquals(4, bs.read(array));
		assertEquals("abcd", new String(array));
		bs.reset();
		assertEquals(4, bs.read(array));
		assertEquals("abcd", new String(array));
		assertEquals(4, bs.read(array));
		assertEquals("efgh", new String(array));
		assertEquals(4, bs.read(array));
		assertEquals("ijkl", new String(array));
		assertEquals(-1, bs.read());
	}

	public void testMarkOffsetMiddleOfStream() throws java.io.IOException {
		byte[] array = new byte[4];
		BufferedStream bs = NewBufferedStream(8, "abcdefghijkl");
		assertEquals(true, bs.markSupported());
		assertEquals(4, bs.read(array));
		assertEquals("abcd", new String(array));
		bs.mark(4);
		assertEquals(4, bs.read(array));
		assertEquals("efgh", new String(array));
		bs.reset();
		assertEquals(4, bs.read(array));
		assertEquals("efgh", new String(array));
		assertEquals(4, bs.read(array));
		assertEquals("ijkl", new String(array));
		assertEquals(-1, bs.read());
	}
}
