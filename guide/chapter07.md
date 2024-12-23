
## Streamlining OpenGL Texture Calls

What kind of Java programmers would we be if we wouldn't immediately start making **everything**
into objects. From RendererTest of the previous chapter we initialized our texture with the following code:  

```
// TEXTURE

// Load a png file from the resources folder
ByteBuffer png = Resources.readToBuffer("texture-test.png",512);
Bitmap bitmap = new Bitmap(png,false); // decode the png to a bitmap
texture = glGenTextures(); // generate a texture (reference used for future operations)
glActiveTexture(GL_TEXTURE0); // activate texture slot 0 (future operations apply to texture slot 0)
glBindTexture(GL_TEXTURE_2D,texture); // and bind the texture to slot 0 (texture is now the active texture)
// specify format and allocate memory on the gpu (GL_RGBA8 tells opengl that the size of a pixel is 4 * 8 = 32bit)
glTexStorage2D(GL_TEXTURE_2D,1,GL_RGBA8,bitmap.width(),bitmap.height());
// transfer the actual pixel data to gpu storage,
// telling opengl how to interpret the data (we have 4 channels rgba of type unsigned byte)
glTexSubImage2D(GL_TEXTURE_2D,0,0,0,bitmap.width(),bitmap.height(),GL_RGBA,GL_UNSIGNED_BYTE,bitmap.pixels());
// Note: You could also allocate and transfer the pixels in one operation.
// telling opengl how the texture should be sampled
glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MIN_FILTER,GL_NEAREST);
glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MAG_FILTER,GL_NEAREST);
glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_WRAP_S,GL_REPEAT);
glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_WRAP_T,GL_REPEAT);
// we no longer need the bitmap stored on the cpu
// we free the bitmap to avoid memory leak
bitmap.dispose();
```

Shortened to:
```
// TEXTURE

ByteBuffer png = Resources.readToBuffer("texture-test.png",512);
Bitmap bitmap = new Bitmap(png,false); // decode the png to a bitmap
texture = bitmap.asTexture(); // create texture from bitmap
texture.bindToSlot(0);  // bind to texture slot 0
texture.filterNearest(); // sample nearest pixel (as opposed to linear filtering)
texture.textureRepeat(); // UV repeats
bitmap.dispose(); // free the bitmap
```

### OOP and Opengl (Sidenote)
I encourage you NOT to force opengl objects to fit the Java OOP paradigm.
OpenGL works like a state machine. Calls like:

```
glBindTexture(GL_TEXTURE_2D,texture);
```
will bind the opengl texture object to the active texture slot.
Future texture calls / operations will modify that textures state.
Even if you can encapsulate a texture (or other opengl objects) into a Java class, it's still **very** easy to
do operations on another object than the intended one. You might assume calling:

```
texture.alterSomething(new_value);
```
would change the opengl object you intended to encapsulate, but forgot to bind the actual opengl texture object.
So while thinking you are modifying the texture you're actually modifying another one.
Things like this can make your program prone to bugs and be... very frustrating.
So just be aware of this if you're coming from Java. I'd recommend getting comfortable using the
opengl calls directly before trying to wrap and hide opengl functionality in Java objects.   

### That said...

Here's a fancy new class to encapsulate opengl textures.

```
public class Texture implements Disposable {

    private TextureFormat format;   // texture format
    private int id;                 // opengl texture reference
    private int mip_levels;         // mipmap levels
    private final int target;       // texture target
    private final int width;        // width of texture
    private final int height;       // height of texture (1D Array / 2D)
    private final int depth;        // depth of texture (2D Array / 3D)
    private boolean allocated;      // texture has been allocated
```


```
// VERTICES

Resolution app_res = Engine.get().window().gameResolution();
final float x1 = 0.0f;
final float y1 = 0.0f;
final float x2 = app_res.width();
final float y2 = app_res.height();
final float u1 = 0.0f;
final float v1 = 0.0f;
final float u2 = (float) app_res.width() / tex.width();     // U2 > 1.0f (repeats)
final float v2 = (float) app_res.height() / tex.height();   // V2 > 1.0f (repeats)
final float[] vertices = new float[] {

        /*{ V0 }*/x1, y2, 0,/*position (xyz)*/u1, v1,/*texture coordinate (uv)*/
        /*{ V1 }*/x1, y1, 0,/*position (xyz)*/u1, v2,/*texture coordinate (uv)*/
        /*{ V2 }*/x2, y2, 0,/*position (xyz)*/u2, v1,/*texture coordinate (uv)*/
        /*{ V3 }*/x2, y2, 0,/*position (xyz)*/u2, v1,/*texture coordinate (uv)*/
        /*{ V4 }*/x1, y1, 0,/*position (xyz)*/u1, v2,/*texture coordinate (uv)*/
        /*{ V5 }*/x2, y1, 0,/*position (xyz)*/u2, v2,/*texture coordinate (uv)*/

};
```

![screenshot](img/07/screenshot-chapter-7-0.png)

