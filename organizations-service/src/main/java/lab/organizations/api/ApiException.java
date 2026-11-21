package lab.organizations.api;

import java.util.List;
import lab.organizations.model.Model.Detail;

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

  public static ApiException missing() {
    return new ApiException(404, "Организация не найдена", List.of());
  }
}
