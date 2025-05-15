import com.example.MikoVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
public class MikoVerticleTest {
    private static final int PORT = 8888;

    @BeforeAll
    static void deploy_verticle(Vertx vertx, VertxTestContext testContext) {
        vertx.deployVerticle(new MikoVerticle(), testContext.succeedingThenComplete());
    }

    @Test
    void helloTest(Vertx vertx, VertxTestContext testContext) {
        WebClient client = WebClient.create(vertx);

        client.get(PORT, "localhost", "/hello").send().onComplete(res -> {
            if (res.succeeded() && res.result().statusCode() == 200) {
                testContext.completeNow();
            } else {
                testContext.failNow(res.cause());
            }
        });
    }

    @Test
    void createUserTest(Vertx vertx, VertxTestContext ctx) {
        WebClient client = WebClient.create(vertx, new WebClientOptions().setDefaultPort(PORT));

        JsonObject user = new JsonObject().put("age", 10).put("name", "Suraj");

        client.post("/users").sendJsonObject(user).onSuccess(response -> {
            Assertions.assertEquals(201, response.statusCode());
            Assertions.assertEquals("User created", response.bodyAsString());
            ctx.completeNow();
        }).onFailure(ctx::failNow);
    }

}
