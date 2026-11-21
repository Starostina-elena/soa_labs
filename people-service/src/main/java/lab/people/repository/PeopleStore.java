package lab.people.repository;

import lab.people.api.ApiException;
import lab.people.config.HibernateConfig;
import lab.people.model.*;
import lab.people.model.Model.*;

import org.hibernate.*;

import java.util.*;
import java.util.stream.Collectors;

public final class PeopleStore implements AutoCloseable {
    private final SessionFactory sessions =
            HibernateConfig.create(PersonEntity.class, Employment.class);

    @Override
    public void close() {
        sessions.close();
    }

    private PersonEntity find(Session session, long id) {
        var person = session.find(PersonEntity.class, id);
        if (person == null || person.deleted) throw ApiException.missing("id");
        return person;
    }

    private Person view(Session session, PersonEntity person) {
        var ids =
                session.createSelectionQuery(
                                "select organizationId from Employment where personId=:id order by"
                                    + " organizationId",
                                Long.class)
                        .setParameter("id", person.id)
                        .getResultList();
        return new Person(person.id, person.name, ids);
    }

    public Person get(long id) {
        return sessions.fromTransaction(session -> view(session, find(session, id)));
    }

    public List<Person> all() {
        return sessions.fromTransaction(
                session -> {
                    var persons =
                            session.createSelectionQuery(
                                            "from Person where deleted=false order by id",
                                            PersonEntity.class)
                                    .getResultList();
                    var links =
                            session
                                    .createSelectionQuery(
                                            "from Employment e join Person p on p.id=e.personId"
                                                + " where p.deleted=false order by"
                                                + " e.organizationId",
                                            Employment.class)
                                    .getResultList()
                                    .stream()
                                    .collect(
                                            Collectors.groupingBy(
                                                    e -> e.personId,
                                                    Collectors.mapping(
                                                            e -> e.organizationId,
                                                            Collectors.toList())));
                    return persons.stream()
                            .map(p -> new Person(p.id, p.name, links.getOrDefault(p.id, List.of())))
                            .toList();
                });
    }

    private void links(Session session, long id, List<Long> ids) {
        session.createMutationQuery("delete from Employment where personId=:id")
                .setParameter("id", id)
                .executeUpdate();
        for (long organization : new LinkedHashSet<>(ids))
            session.persist(new Employment(id, organization));
    }

    public Person create(PersonInput input) {
        return sessions.fromTransaction(
                session -> {
                    var person = new PersonEntity();
                    person.name = input.name();
                    session.persist(person);
                    links(session, person.id, input.organizationIds());
                    return view(session, person);
                });
    }

    public Person update(long id, PersonInput input) {
        return sessions.fromTransaction(
                session -> {
                    var person = find(session, id);
                    person.name = input.name();
                    links(session, id, input.organizationIds());
                    return view(session, person);
                });
    }

    public void delete(long id) {
        sessions.inTransaction(session -> find(session, id).deleted = true);
    }

    public void fire(long organization) {
        sessions.inTransaction(
                session ->
                        session.createMutationQuery(
                                        "delete from Employment where organizationId=:id")
                                .setParameter("id", organization)
                                .executeUpdate());
    }

    public void moveEmployees(long a, long b, long newOrganizationId) {
        sessions.inTransaction(
                session -> {
                    session.createMutationQuery(
                                    "insert into Employment (personId,organizationId) select"
                                        + " distinct e.personId,:target from Employment e join"
                                        + " Person p on p.id=e.personId where p.deleted=false and"
                                        + " e.organizationId in (:a,:b)")
                            .setParameter("target", newOrganizationId)
                            .setParameter("a", a)
                            .setParameter("b", b)
                            .executeUpdate();
                    session.createMutationQuery(
                                    "delete from Employment where organizationId in (:a,:b)")
                            .setParameter("a", a)
                            .setParameter("b", b)
                            .executeUpdate();
                });
    }
}
