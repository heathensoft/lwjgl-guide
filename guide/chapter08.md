## Input Processing

Before looking at the Mouse and Keyboard classes, it's useful to take a look
at how input processing works in the main loop.

Our game-loop can be broken down into two loops. 

* One **outer loop** that runs at a target "frames per second".
* And an **inner loop** that runs at a target "updates per second".

If the target FPS > target UPS then the outer loop could run several
times without entering the inner loop.

And if the target FPS < target UPS, the inner loop might run several
times for each outer loop.

#### Example Code

```
fixed_time_step = 1/30; // update game 30 times / second

// runs at monitors refresh rate (with v-sync enabled)
// one loop = one frame
while(running) { 

    // time of the last frame
    time_accumulator += time.frameTimeSeconds();
    
    // update at fixed timestep
    // one loop = one update tick
    while (time_accumulator >= fixed_time_step) {
    
        // game logic
        // Here you wan't to check if any buttons were pressed etc.
        // I.e. LEFT_PRESSED -> Move left
        game.update(); 
        
        time_accumulator -= fixed_time_step;
    }
    
    // triggers input event callbacks
    // if any since the previous frame
    window.pollUserEvents();
    
    game.render();
    
    // v-sync happens here (if enabled)
    // waits to sync up with the monitor refresh rate.
    // display the framebuffer content
    window.swapRenderBuffers();

}
```

You can see that for every frame we are polling for [user events](https://www.glfw.org/docs/3.3/input_guide.html#events).
Key presses, cursor movements etc. And if there were any events, glfw will trigger appropriate callbacks.
We have set up callback listeners in the GLFWWindow class. For example:

```
glfwSetCursorPosCallback(window, new GLFWCursorPosCallback() {
    public void invoke(long window, double xpos, double ypos) {
        mouse.onCursorHover(xpos,ypos);
    }
});
```

### Queueing Input

When game update ticks and input polling happens at different time intervals, we have to be careful
not to "miss input". 

Let's say the game updates at 30 ups and the outer / render loop runs at 144 fps.
The pollUserEvents() picks up that the user has pressed the LEFT_ARROW_KEY.
But once the next update tick happens (could be 4 frames later) the user has already released
the arrow key and the game logic could miss the button press entirely.

We also need to be careful the other way around (UPS > FPS). In that case a typing a character
might be read as the user holding that character down or other similar bugs.

To solve these problems the callbacks called when polling user events queues up input.

```
void keyEventCallback(int key) {
    queue.push(key);
}
```
or
```
void mouseScrollEventCallback(float amount) {
    scroll_amount += amount;
}
```
Then during the update tick we process the inputs queued from the callbacks.

```
void processInput() {
    while(!queue.isEmpty()) 
        process(queue.pop());
}
```
Main loop

```
fixed_time_step = 1/30; // update game 30 times / second

// runs at monitors refresh rate (with v-sync enabled)
// one loop = one frame
while(running) { 

    // time of the last frame
    time_accumulator += time.frameTimeSeconds();
    
    // input should be processed
    boolean process_input = true;
    
    // update at fixed timestep
    // one loop = one update tick
    while (time_accumulator >= fixed_time_step) {
    
        // We also takes care not to process input
        // more than once each frame.
        if (process_input) {
            // process the input queues
            processInput();
            process_input = false;
        }
    
        // game logic
        // Here you wan't to check if any buttons were pressed etc.
        // I.e. LEFT_PRESSED -> Move left
        game.update(); 
        
        time_accumulator -= fixed_time_step;
    }
    
    // triggers input event callbacks
    // if any since the previous frame
    // queues up input to be processed
    window.pollUserEvents();
    
    game.render();
    
    // v-sync happens here (if enabled)
    // waits to sync up with the monitor refresh rate.
    // display the framebuffer content
    window.swapRenderBuffers();

}
```

## Keyboard





