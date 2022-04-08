from tkinter import *
from commands.common.i2c_command import *
from commands.common.swarm_command import *
from .adb import Device

window = Tk()
window.title('Guardian Diagnosis')
window.geometry("780x560")

def start():

    device = Device()
    # Get i2c available (expect 0x0001)
    i2c = i2cStatus(device)

    # Get Input Voltage -
    input_v = input_voltage(device)
    input_v = "{:.2f}".format(input_v)

    # Get Input Current 
    input_c = input_current(device)
    input_c = float("{:.2f}".format(input_c))

    # Get sys Voltage 
    sys_v = system_voltage(device)
    sys_v = float("{:.2f}".format(sys_v))

    # Get Battery Voltage 
    bat_v = battery_voltage(device)
    bat_v = float("{:.2f}".format(bat_v))

    # Get Battery Current 
    bat_c = battery_curent(device)
    bat_c = float("{:.2f}".format(bat_c))

    # Get Battery percent
    bat_p = battery_percentage(device)
    bat_p = float("{:.2f}".format(bat_p))

    # Get Swarm status 
    sw_status = swarm_status(device)

    # Get Swarm id
    sw_id = swarm_id(device)

    # Get Swarm GPS
    sw_gps = swarm_gps(device)

    # Get Swarm firm ware
    sw_fw = swarm_firmware(device)
   
    # Get Swarm datetime
    sw_dt = swarm_datetime(device)
 
    # Call function to show value on ui
    complete(window,1)
    show_i2c_status(window,i2c)
    sentnel_info(window,input_v,input_c,sys_v,bat_v,bat_c,bat_p)
    swarm_info(window,sw_status,sw_id,sw_gps,sw_fw,sw_dt)

def complete(window,status): # show DONE!!! when finish command
    if status == 1: 
        complete = Label(window, text="DONE!!!", font=("Courier", 16),fg="green")
        complete.place(x=400, y=50)
    else:
        complete = Label(window, text="         ", font=("Courier", 16),fg="green")
        complete.place(x=400, y=50)

def show_i2c_status(window, val): # Show i2c_status

    i2c = Label(window, text="i2c Status   :", font=("Courier", 14))
    i2c.place(x=20, y=100)

    color = 'blue'
    if val == True:
        val = "I2C is OK"
        color = 'green'
    elif val == False:
        color = 'red'
        val = "Sentinel Power Chip is NOT Accessible via I2C"
    elif val == None:
        color = 'blue'
        val = 'No connect'
    i2c_val = Label(window, text="                                        ", font=("Courier", 14), fg=color)
    i2c_val.place(x=180, y=100)
    i2c_val = Label(window, text=str(val), font=("Courier", 14), fg=color)
    i2c_val.place(x=180, y=100)

def sentnel_info(window, in_v,in_c,sys_v,bat_v,bat_c,bat_p): # show value of input power / system power / battery power 
    # input power info
    input_info = Label(window, text="input info   :", font=("Courier", 14))
    input_info.place(x=20, y=140)

    input_v = Label(window, text="voltage =", font=("Courier", 14))
    input_v.place(x=180, y=140)
    input_v_val = Label(window, text="          ", font=("Courier", 14), fg="blue")
    input_v_val.place(x=285, y=140)
    input_v_val = Label(window, text=str(in_v), font=("Courier", 14), fg="blue")
    input_v_val.place(x=285, y=140)
    input_v_unit = Label(window, text=str("mV"), font=("Courier", 14), )
    input_v_unit.place(x=380, y=140)

    input_c = Label(window, text="/ current =", font=("Courier", 14))
    input_c.place(x=450, y=140)
    input_c_val = Label(window, text="          ", font=("Courier", 14), fg="blue")
    input_c_val.place(x=580, y=140)
    input_c_val = Label(window, text=str(in_c), font=("Courier", 14), fg="blue")
    input_c_val.place(x=580, y=140)
    input_c_unit = Label(window, text=str("mA"), font=("Courier", 14), )
    input_c_unit.place(x=675, y=140)

    # system info
    system_info = Label(window, text="system info  :", font=("Courier", 14))
    system_info.place(x=20, y=180)

    system_v = Label(window, text="voltage =", font=("Courier", 14))
    system_v.place(x=180, y=180)
    system_v_val = Label(window, text="          ", font=("Courier", 14), fg="blue")
    system_v_val.place(x=285, y=180)
    system_v_val = Label(window, text=str(sys_v), font=("Courier", 14), fg="blue")
    system_v_val.place(x=285, y=180)
    system_v_unit = Label(window, text=str("mV"), font=("Courier", 14), )
    system_v_unit.place(x=380, y=180)
  
    # battery info
    battery_info = Label(window, text="battery info :", font=("Courier", 14))
    battery_info.place(x=20, y=220)

    battery_v = Label(window, text="voltage =", font=("Courier", 14))
    battery_v.place(x=180, y=220)
    battery_v_val = Label(window, text="          ", font=("Courier", 14), fg="blue")
    battery_v_val.place(x=285, y=220)
    battery_v_val = Label(window, text=str(bat_v), font=("Courier", 14), fg="blue")
    battery_v_val.place(x=285, y=220)
    battery_v_unit = Label(window, text=str("mV"), font=("Courier", 14), )
    battery_v_unit.place(x=380, y=220)

    battery_c = Label(window, text="/ current =", font=("Courier", 14))
    battery_c.place(x=450, y=220)
    battery_c_val = Label(window, text="          ", font=("Courier", 14), fg="blue")
    battery_c_val.place(x=580, y=220)
    battery_c_val = Label(window, text=str(bat_c), font=("Courier", 14), fg="blue")
    battery_c_val.place(x=580, y=220)
    battery_c_unit = Label(window, text=str("mA"), font=("Courier", 14), )
    battery_c_unit.place(x=675, y=220)

    battery_percen = Label(window, text="percentage =", font=("Courier", 14))
    battery_percen.place(x=180, y=260)
    battery_percen_val = Label(window, text="      ", font=("Courier", 14), fg="blue")
    battery_percen_val.place(x=320, y=260)
    battery_percen_val = Label(window, text=str(bat_p), font=("Courier", 14), fg="blue")
    battery_percen_val.place(x=320, y=260)
    battery_percen_unit = Label(window, text=str("%"), font=("Courier", 14), )
    battery_percen_unit.place(x=380, y=260)

