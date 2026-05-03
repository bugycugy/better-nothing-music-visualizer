package com.better.nothing.music.vizualizer;

import org.jtransforms.fft.DoubleFFT_1D;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Handles audio capture, FFT processing, and frequency analysis.
 */
public class AudioProcessor {

    private static final int SAMPLE_RATE = 44100;
    private static final int FPS = 60;
    private static final int HOP = Math.round(SAMPLE_RATE / (float) FPS);
    private static final float SPECTRUM_GAIN = 4f;
    private static final float SPECTRUM_LEAKAGE_FLOOR_RATIO = 0.12f;
    private static final float EPSILON = 0.000001f;
    private static final float PEAK_FALLOFF = 0.9995f;

    private int fftSize;
    private int analysisWindow;
    private float hzPerBin;

    private float[] ring;
    private int ringPosition = 0;
    private int filled = 0;

    public AudioProcessor() {
        updateFFTSize(0); // Default
    }

    public float getHzPerBin() {
        return hzPerBin;
    }

    public AudioFrameResult processAudioFrame(short[] hopBuffer, VisualizerConfig config, FrequencyRange hapticRange) {
        // Fill ring buffer
        for (int i = 0; i < hopBuffer.length; i++) {
            ring[ringPosition] = hopBuffer[i] / 32768f;
            ringPosition = (ringPosition + 1) % analysisWindow;
        }
        filled = Math.min(filled + hopBuffer.length, analysisWindow);

        if (filled < analysisWindow) {
            return null; // Not enough data yet
        }

        // Process FFT
        Arrays.fill(fftData, 0d);
        for (int i = 0; i < analysisWindow; i++) {
            fftData[2 * i] = ring[(ringPosition + i) % analysisWindow] * hann[i];
        }

        fft.realForwardFull(fftData);
        for (int i = 0; i <= fftSize / 2; i++) {
            double re = fftData[2 * i];
            double im = fftData[2 * i + 1];
            magnitude[i] = (float) Math.hypot(re, im);
        }

        // Compute peaks
        float[] uniquePeaks = computeUniquePeaks(config, magnitude);
        float hapticPeak = hapticRange != null ? computeRangePeak(hapticRange, magnitude) : 0f;

        return new AudioFrameResult(uniquePeaks, hapticPeak);
    }

    private float[] computeUniquePeaks(VisualizerConfig config, float[] magnitude) {
        if (config == null) return new float[0];
        float[] uniquePeaks = new float[config.uniqueRanges.length];
        float dominantPeak = 0f;
        for (int i = 0; i < config.uniqueRanges.length; i++) {
            float peak = computeRangePeak(config.uniqueRanges[i], magnitude);
            uniquePeaks[i] = peak;
            if (peak > dominantPeak) {
                dominantPeak = peak;
            }
        }

        if (dominantPeak <= EPSILON) {
            return uniquePeaks;
        }

        float leakageFloor = dominantPeak * SPECTRUM_LEAKAGE_FLOOR_RATIO;
        for (int i = 0; i < uniquePeaks.length; i++) {
            uniquePeaks[i] = Math.max(0f, uniquePeaks[i] - leakageFloor);
        }
        return uniquePeaks;
    }

    private float computeRangePeak(FrequencyRange range, float[] magnitude) {
        if (range == null || magnitude == null || magnitude.length == 0) {
            return 0f;
        }

        int start = Math.max(0, Math.min(range.binLo, magnitude.length - 1));
        int end = Math.max(start, Math.min(range.binHi, magnitude.length - 1));
        float peak = 0f;
        for (int bin = start; bin <= end; bin++) {
            peak = Math.max(peak, magnitude[bin]);
        }
        return peak;
    }

    private static float[] buildHannWindow(int size) {
        float[] hann = new float[size];
        double denom = Math.max(1d, size - 1d);
        for (int i = 0; i < size; i++) {
            double phase = (2d * Math.PI * i) / denom;
            hann[i] = (float) (0.5d * (1d - Math.cos(phase)));
        }
        return hann;
    }

    // Inner classes for config
    public static final class ZoneSpec {
        final float lowHz;
        final float highHz;
        final float lowPercent;
        final float highPercent;

        ZoneSpec(float lowHz, float highHz, float lowPercent, float highPercent) {
            this.lowHz = lowHz;
            this.highHz = highHz;
            this.lowPercent = lowPercent;
            this.highPercent = highPercent;
        }

        boolean hasPercentSlice() {
            return !Float.isNaN(lowPercent) && !Float.isNaN(highPercent);
        }
    }

    public static final class FrequencyRange {
        final float lowHz;
        final float highHz;
        final int binLo;
        final int binHi;

        FrequencyRange(float lowHz, float highHz, float hzPerBin, int fftSize) {
            this.lowHz = lowHz;
            this.highHz = highHz;
            this.binLo = Math.max(0, (int) Math.ceil(lowHz / hzPerBin));
            this.binHi = Math.max(binLo, Math.min(fftSize / 2, (int) Math.floor(highHz / hzPerBin)));
        }
    }

    public static final class AudioFrameResult {
        public final float[] uniquePeaks;
        public final float hapticPeak;

        public AudioFrameResult(float[] uniquePeaks, float hapticPeak) {
            this.uniquePeaks = uniquePeaks;
            this.hapticPeak = hapticPeak;
        }
    }