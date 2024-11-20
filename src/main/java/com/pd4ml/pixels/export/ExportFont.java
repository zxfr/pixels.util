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

package com.pd4ml.pixels.export;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;

import com.pd4ml.pixels.export.App.Params;
import com.pd4ml.pixels.fonts.RasterFont;
import com.pd4ml.pixels.util.Util;


public class ExportFont {

	protected Object result;

	private Font font;

	private int baseline;
	private int glyphHeight;

	private String charactersToImport;

	private boolean antialiased;

	private RasterFont rasterFont;
	
	public ExportFont(String fontPath, Params p) {
		try {
			FileInputStream fis = new FileInputStream( fontPath );
			font = Font.createFont( Font.TRUETYPE_FONT, fis );
			font.getName();
			fis.close();

			antialiased = p.antialiased;
			charactersToImport = p.text;
			if (charactersToImport == null) {
				charactersToImport = "A-Z a-z . , - + #x0021-#x0024 #00048-#00057";
			}
			
	        generateGlyphImages(charactersToImport, (int)(1.35 * p.fontSize + .5));

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public byte[] getFontBytes() throws IOException {
		return rasterFont.toByteArray(font.getPSName());
	}


	public String getFontData() throws IOException {
		return Util.formatFontBytes(false, getFontName(), charactersToImport, getFontBytes(), glyphHeight, baseline, antialiased, "pxs");
	}

	private void generateGlyphImages( String chars, int fsize ) {
		
		if ( chars == null ) {
			return;
		}
		
		chars = buildRange( chars );
		
//		fsize = (int)(1.35 * fsize + .5);
//		System.out.println(fsize);
		
		Font f = font.deriveFont((float)fsize);
		@SuppressWarnings("deprecation")
		FontMetrics fm = Toolkit.getDefaultToolkit().getFontMetrics( f );
		int height = (int)((fm.getAscent() + fm.getDescent()) * 1.5);
		baseline = fm.getAscent();

		rasterFont = new RasterFont(antialiased);
		
		int top = height;
		int bottom = 0;

		int widthx = fm.charWidth(f.getMissingGlyphCode());
		if ( widthx <= 0 ) {
			widthx = 1;
		}
		BufferedImage bix = new BufferedImage(widthx, height, BufferedImage.TYPE_INT_RGB);
		Graphics2D gx = bix.createGraphics();
		if ( antialiased ) {
			gx.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);			
		}
		gx.setColor(Color.WHITE);
		gx.fillRect(0, 0, widthx, height);
		gx.setColor(Color.BLACK);
		gx.setFont(f);
		gx.drawString("" + (char)f.getMissingGlyphCode(), 0, fm.getAscent());
		gx.dispose();

		for ( int i = 0; i < chars.length(); i++ ) {
			char c = chars.charAt(i);
			int width = fm.charWidth(c);
			
			if ( width == 0 ) {
				continue;
			}

			BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
			Graphics2D g = bi.createGraphics();
			if ( antialiased ) {
				g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);			
			}
			g.setColor(Color.WHITE);
			g.fillRect(0, 0, width, height);
			g.setColor(Color.BLACK);
			g.setFont(f);
			g.drawString("" + c, 0, fm.getAscent());
			g.dispose();
			
			for ( int y = 0; y < top; y++ ) {
				boolean blank = true;
				for ( int x = 0; x < width; x++ ) {
					int pix = bi.getRGB(x, y);
					if ( (0xffffff & pix) != 0xffffff ) {
						blank = false;
						break;
					}
				}
				if ( !blank ) {
					top = y-1;
					break;
				}
			}

			for ( int y = height-1; y >= bottom; y-- ) {
				boolean blank = true;
				for ( int x = 0; x < width; x++ ) {
					int pix = bi.getRGB(x, y);
					if ( (0xffffff & pix) != 0xffffff ) {
						blank = false;
						break;
					}
				}
				if ( !blank ) {
					bottom = y+1;
					break;
				}
			}
		}
		
		glyphHeight = height;
		
		height = bottom+top;
		
		rasterFont.setHeight(bottom-top, fm.getAscent());
		
		for ( int i = 0; i < chars.length(); i++ ) {
			char c = chars.charAt(i);
			int width = fm.charWidth(c);

			if ( width == 0 || bottom-top <= 0 ) {
				continue;
			}

			BufferedImage bi = new BufferedImage(width, bottom-top, BufferedImage.TYPE_INT_RGB);
			Graphics2D g = bi.createGraphics();
			if ( rasterFont.isAntialiased() ) {
				g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);			
			}
			g.setColor(Color.WHITE);
			g.fillRect(0, 0, width, height);
			g.setColor(Color.BLACK);
			g.setFont(f);
			g.drawString("" + c, 0, fm.getAscent()-top);
			g.dispose();
			
			int[] glyphBytes = getGlyphBytes( bi );
			rasterFont.addGlyph(c, glyphBytes, width);
		}
	}
	
	private int LIMIT = 100;
	
	private String buildRange(String chars) {
    	
		chars = resolveEntities(chars);

		HashSet<Character> set = new HashSet<>();
		char[] cx = chars.toCharArray();
		for (int i = 0; i < cx.length; i++) {
			char ch = cx[i];
			int rest = cx.length - i - 1;
			char prev = i > 0 ? cx[i-1] : ' ';
			
			if (ch == '-' && rest > 0 && i > 0 && prev != ' ' && cx[i+1] != ' ') {
				char ce = cx[i+1];
				if ( ce > prev ) {
					for (char j = (char)(prev + 1); j <= ce; j++) {
						Character c = new Character(j);
						set.add(c);
						if ( set.size() >= LIMIT ) {
							break;
						}
					}
					i++;
					continue;
				}
			}

			if ( set.size() >= LIMIT ) {
				break;
			}
			
			Character c = new Character(ch);
			set.add(c);
		}
		
		ArrayList<Character> list = new ArrayList<>(set);
		Collections.sort(list, new Comparator<Character>() {
			public int compare(Character o1, Character o2) {
				return o1.charValue() > o2.charValue() ? 1 : -1;
			}
		});
		chars = "";
		Iterator<Character> ii = list.iterator();
		while (ii.hasNext()) {
			Character c = ii.next();
			chars += c.charValue();
		}
//		System.out.println(chars);
		return chars;
	}

	public static String resolveEntities(String chars) {
		String res = "";
    	char[] cx = chars.toCharArray();
    	for ( int i = 0; i < cx.length; i++ ) {
    		char ch = cx[i];
    		int rest = cx.length - i - 1;
    		
    		if ( rest >= 5 ) {
    			if ( ch == '#' ) {
    				if ( cx[i + 1] == 'x' ) {
    					String num = chars.substring(i+2, i+6);
    					try {
							int ix = Integer.parseInt(num, 16);
							ch = (char)ix;
							i += 5;
						} catch (NumberFormatException e) {
						}
    					
    				} else {
    					String num = chars.substring(i+1, i+6);
    					try {
							int ix = Integer.parseInt(num);
							ch = (char)ix;
							i += 5;
						} catch (NumberFormatException e) {
						}
    				}
    			}
    		}
    		res += ch;
    	}
//    	System.out.println( "[" + res + "]" );
		return res;
	}

    private int[] getGlyphBytes(BufferedImage bufferedImage) {
    	int[] result = null;

        ColorModel colorModel = bufferedImage.getColorModel();
//        IndexColorModel indexColorModel = null;
//        if (colorModel instanceof IndexColorModel)
//        {
//            indexColorModel = (IndexColorModel)colorModel;
//        }
//        else
//        {
//            System.out.println("No IndexColorModel");
//            return null;
//        }

        DataBuffer dataBuffer = bufferedImage.getRaster().getDataBuffer();
        DataBufferByte dataBufferByte = null;
        if (dataBuffer instanceof DataBufferByte)
        {
            dataBufferByte = (DataBufferByte)dataBuffer;
            int w = bufferedImage.getWidth();
            int h = bufferedImage.getHeight();
            result = new int[w * h];
            BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            byte data[] = dataBufferByte.getData();
            for (int y=0; y<h; y++)
            {
            	for (int x=0; x<w; x++)
            	{
            		int arrayIndex = x + y * w;
            		int colorIndex = data[arrayIndex];
            		int color = colorModel.getRGB(colorIndex);
            		result[arrayIndex] = color;
//            		System.out.println("At "+x+" "+y+" index is "+colorIndex+
//            				" with color "+Integer.toHexString(color));
            		bi.setRGB(x, y, color);
            	}
            }		
        }
        else
        {
        	DataBufferInt dataBufferInt = (DataBufferInt)dataBuffer;
            int w = bufferedImage.getWidth();
            int h = bufferedImage.getHeight();
            result = new int[w * h];
            BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            int data[] = dataBufferInt.getData();
            for (int y=0; y<h; y++)
            {
            	for (int x=0; x<w; x++)
            	{
            		int arrayIndex = x + y * w;
            		int colorIndex = data[arrayIndex];
            		int color = colorModel.getRGB(colorIndex);
            		result[arrayIndex] = color;
//            		System.out.println("At "+x+" "+y+" index is "+colorIndex+
//            				" with color "+Integer.toHexString(color));
            		bi.setRGB(x, y, color);
            	}
            }		
        }

		return result;
	}

	public String getFontName() {
		return font == null ? "UNDEFINED" : font.getPSName();
	}
}
