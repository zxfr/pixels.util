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
import java.awt.FontFormatException;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Paths;

import javax.imageio.ImageIO;

import com.pd4ml.pixels.export.App.Params;
import com.pd4ml.pixels.util.Util;

public class ExportIcon {

	protected Object result;

	private String imagePath;
	private int imgWidth;
	private int imgHeight;

//	private boolean monochrome;
	private int edge = 0;

	private int actualWidth = 0;
	private int actualHeight = 0;
	
	private java.awt.Font awtFont;
	private String chosenGlyph;
	
	private int fontSize;
	
	private boolean antialiased;
	
	Image img;
	
	public ExportIcon(String path, Params p) throws FontFormatException, IOException {
		
		imagePath = path;
		antialiased = p.antialiased;
		chosenGlyph = p.glyph;
		fontSize = p.fontSize;
		
		if (p.glyph != null) {
			
			FileInputStream fis = new FileInputStream(path);
			awtFont = Font.createFont(Font.TRUETYPE_FONT, fis);
			awtFont.getName();
			fis.close();

			
		} else {
			
		}
	}

	public String getIconData() throws IOException {
		if (chosenGlyph != null) {
			return Util.formatIconBytes(true, Paths.get(imagePath).getFileName().toString(), getGlyphIconBytes(), actualWidth, actualHeight, "pxs");
		} else {
			return Util.formatIconBytes(true, Paths.get(imagePath).getFileName().toString(), getIconBytes(), actualWidth, actualHeight, "pxs");
		}
	}


