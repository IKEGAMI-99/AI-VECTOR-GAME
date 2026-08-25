#include <jni.h>
#include <android/log.h>
#include <algorithm>
#include <cmath>
#include <mutex>
#include <numeric>
#include <sstream>
#include <string>
#include <vector>

#include "llama.h"
#include "ggml-backend.h"

#define LOG_TAG "AIVectorNative"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

namespace {
std::mutex g_mutex;
std::once_flag g_backend_once;
llama_model * g_causal_model = nullptr;
llama_model * g_embedding_model = nullptr;
std::string g_last_error;

void init_backend_once() {
    std::call_once(g_backend_once, [] {
        llama_backend_init();
        ggml_backend_load_all();
    });
}

void set_error(const std::string & message) {
    g_last_error = message;
    LOGE("%s", message.c_str());
}

std::string jstring_to_utf8(JNIEnv * env, jstring value) {
    if (!value) return {};
    const char * chars = env->GetStringUTFChars(value, nullptr);
    if (!chars) return {};
    std::string result(chars);
    env->ReleaseStringUTFChars(value, chars);
    return result;
}

bool is_valid_utf8(const std::string & s) {
    const unsigned char * bytes = reinterpret_cast<const unsigned char *>(s.data());
    size_t i = 0;
    while (i < s.size()) {
        unsigned char c = bytes[i];
        if (c <= 0x7F) { i++; continue; }
        int extra = 0;
        if ((c & 0xE0) == 0xC0) extra = 1;
        else if ((c & 0xF0) == 0xE0) extra = 2;
        else if ((c & 0xF8) == 0xF0) extra = 3;
        else return false;
        if (i + extra >= s.size()) return false;
        for (int k = 1; k <= extra; ++k) {
            if ((bytes[i + k] & 0xC0) != 0x80) return false;
        }
        i += extra + 1;
    }
    return true;
}

std::string token_piece(const llama_vocab * vocab, llama_token token) {
    std::vector<char> buffer(128);
    int32_t n = llama_token_to_piece(vocab, token, buffer.data(), static_cast<int32_t>(buffer.size()), 0, true);
    if (n < 0) {
        buffer.resize(static_cast<size_t>(-n));
        n = llama_token_to_piece(vocab, token, buffer.data(), static_cast<int32_t>(buffer.size()), 0, true);
    }
    if (n <= 0) return {};
    return std::string(buffer.data(), static_cast<size_t>(n));
}

std::string json_escape(const std::string & s) {
    std::ostringstream out;
    for (unsigned char c : s) {
        switch (c) {
            case '\\': out << "\\\\"; break;
            case '"': out << "\\\""; break;
            case '\n': out << "\\n"; break;
            case '\r': out << "\\r"; break;
            case '\t': out << "\\t"; break;
            default:
                if (c < 0x20) {
                    const char hex[] = "0123456789abcdef";
                    out << "\\u00" << hex[(c >> 4) & 0xF] << hex[c & 0xF];
                } else {
                    out << static_cast<char>(c);
                }
        }
    }
    return out.str();
}

bool load_model(const std::string & path, llama_model *& slot) {
    init_backend_once();
    if (slot) {
        llama_model_free(slot);
        slot = nullptr;
    }
    llama_model_params params = llama_model_default_params();
    params.n_gpu_layers = 0;
    slot = llama_model_load_from_file(path.c_str(), params);
    if (!slot) {
        set_error("llama.cpp could not load model: " + path);
        return false;
    }
    return true;
}

std::vector<llama_token> tokenize_text(
    const llama_vocab * vocab,
    const std::string & text,
    bool add_special
) {
    int32_t count = llama_tokenize(
        vocab,
        text.c_str(),
        static_cast<int32_t>(text.size()),
        nullptr,
        0,
        add_special,
        true
    );
    if (count >= 0) return {};
    count = -count;
    std::vector<llama_token> tokens(static_cast<size_t>(count));
    int32_t written = llama_tokenize(
        vocab,
        text.c_str(),
        static_cast<int32_t>(text.size()),
        tokens.data(),
        count,
        add_special,
        true
    );
    if (written < 0) return {};
    tokens.resize(static_cast<size_t>(written));
    return tokens;
}

std::vector<llama_token> tokenize(const llama_vocab * vocab, const std::string & text) {
    return tokenize_text(vocab, text, true);
}

struct Candidate {
    llama_token id;
    float logit;
    float prob;
    std::string piece;
};

struct SequenceScore {
    float sum_logprob;
    float avg_logprob;
    int token_count;
};

std::string predict_json(const std::string & prompt, int top_k) {
    if (!g_causal_model) {
        set_error("Causal model is not loaded");
        return "[]";
    }
    const llama_vocab * vocab = llama_model_get_vocab(g_causal_model);
    auto tokens = tokenize(vocab, prompt);
    if (tokens.empty()) {
        set_error("Tokenization returned no tokens");
        return "[]";
    }

    llama_context_params cp = llama_context_default_params();
    cp.n_ctx = std::max<uint32_t>(256, static_cast<uint32_t>(tokens.size() + 16));
    cp.n_batch = static_cast<uint32_t>(tokens.size());
    cp.n_ubatch = cp.n_batch;
    cp.n_threads = 4;
    cp.n_threads_batch = 4;
    cp.no_perf = true;
    llama_context * ctx = llama_init_from_model(g_causal_model, cp);
    if (!ctx) {
        set_error("Could not create causal llama_context");
        return "[]";
    }

    llama_batch batch = llama_batch_get_one(tokens.data(), static_cast<int32_t>(tokens.size()));
    const int decode_status = llama_decode(ctx, batch);
    if (decode_status != 0) {
        llama_free(ctx);
        set_error("llama_decode failed for causal model: " + std::to_string(decode_status));
        return "[]";
    }

    const float * logits = llama_get_logits_ith(ctx, -1);
    if (!logits) {
        llama_free(ctx);
        set_error("No logits returned by llama.cpp");
        return "[]";
    }

    const int n_vocab = llama_vocab_n_tokens(vocab);
    float max_logit = -INFINITY;
    for (int i = 0; i < n_vocab; ++i) max_logit = std::max(max_logit, logits[i]);
    double denom = 0.0;
    for (int i = 0; i < n_vocab; ++i) denom += std::exp(static_cast<double>(logits[i] - max_logit));

    std::vector<int> ids(static_cast<size_t>(n_vocab));
    std::iota(ids.begin(), ids.end(), 0);
    const int scan_count = std::min(n_vocab, std::max(top_k * 12, 64));
    std::partial_sort(ids.begin(), ids.begin() + scan_count, ids.end(), [&](int a, int b) {
        return logits[a] > logits[b];
    });

    std::vector<Candidate> candidates;
    candidates.reserve(static_cast<size_t>(top_k));
    for (int rank = 0; rank < scan_count && static_cast<int>(candidates.size()) < top_k; ++rank) {
        llama_token id = static_cast<llama_token>(ids[rank]);
        if (llama_vocab_is_eog(vocab, id)) continue;
        std::string piece = token_piece(vocab, id);
        if (piece.empty() || !is_valid_utf8(piece) || piece.find('\0') != std::string::npos) continue;
        const float prob = static_cast<float>(std::exp(static_cast<double>(logits[id] - max_logit)) / denom);
        candidates.push_back({id, logits[id], prob, piece});
    }
    llama_free(ctx);

    std::ostringstream json;
    json << '[';
    for (size_t i = 0; i < candidates.size(); ++i) {
        if (i) json << ',';
        const auto & c = candidates[i];
        json << "{\"id\":" << c.id
             << ",\"piece\":\"" << json_escape(c.piece) << "\""
             << ",\"logit\":" << c.logit
             << ",\"prob\":" << c.prob << '}';
    }
    json << ']';
    return json.str();
}

bool score_one_continuation(
    const std::string & prompt,
    const std::string & continuation,
    SequenceScore & out
) {
    if (!g_causal_model) {
        set_error("Causal model is not loaded");
        return false;
    }

    const llama_vocab * vocab = llama_model_get_vocab(g_causal_model);
    auto prompt_tokens = tokenize_text(vocab, prompt, true);
    auto continuation_tokens = tokenize_text(vocab, continuation, false);
    if (prompt_tokens.empty()) {
        set_error("Prompt tokenization returned no tokens");
        return false;
    }
    if (continuation_tokens.empty()) {
        set_error("Continuation tokenization returned no tokens");
        return false;
    }

    std::vector<llama_token> tokens;
    tokens.reserve(prompt_tokens.size() + continuation_tokens.size());
    tokens.insert(tokens.end(), prompt_tokens.begin(), prompt_tokens.end());
    tokens.insert(tokens.end(), continuation_tokens.begin(), continuation_tokens.end());

    llama_context_params cp = llama_context_default_params();
    cp.n_ctx = std::max<uint32_t>(512, static_cast<uint32_t>(tokens.size() + 16));
    cp.n_batch = static_cast<uint32_t>(tokens.size());
    cp.n_ubatch = cp.n_batch;
    cp.n_threads = 4;
    cp.n_threads_batch = 4;
    cp.no_perf = true;

    llama_context * ctx = llama_init_from_model(g_causal_model, cp);
    if (!ctx) {
        set_error("Could not create continuation scoring context");
        return false;
    }

    llama_batch batch = llama_batch_init(static_cast<int32_t>(tokens.size()), 0, 1);
    batch.n_tokens = static_cast<int32_t>(tokens.size());
    for (int32_t i = 0; i < batch.n_tokens; ++i) {
        batch.token[i] = tokens[static_cast<size_t>(i)];
        batch.pos[i] = i;
        batch.n_seq_id[i] = 1;
        batch.seq_id[i][0] = 0;
        // Request logits at every position because each continuation token is scored
        // from the preceding position's vocabulary distribution.
        batch.logits[i] = true;
    }

    const int decode_status = llama_decode(ctx, batch);
    if (decode_status != 0) {
        llama_batch_free(batch);
        llama_free(ctx);
        set_error("llama_decode failed while scoring continuation: " + std::to_string(decode_status));
        return false;
    }

    const int n_vocab = llama_vocab_n_tokens(vocab);
    double sum_logprob = 0.0;
    for (size_t j = 0; j < continuation_tokens.size(); ++j) {
        const int predictor_position = static_cast<int>(prompt_tokens.size() + j - 1);
        const float * logits = llama_get_logits_ith(ctx, predictor_position);
        if (!logits) {
            llama_batch_free(batch);
            llama_free(ctx);
            set_error("Missing intermediate logits while scoring continuation");
            return false;
        }

        float max_logit = -INFINITY;
        for (int token = 0; token < n_vocab; ++token) {
            max_logit = std::max(max_logit, logits[token]);
        }
        double denom = 0.0;
        for (int token = 0; token < n_vocab; ++token) {
            denom += std::exp(static_cast<double>(logits[token] - max_logit));
        }

        const llama_token target = continuation_tokens[j];
        const double logprob = static_cast<double>(logits[target] - max_logit) - std::log(denom);
        sum_logprob += logprob;
    }

    llama_batch_free(batch);
    llama_free(ctx);

    out.sum_logprob = static_cast<float>(sum_logprob);
    out.avg_logprob = static_cast<float>(sum_logprob / static_cast<double>(continuation_tokens.size()));
    out.token_count = static_cast<int>(continuation_tokens.size());
    return true;
}

std::string score_continuations_json(
    const std::string & prompt,
    const std::vector<std::string> & continuations
) {
    std::vector<SequenceScore> scores;
    scores.reserve(continuations.size());
    for (const auto & continuation : continuations) {
        SequenceScore score{};
        if (!score_one_continuation(prompt, continuation, score)) {
            return "[]";
        }
        scores.push_back(score);
    }

    std::ostringstream json;
    json << '[';
    for (size_t i = 0; i < scores.size(); ++i) {
        if (i) json << ',';
        const auto & score = scores[i];
        json << "{\"sum_logprob\":" << score.sum_logprob
             << ",\"avg_logprob\":" << score.avg_logprob
             << ",\"tokens\":" << score.token_count << '}';
    }
    json << ']';
    return json.str();
}

std::vector<float> compute_embedding(const std::string & text) {
    if (!g_embedding_model) {
        set_error("Embedding model is not loaded");
        return {};
    }
    const llama_vocab * vocab = llama_model_get_vocab(g_embedding_model);
    auto tokens = tokenize(vocab, text);
    if (tokens.empty()) {
        set_error("Embedding tokenization returned no tokens");
        return {};
    }

    llama_context_params cp = llama_context_default_params();
    cp.n_ctx = std::max<uint32_t>(128, static_cast<uint32_t>(tokens.size() + 8));
    cp.n_batch = static_cast<uint32_t>(tokens.size());
    cp.n_ubatch = cp.n_batch;
    cp.n_threads = 4;
    cp.n_threads_batch = 4;
    cp.embeddings = true;
    cp.pooling_type = LLAMA_POOLING_TYPE_UNSPECIFIED;
    cp.attention_type = LLAMA_ATTENTION_TYPE_UNSPECIFIED;
    cp.no_perf = true;

    llama_context * ctx = llama_init_from_model(g_embedding_model, cp);
    if (!ctx) {
        set_error("Could not create embedding llama_context");
        return {};
    }

    llama_batch batch = llama_batch_init(static_cast<int32_t>(tokens.size()), 0, 1);
    batch.n_tokens = static_cast<int32_t>(tokens.size());
    for (int32_t i = 0; i < batch.n_tokens; ++i) {
        batch.token[i] = tokens[static_cast<size_t>(i)];
        batch.pos[i] = i;
        batch.n_seq_id[i] = 1;
        batch.seq_id[i][0] = 0;
        batch.logits[i] = true;
    }

    const int decode_status = llama_decode(ctx, batch);
    if (decode_status != 0) {
        llama_batch_free(batch);
        llama_free(ctx);
        set_error("llama_decode failed for embedding model: " + std::to_string(decode_status));
        return {};
    }

    const float * emb = nullptr;
    if (llama_pooling_type(ctx) == LLAMA_POOLING_TYPE_NONE) {
        emb = llama_get_embeddings_ith(ctx, batch.n_tokens - 1);
    } else {
        emb = llama_get_embeddings_seq(ctx, 0);
        if (!emb) emb = llama_get_embeddings_ith(ctx, batch.n_tokens - 1);
    }

    if (!emb) {
        llama_batch_free(batch);
        llama_free(ctx);
        set_error("No embedding vector returned by llama.cpp");
        return {};
    }

    const int dim = llama_model_n_embd_out(g_embedding_model);
    std::vector<float> result(static_cast<size_t>(dim));
    double norm_sq = 0.0;
    for (int i = 0; i < dim; ++i) norm_sq += static_cast<double>(emb[i]) * emb[i];
    const double inv_norm = norm_sq > 1e-20 ? 1.0 / std::sqrt(norm_sq) : 1.0;
    for (int i = 0; i < dim; ++i) result[static_cast<size_t>(i)] = static_cast<float>(emb[i] * inv_norm);

    llama_batch_free(batch);
    llama_free(ctx);
    return result;
}
} // namespace

