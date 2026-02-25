/**
 * musicViz.js - High-Performance Audio Analysis for Nothing Phone Glyphs
 * Pipeline: mono → Real FFT → peak extraction per zone → decay → normalize to 0-4095
 */
class MusicVisualizer {
    constructor(configPath, logCallback = null) {
        this.configPath = configPath;
        this.config = null;
        this.audioContext = new (window.AudioContext || window.webkitAudioContext)();
        this.FPS = 60;
        this.logCallback = logCallback;
        this.fftInstances = new Map();
        // Lazy-load FFT library
        this.fftReady = this._loadFFT();
    }

    log(msg) {
        if (this.logCallback) this.logCallback(msg);
        console.log(msg);
    }

    async _loadFFT() {
        if (window.FFT) return;

        const sources = [
            'https://cdnjs.cloudflare.com/ajax/libs/dsp.js/1.0.1/dsp.min.js',
            'https://cdn.jsdelivr.net/npm/dsp.js@1.0.1/dsp.min.js'
        ];

        let lastError = null;
        for (const src of sources) {
            try {
                await new Promise((resolve, reject) => {
                    const script = document.createElement('script');
                    script.src = src;
                    script.onload = resolve;
                    script.onerror = () => reject(new Error(`Failed to load ${src}`));
                    document.head.appendChild(script);
                });
                if (window.FFT) return;
            } catch (err) {
                lastError = err;
            }
        }

        throw lastError || new Error('Could not load browser FFT library');
    }

    _getFFT(bufferSize, sampleRate) {
        const key = `${bufferSize}@${sampleRate}`;
        if (!this.fftInstances.has(key)) {
            this.fftInstances.set(key, new window.FFT(bufferSize, sampleRate));
        }
        return this.fftInstances.get(key);
    }

    async loadConfig() {
        const response = await fetch(this.configPath);
        if (!response.ok) throw new Error(`Config load failed: ${response.status}`);
        try {
            const text = await response.text();
            this.config = JSON.parse(text);
        } catch (e) {
            throw new Error(`Failed to parse zones.config: ${e.message}`);
        }
    }

    getAvailablePhones() {
        return Object.keys(this.config || {}).filter(k => k !== 'version' && k !== 'amp' && k !== 'decay-alpha' && k !== 'what-is-decay-alpha' && k !== 'what-is-decay');
    }
    getPhoneInfo(key) {
        const phone = this.config?.[key];
        if (!phone) return null;
        return {
            phone_model: phone.phone_model || key,
            description: phone.description || '',
            zone_count: phone.zones?.length || 0,
            decay_alpha: this.config?.['decay-alpha'] || 0.8
        };
    }

    async loadAudioFile(file) {
        return new Promise((resolve, reject) => {
            const reader = new FileReader();
            reader.onload = async (e) => {
                try {
                    resolve(await this.audioContext.decodeAudioData(e.target.result));
                } catch (err) {
                    reject(new Error(`Decode failed: ${err.message}`));
                }
            };
            reader.onerror = () => reject(new Error('File read failed'));
            reader.readAsArrayBuffer(file);
        });
    }

    _hannWindow(len) {
        const w = new Float32Array(len);
        for (let i = 0; i < len; i++) w[i] = 0.5 * (1 - Math.cos(2 * Math.PI * i / (len - 1)));
        return w;
    }

    // Real FFT with peak extraction per frequency range
    async _fftAnalysis(frame, sr, freqRanges) {
        await this.fftReady;
        if (!window.FFT) throw new Error('FFT library not loaded');
        
        // Pad to next power of 2
        const padSize = Math.pow(2, Math.ceil(Math.log2(frame.length)));
        const padded = new Float32Array(padSize);
        padded.set(frame);
        
        // Compute FFT and use library-provided magnitude spectrum (N/2 bins)
        const fft = this._getFFT(padSize, sr);
        fft.forward(padded);
        const mag = fft.spectrum;
        
        // Extract peak magnitude for each frequency range
        const peaks = new Float32Array(freqRanges.length);
        const freqRes = sr / padSize;
        
        for (let fi = 0; fi < freqRanges.length; fi++) {
            const [low, high] = freqRanges[fi];
            const binLow = Math.ceil(low / freqRes);
            const binHigh = Math.floor(high / freqRes);
            let peak = 0;
            for (let b = binLow; b <= binHigh; b++) {
                if (b < mag.length) peak = Math.max(peak, mag[b]);
            }
            peaks[fi] = peak;
        }
        return peaks;
    }

