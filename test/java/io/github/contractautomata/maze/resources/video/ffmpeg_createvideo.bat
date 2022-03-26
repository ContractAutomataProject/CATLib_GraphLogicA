ffmpeg -r 8 -start_number 1 -i %01d.png -c:v libx264 -vf "pad=ceil(iw/2)*2:ceil(ih/2)*2,fps=5,format=yuv420p,scale=320:320:flags=neighbor" out.mp4 
ffmpeg -i out.mp4 -i "Sleepless Night.mp3" -map 0 -map 1:a -c:v copy -shortest outaudio.mp4 
