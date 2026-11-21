package lab.organizations.model;

import jakarta.persistence.*;
import jakarta.xml.bind.annotation.*;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;

public final class Model {
  private Model() {}

  public enum Type {
    PUBLIC,
    TRUST,
    OPEN_JOINT_STOCK_COMPANY
  }

  @Embeddable
  public record Input(
      String name,
      long x,
      long y,
      @Column(name = "turnover") float annualTurnover,
      @Column(name = "full_name") String fullName,
      @Enumerated(EnumType.STRING) @Column(name = "org_type") Type type,
      String street,
      @Column(name = "zip_code") String zipCode) {}

  public record Detail(String parameter, String message) {}

  public record Organization(long id, OffsetDateTime creationDate, boolean deleted, Input data) {}

  public record Condition(String field, String operation, Object value) {}

  public record SearchQuery(int page, int size, List<String> sort, List<Condition> filters) {}

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

    public OrganizationData(Organization organization) {
      this(organization.data());
      id = Long.toString(organization.id());
      creationDate = organization.creationDate().toString();
      deleted = organization.deleted();
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

  @XmlRootElement(name = "organizations")
  @XmlAccessorType(XmlAccessType.FIELD)
  public static class OrganizationList {
    public int page;

    public int size;

    public long total;

    @XmlElementWrapper(name = "items")
    @XmlElement(name = "organization")
    public List<OrganizationData> items = new ArrayList<>();

    public OrganizationList() {}

    public OrganizationList(int page, int size, long total, List<Organization> items) {
      this.page = page;
      this.size = size;
      this.total = total;
      this.items = items.stream().map(OrganizationData::new).toList();
    }
  }

  @XmlRootElement(name = "countResult")
  @XmlAccessorType(XmlAccessType.FIELD)
  public static class CountResult {
    public long count;

    public CountResult() {}

    public CountResult(long count) {
      this.count = count;
    }
  }

  @XmlRootElement(name = "organizationListRequest")
  @XmlAccessorType(XmlAccessType.FIELD)
  public static class OrganizationListRequest {
    public String page;

    public String size;

    @XmlElementWrapper(name = "sort")
    @XmlElement(name = "string")
    public List<String> sort = new ArrayList<>();

    public Filters filters;
  }

  @XmlAccessorType(XmlAccessType.FIELD)
  public static class Filters {
    public Range id;

    public StringFilter name;

    public CoordinatesFilter coordinates;

    public Range creationDate;

    public Range annualTurnover;

    public StringFilter fullName;

    public TypeFilter type;

    public AddressFilter postalAddress;
  }

  @XmlAccessorType(XmlAccessType.FIELD)
  public static class CoordinatesFilter {
    public Range x;

    public Range y;
  }

  @XmlAccessorType(XmlAccessType.FIELD)
  public static class AddressFilter {
    public StringFilter street;

    public StringFilter zipCode;
  }

  @XmlAccessorType(XmlAccessType.FIELD)
  public static class Range {
    public String eq;

    public String gt;

    public String gte;

    public String lt;

    public String lte;
  }

  @XmlAccessorType(XmlAccessType.FIELD)
  public static class StringFilter {
    public String eq;

    public String contains;
  }

  @XmlAccessorType(XmlAccessType.FIELD)
  public static class TypeFilter {
    public String eq;

    @XmlElementWrapper(name = "in")
    @XmlElement(name = "type")
    public List<String> in;
  }
}
