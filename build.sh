#!/bin/bash

# navigate to directory
old_dir=$(pwd)
cd $1

# save
git add .
git commit -m "[auto] build save"
git push

# setup directories to look for
rain_home=$(pwd)
rain_build="$rain_home/build"
rain_dist="$rain_home/dist"
rain_src="$rain_home/src"
rain_lib="$rain_home/lib"

# compile
#

cd $rain_src

# find files (.java)
src_files=$(find . -name "*.java") 
echo -en "\e[33m"
echo -e "$src_files" | awk '{ print "compiling... " $0 }'
echo -en "\e[32m"

# add config to jar (plugin.yml)
jar -cvf "$rain_dist/RainCraft.jar" plugin.yml

# create .class files
javac -cp $rain_lib/*.jar $src_files -d $rain_build


# compress
#

cd $rain_build

# create .jar
class_files=$(find . -name "*.class")
echo -en "\e[33m"
echo -e "$src_files" | awk '{ print "compressing... " $0 }'
echo -en "\e[32m"

jar -uvf "$rain_dist/RainCraft.jar" $class_files
echo -en "\e[37m"

cd $old_dir
