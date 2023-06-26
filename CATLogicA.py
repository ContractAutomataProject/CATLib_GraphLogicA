#!/usr/bin/env python3

import os
import shutil
import subprocess
import sys
python_dir_path = "./src/main/python/io.github.contractautomata.maze"
sys.path.append(python_dir_path)
import orchestrate
import argparse

#-phase1 (compute the composition and generate the images)\n
#-phase2 (read the logs of voxlogica and perform the synthesis)
#-position_agent_1 x y\n
#-position_agent_2 x y\n
#-gateCoordinates x y\n
#-inputCompositionPath String (the path where the composition has been stored)\n
#-forbiddenAttribute String (name of the forbidden attribute in the voxlogica log)\n
#-finalAttribute String (name of the final attribute in the voxlogica log)\n
#-controllability [1|2] (1: agents controllable, gate uncontrollable - 2: gate controllable, agents uncontrollable)
#-jsonPath String (the path in where the VoxLogicA output is stored)
#-outputCompositionPath String (the path where to store the composition)\n
#-voxLogica_output_path String (the path where the output of VoxLogica is located)\n

def usage():
    message = f'''
        Please specify the following arguments:\n
        -experiment [1|2|3] (select either experiment 1, 2 or 3)\n
        -imagePath String (the path where the image is located)\n     
        -specification String (the file containing the ImgQL specification)
    '''

def pair(arg):
    return [int(x) for x in arg.split(',')]

parser = argparse.ArgumentParser()
group = parser.add_mutually_exclusive_group()
#group.add_argument("-phase1", required=False, action='store_true')
#group.add_argument("-phase2", required=False, action='store_true')
parser.add_argument("-experiment", type=int, required=True)
#parser.add_argument("-gateCoordinates", type=pair, required=False)
#parser.add_argument("-position_agent_1", type=pair, required=False)
#parser.add_argument("-position_agent_2", type=pair, required=False)
parser.add_argument("-imagePath", type=str, required=False)
#parser.add_argument("-outputCompositionPath", type=str, required=False)
#parser.add_argument("-inputCompositionPath", type=str, required=False)
#parser.add_argument("-voxLogica_output_path", type=str, required=False)
#parser.add_argument("-forbiddenAttribute", type=str, required=False)
#parser.add_argument("-finalAttribute", type=str, required=False)
#parser.add_argument("-controllability", type=int, required=False)
parser.add_argument("-specification", type=str, required=False)
#parser.add_argument("-jsonPath", type=str, required=False)

group = parser.parse_args()
args = parser.parse_args()

if args == None:
    print(usage)

# Run the Java jar file
jar_file_path = "./maze-0.0.2-SNAPSHOT-jar-with-dependencies.jar"

if args.imagePath:
    image_path = args.imagePath
elif args.experiment == 1 or args.experiment == 2:
    image_path = "./maze3.png"
else:
    image_path = "./trainExample.png"

png_dir_path = "./png"

subprocess.run(["java", "-jar", jar_file_path, "-phase1", "-experiment", str(args.experiment), "-outputCompositionPath", ".", "-imagepath", image_path])

# Copy files to the specified directory
if args.outputCompositionPath:
    dir_path = args.outputCompositionPath
else:
    dir_path = "src/test/java/io/github/contractautomata/maze/resources/twoagentsimages/png"

if os.path.exists(dir_path):
    shutil.rmtree(dir_path)

shutil.copytree(png_dir_path, dir_path)

orchestrate.orchestrate(args.specification, args.experiment, image_path, dir_path)

subprocess.run(["java", "-jar", jar_file_path, "-phase2", "-experiment", str(args.experiment), "-inputCompositionPath", ".", "-voxLogica_output_path", "./cache.json", "-imagepath", image_path])