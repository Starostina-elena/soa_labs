package lab.organizations.model;

import jakarta.persistence.*;

import lab.organizations.model.Model.*;

import org.hibernate.annotations.Formula;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Entity(name = "Organization")
@Table(name = "organizations")
public class OrganizationEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(length = 64)
    public String created;

    @Formula("created::timestamptz")
    public OffsetDateTime creationDate;

    public boolean deleted;

    @Embedded public Input data;

    public OrganizationEntity() {}

    public OrganizationEntity(Input data) {
        created = OffsetDateTime.now(ZoneOffset.UTC).toString();
        this.data = data;
    }

    public Organization toModel() {
        return new Organization(id, OffsetDateTime.parse(created), deleted, data);
    }
}
