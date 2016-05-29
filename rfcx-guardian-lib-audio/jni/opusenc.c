#include <stdio.h>
#include <stdlib.h>
#include <jni.h>
#include <android/log.h>

#include "opus-1.1.1/include/opus.h"

#define LOG_TAG "OPUS ENCODER"
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

#include <string.h>
#include <math.h>

# define I64FORMAT "lld"
# define fopen_utf8(_x,_y) fopen((_x),(_y))

#include <opus.h>
#include <opus_multistream.h>
#include <ogg/ogg.h>
#include "wav_io.h"

#include "opus_header.h"
#include "opusenc.h"

void save_range(FILE *frange, int frame_size, unsigned char *packet, int nbBytes, opus_uint32 *rngs, int nb_streams);

static int comment_init(char **comments, int* length, const char *vendor_string);
static int comment_pad(char **comments, int* length, int amount);

// Write an Ogg page to a file pointer
static inline int oe_write_page(ogg_page *page, FILE *fp)
{
	int written;
	written=fwrite(page->header,1,page->header_len, fp);
	written+=fwrite(page->body,1,page->body_len, fp);
	return written;
}

#define MAX_FRAME_BYTES 61295
#define IMIN(a,b) ((a) < (b) ? (a) : (b))   /**< Minimum int value.   */
#define IMAX(a,b) ((a) > (b) ? (a) : (b))   /**< Maximum int value.   */

#define PACKAGE_NAME "RFCX transcoder"
#define PACKAGE_VERSION "0.1.3"

int convert( const char* source_path, const char* target_path, int bitRate, int quality);

JNIEXPORT jint JNICALL Java_org_rfcx_guardian_audio_opus_OpusAudioRecorder_encodeOpusFile(JNIEnv *env,
		jobject jobj, jstring in_source_path, jstring in_target_path,
		jint in_brate, jint in_quality) {
	const char *source_path, *target_path;
	source_path = (*env)->GetStringUTFChars(env, in_source_path, NULL);
	target_path = (*env)->GetStringUTFChars(env, in_target_path, NULL);

	int result = convert( source_path, target_path, in_brate, in_quality);

	(*env)->ReleaseStringUTFChars( env, in_source_path, source_path );
	(*env)->ReleaseStringUTFChars( env, in_target_path, target_path );

	return result;
}

