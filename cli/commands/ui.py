from tkinter import *
from commands.common.i2c_command import *
from commands.common.swarm_command import *
from .adb import Device

window = Tk()
window.title('Guardian Diagnostics')
window.columnconfigure([0, 1, 2, 3, 4, 5], minsize=50, weight=1)

def start():
    device = Device()
   
    i2c = i2cStatus(device) # Get i2c available (expect 0x0001)
    input_v = input_voltage(device)  # Get Input Voltage 
    input_v = "{:.2f}".format(input_v)
    input_c = input_current(device) # Get Input Current 
    input_c = float("{:.2f}".format(input_c))
    sys_v = system_voltage(device)  # Get sys Voltage 
    sys_v = float("{:.2f}".format(sys_v))
    bat_v = battery_voltage(device) # Get Battery Voltage
    bat_v = float("{:.2f}".format(bat_v))
    bat_c = battery_curent(device) # Get Battery Current
    bat_c = float("{:.2f}".format(bat_c))
    bat_p = battery_percentage(device) # Get Battery percent
    bat_p = float("{:.2f}".format(bat_p))

    sw_status = swarm_status(device) # Get Swarm status 
    sw_id = swarm_id(device) # Get Swarm id
    sw_gps = swarm_gps(device) # Get Swarm GPS
    sw_fw = swarm_firmware(device) # Get Swarm firm ware
    sw_dt = swarm_datetime(device) # Get Swarm datetime
 
    # Call function to show value on ui
    show_i2c_status(window, i2c)
    sentnel_info(window, input_v, input_c, sys_v, bat_v, bat_c, bat_p, None)
    swarm_info(window, sw_status, sw_id, sw_gps, sw_fw, sw_dt)

def show_i2c_status(window, val): # Show i2c_status

    color = 'blue'
    if val == True:
        val = "I2C is OK"
        color = 'green'
    elif val == False:
        color = 'red'
        val = "Sentinel Power Chip is NOT Accessible via I2C"
    elif val == None:
        color = 'blue'
        val = None

    i2c_val = Label(window, text=str(val), font=("Courier", 14), fg=color, relief=SUNKEN,width=35)
    i2c_val.grid(row=1, column=1, sticky="w")

def sentnel_info(window, in_v,in_c,sys_v,bat_v,bat_c,bat_p,reset): # show value of input power / system power / battery power 
    color = "green"
    if reset == "reset":
        color = "blue"
        
    input_v_val = Label(window, text=str(in_v), font=("Courier", 14), fg=color, relief=SUNKEN, width=35)
    input_v_val.grid(row=3, column=1, sticky="w")
   
    input_c_val = Label(window, text=str(in_c), font=("Courier", 14), fg=color, relief=SUNKEN, width=35)
    input_c_val.grid(row=3, column=4, sticky="w")
 
    system_v_val = Label(window, text=str(sys_v), font=("Courier", 14), fg=color, relief=SUNKEN, width=35)
    system_v_val.grid(row=5, column=1,sticky="w")

    battery_v_val = Label(window, text=str(bat_v), font=("Courier", 14), fg=color, relief=SUNKEN, width=35)
    battery_v_val.grid(row=7, column=1, sticky="w")

    battery_c_val = Label(window, text=str(bat_c), font=("Courier", 14), fg=color,relief=SUNKEN,width=35)
    battery_c_val.grid(row=7, column=4,sticky="w")

    battery_percen_val = Label(window, text=str(bat_p), font=("Courier", 14), fg=color, relief=SUNKEN, width=35)
    battery_percen_val.grid(row=9, column=1,sticky="w")

