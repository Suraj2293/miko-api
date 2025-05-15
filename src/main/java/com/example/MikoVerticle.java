package com.example;

import com.example.config.DbConfigProvider;
import com.example.config.MysqlDBProvider;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.SQLClient;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;

import static com.example.constant.MikoConstants.*;

public class MikoVerticle extends AbstractVerticle {

    private SQLClient jdbc;


    public static void main(String[] args) {
        Vertx v = Vertx.vertx();
        v.deployVerticle(new MikoVerticle());

    }

    @Override
    public void start(Promise<Void> startPromise) {

        DbConfigProvider configProvider = new MysqlDBProvider(vertx, "mysql_config.yaml");

        configProvider.loadConfig("db").onSuccess(jdbcConfig -> {
            jdbc = JDBCClient.createShared(vertx, jdbcConfig);

            Router router = Router.router(vertx);

            router.get("/health").handler(ctx -> ctx.response().end("Service is up"));

            router.route().handler(BodyHandler.create());

            // Post API to create USER
            router.post("/users").handler(ctx -> {
                JsonObject body = ctx.getBodyAsJson();
                String name = body.getString("name");
                int age = body.getInteger("age");

                jdbc.getConnection(ar -> {
                    if (ar.failed()) {
                        ctx.response().setStatusCode(500).end(DB_ERROR);
                        return;
                    }

                    SQLConnection conn = ar.result();
                    conn.updateWithParams(INSERT,
                            new JsonArray().add(name).add(age),
                            res -> {
                                if (res.succeeded()) {
                                    ctx.response().setStatusCode(201).end(USER_CREATED);
                                } else {
                                    ctx.response().setStatusCode(500).end(INSERT_FAILED);
                                }
                                conn.close();
                            });
                });
            });

            // Get API to fetch users
            router.get("/users").handler(ctx -> {
                jdbc.getConnection(ar -> {
                    if (ar.failed()) {
                        ctx.response().setStatusCode(500).end(DB_ERROR);
                        return;
                    }

                    SQLConnection conn = ar.result();
                    conn.query(FETCH, res -> {
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

            // Testing Miko
            router.get("/hello").handler(ctx -> {
                ctx.response()
                        .putHeader("content-type", "text/plain")
                        .end("Testing Miko..");
            });

            // server start
            vertx.createHttpServer()
                    .requestHandler(router)
                    .listen(8888, http -> {
                        if (http.succeeded()) {
                            startPromise.complete();
                            System.out.println("Server started on port 8888");
                        } else {
                            startPromise.fail(http.cause());
                        }
                    });
        }).onFailure(startPromise::fail);
    }

}