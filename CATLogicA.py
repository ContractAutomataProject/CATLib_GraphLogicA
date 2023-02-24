#!/usr/bin/env python3

import os
import shutil
import subprocess
import sys
python_dir_path = "./src/main/python/io.github.contractautomata.maze"
sys.path.append(python_dir_path)
import orchestrate

# Run the Java jar file
jar_file_path = "./maze-0.0.1-SNAPSHOT-jar-with-dependencies.jar"
image_path = "./"
subprocess.run(["java", "-jar", jar_file_path, "-phase1", "-imagepath", image_path])

# Copy files to the specified directory
dir_path = "src/test/java/io/github/contractautomata/maze/resources/twoagentsimages/png"
png_dir_path = "./png"
if os.path.exists(dir_path):
    shutil.rmtree(dir_path)
shutil.copytree(png_dir_path, dir_path)

orchestrate.orchestrate()