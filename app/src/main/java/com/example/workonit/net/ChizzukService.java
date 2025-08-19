package com.example.workonit.net;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.example.workonit.R;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.Call;
import okhttp3.Callback;
import com.example.workonit.net.HttpClients;

public class ChizzukService {

    public interface ChizzukCallback {
        void onSuccess(String message);
        void onError(String error);
    }


    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final Handler main = new Handler(Looper.getMainLooper());

    public static void generateChizzuk(Context context,
                                       String goalName,
                                       boolean isPositive,
                                       String notes,
                                       ChizzukCallback cb) {

        // ✅ get API key from resources (set via resValue in build.gradle)
        String apiKey = context.getString(R.string.openai_api_key);
        if (apiKey == null || apiKey.isEmpty()) {
            postError(cb, "missing OPENAI_API_KEY");
            return;
        }

        try {
            JSONObject root = new JSONObject();
            root.put("model", "gpt-4o-mini");
            root.put("temperature", 0.9);

            JSONArray messages = new JSONArray();

            messages.put(new JSONObject()
                    .put("role", "system")
                    .put("content",
                            "you give short, warm Jewish-style 'chizzuk' (encouragement). " +
                                    "2–4 sentences. practical, kind, and motivating. " +
                                    "no emojis, no quotes, no markdown."));

            StringBuilder user = new StringBuilder();
            user.append("Goal: ").append(goalName).append(". ");
            user.append("Type: ").append(isPositive ? "positive (do the thing)" : "negative (avoid the thing)").append(". ");
            if (notes != null && !notes.trim().isEmpty()) {
                user.append("User notes/context: ").append(notes.trim()).append(". ");
            }
            user.append("please respond with a short chizzuk message tailored to this goal. Make sure it is relatable and will actually encourage the user to continue pursuing it.");

            messages.put(new JSONObject()
                    .put("role", "user")
                    .put("content", user.toString()));

            root.put("messages", messages);

            RequestBody body = RequestBody.create(root.toString(), JSON);
            Request request = new Request.Builder()
                    .url("https://api.openai.com/v1/chat/completions")
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build();

            OkHttpClient client = HttpClients.get(context);



            client.newCall(request).enqueue(new Callback() {
                @Override public void onFailure(Call call, IOException e) {
                    postError(cb, "network error: " + e.getMessage());
                }

                @Override public void onResponse(Call call, Response response) {
                    try (Response r = response) {
                        if (!r.isSuccessful()) {
                            postError(cb, "api error: " + r.code() + " " + r.message());
                            return;
                        }
                        String resp = r.body() != null ? r.body().string() : "";
                        JSONObject json = new JSONObject(resp);
                        JSONArray choices = json.optJSONArray("choices");
                        if (choices == null || choices.length() == 0) {
                            postError(cb, "empty response");
                            return;
                        }
                        JSONObject msg = choices.getJSONObject(0).getJSONObject("message");
                        String content = msg.optString("content", "").trim();
                        if (content.isEmpty()) {
                            postError(cb, "no content");
                            return;
                        }
                        postSuccess(cb, content);
                    } catch (Exception ex) {
                        postError(cb, "parse error: " + ex.getMessage());
                    }
                }
            });

        } catch (Exception ex) {
            postError(cb, "build request error: " + ex.getMessage());
        }
    }

    private static void postSuccess(ChizzukCallback cb, String m) {
        main.post(() -> cb.onSuccess(m));
    }
    private static void postError(ChizzukCallback cb, String e) {
        main.post(() -> cb.onError(e));
    }
}