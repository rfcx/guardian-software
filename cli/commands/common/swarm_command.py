import operator
from functools import reduce
from urllib import response
import typer

def execute(device, message):
    command = make_command(message)
    shell_command = f'echo -n \'{command}\\r\' > /dev/ttyMT1 | /system/xbin/busybox timeout 1 /system/bin/cat < /dev/ttyMT1'
    response = device.shell(shell_command)
    lines = response.split("\n")
    # Validate checksum and remove $ *
    validated_lines = [validate_checksum(line) for line in lines] 
    # Return first line matching prefix of message
    prefix = message[:2]
    value = None
    for v in validated_lines:
        if v.startswith(prefix):
            value = v
    return value

def swarm_status(device):
    shell_command = f'cat /sys/devices/virtual/misc/mtgpio/pin | grep \'128:\''
    sw_status = device.shell(shell_command)
    sw_status = sw_status.split("\n")[0].split(":")[1].split("\r")[0]
    if sw_status == "0011010-1":
        return True
    return False

def swarm_id(device):
    swarm_id = execute(device, 'CS')
    if swarm_id != None:
        return str(swarm_id.split(" ")[1])
    return None

def swarm_gps(device):
    # swarm_gps = 'GS 109,214,9,0,G3' #Returns a HDOP of 1.09, VDOP of 2.14, 
    # the device is using 9 GNSS satellites for this solution,and it is a Standalone 3D solution.
    swarm_gps = execute(device, 'GS @')
    if swarm_gps != None:
        swarm_gps = swarm_gps.split(" ")[1].split(",")
        return swarm_gps
    return None
 
def swarm_firmware(device):
    swarm_fw = execute(device, 'FV') 
    if swarm_fw != None:
        return str(swarm_fw.split(" ")[1])
    return None

def swarm_datetime(device):
    swarm_dt = execute(device, 'DT') 
    if swarm_dt != None:
        return str(swarm_dt.split(" ")[1])
    return None

def validate_checksum(sentence: str):
    if(sentence.startswith('$')):
        sentence = sentence.strip("$\n")
        nmeadata, checksum = sentence.split("*", 1)
        calculated_checksum = reduce(operator.xor, (ord(s) for s in nmeadata), 0)
        if int(checksum, base=16) == calculated_checksum:
            return nmeadata
        else:
            raise ValueError("The NMEA data does not match it's checksum")
    else:
        return "None" # If your code to detect None before calling startswith.

def make_command(message: str):
    return '$' + message + '*' + get_checksum(message)

def get_checksum(message: str):
    # Calculate the checksum on the message
    sum1 = 0
    for c in message:
        sum1 = sum1 ^ ord(c)
    sum1 = sum1 & 0xFF
    return str(hex(sum1))[2:]
