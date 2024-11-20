# pixels.util
TTF and image import utility for Pixels/uText libraries

https://github.com/zxfr/Pixels

https://github.com/zxfr/uText

Usage 1:

```java -jar pixels.util-1.0.jar -Xmx512m [-Djava.awt.headless=true] --font "<font file path>" [--fontsize 18] [--for "<Your text | #x0021-#x0024 #00048-#00057>"] [--antialiased]  [--out "<output file path>"]```

Usage 2:

```java -jar pixels.util-1.0.jar -Xmx512m [-Djava.awt.headless=true] --icon "<image file path>" [--out "<output file path]>"]```

Usage 3:

```java -jar pixels.util-1.0.jar -Xmx512m [-Djava.awt.headless=true] --icon "<font file path>" --glyph <W | #x0022> [--fontsize 18] [--antialiased] [--out "<output file path>"]```

Usage 4:

```java -jar pixels.util-1.0.jar -Xmx512m [-Djava.awt.headless=true] --image "<image file path>" [--scale 50%] [--out "<output file path>"] [--fixFFFF]```

# Build instructions
```
git clone https://github.com/zxfr/pixels.util.git
cd pixels.util/
mvn clean install
cd bin/
```
