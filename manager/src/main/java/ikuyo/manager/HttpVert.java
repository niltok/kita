package ikuyo.manager;

import ikuyo.api.User;
import io.vertx.await.Async;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.Cookie;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.Tuple;

import java.util.UUID;

import static io.vertx.await.Async.await;

public class HttpVert extends AbstractVerticle {
    Async async;
    HttpServer server;
    Router router;
    PgPool pool;

    @Override
    public void start() throws Exception {
        super.start();
        async = new Async(vertx);
        System.out.println(Thread.currentThread().getName());
        async.run(v -> {
            System.out.println(Thread.currentThread().getName());
            vertx.runOnContext(vv -> {
                System.out.println(Thread.currentThread().getName());
            });
            startAsync();
        });
    }

    void startAsync() {
        pool = PgPool.pool(vertx, new PoolOptions());
        server = vertx.createHttpServer();
        router = Router.router(vertx);
        router.route().handler(BodyHandler.create()).handler(CorsHandler.create());
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
            await(req.response().setStatusCode(404).end("no such a user or password wrong"));
            return;
        }
        var token = UUID.randomUUID().toString();
        await(pool.preparedQuery("""
            update "user" set token = $2 where name = $1
            """).execute(Tuple.of(name, token)));
        await(req.response().addCookie(Cookie.cookie("token", token)).end(token));
    }

    void getEndpointHandler(RoutingContext req) {
        var token = req.request().getCookie("token").getValue();
        var user = User.getUserByToken(pool, token);
        if (user == null) {
            await(req.response().setStatusCode(404).end("no such a user"));
            return;
        }
        // TODO: look up server by star or load star to a new verticle
    }
}
