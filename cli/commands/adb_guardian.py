from tkinter import *
import ctypes
from adb_shell.adb_device import AdbDeviceTcp
# import typer


def start():
    print("start")

    host = '192.168.43.1'
    port = 7329
    device = AdbDeviceTcp(host, port, default_transport_timeout_s=5.)
    device.connect(auth_timeout_s=.5)

    # ECHO Checking i2c available (expect 0x0001)
    i2c_cmd = f'i2cget -y 1 0x68 0x4a w'
    i2c = device.shell(i2c_cmd)
    i2c = i2c[0:6]
    # print("i2c",i2c,type(i2c))

    # ECHO Input Voltage 
    input_v_cmd = 'i2cget -y 1 0x68 0x3b w'
    input_v = device.shell(input_v_cmd)
    input_v = input_v[0:6]
    input_v = int(input_v, 16)*1.648
    input_v = float("{:.2f}".format(input_v))
    # print(input_v)

    # ECHO Input Current 
    input_c_cmd = 'i2cget -y 1 0x68 0x3e w'
    input_c = device.shell(input_c_cmd)
    input_c = input_c[0:6]
    input_c = int(input_c, 16)*(0.00146487 /0.005)
    input_c = float("{:.2f}".format(input_c))
    # print(input_c)

     # ECHO sys Voltage 
    sys_v_cmd = 'i2cget -y 1 0x68 0x3c w'
    sys_v = device.shell(sys_v_cmd)
    sys_v = sys_v[0:6]
    sys_v = int(sys_v, 16)*1.648
    sys_v = float("{:.2f}".format(sys_v))
    # print(sys_v)

    # ECHO Battery Voltage 
    # bat_v = os.popen('adb shell i2cget -y 1 0x68 0x3a w').read()
    bat_v_cmd = 'i2cget -y 1 0x68 0x3a w'
    bat_v = device.shell(bat_v_cmd)
    bat_v = bat_v[0:6]
    bat_v = int(bat_v, 16)*0.192264
    bat_v = float("{:.2f}".format(bat_v))
    # print(bat_v)

    # ECHO Battery Current 
    bat_c_cmd = 'i2cget -y 1 0x68 0x3d w'
    bat_c = device.shell(bat_c_cmd)
    bat_c = bat_c[0:6]
    bat_c = int(bat_c, 16)
    bat_c = ctypes.c_int16(bat_c).value
    bat_c = bat_c*(0.00146487/0.003)
    bat_c = float("{:.2f}".format(bat_c))
    # print("Battery Current:",bat_c)

    # ECHO Battery percent
    bat_p_cmd = 'i2cget -y 1 0x68 0x13 w'
    bat_p = device.shell(bat_p_cmd)
    bat_p = bat_p[0:6]
    bat_p =((int(bat_p, 16)-16384)/32768)*100
    bat_p = float("{:.2f}".format(bat_p))
    # print("Battery percent",bat_p)

    # ECHO Checking Swarm is on (128:0011010-1 = on, 128:0000010-1 = off)
    swarm_status_cmd = 'cat /sys/devices/virtual/misc/mtgpio/pin | grep \'128:\''
    swarm_status = device.shell(swarm_status_cmd)
    swarm_status = swarm_status[4:13]
    # print(swarm_status)

    # ECHO Swarm id
    sw_id_cmd = '$CS*10'
    shell_command = f'echo -n \'{sw_id_cmd}\\r\' > /dev/ttyMT1 | /system/xbin/busybox timeout 1 /system/bin/cat < /dev/ttyMT1'
    # typer.echo(shell_command)
    sw_id = device.shell(shell_command)
    sw_id = sw_id.split("\n")
    if sw_id[0] == 'Terminated':
        sw_id_list = list(sw_id[0])
        sw_id = sw_id_list[0]
    else:
        split_id = list(sw_id[0].split(" "))
        sw_id = str(split_id[1])
    # print(sw_id)

    
    # ECHO Swarm GPS
    sw_gps_cmd = '$GS @*74'
    shell_command = f'echo -n \'{sw_gps_cmd}\\r\' > /dev/ttyMT1 | /system/xbin/busybox timeout 1 /system/bin/cat < /dev/ttyMT1'
    sw_gps = device.shell(shell_command)
    sw_gps = list(sw_gps.split("\n"))
    sw_gps = sw_gps[0]
    # print("sw_gps",sw_gps)


    # ECHO Swarm firm ware
    sw_fw_cmd = '$FV*10'
    shell_command = f'echo -n \'{sw_fw_cmd}\\r\' > /dev/ttyMT1 | /system/xbin/busybox timeout 1 /system/bin/cat < /dev/ttyMT1'
    sw_fw = device.shell(shell_command)
    sw_fw = list(sw_fw.split("\n"))
    if sw_fw[0] == 'Terminated':
        sw_fw_list = list(sw_fw[0])
        sw_fw = sw_fw_list[0]
    else:
        split_fw = list(sw_fw[0].split(" "))
        sw_fw = str(split_fw[1])
    # print(sw_fw)

      # ECHO Swarm datetime
    sw_dt_cmd = '$DT @*70'
    shell_command = f'echo -n \'{sw_dt_cmd}\\r\' > /dev/ttyMT1 | /system/xbin/busybox timeout 1 /system/bin/cat < /dev/ttyMT1'
    sw_dt = device.shell(shell_command)
    sw_dt = list(sw_dt.split("\n"))
    sw_dt = sw_dt[0]
    # print('sw_dt',sw_dt)

    #disconnect adb
    device.close()
    
    # call function
    complete()
    show_i2c_status(i2c)
    sentnel_info(input_v,input_c,sys_v,bat_v,bat_c,bat_p)
    swarm_info(swarm_status,sw_id,sw_gps,sw_fw,sw_dt)

