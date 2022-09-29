import tkinter as tk
from commands.common.i2c_command import *
from commands.common.swarm_command import *
from .adb import Device

class Diagnostics(tk.Frame):
    def start(self):
    
        i2c = i2cStatus(self.device) # Get i2c available (expect 0x0001)
        input_v = input_voltage(self.device)  # Get Input Voltage 
        input_v = "{:.2f}".format(input_v)
        input_c = input_current(self.device) # Get Input Current 
        input_c = float("{:.2f}".format(input_c))
        sys_v = system_voltage(self.device)  # Get sys Voltage 
        sys_v = float("{:.2f}".format(sys_v))
        bat_v = battery_voltage(self.device) # Get Battery Voltage
        bat_v = float("{:.2f}".format(bat_v))
        bat_c = battery_curent(self.device) # Get Battery Current
        bat_c = float("{:.2f}".format(bat_c))
        bat_p = battery_percentage(self.device) # Get Battery percent
        bat_p = float("{:.2f}".format(bat_p))

        sw_status = swarm_status(self.device) # Get Swarm status 
        sw_id = swarm_id(self.device) # Get Swarm id
        sw_gps = swarm_gps(self.device) # Get Swarm GPS
        sw_fw = swarm_firmware(self.device) # Get Swarm firm ware
        sw_dt = swarm_datetime(self.device) # Get Swarm datetime
    
        # Call function to show value on ui
        self.show_i2c_status(i2c)
        self.sentnel_info(input_v, input_c, sys_v, bat_v, bat_c, bat_p, None)
        self.swarm_info(sw_status, sw_id, sw_gps, sw_fw, sw_dt)

    def show_i2c_status(self, val): # Show i2c_status
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

        self.i2c_val = tk.Label(self, text=str(val), font=("Courier", 14), fg=color, relief=tk.SUNKEN,width=35)
        self.i2c_val.grid(row=1, column=1, sticky="w")

    def sentnel_info(self, in_v,in_c,sys_v,bat_v,bat_c,bat_p,reset): # show value of input power / system power / battery power 
        color = "green"
        if reset == "reset":
            color = "blue"
            
        self.input_v_val = tk.Label(self, text=str(in_v), font=("Courier", 14), fg=color, relief=tk.SUNKEN, width=35)
        self.input_v_val.grid(row=3, column=1, sticky="w")
    
        self.input_c_val = tk.Label(self, text=str(in_c), font=("Courier", 14), fg=color, relief=tk.SUNKEN, width=35)
        self.input_c_val.grid(row=3, column=4, sticky="w")
    
        self.system_v_val = tk.Label(self, text=str(sys_v), font=("Courier", 14), fg=color, relief=tk.SUNKEN, width=35)
        self.system_v_val.grid(row=5, column=1,sticky="w")

        self.battery_v_val = tk.Label(self, text=str(bat_v), font=("Courier", 14), fg=color, relief=tk.SUNKEN, width=35)
        self.battery_v_val.grid(row=7, column=1, sticky="w")

        self.battery_c_val = tk.Label(self, text=str(bat_c), font=("Courier", 14), fg=color,relief=tk.SUNKEN,width=35)
        self.battery_c_val.grid(row=7, column=4,sticky="w")

        self.battery_percen_val = tk.Label(self, text=str(bat_p), font=("Courier", 14), fg=color, relief=tk.SUNKEN, width=35)
        self.battery_percen_val.grid(row=9, column=1,sticky="w")

    def swarm_info(self, sw_status,sw_id,sw_gps,sw_fw,sw_dt): # show value of swarm 
        # swarm status
        color = 'blue'

        if sw_status == True:
            sw_status = "Swarm is ON"
            color = 'green'
        elif sw_status == False:
            color = 'red'
            sw_status = "Swarm is OFF"
        self.sw_status_val.config(text = str(sw_status))

        # swarm id
        if sw_id != None:
            color = 'green'
        elif sw_id == None:
            color = 'blue'
        self.swarm_id_val.config(text = str(sw_id))

        # swarm GPS
        if sw_gps != None:
            color = 'green'
        elif sw_gps == None:
            color = 'blue'
        self.swarm_gps_val.config(text = str(sw_gps))

        # swarm firmware
        if sw_fw != None:
            color = 'green'
        elif sw_fw == None:
            color = 'blue'
        self.swarm_fw_val.config(text = str(sw_fw))

        # swarm datetime
        if sw_dt != None:
            color = 'green' 
        elif sw_dt == None:
            color = 'blue'  
        self.swarm_dt_val.config(text = str(sw_dt))

    def reset(self):
        show_i2c_status(None)
        sentnel_info(None, None, None, None, None, None, "reset")
        swarm_info(None, None, None, None, None)

    def __init__(self, parent, controller):
        tk.Frame.__init__(self, parent)
        self.device = controller.getDevice()

        self.columnconfigure([0, 1, 2, 3, 4, 5], minsize=50, weight=1)
        #btn start
        self.btn_start = tk.Button(self, text="START", fg='white', bg="green", width=25, height=2, command = lambda: self.start())
        self.btn_start.pack(side = tk.LEFT)
        self.btn_start.grid(row=0, column=0, sticky="nsew", columnspan = 2)

        #btn reset
        self.btn_reset = tk.Button(self, text="RESET", fg='white', bg="red", width=25, height=2, command = lambda: self.reset())
        self.btn_reset.grid(row=0, column=2, sticky="nsew", columnspan = 5)

        # i2c status
        self.i2c = tk.Label(self, text="i2c Status:", font=("Courier", 14))
        self.i2c.grid(row=1, column=0,sticky="w")
        self.i2c_val = tk.Label(self, text=str(None), font=("Courier", 14), fg="blue", relief=tk.SUNKEN, width=35)
        self.i2c_val.grid(row=1, column=1, sticky="w")

        # input info
        self.input_info = tk.Label(self, text="input info:", font=("Courier", 14))
        self.input_info.grid(row=2, column=0, sticky="w")
        self.input_v = tk.Label(self, text="voltage", font=("Courier", 14))
        self.input_v.grid(row=2, column=1, sticky="w")
        self.input_v_val = tk.Label(self, text=str(None), font=("Courier", 14), fg="blue", relief=tk.SUNKEN, width=35)
        self.input_v_val.grid(row=3, column=1, sticky="w")
        self.input_v_unit = tk.Label(self, text=str("mV"), font=("Courier", 14))
        self.input_v_unit.grid(row=3, column=2, sticky="w")

        self.input_c = tk.Label(self, text="current", font=("Courier", 14))
        self.input_c.grid(row=2, column=4, sticky="w")
        self.input_c_val = tk.Label(self, text=str(None), font=("Courier", 14), fg="blue", relief=tk.SUNKEN, width=35)
        self.input_c_val.grid(row=3, column=4, sticky="w")
        self.input_c_unit = tk.Label(self, text=str("mA"), font=("Courier", 14))
        self.input_c_unit.grid(row=3, column=5, sticky="w")

        # system info
        self.system_info = tk.Label(self, text="system info:", font=("Courier", 14))
        self.system_info.grid(row=4, column=0, sticky="w")

        self.system_v = tk.Label(self, text="voltage", font=("Courier", 14))
        self.system_v.grid(row=4, column=1, sticky="w")
        self.system_v_val = tk.Label(self, text=str(None), font=("Courier", 14), fg="blue", relief=tk.SUNKEN, width=35)
        self.system_v_val.grid(row=5, column=1, sticky="w") 
        self.system_v_unit = tk.Label(self, text=str("mV"), font=("Courier", 14))
        self.system_v_unit.grid(row=5, column=2, sticky="w")
    
        # battery info
        self.battery_info = tk.Label(self, text="battery info:", font=("Courier", 14))
        self.battery_info.grid(row=6, column=0, sticky="w")

        self.battery_v = tk.Label(self, text="voltage", font=("Courier", 14))
        self.battery_v.grid(row=6, column=1, sticky="w")
        self.battery_v_val = tk.Label(self, text=str(None), font=("Courier", 14), fg="blue", relief=tk.SUNKEN, width=35)
        self.battery_v_val.grid(row=7, column=1, sticky="w")
        self.battery_v_unit = tk.Label(self, text=str("mV"), font=("Courier", 14))
        self.battery_v_unit.grid(row=7, column=2, sticky="w")

        self.battery_c = tk.Label(self, text="current", font=("Courier", 14))
        self.battery_c.grid(row=6, column=4, sticky="w")
        self.battery_c_val = tk.Label(self, text=str(None), font=("Courier", 14), fg="blue", relief=tk.SUNKEN, width=35)
        self.battery_c_val.grid(row=7, column=4, sticky="w")
        self.battery_c_unit = tk.Label(self, text=str("mA"), font=("Courier", 14))
        self.battery_c_unit.grid(row=7, column=5, sticky="w")

        self.battery_percen = tk.Label(self, text="percentage", font=("Courier", 14))
        self.battery_percen.grid(row=8, column=1, sticky="w")
        self.battery_percen_val = tk.Label(self, text=str(None), font=("Courier", 14), fg="blue", relief=tk.SUNKEN, width=35)
        self.battery_percen_val.grid(row=9, column=1,sticky="w")
        self.battery_percen_unit = tk.Label(self, text=str("%"), font=("Courier", 14))
        self.battery_percen_unit.grid(row=9, column=2, sticky="w")
        
        self.spacer1 = tk.Label(self, text="")
        self.spacer1.grid(row=10, column=0, pady=10)
        #swarm status
        self.sw_status = tk.Label(self, text="swarm status:", font=("Courier", 14))
        self.sw_status.grid(row=11, column=0, sticky="w")
        self.sw_status_val = tk.Label(self, text=str(None), font=("Courier", 14), fg="blue", relief=tk.SUNKEN, width=35)
        self.sw_status_val.grid(row=11, column=1, sticky="w")

        #Swarm id
        self.swarm_id = tk.Label(self, text="swarm ID:", font=("Courier", 14))
        self.swarm_id.grid(row=12, column=0, sticky="w")
        self.swarm_id_val = tk.Label(self, text=str(None), font=("Courier", 14), fg="blue", relief=tk.SUNKEN, width=35)
        self.swarm_id_val.grid(row=12, column=1, sticky="w")

        # swarm GPS
        self.swarm_gps = tk.Label(self, text="swarm GPS:", font=("Courier", 14))
        self.swarm_gps.grid(row=13, column=0,sticky="w")
        self.swarm_gps_val = tk.Label(self, text=str(None), font=("Courier", 14), fg="blue", relief=tk.SUNKEN, width=35)
        self.swarm_gps_val.grid(row=13, column=1, sticky="w")

        # swarm firmware
        self.swarm_fw = tk.Label(self, text="swarm firmware:", font=("Courier", 14))
        self.swarm_fw.grid(row=14, column=0,sticky="w")
        self.swarm_fw_val = tk.Label(self, text=str(None), font=("Courier", 14), fg="blue", relief=tk.SUNKEN, width=35)
        self.swarm_fw_val.grid(row=14, column=1, sticky="w")

        # swarm datetime
        self.swarm_dt = tk.Label(self, text="swarm datetime:", font=("Courier", 14))
        self.swarm_dt.grid(row=15, column=0,sticky="w")
        self.swarm_dt_val = tk.Label(self, text=str(None), font=("Courier", 14), fg="blue", relief=tk.SUNKEN, width=35)
        self.swarm_dt_val.grid(row=15, column=1, sticky="w")
