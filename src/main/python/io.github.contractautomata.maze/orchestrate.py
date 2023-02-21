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

baseimage = f'{basedir}/src/main/java/io/github/contractautomata/maze/twoagentsproblem/resources/maze3.png'
datadir = f'{basedir}/src/test/java/io/github/contractautomata/maze/resources/twoagentsimages/png'
images = [fname for fname in os.listdir(datadir) if fname.endswith(".png")]
tmpdir = "./tmp"
cache = "./cache.json"
#image_start = 160
batch = 150
batches = int(len(images) / batch) + 1

# Result computation and auxiliary function "view"
def compute(specification,start=0,end=len(images)-1,images=images):
    num_cores = 1 #multiprocessing.cpu_count()
    files = []
    specs = []
    for k in range(0, batches):
        file, spec = specification(k)
        files.append(file)
        specs.append(spec)
        #print(spec)
    print("there are " + str(len(images)) + " images")  
    
    #print(batches)

    def processInput(i):
        global batch
        #global images
        start = i*batch
        #image_start = start
        results = {"filename": files[0], "output": voxlogica.run_voxlogica(specs[i])}
        print(results)
        #print(x['filename'])
        #print('done')
        return results

    voxlogica_output = Parallel(n_jobs=num_cores)(delayed(processInput)(i) for i in range(0,batches))
    #print(voxlogica_output)

    return voxlogica.simplify_results(voxlogica_output)

def view(result,rows = 5,cols = 5,sizex = 20,sizey = 20):
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

def specification(index):
    global batch
    text = f'''

let r = red(img)
let g = green(img)
let b = blue(img)
let rb = red(base)
let gb = green(base)
let bb = blue(base)

let exists(p) = volume(p) .>. 0
let forall(p) = volume(p) .=. volume(tt)
let forallin(x,p) = forall( (!x) | p)

let door = (r =. 0) & (b =. 255) & (g =. 0)
let floorNoDoor = (rb =. 255) & (bb =. 255) & (gb =. 255)
let floor = floorNoDoor & (!door)
let wall = !floor


let mrRed = (r =. 255) & (b =. 0) & (g =. 0)
let mrGreen = (r =. 0) & (b =. 0) & (g =. 255)

let mrX = mrRed | mrGreen

let initial1 = exists(mrRed & ((x =. 1) & (y =. 1))) .&. exists(mrGreen & ((x =. 1) & (y =. 2)))
let initial2 = exists(mrRed & ((x =. 6) & (y =. 3))) .&. exists(mrGreen & ((x =. 1) & (y =. 4)))
let wrong = exists(mrX & wall) .|. (!. (exists(mrRed) .&. exists(mrGreen)))
let exit = (x =. 9) & (y >. 2) & (y <. 9)
let pathToExit = (floor ~> exit)
let canExit(mr) = forallin(mr,pathToExit)
let sameRoom = forallin(mrGreen,(mrGreen|floor) ~> mrRed)

let greenFlees = (!.wrong) .&. canExit(mrGreen) .&. (!.(canExit(mrRed)))
let nearby = exists(mrRed & (N N mrGreen))

let forbidden1 = greenFlees .|. wrong
let forbidden2 = forbidden1 .|. (!. nearby)

let final = exists(mrRed & exit) .&. exists(mrGreen & exit)

'''
    new_text = ""
    filenames = [name for name in images[batch*index: batch*(index+1)]]
    for image_name in filenames:
        #print("inside spec")
        fname = f'''let filename = "{image_name}"'''
        base_name = f'''load base ="{baseimage}"\n'''
        new_text += fname
        new_text += base_name
        string_set = ["initial1_"+image_name,
                      "initial2_"+image_name,
                      "forbidden1_"+image_name,
                      "forbidden2_"+image_name,
                      "final_"+image_name,
                      "canExit(mrGreen)_"+image_name,
                      "wrong_"+image_name,
                      "sameRoom_"+image_name,
                      "greenFlees_"+image_name,
                      "nearby_"+image_name]
        new_text += f'''load img = "{datadir}/{image_name}"\n''' + text
        new_text += f'''
        
            print "{string_set[0]}" initial1
            print "{string_set[1]}" initial2
            print "{string_set[2]}" forbidden1
            print "{string_set[3]}" forbidden2
            print "{string_set[4]}" final
            print "{string_set[5]}" canExit(mrGreen)
            print "{string_set[6]}" wrong
            print "{string_set[7]}" sameRoom
            print "{string_set[8]}" greenFlees
            print "{string_set[9]}" nearby\n

            '''
    
    return filenames, new_text


# {vlsave(image,"door")}
# {vlsave(image,"exit")}
# {vlsave(image,"mrRed")}
# {vlsave(image,"mrGreen")}
# {vlsave(image,"pathToExit")}



items = []
if (os.path.exists(cache)):    
    with open(cache) as f:
        items = json.load(f)
else:
    items = compute(specification)
    with open(cache, 'w') as f:
        json.dump(items, f)    

# %%

wrong = [ x["filename"] for x in items if len(x["results"].keys()) != 15 ]
# %%
rescued = compute(specification,images=wrong)

# %%

correct = [x for x in items if len(x["results"].keys()) == 15 ]
# %%

final = correct + rescued

with open("cache.json", 'w') as f:
    json.dump(final, f)    

# %%

