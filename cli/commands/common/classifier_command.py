# command to get classifier information
import ctypes
import urllib.request
import shutil
import gzip
import os
import os.path
import logging
import time
import json

dirPath = os.path.abspath(os.getcwd())

logging.basicConfig(level=logging.INFO)

def getClassifiers(device):
    classifier = device.shell(f'sqlite3 /data/data/org.rfcx.guardian.guardian/databases/library-classifier.db \"SELECT * FROM classifier;\"')
    logging.info(classifier)
    if classifier == '':
      return {}
    else:
      filtered = list(filter(lambda item: item != '', classifier.split("\r\n")))
      classifiers = {}
      for item in filtered:
        meta = json.loads(item.split("|")[7])
        classifierName = meta["classifier_name"]
        classifierVersion = meta["classifier_version"]
        classifiers[classifierName] = classifierVersion
      return classifiers

def removeClassifiers(device):
    device.shell(f'sqlite3 /data/data/org.rfcx.guardian.guardian/databases/library-classifier.db \"DELETE FROM classifier;\"')
    device.shell(f'sqlite3 /data/data/org.rfcx.guardian.guardian/databases/audio-classifier-active.db \"DELETE FROM active;\"')
    device.shell(f'rm -f /data/data/org.rfcx.guardian.guardian/files/classifiers/active/*')
    device.shell(f'rm -f /data/data/org.rfcx.guardian.guardian/files/classifiers/library/*')
    return getClassifiers(device)

def downloadClassifier(device, classifier):
    url = ''
    fileName = ''
    clrsId = ''
    clrsName = ''
    clrsVersion = ''
    clrsSha1 = ''
    clrsSampleRate = ''
    clrsInputGain = ''
    clrsWindowSize = ''
    clrsStepSize = ''
    clrsClassifications = ''
    clrsFilterThreshold = ''
    if classifier == 'asia-elephant-edge':
      url = 'https://rfcx-install.s3.eu-west-1.amazonaws.com/rfcx-guardian/guardian-asset-classifier/1637901623151.tflite.gz'
      fileName = '1637901623151.tflite'
      clrsId = '1637901623151'
      clrsName = 'asia-elephant-edge'
      clrsVersion = '2'
      clrsSha1 = '69482d8b65083e2fabcf1096033c863409cc50f7'
      clrsSampleRate = '8000'
      clrsInputGain = '1.0'
      clrsWindowSize = '2.5000'
      clrsStepSize = '2.0000'
      clrsClassifications = 'elephas_maximus,environment'
      clrsFilterThreshold = '0.98,1.00'
    elif classifier == 'chainsaw':
      url = 'https://rfcx-install.s3.eu-west-1.amazonaws.com/rfcx-guardian/guardian-asset-classifier/1617208867756.tflite.gz'
      fileName = '1617208867756.tflite'
      clrsId = '1617208867756'
      clrsName = 'chainsaw'
      clrsVersion = '5'
      clrsSha1 = 'accfb018701e52696835c9d1c02600a67a228db1'
      clrsSampleRate = '12000'
      clrsInputGain = '1.0'
      clrsWindowSize = '0.9750'
      clrsStepSize = '1'
      clrsClassifications = 'chainsaw,environment'
      clrsFilterThreshold = '0.95,1.00'

    localFileDir = f'temp'
    if (os.path.exists(localFileDir) == False):
      os.makedirs(localFileDir)
    localFilePath = f'temp/{fileName}'
    if (os.path.exists(localFilePath)):
      device.push(f'/data/data/org.rfcx.guardian.guardian/files/classifiers/library/{fileName}', localFilePath, callback)
      device.shell(f'sqlite3 /data/data/org.rfcx.guardian.guardian/databases/library-classifier.db "INSERT INTO classifier VALUES (\'{int(time.time())}\',\'{clrsId}\',\'classifier\',\'tflite\',\'{clrsSha1}\',\'/data/data/org.rfcx.guardian.guardian/files/classifiers/library/{fileName}\',\'{os.path.getsize(localFilePath)}\',\'{{\\\"classifier_name\\\":\\\"{clrsName}\\\",\\\"classifier_version\\\":\\\"{clrsVersion}\\\",\\\"sample_rate\\\":\\\"{clrsSampleRate}\\\",\\\"input_gain\\\":\\\"{clrsInputGain}\\\",\\\"window_size\\\":\\\"{clrsWindowSize}\\\",\\\"step_size\\\":\\\"{clrsStepSize}\\\",\\\"classifications\\\":\\\"{clrsClassifications}\\\",\\\"classifications_filter_threshold\\\":\\\"{clrsFilterThreshold}\\\"}}\',\'0\',\'0\',\'0\',\'{int(time.time())}\');"')
      return getClassifiers(device)
    else:
      with urllib.request.urlopen(url) as response:
          with gzip.GzipFile(fileobj=response) as uncompressed, open(localFilePath, 'wb') as out_file:
              shutil.copyfileobj(response, out_file)
              device.push(f'/data/data/org.rfcx.guardian.guardian/files/classifiers/library/{fileName}', localFilePath, callback)
              device.shell(f'sqlite3 /data/data/org.rfcx.guardian.guardian/databases/library-classifier.db "INSERT INTO classifier VALUES (\'{int(time.time())}\',\'{clrsId}\',\'classifier\',\'tflite\',\'{clrsSha1}\',\'/data/data/org.rfcx.guardian.guardian/files/classifiers/library/{fileName}\',\'{os.path.getsize(localFilePath)}\',\'{{\\\"classifier_name\\\":\\\"{clrsName}\\\",\\\"classifier_version\\\":\\\"{clrsVersion}\\\",\\\"sample_rate\\\":\\\"{clrsSampleRate}\\\",\\\"input_gain\\\":\\\"{clrsInputGain}\\\",\\\"window_size\\\":\\\"{clrsWindowSize}\\\",\\\"step_size\\\":\\\"{clrsStepSize}\\\",\\\"classifications\\\":\\\"{clrsClassifications}\\\",\\\"classifications_filter_threshold\\\":\\\"{clrsFilterThreshold}\\\"}}\',\'0\',\'0\',\'0\',\'{int(time.time())}\');"')
      return getClassifiers(device)

def callback(device_path, bytes_written, total_bytes):
    logging.info(f"Progress: {device_path} written:{bytes_written} total:{total_bytes}")
