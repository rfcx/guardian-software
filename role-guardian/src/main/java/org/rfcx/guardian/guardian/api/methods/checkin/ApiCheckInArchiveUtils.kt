package org.rfcx.guardian.guardian.api.methods.checkin

import android.content.Context
import android.os.Environment
import android.text.TextUtils
import android.util.Log
import org.json.JSONObject
import org.rfcx.guardian.guardian.RfcxGuardian
import org.rfcx.guardian.utility.asset.RfcxAudioFileUtils
import org.rfcx.guardian.utility.device.capture.DeviceStorage
import org.rfcx.guardian.utility.misc.FileUtils
import org.rfcx.guardian.utility.misc.StringUtils
import org.rfcx.guardian.utility.rfcx.RfcxLog
import org.rfcx.guardian.utility.rfcx.RfcxPrefs
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class ApiCheckInArchiveUtils(private val context: Context) {

    private val logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "ApiCheckInArchiveUtils")


    private var app: RfcxGuardian? = null
    private var rfcxDeviceId: String? = null
    private var archiveTimestamp: Long? = null

    private val dirDateFormat = SimpleDateFormat("yyyy-MM", Locale.US)
    private val fileDateTimeFormat = SimpleDateFormat("yyyy-MM-dd'T'HH-mm-ss", Locale.US)
    private val metaDateTimeFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.US)
    private val tsvMetaColumns = arrayOf(
        "measured_at",
        "queued_at",
        "filename",
        "format",
        "sha1checksum",
        "samplerate",
        "bitrate",
        "encode_duration"
    )
    private var archiveSdCardDir: String? = null
    private var archiveTitle: String? = null
    private var archiveWorkDir: String? = null
    private var archiveTar: String? = null
    private var archiveTarFilePath: String? = null
    private var archiveFinalFilePath: String? = null

    init {
        app = context.applicationContext as RfcxGuardian
        rfcxDeviceId = app!!.rfcxGuardianIdentity.guid
    }

    fun archiveCheckIn() {
        val archiveFileSizeTarget =
            app!!.rfcxPrefs.getPrefAsLong(RfcxPrefs.Pref.CHECKIN_ARCHIVE_FILESIZE_TARGET)
        val archiveFileSizeTargetInBytes = archiveFileSizeTarget * 1024 * 1024

        val stashFileSizeBuffer =
            app!!.rfcxPrefs.getPrefAsLong(RfcxPrefs.Pref.CHECKIN_STASH_FILESIZE_BUFFER)
        val stashFileSizeBufferInBytes = stashFileSizeBuffer * 1024 * 1024

        val stashedCumulativeFileSizeInBytes =
            app!!.apiCheckInDb.dbStashed.cumulativeFileSizeForAllRows

        if (!File(archiveSdCardDir).isDirectory) {
            Log.e(
                logTag,
                "CheckIn Archive job cancelled because SD card directory could not be located: $archiveSdCardDir"
            )
        } else if (stashedCumulativeFileSizeInBytes < stashFileSizeBufferInBytes + archiveFileSizeTargetInBytes) {
            Log.e(
                logTag,
                "CheckIn Archive job cancelled because archive threshold ($archiveFileSizeTarget MB) has not been reached."
            )
        } else {
            try {
                val stashedCheckInsBeyondBuffer: MutableList<Array<String>> = ArrayList()
                val allStashedCheckIns = app!!.apiCheckInDb.dbStashed.allRows
                var fileSizeBufferTracker: Long = 0
                for (i in allStashedCheckIns.indices.reversed()) {
                    fileSizeBufferTracker += allStashedCheckIns[i][6].toLong()
                    if (fileSizeBufferTracker > stashFileSizeBufferInBytes) {
                        stashedCheckInsBeyondBuffer.add(allStashedCheckIns[i])
                    }
                }
                if (!app!!.rfcxPrefs.getPrefAsBoolean(RfcxPrefs.Pref.ENABLE_CHECKIN_ARCHIVE)) {
                    Log.d(
                        logTag,
                        "CheckIn Archive disabled due to preference 'enable_checkin_archive' being explicitly set to false."
                    )
                } else {
                    archive(stashedCheckInsBeyondBuffer)
                }

                // Clean up and remove archived originals
                for (checkIn in stashedCheckInsBeyondBuffer) {
                    FileUtils.delete(checkIn[4])
                    app!!.apiCheckInDb.dbStashed.deleteSingleRowByAudioAttachmentId(checkIn[1])
                }
                FileUtils.delete(archiveWorkDir)
                FileUtils.delete(archiveTarFilePath)
                Log.d(
                    logTag,
                    stashedCheckInsBeyondBuffer.size.toString() + " CheckIns have been deleted from stash."
                )
            } catch (e: Exception) {
                RfcxLog.logExc(logTag, e)
            }
        }
    }

    fun archiveAllQueueAndStash() {
        if (!File(archiveSdCardDir).isDirectory) {
            Log.e(
                logTag,
                "CheckIn Archive job cancelled because SD card directory could not be located: $archiveSdCardDir"
            )
        } else {
            try {
                val queues = app!!.apiCheckInDb.dbQueued.allRows
                val stashes = app!!.apiCheckInDb.dbStashed.allRows
                archive(queues + stashes)
                // Clean up and remove archived originals
                for (checkIn in queues) {
                    FileUtils.delete(checkIn[4])
                    app!!.apiCheckInDb.dbQueued.deleteSingleRowByAudioAttachmentId(checkIn[1])
                }
                for (checkIn in stashes) {
                    FileUtils.delete(checkIn[4])
                    app!!.apiCheckInDb.dbStashed.deleteSingleRowByAudioAttachmentId(checkIn[1])
                }
                FileUtils.delete(archiveWorkDir)
                FileUtils.delete(archiveTarFilePath)
                Log.d(
                    logTag,
                    (queues + stashes).size.toString() + " CheckIns have been deleted from stash."
                )
            } catch (e: Exception) {
                RfcxLog.logExc(logTag, e)
            }
        }
    }

    private fun archive(files: List<Array<String>>) {
        Log.i(logTag, "Preparing CheckIn Archive Process...")
        Log.i(
            logTag,
            "Archiving " + files.size + " Stashed CheckIns."
        )

        // Create Archive File List
        val archiveFileList: MutableList<String> = ArrayList()
        val tsvRows = StringBuilder()
        tsvRows.append(TextUtils.join("\t", tsvMetaColumns)).append("\n")
        var oldestCheckInTimestamp = System.currentTimeMillis()
        var newestCheckInTimestamp: Long = 0
        for (checkIn in files) {

            // Create TSV contents row
            val audioJson = JSONObject(checkIn[2])
            val audioMeta = audioJson.getString("audio").split("*").toTypedArray()
            val measuredAt = audioMeta[1].toLong()
            val audioDuration = audioMeta[10].toInt()
            val sampleRate = audioMeta[4].toInt()
            val archivedAudioFileName = RfcxAudioFileUtils.getAudioFileName(
                rfcxDeviceId, measuredAt,
                audioMeta[2], audioDuration, sampleRate
            )
            val archivedAudioTmpFilePath =
                archiveWorkDir.toString() + "/audio/" + archivedAudioFileName
            val tsvRow = (("" /* measured_at */
                    + metaDateTimeFormat.format(Date(measuredAt))) + "\t" /* queued_at */
                    + metaDateTimeFormat.format(
                Date(
                    audioJson.getString("queued_at").toLong()
                )
            ).toString() + "\t" + archivedAudioFileName.toString() + "\t" /* format */
                    + audioMeta[2] + "\t" /* sha1checksum */
                    + audioMeta[3] + "\t" /* samplerate */
                    + sampleRate.toString() + "\t" /* bitrate */
                    + audioMeta[5] + "\t" /* encode_duration */
                    + audioMeta[8] + "\n")

            // UnGZip audio files into position
            FileUtils.gUnZipFile(checkIn[4], archivedAudioTmpFilePath)
            if (FileUtils.exists(archivedAudioTmpFilePath)) {
                FileUtils.chmod(archivedAudioTmpFilePath, "rw", "rw")
                tsvRows.append(tsvRow)
                archiveFileList.add(archivedAudioTmpFilePath)
            }
            if (measuredAt < oldestCheckInTimestamp) {
                oldestCheckInTimestamp = measuredAt
            }
            if (measuredAt > newestCheckInTimestamp) {
                newestCheckInTimestamp = measuredAt
            }
        }
        StringUtils.saveStringToFile(
            tsvRows.toString(),
            archiveWorkDir.toString() + "/_metadata_audio.tsv"
        )
        archiveFileList.add(archiveWorkDir.toString() + "/_metadata_audio.tsv")
        FileUtils.chmod(archiveWorkDir.toString() + "/_metadata_audio.tsv", "rw", "rw")
        Log.i(
            logTag,
            "Creating CheckIn Archive: $archiveTitle"
        )
        FileUtils.createTarArchiveFromFileList(archiveFileList, archiveTarFilePath)
        FileUtils.chmod(archiveTarFilePath, "rw", "rw")
        val archiveFileSize = FileUtils.getFileSizeInBytes(archiveTarFilePath)
        if (DeviceStorage.isExternalStorageWritable()) {
            Log.i(
                logTag,
                "Transferring CheckIn Archive (" + FileUtils.bytesAsReadableString(
                    archiveFileSize
                ) + ") to External Storage: " + archiveFinalFilePath
            )
            FileUtils.copy(archiveTarFilePath, archiveFinalFilePath)
            FileUtils.chmod(archiveFinalFilePath, "rw", "rw")
            val isSkipping =
                app!!.rfcxPrefs.getPrefAsBoolean(RfcxPrefs.Pref.ENABLE_CUTOFFS_SAMPLING_RATIO)
            var skipping = 0
            if (isSkipping) {
                val ratio =
                    app!!.rfcxPrefs.getPrefAsString(RfcxPrefs.Pref.AUDIO_SAMPLING_RATIO)
                skipping = ratio.split(":").toTypedArray()[1].toInt()
            }
            app!!.apiCheckInArchiveDb.dbArchive.insert(
                Date(archiveTimestamp!!),  // archived_at
                Date(oldestCheckInTimestamp),  // archive_begins_at
                Date(newestCheckInTimestamp),  // archive_ends_at
                files.size,  // record_count
                app!!.rfcxPrefs.getPrefAsInt(RfcxPrefs.Pref.AUDIO_CYCLE_DURATION),  // duration per file
                skipping,  // skipping amount
                archiveFileSize,  // filesize in bytes
                archiveFinalFilePath // filepath
            )
            Log.i(
                logTag, "CheckIn Archive Job Complete: "
                        + files.size + " audio files, "
                        + FileUtils.bytesAsReadableString(archiveFileSize) + ", "
                        + archiveFinalFilePath
            )
        }
    }

    private fun setAndInitializeCheckInArchiveDirectories() {
        archiveTimestamp = System.currentTimeMillis();
        archiveTitle =
            "archive_" + rfcxDeviceId + "_" + fileDateTimeFormat.format(
                Date(archiveTimestamp!!)
            )
        archiveWorkDir = context.filesDir.toString() + "/archive/" + archiveTitle
        archiveTar = "archive/$archiveTitle.tar"
        archiveTarFilePath = context.filesDir.toString() + "/" + archiveTar
        archiveSdCardDir = Environment.getExternalStorageDirectory()
            .toString() + "/rfcx/archive/audio/" + dirDateFormat.format(
            Date(archiveTimestamp!!)
        )
        archiveFinalFilePath = "$archiveSdCardDir/$archiveTitle.tar"
        FileUtils.initializeDirectoryRecursively(archiveSdCardDir, true)
        FileUtils.initializeDirectoryRecursively("$archiveWorkDir/audio", false)
    }

    fun cleanupTempArchiveDir() {
        setAndInitializeCheckInArchiveDirectories() // best to run this before AND after the cleanup

        val archiveAppFilesDir = context.filesDir.toString() + "/archive"
        for (fileToRemove in FileUtils.getDirectoryContents(archiveAppFilesDir, true)) {
            FileUtils.delete(fileToRemove)
        }
        for (fileToRemove in FileUtils.getEmptyDirectories(archiveAppFilesDir)) {
            FileUtils.delete(fileToRemove)
        }
        FileUtils.deleteDirectoryContents(archiveAppFilesDir)

        setAndInitializeCheckInArchiveDirectories() // best to run this before AND after the cleanup

    }
}