    _getMinMax(arr) {
        let min = Infinity, max = -Infinity;
        for (let i = 0; i < arr.length; i++) {
            if (arr[i] < min) min = arr[i];
            if (arr[i] > max) max = arr[i];
        }
        return { min: min === Infinity ? 0 : min, max: max === -Infinity ? 0 : max };
    }

    // Normalize to 0-4095 with dynamic range compression for beats
    _normalizeToBrightness(zoneTable, nFrames, nZones) {
        const result = new Float32Array(zoneTable.length);
        
        // Find min/max per zone for dynamic normalization
        const zoneStats = new Array(nZones);
        for (let zi = 0; zi < nZones; zi++) {
            let min = Infinity, max = -Infinity;
            for (let i = 0; i < nFrames; i++) {
                const val = zoneTable[i * nZones + zi];
                if (val < min) min = val;
                if (val > max) max = val;
            }
            zoneStats[zi] = { min, max, range: max - min };
        }
        
        // Scale each zone to 0-4095, compressing dynamics so beats hit 4095
        for (let i = 0; i < nFrames; i++) {
            for (let zi = 0; zi < nZones; zi++) {
                const idx = i * nZones + zi;
                const { min, range } = zoneStats[zi];
                const normalized = range > 1e-6 ? (zoneTable[idx] - min) / range : 0;
                // Apply compression curve to boost beat peaks to 4095
                const compressed = Math.pow(Math.max(0, Math.min(1, normalized)), 0.7);
                result[idx] = Math.round(compressed * 4095);
            }
        }
        
        return result;
    }

    _applyDecay(zoneTable, decayAlpha, nFrames, nZones) {
        const result = new Float32Array(zoneTable.length);
        const alpha = 0.86 + decayAlpha / 10;
        
        // First frame
        for (let zi = 0; zi < nZones; zi++) result[zi] = zoneTable[zi];
        
        // Decay: instant rise, smooth exponential fall
        for (let i = 1; i < nFrames; i++) {
            for (let zi = 0; zi < nZones; zi++) {
                const idx = i * nZones + zi;
                const prev = result[(i - 1) * nZones + zi];
                const cur = zoneTable[idx];
                result[idx] = Math.max(prev, cur) * alpha + cur * (1 - alpha);
            }
        }
        
        return result;
    }

    _mapToZones(freqTable, uniqueFreqs, zones, nFrames, nFreqs) {
        const nZones = zones.length;
        const zoneTable = new Float32Array(nFrames * nZones);
        
        for (let i = 0; i < nFrames; i++) {
            for (let zi = 0; zi < nZones; zi++) {
                const [zoneLow, zoneHigh] = zones[zi];
                let peak = 0;
                
                // Find peak magnitude in this zone's range
                for (let fi = 0; fi < nFreqs; fi++) {
                    const [fLow, fHigh] = uniqueFreqs[fi];
                    if (!(fHigh < zoneLow || fLow > zoneHigh)) {
                        peak = Math.max(peak, freqTable[i * nFreqs + fi]);
                    }
                }
                
                zoneTable[i * nZones + zi] = peak * peak; // quadratic boost
            }
        }
        
        return zoneTable;
    }

