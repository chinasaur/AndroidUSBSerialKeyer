/*
 * Copyright (C) 2025 Peter K6PLI
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
#include <vector>
#include <unistd.h>

#include "SidetoneEngine.h"
#include "logging_macros.h"


SidetoneEngine::SidetoneEngine() : mDataCallback(std::make_shared<SidetoneCallback>()) {}

oboe::Result SidetoneEngine::openPlaybackStream(int32_t device_id, oboe::AudioApi audio_api) {
    oboe::AudioStreamBuilder builder;
    return builder.setSharingMode(oboe::SharingMode::Exclusive)
            ->setPerformanceMode(oboe::PerformanceMode::LowLatency)
            ->setFormat(oboe::AudioFormat::Float)
            ->setFormatConversionAllowed(true)
            ->setDataCallback(mDataCallback)
            ->setErrorCallback(this)
            ->setAudioApi(audio_api)
            ->setChannelCount(1)
            ->setDeviceId(device_id)
            ->setAttributionTag("sidetone_audio")
            ->openStream(mStream);
}

oboe::Result SidetoneEngine::start(oboe::AudioApi audio_api, int32_t device_id, float frequency) {
    std::lock_guard<std::mutex> lock(mLock);

    mAudioApi = audio_api;
    mDeviceId = device_id;
    mFrequency = frequency;

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

            mAudioSource = std::make_shared<SidetoneOscillator>();
            mAudioSource->setSampleRate(mStream->getSampleRate());
            mAudioSource->setFrequency(mFrequency);
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

void SidetoneEngine::playSidetone() {
    if (mAudioSource) mAudioSource->setWaveOn(true);
}

void SidetoneEngine::playSilence() {
    if (mAudioSource) mAudioSource->setWaveOn(false);
}

void SidetoneEngine::setFrequency(float frequency) {
    std::lock_guard<std::mutex> lock(mLock);
    mFrequency = frequency;
    if (mAudioSource) {
        mAudioSource->setFrequency(mFrequency);
    }
}

void SidetoneEngine::onErrorAfterClose(oboe::AudioStream *oboeStream, oboe::Result error) {
    if (error == oboe::Result::ErrorDisconnected) {
        LOGI("Audio device disconnected, restarting stream");
        start(mAudioApi, mDeviceId, mFrequency);
    }
}