int convert( const char* source_path, const char* target_path, int bitRate, int quality) {
	static const input_format raw_format = {NULL, 0, raw_open, wav_close, "raw",N_("RAW file reader")};
	int i, ret;
	int                cline_size;
	OpusMSEncoder      *st;
	const char         *opus_version;
	unsigned char      *packet;
#ifdef FIXED_POINT
	opus_int16         *input;
#else
	float              *input;
#endif
	/*I/O*/
	oe_enc_opt         inopt;
	const input_format *in_format;
	char               *range_file;
	FILE               *fin;
	FILE               *fout;
	FILE               *frange;
	ogg_stream_state   os;
	ogg_page           og;
	ogg_packet         op;
	ogg_int64_t        last_granulepos=0;
	ogg_int64_t        enc_granulepos=0;
	ogg_int64_t        original_samples=0;
	ogg_int32_t        id=-1;
	int                last_segments=0;
	int                eos=0;
	OpusHeader         header;
	char               ENCODER_string[1024];
	/*Counters*/
	opus_int64         nb_encoded=0;
	opus_int64         bytes_written=0;
	opus_int64         pages_out=0;
	opus_int64         total_bytes=0;
	opus_int64         total_samples=0;
	opus_int32         nbBytes;
	opus_int32         nb_samples;
	opus_int32         peak_bytes=0;
	opus_int32         min_bytes;
	time_t             start_time;
	time_t             stop_time;
	time_t             last_spin=0;
	int                last_spin_len=0;
	/*Settings*/
	int                quiet=0;
	int                max_frame_bytes;
	opus_int32         bitrate=bitRate * 1000;
	opus_int32         rate=48000;
	opus_int32         coding_rate=48000;
	opus_int32         frame_size=960;
	int                chan=2;
	int                with_hard_cbr=0;
	int                with_cvbr=0;
	int                expect_loss=0;
	int                complexity=10 - quality;	// 10 -- best
	int                downmix=0;
	int                *opt_ctls_ctlval;
	int                opt_ctls=0;
	int                max_ogg_delay=48000; // 48kHz samples
	int                seen_file_icons=0;
	int                comment_padding=512;
	int                serialno;
	opus_int32         lookahead=0;

	opt_ctls_ctlval=NULL;
	frange=NULL;
	range_file=NULL;
	in_format=NULL;
	inopt.channels=chan;
	inopt.rate=coding_rate=rate;
	// 0 dB gain is recommended unless you know what you're doing
	inopt.gain=0;
	inopt.samplesize=16;
	inopt.endianness=0;
	inopt.rawmode=0;
	inopt.ignorelength=0;
	inopt.copy_comments=1;

	start_time = time(NULL);
	srand(((getpid()&65535)<<15)^start_time);
	serialno=rand();

	opus_version= "libopus 1.1.1";	//opus_get_version_string();
	// Vendor string should just be the encoder library, the ENCODER comment specifies the tool used.
	if( comment_init(&inopt.comments, &inopt.comments_length, opus_version) ) {
		return 1;	// failed
	}
	snprintf(ENCODER_string, sizeof(ENCODER_string), "%s %s",PACKAGE_NAME,PACKAGE_VERSION);
	if( comment_add(&inopt.comments, &inopt.comments_length, "ENCODER", ENCODER_string) ) {
		return 1;	// failed
	}

	if(cline_size>0){
		if( comment_add(&inopt.comments, &inopt.comments_length, "ENCODER_OPTIONS", ENCODER_string) ) {
			return 1;	// failed
		}
	}

	fin=fopen_utf8(source_path, "rb");
	if(!fin){
		perror(source_path);
		return 1;
	}

	if(inopt.rawmode){
		in_format = &raw_format;
		in_format->open_func(fin, &inopt, NULL, 0);
	}else in_format=open_audio_file(fin,&inopt);

	if(!in_format){
		LOGD("Error parsing input file: %s\n",source_path);
		return 1;
	}

	if(downmix==0&&inopt.channels>2&&bitrate>0&&bitrate<(16000*inopt.channels)){
		if(!quiet)LOGD("Notice: Surround bitrate less than 16kbit/sec/channel, downmixing.\n");
		downmix=inopt.channels>8?1:2;
	}

	if(downmix>0&&downmix<inopt.channels)downmix=setup_downmix(&inopt,downmix);
	else downmix=0;

	rate=inopt.rate;
	chan=inopt.channels;
	inopt.skip=0;

	// In order to code the complete length we'll need to do a little padding
	setup_padder(&inopt,&original_samples);

	if(rate>24000)coding_rate=48000;
	else if(rate>16000)coding_rate=24000;
	else if(rate>12000)coding_rate=16000;
	else if(rate>8000)coding_rate=12000;
	else coding_rate=8000;

	frame_size=frame_size/(48000/coding_rate);

	// Scale the resampler complexity, but only for 48000 output because
    // the near-cutoff behavior matters a lot more at lower rates.
//	if(rate!=coding_rate)setup_resample(&inopt,coding_rate==48000?(complexity+1)/2:5,coding_rate);

	if(rate!=coding_rate&&complexity!=10&&!quiet){
		LOGD("Notice: Using resampling with complexity<10.\n");
		LOGD("Opusenc is fastest with 48, 24, 16, 12, or 8kHz input.\n\n");
	}

	// OggOpus headers // FIXME: broke forcemono
	header.channels=chan;
	header.channel_mapping=header.channels>8?255:chan>2;
	header.input_sample_rate=rate;
	header.gain=inopt.gain;

	// Initialize OPUS encoder
	st=opus_multistream_surround_encoder_create(coding_rate, chan, header.channel_mapping, &header.nb_streams, &header.nb_coupled,
			header.stream_map, OPUS_APPLICATION_AUDIO, &ret);
	if(ret!=OPUS_OK){
		LOGD( "Error cannot create encoder: %s\n", opus_strerror(ret));
		return 1;
	}

	min_bytes=max_frame_bytes=(1275*3+7)*header.nb_streams;
	packet=malloc(sizeof(unsigned char)*max_frame_bytes);
	if(packet==NULL){
		LOGD("Error allocating packet buffer.\n");
		return 1;
	}

	if(bitrate<0){
		// Lower default rate for sampling rates [8000-44100) by a factor of (rate+16k)/(64k)
		bitrate=((64000*header.nb_streams+32000*header.nb_coupled)*
				(IMIN(48,IMAX(8,((rate<44100?rate:48000)+1000)/1000))+16)+32)>>6;
	}

	if(bitrate>(1024000*chan)||bitrate<500){
		LOGD("Error: Bitrate %d bits/sec is insane.\nDid you mistake bits for kilobits?\n",bitrate);
		LOGD("--bitrate values from 6-256 kbit/sec per channel are meaningful.\n");
		return 1;
	}
	bitrate=IMIN(chan*256000,bitrate);

	ret=opus_multistream_encoder_ctl(st, OPUS_SET_BITRATE(bitrate));
	if(ret!=OPUS_OK){
		LOGD("Error OPUS_SET_BITRATE returned: %s\n",opus_strerror(ret));
		return 1;
	}

	ret=opus_multistream_encoder_ctl(st, OPUS_SET_VBR(!with_hard_cbr));
	if(ret!=OPUS_OK){
		LOGD("Error OPUS_SET_VBR returned: %s\n",opus_strerror(ret));
		return 1;
	}

	if(!with_hard_cbr){
		ret=opus_multistream_encoder_ctl(st, OPUS_SET_VBR_CONSTRAINT(with_cvbr));
		if(ret!=OPUS_OK){
			LOGD("Error OPUS_SET_VBR_CONSTRAINT returned: %s\n",opus_strerror(ret));
			return 1;
		}
	}

	ret=opus_multistream_encoder_ctl(st, OPUS_SET_COMPLEXITY(complexity));
	if(ret!=OPUS_OK){
		LOGD("Error OPUS_SET_COMPLEXITY returned: %s\n",opus_strerror(ret));
		return 1;
	}

	ret=opus_multistream_encoder_ctl(st, OPUS_SET_PACKET_LOSS_PERC(expect_loss));
	if(ret!=OPUS_OK){
		LOGD("Error OPUS_SET_PACKET_LOSS_PERC returned: %s\n",opus_strerror(ret));
		return 1;
	}

#ifdef OPUS_SET_LSB_DEPTH
	ret=opus_multistream_encoder_ctl(st, OPUS_SET_LSB_DEPTH(IMAX(8,IMIN(24,inopt.samplesize))));
	if(ret!=OPUS_OK){
		LOGD("Warning OPUS_SET_LSB_DEPTH returned: %s\n",opus_strerror(ret));
	}
#endif

	// This should be the last set of CTLs, except the lookahead get, so it can override the defaults.
	for(i=0;i<opt_ctls;i++){
		int target=opt_ctls_ctlval[i*3];
		if(target==-1){
			ret=opus_multistream_encoder_ctl(st,opt_ctls_ctlval[i*3+1],opt_ctls_ctlval[i*3+2]);
			if(ret!=OPUS_OK){
				LOGD("Error opus_multistream_encoder_ctl(st,%d,%d) returned: %s\n",opt_ctls_ctlval[i*3+1],opt_ctls_ctlval[i*3+2],opus_strerror(ret));
				return 1;
			}
		}else if(target<header.nb_streams){
			OpusEncoder *oe;
			opus_multistream_encoder_ctl(st,OPUS_MULTISTREAM_GET_ENCODER_STATE(i,&oe));
			ret=opus_encoder_ctl(oe, opt_ctls_ctlval[i*3+1],opt_ctls_ctlval[i*3+2]);
			if(ret!=OPUS_OK){
				LOGD("Error opus_encoder_ctl(st[%d],%d,%d) returned: %s\n",target,opt_ctls_ctlval[i*3+1],opt_ctls_ctlval[i*3+2],opus_strerror(ret));
				return 1;
			}
		}else{
			LOGD("Error --set-ctl-int target stream %d is higher than the maximum stream number %d.\n",target,header.nb_streams-1);
			return 1;
		}
	}

	// We do the lookahead check late so user CTLs can change it
	ret=opus_multistream_encoder_ctl(st, OPUS_GET_LOOKAHEAD(&lookahead));
	if(ret!=OPUS_OK){
		LOGD("Error OPUS_GET_LOOKAHEAD returned: %s\n",opus_strerror(ret));
		return 1;
	}
	inopt.skip+=lookahead;
	// Regardless of the rate we're coding at the ogg timestamping/skip is always timed at 48000.
	header.preskip=inopt.skip*(48000./coding_rate);
	// Extra samples that need to be read to compensate for the pre-skip
	inopt.extraout=(int)header.preskip*(rate/48000.);

	if(!quiet){
		int opus_app;
		LOGD("Encoding using %s",opus_version);
		opus_multistream_encoder_ctl(st,OPUS_GET_APPLICATION(&opus_app));
//		if(opus_app==OPUS_APPLICATION_VOIP)LOGD(" (VoIP)\n");
//		else if(opus_app==OPUS_APPLICATION_AUDIO)LOGD(" (audio)\n");
//		else if(opus_app==OPUS_APPLICATION_RESTRICTED_LOWDELAY)LOGD(" (low-delay)\n");
//		else LOGD(" (unknown)\n");
		LOGD("-----------------------------------------------------\n");
		LOGD("   Input: %0.6gkHz %d channel%s\n",
				header.input_sample_rate/1000.,chan,chan<2?"":"s");
		LOGD("  Output: %d channel%s (",header.channels,header.channels<2?"":"s");
		if(header.nb_coupled>0)LOGD("%d coupled",header.nb_coupled*2);
		if(header.nb_streams-header.nb_coupled>0)LOGD(
				"%s%d uncoupled",header.nb_coupled>0?", ":"",
						header.nb_streams-header.nb_coupled);
		LOGD(")\n          %0.2gms packets, %0.6gkbit/sec%s\n",
				frame_size/(coding_rate/1000.), bitrate/1000.,
				with_hard_cbr?" CBR":with_cvbr?" CVBR":" VBR");
		LOGD(" Preskip: %d\n",header.preskip);

		if(frange!=NULL)LOGD("         Writing final range file %s\n",range_file);
		LOGD("\n");
	}

	fout=fopen_utf8(target_path, "wb");
	if(!fout){
		perror(target_path);
		return 1;
	}

	// Initialize Ogg stream struct
	if(ogg_stream_init(&os, serialno)==-1){
		LOGD("Error: stream init failed\n");
		return 1;
	}

	// Write header
	{
		unsigned char header_data[100];
		int packet_size=opus_header_to_packet(&header, header_data, 100);
		op.packet=header_data;
		op.bytes=packet_size;
		op.b_o_s=1;
		op.e_o_s=0;
		op.granulepos=0;
		op.packetno=0;
		ogg_stream_packetin(&os, &op);

		while((ret=ogg_stream_flush(&os, &og))){
			if(!ret)break;
			ret=oe_write_page(&og, fout);
			if(ret!=og.header_len+og.body_len){
				LOGD("Error: failed writing header to output stream\n");
				return 1;
			}
			bytes_written+=ret;
			pages_out++;
		}

		if( comment_pad(&inopt.comments, &inopt.comments_length, comment_padding) ) {
			return 1;	// failed
		}
		op.packet=(unsigned char *)inopt.comments;
		op.bytes=inopt.comments_length;
		op.b_o_s=0;
		op.e_o_s=0;
		op.granulepos=0;
		op.packetno=1;
		ogg_stream_packetin(&os, &op);
	}

	// writing the rest of the opus header packets
	while((ret=ogg_stream_flush(&os, &og))){
		if(!ret)break;
		ret=oe_write_page(&og, fout);
		if(ret!=og.header_len + og.body_len){
			LOGD("Error: failed writing header to output stream\n");
			return 1;
		}
		bytes_written+=ret;
		pages_out++;
	}

	free(inopt.comments);

#ifdef FIXED_POINT
	input=malloc(sizeof(opus_int16)*frame_size*chan);
#else
	input=malloc(sizeof(float)*frame_size*chan);
#endif
	if(input==NULL){
		LOGD("Error: couldn't allocate sample buffer.\n");
		return 1;
	}

	// Main encoding loop (one frame per iteration)
	eos=0;
	nb_samples=-1;
	while(!op.e_o_s){
		int size_segments,cur_frame_size;
		id++;

		if(nb_samples<0){
			nb_samples = inopt.read_samples(inopt.readdata,input,frame_size);
			total_samples+=nb_samples;
			if(nb_samples<frame_size)op.e_o_s=1;
			else op.e_o_s=0;
		}
		op.e_o_s|=eos;

		if(start_time==0){
			start_time = time(NULL);
		}

		cur_frame_size=frame_size;

		// No fancy end padding, just fill with zeros for now.
		if(nb_samples<cur_frame_size)for(i=nb_samples*chan;i<cur_frame_size*chan;i++)input[i]=0;

		// Encode current frame
#ifdef FIXED_POINT
		nbBytes=opus_multistream_encode(st, input, cur_frame_size, packet, max_frame_bytes);
#else
		nbBytes=opus_multistream_encode_float(st, input, cur_frame_size, packet, max_frame_bytes);
#endif
		if(nbBytes<0){
			LOGD( "Encoding failed: %s. Aborting.\n", opus_strerror(nbBytes));
			break;
		}
		nb_encoded+=cur_frame_size;
		enc_granulepos+=cur_frame_size*48000/coding_rate;
		total_bytes+=nbBytes;
		size_segments=(nbBytes+255)/255;
		peak_bytes=IMAX(nbBytes,peak_bytes);
		min_bytes=IMIN(nbBytes,min_bytes);

		if(frange!=NULL){
			opus_uint32 rngs[256];
			OpusEncoder *oe;
			for(i=0;i<header.nb_streams;i++){
				ret=opus_multistream_encoder_ctl(st,OPUS_MULTISTREAM_GET_ENCODER_STATE(i,&oe));
				ret=opus_encoder_ctl(oe,OPUS_GET_FINAL_RANGE(&rngs[i]));
			}
			save_range(frange,cur_frame_size*(48000/coding_rate),packet,nbBytes,
					rngs,header.nb_streams);
		}

		// Flush early if adding this packet would make us end up with a
		// continued page which we wouldn't have otherwise.
		while((((size_segments<=255)&&(last_segments+size_segments>255))||
				(enc_granulepos-last_granulepos>max_ogg_delay))&&
#ifdef OLD_LIBOGG
				ogg_stream_flush(&os, &og)){
#else
			ogg_stream_flush_fill(&os, &og,255*255)){
#endif
			if(ogg_page_packets(&og)!=0)last_granulepos=ogg_page_granulepos(&og);
			last_segments-=og.header[26];
			ret=oe_write_page(&og, fout);
			if(ret!=og.header_len+og.body_len){
				LOGD("Error: failed writing data to output stream\n");
				return 1;
			}
			bytes_written+=ret;
			pages_out++;
		}

		// The downside of early reading is if the input is an exact
		// multiple of the frame_size you'll get an extra frame that needs
		// to get cropped off. The downside of late reading is added delay.
		// If your ogg_delay is 120ms or less we'll assume you want the
		// low delay behavior.
		if((!op.e_o_s)&&max_ogg_delay>5760){
			nb_samples = inopt.read_samples(inopt.readdata,input,frame_size);
			total_samples+=nb_samples;
			if(nb_samples<frame_size)eos=1;
			if(nb_samples==0)op.e_o_s=1;
		} else nb_samples=-1;

		op.packet=(unsigned char *)packet;
		op.bytes=nbBytes;
		op.b_o_s=0;
		op.granulepos=enc_granulepos;
		if(op.e_o_s){
			// We compute the final GP as ceil(len*48k/input_rate). When a resampling
			// decoder does the matching floor(len*input/48k) conversion the length will
			// be exactly the same as the input.
			op.granulepos=((original_samples*48000+rate-1)/rate)+header.preskip;
		}
		op.packetno=2+id;
		ogg_stream_packetin(&os, &op);
		last_segments+=size_segments;

		// If the stream is over or we're sure that the delayed flush will fire,
		// go ahead and flush now to avoid adding delay.*/
		while((op.e_o_s||(enc_granulepos+(frame_size*48000/coding_rate)-last_granulepos>max_ogg_delay)||
				(last_segments>=255))?
#ifdef OLD_LIBOGG
					// Libogg > 1.2.2 allows us to achieve lower overhead by producing larger pages.
					// For 20ms frames this is only relevant above ~32kbit/sec.
					ogg_stream_flush(&os, &og):
					ogg_stream_pageout(&os, &og)){
#else
					ogg_stream_flush_fill(&os, &og,255*255):
					ogg_stream_pageout_fill(&os, &og,255*255)){
#endif
			if(ogg_page_packets(&og)!=0)last_granulepos=ogg_page_granulepos(&og);
			last_segments-=og.header[26];
			ret=oe_write_page(&og, fout);
			if(ret!=og.header_len+og.body_len){
				LOGD("Error: failed writing data to output stream\n");
				return 1;
			}
			bytes_written+=ret;
			pages_out++;
		}

		if(!quiet && 0){	// skip this
			stop_time = time(NULL);
			if(stop_time>last_spin){
				double estbitrate;
				double coded_seconds=nb_encoded/(double)coding_rate;
				double wall_time=(stop_time-start_time)+1e-6;
				char sbuf[55];
				static const char spinner[]="|/-\\";
				if(!with_hard_cbr){
					double tweight=1./(1+exp(-((coded_seconds/10.)-3.)));
					estbitrate=(total_bytes*8.0/coded_seconds)*tweight+
							bitrate*(1.-tweight);
				}else estbitrate=nbBytes*8*((double)coding_rate/frame_size);
				LOGD("\r");
				for(i=0;i<last_spin_len;i++)LOGD(" ");
				if(inopt.total_samples_per_channel>0 && inopt.total_samples_per_channel<nb_encoded){
					snprintf(sbuf,54,"\r[%c] %02d%% ",spinner[last_spin&3],
							(int)floor(nb_encoded/(double)(inopt.total_samples_per_channel+inopt.skip)*100.));
				}else{
					snprintf(sbuf,54,"\r[%c] ",spinner[last_spin&3]);
				}
				last_spin_len=strlen(sbuf);
				snprintf(sbuf+last_spin_len,54-last_spin_len,
						"%02d:%02d:%02d.%02d %4.3gx realtime, %5.4gkbit/s",
						(int)(coded_seconds/3600),(int)(coded_seconds/60)%60,
						(int)(coded_seconds)%60,(int)(coded_seconds*100)%100,
						coded_seconds/wall_time,
						estbitrate/1000.);
				LOGD("%s",sbuf);
				fflush(stderr);
				last_spin_len=strlen(sbuf);
				last_spin=stop_time;
			}
		}
	}
	stop_time = time(NULL);

	for(i=0;i<last_spin_len;i++)LOGD(" ");
	if(last_spin_len)LOGD("\r");

	if(!quiet){
		double coded_seconds=nb_encoded/(double)coding_rate;
		double wall_time=(stop_time-start_time)+1e-6;
		LOGD("Encoding complete                                    \n");
		LOGD("-----------------------------------------------------\n");
		LOGD("       Encoded: %.2f sec", coded_seconds);
		//print_time(coded_seconds);
		//LOGD("\n       Runtime:");
		//print_time(wall_time);
		LOGD("\n                (%0.4gx realtime)\n",coded_seconds/wall_time);
		LOGD("         Wrote: %" I64FORMAT " bytes, %d packets, %" I64FORMAT " pages\n",bytes_written,id+1,pages_out);
		LOGD("       Bitrate: %0.6gkbit/s (without overhead)\n",
				total_bytes*8.0/(coded_seconds)/1000.0);
		LOGD(" Instant rates: %0.6gkbit/s to %0.6gkbit/s\n                (%d to %d bytes per packet)\n",
				min_bytes*8*((double)coding_rate/frame_size/1000.),
				peak_bytes*8*((double)coding_rate/frame_size/1000.),min_bytes,peak_bytes);
		LOGD("      Overhead: %0.3g%% (container+metadata)\n",(bytes_written-total_bytes)/(double)bytes_written*100.);
#ifdef OLD_LIBOGG
		if(max_ogg_delay>(frame_size*(48000/coding_rate)*4))LOGD("    (use libogg 1.3 or later for lower overhead)\n");
#endif
		LOGD("\n");
	}

	opus_multistream_encoder_destroy(st);
	ogg_stream_clear(&os);
	free(packet);
	free(input);
	if(opt_ctls)free(opt_ctls_ctlval);

//	if(rate!=coding_rate)clear_resample(&inopt);
	clear_padder(&inopt);
	if(downmix)clear_downmix(&inopt);
	in_format->close_func(inopt.readdata);
	if(fin)fclose(fin);
	if(fout)fclose(fout);
	if(frange)fclose(frange);

	return 0;
}