def swarm_info(window, sw_status,sw_id,sw_gps,sw_fw,sw_dt): # show value of swarm 
    # swarm status
    swarm_info = Label(window, text="swarm status :", font=("Courier", 14))
    swarm_info.place(x=20, y=300)
    color = 'blue'
    if sw_status == True:
        sw_status = "Swarm is ON"
        color = 'green'
    elif sw_status == False:
        color = 'red'
        sw_status = "Swarm is OFF"

    battery_percen_val = Label(window, text="             ", font=("Courier", 14), fg=color)
    battery_percen_val.place(x=180, y=300)
    battery_percen_val = Label(window, text=str(sw_status), font=("Courier", 14), fg=color)
    battery_percen_val.place(x=180, y=300)

    # swarm id
    swarm_id = Label(window, text="swarm ID     :", font=("Courier", 14))
    swarm_id.place(x=20, y=340)
    if sw_id != None:
        color = 'green'
    elif sw_id == None:
        color = 'blue'
    swarm_id = Label(window, text="                                         ", font=("Courier", 14), fg=color)
    swarm_id.place(x=180, y=340)
    swarm_id = Label(window, text=str(sw_id), font=("Courier", 14), fg=color)
    swarm_id.place(x=180, y=340)

    # swarm GPS
    swarm_gps = Label(window, text="swarm GPS    :", font=("Courier", 14))
    swarm_gps.place(x=20, y=380)
    if sw_gps != None:
        color = 'green'
    elif sw_gps == None:
        color = 'blue'
    swarm_gps = Label(window, text="                                        ", font=("Courier", 14), fg=color)
    swarm_gps.place(x=180, y=380)
    swarm_gps = Label(window, text=str(sw_gps), font=("Courier", 14), fg=color)
    swarm_gps.place(x=180, y=380)

    # swarm firmware
    swarm_fw = Label(window, text="swarm firmware:", font=("Courier", 14))
    swarm_fw.place(x=20, y=420)
    if sw_fw != None:
        color = 'green'
    elif sw_fw == None:
        color = 'blue'
    swarm_fw = Label(window, text="                                        ", font=("Courier", 14), fg=color)
    swarm_fw.place(x=200, y=420)
    swarm_fw = Label(window, text=str(sw_fw), font=("Courier", 14), fg=color)
    swarm_fw.place(x=200, y=420)

    # swarm datetime
    swarm_dt = Label(window, text="swarm datetime:", font=("Courier", 14))
    swarm_dt.place(x=20, y=460)
    if sw_dt != None:
        color = 'green' 
    elif sw_dt == None:
        color = 'blue'  
    swarm_dt = Label(window, text="                                       ", font=("Courier", 14), fg=color)
    swarm_dt.place(x=200, y=460)
    swarm_dt = Label(window, text=str(sw_dt), font=("Courier", 14), fg=color)
    swarm_dt.place(x=200, y=460)
def reset():

    show_i2c_status(window, None)
    sentnel_info(window, None,None,None,None,None,None)
    swarm_info(window, None,None,None,None,None)
    complete(window,0)

def load():
    # do btn start
    btn_start = Button(window, text="START", fg='white',
                    bg="green", width=25, height=2, command=start)
    btn_start.place(x=20, y=30)

    btn_start = Button(window, text="RESET", fg='white',
                    bg="red", width=25, height=2, command=reset)
    btn_start.place(x=210, y=30)

    #call function initially
    show_i2c_status(window, None)
    sentnel_info(window, None,None,None,None,None,None)
    swarm_info(window, None,None,None,None,None)

    window.mainloop()
