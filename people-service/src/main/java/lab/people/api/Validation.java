package lab.people.api;

import java.util.*;
import lab.people.model.Model.*;

public final class Validation {
  final List<Detail> errors = new ArrayList<>();

  void error(String field, String message) {
    errors.add(new Detail(field, message));
  }

  public String string(String value, String field, boolean required,
                       boolean nonempty) {
    if (value == null) {
      if (required)
        error(field, "Обязательное поле");
    } else if (nonempty && value.isEmpty()) {
      error(field, "Строка не должна быть пустой");
    }
    return value;
  }

  public Long integer(String value, String field, boolean required) {
    if (string(value, field, required, false) == null)
      return null;
    try {
      return Long.valueOf(value.trim());
    } catch (NumberFormatException e) {
      error(field, "Ожидается целое число int64");
      return null;
    }
  }

  Float decimal(String value, String field, boolean required) {
    if (string(value, field, required, false) == null)
      return null;
    try {
      float result = Float.parseFloat(value.trim());
      if (!Float.isFinite(result))
        throw new NumberFormatException();
      return result;
    } catch (NumberFormatException e) {
      error(field, "Ожидается конечное число float");
      return null;
    }
  }

  Type type(String value, String field) {
    if (value == null)
      return null;
    try {
      return Type.valueOf(value.trim());
    } catch (IllegalArgumentException e) {
      error(field, "Допустимы PUBLIC, TRUST, OPEN_JOINT_STOCK_COMPANY");
      return null;
    }
  }

  void readOnly(Object value, String field) {
    if (value != null)
      error(field, "Поле доступно только для чтения");
  }

  public void finish() {
    if (!errors.isEmpty())
      throw new ApiException(400, "Некорректный запрос", errors);
  }

  static long id(String raw, String field) {
    try {
      long id = Long.parseLong(raw);
      if (id < 1)
        throw new NumberFormatException();
      return id;
    } catch (NumberFormatException e) {
      throw ApiException.bad(field, "Ожидается целое число int64 >= 1");
    }
  }

  static Input input(OrganizationData data) {
    if (data == null)
      throw ApiException.bad("body", "Тело запроса обязательно");
    var v = new Validation();
    v.readOnly(data.id, "id");
    v.readOnly(data.creationDate, "creationDate");
    v.readOnly(data.deleted, "deleted");
    String name = v.string(data.name, "name", true, true);
    Long x = null, y = null;
    if (data.coordinates == null) {
      v.error("coordinates", "Обязательное поле");
    } else {
      x = v.integer(data.coordinates.x, "coordinates.x", true);
      y = v.integer(data.coordinates.y, "coordinates.y", true);
      if (x != null && x > 775)
        v.error("coordinates.x", "Должно быть <= 775");
    }
    Float turnover = v.decimal(data.annualTurnover, "annualTurnover", true);
    if (turnover != null && turnover <= 0)
      v.error("annualTurnover", "Должно быть > 0");
    String fullName = v.string(data.fullName, "fullName", true, false);
    Type type = v.type(data.type, "type");
    String street = null, zipCode = null;
    if (data.postalAddress == null) {
      v.error("postalAddress", "Обязательное поле");
    } else {
      street = v.string(data.postalAddress.street, "postalAddress.street", true,
                        true);
      zipCode = v.string(data.postalAddress.zipCode, "postalAddress.zipCode",
                         true, false);
    }
    v.finish();
    return new Input(name, x, y, turnover, fullName, type, street, zipCode);
  }

  static PersonInput personInput(PersonData data) {
    if (data == null)
      throw ApiException.bad("body", "Тело запроса обязательно");
    var v = new Validation();
    v.readOnly(data.id, "id");
    v.readOnly(data.deleted, "deleted");
    v.readOnly(data.organizations, "organizations");
    String name = v.string(data.name, "name", true, true);
    var ids = new LinkedHashSet<Long>();
    if (data.organizationIds != null) {
      for (int i = 0; i < data.organizationIds.size(); i++) {
        String field = "organizationIds[" + i + "]";
        Long id = v.integer(data.organizationIds.get(i), field, true);
        if (id != null) {
          if (id < 1)
            v.error(field, "ID должен быть >= 1");
          else
            ids.add(id);
        }
      }
    }
    v.finish();
    return new PersonInput(name, List.copyOf(ids));
  }
}
