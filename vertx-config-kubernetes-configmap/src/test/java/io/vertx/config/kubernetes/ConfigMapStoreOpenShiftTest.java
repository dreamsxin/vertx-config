package io.vertx.config.kubernetes;

import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import io.fabric8.openshift.client.server.mock.OpenShiftMockServer;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
@RunWith(VertxUnitRunner.class)
public class ConfigMapStoreOpenShiftTest extends ConfigMapStoreTest {

  @Before
  public void setUp(TestContext tc) throws MalformedURLException {
    vertx = Vertx.vertx();
    vertx.exceptionHandler(tc.exceptionHandler());

    ConfigMap map1 = new ConfigMapBuilder().withMetadata(new ObjectMetaBuilder().withName("my-config-map").build())
      .addToData("my-app-json", SOME_JSON)
      .addToData("my-app-props", SOME_PROPS)
      .build();

    Map<String, String> data = new LinkedHashMap<>();
    data.put("key", "value");
    data.put("bool", "true");
    data.put("count", "3");
    ConfigMap map2 = new ConfigMapBuilder().withMetadata(new ObjectMetaBuilder().withName("my-config-map-2").build())
      .withData(data)
      .build();

    ConfigMap map3 = new ConfigMapBuilder().withMetadata(new ObjectMetaBuilder().withName("my-config-map-x").build())
      .addToData("my-app-json", SOME_JSON)
      .build();

    Secret secret = new SecretBuilder().withMetadata(new ObjectMetaBuilder().withName("my-secret").build())
      .addToData("password", "secret")
      .build();

    server = new OpenShiftMockServer(false);

    server.expect().get().withPath("/api/v1/namespaces/default/configmaps").andReturn(200, new
      ConfigMapListBuilder().addToItems(map1, map2).build()).always();
    server.expect().get().withPath("/api/v1/namespaces/my-project/configmaps").andReturn(200, new
      ConfigMapListBuilder().addToItems(map3).build()).always();

    server.expect().get().withPath("/api/v1/namespaces/default/configmaps/my-config-map")
      .andReturn(200, map1).always();
    server.expect().get().withPath("/api/v1/namespaces/default/configmaps/my-config-map-2")
      .andReturn(200, map2).always();

    server.expect().get().withPath("/api/v1/namespaces/my-project/configmaps/my-config-map-x")
      .andReturn(200, map3).always();

    server.expect().get().withPath("/api/v1/namespaces/default/configmaps/my-unknown-config-map")
      .andReturn(500, null).always();
    server.expect().get().withPath("/api/v1/namespaces/default/configmaps/my-unknown-map")
      .andReturn(500, null).always();

    server.expect().get().withPath("/api/v1/namespaces/my-project/secrets/my-secret").andReturn(200, secret)
      .always();

    server.init();
    client = server.createClient();
    port = new URL(client.getConfiguration().getMasterUrl()).getPort();
  }

}
