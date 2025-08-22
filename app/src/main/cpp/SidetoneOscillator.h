/*
 * Copyright 2018 The Android Open Source Project
 * Modified 2025 by Peter K6PLI.
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
 */
#ifndef USBKEYEROBOEJAVA_SIDETONEOSCILLATOR_H
#define USBKEYEROBOEJAVA_SIDETONEOSCILLATOR_H

#include <cstdint>
#include <atomic>
#include <cmath>
#include <memory>

#include "logging_macros.h"

constexpr float kDefaultFrequency = 700.f;
constexpr int32_t kDefaultSampleRate = 48000;
constexpr float kDefaultEnvelopeRiseFallMs = 5.f;
constexpr float kPi = M_PI;
constexpr float kTwoPi = kPi * 2;

class SidetoneOscillator {
public:
    ~SidetoneOscillator() = default;

    void setWaveOn(bool isWaveOn) {
        mIsWaveOn.store(isWaveOn);
    }

    void setSampleRate(int32_t sampleRate) {
        mSampleRate = sampleRate;
        updatePhaseIncrement();
    }

    void setFrequency(float frequency) {
        mFrequency = frequency;
        updatePhaseIncrement();
    }

    inline void setAmplitude(float amplitude) {
        mAmplitude = amplitude;
    }

    void renderAudio(float* audioData, int32_t numFrames) {
        // If wave is completely off, just write silence and return.
        if (!mIsWaveOn && mEnvelopePhase <= 0.f) {
            memset(audioData, 0, sizeof(float) * numFrames);
            return;
        }

        // Otherwise, write a sine wave and then apply envelope shaping if needed.
        for (int i = 0; i < numFrames; ++i) {
            audioData[i] = sinf(mPhase) * mAmplitude;
            mPhase += mPhaseIncrement;
            if (mPhase > kTwoPi) mPhase -= kTwoPi;
        }

        // If envelope shaping is active, reshape the output sine wave to reduce audio popping.
        // It would be more efficient to do the sine wave synthesis and the envelope shaping in a
        // single pass, but doing them sequentially is more readable and in practice seems fine.
        // We also allow mEnvelopePhase to go one step past [0, pi] for simplicity.
        if (mIsWaveOn && mEnvelopePhase < kPi) {
            for (int i = 0; i < numFrames && mEnvelopePhase < kPi; ++i) {
                audioData[i] *= 0.5f - cosf(mEnvelopePhase) / 2.f;
                mEnvelopePhase += mEnvelopePhaseIncrement;  // Ramping up.
            }
        } else if (!mIsWaveOn && mEnvelopePhase > 0.f) {
            int i = 0;
            while (i < numFrames && mEnvelopePhase > 0.f) {
                audioData[i++] *= 0.5f - cosf(mEnvelopePhase) / 2.f;
                mEnvelopePhase -= mEnvelopePhaseIncrement;  // Ramping back down.
            }
            while (i < numFrames) audioData[i++] = 0.f;  // Zero out the rest.
        }
    }

private:
    std::atomic<bool> mIsWaveOn { false };
    float mFrequency = kDefaultFrequency;
    float mPhase = 0.f;
    float mPhaseIncrement = 0.f;
    float mEnvelopeRiseFallMs = kDefaultEnvelopeRiseFallMs;
    float mEnvelopePhase = 0.f;
    float mEnvelopePhaseIncrement = 0.f;
    std::atomic<float> mAmplitude { 0.f };
    int32_t mSampleRate = kDefaultSampleRate;

    void updatePhaseIncrement() {
        mPhaseIncrement = kTwoPi * mFrequency / static_cast<float>(mSampleRate);
        float envelopeFrequency = 1000 / mEnvelopeRiseFallMs / 2;
        mEnvelopePhaseIncrement = kTwoPi * envelopeFrequency / static_cast<float>(mSampleRate);
    }
};

#endif //USBKEYEROBOEJAVA_SIDETONEOSCILLATOR_H