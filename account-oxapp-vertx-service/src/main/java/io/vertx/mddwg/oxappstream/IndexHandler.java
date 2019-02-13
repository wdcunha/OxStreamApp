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

import io.reactivex.Single;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.ext.jdbc.JDBCClient;
import io.vertx.reactivex.ext.sql.SQLConnection;
import io.vertx.reactivex.ext.sql.SQLRowStream;
import io.vertx.reactivex.ext.web.RoutingContext;
import io.vertx.reactivex.ext.web.templ.freemarker.FreeMarkerTemplateEngine;

import java.util.Properties;

/**
 * @author Thomas Segismont
 */
public class IndexHandler implements Handler<RoutingContext> {

  private final JDBCClient dbClient;
  private final String findAllUsers;
  private final FreeMarkerTemplateEngine templateEngine;

  public IndexHandler(JDBCClient dbClient, Properties sqlQueries, FreeMarkerTemplateEngine templateEngine) {
    this.dbClient = dbClient;
    findAllUsers = sqlQueries.getProperty("findAllUsers");
    this.templateEngine = templateEngine;
  }


  @Override
  public void handle(RoutingContext rc) {
    dbClient.rxGetConnection().flatMap(sqlConnection -> {
      return findUsers(sqlConnection).doAfterTerminate(sqlConnection::close);
    }).flatMap(users -> {
      rc.put("users", users);
      return templateEngine.rxRender(rc.data(), "templates/index");
    }).subscribe(rc.response()::end, rc::fail);
  }

  private Single<JsonArray> findUsers(SQLConnection sqlConnection) {
    return sqlConnection.rxQueryStream(findAllUsers)
            .flatMapObservable(SQLRowStream::toObservable)
            .map(row -> new JsonObject().put("username", row.getString(0)))
            .collect(JsonArray::new, JsonArray::add);
  }
}
