from playsound import playsound
import time
import random

def play_sound():
    name = ['dog','truck','motorcycle','chainsaw']
    ran = random.choice(name)
    print("Playing..."+ran)
    playsound(ran+'.wav')

while True:
    result = time.localtime()
    t_hr = result.tm_hour
    t_min = result.tm_min
    print(str(t_hr)+":"+str(t_min))

    if t_hr >= 9 and t_hr < 22:
        if t_min == 00 or t_min == 30:
            print(t_hr)
            play_sound()
        time.sleep(1)
    elif  t_hr == 22:
        if t_min == 00:
            print(t_hr)
            play_sound()
        