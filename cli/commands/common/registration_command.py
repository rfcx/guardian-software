# command to get registration information
import ctypes

def isRegistered(device):
    keystore = device.shell(f'ls /data/data/org.rfcx.guardian.guardian/files/txt/keystore_passphrase')
    if "No such file or directory" in keystore:
      return "This guardian is not registered"
    else:
      return "This guardian is registered"

def removeRegistration(device):
    device.shell(f'rm /data/data/org.rfcx.guardian.guardian/files/txt/keystore_passphrase')
    device.shell(f'rm /data/data/org.rfcx.guardian.guardian/files/txt/pin_code')
    device.shell(f'rm /data/data/org.rfcx.guardian.guardian/files/txt/token')
    device.shell(f'am force-stop org.rfcx.guardian.guardian')
    device.shell(f'monkey -p org.rfcx.guardian.guardian 1')
    return isRegistered(device)
