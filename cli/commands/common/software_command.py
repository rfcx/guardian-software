# command to get classifier information
import ctypes
import urllib.request
import shutil
import gzip
import os
import logging

dirPath = os.path.abspath(os.getcwd())

logging.basicConfig(level=logging.INFO)

def getSoftwares(device):
    guardian = device.shell(f'cat /data/data/org.rfcx.guardian.guardian/files/txt/version')
    admin = device.shell(f'cat /data/data/org.rfcx.guardian.admin/files/txt/version')
    classify = device.shell(f'cat /data/data/org.rfcx.guardian.classify/files/txt/version')
    updater = device.shell(f'cat /data/data/org.rfcx.guardian.updater/files/txt/version')
    software = {"guardian": guardian, "admin": admin, "classify": classify, "updater": updater}
    return software

def downloadSoftwares(device, role):
    url = ''
    fileName = ''
    if role == 'guardian':
        url = f'http://install.rfcx.org/rfcx-guardian/guardian-android-{role}/production/{role}-1.1.5.apk.gz'
        fileName = f'{role}-1.1.5.apk'
    elif role == 'admin':
        url = f'http://install.rfcx.org/rfcx-guardian/guardian-android-{role}/production/{role}-1.1.4.apk.gz'
        fileName = f'{role}-1.1.4.apk'
    elif role == 'classify':
        url = f'http://install.rfcx.org/rfcx-guardian/guardian-android-{role}/production/{role}-1.1.3.apk.gz'
        fileName = f'{role}-1.1.3.apk'
    elif role == 'updater':
        url = f'http://install.rfcx.org/rfcx-guardian/guardian-android-{role}/production/{role}-1.0.0.apk.gz'
        fileName = f'{role}-1.0.0.apk'

    with urllib.request.urlopen(url) as response:
        with gzip.GzipFile(fileobj=response) as uncompressed, open(fileName, 'wb') as out_file:
            shutil.copyfileobj(uncompressed, out_file)
            filePath = f'{dirPath}/{fileName}'
            devicePath = f'/sdcard/{fileName}'
            device.push(devicePath, filePath, callback)
            response = device.shell(f'pm install -r {devicePath}')
            device.shell(f'monkey -p org.rfcx.guardian.{role} 1')
    return getSoftwares(device)

def downgradeSoftwares(device, role):
    url = ''
    fileName = ''
    if role == 'guardian':
        url = f'http://install.rfcx.org/rfcx-guardian/guardian-android-{role}/production/{role}-1.1.4.apk.gz'
        fileName = f'{role}-1.1.4.apk'
    elif role == 'admin':
        url = f'http://install.rfcx.org/rfcx-guardian/guardian-android-{role}/production/{role}-1.1.3.apk.gz'
        fileName = f'{role}-1.1.3.apk'
    elif role == 'classify':
        url = f'http://install.rfcx.org/rfcx-guardian/guardian-android-{role}/production/{role}-1.0.0.apk.gz'
        fileName = f'{role}-1.0.0.apk'
    elif role == 'updater':
        url = f'http://install.rfcx.org/rfcx-guardian/guardian-android-{role}/production/{role}-0.9.0.apk.gz'
        fileName = f'{role}-0.9.0.apk'

    with urllib.request.urlopen(url) as response:
        with gzip.GzipFile(fileobj=response) as uncompressed, open(fileName, 'wb') as out_file:
            shutil.copyfileobj(uncompressed, out_file)
            filePath = f'{dirPath}/{fileName}'
            devicePath = f'/sdcard/{fileName}'
            device.push(devicePath, filePath, callback)
            device.shell(f'pm uninstall org.rfcx.guardian.{role}')
            response = device.shell(f'pm install -r {devicePath}')
            device.shell(f'monkey -p org.rfcx.guardian.{role} 1')
    return getSoftwares(device)

def callback(device_path, bytes_written, total_bytes):
    logging.info(f"Progress: {device_path} written:{bytes_written} total:{total_bytes}")
