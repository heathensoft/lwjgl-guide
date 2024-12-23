
## Streamlining OpenGL Texture Calls

### Changes
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

Now, shortened to:
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
In this chapter we'll take a closer look at textures while making utility classes
for opengl texture calls.

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

I made a new class to encapsulate opengl textures.

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

#### Texture Format (Image format)

> An [Image Format](https://www.khronos.org/opengl/wiki/Image_Format) describes the way that
> the images in Textures and renderbuffers store their data. They define the meaning of the image's data.

The format is used to tell opengl what kind of image format the texture is and how it should be interpreted.
We touched upon this in the previous chapter. When we allocate, upload, copy or modify a texture, opengl
need to know how it should interpret the continuous array of bytes (texture data). 
How many color channels are there, what's the size of each component, should the values be normalized
in the shader, should they even be interpreted as colors at all? Etc.

I encourage you to read about the [image formats](https://www.khronos.org/opengl/wiki/Image_Format).
The TextureFormat class (enum) is a collection of common image formats and related values.

#### Texture Target

The [Texture Target](https://www.khronos.org/opengl/wiki/texture#Theory) is one of:

>* **GL_TEXTURE_1D:** Images in this texture all are 1-dimensional. They have width, but no height or depth.
>* **GL_TEXTURE_2D:** Images in this texture all are 2-dimensional. They have width and height, but no depth.
>* **GL_TEXTURE_3D:** Images in this texture all are 3-dimensional. They have width, height, and depth.
>* **GL_TEXTURE_RECTANGLE:** The image in this texture (only one image. No mipmapping) is 2-dimensional. Texture coordinates used for these textures are not normalized.
>* **GL_TEXTURE_BUFFER:** The image in this texture (only one image. No mipmapping) is 1-dimensional. The storage for this data comes from a Buffer Object.
>* **GL_TEXTURE_CUBE_MAP:** There are exactly 6 distinct sets of 2D images, each image being of the same size and must be of a square size. These images act as 6 faces of a cube.
>* **GL_TEXTURE_1D_ARRAY:** Images in this texture all are 1-dimensional. However, it contains multiple sets of 1-dimensional images, all within one texture. The array length is part of the texture's size.
>* **GL_TEXTURE_2D_ARRAY:** Images in this texture all are 2-dimensional. However, it contains multiple sets of 2-dimensional images, all within one texture. The array length is part of the texture's size.
>* **GL_TEXTURE_CUBE_MAP_ARRAY:** Images in this texture are all cube maps. It contains multiple sets of cube maps, all within one texture. The array length * 6 (number of cube faces) is part of the texture size.
>* **GL_TEXTURE_2D_MULTISAMPLE:** The image in this texture (only one image. No mipmapping) is 2-dimensional. Each pixel in these images contains multiple samples instead of just one value.
>* **GL_TEXTURE_2D_MULTISAMPLE_ARRAY:** Combines 2D array and 2D multisample types. No mipmapping.

We will mostly be dealing with 2D and 2D Array textures.

#### Mip maps

[Mip maps](https://www.khronos.org/opengl/wiki/texture#Mip_maps) are pre-shrunk versions of the full-sized image.
It's often helpful to sample from smaller versions of an image if the object is far from view (the camera) or the
objects surface angle to the view gets "very sharp". The sampler can then pick the most suited "mip map" and
avoid aliasing artifacts.

> When sampling a texture, the implementation will automatically select which mipmap to use 
> based on the viewing angle, size of texture, and various other factors.

![mipmap](img/07/mipmap.png)

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

