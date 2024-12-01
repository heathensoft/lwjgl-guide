


### Installing a logger
It's better to install a logger now rather than later.
It gives us more meaningful output than printing messages to a console.
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