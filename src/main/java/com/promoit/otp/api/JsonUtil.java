package com.promoit.otp.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.lang.reflect.Type;

public class JsonUtil {
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public static Gson getGson() { return gson; }
    public static String toJson(Object obj) { return gson.toJson(obj); }
    public static <T> T fromJson(String json, Class<T> clazz) { return gson.fromJson(json, clazz); }

    // Новый метод для Type
    public static <T> T fromJson(String json, Type type) {
        return gson.fromJson(json, type);
    }
}