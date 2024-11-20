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

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.PixelGrabber;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;

import javax.imageio.ImageIO;

import com.pd4ml.pixels.export.App.Params;
import com.pd4ml.pixels.util.Compressor;
import com.pd4ml.pixels.util.Util;



public class ExportImage {

	protected Object result;

	private String imagePath;

	private int imgWidth;
	private int imgHeight;

	private int actualWidth;
	private int actualHeight;
	
	private boolean fixit;
	
	private int scale;

	public ExportImage(String imagePath, Params p) {
		this.imagePath = imagePath;
		fixit = p.fixFFFF;
		scale = p.scale;
	}

	public String getImageData() throws IOException {
		return Util.formatCompressedImageBytes(true, Paths.get(imagePath).getFileName().toString(), getImageBytes(fixit), actualWidth, actualHeight, "pxs");
	}

	private byte[] getImageBytes( boolean fixit ) {

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
        int w = imgWidth * scale / 100;
        int h = imgHeight * scale / 100;

		ByteArrayOutputStream baos = new ByteArrayOutputStream();

	    ComponentColorModel colorModel = new ComponentColorModel(
	    		ColorSpace.getInstance(ColorSpace.CS_sRGB), true, false, Transparency.TRANSLUCENT, DataBuffer.TYPE_BYTE);
	    WritableRaster raster = Raster.createInterleavedRaster(DataBuffer.TYPE_BYTE, w, h, w * 4, 4, new int[] {0, 1, 2, 3}, null); // R, G, B, A order
	    BufferedImage scaled = new BufferedImage(colorModel, raster, colorModel.isAlphaPremultiplied(), null);

	    Graphics2D g = scaled.createGraphics();
	    try {
			g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			g.drawImage(img, 0, 0, w, h, 0, 0, img.getWidth(), img.getHeight(), null);
	    }
	    finally {
	        g.dispose();
	    }

		PixelGrabber pg = new PixelGrabber(scaled, 0, 0, -1, -1, true);
		try {
			pg.grabPixels();
		} catch (InterruptedException e) {
		    e.printStackTrace();
		    return null;
		}
		
		int[] data = (int[]) pg.getPixels();

        result = new int[w * h];
 
		int x = 0; 
		int y = 0;

		boolean repeat = true;
		int xlimit = 0;
		int ylimit = 0;
		
		while ( repeat ) {
			repeat = false;
			byte[] bx = new byte[2];
			for ( int j = y; j < h + y; j++  ) {
				for ( int i = x; i < w + x; i++  ) {
					if ( i >= w + x - xlimit || j >= h + y - ylimit ) {
						continue;
					}
	                int arrayIndex = (i + j * w);
	                
	    			short r = (short)(0xff & (data[arrayIndex] >> 16));
	    			short gr = (short)(0xff & (data[arrayIndex] >> 8));
	    			short b = (short)(0xff & (data[arrayIndex] >> 0));

					int px565 = ((((r & 0xff) >> 3) << 11) + 
							(((gr & 0xff) >> 2) << 5) + 
							((b & 0xff) >> 3));
					if ( fixit && px565 == 0xffff ) {
						bx[0] = (byte) 0xff;
						bx[1] = (byte) 0xdf;
					} else {
						bx[0] = (byte)((px565 & 0xff00) >> 8);
						bx[1] = (byte)(px565 & 0x00ff);
					}
					try {
						baos.write(bx);
					} catch (OutOfMemoryError e) {
						baos.reset();
						repeat = true;
						xlimit += w/2 + xlimit;
						ylimit += h/2 + ylimit;
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
		
		actualWidth = w - xlimit;
		actualHeight = h - ylimit;
		
		byte[] bb = baos.toByteArray();
		if ( bb == null || bb.length < 8 ) {
			System.err.println("Compression failed! Corrupted image bytes." );
			return bb;
		} 

//			System.out.println("Original: " + bb.length + "b" );

		byte[] result = Compressor.compress( bb, actualWidth, actualHeight );

//			System.out.println("Compressed: " + result.length + "b" );
//			if ( result == null || result.length < 8 ) {
//				System.out.println("Hmm... " + result );
//			} else {
//				System.out.println("Window length: " + (0xFF & result[7]) + "b" );
//			}

		byte[] test = Compressor.uncompress(result);

		if(!Arrays.equals(bb, test)) {
			System.err.println("Compression failed! " + (bb.length != test.length ? bb.length + " vs. " + test.length : "") );
			for ( int i = 0; i < bb.length; i++ ) {
				System.err.print(hex(bb[i]));
				if ( bb[i] != test[i] ) {
					System.err.println( "!= " + hex(test[i]) + "(" + i + ")" );
					break;
				}
			}
		}

		return result;
	}
	
	private String hex( byte i ) {
		String s = Integer.toHexString(0xFF & i);
		if ( s.length() == 1 ) {
			s = "0" + s;
		}
		return (s + " ").toUpperCase();
	}
}

