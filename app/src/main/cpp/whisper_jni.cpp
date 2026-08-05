#include <jni.h>
#include <algorithm>
#include <chrono>
#include <string>
#include <thread>
#include <vector>
#include <android/log.h>
#include "whisper.h"

namespace {
constexpr const char *LOG_TAG = "TakoFlowWhisper";

std::string to_string(JNIEnv *env, jstring value) {
    if (value == nullptr) return {};
    const char *chars = env->GetStringUTFChars(value, nullptr);
    std::string result(chars == nullptr ? "" : chars);
    if (chars != nullptr) env->ReleaseStringUTFChars(value, chars);
    return result;
}

int transcription_threads() {
    const unsigned int available = std::thread::hardware_concurrency();
    if (available == 0) return 4;
    return std::max(2, std::min(6, static_cast<int>(available)));
}
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_example_speech_WhisperBridge_nativeInit(
        JNIEnv *env,
        jobject,
        jstring model_path) {
    const std::string path = to_string(env, model_path);
    whisper_context_params context_params = whisper_context_default_params();
    context_params.use_gpu = false;
    context_params.flash_attn = true;
    whisper_context *context = whisper_init_from_file_with_params(path.c_str(), context_params);
    return reinterpret_cast<jlong>(context);
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_speech_WhisperBridge_nativeTranscribe(
        JNIEnv *env,
        jobject,
        jlong context_pointer,
        jfloatArray samples_array) {
    auto *context = reinterpret_cast<whisper_context *>(context_pointer);
    if (context == nullptr || samples_array == nullptr) {
        return env->NewStringUTF("");
    }

    const jsize sample_count = env->GetArrayLength(samples_array);
    if (sample_count <= 0) {
        return env->NewStringUTF("");
    }

    std::vector<float> samples(static_cast<size_t>(sample_count));
    env->GetFloatArrayRegion(samples_array, 0, sample_count, samples.data());

    whisper_full_params params = whisper_full_default_params(WHISPER_SAMPLING_GREEDY);
    params.language = "en";
    params.translate = false;
    params.no_context = true;
    params.no_timestamps = true;
    params.single_segment = true;
    params.print_special = false;
    params.print_progress = false;
    params.print_realtime = false;
    params.print_timestamps = false;
    params.token_timestamps = false;
    params.suppress_blank = true;
    params.suppress_nst = true;
    params.max_tokens = 96;
    params.greedy.best_of = 1;
    params.temperature_inc = 0.0f;
    params.n_threads = transcription_threads();

    const auto started = std::chrono::steady_clock::now();
    const int result = whisper_full(
        context,
        params,
        samples.data(),
        static_cast<int>(samples.size())
    );
    const auto elapsed_ms = std::chrono::duration_cast<std::chrono::milliseconds>(
        std::chrono::steady_clock::now() - started
    ).count();

    __android_log_print(
        ANDROID_LOG_INFO,
        LOG_TAG,
        "Processed %.2f seconds of audio in %lld ms using %d threads",
        static_cast<double>(samples.size()) / WHISPER_SAMPLE_RATE,
        static_cast<long long>(elapsed_ms),
        params.n_threads
    );

    if (result != 0) {
        return env->NewStringUTF("");
    }

    std::string transcript;
    const int segment_count = whisper_full_n_segments(context);
    for (int index = 0; index < segment_count; ++index) {
        const char *segment = whisper_full_get_segment_text(context, index);
        if (segment != nullptr) transcript += segment;
    }

    return env->NewStringUTF(transcript.c_str());
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_speech_WhisperBridge_nativeFree(
        JNIEnv *,
        jobject,
        jlong context_pointer) {
    auto *context = reinterpret_cast<whisper_context *>(context_pointer);
    if (context != nullptr) whisper_free(context);
}