def complete():
    complete = Label(window, text="DONE!!!", font=("Courier", 16),fg="green")
    complete.place(x=230, y=50)

def show_i2c_status(val):
    val = str(val)
    i2c = Label(window, text="i2c Status   :", font=("Courier", 14))
    i2c.place(x=20, y=100)

    color = 'blue'
    print(type(val))
    if val == "0x0001":
        val = str(val)+" (I2C OK)"
        color = 'green'
    elif val == "0x0000":
        color = 'red'
        val = str(val)+" (Sentinel Power Chip is NOT Accessible via I2C)"
    elif val == "Null":
        color = 'blue'
        val = "Null"
    else :
        color = 'red'
        val = "Read Fail"

    i2c_val = Label(window, text=str(val), font=("Courier", 14), fg=color)
    i2c_val.place(x=180, y=100)

def sentnel_info(in_v,in_c,sys_v,bat_v,bat_c,bat_p):
   
    # input power info
    input_info = Label(window, text="input info   :", font=("Courier", 14))
    input_info.place(x=20, y=140)

    input_v = Label(window, text="voltage =", font=("Courier", 14))
    input_v.place(x=180, y=140)
    input_v_val = Label(window, text=str(in_v), font=("Courier", 14), fg="blue")
    input_v_val.place(x=285, y=140)
    input_v_unit = Label(window, text=str("mV"), font=("Courier", 14), )
    input_v_unit.place(x=380, y=140)

    input_c = Label(window, text="/ current =", font=("Courier", 14))
    input_c.place(x=450, y=140)
    input_c_val = Label(window, text=str(in_c), font=("Courier", 14), fg="blue")
    input_c_val.place(x=580, y=140)
    input_c_unit = Label(window, text=str("mA"), font=("Courier", 14), )
    input_c_unit.place(x=675, y=140)

    # system info
    system_info = Label(window, text="system info  :", font=("Courier", 14))
    system_info.place(x=20, y=180)

    system_v = Label(window, text="voltage =", font=("Courier", 14))
    system_v.place(x=180, y=180)
    system_v_val = Label(window, text=str(sys_v), font=("Courier", 14), fg="blue")
    system_v_val.place(x=285, y=180)
    system_v_unit = Label(window, text=str("mV"), font=("Courier", 14), )
    system_v_unit.place(x=380, y=180)
  
    # battery info
    battery_info = Label(window, text="battery info :", font=("Courier", 14))
    battery_info.place(x=20, y=220)

    battery_v = Label(window, text="voltage =", font=("Courier", 14))
    battery_v.place(x=180, y=220)
    battery_v_val = Label(window, text=str(bat_v), font=("Courier", 14), fg="blue")
    battery_v_val.place(x=285, y=220)
    battery_v_unit = Label(window, text=str("mV"), font=("Courier", 14), )
    battery_v_unit.place(x=380, y=220)

    battery_c = Label(window, text="/ current =", font=("Courier", 14))
    battery_c.place(x=450, y=220)
    battery_c_val = Label(window, text=str(bat_c)+" ", font=("Courier", 14), fg="blue")
    battery_c_val.place(x=580, y=220)
    battery_c_unit = Label(window, text=str("mA"), font=("Courier", 14), )
    battery_c_unit.place(x=675, y=220)

    battery_percen = Label(window, text="percentage =", font=("Courier", 14))
    battery_percen.place(x=180, y=260)
    battery_percen_val = Label(window, text=str(bat_p), font=("Courier", 14), fg="blue")
    battery_percen_val.place(x=320, y=260)
    battery_percen_unit = Label(window, text=str("%"), font=("Courier", 14), )
    battery_percen_unit.place(x=380, y=260)

