#include <jni.h>
#include <string>
#include <vector>
#include "whisper.h"

namespace {
std::string to_string(JNIEnv *env, jstring value) {
    if (value == nullptr) return {};
    const char *chars = env->GetStringUTFChars(value, nullptr);
    std::string result(chars == nullptr ? "" : chars);
    if (chars != nullptr) env->ReleaseStringUTFChars(value, chars);
    return result;
}
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_example_speech_WhisperBridge_nativeInit(
        JNIEnv *env,
        jclass,
        jstring model_path) {
    const std::string path = to_string(env, model_path);
    whisper_context_params context_params = whisper_context_default_params();
    context_params.use_gpu = false;
    whisper_context *context = whisper_init_from_file_with_params(path.c_str(), context_params);
    return reinterpret_cast<jlong>(context);
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_speech_WhisperBridge_nativeTranscribe(
        JNIEnv *env,
        jclass,
        jlong context_pointer,
        jfloatArray samples_array) {
    auto *context = reinterpret_cast<whisper_context *>(context_pointer);
    if (context == nullptr || samples_array == nullptr) {
        return env->NewStringUTF("");
    }

    const jsize sample_count = env->GetArrayLength(samples_array);
    std::vector<float> samples(static_cast<size_t>(sample_count));
    env->GetFloatArrayRegion(samples_array, 0, sample_count, samples.data());

    whisper_full_params params = whisper_full_default_params(WHISPER_SAMPLING_GREEDY);
    params.language = "en";
    params.translate = false;
    params.no_context = true;
    params.single_segment = false;
    params.print_progress = false;
    params.print_realtime = false;
    params.print_timestamps = false;
    params.suppress_blank = true;
    params.suppress_nst = true;
    params.n_threads = 4;

    if (whisper_full(context, params, samples.data(), static_cast<int>(samples.size())) != 0) {
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
        jclass,
        jlong context_pointer) {
    auto *context = reinterpret_cast<whisper_context *>(context_pointer);
    if (context != nullptr) whisper_free(context);
}
