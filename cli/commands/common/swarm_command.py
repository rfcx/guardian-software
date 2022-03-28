import operator
from functools import reduce

def execute(device, command):
    shell_command = f'echo -n \'{command}\\r\' > /dev/ttyMT1 | /system/xbin/busybox timeout 1 /system/bin/cat < /dev/ttyMT1'
    response = device.shell(shell_command)
    return response

def swarm_status(device):
    shell_command = f'cat /sys/devices/virtual/misc/mtgpio/pin | grep \'128:\''
    sw_status = device.shell(shell_command)
    return sw_status

def swarm_id(device):
    commamd = make_checksum('CS')
    swarm_id = execute(device, commamd)
    return swarm_id

def swarm_gps(device):
    commamd = make_checksum('GS @')
    swarm_gps = execute(device, commamd)
    return swarm_gps

def swarm_firmware(device):
    commamd = make_checksum('FV')
    swarm_fw = execute(device, commamd) 
    return swarm_fw

def swarm_datetime(device):
    commamd = make_checksum('DT')
    swarm_dt = execute(device, commamd) 
    return swarm_dt


def nmea_checksum(sentence: str):
    sentence = sentence.strip("$\n")
    nmeadata, checksum = sentence.split("*", 1)
    calculated_checksum = reduce(operator.xor, (ord(s) for s in nmeadata), 0)
    if int(checksum, base=16) == calculated_checksum:
        return nmeadata
    else:
        raise ValueError("The NMEA data does not match it's checksum")

def make_checksum( msg ):
    msg = '$'+msg+'*'
    # Find the start of the NMEA sentence
    startchars = "!$"
    for c in startchars:
        i = msg.find(c)
        if i >= 0: break
    else:
        return (False, None, None)

    # Calculate the checksum on the message
    sum1 = 0
    for c in msg[i+1:]:
        if c == '*':
	        break
        sum1 = sum1 ^ ord(c)
    sum1 = sum1 & 0xFF
    c_sum = str(hex(sum1))[2:]
    c_sum = msg+c_sum
    return c_sum