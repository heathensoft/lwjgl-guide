
## What is a shader?



![screenshot](img/02/screenshot-chapter-2.png)

## Uploading Geometry to the GPU

```
[1]
float[] vertices = new float[] {
        /*{ V0 }*/-1.0f, 1.0f, 0.0f,/*position (xyz)*/0.2f, 0.1f, 0.4f,/*color (rgb)*/
        /*{ V1 }*/-1.0f,-1.0f, 0.0f,/*position (xyz)*/0.2f, 0.1f, 0.4f,/*color (rgb)*/
        /*{ V2 }*/ 1.0f, 1.0f, 0.0f,/*position (xyz)*/0.2f, 0.1f, 0.4f,/*color (rgb)*/
        /*{ V3 }*/ 1.0f, 1.0f, 0.0f,/*position (xyz)*/0.2f, 0.2f, 0.4f,/*color (rgb)*/
        /*{ V4 }*/-1.0f,-1.0f, 0.0f,/*position (xyz)*/0.2f, 0.2f, 0.4f,/*color (rgb)*/
        /*{ V5 }*/ 1.0f,-1.0f, 0.0f,/*position (xyz)*/0.2f, 0.2f, 0.4f,/*color (rgb)*/
};
[2.]
vertex_attrib_array = glGenVertexArrays();
[3.]
vertex_buffer_object = glGenBuffers();
[4.]
glBindVertexArray(vertex_attrib_array);
[5.]
glBindBuffer(GL_ARRAY_BUFFER,vertex_buffer_object);
[6.]
glBufferData(GL_ARRAY_BUFFER,vertices,GL_STATIC_DRAW);
[7.]
glVertexAttribPointer(0,3,GL_FLOAT,false,6 * Float.BYTES,0);
glVertexAttribPointer(1,3,GL_FLOAT,false,6 * Float.BYTES,3 * Float.BYTES);
glEnableVertexAttribArray(0);
glEnableVertexAttribArray(1);
glBindVertexArray(0);
```
1. We define our geometry. In this case, 6 vertices with position (3 float) and color (3 float).
2. We create a [vertex array object](https://www.khronos.org/opengl/wiki/vertex_Specification#Vertex_Array_Object) (VAO)
to store the format of the vertex data as well as references to the actual data buffers.
Describing the layout of the geometry. See [7.]
3. We tell opengl to generate a buffer for storing the geometry. A vertex buffer object (VBO).
It returns an integer reference for the buffer. No memory is allocated atp.
4. Then we bind the vertex array object. This tells opengl that future instructions will
affect the buffer. (until we bind another VAO or NULL)
5. For the same reason, we also bind the VBO (geometry buffer).
Note that a vertex buffer (VBO) and a vertex array object (VAO) are separate object types that take
up separate "slots" for binding. Binding the VBO does not unbind the VAO.
We bind it as a GL_ARRAY_BUFFER. The most common buffer type.
6. Here we upload the geometry (vertices array) to the currently bound GL_ARRAY_BUFFER (our VBO).
Since the geometry will stay the same for the duration of the program ("static"),
we can also give opengl a hint to let it know that this particular buffer will likely
not be modified in the future. OpenGL could use this to optimize performance.

---
**7. Now we tell opengl how we would like it to treat our data (geometry):**

One vertex consist of two elements: 3 float for position [x,y,z] and 3 float for color [r,g,b]
(It doesn't have to, but we defined it as such).
If we take a second look at our vertex shader, that's also how we specified its layout:
```
layout (location = 0) in vec3 a_position;
layout (location = 1) in vec3 a_color;
```
We need to tell opengl how the elements are stored in the buffer, and the stride between
each element. For our float vertices[] {...} array, the stride in float would be 6.
(**OpenGL operates in bytes**, but let's just stick with floats for now)

| V0 | V0 | V0 | V0 | V0 | V0 | V1 | V1 | V1 | V1 | V1 | V1 | V2 | V2 | V2 | V2 | V2 | V2 |
|----|----|----|----|----|----|----|----|----|----|----|----|----|----|----|----|----|----|
| x  | y  | z  | r  | g  | b  | x  | y  | z  | r  | g  | b  | x  | y  | z  | r  | g  | b  |
| 0  | 1  | 2  | 3  | 4  | 5  | 6  | 7  | 8  | 9  | 10 | 11 | 12 | 13 | 14 | 15 | 16 | 17 |

For each shader vertex operation, opengl needs to "increment the pointer by 6 to locate the next one".
OpenGL also need to know the internal layout of the 2 components, position and color.  

```
glVertexAttribPointer(0,3,GL_FLOAT,false,6 * Float.BYTES,0);
glEnableVertexAttribArray(0); // enable
glVertexAttribPointer(1,3,GL_FLOAT,false,6 * Float.BYTES,3 * Float.BYTES);
glEnableVertexAttribArray(1); // enable
```

* We tell opengl that our first component (position) is layout 0 in the shader
* it has 3 values [x,y,z]
* the values should be treated as floats
* the values should not be normalized (used to normalize integers from 0 to 1)
* the stride of one vertex is 6 floats
* the internal offset of the position component is 0
* the same for the color component except the layout location = 1, and the offset is 3 floats.

Finally, unbind the vertex array object.
```
glBindVertexArray(0);
```

Now the geometry is stored on the GPU and ready to be rendered

---

### NDC (Normalized Device Coordinates)

The OpenGL coordinate system ranges from -1 to 1 in all 3 axis.
For now, think of a box where the xy-plane is parallel to your screen,
and a z-axis extending into your screen with z = 1, where your fingers touch
the glass. As far as OpenGL is concerned, every visible object in located somewhere
inside that cube. Let's ignore the z-axis for now.

![normalize device coordinates](img/02/opengl-ndc.png)

As far as OpenGL is concerned, the bottom-left corner of your screen is (-1,-1) and
the top-right corner is (1,1). This is also known as NDC (normalized device coordinated).

*When dealing with objects in our game it's advantageous to use positions relative to world space,
then transform them to NDC space in our shaders with a transformation matrix.
We're not doing this yet, but we will come back to transforms and cameras in later chapters.*


### The Vertices

```
float[] vertices = new float[] {

        /*{ V0 }*/-1.0f, 1.0f, 0.0f,/*position (xyz)*/0.2f, 0.1f, 0.4f,/*color (rgb)*/
        /*{ V1 }*/-1.0f,-1.0f, 0.0f,/*position (xyz)*/0.2f, 0.1f, 0.4f,/*color (rgb)*/
        /*{ V2 }*/ 1.0f, 1.0f, 0.0f,/*position (xyz)*/0.2f, 0.1f, 0.4f,/*color (rgb)*/

        /*{ V3 }*/ 1.0f, 1.0f, 0.0f,/*position (xyz)*/0.2f, 0.2f, 0.4f,/*color (rgb)*/
        /*{ V4 }*/-1.0f,-1.0f, 0.0f,/*position (xyz)*/0.2f, 0.2f, 0.4f,/*color (rgb)*/
        /*{ V5 }*/ 1.0f,-1.0f, 0.0f,/*position (xyz)*/0.2f, 0.2f, 0.4f,/*color (rgb)*/
};
```

![first rectangle](img/02/first-rectangle.png)

We uploaded 6 vertices to the GPU and in the render loop,
we instruct the GPU to draw them as triangles with the following command:

```
glDrawArrays(GL_TRIANGLES,0,6);
```

The GPU will know that every 3 (in order of upload) vertices should be considered as a triangle.
The 6 vertices form 2 triangles that forms a rectangle covering our screen.
