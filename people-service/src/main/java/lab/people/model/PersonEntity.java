package lab.people.model;

import jakarta.persistence.*;

@Entity(name = "Person")
@Table(name = "persons")
public class PersonEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    public String name;

    public boolean deleted;
}
