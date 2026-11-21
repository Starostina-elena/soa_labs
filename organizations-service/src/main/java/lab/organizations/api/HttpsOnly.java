package lab.organizations.api;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.*;
import jakarta.ws.rs.ext.Provider;
import java.util.List;

@Provider
@PreMatching
@Priority(Priorities.AUTHENTICATION)
public class HttpsOnly implements ContainerRequestFilter {
  @Override
  public void filter(ContainerRequestContext request) {
    if (!request.getSecurityContext().isSecure())
      request.abortWith(
          Errors.response(403, "Доступ разрешён только по HTTPS", List.of()));
  }
}
