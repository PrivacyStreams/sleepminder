package de.sopamo.uni.sleepminder.lib.recorders;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

import com.github.privacystreams.audio.Audio;
import com.github.privacystreams.audio.AudioOperators;
import com.github.privacystreams.core.Callback;
import com.github.privacystreams.core.UQI;
import com.github.privacystreams.core.purposes.Purpose;

import java.util.Arrays;
import java.util.List;

import de.sopamo.uni.sleepminder.lib.DebugView;
import de.sopamo.uni.sleepminder.lib.detection.FeatureExtractor;
import de.sopamo.uni.sleepminder.lib.detection.NoiseModel;

public class AudioRecorder extends Thread {
    private boolean stopped = false;
    private static AudioRecord recorder = null;
    private static int N = 0;
    private NoiseModel noiseModel;
    private DebugView debugView;
    private short[] buffer;
    private FeatureExtractor featureExtractor;
    private Context context;


    public AudioRecorder(Context context, NoiseModel noiseModel, DebugView debugView) {
        this.context = context;
        this.noiseModel = noiseModel;
        this.debugView = debugView;
        this.featureExtractor = new FeatureExtractor(noiseModel);
    }

    @Override
    public void run() {

        capture();

    }

    private void capture() {
        UQI uqi = new UQI(this.context);
        int i = 0;
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);

        uqi.getData(Audio.recordPeriodic(500, 0), Purpose.HEALTH("monitoring sleep"))
                .setField("amplitudeSamples", AudioOperators.getAmplitudeSamples(Audio.AUDIO_DATA))
                .forEach("amplitudeSamples", new Callback<List<Integer>>() {
                    @Override
                    protected void onInput(List<Integer> amplitudeSamples) {
                        short[] amplitudes = new short[amplitudeSamples.size()];
                        for (int i = 0; i < amplitudeSamples.size(); i++) {
                            amplitudes[i] = amplitudeSamples.get(i).shortValue();
                        }
                        process(amplitudes);
                    }
                });
//        if(buffer == null) {
//            buffer  = new short[1600];
//        }
//
//        if(N == 0 || (recorder == null || recorder.getState() != AudioRecord.STATE_INITIALIZED)) {
//            N = AudioRecord.getMinBufferSize(16000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
//            if(N < 1600) {
//                N = 1600;
//            }
//            recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
//                    16000,
//                    AudioFormat.CHANNEL_IN_MONO,
//                    AudioFormat.ENCODING_PCM_16BIT,
//                    N);
//        }
//        recorder.startRecording();
//
        while(!this.stopped) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        uqi.stopAll();
//        recorder.stop();
//        recorder.release();

    }

    private void process(short[] buffer) {

        featureExtractor.update(buffer);

        if(debugView != null) {
            /*debugView.addPoint2(noiseModel.getNormalizedRLH(), noiseModel.getNormalizedVAR());
            debugView.setLux((float) (noiseModel.getNormalizedRMS()));*/
            debugView.addPoint2(noiseModel.getLastRLH(), noiseModel.getNormalizedVAR());
            debugView.setLux((float) (noiseModel.getLastRMS()));
            debugView.post(new Runnable() {
                @Override
                public void run() {
                    debugView.invalidate();
                }
            });

        }

    }


    public void close() {
        stopped = true;
    }

}