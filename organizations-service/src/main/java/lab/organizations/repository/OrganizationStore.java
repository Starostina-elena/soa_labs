package lab.organizations.repository;

import jakarta.annotation.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.criteria.*;

import lab.organizations.api.ApiException;
import lab.organizations.config.HibernateConfig;
import lab.organizations.model.Model.*;
import lab.organizations.model.OrganizationEntity;

import org.hibernate.*;
import org.hibernate.query.NullPrecedence;

import java.util.*;

@ApplicationScoped
public class OrganizationStore implements AutoCloseable {
    private SessionFactory sessions;

    @PostConstruct
    public void init() {
        sessions = HibernateConfig.create(OrganizationEntity.class);
    }

    @Override
    @PreDestroy
    public void close() {
        if (sessions != null) sessions.close();
    }

    private OrganizationEntity find(Session session, long id) {
        var organization = session.find(OrganizationEntity.class, id);
        if (organization == null || organization.deleted) throw ApiException.missing();
        return organization;
    }

    public Organization create(Input input) {
        return sessions.fromTransaction(
                session -> {
                    var organization = new OrganizationEntity(input);
                    session.persist(organization);
                    return organization.toModel();
                });
    }

    public Organization get(long id) {
        return sessions.fromTransaction(session -> find(session, id).toModel());
    }

    public Organization update(long id, Input input) {
        return sessions.fromTransaction(
                session -> {
                    var organization = find(session, id);
                    organization.data = input;
                    return organization.toModel();
                });
    }

    public void delete(long id) {
        sessions.inTransaction(session -> find(session, id).deleted = true);
    }

    public synchronized void deleteType(Type type) {
        sessions.inTransaction(
                session -> {
                    var items =
                            session.createSelectionQuery(
                                            "from Organization where deleted=false and"
                                                + " data.type=:type order by id",
                                            OrganizationEntity.class)
                                    .setParameter("type", type)
                                    .setMaxResults(1)
                                    .getResultList();
                    if (items.isEmpty()) throw ApiException.missing();
                    items.get(0).deleted = true;
                });
    }

    private Path<?> path(Root<OrganizationEntity> root, String field) {
        return switch (field) {
            case "id", "creationDate" -> root.get(field);
            case "coordinates.x" -> root.get("data").get("x");
            case "coordinates.y" -> root.get("data").get("y");
            case "postalAddress.street" -> root.get("data").get("street");
            case "postalAddress.zipCode" -> root.get("data").get("zipCode");
            default -> root.get("data").get(field);
        };
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private Predicate[] filters(
            CriteriaBuilder builder, Root<OrganizationEntity> root, SearchQuery query) {
        var predicates = new ArrayList<Predicate>();
        predicates.add(builder.isFalse(root.get("deleted")));

        for (Condition condition : query.filters()) {
            Expression field = path(root, condition.field());
            Object value = condition.value();
            predicates.add(
                    switch (condition.operation()) {
                        case "eq" -> builder.equal(field, value);
                        case "gt" -> builder.greaterThan(field, (Comparable) value);
                        case "gte" -> builder.greaterThanOrEqualTo(field, (Comparable) value);
                        case "lt" -> builder.lessThan(field, (Comparable) value);
                        case "lte" -> builder.lessThanOrEqualTo(field, (Comparable) value);
                        case "contains" -> builder.gt(builder.locate(field, (String) value), 0);
                        case "in" -> field.in((List<?>) value);
                        default ->
                                throw new IllegalArgumentException("Неизвестный оператор фильтра");
                    });
        }

        return predicates.toArray(Predicate[]::new);
    }

    public OrganizationList search(SearchQuery request) {
        return sessions.fromTransaction(
                session -> {
                    var builder = session.getCriteriaBuilder();
                    var count = builder.createQuery(Long.class);
                    var countRoot = count.from(OrganizationEntity.class);
                    count.select(builder.count(countRoot))
                            .where(filters(builder, countRoot, request));
                    long total = session.createSelectionQuery(count).getSingleResult();

                    var query = builder.createQuery(OrganizationEntity.class);
                    var root = query.from(OrganizationEntity.class);
                    query.select(root).where(filters(builder, root, request));

                    var order = new ArrayList<Order>();
                    for (String key : request.sort()) {
                        boolean descending = key.startsWith("-");
                        var field = path(root, descending ? key.substring(1) : key);
                        order.add(
                                (descending ? builder.desc(field) : builder.asc(field))
                                        .nullPrecedence(NullPrecedence.LAST));
                    }
                    order.add(builder.asc(root.get("id")));
                    query.orderBy(order)
                            .offset((long) (request.page() - 1) * request.size())
                            .fetch(request.size());

                    var items =
                            session.createSelectionQuery(query).getResultList().stream()
                                    .map(OrganizationEntity::toModel)
                                    .toList();
                    return new OrganizationList(request.page(), request.size(), total, items);
                });
    }

    public long countAnnualTurnover(float value) {
        return sessions.fromTransaction(
                session ->
                        session.createSelectionQuery(
                                        "select count(*) from Organization where deleted=false and"
                                            + " data.annualTurnover>:value",
                                        Long.class)
                                .setParameter("value", value)
                                .getSingleResult());
    }
}
