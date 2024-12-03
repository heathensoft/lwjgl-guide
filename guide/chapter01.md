


## Previously

In [Chapter 0](chapter00.md) we added the LWJGL dependencies to our Gradle build script,
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
                
                glClear(GL_COLOR_BUFFER_BIT);
                glViewport(window.viewportX(),window.viewportY(),window.viewportW(),window.viewportH());
                // ---> DRAW OPERATIONS HERE <---
                window.swapRenderBuffers();
            } window.processUserEvents();
        } window.terminate();
    }
```

### Installing a logger

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


[Window Creation Hints](https://www.glfw.org/docs/latest/window_guide.html#window_hints)