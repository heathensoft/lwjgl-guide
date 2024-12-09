
## Streamlining Shader Creation

### Previously
Setting up shaders in RenderTest.java (previous chapters) involved a lot of code.

```
// create an empty shader program
shader_program = glCreateProgram();
// create vertex shader object
int vertex_shader = glCreateShader(GL_VERTEX_SHADER);
// copy the shader source characters into the shader object
glShaderSource(vertex_shader,vertex_shader_source);
// compile the vertex shader
glCompileShader(vertex_shader);
int compile_status = glGetShaderi(vertex_shader,GL_COMPILE_STATUS);
if (compile_status == GL_FALSE) {
    String error_message = glGetShaderInfoLog(vertex_shader);
    glDeleteShader(vertex_shader);
    glDeleteProgram(shader_program);
    throw new Exception(error_message);
} // attach the vertex shader to the program
glAttachShader(shader_program,vertex_shader);

// create fragment shader object
int fragment_shader = glCreateShader(GL_FRAGMENT_SHADER);
// copy the shader source characters into the shader object
glShaderSource(fragment_shader,fragment_shader_source);
// compile the fragment shader
glCompileShader(fragment_shader);
compile_status = glGetShaderi(fragment_shader,GL_COMPILE_STATUS);
if (compile_status == GL_FALSE) {
    String error_message = glGetShaderInfoLog(fragment_shader);
    glDeleteShader(vertex_shader);
    glDeleteShader(fragment_shader);
    glDeleteProgram(shader_program);
    throw new Exception(error_message);
} // attach the fragment shader to the program
glAttachShader(shader_program,fragment_shader);

// link the program
// each attached shaders will be used to create an executable
// that will run for that stage in the graphics pipeline
glLinkProgram(shader_program);

// Once the shaders have been linked, there is no need to keep them around
glDetachShader(shader_program,vertex_shader);
glDetachShader(shader_program,fragment_shader);
glDeleteShader(vertex_shader);
glDeleteShader(fragment_shader);
int link_status = glGetProgrami(shader_program,GL_LINK_STATUS);
if (link_status == GL_FALSE) {
    String error_message = glGetProgramInfoLog(shader_program);
    glDeleteProgram(shader_program);
    throw new Exception(error_message);
}
// checks to see whether the executables contained in
// program can execute given the current OpenGL state
glValidateProgram(shader_program);
int validate_status = glGetProgrami(shader_program,GL_VALIDATE_STATUS);
if (validate_status == GL_FALSE) {
    String error_message = glGetProgramInfoLog(shader_program);
    glDeleteProgram(shader_program);
    throw new Exception(error_message);
}
```
## Shader and ShaderProgram Wrapper classes

To simplify the process of setting up shaders I've med two new classes.
* **Shader.java** - code for a specific stage in the graphics pipeline
* **ShaderProgram.java** - all the stages linked together to a program


```
String vert_shader_source = Resources.asString("render-test.vert");
String frag_shader_source = Resources.asString("render-test.frag");
Shader vert_shader = new Shader(vert_shader_source, Shader.Type.VERT_SHADER);
Shader frag_shader = new Shader(frag_shader_source, Shader.Type.FRAG_SHADER);
shader_program = new ShaderProgram(vert_shader,frag_shader);
shader_program.detachShaders(true);
```
This code does pretty much the same thing as the previous block of code.
Except, now we load external shader files instead of using strings.
So we load the shader code to strings. 

Then we create the Shader objects using
the strings as argument. Creating a new Shader will upload the
string to gpu memory, then compile the glsl code. If the glsl is
not valid, the Shader constructor will throw an exception with a
message telling you what went wrong. Even the line where the compilation failed.

If the Shaders compiled successfully, we can use them to create a new ShaderProgram.
Creating a new shader program will link the compiled shaders together to create
a cohesive shader program. If the linking process failed the constructor will throw
an exception telling you what went wrong.

After linking the programs together you are free to detach and delete the Shaders
from the program. (Successfully linking the program creates an executable. Once
you have the executable you no longer need to keep the source code on the gpu.)


## GLSL Plugin (Optional)

Now that we have a way to load shader files, let's download the GLSL plugin for Intellij.


![glsl-plugin](img/05/glsl-plugin.png)

It's useful to have some syntax highlighting for when we start programming shader files.

![glsl-plugin-example](img/05/glsl-plugin-example-code.png)


