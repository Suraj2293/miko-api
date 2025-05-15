package com.example.config;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;

public interface DbConfigProvider {
    Future<JsonObject> loadConfig(String dbKey);
}
