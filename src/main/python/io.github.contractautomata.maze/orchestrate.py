#!/bin/env python3

# %%

# Setup
import voxlogica
import os
import matplotlib.pyplot as plt
import matplotlib.image as mpimg
import json
import os.path
from joblib import Parallel, delayed
import multiprocessing
import subprocess 

try:
    basedir = subprocess.check_output(['git','rev-parse','--show-toplevel'],encoding='utf-8').strip()
except:
    basedir="."
#%%

# Default values
baseimage = f'{basedir}/src/main/java/io/github/contractautomata/maze/twoagentsproblem/resources/maze3.png'
defaultdir = f'{basedir}/src/test/java/io/github/contractautomata/maze/resources/twoagentsimages/png'
default_specification="./specification.imgql"
images = []
tmpdir = "./tmp"
cache = "./cache.json"
batch = 150
batches = 0

# Result computation and auxiliary function "view"
def compute(specification, specname, imagepath, datadir, parimages):
    global images
    num_cores = 1
    if parimages == None:
        parimages = images
    files = []
    specs = []
    print(batches)
    print(len(parimages))
    for k in range(0, batches):
        print(batch*k)
        file, spec = specification(k, specname, imagepath, datadir, parimages[batch*k: batch*(k+1)])
        files.append(file)
        specs.append(spec)
        #print(spec)
    
    #print(batches)

    def processInput(i):
        global batch
        #global images
        start = i*batch
        #image_start = start
        results = {"filename": files[i], "output": voxlogica.run_voxlogica(specs[i])}
        print(results)
        #print(x['filename'])
        #print('done')
        return results

    voxlogica_output = Parallel(n_jobs=num_cores)(delayed(processInput)(i) for i in range(0,batches))
    #print(voxlogica_output)

    return voxlogica.simplify_results(voxlogica_output)

def view(result,rows = 5,cols = 5,sizex = 20,sizey = 20, datadir=defaultdir):
    fname = result["filename"]
    plt.figure(figsize=(sizex,sizey))
    img = mpimg.imread(f'{datadir}/{fname}')
    ax=plt.subplot(rows,cols,1)
    ax.set_title("model")
    plt.imshow(img)
    i=2
    toprint = {}
    for (key,value) in result["results"].items():
        if isinstance(value,(int,float)):
            toprint[key] = value
            #toprint.append(f'{key}: {value}')            
        else:
            fmla = f'{key.removeprefix(fname+"_")}'
            img = mpimg.imread(f'{tmpdir}/{value["filename"]}')            
            ax = plt.subplot(rows,cols,i)    # the number of images in the grid is 5*5 (25)    
            ax.set_title(fmla)
            plt.imshow(img,cmap="Greys_r")
            i = i+1            
    plt.show()     
    print(toprint)  

# %%  
# Specification and analysis
def vlsave(image,var):
    return f'save "{tmpdir}/{image}_{var}.png" {var}'

def vlprint(var):
    return f'print "{var}" {var}'

def specification(index, scriptname, imagepath, datadir, parimages):
    global batch
    input_script = open(scriptname, "r")
    script_lines = input_script.read()
    new_text = ""
    filenames = [name for name in parimages]
    for image_name in filenames:
        #print("inside spec")
        fname = f'''let filename = "{image_name}"'''
        base_name = f'''load base ="{imagepath}"\n'''
        new_text += fname
        new_text += base_name
        string_set = [#"initial2_"+image_name,
                      "initial3_"+image_name,
                      #"forbidden1_"+image_name,
                      "forbidden3_"+image_name,
                      "final3_"+image_name,
                      "canExit(mrGreen)_"+image_name,
                      "wrong_"+image_name,
                      "sameRoom_"+image_name,
                      "greenFlees_"+image_name,
                      "nearby_"+image_name]
        new_text += f'''load img = "{datadir}/{image_name}"\n''' + script_lines
            #print "{string_set[0]}" initial2
            #print "{string_set[2]}" forbidden1
            #print "{string_set[3]}" canExit(mrGreen)
            #print "{string_set[4]}" wrong
            #print "{string_set[5]}" sameRoom
            #print "{string_set[6]}" greenFlees
            #print "{string_set[7]}" nearby
        new_text += f'''
        
            print "{string_set[0]}" initial3
            print "{string_set[1]}" forbidden3
            print "{string_set[2]}" final3

            '''
    
    return filenames, new_text


# {vlsave(image,"door")}
# {vlsave(image,"exit")}
# {vlsave(image,"mrRed")}
# {vlsave(image,"mrGreen")}
# {vlsave(image,"pathToExit")}

def orchestrate(specname, imagepath=baseimage, datadir=defaultdir):
    global images
    global default_specification
    global batches 
    if specname == None:
        specname = default_specification

    images = [fname for fname in os.listdir(datadir) if fname.endswith(".png")]
    batches = int(len(images) / batch) + 1
    print("There are " + str(len(images)) + "images in Orchestrate")
    items = []
    if (os.path.exists(cache)):    
        with open(cache) as f:
            items = json.load(f)
    else:
        items = compute(specification, specname, imagepath, datadir)
        with open(cache, 'w') as f:
            json.dump(items, f)    

    # %%

    wrong = [ x["filename"] for x in items if len(x["results"].keys()) != 15 ]
    # %%
    rescued = compute(specification, specname, imagepath, datadir, images)

    # %%

    correct = [x for x in items if len(x["results"].keys()) == 15 ]
    # %%

    final = correct + rescued

    with open(cache, 'w') as f:
        json.dump(final, f)    

# %%