extern "C" JNIEXPORT jboolean JNICALL
Java_com_aivectorgame_app_ai_NativeEngine_nativeLoadCausal(JNIEnv * env, jobject, jstring path) {
    std::lock_guard<std::mutex> lock(g_mutex);
    g_last_error.clear();
    return load_model(jstring_to_utf8(env, path), g_causal_model) ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_aivectorgame_app_ai_NativeEngine_nativeLoadEmbedding(JNIEnv * env, jobject, jstring path) {
    std::lock_guard<std::mutex> lock(g_mutex);
    g_last_error.clear();
    return load_model(jstring_to_utf8(env, path), g_embedding_model) ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_aivectorgame_app_ai_NativeEngine_nativePredictTopTokens(JNIEnv * env, jobject, jstring prompt, jint top_k) {
    std::lock_guard<std::mutex> lock(g_mutex);
    g_last_error.clear();
    const std::string result = predict_json(jstring_to_utf8(env, prompt), std::max(1, static_cast<int>(top_k)));
    return env->NewStringUTF(result.c_str());
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_aivectorgame_app_ai_NativeEngine_nativeScoreContinuations(
    JNIEnv * env,
    jobject,
    jstring prompt,
    jobjectArray candidates
) {
    std::lock_guard<std::mutex> lock(g_mutex);
    g_last_error.clear();

    std::vector<std::string> values;
    if (candidates) {
        const jsize count = env->GetArrayLength(candidates);
        values.reserve(static_cast<size_t>(count));
        for (jsize i = 0; i < count; ++i) {
            auto value = static_cast<jstring>(env->GetObjectArrayElement(candidates, i));
            values.push_back(jstring_to_utf8(env, value));
            env->DeleteLocalRef(value);
        }
    }

    if (values.empty()) {
        set_error("No continuation candidates supplied");
        return env->NewStringUTF("[]");
    }

    const std::string result = score_continuations_json(jstring_to_utf8(env, prompt), values);
    return env->NewStringUTF(result.c_str());
}

extern "C" JNIEXPORT jfloatArray JNICALL
Java_com_aivectorgame_app_ai_NativeEngine_nativeEmbedding(JNIEnv * env, jobject, jstring text) {
    std::lock_guard<std::mutex> lock(g_mutex);
    g_last_error.clear();
    const auto values = compute_embedding(jstring_to_utf8(env, text));
    jfloatArray array = env->NewFloatArray(static_cast<jsize>(values.size()));
    if (!values.empty()) env->SetFloatArrayRegion(array, 0, static_cast<jsize>(values.size()), values.data());
    return array;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_aivectorgame_app_ai_NativeEngine_nativeLastError(JNIEnv * env, jobject) {
    std::lock_guard<std::mutex> lock(g_mutex);
    return env->NewStringUTF(g_last_error.c_str());
}
