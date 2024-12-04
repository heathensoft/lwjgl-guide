
## The Source Code as a Guide

The textual part of the guide will cover changes from one chapter to the next, explain key concepts and point to helpful external resources.
But it won't be covering *everything*. By browsing the source code you'll find the state of the program
between each chapter. (I'll try not to cover more than a few topics each chapter,
making it easier to keep up with changes in the source code from one chapter to the next.)

## Documentation

Both OpenGL and GLFW have excellent documentation.
And LWJGL have documented every method with java doc.
Often with links to the documentation / reference pages online.

*If you're using Intellij IDE, hovering over the LWJGL library methods will show you the doc.
And if not, an option should pop up to download the source files.*

* [GLFW Documentation](https://www.glfw.org/docs/latest/) 
* [OpenGL Reference Pages](https://registry.khronos.org/OpenGL-Refpages/gl4/)


## Previously

In [Chapter 0](chapter00.md) we added the LWJGL dependencies to our gradle build script,
and copy-pasted the LWJGL example program into **Main.java**:

```
public void run() {
        System.out.println("Hello LWJGL " + Version.getVersion() + "!");
        init();
        loop();
        
        // 11.
        glfwFreeCallbacks(window);
        glfwDestroyWindow(window);
        glfwTerminate();
        glfwSetErrorCallback(null).free();
    }

    private void init() {
        
        // 1.
        GLFWErrorCallback.createPrint(System.err).set();
        
        // 2.
        if ( !glfwInit() )
            throw new IllegalStateException("Unable to initialize GLFW");
        
        // 3.
        glfwDefaultWindowHints(); 
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE); 
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
        
        // 4.
        window = glfwCreateWindow(300, 300, "Hello World!", NULL, NULL);
        if ( window == NULL ) throw new RuntimeException("Failed to create the GLFW window");
        
        // 5.
        glfwSetKeyCallback(window, (window, key, scancode, action, mods) -> {
            if ( key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE )
                glfwSetWindowShouldClose(window, true);
        });
        
        // 6.
        try ( MemoryStack stack = stackPush() ) {
            IntBuffer pWidth = stack.mallocInt(1); // int*
            IntBuffer pHeight = stack.mallocInt(1); // int*
            glfwGetWindowSize(window, pWidth, pHeight);
            GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());
            glfwSetWindowPos(
                    window,
                    (vidmode.width() - pWidth.get(0)) / 2,
                    (vidmode.height() - pHeight.get(0)) / 2
            );
        }
        // 7.
        glfwMakeContextCurrent(window);
        glfwSwapInterval(1);
        glfwShowWindow(window);
    }

    private void loop() {
        // 8.
        GL.createCapabilities();
        
        // 9.
        glClearColor(1.0f, 0.0f, 0.0f, 0.0f);
        
        // 10.
        while ( !glfwWindowShouldClose(window) ) {
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); 
            glfwSwapBuffers(window); 
            glfwPollEvents();
        }
    }
```
Since the program is relatively short, let's break it down sequentially:

1. Tell GLFW to print error messages to the standard output stream
2. Initialize GLFW. Most GLFW functions will not work before doing this
3. We tell GLFW how to create the window. 
There are [Plenty of Hints](https://www.glfw.org/docs/latest/window_guide.html#window_hints) to provide.
We'll look closer at some of them later in this chapter.
4. Create the new "windowed mode" window with a 300x300px resolution and a title
5. Set a user key-callback. The function signals the window to close once the ESCAPE key is released
6. Here we query the window and monitor dimensions, and center the window in monitor space.
If you're wondering about the "try-with-resources" block, take a look at this blog about [Memory Management in LWJGL](https://blog.lwjgl.org/memory-management-in-lwjgl-3/).
That blog in particular pops up frequently at "stack overflow".
Remember, the LWJGL library is a Java wrapper around native code written in C.
Java devs will get increasingly familiar with allocating and freeing off-heap memory as we get into future chapters.
7. Make the OpenGL context current in the calling thread (much of the OpenGL functionality, like rendering operations,
can only be called from the context thread. The LWJGL java-doc usually explicitly tells you whether
a method can be called from the context thread or any thread).
*glfwSwapInterval(1)* is equivalent to turning on [Vertical Synchronization](https://en.wikipedia.org/wiki/Screen_tearing#Vertical_synchronization)
(vsync) to match the framerate of the monitor.  
8. Creates the GLCapabilities instance and makes the OpenGL bindings available for use.
9. Tell opengl to clear the window framebuffer with the color RED when calling glClear.
10. The program runs until the window has been signaled to close.
Either programmatically or by pressing the close button.
*glClear()* clears the current (window) framebuffer to RED like we specified earlier.
In this case we tell opengl to clear both the color and the depth buffer (if it has one).
(We'll come back to depth buffers in a later chapter, for now we're just interested in the color part)
*glSwapBuffers()* swaps the front buffer and back buffer. The back buffer is the buffer we are rendering to,
and the front buffer is the one displayed in the window.
Since we enabled vsync earlier, this syncs up with the monitors framerate.
Lastly *glfwPollEvents()* checks for user input / display - events.
Callbacks are executed, like the key event callback we specified earlier.
11. Finally, we free the callbacks and terminate the window.
As these objects are not tracked by the garbage collector. 

    
## The new Main.java

```
public void run() {
        GLFWWindow window = new GLFWWindow();
        try { BootConfiguration configuration = new BootConfiguration();
            configuration.window_title = "lwjgl-guide";
            configuration.supported_resolutions.add(Resolution.R_1280x720);
            configuration.supported_resolutions.add(Resolution.R_1920x1080);
            configuration.windowed_mode = true;
            configuration.resizable_window = true;
            configuration.vsync_enabled = true;
            org.tinylog.configuration.Configuration.set("writer","console");
            org.tinylog.configuration.Configuration.set("writer.format","{date: HH:mm:ss.SS} {level}: {message}");
            window.initialize(configuration);
        } catch (Exception e) {
            Logger.error(e);
            System.exit(0);
        }
        int escape_key_prev = GLFW_RELEASE;
        int toggle_key_prev = GLFW_RELEASE;
        glClearColor(1.0f, 0.0f, 0.0f, 0.0f);
        while (!window.shouldClose()) {
            if (!window.isMinimized()) {
            
                int escape_key_state = glfwGetKey(window.handle(),GLFW_KEY_ESCAPE);
                int toggle_key_state = glfwGetKey(window.handle(),GLFW_KEY_F1);
                if (escape_key_state == GLFW_PRESS && escape_key_prev == GLFW_RELEASE) {
                    window.signalToClose();
                }  else if (toggle_key_state == GLFW_PRESS && toggle_key_prev == GLFW_RELEASE) {
                    if (window.isWindowedMode()) window.fullScreen();
                    else window.windowedMode(Resolution.R_1280x720);
                } escape_key_prev = escape_key_state;
                toggle_key_prev = toggle_key_state;
                
                glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
                glViewport(window.viewportX(),window.viewportY(),window.viewportW(),window.viewportH());
                // ---> DRAW OPERATIONS HERE <---
                window.swapRenderBuffers();
            } window.processUserEvents();
        } window.terminate();
    }
```
Our new program does much of the same as the LWJGL example program above:
Create a window and clear the screen to RED each frame. But now we have created
some utility classes to generify window creation and initialization.
(The program also toggles between fullscreen and windowed mode by pressing F1)

 

### BootConfiguration Class

We have created a BootConfiguration class that holds some variables that instructs our program
how to initialize the window.
When initializing the window we pass in a BootConfiguration instance with some configurable fields.

```
public List<Resolution> supported_resolutions = new ArrayList<>();
public String window_title = "";
public boolean resizable_window = false;
public boolean vsync_enabled = true;
public boolean cursor_enabled = true;
public boolean windowed_mode = false;
public int windowed_mode_width = 1280;
public int windowed_mode_height = 720;
public int target_ups = 60;
public int target_fps = 60;
```
Do we want a fullscreen window or a windowed one? 
What resolutions does the game support? 
Do we want vsync to be enabled by default? etc.
(Later on we could add more fields if we need to)

### Resolution Class

The Resolution class is a convenience object (Java record) with a width and height (in pixels).
It contains som static instances of the [most common desktop resolutions](https://gs.statcounter.com/screen-resolution-stats/desktop/worldwide),
and a static method to sort a list of resolutions by the closest matching resolution to some desired resolution.
(I.e. the supported resolution that most closely resembles the resolution of the monitor)

### Installing a logger
Before initializing the window we set up a logger with two lines of code:  
```
org.tinylog.configuration.Configuration.set("writer","console");
org.tinylog.configuration.Configuration.set("writer.format","{date: HH:mm:ss.SS} {level}: {message}");
```
It's better to install a logger now rather than later.
It'll give us more meaningful output than printing messages to a console.
You can use any logger you'd like, but I'll be using [tinylog](https://tinylog.org/v2/).
Just add the following lines to our buildscript (build.gradle.kts):
```
val tinyLogVersion = "2.7.0"
```
And adjust the value to the latest available version
```
implementation("org.tinylog:tinylog-api:$tinyLogVersion")
implementation("org.tinylog:tinylog-impl:$tinyLogVersion")
```
We want a logger for debugging our code.

### Initializing the Window

We'll jump into the initialize method when covering the GLFWWindow class later on.
```
window.initialize(configuration);
```
This provides the GLFWWindow instance with our BootConfiguration instance with instructions for
how to create the window.

### The Main loop

```
while (!window.shouldClose()) {
    if (!window.isMinimized()) {
    
        int escape_key_state = glfwGetKey(window.handle(),GLFW_KEY_ESCAPE);
        int toggle_key_state = glfwGetKey(window.handle(),GLFW_KEY_F1);
        if (escape_key_state == GLFW_PRESS && escape_key_prev == GLFW_RELEASE) {
            window.signalToClose();
        }  else if (toggle_key_state == GLFW_PRESS && toggle_key_prev == GLFW_RELEASE) {
            if (window.isWindowedMode()) window.fullScreen();
            else window.windowedMode(Resolution.R_1280x720);
        } escape_key_prev = escape_key_state;
        toggle_key_prev = toggle_key_state;
        
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        glViewport(window.viewportX(),window.viewportY(),window.viewportW(),window.viewportH());
        // ---> DRAW OPERATIONS HERE <---
        window.swapRenderBuffers();
    } window.processUserEvents();
} window.terminate();
```
Each frame, while the window is not signalled to close and the window is not minimized,
we clear the windows framebuffer to RED and swap the front and back buffer
(We still check for user events even if the window is minimized, as restoring the window is a user event)

We also provide OpenGL with a "viewport", which is the area of the window to draw to.
```
glViewport(window.viewportX(),window.viewportY(),window.viewportW(),window.viewportH());
```
As the actual resolution of the windows framebuffer might not equal the desired resolution of our game.
I.e. The game supports a resolution of 1280x720px. That's an aspect ratio of:
```
1280 / 720 = 1.77...
```
Even if the window is resized, we'd like to keep our target aspect ratio.
So we calculate an appropriate "rectangle" to fit inside our actual window,
and provide OpenGL with that rectangle. Future render commands to OpenGL will be
relative to the viewport.

*Since we are not rendering anything, the viewport won't be visible atp. Once we start rendering,
the viewport will become obvious*

## The Window

The GLFWWindow class is basically a convenience wrapper around the glfw window and the opengl context.
It initializes and terminates GLFW / OpenGL, handles user input / display -events and 
provide common window related functionality, like swapping between fullscreen and windowed mode.
```
private List<Resolution> supported_resolutions; // resolutions supported by our game 
private Resolution game_resolution; // The current resolution
private boolean game_resolution_changed; // found a better supported resolution for the game
private long window;                // glfw window pointer
private int target_fps;             // requested target FPS without vsync
private int target_ups;             // game logic updates per second
private int framebuffer_w;          // width of the window framebuffer in pixels
private int framebuffer_h;          // height of the window framebuffer in pixels
private int viewport_x;             // viewport x position relative to the framebuffer in pixels
private int viewport_y;             // viewport y position relative to the framebuffer in pixels
private int viewport_w;             // width of the viewport
private int viewport_h;             // height of the viewport
private boolean minimized;          // whether the window is minimized
private boolean vsync_enabled;      // limits fps to the display frame rate
```

### Initialization

#### [Error Callbacks](https://www.glfw.org/docs/latest/intro_guide.html#error_handling)

Set an error callback function to log any glfw related errors.
```
glfwSetErrorCallback(new GLFWErrorCallback() {
    public void invoke(int error, long description) {
        if (error != GLFW_NO_ERROR) {
            Logger.error("GLFW ERROR[{}]: {}",
                    error,GLFWErrorCallback.getDescription(description));
        }
    }
});
```
#### Monitor

We ask for a handle for the primary monitor,
and get a GLFWVidMode object with the current resolution and refresh rate.
We need this to create a fullscreen window, or centering a windowed one.

```
long monitor = glfwGetPrimaryMonitor();
if (monitor == 0L) {
    glfwTerminate();
    freeGLFWErrorCallback();
    throw new Exception("unable to detect primary monitor");
}

GLFWVidMode display = glfwGetVideoMode(monitor);
if (display == null) {
    glfwTerminate();
    freeGLFWErrorCallback();
    throw new Exception("unable to get the primary monitors current video mode");
}
```

#### [Window Hints](https://www.glfw.org/docs/latest/window_guide.html#window_hints)

>There are a number of hints that can be set before the creation of a window and context. 
>Some affect the window itself, others affect the framebuffer or context. 
>These hints are set to their default values each time the library is initialized with glfwInit. 
>Integer value hints can be set individually with glfwWindowHint and string value hints with glfwWindowHintString. 
>You can reset all at once to their defaults with glfwDefaultWindowHints. Some hints are platform specific. 
>These are always valid to set on any platform but they will only affect their specific platform. 
>Other platforms will ignore them. Setting these hints requires no platform specific headers or calls.

First, we want our window to be hidden (until the initialization is complete)
and the mouse cursor to be centered in the window.
```
glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
glfwWindowHint(GLFW_CENTER_CURSOR, GLFW_TRUE);
```
We are developing for desktop (windows, mac and linux) and not for an embedded system
like mobile. So we tell we'll need the opengl api and not the opengl es api.
```
glfwWindowHint(GLFW_CLIENT_API, GLFW_OPENGL_API);
```
Should the window be resizable in windowed mode? 
```
glfwWindowHint(GLFW_RESIZABLE, config.resizable_window ? GLFW_TRUE : GLFW_FALSE);
```
[Multisampling](https://en.wikipedia.org/wiki/Multisample_anti-aliasing) is not necessary for our purposes.
```
glfwWindowHint(GLFW_SAMPLES, 0); // 4 for antialiasing
```
The minimum version of OpenGL needed to run the application.
```
glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 4);
glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 4);
```
We want the refresh rate to be the same as the monitors current video modes' refresh rate.
The monitor can have multiple options to select from but by using the current video mode,
toggling between a fullscreen game and other desktop applications become seamless.
```
GLFWVidMode display = glfwGetVideoMode(monitor);
glfwWindowHint(GLFW_REFRESH_RATE,display.refreshRate());
```
Finally, we need to specify forward compatibility for Mac users.
(Read more about it in the GLFW documentation for window hints)
```
if (OS.name == OS.NAME.MAC) {
    glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GL_TRUE);
    glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_COMPAT_PROFILE);
} else glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
```

#### [Window Creation](https://www.glfw.org/docs/latest/window_guide.html#window_creation)

Based on whether we wanted to initialize a full screen window or a windowed one.
For a windowed mode window we provide the configurations width and height and set monitor to "0" (windowed).
If fullscreen was specified, we provide the monitor width, height and the monitor handle.

```
if (config.windowed_mode) {
    window = glfwCreateWindow(
            config.windowed_mode_width,
            config.windowed_mode_height,
            config.window_title,0L,0L);
} else window = glfwCreateWindow(
            display.width(),
            display.height(),
            config.window_title,
            monitor,0L);
```
#### Querying the actual dimensions

It's not a given that the window was created with the specified dimensions.
So after the window is created, we need to query the actual size of the window framebuffer:

```
glfwGetFramebufferSize(window,w,h);
framebuffer_w = w.get(0);
framebuffer_h = h.get(0);
```
That's the actual framebuffer size in pixels for our window.

#### Picking the Game Resolution

The BootConfiguration should provide at least one resolution-option supported by our game. 
We pick the option that most closely matches the window framebuffer.
This is a sorting operation that takes both aspect ratio and "optimal use of space" into account.

*The game logic should only need to concern itself with the supported resolution (even
if the window framebuffer resolution is something else entirely). We'll get more into this in a later chapter.*

#### Setting callbacks for display changing events

We provide reactive functions for a few events: 
```
glfwSetWindowIconifyCallback    // window was minimized or restored
glfwSetFramebufferSizeCallback  // window (framebuffer) was resized
glfwSetMonitorCallback          // a monitor was connected or disconnected
```
*We'll take a closer look at framebuffer size callback when we discuss viewports below.*

#### Finally

```
glfwMakeContextCurrent(window); // make the opengl context current in the calling thread
glfwShowWindow(window);         // make the window visible
GL.createCapabilities();        // makes the opengL bindings available for use.
```
## Game Resolution and The Viewport

```
glfwSetFramebufferSizeCallback(window, new GLFWFramebufferSizeCallback() {
    public void invoke(long window, int width, int height) {
        framebufferResizeEvent(width,height);
    }
});
```
```
private void framebufferResizeEvent(int framebuffer_w, int framebuffer_h) {
    this.framebuffer_w = framebuffer_w; 
    this.framebuffer_h = framebuffer_h;
    Resolution framebuffer_resolution = new Resolution(framebuffer_w,framebuffer_h);
    Resolution.sortByClosest(framebuffer_resolution, supported_resolutions);
    Resolution closest_resolution = supported_resolutions.getFirst();
    if (!closest_resolution.equals(game_resolution)) {
        game_resolution = closest_resolution;
        game_resolution_changed = true;
    } fitViewport(framebuffer_w,framebuffer_h);
}
```

```
private void fitViewport(int framebuffer_w, int framebuffer_h) {
    float game_aspect_ratio = game_resolution.aspectRatio();
    viewport_w = framebuffer_w;
    viewport_h = Math.round(viewport_w / game_aspect_ratio);
    if (viewport_h > framebuffer_h) {
        viewport_h = framebuffer_h;
        viewport_w = Math.round(viewport_h * game_aspect_ratio);
    } viewport_x = Math.round((framebuffer_w / 2f) - (viewport_w / 2f));
    viewport_y = Math.round((framebuffer_h / 2f) - (viewport_h / 2f));
}
```






