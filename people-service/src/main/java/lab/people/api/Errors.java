package lab.people.api;

import java.util.List;
import java.util.logging.*;
import lab.people.model.Model;
import lab.people.model.Model.Detail;
import org.springframework.http.*;
import org.springframework.web.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class Errors {
  private static final Logger LOG = Logger.getLogger(Errors.class.getName());

  @ExceptionHandler(Exception.class)
  public ResponseEntity<Model.ErrorData> handle(Exception exception) {
    int status;
    String message;
    List<Detail> details = List.of();
    if (exception instanceof ApiException e) {
      status = e.status;
      message = e.getMessage();
      details = e.details;
    } else if (exception instanceof org.springframework.http.converter
                                        .HttpMessageNotReadableException) {
      status = 400;
      message = "Некорректный XML";
      details = List.of(new Detail("body", message));
    } else if (exception instanceof HttpMediaTypeNotSupportedException) {
      status = 415;
      message = "Используйте Content-Type: application/xml";
    } else if (exception instanceof HttpMediaTypeNotAcceptableException) {
      status = 406;
      message = "Поддерживается ответ application/xml";
    } else if (exception instanceof HttpRequestMethodNotSupportedException) {
      status = 405;
      message = "Метод не поддерживается";
    } else if (exception instanceof NoResourceFoundException ||
               exception instanceof NoHandlerFoundException) {
      status = 404;
      message = "Ресурс не найден";
    } else {
      status = 500;
      message = "Внутренняя ошибка сервиса";
      LOG.log(Level.SEVERE, message, exception);
    }
    var response =
        ResponseEntity.status(status).contentType(MediaType.APPLICATION_XML);
    if (exception instanceof HttpRequestMethodNotSupportedException e &&
        e.getSupportedHttpMethods() != null)
      response.allow(e.getSupportedHttpMethods().toArray(HttpMethod[] ::new));
    return response.body(new Model.ErrorData(message, details));
  }
}
