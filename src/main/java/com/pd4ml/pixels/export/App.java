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

import java.io.File;

public class App 
{
	enum Mode {
		fontMode,
		iconMode,
		imageMode
	}
	
	public static class Params {
		
		Mode mode;
		
		public String input;
		public String output;
		
		public int fontSize = 16;
		public int scale = 100;
		public String text;
		public boolean antialiased;
		public boolean fixFFFF;

		public String glyph;
	}
	
    public static void main( String[] args )
    {
		char quote = File.pathSeparatorChar == '\\' ? '\"' : '\'';
		
		Params p = new Params();
		
		String USAGE = 
		
			"Usage 1:\njava -jar pixels.util-1.0.jar -Xmx512m [-Djava.awt.headless=true] --font "+quote+"<font file path>"+quote+" [--fontsize 18] [--for <"+quote+"Your text | #x0021-#x0024 #00048-#00057"+quote+">] [--antialiased]  [--out "+quote+"[output file path]"+quote+"]\n\n" +
	
			"Usage 2:\njava -jar pixels.util-1.0.jar -Xmx512m [-Djava.awt.headless=true] --icon "+quote+"<image file path>"+quote+" [--out "+quote+"[output file path]"+quote+"]\n\n" +
	
			"Usage 3:\njava -jar pixels.util-1.0.jar -Xmx512m [-Djava.awt.headless=true] --icon "+quote+"<font file path>"+quote+" --glyph <"+quote+"W"+quote+" | #x0022> [--fontsize 18] [--antialiased] [--out "+quote+"[output file path]"+quote+"]\n\n" +
	
			"Usage 4:\njava -jar pixels.util-1.0.jar -Xmx512m [-Djava.awt.headless=true] --image "+quote+"<image file path?"+quote+" [--scale 50%] [--out "+quote+"[output file path]"+quote+"] [--fixFFFF]";
		
		for( int i = 0; i < args.length; i++ ) {

			if ( "--font".equals( args[i] ) ) {
				++i;
				if ( i == args.length ) {
					System.out.println("invalid --font parameter: font file is missing. Example:\n--font /work/myfonts/arial.ttf\n");
					System.out.println( USAGE );
					return; 
				}
				p.mode = Mode.fontMode;
				p.input = args[i];
				continue;
			}
			
			if ( "--icon".equals( args[i] ) ) {
				++i;
				if ( i == args.length ) {
					System.out.println("invalid --icon parameter: font file is missing. Example:\n--icon /work/clipart/emoji.png\nOR\n--icon /work/myfonts/FontAwesome.ttf --glyph #xF32F --fontsize 16\n");
					System.out.println( USAGE );
					return; 
				}
				p.mode = Mode.iconMode;
				p.input = args[i];
				continue;
			}
			
			if ( "--image".equals( args[i] ) ) {
				++i;
				if ( i == args.length ) {
					System.out.println("invalid --image parameter: font file is missing. Example:\n--image /work/clipart/background.png\n");
					System.out.println( USAGE );
					return; 
				}
				p.mode = Mode.imageMode;
				p.input = args[i];
				continue;
			}
			
			if ( "--glyph".equals( args[i] ) ) {
				++i;
				if ( i == args.length ) {
					System.out.println("invalid --glyph parameter: glyph code is missing. Example:\n--glyph #xF32F\n");
					System.out.println( USAGE );
					return; 
				}
				p.glyph = ExportFont.resolveEntities(args[i]);
				continue;
			}
			
			if ( "--out".equals( args[i] ) ) {
				++i;
				if ( i == args.length ) {
					System.out.println("invalid --out parameter: output file name is missing.\n");
					System.out.println( USAGE );
					return; 
				}
				p.mode = Mode.imageMode;
				p.input = args[i];
				continue;
			}
			
			if ( "--for".equals( args[i] ) ) {
				++i;
				if ( i == args.length ) {
					System.out.println("invalid --for parameter: text or character range is missing.\n");
					System.out.println( USAGE );
					return; 
				}
				p.text = args[i];
				continue;
			}
			
			if ( "--fontsize".equals( args[i] ) ) {
				++i;
				if ( i == args.length ) {
					System.out.println("invalid --fontsize parameter: font size is missing.\n");
					System.out.println( USAGE );
					return; 
				}
				
				try {
					p.fontSize = Integer.parseInt(args[i]);
				} catch (NumberFormatException e) {
					System.out.println("invalid parameter: invalid font size " + args[i] + "\n");
					System.out.println( USAGE );
					return; 
				}
				continue;
			}

			if ( "--scale".equals( args[i] ) ) {
				++i;
				if ( i == args.length ) {
					System.out.println("invalid --scale parameter: scale percent value is missing.\n");
					System.out.println( USAGE );
					return; 
				}
				
				try {
					String val = args[i];
					int idx = val.indexOf("%");
					if (idx > 0) {
						val = val.substring(0, idx);
					}
					p.scale = Integer.parseInt(val);
				} catch (NumberFormatException e) {
					System.out.println("invalid parameter: invalid font size " + args[i] + "\n");
					System.out.println( USAGE + "\n" );
					e.printStackTrace();
					return; 
				}
				
				continue;
			}
			
			if ( "--antialiased".equalsIgnoreCase( args[i] ) ) {
				p.antialiased = true;
				continue;
			}

			if ( "--fixFFFF".equalsIgnoreCase( args[i] ) ) {
				p.fixFFFF = true;
				continue;
			}
		}
		
		if ( p.mode == null ) {
			System.out.println("Error: no mode specified. Expected --font, --icon OR --image\n");
			System.out.println( USAGE );
			return; 
		}

		if ( p.input == null ) {
			System.out.println("Error: no input file specified.\n");
			System.out.println( USAGE );
			return; 
		}
		
		String res = "";
		
		switch (p.mode) {
		case fontMode:
			try {
				res = new ExportFont(p.input, p).getFontData();
			} catch (Exception e) {
				e.printStackTrace();
			}
			break;
		case iconMode:
			try {
				res = new ExportIcon(p.input, p).getIconData();
			} catch (Exception e) {
				e.printStackTrace();
			}
			break;
		case imageMode:
			try {
				res = new ExportImage(p.input, p).getImageData();
			} catch (Exception e) {
				e.printStackTrace();
			}
			break;
		}
		
		System.out.println(res);
    }
}
