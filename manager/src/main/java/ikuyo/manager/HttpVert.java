package ikuyo.manager;

import ikuyo.api.User;
import ikuyo.utils.AsyncVerticle;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.Tuple;
import java.util.UUID;

public class HttpVert extends AsyncVerticle {
    HttpServer server;
    Router router;
    PgPool pool;
    EventBus eb;

    @Override
    public void startAsync() {
        eb = vertx.eventBus();
        pool = PgPool.pool(vertx, new PoolOptions());
        server = vertx.createHttpServer();
        router = Router.router(vertx);
        router.route().handler(CorsHandler.create()).handler(BodyHandler.create());
        router.get("/").handler(this::indexPage);
        router.post("/login").handler(this::loginHandler);
        router.get("/endpoint").handler(this::getEndpointHandler);
        server.requestHandler(router);
        await(server.listen(8070));
        System.out.println("listening...");
    }

    void indexPage(RoutingContext req) {
        System.out.println(Thread.currentThread().getName());
        //language=HTML
        await(req.response().end("""
            <!DOCTYPE html>
            <html lang="zh-cn">
            <body>
            <h1>hello</h1>
            </body>
            </html>
            """));
    }

    void loginHandler(RoutingContext req) {
        var name = req.request().getHeader("name");
        var pwd = req.request().getHeader("pwd");
        var user = User.getUserByName(pool, name);
        if (user == null || !user.pwd().equals(pwd)) {
            await(req.response().setStatusCode(401).end("no such a user or password wrong"));
            return;
        }
        var token = UUID.randomUUID().toString();
        await(pool.preparedQuery("""
            update "user" set token = $2 where name = $1
            """).execute(Tuple.of(name, token)));
        await(req.response().end(token));
    }

    void getEndpointHandler(RoutingContext req) {
        var token = req.request().getHeader("token");
        var user = User.getUserByToken(pool, token);
        if (user == null) {
            await(req.response().setStatusCode(401).end("no such a user"));
            return;
        }
        try {
            var pingRes = (JsonObject)await(eb.request("star." + user.star(), JsonObject.of(
                    "type", "ping"), new DeliveryOptions().setSendTimeout(1000))).body();
            await(req.response().end(pingRes.getString("endpoint")));
        } catch (Exception e) {
            try {
                var loadRes = (JsonObject) await(eb.request("star.none", JsonObject.of(
                        "type", "load", "id", user.star()
                ), new DeliveryOptions().setSendTimeout(1000))).body();
                await(req.response().end(loadRes.getString("endpoint")));
            } catch (Exception ex) {
                await(req.response().setStatusCode(503).end("server is busy"));
            }
        }
    }
}
