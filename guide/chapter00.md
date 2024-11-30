## Setting up the programming environment

For the development I will be using JetBrains Intelli IDEA Community Edition.
If you've had some experience programming in Java, chances are you're already somewhat familiar with either Intellij or Eclipse.

An IDE can make project management easier.
Build tools and Version Control are usually neatly integrated with the IDE,
and you can execute buildscripts and git commands from a UI instead of using a terminal.

*I won't spend much time on installation, as I suspect most of you have some form of a setup already.*

### Installation

*These are the installation steps for Windows 11.
Some of the steps are different depending on your platform, but you can easily find resources online.*

#### JDK (Java Development Kit)

1. Go to the [Oracle](https://www.oracle.com/java/technologies/downloads/) download page for the latest version
2. Download the "x64 Installer" (Windows)
3. Run the installer executable and select an appropriate directory. Make sure to remember the path for later (default path is fine)

#### Intellij IDEA

1. Install [Intellij IDE](https://www.jetbrains.com/idea/download/?section=windows)
2. In IntelliJ select: new -> project...
3. If you find the installed JDK in the JDK drop-down (either selected or detected), you're done
4. If not, click "Add JDK from disk" and provide the installation path from earlier
5. From now on Intellij should be aware of the installed JDK

#### Without an IDE

If you prefer working in an editor like emacs or vim, you'll need to install Gradle on your machine and set up environment variables (Windows) for both Java and Gradle.

* When installing [Gradle](https://gradle.org/install/) manually. Make sure the Gradle version is compatible with the JDK