package com.better.nothing.music.vizualizer;

import java.util.Arrays;

/**
 * Handles glyph state computation, breathing effects, and frame rendering.
 */
public class GlyphRenderer {

    private static final float PEAK_FALLOFF = 0.9995f;
    private static final float SPECTRUM_GAIN = 4f;
    private static final float EPSILON = 0.000001f;
    private static final float SILENCE_THRESHOLD = 0.005f;
    private static final long BREATH_DELAY_MS = 3000L;
    private static final long BREATH_PERIOD_MS = 5000L;
    private static final long FLASH_DURATION_MS = 200L;

    private float mGamma;
    private boolean mIdleBreathingEnabled;
    private final boolean mNotificationFlashEnabled;
    private int mDeviceType = DeviceProfile.DEVICE_UNKNOWN;
    private String mIdlePattern = "pulse";

    private float[] mCurrentLightState = new float[0];
    private float[] mZonePeaks = new float[0];
    private float[] mDecayedFrequencyState = new float[0];
    private int mLastHash = Integer.MIN_VALUE;
    private long mLastSendMs = 0L;
    private long mSilenceStartTimeMs = 0;
    private long mLastNotificationFlashMs = 0;
    private long mLastFrameMs = 0;
    private float mBreathingEnvelope = 0f;

    public GlyphRenderer(float gamma, boolean idleBreathingEnabled, boolean notificationFlashEnabled, int deviceType) {
        this.mGamma = gamma;
        this.mIdleBreathingEnabled = idleBreathingEnabled;
        this.mNotificationFlashEnabled = notificationFlashEnabled;
        this.mDeviceType = deviceType;
    }

    public void setIdleBreathingEnabled(boolean enabled) {
        mIdleBreathingEnabled = enabled;
        if (!enabled) {
            mSilenceStartTimeMs = 0;
        }
    }

    public void setIdlePattern(String pattern) {
        this.mIdlePattern = pattern;
    }

    public void setGamma(float gamma) {
        mGamma = gamma;
        mLastHash = Integer.MIN_VALUE; // Force redraw with new gamma
    }

    public void setDeviceType(int deviceType) {
        this.mDeviceType = deviceType;
        mLastHash = Integer.MIN_VALUE;
    }

    public void resetState(AudioProcessor.VisualizerConfig config) {
        if (config == null) {
            mCurrentLightState = new float[0];
            mZonePeaks = new float[0];
            mDecayedFrequencyState = new float[0];
        } else {
            mCurrentLightState = new float[config.zones.length];
            mZonePeaks = new float[config.zones.length];
            Arrays.fill(mZonePeaks, EPSILON);
            mDecayedFrequencyState = new float[config.uniqueRanges.length];
        }
        mLastHash = Integer.MIN_VALUE;
        mLastSendMs = 0L;
        mSilenceStartTimeMs = 0;
        mLastFrameMs = 0;
        mBreathingEnvelope = 0f;
    }

    public int[] processFrame(float[] uniquePeaks, AudioProcessor.VisualizerConfig config, long nowMs) {
        if (config == null) {
            return new int[0];
        }

        int hardwareCount = DeviceProfile.getLedCount(mDeviceType);
        int zoneCount = Math.max(config.zones.length, hardwareCount);

        ensureStateArrays(zoneCount, config.uniqueRanges.length);

        float[] nextLightState = computeNextLightState(uniquePeaks, config, zoneCount);

        if (nowMs - mLastNotificationFlashMs < FLASH_DURATION_MS) {
            Arrays.fill(nextLightState, 1.0f);
        } else {
            applyIdleBreathing(nextLightState, uniquePeaks, nowMs);
        }

        System.arraycopy(nextLightState, 0, mCurrentLightState, 0, nextLightState.length);

        int[] frameColors = buildFrameColors(nextLightState, zoneCount);
        int frameHash = Arrays.hashCode(frameColors);
        if (frameHash == mLastHash) {
            return null; // No change
        }

        mLastHash = frameHash;
        mLastSendMs = nowMs;
        return frameColors;
    }

    public void triggerNotificationFlash(long nowMs) {
        if (mNotificationFlashEnabled) {
            mLastNotificationFlashMs = nowMs;
        }
    }

    public float[] getCurrentLightState() {
        return mCurrentLightState != null ? Arrays.copyOf(mCurrentLightState, mCurrentLightState.length) : new float[0];
    }

    private float[] computeNextLightState(float[] uniquePeaks, AudioProcessor.VisualizerConfig config, int totalCount) {
        float[] decayedFrequencyState = computeDecayedFrequencyState(uniquePeaks, config);
        float[] nextState = new float[totalCount];

        for (int i = 0; i < config.zones.length; i++) {
            float rawZonePeak = 0f;
            int[] overlappingRanges = config.zoneToRangeIndices[i];
            for (int rangeIndex : overlappingRanges) {
                if (rangeIndex >= 0 && rangeIndex < decayedFrequencyState.length) {
                    rawZonePeak = Math.max(rawZonePeak, decayedFrequencyState[rangeIndex]);
                }
            }

            mZonePeaks[i] = Math.max(rawZonePeak, mZonePeaks[i] * PEAK_FALLOFF);
            if (mZonePeaks[i] < EPSILON) {
                mZonePeaks[i] = EPSILON;
            }

            float normalized = rawZonePeak / mZonePeaks[i];
            float shaped = normalized * normalized;
            float mapped = applyPercentSlice(shaped, config.zones[i]);
            nextState[i] = mapped < EPSILON ? 0f : mapped;
        }

        return nextState;
    }

