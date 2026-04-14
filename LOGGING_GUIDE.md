# Comprehensive Logging Guide

## Overview
Extensive logging has been added throughout the app to help with debugging via logcat. All logs use the tag `"BetterViz"` and are at various levels (DEBUG, WARN, ERROR).

## MainActivity.kt Logging

### Service Lifecycle
- **onServiceConnected**: Logs when service connects with component name
- **onServiceDisconnected**: Logs when service disconnects
- **onCreate**: Logs initialization with loaded settings (gamma, latency, presets)

### Permissions & Projection Flow
- **notificationLauncher**: Logs notification permission grant/denial and projection launch
- **projectionLauncher**: Logs projection result code, grants, and denials
- **requestProjection**: Logs permission check and request flow
- **launchProjection**: Logs when media projection intent is launched

### Visualizer Control
- **toggleVisualizer**: Logs start/stop toggle and state transitions
- **deliverProjectionToken**: Logs each step of token delivery and service binding
- **applyServiceSettings**: Logs all settings being applied (device, latency, gamma, preset)
- **stopEverything**: Logs shutdown sequence including unbinding, stopping, etc.

### Settings Updates
- **selectPreset**: Logs preset selection
- **updateLatency**: Logs latency changes with old→new values
- **updateLatencyPresets**: Logs preset list updates
- **updateGamma**: Logs gamma adjustments with new value
- **refreshPresets**: Logs preset refresh and device detection

## AudioCaptureService.java Logging

### Service Lifecycle
- **onCreate**: Logs device detection, settings loading, preset catalog refresh
- **onStartCommand**: Logs command flags, startId, and requested preset
- **onBind**: Logs when client binds to service
- **onDestroy**: Logs destruction with session cleanup steps

### Capture Methods
- **startCapture**: Detailed logging of:
  - Current capture state
  - MediaProjectionManager availability
  - AudioRecord initialization
  - Executor and capture loop startup
  - Each major step with success/failure

- **stopCapture**: Detailed logging of:
  - AudioRecord stopping and release
  - MediaProjection stopping
  - Executor shutdown
  - Cleanup completion

- **captureLoop**: Performance tracking:
  - Background thread startup
  - AudioRecord recording start
  - Frame count every second with FPS calculation
  - AudioRecord error codes
  - Loop exit conditions

### Settings Methods
- **setDevice**: Logs device changes and loaded latency
- **setLatencyCompensationMs**: Logs latency changes with clamping info
- **setGamma**: Logs gamma adjustments with clamping info
- **setPreset**: Logs preset switching (not in current version, but logged in service)

### Frame Processing
- **processFrame**: Already logs setFrameColors errors via catch block
- **Capture loop**: Logs FPS/performance metrics every second

## How to Use in Logcat

### Filter by app:
```bash
adb logcat -s "BetterViz"
```

### Follow startup sequence:
```
BetterViz: MainActivity.onCreate()
BetterViz: Settings loaded: gamma=X, latency=Y...
BetterViz: toggleVisualizer called
BetterViz: Checking notification permission
BetterViz: Launching media projection intent
BetterViz: Projection result: code=RESULT_OK
BetterViz: deliverProjectionToken: resultCode=...
BetterViz: Binding to service
BetterViz: Starting foreground service
BetterViz: onServiceConnected
BetterViz: Service bound successfully
BetterViz: Applying service settings...
BetterViz: startCapture OK - capture loop started
BetterViz: captureLoop started on background thread
BetterViz: AudioRecord recording started, entering processing loop
BetterViz: Capture: N frames, ~60 fps
```

### Follow shutdown sequence:
```
BetterViz: toggleVisualizer called, running=true
BetterViz: Stopping visualizer
BetterViz: stopEverything called
BetterViz: Unbinding service
BetterViz: Stopping service
BetterViz: stopCapture() called
BetterViz: Releasing AudioRecord
BetterViz: MediaProjection stopped
BetterViz: Executor shut down
BetterViz: Service onDestroy() called
```

### Monitor for errors:
```bash
adb logcat -s "BetterViz" | grep -E "ERROR|Exception|failed"
```

### Performance metrics:
Look for lines like:
```
BetterViz: Capture: 60 frames, ~60 fps
```
These appear every second during capture.

## Key Log Points for Debugging

| Issue | Log to Check |
|-------|--------------|
| App won't start visualizer | Check projection permission requests and service binding logs |
| Glyphs aren't lighting up | Check processFrame and setFrameColors logs, AudioRecord start |
| Glyphs are flashing | Check frame frequency in capture loop (~60fps expected) |
| Audio not being captured | Check AudioRecord initialization in startCapture |
| Settings not persisting | Check updateGamma, updateLatency logs for save operations |
| Service crashes | Check onDestroy and stopCapture cleanup logs |
| FPS is low | Check capture loop FPS metrics (should be ~60fps) |

