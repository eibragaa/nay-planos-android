// Ponte JNI entre o app e o llama.cpp (multimodal via libmtmd).
// API verificada no commit da tag v0.9.0: llama.h, tools/mtmd/mtmd.h e tools/mtmd/mtmd-helper.h.
#include <jni.h>
#include <android/log.h>

#include <string>
#include <vector>

#include "llama.h"
#include "mtmd.h"
#include "mtmd-helper.h"

#define LOG_TAG "nayllm"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace {

constexpr int kNCtx = 4096;
constexpr int kNBatch = 512;
constexpr int kMaxTokenPieceBytes = 256;

struct Engine {
    llama_model * model = nullptr;
    llama_context * ctx = nullptr;
    mtmd_context * mtmd = nullptr;
    const llama_vocab * vocab = nullptr;
};

bool g_backend_ready = false;

void throw_java(JNIEnv * env, const char * message) {
    jclass cls = env->FindClass("java/lang/IllegalStateException");
    if (cls != nullptr) env->ThrowNew(cls, message);
}

void free_engine(Engine * engine) {
    if (engine->mtmd != nullptr) mtmd_free(engine->mtmd);
    if (engine->ctx != nullptr) llama_free(engine->ctx);
    if (engine->model != nullptr) llama_model_free(engine->model);
    delete engine;
}

}  // namespace

extern "C" JNIEXPORT jlong JNICALL
Java_br_com_eibragaa_nayplanos_data_llm_NativeLlm_nativeLoad(
        JNIEnv * env, jobject /*thiz*/, jstring jModelPath, jstring jMmprojPath, jint nThreads) {
    if (!g_backend_ready) {
        llama_backend_init();
        g_backend_ready = true;
    }
    const char * modelPath = env->GetStringUTFChars(jModelPath, nullptr);
    const char * mmprojPath = env->GetStringUTFChars(jMmprojPath, nullptr);

    auto * engine = new Engine();

    llama_model_params mparams = llama_model_default_params();
    mparams.n_gpu_layers = 0;  // CPU: o aparelho usa kernels ARM otimizados
    engine->model = llama_model_load_from_file(modelPath, mparams);
    if (engine->model == nullptr) {
        LOGE("falha ao carregar o modelo: %s", modelPath);
        free_engine(engine);
        env->ReleaseStringUTFChars(jModelPath, modelPath);
        env->ReleaseStringUTFChars(jMmprojPath, mmprojPath);
        throw_java(env, "Não foi possível carregar o modelo.");
        return 0;
    }

    llama_context_params cparams = llama_context_default_params();
    cparams.n_ctx = kNCtx;
    cparams.n_batch = kNBatch;
    cparams.n_ubatch = kNBatch;
    cparams.n_threads = nThreads;
    cparams.n_threads_batch = nThreads;
    engine->ctx = llama_init_from_model(engine->model, cparams);

    mtmd_context_params vparams = mtmd_context_params_default();
    vparams.use_gpu = false;
    vparams.n_threads = nThreads;
    vparams.warmup = false;
    engine->mtmd = mtmd_init_from_file(mmprojPath, engine->model, vparams);

    env->ReleaseStringUTFChars(jModelPath, modelPath);
    env->ReleaseStringUTFChars(jMmprojPath, mmprojPath);

    if (engine->ctx == nullptr || engine->mtmd == nullptr || !mtmd_support_vision(engine->mtmd)) {
        LOGE("contexto ou codificador de imagem indisponível");
        free_engine(engine);
        throw_java(env, "O modelo não oferece suporte a imagens neste aparelho.");
        return 0;
    }
    engine->vocab = llama_model_get_vocab(engine->model);
    return reinterpret_cast<jlong>(engine);
}

