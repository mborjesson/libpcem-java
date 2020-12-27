# pcem-java

Inspired by #doscember I also wanted to do something DOS-related this month. The result is this project, where the goal was to see if it would be possible to build [PCem](https://pcem-emulator.co.uk/) as a library and use it in Java.

It should have a simple API and the features that I needed to get DOS up and running with graphics, sound and mouse-/keyboard-input.

## Please note

This project has been uploaded as-is and is only to demonstrate how libpcem works and what it can do. The code is a bit messy since I used it to test various functions of libpcem.

## How to build

Run gradlew to build a distribution:
```
sh gradlew clean build distTar
```

The build can be found in build/distributions/. Unpack it somewhere and then copy libpcem.so into the bin-directory.

## How to run

Enter the bin-directory and run pcem-java:
```
./pcem-java -c ${HOME}/.pcem/ machineconfig
```

This assumes you have a .pcem-folder with ROMs etc, just as you would with a normal PCem installation.
machineconfig is the name of your machine-configuration file (without .cfg).
