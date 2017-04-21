package de.sopamo.uni.sleepminder.lib.detection;

import android.util.Log;

/**
 * Created by paulmohr on 19.06.15.
 */
public class FeatureExtractor {

    private NoiseModel noiseModel;
    private float[] lowFreq;
    private float[] highFreq;

    public FeatureExtractor(NoiseModel noiseModel) {

        this.noiseModel = noiseModel;
    }

    public void update(short[] buffer) {
        lowFreq = new float[buffer.length];
        highFreq = new float[buffer.length];
        noiseModel.addRLH(calculateRLH(buffer));
        noiseModel.addRMS(calculateRMS(buffer));
        noiseModel.addVAR(calculateVar(buffer));

        noiseModel.calculateFrame();
    }


    private double calculateRMS(short[] buffer) {
        double sum = 0;
        for(int i=0;i<buffer.length;i++) {
            sum += buffer[i] * buffer[i] / buffer.length;
        }
        return Math.sqrt(sum);
    }

    private double calculateRMS(float[] buffer) {
        double sum = 0;
        for(int i=0;i<buffer.length;i++) {
            sum += buffer[i] * buffer[i] / buffer.length;
        }
        return Math.sqrt(sum);
    }

    private double calculateLowFreqRMS(short[] buffer) {
        lowFreq[0] = 0;

        float a = 0.25f;

        for(int i=1;i<buffer.length;i++) {
            lowFreq[i] = lowFreq[i-1] + a * (buffer[i] - lowFreq[i-1]);
        }

        return calculateRMS(lowFreq);
    }

    private double calculateHighFreqRMS(short[] buffer) {
        highFreq[0] = 0;

        float a = 0.25f;

        for(int i=1;i<buffer.length;i++) {
            highFreq[i] = a * (highFreq[i-1] + buffer[i] - buffer[i-1]);
        }

        return calculateRMS(highFreq);
    }

    private double calculateRLH(short[] buffer) {
        double rmsh = calculateHighFreqRMS(buffer);
        double rmsl = calculateLowFreqRMS(buffer);
        if(rmsh == 0) return 0;
        if(rmsl == 0) return 0;
        return  rmsl / rmsh;
    }

    /**
     * Calculates the var of one frame
     *
     * @param buffer
     * @return
     */
    private double calculateVar(short[] buffer) {

        double mean = calculateMean(buffer);
        double var = 0;
        for(short s: buffer) {
            var += ((double) s - mean) * ((double) s - mean) / buffer.length;
        }
        return var;
    }

    /**
     * Calculate the mean of one fram
     *
     * @param buffer
     * @return
     */
    private double calculateMean(short[] buffer) {
        double mean = 0;
        for(short s: buffer) {
            mean += s;
        }
        return mean / buffer.length;
    }
}
