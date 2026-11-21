package lab.people.model;

import jakarta.persistence.*;

import java.io.Serializable;

@Entity
@Table(name = "person_organizations")
@IdClass(Employment.Key.class)
public class Employment {
    @Id
    @Column(name = "person_id")
    public Long personId;

    @Id
    @Column(name = "organization_id")
    public Long organizationId;

    public record Key(Long personId, Long organizationId) implements Serializable {}

    public Employment() {}

    public Employment(long personId, long organizationId) {
        this.personId = personId;
        this.organizationId = organizationId;
    }
}
