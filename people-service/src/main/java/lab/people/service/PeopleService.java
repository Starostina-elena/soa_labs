package lab.people.service;

import java.util.*;
import lab.people.api.ApiException;
import lab.people.client.OrganizationClient;
import lab.people.model.Model.*;
import lab.people.repository.PeopleStore;

public final class PeopleService {
  private final PeopleStore store;

  private final OrganizationClient organizations;

  public PeopleService(PeopleStore store, OrganizationClient organizations) {
    this.store = store;
    this.organizations = organizations;
  }

  public record View(Person person, List<OrganizationRef> references) {}

  private List<OrganizationRef>
  references(Person person, Map<Long, Optional<OrganizationRef>> cache) {
    var refs = new ArrayList<OrganizationRef>();
    for (long id : person.organizationIds()) {
      cache
          .computeIfAbsent(id,
                           key -> Optional.ofNullable(organizations.find(key)))
          .ifPresent(refs::add);
    }
    return refs;
  }

  public synchronized List<View> all() {
    var cache = new HashMap<Long, Optional<OrganizationRef>>();
    return store.all()
        .stream()
        .map(p -> new View(p, references(p, cache)))
        .toList();
  }

  public synchronized View get(long id) {
    Person person = store.get(id);
    return new View(person, references(person, new HashMap<>()));
  }

  private List<OrganizationRef> validate(PersonInput input) {
    var errors = new ArrayList<Detail>();
    var refs = new ArrayList<OrganizationRef>();
    for (int i = 0; i < input.organizationIds().size(); i++) {
      var ref = organizations.find(input.organizationIds().get(i));
      if (ref == null) {
        errors.add(new Detail("organizationIds[" + i + "]",
                              "Организация не существует или удалена"));
      } else {
        refs.add(ref);
      }
    }
    if (!errors.isEmpty())
      throw new ApiException(400, "Некорректные организации сотрудника",
                             errors);
    return refs;
  }

  public synchronized View create(PersonInput input) {
    var refs = validate(input);
    return new View(store.create(input), refs);
  }

  public synchronized View update(long id, PersonInput input) {
    store.get(id);
    var refs = validate(input);
    return new View(store.update(id, input), refs);
  }

  public synchronized void delete(long id) { store.delete(id); }

  public synchronized void fire(long id) {
    if (organizations.find(id) == null)
      throw ApiException.missing("id");
    store.fire(id);
  }

  public synchronized OrganizationRef merge(long id1, long id2, Input input) {
    if (id1 == id2)
      throw ApiException.bad("id2",
                             "Для объединения нужны две разные организации");

    var errors = new ArrayList<Detail>();
    if (organizations.find(id1) == null)
      errors.add(new Detail("id1", "Организация не найдена"));
    if (organizations.find(id2) == null)
      errors.add(new Detail("id2", "Организация не найдена"));
    if (!errors.isEmpty())
      throw new ApiException(404, "Исходные организации не найдены", errors);

    OrganizationRef created = organizations.create(input);
    store.moveEmployees(id1, id2, created.id());
    organizations.delete(id1);
    organizations.delete(id2);
    return created;
  }
}
