# command to get registration information
import ctypes
import re

def getStorage(device):
    storage = device.shell(f'df /data')
    filteredStorage = re.findall('[\d,]+\.\d+[G,M,K]', storage.split("\r\n")[1])
    return { "free": filteredStorage[1], "all": filteredStorage[2]}

def removeStorage(device):
    device.shell(f'rm -f /data/log_temp/boot/*')
    device.shell(f'rm -f /data/data/org.rfcx.guardian.guardian/files/audio/encode/*')
    device.shell(f'rm -rf /storage/sdcard0/mtklog')
    device.shell(f'rm -rf /storage/sdcard1/mtklog')
    return getStorage(device)