def swarm_info(sw_status,sw_id,sw_gps,sw_fw,sw_dt):
    # swarm status
    swarm_info = Label(window, text="swarm status :", font=("Courier", 14))
    swarm_info.place(x=20, y=300)
    color = 'blue'
    if sw_status == "0011010-1":
        sw_status = str(sw_status)+" (ON)"
        color = 'green'
    elif sw_status == "0000010-1":
        color = 'red'
        sw_status = str(sw_status)+" (OFF)"
    battery_percen_val = Label(window, text=str(sw_status), font=("Courier", 14), fg=color)
    battery_percen_val.place(x=180, y=300)

    # swarm id
    swarm_id = Label(window, text="swarm ID     :", font=("Courier", 14))
    swarm_id.place(x=20, y=340)
    color = 'blue'
    if sw_id == " ":
        sw_id = "Swarm is OFF"
        color = 'red'
    elif sw_id == 'Null':
        sw_id = "Null"
        color = 'blue'
    else:
        color = 'green'
        sw_id = str(sw_id)
    swarm_id = Label(window, text=str(sw_id), font=("Courier", 14), fg=color)
    swarm_id.place(x=180, y=340)

    # swarm GPS
    swarm_gps = Label(window, text="swarm GPS    :", font=("Courier", 14))
    swarm_gps.place(x=20, y=380)
    color = 'blue'
    if (sw_gps != "Null"):
        color = 'green'
    swarm_gps = Label(window, text=str(sw_gps), font=("Courier", 14), fg=color)
    swarm_gps.place(x=180, y=380)


    # swarm firmware
    swarm_fw = Label(window, text="swarm firmware:", font=("Courier", 14))
    swarm_fw.place(x=20, y=420)
    color = 'blue'
    if sw_fw == " ":
        sw_fw = "Swarm is OFF"
        color = 'red'
    elif sw_fw == 'Null':
        sw_fw = "Null"
        color = 'blue'
    else:
        color = 'green'
        sw_fw = str(sw_fw)
    swarm_fw = Label(window, text=str(sw_fw), font=("Courier", 14), fg=color)
    swarm_fw.place(x=200, y=420)

    # swarm datetime
    swarm_dt = Label(window, text="swarm datetime:", font=("Courier", 14))
    swarm_dt.place(x=20, y=460)
    color = 'blue'
    if (sw_dt != "Null"):
        color = 'green'
        
    swarm_dt = Label(window, text=str(sw_dt), font=("Courier", 14), fg=color)
    swarm_dt.place(x=200, y=460)

#make windows exe
window = Tk()
window.title('Guardian Diagnosis')
window.geometry("720x540+10+20")
 
# do btn start
btn_start = Button(window, text="START", fg='white',
                   bg="green", width=25, height=2, command=start)
btn_start.place(x=20, y=30)

#call function before
show_i2c_status("Null")
sentnel_info("Null","Null","Null","Null","Null","Null")
swarm_info("Null","Null","Null","Null","Null")

window.mainloop()