// Gera texto a partir de uma imagem RGB (3 bytes por pixel, linha a linha) e um prompt.
extern "C" JNIEXPORT jstring JNICALL
Java_br_com_eibragaa_nayplanos_data_llm_NativeLlm_nativeDescribe(
        JNIEnv * env, jobject /*thiz*/, jlong handle, jbyteArray jRgb, jint width, jint height,
        jstring jPrompt, jint maxTokens) {
    auto * engine = reinterpret_cast<Engine *>(handle);
    if (engine == nullptr || engine->ctx == nullptr) {
        throw_java(env, "Modelo não carregado.");
        return nullptr;
    }
    const jsize rgbLen = env->GetArrayLength(jRgb);
    if (width <= 0 || height <= 0 || static_cast<jlong>(rgbLen) != static_cast<jlong>(width) * height * 3) {
        throw_java(env, "Imagem com dimensões inválidas.");
        return nullptr;
    }

    // Limpa o KV cache: cada descrição é independente.
    llama_memory_clear(llama_get_memory(engine->ctx), true);

    std::vector<jbyte> rgb(rgbLen);
    env->GetByteArrayRegion(jRgb, 0, rgbLen, rgb.data());
    mtmd_bitmap * bitmap = mtmd_bitmap_init(
            static_cast<uint32_t>(width), static_cast<uint32_t>(height),
            reinterpret_cast<const unsigned char *>(rgb.data()));
    if (bitmap == nullptr) {
        throw_java(env, "Não foi possível preparar a imagem.");
        return nullptr;
    }

    // Template ChatML usado pela família Qwen2-VL. O marcador de mídia é o padrão do mtmd.
    const char * prompt = env->GetStringUTFChars(jPrompt, nullptr);
    std::string text =
            "<|im_start|>system\nVocê é um assistente que escreve em português do Brasil.<|im_end|>\n"
            "<|im_start|>user\n";
    text += mtmd_default_marker();
    text += "\n";
    text += prompt;
    text += "<|im_end|>\n<|im_start|>assistant\n";
    env->ReleaseStringUTFChars(jPrompt, prompt);

    mtmd_input_text input{};
    input.text = text.c_str();
    input.text_len = text.size();
    input.add_special = true;
    input.parse_special = true;

    mtmd_input_chunks * chunks = mtmd_input_chunks_init();
    const mtmd_bitmap * bitmaps[] = {bitmap};
    int32_t tok = mtmd_tokenize(engine->mtmd, chunks, &input, bitmaps, 1);
    mtmd_bitmap_free(bitmap);
    if (tok != 0) {
        mtmd_input_chunks_free(chunks);
        throw_java(env, "Falha ao preparar a imagem para o modelo.");
        return nullptr;
    }

    llama_pos n_past = 0;
    int32_t ev = mtmd_helper_eval_chunks(engine->mtmd, engine->ctx, chunks,
                                         0, 0, kNBatch, true, &n_past);
    mtmd_input_chunks_free(chunks);
    if (ev != 0) {
        throw_java(env, "Falha ao processar a imagem.");
        return nullptr;
    }

    // Amostragem gulosa: saída estável para formulários de planejamento.
    llama_sampler * sampler = llama_sampler_chain_init(llama_sampler_chain_default_params());
    llama_sampler_chain_add(sampler, llama_sampler_init_greedy());

    std::string out;
    char piece[kMaxTokenPieceBytes];

    for (int i = 0; i < maxTokens; ++i) {
        llama_token token = llama_sampler_sample(sampler, engine->ctx, -1);
        if (llama_vocab_is_eog(engine->vocab, token)) break;

        int n = llama_token_to_piece(engine->vocab, token, piece, sizeof(piece), 0, true);
        if (n > 0) out.append(piece, n);

        // Um token por vez. Com M-RoPE a posição é repetida nas quatro seções.
        llama_pos pos[4] = {n_past, n_past, n_past, n_past};
        llama_seq_id seq0 = 0;
        llama_seq_id * seq_ptrs[1] = {&seq0};
        int32_t n_seq[1] = {1};
        int8_t logits[1] = {1};
        llama_batch batch{};
        batch.n_tokens = 1;
        batch.token = &token;
        batch.pos = pos;
        batch.n_seq_id = n_seq;
        batch.seq_id = seq_ptrs;
        batch.logits = logits;

        if (llama_decode(engine->ctx, batch) != 0) {
            LOGE("llama_decode falhou no token %d", i);
            break;
        }
        n_past += 1;
    }
    llama_sampler_free(sampler);

    return env->NewStringUTF(out.c_str());
}

extern "C" JNIEXPORT void JNICALL
Java_br_com_eibragaa_nayplanos_data_llm_NativeLlm_nativeFree(
        JNIEnv * /*env*/, jobject /*thiz*/, jlong handle) {
    auto * engine = reinterpret_cast<Engine *>(handle);
    if (engine != nullptr) free_engine(engine);
}
