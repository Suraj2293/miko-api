package com.example.config;

import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

public class MysqlDBProvider implements DbConfigProvider{

    private final Vertx vertx;
    private final String path;

    public MysqlDBProvider(Vertx vertx, String path) {
        this.vertx = vertx;
        this.path = path;
    }

    @Override
    public Future<JsonObject> loadConfig(String dbKey) {
        Promise<JsonObject> promise = Promise.promise();

        ConfigStoreOptions fileStore = new ConfigStoreOptions()
                .setType("file")
                .setFormat("yaml")
                .setConfig(new JsonObject().put("path", path));

        ConfigRetrieverOptions options = new ConfigRetrieverOptions().addStore(fileStore);
        ConfigRetriever retriever = ConfigRetriever.create(vertx, options);

        retriever.getConfig(ar -> {
            if (ar.succeeded()) {
                JsonObject jdbcConfig = ar.result().getJsonObject(dbKey);
                if (jdbcConfig == null) {
                    promise.fail("No config found for key: " + dbKey);
                } else {
//                    JsonObject jdbcConfig = ar.result().getJsonObject("db");
//                    JsonObject jdbcConfig = new JsonObject()
//                            .put("url", config.getUrl())
//                            .put("driver_class", config.getDriver_class())
//                            .put("user", config.getUser())
//                            .put("password", config.getPassword());
                    promise.complete(jdbcConfig);
                }
            } else {
                promise.fail(ar.cause());
            }
        });

        return promise.future();
    }

}
