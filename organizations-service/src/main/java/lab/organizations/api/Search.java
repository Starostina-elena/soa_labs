package lab.organizations.api;

import lab.organizations.model.Model.*;

import java.util.*;

final class Search {
    int page = 1, size = 10;

    final List<String> sort = new ArrayList<>();

    final List<Condition> filters = new ArrayList<>();

    static final Set<String> FIELDS =
            Set.of(
                    "id",
                    "name",
                    "coordinates.x",
                    "coordinates.y",
                    "creationDate",
                    "annualTurnover",
                    "fullName",
                    "type",
                    "postalAddress.street",
                    "postalAddress.zipCode");

    static SearchQuery parse(OrganizationListRequest request) {
        var result = new Search();
        if (request == null) return new SearchQuery(1, 10, List.of(), List.of());
        var v = new Validation();
        Long page = v.integer(request.page, "page", false);
        Long size = v.integer(request.size, "size", false);
        if (page != null) {
            if (page < 1 || page > Integer.MAX_VALUE)
                v.error("page", "Должно быть от 1 до 2147483647");
            else result.page = page.intValue();
        }
        if (size != null) {
            if (size < 1 || size > Integer.MAX_VALUE)
                v.error("size", "Должно быть от 1 до 2147483647");
            else result.size = size.intValue();
        }
        for (String key : request.sort) {
            if (key != null && FIELDS.contains(key.startsWith("-") ? key.substring(1) : key))
                result.sort.add(key);
            else v.error("sort", "Неизвестное поле сортировки: " + key);
        }
        Filters f = request.filters;
        if (f != null) {
            result.range(f.id, "id", v);
            result.strings(f.name, "name", v);
            result.range(f.creationDate, "creationDate", v);
            result.range(f.annualTurnover, "annualTurnover", v);
            result.strings(f.fullName, "fullName", v);
            result.types(f.type, v);
            if (f.coordinates != null) {
                result.range(f.coordinates.x, "coordinates.x", v);
                result.range(f.coordinates.y, "coordinates.y", v);
            }
            if (f.postalAddress != null) {
                result.strings(f.postalAddress.street, "postalAddress.street", v);
                result.strings(f.postalAddress.zipCode, "postalAddress.zipCode", v);
            }
        }
        v.finish();
        return new SearchQuery(result.page, result.size, result.sort, result.filters);
    }

    private void range(Range filter, String field, Validation v) {
        if (filter == null) return;
        String[] operations = {"eq", "gt", "gte", "lt", "lte"};
        String[] values = {filter.eq, filter.gt, filter.gte, filter.lt, filter.lte};
        for (int i = 0; i < operations.length; i++) {
            if (values[i] == null) continue;
            String path = "filters." + field + "." + operations[i];
            Object expected;
            if (field.equals("creationDate")) {
                var date = v.date(values[i], path);
                expected = date;
            } else if (field.equals("annualTurnover")) {
                expected = v.decimal(values[i], path, false);
            } else {
                expected = v.integer(values[i], path, false);
            }
            String operation = operations[i];
            if (expected != null) filters.add(new Condition(field, operation, expected));
        }
    }

    private void strings(StringFilter filter, String field, Validation v) {
        if (filter == null) return;
        String equal = v.string(filter.eq, "filters." + field + ".eq", false, true);
        String contains = v.string(filter.contains, "filters." + field + ".contains", false, true);
        if (equal != null) filters.add(new Condition(field, "eq", equal));
        if (contains != null) filters.add(new Condition(field, "contains", contains));
    }

    private void types(TypeFilter filter, Validation v) {
        if (filter == null) return;
        Type equal = v.type(filter.eq, "filters.type.eq");
        if (equal != null) filters.add(new Condition("type", "eq", equal));
        if (filter.in != null) {
            var allowed = new LinkedHashSet<Type>();
            for (String value : filter.in) {
                Type type = v.type(value, "filters.type.in");
                if (type != null) allowed.add(type);
            }
            filters.add(new Condition("type", "in", List.copyOf(allowed)));
        }
    }
}
