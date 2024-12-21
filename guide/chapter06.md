
## Bitmap

A Bitmap is an array of bytes representing image color information (Pixels).
Commonly RED, GREEN, BLUE (and ALPHA) with a byte of data for each channel (32-bit color).

*Here each color-channel can have 256 different values ( 1 / 256 precision).
It's limited, but still good enough for most monitors (8-bit per channel [color depth](https://en.wikipedia.org/wiki/Color_depth)).*

The most used image-formats are compressed formats (like png or jpeg), but when I'm
talking about bitmaps I am referring to the uncompressed image. The array of "pixels".

The byte array begins in the TOP-LEFT pixel of the image and ends with the BOTTOM-RIGHT pixel.
Where the RED is the least significant byte. In the case for RGBA the layout would look like this:

| pixel 0 | pixel 0 | pixel 0 | pixel 0 | pixel 1 | pixel 1 | pixel 1 | pixel 1 |
|---------|---------|---------|---------|---------|---------|---------|---------|
| R       | G       | B       | A       | R       | G       | B       | A       |

Example: fetch color-value from bitmap (RGBA):

```
// The alpha value of the 5th pixel in the 7th row

int row = 7;
int col = 5;
int channels = 4; // rgba
int channel_offset = 3; // alpha channel offset

int pixel_offset = (row * image_width + col) * channels;
byte color_value = bitmap[ pixel_offset + channel_offset ];

```

Example: fetch a value from bitmap (black and white):

```
// The value of the 5th pixel in the 7th row

int row = 7;
int col = 5;
int channels = 1; // red only 
int channel_offset = 0; // just one channel

int pixel_offset = (row * image_width + col) * channels;
byte color_value = bitmap[ pixel_offset + channel_offset ];

```

### Loading images

To load images from IO, we are going to use the [stb image library](https://github.com/nothings/stb).
(LWJGL provides it for us). With it, we can load / decode png files.  

I've made the Resources and ExternalFile utility classes to load files, 
and a Bitmap class to store the decoded pixels:

```
public class Bitmap implements Disposable {
    private final ByteBuffer pixels;
    private final int width;
    private final int height;
    private final int channels;
    // ...
```

Making the process of loading images more convenient:

```
String image_path = ... ;       // path relative to the resources folder
int size = ... ;                // approximate size of the image (buffer will auto grow to fit)
boolean vFlip = false;          // if we need to flip the image vertically

ByteBuffer png = Resources.readToBuffer(image_path, size);
Bitmap bitmap = new Bitmap(png,vFlip);
```

Inside the Bitmap constructor we decode the png and store the pixels. 
The bitmap array is allocated by stb and must be freed before exiting the program.
That's why the Bitmap class implements Disposable. It's to signify that the object must be freed by us
and not the garbage collector.

## Bitmap to Texture

Now we can load our images and store them on the CPU. To be able to draw the images we need
to






