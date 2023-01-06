# command to get classifier information
import ctypes
import urllib.request
import shutil
import gzip
import os
import os.path
import logging
import glob
import pathlib
import sys

dirPath = os.path.dirname(sys.executable)

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
        url = f'http://install.rfcx.org/rfcx-guardian/guardian-android-{role}/production/{role}-1.1.7.apk.gz'
        fileName = f'{role}-1.1.7.apk'
    elif role == 'admin':
        url = f'http://install.rfcx.org/rfcx-guardian/guardian-android-{role}/production/{role}-1.1.7.apk.gz'
        fileName = f'{role}-1.1.7.apk'
    elif role == 'classify':
        url = f'http://install.rfcx.org/rfcx-guardian/guardian-android-{role}/production/{role}-1.1.4.apk.gz'
        fileName = f'{role}-1.1.4.apk'
    elif role == 'updater':
        url = f'http://install.rfcx.org/rfcx-guardian/guardian-android-{role}/production/{role}-1.0.0.apk.gz'
        fileName = f'{role}-1.0.0.apk'

    localFileDir = dirPath + f'/temp'
    if (os.path.exists(localFileDir) == False):
        os.makedirs(localFileDir)
    localFilePath = dirPath + f'/temp/{fileName}'
    if (os.path.exists(localFilePath)):
        return installSoftware(device, fileName, role)
    else:
        with urllib.request.urlopen(url) as response:
            with gzip.GzipFile(fileobj=response) as uncompressed, open(localFilePath, 'wb') as out_file:
                shutil.copyfileobj(uncompressed, out_file)
                return installSoftware(device, fileName, role)

def installSoftware(device, fileName, role):
    filePath = dirPath + f'/temp/{fileName}'
    devicePath = f'/sdcard/{fileName}'
    device.push(devicePath, filePath, callback)
    response = device.shell(f'pm install -r {devicePath}')
    device.shell(f'monkey -p org.rfcx.guardian.{role} 1')
    device.shell(f'content query --uri content://org.rfcx.guardian.guardian/prefs_set/enable_file_socket%7Ctrue')
    return getSoftwares(device)

def downgradeSoftwares(device, role):
    url = ''
    fileName = ''
    if role == 'guardian':
        url = f'http://install.rfcx.org/rfcx-guardian/guardian-android-{role}/production/{role}-1.1.6.apk.gz'
        fileName = f'{role}-1.1.6.apk'
    elif role == 'admin':
        url = f'http://install.rfcx.org/rfcx-guardian/guardian-android-{role}/production/{role}-1.1.6.apk.gz'
        fileName = f'{role}-1.1.6.apk'
    elif role == 'classify':
        url = f'http://install.rfcx.org/rfcx-guardian/guardian-android-{role}/production/{role}-1.1.3.apk.gz'
        fileName = f'{role}-1.1.3.apk'
    elif role == 'updater':
        url = f'http://install.rfcx.org/rfcx-guardian/guardian-android-{role}/production/{role}-0.9.0.apk.gz'
        fileName = f'{role}-0.9.0.apk'
    
    localFileDir = dirPath + f'/temp'
    if (os.path.exists(localFileDir) == False):
        os.makedirs(localFileDir)
    localFilePath = dirPath + f'/temp/{fileName}'
    if (os.path.exists(localFilePath)):
        device.shell(f'pm uninstall org.rfcx.guardian.{role}')
        return installSoftware(device, fileName, role)
    else:
        with urllib.request.urlopen(url) as response:
            with gzip.GzipFile(fileobj=response) as uncompressed, open(localFilePath, 'wb') as out_file:
                shutil.copyfileobj(uncompressed, out_file)
                device.shell(f'pm uninstall org.rfcx.guardian.{role}')
                return installSoftware(device, fileName, role)

def callback(device_path, bytes_written, total_bytes):
    logging.info(f"Progress: {device_path} written:{bytes_written} total:{total_bytes}")