def swarm_info(window, sw_status,sw_id,sw_gps,sw_fw,sw_dt): # show value of swarm 
     # swarm status
    color = 'blue'

    if sw_status == True:
        sw_status = "Swarm is ON"
        color = 'green'
    elif sw_status == False:
        color = 'red'
        sw_status = "Swarm is OFF"
    sw_status_val = Label(window, text=str(sw_status), font=("Courier", 14), fg=color, relief=SUNKEN, width=35)
    sw_status_val.grid(row=11, column=1, sticky="w")

    # swarm id
    if sw_id != None:
        color = 'green'
    elif sw_id == None:
        color = 'blue'
    swarm_id_val = Label(window, text=str(sw_id), font=("Courier", 14), fg=color, relief=SUNKEN, width=35)
    swarm_id_val.grid(row=12, column=1, sticky="w")

    # swarm GPS
    if sw_gps != None:
        color = 'green'
    elif sw_gps == None:
        color = 'blue'
    swarm_gps_val = Label(window, text=str(sw_gps), font=("Courier", 14), fg=color, relief=SUNKEN, width=35)
    swarm_gps_val.grid(row=13, column=1, sticky="w")

    # swarm firmware
    if sw_fw != None:
        color = 'green'
    elif sw_fw == None:
        color = 'blue'
    swarm_fw_val = Label(window, text=str(sw_fw), font=("Courier", 14), fg=color, relief=SUNKEN, width=35)
    swarm_fw_val.grid(row=14, column=1, sticky="w")

    # swarm datetime
    if sw_dt != None:
        color = 'green' 
    elif sw_dt == None:
        color = 'blue'  
    swarm_dt_val = Label(window, text=str(sw_dt), font=("Courier", 14), fg=color, relief=SUNKEN, width=35)
    swarm_dt_val.grid(row=15, column=1, sticky="w")

def reset():
    show_i2c_status(window, None)
    sentnel_info(window, None, None, None, None, None, None, "reset")
    swarm_info(window, None, None, None, None, None)

