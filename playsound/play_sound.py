from playsound import playsound
import time
import random
import os 

path = 'sound/'

def readFiles():
    files = os.listdir(path)
    return files

def play_sound():
    files = readFiles()
    if len(files) > 0:
        file  = random.choice(files)
        print("Playing..." + file)
        playsound(path+file)
    else:
        print("No file sound")

while True:
    time.sleep(10)
    local_time = time.localtime()
    t_hr = local_time.tm_hour
    t_min = local_time.tm_min
    
    if t_hr > 22 and t_hr < 9:
        continue 
    if t_min != 0 or t_min != 30:
        continue 
    if t_hr == 22 and t_min > 0: 
        continue 
    play_sound()