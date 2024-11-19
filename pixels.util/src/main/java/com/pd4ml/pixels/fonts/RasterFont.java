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

package com.pd4ml.pixels.fonts;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;

public class RasterFont {
	
	public static class InvalidFontException extends Exception {
		private static final long serialVersionUID = -3814166911175907921L;

		public InvalidFontException( String message ) {
			super(message);
		}
	}
	
	public final static int BITMASK_FONT = 1;
	public final static int ANTIALIASED_FONT = 2;
	private static final int HEADER_LENGTH = 5;
	
	/*
	 Prefix:
	 0-1: ZF
	 2: flags
	 3: height
	 4: baseline
	 
	 */
	
	public int type;
	public int height;
	private int baseline;
	private HashMap<Character, RasterGlyph> glyphs = new HashMap<>();
	
	public RasterFont( boolean antialiased ) {
		if ( antialiased ) {
			type = ANTIALIASED_FONT;
		} else {
			type = BITMASK_FONT;
		}
	}
	
	public RasterFont( byte[] rasterFont ) throws InvalidFontException {

		byte p1 = rasterFont[0];
		byte p2 = rasterFont[1];
		if ( p1 != 'Z' || p2 != 'F' ) {
			throw new InvalidFontException("Invalid font prefix");
		}
		type = rasterFont[2];
		if ( type != ANTIALIASED_FONT && type != BITMASK_FONT ) {
			throw new InvalidFontException("Unsupported font type");
		}
		height = rasterFont[3];
		baseline = rasterFont[4];

		int ptr = HEADER_LENGTH;

		while ( ptr < rasterFont.length ) {
			if ( rasterFont[ptr+0]== 0 && rasterFont[ptr+1]== 0 ) {
				break;
			}
			RasterGlyph rg = new RasterGlyph(this, rasterFont, ptr);
			char c = rg.getChar();
			ptr += rg.getByteLength();
			glyphs.put(new Character(c), rg);
			
//			System.out.println( c + ": " + rg.getByteLength() + "bytes");
		}
	}

	public byte[] toByteArray(String fontName) throws IOException {
		
		ByteArrayOutputStream fontStream = new ByteArrayOutputStream();
		fontStream.write('Z');
		fontStream.write('F');
		fontStream.write(type);
		fontStream.write(height);
		fontStream.write(baseline);

		Iterator<Character> ii = glyphs.keySet().iterator();
		while ( ii.hasNext() ) {
			Character key = ii.next();
			RasterGlyph glyph = glyphs.get(key);
			byte[] g = glyph.getGlyph();
			fontStream.write(g);
		}
		
		fontStream.write(new byte[] {0, 0});
		
		return fontStream.toByteArray();
	}
	
	public boolean isAntialiased() {
		return type == ANTIALIASED_FONT;
	}
	
	public void setHeight( int height, int baseline ) {
		this.height = height;
		this.baseline = baseline;
	}
	
	public int getBaseline() {
		return this.baseline;
	}
	
	public void addGlyph( char c, int[] glyph, int width ) {
		
		if ( glyph == null ) {
			return;
		}
		
		glyphs.put(new Character(c), new RasterGlyph(this, c, glyph, width));
		
//		for ( int y = 0; y < glyph.length; y++ ) {
//			for ( int x = 0; x < glyph[y].length; x++ ) {
//				System.out.print( (glyph[y][x] & 0xffffff) == 0 ? " " : "*" );
//			}
//			System.out.println();
//		}
//		System.out.println("-------------------------------");
	}
	
	public int estimateSize() {
		int result = 0;
		Iterator<Character> ii = glyphs.keySet().iterator();
		while ( ii.hasNext() ) {
			Object key = ii.next();
			RasterGlyph glyph = glyphs.get(key);
			result += glyph.getSize();
		}
		return result + HEADER_LENGTH + 2; // 2 trailing zeros
	}

	public byte[] getGlyph(char c) {
		RasterGlyph glyph = (RasterGlyph)glyphs.get(new Character(c));
		if ( c != glyph.getChar() ) {
			System.out.println( "got wrong glyph for '" + (int)c + "'" );
		}
		return RasterGlyph.restoreImageData(height, glyph.glyph, type == RasterFont.ANTIALIASED_FONT );
	}

	public int getGlyphWidth(char c) {
		RasterGlyph glyph = (RasterGlyph)glyphs.get(new Character(c));
		if ( c != glyph.getChar() ) {
			System.out.println( "got wrong glyph '" + (int)glyph.getChar() + "' for '" + (int)c + "'" );
		}
		return RasterGlyph.getWidth(glyph.glyph);
	}

	public int getHeight() {
		return height;
	}
}
