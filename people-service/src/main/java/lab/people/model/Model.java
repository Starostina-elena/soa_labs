package lab.people.model;

import jakarta.xml.bind.annotation.*;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;

public final class Model {
  private Model() {}

  public enum Type { PUBLIC, TRUST, OPEN_JOINT_STOCK_COMPANY }

  public record
      Input(String name, long x, long y, float annualTurnover, String fullName,
            Type type, String street, String zipCode) {}

  public record Detail(String parameter, String message) {}

  public record PersonInput(String name, List<Long> organizationIds) {
    public PersonInput { organizationIds = List.copyOf(organizationIds); }
  }

  public record Person(long id, String name, List<Long> organizationIds) {
    public Person { organizationIds = List.copyOf(organizationIds); }
  }

  public record OrganizationRef(long id, String name) {}

  @XmlRootElement(name = "organization")
  @XmlAccessorType(XmlAccessType.FIELD)
  public static class OrganizationData {
    public String id;

    public String creationDate;

    public Boolean deleted;

    public String name;

    public Coordinates coordinates;

    public String annualTurnover;

    public String fullName;

    public String type;

    public Address postalAddress;

    public OrganizationData() {}

    public OrganizationData(Input input) {
      name = input.name();
      coordinates = new Coordinates(input.x(), input.y());
      annualTurnover = Float.toString(input.annualTurnover());
      fullName = input.fullName();
      type = input.type() == null ? null : input.type().name();
      postalAddress = new Address(input.street(), input.zipCode());
    }

    public OrganizationData(OrganizationRef reference) {
      id = Long.toString(reference.id());
      name = reference.name();
    }
  }

  @XmlAccessorType(XmlAccessType.FIELD)
  public static class Coordinates {
    public String x;

    public String y;

    public Coordinates() {}

    public Coordinates(long x, long y) {
      this.x = Long.toString(x);
      this.y = Long.toString(y);
    }
  }

  @XmlAccessorType(XmlAccessType.FIELD)
  public static class Address {
    public String street;

    public String zipCode;

    public Address() {}

    public Address(String street, String zipCode) {
      this.street = street;
      this.zipCode = zipCode;
    }
  }

  @XmlRootElement(name = "error")
  @XmlAccessorType(XmlAccessType.FIELD)
  public static class ErrorData {
    public String timestamp;

    public String message;

    @XmlElementWrapper(name = "errors")
    @XmlElement(name = "errorDetail")
    public List<ErrorDetail> errors = new ArrayList<>();

    public ErrorData() {}

    public ErrorData(String message, List<Detail> details) {
      timestamp = OffsetDateTime.now(ZoneOffset.UTC).toString();
      this.message = message;
      errors = details.stream().map(ErrorDetail::new).toList();
    }
  }

  @XmlAccessorType(XmlAccessType.FIELD)
  public static class ErrorDetail {
    public String parameter;

    public String message;

    public ErrorDetail() {}

    public ErrorDetail(Detail detail) {
      parameter = detail.parameter();
      message = detail.message();
    }
  }

  @XmlRootElement(name = "person")
  @XmlAccessorType(XmlAccessType.FIELD)
  public static class PersonData {
    public String id;

    public String name;

    public Boolean deleted;

    @XmlElementWrapper(name = "organizationIds")
    @XmlElement(name = "organizationId")
    public List<String> organizationIds;

    @XmlElementWrapper(name = "organizations")
    @XmlElement(name = "organization")
    public List<OrganizationData> organizations;

    public PersonData() {}

    public PersonData(Person person, List<OrganizationRef> references) {
      id = Long.toString(person.id());
      name = person.name();
      deleted = false;
      organizationIds =
          references.stream().map(ref -> Long.toString(ref.id())).toList();
      organizations = references.stream().map(OrganizationData::new).toList();
    }
  }

  @XmlRootElement(name = "persons")
  @XmlAccessorType(XmlAccessType.FIELD)
  public static class PersonList {
    @XmlElementWrapper(name = "items")
    @XmlElement(name = "person")
    public List<PersonData> items = new ArrayList<>();

    public PersonList() {}

    public PersonList(List<PersonData> items) { this.items = items; }
  }
}
