/******************************************************************************
 *  ____  _            _                    _
 * | __ )| |_   _  ___| |   _   _  __ _    | |
 * |  _ \| | | | |/ _ \ |  | | | |/ _` |_  | |
 * | |_) | | |_| |  __/ |__| |_| | (_| | |_| |
 * |____/|_|\__,_|\___|_____\__,_|\__,_|\___/
 *
 *  BlueLuaK
 *  https://github.com/BluevaDevelopment/BlueLuaK/t
 *
 *  Based on LuaJ (https://luaj.org)
 *  Original work Copyright (c) 2009 Luaj.org
 *  Modifications Copyright (c) 2026 Blueva Development
 *
 *  SPDX-License-Identifier: MIT
 ******************************************************************************/
package net.blueva.luak.ast;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;

import net.blueva.luak.LuaString;

public class Str {
	
	private Str() {}
	
	public static LuaString quoteString(String image) {
		String s = image.substring(1, image.length()-1);
		byte[] bytes = unquote(s);
		return LuaString.valueUsing(bytes);
	}
	
	public static LuaString charString(String image) {
		String s = image.substring(1, image.length()-1);
		byte[] bytes = unquote(s);
		return LuaString.valueUsing(bytes);
	}
	
	public static LuaString longString(String image) {
		int i = image.indexOf('[', image.indexOf('[')+1)+1;
		String s = image.substring(i,image.length()-i);
		byte[] b = iso88591bytes(s);
		return LuaString.valueUsing(b);
	}
	
	public static byte[] iso88591bytes( String s ) {
		try {
			return s.getBytes("ISO8859-1");
		} catch (UnsupportedEncodingException e) {
			throw new IllegalStateException("ISO8859-1 not supported");
		}
	}
	
	public static byte[] unquote(String s) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		char[] c = s.toCharArray();
		int n = c.length;
		for ( int i=0; i<n; i++ ) {
			if ( c[i] == '\\' && i<n ) {
				switch ( c[++i] ) {
				case '0': case '1': case '2': case '3': case '4':
				case '5': case '6': case '7': case '8': case '9':
					int d=(int) (c[i++]-'0');
					for ( int j=0; i<n && j<2 && c[i]>='0' && c[i]<='9'; i++, j++ )
						d = d * 10 + (int) (c[i]-'0');
					baos.write( (byte) d );
					--i;
					continue;
				case 'a':  baos.write( (byte) 7 );    continue;
				case 'b':  baos.write( (byte) '\b' ); continue;
				case 'f':  baos.write( (byte) '\f' ); continue;
				case 'n':  baos.write( (byte) '\n' ); continue;
				case 'r':  baos.write( (byte) '\r' ); continue;
				case 't':  baos.write( (byte) '\t' ); continue;
				case 'v':  baos.write( (byte) 11 );   continue;
				case '"':  baos.write( (byte) '"' );  continue;
				case '\'': baos.write( (byte) '\'' ); continue;
				case '\\': baos.write( (byte) '\\' ); continue;
				default: baos.write( (byte) c[i] ); break;
				}
			} else {
				baos.write( (byte) c[i] );
			}
		}
		return baos.toByteArray();
	}
}
