//
// Created by Peter Li on 8/17/25.
//
#include <vector>

#include "SidetoneEngine.h"
#include "logging_macros.h"


SidetoneEngine::SidetoneEngine() : mDataCallback(std::make_shared<DataCallback>()) {}

oboe::Result SidetoneEngine::openPlaybackStream(int32_t device_id, oboe::AudioApi audio_api) {
    oboe::AudioStreamBuilder builder;
    return builder.setSharingMode(oboe::SharingMode::Exclusive)
            ->setPerformanceMode(oboe::PerformanceMode::LowLatency)
            ->setFormat(oboe::AudioFormat::Float)
            ->setFormatConversionAllowed(true)
            ->setDataCallback(mDataCallback)
//            ->setErrorCallback(mErrorCallback)
            ->setAudioApi(audio_api)
            ->setChannelCount(1)
            ->setDeviceId(device_id)
            ->openStream(mStream);
}

oboe::Result SidetoneEngine::start(oboe::AudioApi audio_api, int32_t device_id, float frequency) {
    std::lock_guard<std::mutex> lock(mLock);
    oboe::Result result;
    // It is possible for a stream's device to become disconnected during the open or between
    // the Open and the Start.
    // So if it fails to start, close the old stream and try again.
    int tryCount = 0;
    do {
        if (tryCount > 0) {
            usleep(20 * 1000); // Sleep between tries to give the system time to settle.
        }
        result = openPlaybackStream(device_id, audio_api);
        if (result == oboe::Result::OK) {
            LOGD("Stream opened: AudioAPI = %d, deviceID = %d",
                 mStream->getAudioApi(),
                 mStream->getDeviceId());

            mAudioSource = std::make_shared<Oscillator>();
            mAudioSource->setSampleRate(mStream->getSampleRate());
            mAudioSource->setFrequency(frequency);
            mAudioSource->setAmplitude(1.f);
            mDataCallback->setSource(mAudioSource);

            result = mStream->requestStart();
            if (result != oboe::Result::OK) {
                LOGE("Error starting playback stream. Error: %s",
                     oboe::convertToText(result));
                mStream->close();
                mStream.reset();
            }
        } else {
            LOGE("Error creating playback stream. Error: %s", oboe::convertToText(result));
        }
    } while (result != oboe::Result::OK && tryCount++ < 3);
    return result;
}

oboe::Result SidetoneEngine::stop() {
    oboe::Result result = oboe::Result::OK;
    // Stop, close and delete in case not already closed.
    std::lock_guard<std::mutex> lock(mLock);
    if (mStream) {
        result = mStream->stop();
        mStream->close();
        mStream.reset();
    }
    return result;
}

void SidetoneEngine::play_tone() {
    if (mAudioSource) mAudioSource->setWaveOn(true);
}

void SidetoneEngine::stop_tone() {
    if (mAudioSource) mAudioSource->setWaveOn(false);
}