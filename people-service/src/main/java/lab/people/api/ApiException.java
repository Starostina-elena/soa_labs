package lab.people.api;

import java.util.List;
import lab.people.model.Model.Detail;

public final class ApiException extends RuntimeException {
  final int status;

  final List<Detail> details;

  public ApiException(int status, String message, List<Detail> details) {
    super(message);
    this.status = status;
    this.details = List.copyOf(details);
  }

  public static ApiException bad(String field, String message) {
    return new ApiException(400, "Некорректный запрос",
                            List.of(new Detail(field, message)));
  }

  public static ApiException missing(String field) {
    return new ApiException(404, "Объект не найден",
                            List.of(new Detail(field, "Объект не найден")));
  }

  public static ApiException unavailable() {
    return new ApiException(
        503, "Сервис организаций недоступен или вернул некорректный ответ",
        List.of());
  }
}
