#!/bin/bash

# find files
src_files=$(find ./src -name "*.java") 

# tell user
echo -e "\e[33mLoading files:\e[32m"
echo -e "$src_files" | awk '{ print "> " $0 }'
echo -en "\e[37m"

# compile
javac -cp ./lib/*.jar $src_files -d ./build/

class_files=$(find ./build -name "*.class") 

# compress
jar cvf ./dist/RainCraft.jar $class_files
