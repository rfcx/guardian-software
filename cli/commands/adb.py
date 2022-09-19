from adb_shell.adb_device import AdbDeviceTcp, AdbDeviceUsb
import stat

class Device:

    def __init__(self, host = '192.168.1.218', port = 5555):
        self.device = AdbDeviceTcp(host, port, default_transport_timeout_s=30.)
        self.device.connect(auth_timeout_s=.5)

    def shell(self, command):
        return self.device.shell(command)

    def push(self, devicePath, filePath, callback):
        return self.device.push(filePath, devicePath, progress_callback=callback)
