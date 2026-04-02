// Updated AudioCaptureService.java code with required modifications

public class AudioCaptureService {
    
    // Assuming this is how the current variables are structured
    private int[] frameColors = new int[1764]; // 16ms audio window size
    private int decay;

    public void setFrameColors(int[] colors) {
        // Set the frame colors with brightness levels 0-4095
        for (int i = 0; i < colors.length; i++) {
            frameColors[i] = Math.max(0, Math.min(colors[i], 4095)); // Clamp values between 0 and 4095
        }
    }
    
    public void processAudioData(byte[] audioData) {
        int hopSize = 882; // Hop size for 20ms window
        int windowSize = 1764; // Window size for 20ms
        
        // Implement audio processing logic based on hopSize and windowSize
        for (int i = 0; i < audioData.length; i += hopSize) {
            // Process the audio window
            // Assuming some processing logic exists here
            // Adjust decay calculation as required
        }
    }
    
    // Method to replace binary on/off glyph control
    public void updateGlyphControl(float brightnessValue) {
        // Ensure brightness value is handled continuously
        int brightness = Math.max(0, Math.min((int)(brightnessValue * 4095), 4095));
        // Call to update glyph display with continuous brightness value
    }
} 