/*
 Comments will be stored in the Vorbis style.
 It is describled in the "Structure" section of
    http://www.xiph.org/ogg/vorbis/doc/v-comment.html

 However, Opus and other non-vorbis formats omit the "framing_bit".

The comment header is decoded as follows:
  1) [vendor_length] = read an unsigned integer of 32 bits
  2) [vendor_string] = read a UTF-8 vector as [vendor_length] octets
  3) [user_comment_list_length] = read an unsigned integer of 32 bits
  4) iterate [user_comment_list_length] times {
     5) [length] = read an unsigned integer of 32 bits
     6) this iteration's user comment = read a UTF-8 vector as [length] octets
     }
  7) done.
*/

#define readint(buf, base) (((buf[base+3]<<24)&0xff000000)| \
		((buf[base+2]<<16)&0xff0000)| \
		((buf[base+1]<<8)&0xff00)| \
		(buf[base]&0xff))
#define writeint(buf, base, val) do{ buf[base+3]=((val)>>24)&0xff; \
		buf[base+2]=((val)>>16)&0xff; \
		buf[base+1]=((val)>>8)&0xff; \
		buf[base]=(val)&0xff; \
}while(0)

static int comment_init(char **comments, int* length, const char *vendor_string)
{
	// The 'vendor' field should be the actual encoding library used.
	int vendor_length=strlen(vendor_string);
	int user_comment_list_length=0;
	int len=8+4+vendor_length+4;
	char *p=(char*)malloc(len);
	if(p==NULL){
		LOGD( "malloc failed in comment_init()\n");
		return 1;
	}
	memcpy(p, "OpusTags", 8);
	writeint(p, 8, vendor_length);
	memcpy(p+12, vendor_string, vendor_length);
	writeint(p, 12+vendor_length, user_comment_list_length);
	*length=len;
	*comments=p;

	return 0;
}

