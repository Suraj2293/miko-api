package com.example;

import com.example.config.DbConfigProvider;
import com.example.config.MysqlDBProvider;
import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.SQLClient;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;

import static com.example.constant.MikoConstants.QUERY_FAILED;

public class MainVerticle extends AbstractVerticle {

    private SQLClient jdbc;

    public static void main(String[] args) {
        Vertx v = Vertx.vertx();
        v.deployVerticle(new MainVerticle());

    }
    @Override
    public void start(Promise<Void> startPromise) {

        DbConfigProvider configProvider = new MysqlDBProvider(vertx, "mysql_config.yaml");
        // Load YAML config
        ConfigStoreOptions fileStore = new ConfigStoreOptions()
                .setType("file")
                .setFormat("yaml")
                .setConfig(new JsonObject().put("path", "mysql_config.yaml"));

        ConfigRetrieverOptions options = new ConfigRetrieverOptions().addStore(fileStore);

        ConfigRetriever retriever = ConfigRetriever.create(vertx, options);

        retriever.getConfig(re -> {
            if (re.failed()) {
                startPromise.fail("Failed to load config");
                return;
            }
        JsonObject config = re.result().getJsonObject("db");
//        JsonObject dbConfig = new JsonObject()
//            .put("url", "jdbc:mysql://localhost:3306/practise")
//            .put("driver_class", "com.mysql.cj.jdbc.Driver")
//            .put("user", "root")
//            .put("password", "root");

        jdbc = JDBCClient.createShared(vertx, config);

        Router router = Router.router(vertx);
        router.route().handler(BodyHandler.create());

        router.post("/users").handler(ctx -> {
            JsonObject body = ctx.getBodyAsJson();
            String name = body.getString("name");
            int age = body.getInteger("age");

            jdbc.getConnection(ar -> {
                if (ar.failed()) {
                    ctx.response().setStatusCode(500).end("DB Connection failed");
                    return;
                }

                SQLConnection conn = ar.result();
                conn.updateWithParams("INSERT INTO users (name, age) VALUES (?, ?)",
                    new JsonArray().add(name).add(age),
                    res -> {
                        if (res.succeeded()) {
                            ctx.response().setStatusCode(201).end("User created");
                        } else {
                            ctx.response().setStatusCode(500).end("Insert failed");
                        }
                        conn.close();
                    });
            });
        });

        router.get("/users").handler(ctx -> {
            jdbc.getConnection(ar -> {
                if (ar.failed()) {
                    ctx.response().setStatusCode(500).end("DB Connection failed");
                    return;
                }

                SQLConnection conn = ar.result();
                conn.query("SELECT * FROM users", res -> {
                    if (res.succeeded()) {
                        ResultSet resultSet = res.result();
                        ctx.response()
                            .putHeader("Content-Type", "application/json")
                            .end(resultSet.getRows().toString());
                    } else {
                        ctx.response().setStatusCode(500).end(QUERY_FAILED);
                    }
                    conn.close();
                });
            });
        });

        router.put("/users/:id").handler(ctx -> {
            int id = Integer.parseInt(ctx.pathParam("id"));
            JsonObject body = ctx.getBodyAsJson();
            String name = body.getString("name");
            int age = body.getInteger("age");

            jdbc.getConnection(ar -> {
                if (ar.failed()) {
                    ctx.response().setStatusCode(500).end("DB Connection failed");
                    return;
                }

                SQLConnection conn = ar.result();
                conn.updateWithParams("UPDATE users SET name = ?, age = ? WHERE id = ?",
                    new JsonArray().add(name).add(age).add(id),
                    res -> {
                        if (res.succeeded()) {
                            ctx.response().end("User updated");
                        } else {
                            ctx.response().setStatusCode(500).end("Update failed");
                        }
                        conn.close();
                    });
            });
        });

        router.get("/hello").handler(ctx -> {
            ctx.response()
                    .putHeader("content-type", "text/plain")
                    .end("Testing Miko..");
        });

        vertx.createHttpServer()
            .requestHandler(router)
            .listen(8888, http -> {
                if (http.succeeded()) {
                    startPromise.complete();
                    System.out.println("HTTP server started on port 8888");
                } else {
                    startPromise.fail(http.cause());
                }
            });
        });
    }

}