	private byte[]  getGlyphIconBytes() {
		
		if ( chosenGlyph == null ) {
			return new byte[0];
		}
		
//		int fontSize = 28;
//		fontSize = (int)(1.35 * fontSize + .5);

		int height;
		int width;
		
		java.awt.Font f = awtFont.deriveFont((float)fontSize);
		@SuppressWarnings("deprecation")
		FontMetrics fm = Toolkit.getDefaultToolkit().getFontMetrics( f );
		height = (int)((fm.getAscent() + fm.getDescent()) * 1.5);

		int top = height;
		int bottom = 0;

		width = fm.charWidth(chosenGlyph.charAt(0));
			
		BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = bi.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);			
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, width, height);
		g.setColor(Color.BLACK);
		g.setFont(f);
		g.drawString(chosenGlyph, 0, fm.getAscent());
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
				top = y;
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
				bottom = y;
				break;
			}
		}

		if ( (top != 0 || bottom != height-1) && bottom - top + 1 > 0) {
			height = bottom - top + 1;

			BufferedImage bi2 = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
			g = bi2.createGraphics();
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);			
			g.setColor(Color.WHITE);
			g.fillRect(0, 0, width, height);
			g.drawImage(bi, 0, -top, null);
			g.dispose();
			bi = bi2;
		}

		return extractBytes(bi, width, height, antialiased);
	}

	private byte[] getIconBytes() {
		
		BufferedImage img = null;
		try 
		{
			img = ImageIO.read(new File(imagePath));
			imgWidth = img.getWidth();
			imgHeight = img.getHeight();
		} 
		catch (IOException e) 
		{
		    e.printStackTrace();
		    return null;
		}		

		int width = Math.min(imgWidth, 255);
		int height = Math.min(imgHeight, 255);
		actualWidth = width;
		actualHeight = height;


        BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        // Draw the image on to the buffered image
        Graphics2D bGr = bufferedImage.createGraphics();
        bGr.setBackground(Color.white);
        bGr.fillRect(0, 0, width, height);
        bGr.drawImage(img, 0, 0, null);
        bGr.dispose();

		return extractBytes(bufferedImage, width, height, antialiased);
	}

	private byte[] extractBytes(BufferedImage bufferedImage, int width, int height, boolean antialiased) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();

        ColorModel colorModel = bufferedImage.getColorModel();
        int w = bufferedImage.getWidth();
        int h = bufferedImage.getHeight();
        result = new int[w * h];

        DataBuffer dataBuffer = bufferedImage.getRaster().getDataBuffer();
        DataBufferByte dataBufferByte = null;
        if (dataBuffer instanceof DataBufferByte)
        {
            dataBufferByte = (DataBufferByte)dataBuffer;
            byte data[] = dataBufferByte.getData();
            
            byte[] bx = new byte[1];
            for ( int j = 0; j < height; j++  ) {
            	for ( int i = 0; i < width; i++  ) {
            		if ( i >= width || j >= height ) {
            			continue;
            		}
            		int arrayIndex = i + j * w;
            		int colorIndex = data[arrayIndex];
            		//               int color = indexColorModel.getRGB(colorIndex);
            		bx[0] = (byte)(0xFF & Math.round(0.2126*colorModel.getRed(colorIndex) + 
            				0.7152*colorModel.getGreen(colorIndex) + 
            				0.0722*colorModel.getBlue(colorIndex)));
            		try {
            			baos.write(bx);
            		} catch (IOException e) {
            			e.printStackTrace();
            		}
            	}
            }
        }
        else
        {
        	DataBufferInt dataBufferInt = (DataBufferInt)dataBuffer;
            int data[] = dataBufferInt.getData();
            
            byte[] bx = new byte[1];
            for ( int j = 0; j < height; j++  ) {
            	for ( int i = 0; i < width; i++  ) {
            		if ( i >= width || j >= height ) {
            			continue;
            		}
            		int arrayIndex = i + j * w;
            		int colorIndex = data[arrayIndex];
            		//               int color = indexColorModel.getRGB(colorIndex);
            		bx[0] = (byte)(0xFF & Math.round(0.2126*colorModel.getRed(colorIndex) + 
            				0.7152*colorModel.getGreen(colorIndex) + 
            				0.0722*colorModel.getBlue(colorIndex)));
            		try {
            			baos.write(bx);
            		} catch (IOException e) {
            			e.printStackTrace();
            		}
            	}
            }
        }

		
		byte[] bb = baos.toByteArray();
		if ( bb == null ) {
			System.err.println("Compression failed! Corrupted image bytes." );
			return bb;
		} 
		actualWidth = w;
		actualHeight = h;
		return formatIconBytes(bb, actualWidth, antialiased);
	}

	public byte[] formatIconBytes(byte[] bytes, int width, boolean antialiased) {

		byte[] data = new byte[bytes.length*2+8];
		int ptr = 0;

		int marginLeft = -1;
		int marginRight = -1;
		int marginTop = 0;
		int marginBottom = 0;
		
		int height = bytes.length / width;
		
		for ( int i = 0; i < bytes.length; i++ ) {
			if ( (0xff & bytes[i]) != 0xff ) {
				marginTop = Math.min(255, i / width);
				break;
			}
		}
		for ( int i = bytes.length - 1; i >= 0; i-- ) {
			if ( (0xff & bytes[i]) != 0xff ) {
				marginBottom = Math.min(255, height - (i / width) - 1);
				break;
			}
		}
		for ( int i = 0; i < width; i++ ) {
			for ( int j = 0; j < height; j++ ) {
				if ( (0xff & bytes[j*width+i]) != 0xff ) {
					marginLeft = Math.min(127, i);
					break;
				}
			}
			if ( marginLeft >= 0 ) {
				break;
			}
		}
		if ( marginLeft < 0 ) {
			marginLeft = Math.min(127, width);
		}
		
		for ( int i = 0; i < width; i++ ) {
			for ( int j = 0; j < height; j++ ) {
				if ( (0xff & bytes[j*width+(width-i-1)]) != 0xff ) {
					marginRight = Math.min(127, i);
					break;
				}
			}
			if ( marginRight >= 0 ) {
				break;
			}
		}
		if ( marginRight < 0 ) {
			marginRight = Math.min(127, width);
		}
		
		if ( marginLeft + marginRight > width ) {
			marginLeft = 0;
			marginRight = 0;
		}
		
		if ( marginTop + marginBottom > height ) {
			marginTop = 0;
			marginBottom = 0;
		}
		
		if ( antialiased ) {

			data[0] = (byte)'Z';
			data[1] = (byte)'a';
			data[4] = (byte)(height & 0xff); //it cannot be higher 255px
			data[5] = (byte)(width & 0xff); //it cannot be wider 255px
			data[6] = (byte)(marginLeft & 0x7f);
			data[7] = (byte)(marginTop & 0xff);
			data[8] = (byte)(marginRight & 0x7f);

			ptr = 9;
			int prev = 0;
			for (int y = 0; y < height - marginBottom - marginTop; y++) {
				for (int x = 0; x < width - marginLeft - marginRight; x++) {
					int i = y * (width - marginLeft - marginRight) + x;
					
					if ( i == 0 ) {
						prev = bytes[marginLeft + marginTop * width] & 0xff;
						if ( prev == 0 ) {
							data[ptr++] = (byte)(0x81);
						} else if ( prev == 255 ) {
							data[ptr++] = (byte)(0x41);
						} else {
							data[ptr++] = (byte)(prev/4);
						}
						continue;
					}
					int pixel = bytes[marginLeft + x + (y + marginTop) * width] & 0xff; 
					if ( pixel == 0 || pixel == 255 ) {
						if ( prev == pixel && (0x3f & data[ptr-1]) < 63 ) {
							data[ptr-1]++;
						} else {
							if ( pixel == 0 ) {
								data[ptr++] = (byte)(0x81);
							} else {
								data[ptr++] = (byte)(0x41);
							}
						}
					} else {
						data[ptr++] = (byte)(pixel/4);
					}
					prev = pixel; 
				}
			}
			
			byte[] data2 = data;
			int ptr2 = ptr;

			data = new byte[bytes.length*2+8];
			
			data[0] = (byte)'Z';
			data[1] = (byte)'a';
			data[4] = (byte)(height & 0xff); //it cannot be higher 255px
			data[5] = (byte)(width & 0xff); //it cannot be wider 255px
			data[6] = (byte)((marginLeft+128) & 0xff); // high bit is v-polarization flag 
			data[7] = (byte)(marginTop & 0xff);
			data[8] = (byte)(marginBottom & 0xff);

			ptr = 9;
			prev = 0;
			for (int x = 0; x < width - marginLeft - marginRight; x++) {
				for (int y = 0; y < height - marginBottom - marginTop; y++) {
					int i = y * (width - marginLeft - marginRight) + x;
					
					if ( i == 0 ) {
						prev = bytes[marginLeft + marginTop * width] & 0xff;
						if ( prev == 0 ) {
							data[ptr++] = (byte)(0x81);
						} else if ( prev == 255 ) {
							data[ptr++] = (byte)(0x41);
						} else {
							data[ptr++] = (byte)(prev/4);
						}
						continue;
					}
					int pixel = bytes[marginLeft + x + (y + marginTop) * width] & 0xff; 
					if ( pixel == 0 || pixel == 255 ) {
						if ( prev == pixel && (0x3f & data[ptr-1]) < 63 ) {
							data[ptr-1]++;
						} else {
							if ( pixel == 0 ) {
								data[ptr++] = (byte)(0x81);
							} else {
								data[ptr++] = (byte)(0x41);
							}
						}
					} else {
						data[ptr++] = (byte)(pixel/4);
					}
					prev = pixel; 
				}
			}
			
			if ( ptr2 <= ptr ) {
				ptr = ptr2;
				data = data2;
//				System.out.println(" |");
			} else {
//				System.out.println(" -");
			}
			
		} else {

			byte[] horRaw; // horizontal scan raw
			int hrPtr;
			byte[] horPack; // horizontal packed
			int hpPtr;
			byte[] verRaw; // vertical scan raw
			int vrPtr;
			byte[] verPack; // vertical packed
			int vpPtr;
			
			data[0] = (byte)'Z';
			data[1] = (byte)'b';
			data[4] = (byte)(height & 0xff); //it cannot be higher 255px
			data[5] = (byte)(width & 0xff); //it cannot be wider 255px
			data[6] = (byte)(marginLeft & 0x7f);
			data[7] = (byte)(marginTop & 0xff);
			data[8] = (byte)(marginRight & 0x7f);
			
			ptr = 9;

			ptr--;
			for (int y = 0; y < height - marginBottom - marginTop; y++) {
				for (int x = 0; x < width - marginLeft - marginRight; x++) {
					int i = y * (width - marginLeft - marginRight) + x; 
					if ( i % 8 == 0 ) {
						ptr++;
						data[ptr] = (byte)0xff;
					}
					int pixel = bytes[marginLeft + x + (y + marginTop) * width] & 0xff; // XXX
					if ( pixel < 255 - edge * 10 ) {
						int mask = 1 << (7 - (i % 8));
						data[ptr] ^= mask;
					}
				}
			}
			ptr++;
			
			horRaw = data;
			hrPtr = ptr;
			
			// ------------------------------------------------------------------------
			
			data = new byte[bytes.length*2+9];

			data[0] = (byte)'Z';
			data[1] = (byte)'b';
			data[4] = (byte)(height & 0xff); //it cannot be higher 255px
			data[5] = (byte)(width & 0xff); //it cannot be wider 255px
			data[6] = (byte)(marginLeft & 0x7f);
			data[7] = (byte)(marginTop & 0xff);
			data[8] = (byte)(marginRight & 0x7f);
			
			ptr = 9;

			ptr--;
			for (int x = 0; x < width - marginLeft - marginRight; x++) {
				for (int y = 0; y < height - marginBottom - marginTop; y++) {
					int i = x * (height - marginTop - marginBottom) + y; 
					if ( i % 8 == 0 ) {
						ptr++;
						data[ptr] = (byte)0xff;
					}
					int pixel = bytes[marginLeft + x + (y + marginTop) * width] & 0xff; // XXX
					if ( pixel < 255 - edge * 10 ) {
						int mask = 1 << (7 - (i % 8));
						data[ptr] ^= mask;
					}
				}
			}
			ptr++;

			verRaw = data;
			vrPtr = ptr;

			// ------------------------------------------------------------------------

			data = new byte[bytes.length*2+9];

			data[0] = (byte)'Z';
			data[1] = (byte)'b';
			data[4] = (byte)(height & 0xff); //it cannot be higher 255px
			data[5] = (byte)(width & 0xff); //it cannot be wider 255px
			data[6] = (byte)(marginLeft & 0x7f);
			data[7] = (byte)(marginTop & 0xff);
			data[8] = (byte)((marginRight+128) & 0xff); // high bit is compression flag
			
			ptr = 9;

			boolean prevColor = (horRaw[ptr] & 0x80) == 0;
			int ctr = 0;
			for (int i = 9; i < hrPtr; i++) {
				byte b = horRaw[i];
				for (int j = 0; j < 8; j++) {
					int mask = 1 << (7 - j);
					boolean bitColor = (b & mask) == 0;
					if ( prevColor == bitColor ) {
						ctr++;
						if ( ctr == 127 ) {
							data[ptr++] = (byte)(prevColor ? 0xFF : 0x7F);
							ctr = 0;
						}
					}
					if ( prevColor != bitColor ) {
						if ( ctr > 0 ) {
							data[ptr++] = (byte)(((prevColor ? 0x80 : 0) + ctr) & 0xff);
						}
						prevColor = bitColor;
						ctr = 1;
					}
				}
			}
			if ( ctr > 0 ) {
				data[ptr++] = (byte)(((prevColor ? 0x80 : 0) + ctr) & 0xff);
			}
			
			horPack = data;
			hpPtr = ptr;
			
			// ------------------------------------------------------------------------

			data = new byte[bytes.length*2+9];

			data[0] = (byte)'Z';
			data[1] = (byte)'b';
			data[4] = (byte)(height & 0xff); //it cannot be higher 255px
			data[5] = (byte)(width & 0xff); //it cannot be wider 255px
			data[6] = (byte)((marginLeft+128) & 0xff); // high bit is v-polarization flag
			data[7] = (byte)(marginTop & 0xff);
			data[8] = (byte)((marginBottom+128) & 0xff); // high bit is compression flag
			
			ptr = 9;
			
			prevColor = (verRaw[ptr] & 0x80) == 0;
			ctr = 0;
			for (int i = 9; i < vrPtr; i++) {
				byte b = verRaw[i];
				for (int j = 0; j < 8; j++) {
					int mask = 1 << (7 - j);
					boolean bitColor = (b & mask) == 0;
					if ( prevColor == bitColor ) {
						ctr++;
						if ( ctr == 127 ) {
							data[ptr++] = (byte)(prevColor ? 0xFF : 0x7F); 
							ctr = 0;
						}
					}
					if ( prevColor != bitColor ) {
						if ( ctr > 0 ) {
							data[ptr++] = (byte)(((prevColor ? 0x80 : 0) + ctr) & 0xff);
						}
						prevColor = bitColor;
						ctr = 1;
					}
				}
			}
			if ( ctr > 0 ) {
				data[ptr++] = (byte)(((prevColor ? 0x80 : 0) + ctr) & 0xff);
			}
			
			verPack = data;
			vpPtr = ptr;

			data = horRaw;
			ptr = hrPtr;

			if ( hpPtr < ptr ) {
				data = horPack;
				ptr = hpPtr;
//				System.out.println(" -");
			}

			if ( vpPtr < ptr ) {
				data = verPack;
				ptr = vpPtr;
//				System.out.println(" |");
			}
		} 
		
		data[2] = (byte)((ptr & 0xff00)>>8);
		data[3] = (byte)(ptr & 0xff);
		
		byte[] result = new byte[ptr];
		System.arraycopy(data, 0, result, 0, ptr);
		return result;
	}
}

