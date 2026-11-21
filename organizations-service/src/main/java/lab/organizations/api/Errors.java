package lab.organizations.api;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.*;
import jakarta.ws.rs.ext.*;
import java.util.List;
import java.util.logging.*;
import lab.organizations.model.Model;
import lab.organizations.model.Model.Detail;

@Provider
public class Errors implements ExceptionMapper<Exception> {
  private static final Logger LOG = Logger.getLogger(Errors.class.getName());

  @Override
  public Response toResponse(Exception exception) {
    if (exception instanceof ApiException api)
      return response(api.status, api.getMessage(), api.details);
    if (exception instanceof WebApplicationException web) {
      int status = web.getResponse().getStatus();
      String message = switch (status) {
        case 404 -> "Ресурс не найден";
        case 405 -> "Метод не поддерживается";
        case 406 -> "Поддерживается ответ application/xml";
        case 415 -> "Используйте Content-Type: application/xml";
        default -> "Некорректный HTTP-запрос";
      };
      Response mapped = response(status, message, List.of());
      return Response.fromResponse(mapped)
          .allow(web.getResponse().getAllowedMethods())
          .build();
    }
    LOG.log(Level.SEVERE, "Ошибка обработки запроса", exception);
    return response(500, "Внутренняя ошибка сервиса", List.of());
  }

  static Response response(int status, String message, List<Detail> errors) {
    return Response.status(status)
        .type(MediaType.APPLICATION_XML_TYPE)
        .entity(new Model.ErrorData(message, errors))
        .build();
  }
}