int comment_add(char **comments, int* length, char *tag, char *val)
{
	char* p=*comments;
	int vendor_length=readint(p, 8);
	int user_comment_list_length=readint(p, 8+4+vendor_length);
	int tag_len=(tag?strlen(tag)+1:0);
	int val_len=strlen(val);
	int len=(*length)+4+tag_len+val_len;

	p=(char*)realloc(p, len);
	if(p==NULL){
		LOGD( "realloc failed in comment_add()\n");
		return 1;
	}

	writeint(p, *length, tag_len+val_len);		// length of comment
	if(tag){
		memcpy(p+*length+4, tag, tag_len);		// comment tag
		(p+*length+4)[tag_len-1] = '=';			// separator
	}
	memcpy(p+*length+4+tag_len, val, val_len);	// comment
	writeint(p, 8+4+vendor_length, user_comment_list_length+1);
	*comments=p;
	*length=len;

	return 0;
}

static int comment_pad(char **comments, int* length, int amount)
{
	if(amount>0){
		int i;
		int newlen;
		char* p=*comments;
		// Make sure there is at least amount worth of padding free, and
		// round up to the maximum that fits in the current ogg segments.
		newlen=(*length+amount+255)/255*255-1;
		p=realloc(p,newlen);
		if(p==NULL){
			LOGD("realloc failed in comment_pad()\n");
			return 1;
		}
		for(i=*length;i<newlen;i++)p[i]=0;
		*comments=p;
		*length=newlen;
	}
	return 0;
}
#undef readint
#undef writeint
