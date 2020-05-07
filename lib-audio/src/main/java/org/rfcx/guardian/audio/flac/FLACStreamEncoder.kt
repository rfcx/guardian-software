package org.rfcx.guardian.audio.flac
//
//import java.io.File
//import java.nio.ByteBuffer
//
//class FLACStreamEncoder {
//
//    fun encode(outputFile: File, sampleRate: Int, channels: Int, bitsPerSample: Int, buffer: ByteBuffer, bufferSize: Int): String {
//        val outputFilePath = outputFile.absolutePath
//        init(outputFilePath, sampleRate, channels, bitsPerSample)
//        val result: Int = write(buffer, bufferSize)
//        return if (result == bufferSize){
//            deinit()
//            "OK"
//        } else {
//            deinit()
//            "ERROR"
//        }
//    }
//
//    /*
//    * JNI Implementation
//    */
//
//    var mObject: Long? = null
//
//    //To initialize encoder
//    external fun init(outputFile: String, sampleRate: Int, channels: Int, bitsPerSample: Int)
//
//    //To get rid of encoder
//    external fun deinit()
//
//    //To write the byte of audio
//    external fun write(buffer: ByteBuffer, buffSize: Int): Int
//
//    //To flush encoder
//    external fun flush()
//
//    init {
//        System.loadLibrary("ogg");
//        System.loadLibrary("flac");
//        System.loadLibrary("flacenc");
//    }
//
//}