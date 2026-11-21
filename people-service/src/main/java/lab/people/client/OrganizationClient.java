package lab.people.client;

import java.io.*;
import java.net.URI;
import java.net.http.*;
import java.nio.file.*;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.time.Duration;
import java.util.List;
import javax.net.ssl.*;
import lab.people.api.ApiException;
import lab.people.api.Validation;
import lab.people.model.Model.*;
import org.springframework.http.*;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.http.converter.xml.Jaxb2RootElementHttpMessageConverter;
import org.springframework.web.client.*;

public final class OrganizationClient {
  private final RestClient http;

  public OrganizationClient(String baseUrl, String caCert) {
    URI base = URI.create(baseUrl.endsWith("/") ? baseUrl : baseUrl + "/");
    if (!"https".equals(base.getScheme()) || base.getHost() == null ||
        base.getUserInfo() != null || base.getQuery() != null ||
        base.getFragment() != null)
      throw new IllegalArgumentException(
          "organizations.url должен быть HTTPS base URL без "
          + "credentials/query/fragment");
    try {
      var builder = HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(3))
                        .followRedirects(HttpClient.Redirect.NEVER);
      if (caCert != null && !caCert.isBlank()) {
        var ks = KeyStore.getInstance(KeyStore.getDefaultType());
        ks.load(null);
        try (var in = Files.newInputStream(Path.of(caCert))) {
          int index = 0;
          for (var certificate :
               CertificateFactory.getInstance("X.509").generateCertificates(in))
            ks.setCertificateEntry("organization-ca-" + index++, certificate);
          if (index == 0)
            throw new IllegalArgumentException("Пустой PEM-сертификат");
        }
        var tm = TrustManagerFactory.getInstance(
            TrustManagerFactory.getDefaultAlgorithm());
        tm.init(ks);
        var ssl = SSLContext.getInstance("TLS");
        ssl.init(null, tm.getTrustManagers(), null);
        builder.sslContext(ssl);
      }
      var factory = new JdkClientHttpRequestFactory(builder.build());
      factory.setReadTimeout(Duration.ofSeconds(8));

      http = RestClient.builder()
                 .baseUrl(base)
                 .requestFactory(factory)
                 .messageConverters(
                     List.of(new Jaxb2RootElementHttpMessageConverter()))
                 .build();
    } catch (Exception e) {
      throw new IllegalStateException(
          "Не удалось загрузить сертификат сервиса организаций", e);
    }
  }

  private record
      Reply(int status, OrganizationData organization, ErrorData error) {}

  private Reply request(HttpMethod method, String path, OrganizationData body) {
    var request =
        http.method(method).uri(path).accept(MediaType.APPLICATION_XML);
    if (body != null)
      request.contentType(MediaType.APPLICATION_XML).body(body);

    try {
      return request.exchange((sent, response) -> {
        int status = response.getStatusCode().value();
        return new Reply(status,
                         status == 200 || status == 201
                             ? response.bodyTo(OrganizationData.class)
                             : null,
                         status == 400 ? response.bodyTo(ErrorData.class)
                                       : null);
      });
    } catch (RestClientException | HttpMessageConversionException e) {
      throw ApiException.unavailable();
    }
  }

  private OrganizationRef reference(OrganizationData data) {
    try {
      var v = new Validation();
      Long id = v.integer(data.id, "id", true);
      String name = v.string(data.name, "name", true, true);
      v.finish();
      if (id < 1 || Boolean.TRUE.equals(data.deleted))
        throw ApiException.unavailable();
      return new OrganizationRef(id, name);
    } catch (RuntimeException e) {
      throw ApiException.unavailable();
    }
  }

  public OrganizationRef find(long id) {
    Reply reply = request(HttpMethod.GET, "organizations/" + id, null);
    if (reply.status == 404)
      return null;
    if (reply.status != 200)
      throw ApiException.unavailable();
    OrganizationRef ref = reference(reply.organization);
    if (ref.id() != id)
      throw ApiException.unavailable();
    return ref;
  }

  public OrganizationRef create(Input input) {
    Reply reply =
        request(HttpMethod.POST, "organizations", new OrganizationData(input));
    if (reply.status == 400) {
      var details = new java.util.ArrayList<Detail>();
      try {
        ErrorData error = reply.error;
        for (ErrorDetail detail : error.errors) {
          if (detail.parameter == null || detail.message == null)
            throw ApiException.unavailable();
          details.add(new Detail(detail.parameter, detail.message));
        }
      } catch (RuntimeException e) {
        throw ApiException.unavailable();
      }
      throw new ApiException(400, "Сервис организаций отклонил данные",
                             details);
    }
    if (reply.status != 201)
      throw ApiException.unavailable();
    return reference(reply.organization);
  }

  public void delete(long id) {
    Reply reply = request(HttpMethod.DELETE, "organizations/" + id, null);
    if (reply.status != 204 && reply.status != 404)
      throw ApiException.unavailable();
  }
}
