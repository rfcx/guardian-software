#include <stdio.h>
#include <stdlib.h>
#include <jni.h>
#include <android/log.h> 
#include "libmp3lame/lame.h"

#define LOG_TAG "LAME ENCODER"
#define LOGD(format, args...)  __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, format, ##args);
#define BUFFER_SIZE 8192
#define be_short(s) ((short) ((unsigned short) (s) << 8) | ((unsigned short) (s) >> 8))

struct WAV_HEADER
{
char                RIFF[4];
int                 ChunkSize;
char                WAVE[4];
char                fmt[4];
int                 Subchunk1Size;
short int           AudioFormat;
short int           NumOfChan;
int                 SamplesPerSec;
int                 bytesPerSec;
short int           blockAlign;
short int           bitsPerSample;
int                 Subchunk2Size;
char                Subchunk2ID[4];
} wav_header;

lame_t lame;

int read_samples(FILE *input_file, short *input) {
	int nb_read;
	nb_read = fread(input, 1, sizeof(short), input_file) / sizeof(short);
/*
	int i = 0;
	while (i < nb_read) {
		input[i] = be_short(input[i]);	// don't swap bytes when using WAV on ARM
		i++;
	}
*/
	return nb_read;
}

JNIEXPORT jint JNICALL Java_org_rfcx_guardian_audio_mp3_Mp3AudioEncoder_encodeFile(JNIEnv *env,
		jobject jobj, jstring in_source_path, jstring in_target_path,
		jint in_brate, jint in_quality) {
	const char *source_path, *target_path;
	source_path = (*env)->GetStringUTFChars(env, in_source_path, NULL);
	target_path = (*env)->GetStringUTFChars(env, in_target_path, NULL);

	FILE *input_file, *output_file;
	input_file = fopen(source_path, "rb");
	output_file = fopen(target_path, "wb");

	short input[BUFFER_SIZE];
	char output[BUFFER_SIZE];
	int nb_read = 0;
	int nb_write = 0;
	int nb_total = 0;

	fread(&wav_header, 1, sizeof(wav_header), input_file);

	lame = lame_init();

	LOGD("Init parameters:");
	lame_set_num_channels(lame, wav_header.NumOfChan);
	LOGD("Number of channels: %d", wav_header.NumOfChan);
	lame_set_in_samplerate(lame, wav_header.SamplesPerSec);
	LOGD("Sample rate: %d", wav_header.SamplesPerSec);
	lame_set_brate(lame, in_brate);
	LOGD("Bitrate: %d", in_brate);
	lame_set_mode(lame, wav_header.NumOfChan == 1 ? 3 : 1);
	LOGD("Mode: %d", wav_header.NumOfChan == 1 ? 3 : 1);
	lame_set_quality(lame, in_quality);
	LOGD("Quality: %d", in_quality);

	int res = lame_init_params(lame);
	LOGD("Init returned: %d", res);

	LOGD("Encoding started");
	while (nb_read = read_samples(input_file, input)) {
		nb_write = lame_encode_buffer(lame, input, input, nb_read, output,
				BUFFER_SIZE);
		fwrite(output, nb_write, 1, output_file);
		nb_total += nb_write;
	}
	LOGD("Encoded %d bytes", nb_total);

	nb_write = lame_encode_flush(lame, output, BUFFER_SIZE);
	fwrite(output, nb_write, 1, output_file);
	LOGD("Flushed %d bytes", nb_write);

	fclose(input_file);
	fclose(output_file);

	return 0;
}