    async processAudio(audioFile, phoneKey, onProgress = null) {
        if (!this.config) await this.loadConfig();
        const conf = this.config[phoneKey];
        if (!conf?.zones) throw new Error(`Phone ${phoneKey} not found`);

        this.log(`━━━ Processing: ${audioFile.name}`);
        this.log(`📱 Preset: ${phoneKey} (${conf.zones.length} zones)`);

        // Decode audio to mono
        this.log('🔊 Decoding audio...');
        const audioBuffer = await this.loadAudioFile(audioFile);
        const samples = audioBuffer.getChannelData(0);
        const sr = audioBuffer.sampleRate;
        this.log(`✓ Decoded: ${(samples.length / sr).toFixed(2)}s @ ${sr}Hz`);

        // FFT parameters
        const hop = Math.round(sr / this.FPS);
        const winLen = Math.pow(2, Math.ceil(Math.log2(sr * 0.05)));
        const window = this._hannWindow(winLen);
        const freqRanges = [...new Set(conf.zones.map(z => JSON.stringify([z[0], z[1]])))].map(f => JSON.parse(f));
        
        this.log(`📊 FFT params: ${winLen} window, ${freqRanges.length} freq ranges`);
        
        // Compute frames with real FFT
        const nFrames = Math.ceil(samples.length / hop);
        const nZones = conf.zones.length;
        const zoneTable = new Float32Array(nFrames * nZones);

        this.log(`⚙️ Analyzing ${nFrames} frames...`);
        const progressIntervals = 10; // Log every 10%
        const frameDelta = Math.max(1, Math.floor(nFrames / progressIntervals));
        let lastReportedPercent = 0;

        for (let i = 0; i < nFrames; i++) {
            const start = i * hop;
            const frame = new Float32Array(winLen);
            for (let j = 0; j < winLen; j++) {
                frame[j] = (start + j < samples.length ? samples[start + j] : 0) * window[j];
            }

            // Real FFT analysis for peak magnitudes per range
            const peaks = await this._fftAnalysis(frame, sr, freqRanges);

            // Map peaks to zones
            for (let zi = 0; zi < nZones; zi++) {
                let maxPeak = 0;
                const [zLow, zHigh] = conf.zones[zi];
                for (let fi = 0; fi < freqRanges.length; fi++) {
                    const [fLow, fHigh] = freqRanges[fi];
                    if (!(fHigh < zLow || fLow > zHigh)) {
                        maxPeak = Math.max(maxPeak, peaks[fi]);
                    }
                }
                zoneTable[i * nZones + zi] = maxPeak * maxPeak;
            }

            // Only report progress at intervals, not every frame
            if ((i + 1) % frameDelta === 0) {
                const percent = Math.round((i + 1) / nFrames * 100);
                if (percent !== lastReportedPercent) {
                    if (onProgress) {
                        onProgress({
                            stage: 'FFT Analysis',
                            progress: percent,
                            current: i + 1,
                            total: nFrames
                        });
                    }
                    lastReportedPercent = percent;
                }
            }
        }

        this.log(`✓ FFT analysis complete`);

        // Apply decay
        this.log('🎚️ Applying decay...');
        const decayAlpha = this.config['decay-alpha'] || 0.8;
        const decayed = this._applyDecay(zoneTable, decayAlpha, nFrames, nZones);
        this.log(`✓ Decay applied (alpha: ${decayAlpha})`);

        // Normalize to 0-4095 with beat emphasis
        this.log('🔆 Normalizing to brightness (0-4095)...');
        const final = this._normalizeToBrightness(decayed, nFrames, nZones);
        
        // Check brightness distribution
        let max4095 = 0;
        for (let i = 0; i < final.length; i++) {
            if (final[i] === 4095) max4095++;
        }
        this.log(`✓ Brightness normalized (${((max4095 / final.length) * 100).toFixed(1)}% at full)`);

        this.log(`✅ Processing complete!\n`);

        return {
            data: final,
            nFrames,
            nZones,
            phoneModel: conf.phone_model || phoneKey,
            phoneKey,
            fileName: audioFile.name,
            nFreqs: freqRanges.length
        };
    }

    exportAsNGlyph(result) {
        const { data, nFrames, nZones, phoneModel } = result;
        const rows = [];
        for (let i = 0; i < nFrames; i++) {
            const row = [];
            for (let zi = 0; zi < nZones; zi++) {
                row.push(data[i * nZones + zi]);
            }
            rows.push(row.join(','));
        }
        return JSON.stringify({
            VERSION: 1,
            PHONE_MODEL: phoneModel,
            AUTHOR: rows,
            CUSTOM1: ["1-0", "1050-1"]
        }, null, 2);
    }

    exportAsCSV(result) {
        const { data, nFrames, nZones } = result;
        const rows = [];
        for (let i = 0; i < nFrames; i++) {
            const row = [];
            for (let zi = 0; zi < nZones; zi++) {
                row.push(data[i * nZones + zi]);
            }
            rows.push(row.join(','));
        }
        return rows.join('\n');
    }

    exportAsCompact(result) {
        const { data, nFrames, nZones } = result;
        const buffer = new ArrayBuffer(8 + data.length * 2);
        const view = new DataView(buffer);
        view.setUint32(0, nFrames, true);
        view.setUint32(4, nZones, true);
        for (let i = 0; i < data.length; i++) {
            view.setUint16(8 + i * 2, Math.round(data[i]), true);
        }
        return buffer;
    }

    // All-in-one: config + audio + preset → processed data
    async processAndExport(configPath, audioFile, presetKey, format = 'nglyph', onProgress = null) {
        if (!this.config) {
            this.log('📂 Loading configuration...');
            this.configPath = configPath;
            await this.loadConfig();
            this.log('✓ Config loaded');
        }
        
        const result = await this.processAudio(audioFile, presetKey, onProgress);
        
        this.log(`💾 Exporting as ${format.toUpperCase()}...`);
        let output;
        if (format === 'csv') {
            output = this.exportAsCSV(result);
        } else if (format === 'compact') {
            output = this.exportAsCompact(result);
        } else {
            output = this.exportAsNGlyph(result);
        }
        this.log(`✓ Export ready (${format})\n`);
        
        return output;
    }
}
