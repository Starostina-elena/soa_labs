package lab.people.api;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.List;
import lab.people.model.Model;
import org.springframework.http.MediaType;
import org.springframework.http.converter.xml.Jaxb2RootElementHttpMessageConverter;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.web.filter.OncePerRequestFilter;

public class HttpsOnly extends OncePerRequestFilter {
  private final Jaxb2RootElementHttpMessageConverter converter =
      new Jaxb2RootElementHttpMessageConverter();

  @Override
  protected void doFilterInternal(HttpServletRequest request,
                                  HttpServletResponse response,
                                  FilterChain chain)
      throws ServletException, IOException {
    if (!request.isSecure()) {
      response.setStatus(403);
      converter.write(
          new Model.ErrorData("Доступ разрешён только по HTTPS", List.of()),
          MediaType.APPLICATION_XML, new ServletServerHttpResponse(response));
      return;
    }
    chain.doFilter(request, response);
  }
}
