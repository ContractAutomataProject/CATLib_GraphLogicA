ffmpeg -r 8 -start_number 1 -i %01d.png -c:v libx264 -vf "pad=ceil(iw/2)*2:ceil(ih/2)*2,fps=5,format=yuv420p,scale=320:320:flags=neighbor" out.mp4 