def load():
    #btn start
    btn_start = Button(window, text="START", fg='white', bg="green", width=25, height=2, command=start)
    btn_start.pack(side=LEFT)
    btn_start.grid(row=0, column=0, sticky="nsew", columnspan = 2)

    #btn reset
    btn_reset = Button(window, text="RESET", fg='white', bg="red", width=25, height=2, command=reset)
    btn_reset.grid(row=0, column=2, sticky="nsew", columnspan = 5)

    # i2c status
    i2c = Label(window, text="i2c Status:", font=("Courier", 14))
    i2c.grid(row=1, column=0,sticky="w")
    i2c_val = Label(window, text=str(None), font=("Courier", 14), fg="blue", relief=SUNKEN, width=35)
    i2c_val.grid(row=1, column=1, sticky="w")

    # input info
    input_info = Label(window, text="input info:", font=("Courier", 14))
    input_info.grid(row=2, column=0, sticky="w")
    input_v = Label(window, text="voltage", font=("Courier", 14))
    input_v.grid(row=2, column=1, sticky="w")
    input_v_val = Label(window, text=str(None), font=("Courier", 14), fg="blue", relief=SUNKEN, width=35)
    input_v_val.grid(row=3, column=1, sticky="w")
    input_v_unit = Label(window, text=str("mV"), font=("Courier", 14))
    input_v_unit.grid(row=3, column=2, sticky="w")

    input_c = Label(window, text="current", font=("Courier", 14))
    input_c.grid(row=2, column=4, sticky="w")
    input_c_val = Label(window, text=str(None), font=("Courier", 14), fg="blue", relief=SUNKEN, width=35)
    input_c_val.grid(row=3, column=4, sticky="w")
    input_c_unit = Label(window, text=str("mA"), font=("Courier", 14))
    input_c_unit.grid(row=3, column=5, sticky="w")

    # system info
    system_info = Label(window, text="system info:", font=("Courier", 14))
    system_info.grid(row=4, column=0, sticky="w")

    system_v = Label(window, text="voltage", font=("Courier", 14))
    system_v.grid(row=4, column=1, sticky="w")
    system_v_val = Label(window, text=str(None), font=("Courier", 14), fg="blue", relief=SUNKEN, width=35)
    system_v_val.grid(row=5, column=1, sticky="w") 
    system_v_unit = Label(window, text=str("mV"), font=("Courier", 14))
    system_v_unit.grid(row=5, column=2, sticky="w")
  
    # battery info
    battery_info = Label(window, text="battery info:", font=("Courier", 14))
    battery_info.grid(row=6, column=0, sticky="w")

    battery_v = Label(window, text="voltage", font=("Courier", 14))
    battery_v.grid(row=6, column=1, sticky="w")
    battery_v_val = Label(window, text=str(None), font=("Courier", 14), fg="blue", relief=SUNKEN, width=35)
    battery_v_val.grid(row=7, column=1, sticky="w")
    battery_v_unit = Label(window, text=str("mV"), font=("Courier", 14))
    battery_v_unit.grid(row=7, column=2, sticky="w")

    battery_c = Label(window, text="current", font=("Courier", 14))
    battery_c.grid(row=6, column=4, sticky="w")
    battery_c_val = Label(window, text=str(None), font=("Courier", 14), fg="blue", relief=SUNKEN, width=35)
    battery_c_val.grid(row=7, column=4, sticky="w")
    battery_c_unit = Label(window, text=str("mA"), font=("Courier", 14))
    battery_c_unit.grid(row=7, column=5, sticky="w")

    battery_percen = Label(window, text="percentage", font=("Courier", 14))
    battery_percen.grid(row=8, column=1, sticky="w")
    battery_percen_val = Label(window, text=str(None), font=("Courier", 14), fg="blue", relief=SUNKEN, width=35)
    battery_percen_val.grid(row=9, column=1,sticky="w")
    battery_percen_unit = Label(window, text=str("%"), font=("Courier", 14))
    battery_percen_unit.grid(row=9, column=2, sticky="w")
    
    spacer1 = Label(window, text="")
    spacer1.grid(row=10, column=0, pady=10)
    #swarm status
    sw_status = Label(window, text="swarm status:", font=("Courier", 14))
    sw_status.grid(row=11, column=0, sticky="w")
    sw_status_val = Label(window, text=str(None), font=("Courier", 14), fg="blue", relief=SUNKEN, width=35)
    sw_status_val.grid(row=11, column=1, sticky="w")

    #Swarm id
    swarm_id = Label(window, text="swarm ID:", font=("Courier", 14))
    swarm_id.grid(row=12, column=0, sticky="w")
    swarm_id_val = Label(window, text=str(None), font=("Courier", 14), fg="blue", relief=SUNKEN, width=35)
    swarm_id_val.grid(row=12, column=1, sticky="w")

    # swarm GPS
    swarm_gps = Label(window, text="swarm GPS:", font=("Courier", 14))
    swarm_gps.grid(row=13, column=0,sticky="w")
    swarm_gps_val = Label(window, text=str(None), font=("Courier", 14), fg="blue", relief=SUNKEN, width=35)
    swarm_gps_val.grid(row=13, column=1, sticky="w")

    # swarm firmware
    swarm_fw = Label(window, text="swarm firmware:", font=("Courier", 14))
    swarm_fw.grid(row=14, column=0,sticky="w")
    swarm_fw_val = Label(window, text=str(None), font=("Courier", 14), fg="blue", relief=SUNKEN, width=35)
    swarm_fw_val.grid(row=14, column=1, sticky="w")

    # swarm datetime
    swarm_dt = Label(window, text="swarm datetime:", font=("Courier", 14))
    swarm_dt.grid(row=15, column=0,sticky="w")
    swarm_dt_val = Label(window, text=str(None), font=("Courier", 14), fg="blue", relief=SUNKEN, width=35)
    swarm_dt_val.grid(row=15, column=1, sticky="w")

# Create a button

    window.mainloop()