    private float[] computeDecayedFrequencyState(float[] uniquePeaks, AudioProcessor.VisualizerConfig config) {
        float[] next = new float[mDecayedFrequencyState.length];
        for (int i = 0; i < next.length; i++) {
            float current = (i < uniquePeaks.length ? uniquePeaks[i] : 0f) * SPECTRUM_GAIN;
            float risen = Math.max(mDecayedFrequencyState[i], current);
            float decayed = (config.decay * risen) + ((1f - config.decay) * current);
            next[i] = decayed < EPSILON ? 0f : decayed;
        }
        System.arraycopy(next, 0, mDecayedFrequencyState, 0, next.length);
        return next;
    }

    private void applyIdleBreathing(float[] nextState, float[] uniquePeaks, long nowMs) {
        long deltaMs = (mLastFrameMs > 0) ? (nowMs - mLastFrameMs) : 16;
        mLastFrameMs = nowMs;

        boolean isSilent = true;
        for (float peak : uniquePeaks) {
            if (peak * SPECTRUM_GAIN > SILENCE_THRESHOLD) {
                isSilent = false;
                break;
            }
        }

        if (isSilent) {
            if (mSilenceStartTimeMs == 0) {
                mSilenceStartTimeMs = nowMs;
            }
        } else {
            mSilenceStartTimeMs = 0;
        }

        long silenceDuration = (mSilenceStartTimeMs > 0) ? (nowMs - mSilenceStartTimeMs) : 0;
        boolean shouldBreathe = mIdleBreathingEnabled && (silenceDuration > BREATH_DELAY_MS);
        float targetEnvelope = shouldBreathe ? 1.0f : 0.0f;

        // Smooth envelope transition
        if (mBreathingEnvelope < targetEnvelope) {
            // Fade in slowly (~1.5s)
            mBreathingEnvelope += (float) deltaMs / 1500f;
            if (mBreathingEnvelope > targetEnvelope) mBreathingEnvelope = targetEnvelope;
        } else if (mBreathingEnvelope > targetEnvelope) {
            // Fade out faster (~300ms) for responsiveness
            mBreathingEnvelope -= (float) deltaMs / 300f;
            if (mBreathingEnvelope < targetEnvelope) mBreathingEnvelope = targetEnvelope;
        }

        if (mBreathingEnvelope > 0.01f) {
            int zoneCount = nextState.length;
            for (int i = 0; i < zoneCount; i++) {
                float intensity = 0f;
                switch (mIdlePattern) {
                    case "wave": {
                        double timeProg = (double) (nowMs % 2000L) / 2000L;
                        float phaseShift = (float) i / Math.max(1, zoneCount);
                        intensity = (float) (0.5 + 0.5 * Math.sin(2.0 * Math.PI * (timeProg - phaseShift)));
                        break;
                    }
                    case "scanner": {
                        double timeProg = (double) (nowMs % 2500L) / 2500L;
                        float scannerPos = (float) (0.5 + 0.5 * Math.sin(2.0 * Math.PI * timeProg));
                        float ledPos = (float) i / Math.max(1, zoneCount);
                        float dist = Math.abs(ledPos - scannerPos);
                        intensity = (float) Math.exp(-dist * dist * 40.0);
                        break;
                    }
                    case "static": {
                        intensity = 0.4f;
                        break;
                    }
                    case "pulse":
                    default: {
                        double timeProg = (double) (nowMs % 3000L) / 3000L;
                        float phaseShift = (float) i * 0.02f;
                        intensity = (float) (0.5 + 0.5 * Math.sin(2.0 * Math.PI * (timeProg + phaseShift) - Math.PI / 2.0));
                        break;
                    }
                }

                float breathVal = (0.02f + intensity * 0.48f) * mBreathingEnvelope;
                if (nextState[i] < breathVal) {
                    nextState[i] = breathVal;
                }
            }
        }
    }

    private int[] buildFrameColors(float[] normalizedLightState, int expectedLength) {
        int[] frameColors = new int[expectedLength];
        int count = Math.min(normalizedLightState.length, expectedLength);
        for (int i = 0; i < count; i++) {
            frameColors[i] = Math.round(applyGamma(normalizedLightState[i]) * 4095f);
        }
        return frameColors;
    }

    private float applyGamma(float normalizedValue) {
        if (normalizedValue <= 0f) {
            return 0f;
        }
        return (float) Math.pow(normalizedValue, mGamma);
    }

    private void ensureStateArrays(int zoneCount, int uniqueRangeCount) {
        if (mCurrentLightState.length == zoneCount
                && mZonePeaks.length == zoneCount
                && mDecayedFrequencyState.length == uniqueRangeCount) {
            return;
        }

        mCurrentLightState = new float[zoneCount];
        mZonePeaks = new float[zoneCount];
        Arrays.fill(mZonePeaks, EPSILON);
        mDecayedFrequencyState = new float[uniqueRangeCount];
        mLastHash = Integer.MIN_VALUE;
    }

    private static float applyPercentSlice(float normalizedValue, AudioProcessor.ZoneSpec zone) {
        if (!zone.hasPercentSlice()) {
            return normalizedValue;
        }

        float low = Math.min(zone.lowPercent, zone.highPercent);
        float high = Math.max(zone.lowPercent, zone.highPercent);
        float percent = normalizedValue * 100f;

        if (percent <= low) {
            return 0f;
        }
        if (percent >= high || high == low) {
            return 1f;
        }
        return (percent - low) / (high - low);
    }
}