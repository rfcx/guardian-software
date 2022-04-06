# command to get i2c information
import ctypes

def execute(device, command):
    shell_command = f'i2cget -y 1 0x68 {command} w'
    response = device.shell(shell_command)[0:6]
    response = int(response, 16)
    return response

def i2cStatus(device):
    i2c_status = execute(device, '0x4a') 
    return i2c_status

def input_voltage(device):
    input_v = execute(device, '0x3b') 
    input_v = input_v * 1.648
    return float(input_v)

def input_current(device):
    input_c = execute(device, '0x3e') 
    input_c = input_c * (0.00146487 / 0.005)
    return float(input_c)

def system_voltage(device):
    sys_v = execute(device, '0x3c') 
    sys_v = sys_v * 1.648
    return float(sys_v)

def battery_voltage(device):
    bat_v = execute(device, '0x3a') 
    bat_v = bat_v * 0.192264
    return float(bat_v)

def battery_curent(device):
    bat_c = execute(device, '0x3d') 
    bat_c = ctypes.c_int16(bat_c).value
    bat_c = bat_c * (0.00146487 / 0.003)
    return float(bat_c)

def battery_percentage(device):
    bat_p = execute(device, '0x13') 
    bat_p =((bat_p - 16384) / 32768) * 100
    return float(bat_p)
