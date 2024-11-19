/**
MIT License

Copyright (c) 2024 PD4ML, Munich, Germany

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
 */

package com.pd4ml.pixels.util;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;

public class Util {

	private static final byte[] HEX_CHAR = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };	

	public static String formatFileReference( boolean java, String name, int size, int width, int height ) {
		
		if ( height == 0 ) {
			height = 80;
		}
		
		File f = new File(name);
		
		StringBuffer sb = new StringBuffer();
		
		sb.append("// image file size: ").append(size).append("\n");
		if ( width > 0 ) {
			sb.append("// usage:            utft.loadBitmap(x, y, ").append(width).append(", ").append(height).append(", \"").append(f.getName()).append("\");\n");
		} 
		
		return sb.toString();
	}
	
	public static String formatIconReference( boolean java, String name, int size, String instanceName ) {
		
		File f = new File(name);
		
		StringBuffer sb = new StringBuffer();
		sb.append("/*\n  icon file size: ").append(size).append("\n");
		sb.append("  usage:\n\t").append(instanceName).append(".loadIcon(x, y, ").append("\"").append(f.getName()).append("\");\n*/\n");
		
		return sb.toString();
	}
	
	public static String formatImageBytes( boolean java, String name, byte[] bytes, int width, int height, String instanceName ) {
		
		if ( height == 0 ) {
			height = 80;
		}
		
		name = escapeName(name);
		
		StringBuffer sb = new StringBuffer();
		
		sb.append("prog_uint16_t ").append(name).append("[").append(bytes.length/2).append("] PROGMEM = {").append("\n");

		for ( int i = 0; i < bytes.length-1; i+=2 ) {
			byte b1 = bytes[i];
			byte b2 = bytes[i+1];
			
			sb.append("0x")
			.append((char)(HEX_CHAR[(b1 & 0x00F0) >> 4])).append((char)(HEX_CHAR[b1 & 0x000F]))
			.append((char)(HEX_CHAR[(b2 & 0x00F0) >> 4])).append((char)(HEX_CHAR[b2 & 0x000F]))
			.append( "," );
			
			if ( ((i+2) % height*2) == 0 ) {
				sb.append('\n');
			}
		}
		
		sb.append("};\n/8\n");
		sb.append("array size: ").append(bytes.length/2).append("\n\n");
		if ( width > 0 ) {
			sb.append("usage:\n\t").append(instanceName).append(".drawBitmap(x, y, ").append(width).append(", ").append(height).append(", ").append(name).append(");\n*/\n");
		} 
		
		return sb.toString();
	}

	public static String formatCompressedImageBytes( boolean java, String name, byte[] bytes, int width, int height, String instanceName ) {
		
		int	wrap = 20;
		
		name = escapeName(name) + "_comp";
		
		StringBuffer sb = new StringBuffer();
		
		sb.append("prog_uchar ").append(name).append("[").append(bytes.length).append("] PROGMEM = {").append("\n");

		for ( int i = 0; i < bytes.length; i++ ) {
			if ( i < bytes.length ) {
				byte b1 = bytes[i];
				sb.append("0x")
				  .append((char)(HEX_CHAR[(b1 & 0x00F0) >> 4]))
				  .append((char)(HEX_CHAR[b1 & 0x000F]))
				  .append( "," );
			} else {
				sb.append("0x00,");
			}
			
			if ( ((i+1) % wrap) == 0 ) {
				sb.append('\n');
			}
		}
		
		sb.append("};\n/*\n");
		
		sb.append("array size:   ").append(bytes.length).append("\n");
		sb.append("image width: ").append(width).append("\n");
		sb.append("image height: ").append(height).append("\n\n");
		if ( width > 0 ) {
			sb.append("usage:\n\t").append(instanceName).append(".drawCompressedBitmap(x, y, ").append(name).append(");\n");
		} 
		sb.append("*/\n");
		
		return sb.toString();
	}

	public static String formatFontBytes( boolean java, String name, String range, byte[] bytes, int glyphHeight, int baseline, boolean antialiased, String instanceName ) {
		
		int	wrap = 20;
		
		name = escapeName(name);
		
		StringBuffer sb = new StringBuffer();
		
		sb.append("prog_uchar ").append(name).append("[").append(bytes.length+2).append("] PROGMEM = {").append("\n");

		for ( int i = 0; i < bytes.length + 2; i++ ) {
			if ( i < bytes.length ) {
				byte b1 = bytes[i];
				sb.append("0x")
				  .append((char)(HEX_CHAR[(b1 & 0x00F0) >> 4]))
				  .append((char)(HEX_CHAR[b1 & 0x000F]))
				  .append( "," );
			} else {
				sb.append("0x00,");
			}
			
			if ( ((i+1) % wrap) == 0 ) {
				sb.append('\n');
			}
		}
		
		sb.append("};\n");
		sb.append("/*\n");
		if ( antialiased ) {
			sb.append("antialiased\n");
		}
		sb.append("array size:   ").append(bytes.length+2).append("\n");
		sb.append("glyph height: ").append(glyphHeight).append("\n");
		sb.append("baseline:     ").append(baseline).append("\n");
		sb.append("for:          ").append(range).append("\n\n");
		sb.append("usage:\n\t").append(instanceName).append(".setFont(").append(name).append(");\n");
		sb.append("\t").append(instanceName).append(".print(x, y, \"...\");\n*/\n");
		
		return sb.toString();
	}

	public static String formatIconBytes( boolean java, String name, byte[] bytes, int width, int height, String instanceName ) {
		
		int	wrap = 20;
		
		name = escapeName(name);
		
		StringBuffer sb = new StringBuffer();
		
		sb.append("prog_uchar ").append(name).append("[").append(bytes.length).append("] PROGMEM = {").append("\n");

		for ( int i = 0; i < bytes.length; i++ ) {
			int b1 = 0xFF & bytes[i];
			sb.append("0x")
			.append((char)(HEX_CHAR[(b1 & 0x00F0) >> 4]))
			.append((char)(HEX_CHAR[b1 & 0x000F]))
			.append( "," );
			
			if ( ((i+1) % wrap) == 0 ) {
				sb.append('\n');
			}
		}
		
		sb.append("};\n/*\n");
		sb.append("array size:   ").append(bytes.length).append("\n");
		sb.append("image size:   ").append(width).append("x").append(height).append("\n\n");
		sb.append("usage:\n\t").append(instanceName).append(".drawIcon(x, y, ").append(name).append(");\n");
		sb.append("\t").append(instanceName).append(".cleanIcon(x, y, ").append(name).append(");\n");
			sb.append("*/");
		
		return sb.toString();
	}

	public static String escapeName( String name ) {
		
		StringBuffer result = new StringBuffer();
		int len = name.length();
		for (int i = 0; i < len; i++) {
			char c = name.charAt(i);
			if ( i == 0 && (!Character.isJavaIdentifierStart(c) || !Character.isLetterOrDigit(c)) ) {
				result.append('_');
				continue;
			}
			if ( i != 0 && (!Character.isJavaIdentifierPart(c) || !Character.isLetterOrDigit(c)) ) {
				result.append('_');
				continue;
			}
			result.append(c);
		}
		
		return result.toString();
	}
	
	
	public static Range getWord(String s, int pos, boolean semicolonExpected) {

		if ( s == null || s.length() == 0 ) {
			return new Range(0, 0);
		}

		int startPos = 0;
		
		if ( pos < 0 ) {
			pos = 0;
		}

		if ( pos > 0 && !Character.isLetterOrDigit(s.charAt(pos)) && semicolonExpected ) {
			pos--;
		}

		if ( pos < s.length() - 1 && !Character.isLetterOrDigit(s.charAt(pos)) ) {
			pos++;
		}
		
		if ( !Character.isLetterOrDigit(s.charAt(pos)) ) {
    		for ( int i = pos - 1; i >= 0; i-- ) {
    			if ( Character.isLetterOrDigit(s.charAt(i) ) ) {
    				pos = i;
    				break;
    			}
    		}
		}

		boolean found = false;
		int i = pos - 1;
		for ( ; i >= 0; i-- ) {
			char c = s.charAt(i);
			if ( !Character.isLetterOrDigit(c) ) {
				startPos = i + 1;
				found = true;
				break;
			}
		}
		if ( !found ) {
			startPos = 0;
		}

		found = false;
		i = pos + 1;
		for ( ; i < s.length(); i++ ) {
			char c = s.charAt(i);
			if ( !Character.isLetterOrDigit(c) ) {
				pos = i - 1;
				found = true;
				break;
			}
		}
		if ( !found ) {
			pos = i - 1;
		}

		return new Range(startPos, Math.max(1, pos-startPos+1));
	}

	public static class Range {
		public Range( int from, int len) {
			this.from = from;
			this.len = len;
		}
		public int from;
		public int len;
	}

	public static final String replaceString(String src, String find, String replacement) {
		final int len = src.length();
		final int findLen = find.length();

		int idx = src.indexOf(find);
		if (idx < 0) {
			return src;
		}

		StringBuffer buf = new StringBuffer();
		int beginIndex = 0;
		while (idx != -1 && idx < len) {
			buf.append(src.substring(beginIndex, idx));
			buf.append(replacement);
			
			beginIndex = idx + findLen;
			if (beginIndex < len) {
				idx = src.indexOf(find, beginIndex);
			} else {
				idx = -1;
			}
		}
		if (beginIndex<len) {
			buf.append(src.substring(beginIndex, (idx==-1?len:idx)));
		}
		return buf.toString();
	}

	public final static byte[] readFile( String path ) throws Exception {

		File f = new File( path );
		FileInputStream is = new FileInputStream(f);
		BufferedInputStream bis = new BufferedInputStream(is);
		
		ByteArrayOutputStream fos = new ByteArrayOutputStream();
		byte buffer[] = new byte[2048];

		int read;
		do {
			read = is.read(buffer, 0, buffer.length);
			if (read > 0) { // something to put down
				fos.write(buffer, 0, read); 
			}
		} while (read > -1);

		fos.close();
		bis.close();
		is.close();

		return fos.toByteArray();
	}
}
