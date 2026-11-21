package lab.organizations.api;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import java.util.*;
import lab.organizations.model.Model.*;
import lab.organizations.repository.OrganizationStore;

@Path("/organizations")
@Produces(MediaType.APPLICATION_XML)
public class OrganizationsResource {
  @Inject OrganizationStore store;

  @POST
  @Consumes(MediaType.APPLICATION_XML)
  public Response create(OrganizationData body, @Context UriInfo uri) {
    Organization organization = store.create(Validation.input(body));
    return Response.created(
            uri.getAbsolutePathBuilder().path(Long.toString(organization.id())).build())
        .entity(new OrganizationData(organization))
        .build();
  }

  @POST
  @Path("/search")
  @Consumes(MediaType.APPLICATION_XML)
  public OrganizationList search(OrganizationListRequest body) {
    return store.search(Search.parse(body));
  }

  @GET
  @Path("/{id}")
  public OrganizationData get(@PathParam("id") String id) {
    return new OrganizationData(store.get(Validation.id(id, "id")));
  }

  @PATCH
  @Path("/{id}")
  @Consumes(MediaType.APPLICATION_XML)
  public OrganizationData update(@PathParam("id") String rawId, OrganizationData body) {
    var errors = new ArrayList<Detail>();
    long id = 0;
    Input input = null;
    try {
      id = Validation.id(rawId, "id");
    } catch (ApiException e) {
      errors.addAll(e.details);
    }
    try {
      input = Validation.input(body);
    } catch (ApiException e) {
      errors.addAll(e.details);
    }
    if (!errors.isEmpty()) throw new ApiException(400, "Некорректный запрос", errors);
    return new OrganizationData(store.update(id, input));
  }

  @DELETE
  @Path("/{id}")
  public Response delete(@PathParam("id") String id) {
    store.delete(Validation.id(id, "id"));
    return Response.noContent().build();
  }

  @DELETE
  @Path("/type/{type}")
  public Response deleteType(@PathParam("type") String rawType) {
    Type type;
    try {
      type = Type.valueOf(rawType);
    } catch (IllegalArgumentException e) {
      throw ApiException.bad("type", "Неизвестный тип организации");
    }
    store.deleteType(type);
    return Response.noContent().build();
  }

  @GET
  @Path("/count-annual-turnover")
  public CountResult count(@QueryParam("annualTurnover") String rawValue) {
    var v = new Validation();
    Float value = v.decimal(rawValue, "annualTurnover", true);
    if (value != null && value <= 0) v.error("annualTurnover", "Должно быть > 0");
    v.finish();
    return new CountResult(store.countAnnualTurnover(value));
  }
}
