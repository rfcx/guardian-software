# command to get i2c information
import ctypes

def execute(device, command):
    shell_command = f'i2cget -y 1 0x68 {command} w'
    response = device.shell(shell_command)
    if response != None:
        return response
    else:
        return None

def i2cStatus(device):
    result = execute(device, '0x4a') 
    i2c_status = result[0:6]
    if i2c_status == "0x0001":
        return True
    else:
        return False

def input_voltage(device):
    result = execute(device, '0x3b') 
    input_v = result[0:6]
    input_v = int(input_v, 16)*1.648
    return float(input_v)

def input_current(device):
    result = execute(device, '0x3e') 
    input_c = result[0:6]
    input_c = int(input_c, 16)*(0.00146487 /0.005)
    return float(input_c)

def system_voltage(device):
    result = execute(device, '0x3c') 
    sys_v = result[0:6]
    sys_v = int(sys_v, 16)*1.648
    return float(sys_v)

def battery_voltage(device):
    result = execute(device, '0x3a') 
    bat_v = result[0:6]
    bat_v = int(bat_v, 16)*0.192264
    return float(bat_v)

def battery_cuurent(device):
    result = execute(device, '0x3d') 
    bat_c = result[0:6]
    bat_c = int(bat_c, 16)
    bat_c = ctypes.c_int16(bat_c).value
    bat_c = bat_c*(0.00146487/0.003)
    return float(bat_c)

def battery_percentage(device):
    result = execute(device, '0x13') 
    bat_p = result[0:6]
    bat_p =((int(bat_p, 16)-16384)/32768)*100
    return float(bat_p)
