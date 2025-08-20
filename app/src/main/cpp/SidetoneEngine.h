//
// Created by Peter Li on 8/17/25.
//
#ifndef USBKEYEROBOEJAVA_SIDETONEENGINE_H
#define USBKEYEROBOEJAVA_SIDETONEENGINE_H

#include <vector>
#include <oboe/Oboe.h>
#include "Oscillator.h"


class DataCallback : public oboe::AudioStreamDataCallback {
public:
    oboe::DataCallbackResult onAudioReady(
            oboe::AudioStream *oboeStream, void *audioData, int32_t numFrames) override {
        if (mIsThreadAffinityEnabled && !mIsThreadAffinitySet) {
            setThreadAffinity();
            mIsThreadAffinitySet = true;
        }

        float* outputBuffer = static_cast<float*>(audioData);
        if (!mOscillator) {
            LOGE("Renderable source not set!");
            return oboe::DataCallbackResult::Stop;
        }
        mOscillator->renderAudio(outputBuffer, numFrames);
        return oboe::DataCallbackResult::Continue;
    }

    void setSource(std::shared_ptr<Oscillator> oscillator) {
        mOscillator = oscillator;
    }

    void setThreadAffinityEnabled(bool isEnabled){
        mIsThreadAffinityEnabled = isEnabled;
        LOGD("Thread affinity enabled: %s", (isEnabled) ? "true" : "false");
    }

private:
    std::shared_ptr<Oscillator> mOscillator;
    std::vector<int> mCpuIds; // IDs of CPU cores which the audio callback should be bound to
    std::atomic<bool> mIsThreadAffinityEnabled { false };
    std::atomic<bool> mIsThreadAffinitySet { false };

    /**
     * Set the thread affinity for the current thread to mCpuIds. This can be useful to call on the
     * audio thread to avoid underruns caused by CPU core migrations to slower CPU cores.
     */
    void setThreadAffinity() {
        pid_t current_thread_id = gettid();
        cpu_set_t cpu_set;
        CPU_ZERO(&cpu_set);

        // If the callback cpu ids aren't specified then bind to the current cpu
        if (mCpuIds.empty()) {
            int current_cpu_id = sched_getcpu();
            LOGD("Binding to current CPU ID %d", current_cpu_id);
            CPU_SET(current_cpu_id, &cpu_set);
        } else {
            LOGD("Binding to %d CPU IDs", static_cast<int>(mCpuIds.size()));
            for (size_t i = 0; i < mCpuIds.size(); i++) {
                int cpu_id = mCpuIds.at(i);
                LOGD("CPU ID %d added to cores set", cpu_id);
                CPU_SET(cpu_id, &cpu_set);
            }
        }

        int result = sched_setaffinity(current_thread_id, sizeof(cpu_set_t), &cpu_set);
        if (result == 0) {
            LOGV("Thread affinity set");
        } else {
            LOGW("Error setting thread affinity. Error no: %d", result);
        }

        mIsThreadAffinitySet = true;
    }
};


class SidetoneEngine {
public:
    SidetoneEngine();
    ~SidetoneEngine() = default;
    oboe::Result start(oboe::AudioApi audio_api, int32_t device_id, float frequency);
    oboe::Result stop();
    void play_tone();
    void stop_tone();

private:
    oboe::Result openPlaybackStream(int32_t device_id, oboe::AudioApi audio_api);

    std::shared_ptr<oboe::AudioStream> mStream;
    std::shared_ptr<Oscillator> mAudioSource;
    std::shared_ptr<DataCallback> mDataCallback;
    std::mutex mLock;
};

#endif //USBKEYEROBOEJAVA_SIDETONEENGINE_H