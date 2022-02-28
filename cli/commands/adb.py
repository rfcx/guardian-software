from adb_shell.adb_device import AdbDeviceTcp

class Device:

    def __init__(self, host = '192.168.43.1', port = 7329):
        self.device = AdbDeviceTcp(host, port, default_transport_timeout_s=5.)
        self.device.connect(auth_timeout_s=.5)

    def shell(self, command):
        return self.device().shell(command)
