from playsound import playsound
import time
import random
import os 

path = 'sound/'

def readfile():
    file = os.listdir(path)
    return file

def play_sound():
    name = readfile()
    if len(name) > 0:
        ran = random.choice(name)
        print("Playing..."+ran)
        playsound(path+ran)
    else:
        print("No file sound")

while True:
    result = time.localtime()
    t_hr = result.tm_hour
    t_min = result.tm_min
    print(str(t_hr)+":"+str(t_min))

    if t_hr >= 9 and t_hr < 22:
        if t_min == 0 or t_min == 30:
            print(t_hr)
            play_sound()
    elif  t_hr == 22:
        if t_min == 0:
            print(t_hr)
            play_sound()         
    time.sleep(10)