/*
 * Copyright 2017 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package io.vertx.mddwg.oxappstream;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.CompletableHelper;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.ext.auth.jdbc.JDBCAuth;
import io.vertx.reactivex.ext.jdbc.JDBCClient;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.handler.*;
import io.vertx.reactivex.ext.web.sstore.LocalSessionStore;
import io.vertx.reactivex.ext.web.templ.freemarker.FreeMarkerTemplateEngine;
import org.flywaydb.core.Flyway;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * @author Thomas Segismont
 */
public class OxAppVerticle extends AbstractVerticle {

  private DatasourceConfig datasourceConfig;
  private JDBCClient dbClient;
  private Properties dbQueries;
  private JDBCAuth authProvider;
  private FreeMarkerTemplateEngine templateEngine;

  @Override
  public void start(Future<Void> startFuture) throws Exception {
    datasourceConfig = new DatasourceConfig(config().getJsonObject("datasource", new JsonObject()));
    dbClient = JDBCClient.createShared(vertx, datasourceConfig.toJson(), "OxVertxAppDS");
    templateEngine = FreeMarkerTemplateEngine.create(vertx);

    Completable databaseSetup = updateDB()
      .andThen(loadDbQueries()).doOnSuccess(props -> dbQueries = props)
      .ignoreElement();

    databaseSetup
      .andThen(Completable.fromAction(() -> setupAuthProvider()))
      .andThen(Completable.defer(() -> setupWebServer()))
      .subscribe(CompletableHelper.toObserver(startFuture));
  }

  private Completable updateDB() {
    return vertx.rxExecuteBlocking(future -> {
      Flyway flyway = new Flyway();
      flyway.setDataSource(datasourceConfig.getUrl(), datasourceConfig.getUser(), datasourceConfig.getPassword());
//      flyway.migrate();
      future.complete(new Object());
    }).ignoreElement();
  }

  private Single<Properties> loadDbQueries() {
    return vertx.<Properties>rxExecuteBlocking(fut -> {
      Properties properties = new Properties();
      try (InputStream is = getClass().getClassLoader().getResourceAsStream("db/queries.xml")) {
        properties.loadFromXML(is);
        fut.complete(properties);
      } catch (IOException e) {
        fut.fail(e);
      }
    }).toSingle();
  }

  private void setupAuthProvider() {
    authProvider = JDBCAuth.create(vertx, dbClient)
      .setAuthenticationQuery(dbQueries.getProperty("authenticateUser"))
      .setRolesQuery(dbQueries.getProperty("findRolesByUser"))
      .setPermissionsQuery(dbQueries.getProperty("findPermissionsByUser"));
  }

  private Completable setupWebServer() {
    Router router = Router.router(vertx);

//    SockJSHandler sockJSHandler = SockJSHandler.create(vertx);
//    sockJSHandler.bridge(new BridgeOptions()
//      .addOutboundPermitted(new PermittedOptions().setAddressRegex("album\\.\\d+\\.comments\\.new")));
//    router.route("/eventbus/*").handler(sockJSHandler);

    router.route().handler(BodyHandler.create());

    router.route().handler(CookieHandler.create());
    router.route().handler(SessionHandler.create(LocalSessionStore.create(vertx)));
    router.route().handler(UserSessionHandler.create(authProvider));

    IndexHandler indexHandler = new IndexHandler(dbClient, dbQueries, templateEngine);
    router.get("/").handler(indexHandler);
    router.get("/index.html").handler(indexHandler);

    router.get("/login").handler(new ReturnUrlHandler());
    router.get("/login").handler(rc -> templateEngine.rxRender(rc.data(), "templates/login")
      .subscribe(rc.response()::end, rc::fail));
    router.post("/login").handler(FormLoginHandler.create(authProvider));

    router.get("/add_user").handler(rc -> templateEngine.rxRender(rc.data(), "templates/add_user")
      .subscribe(rc.response()::end, rc::fail));
    router.post("/add_user").handler(new AddUserHandler(dbClient, dbQueries, authProvider));

    router.route().handler(StaticHandler.create());

    Completable completable = vertx.createHttpServer()
      .requestHandler(router::accept)
      .rxListen(8080)
      .ignoreElement();
    return completable;
  }

}
