package lab.people.api;

import java.util.*;
import lab.people.model.Model.*;
import lab.people.service.PeopleService;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(produces = MediaType.APPLICATION_XML_VALUE)
public class PeopleController {
  private final PeopleService service;

  public PeopleController(PeopleService service) { this.service = service; }

  private static PersonData person(PeopleService.View view) {
    return new PersonData(view.person(), view.references());
  }

  @GetMapping("/persons")
  public PersonList all() {
    return new PersonList(
        service.all().stream().map(PeopleController::person).toList());
  }

  @GetMapping("/persons/{id}")
  public PersonData get(@PathVariable String id) {
    return person(service.get(Validation.id(id, "id")));
  }

  @PostMapping(value = "/persons", consumes = MediaType.APPLICATION_XML_VALUE)
  public ResponseEntity<PersonData>
  create(@RequestBody(required = false) PersonData body) {
    var view = service.create(Validation.personInput(body));
    return ResponseEntity.status(201)
        .contentType(MediaType.APPLICATION_XML)
        .body(person(view));
  }

  @PatchMapping(value = "/persons/{id}",
                consumes = MediaType.APPLICATION_XML_VALUE)
  public PersonData
  update(@PathVariable String id,
         @RequestBody(required = false) PersonData body) {
    var errors = new ArrayList<Detail>();
    long parsed = 0;
    PersonInput input = null;
    try {
      parsed = Validation.id(id, "id");
    } catch (ApiException e) {
      errors.addAll(e.details);
    }
    try {
      input = Validation.personInput(body);
    } catch (ApiException e) {
      errors.addAll(e.details);
    }
    if (!errors.isEmpty())
      throw new ApiException(400, "Некорректный запрос", errors);
    return person(service.update(parsed, input));
  }

  @DeleteMapping("/persons/{id}")
  public ResponseEntity<Void> delete(@PathVariable String id) {
    service.delete(Validation.id(id, "id"));
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/fire/all/{id}")
  public ResponseEntity<Void> fire(@PathVariable String id) {
    service.fire(Validation.id(id, "id"));
    return ResponseEntity.noContent().build();
  }

  @PostMapping(value = "/merge/{id1}/{id2}/new-name/new-address",
               consumes = MediaType.APPLICATION_XML_VALUE)
  public ResponseEntity<OrganizationData>
  merge(@PathVariable String id1, @PathVariable String id2,
        @RequestBody(required = false) OrganizationData body) {
    var errors = new ArrayList<Detail>();
    long a = 0, b = 0;
    Input input = null;
    try {
      a = Validation.id(id1, "id1");
    } catch (ApiException e) {
      errors.addAll(e.details);
    }
    try {
      b = Validation.id(id2, "id2");
    } catch (ApiException e) {
      errors.addAll(e.details);
    }
    try {
      input = Validation.input(body);
    } catch (ApiException e) {
      errors.addAll(e.details);
    }
    if (a > 0 && a == b)
      errors.add(
          new Detail("id2", "Для объединения нужны две разные организации"));
    if (!errors.isEmpty())
      throw new ApiException(400, "Некорректный запрос", errors);
    var result = service.merge(a, b, input);
    return ResponseEntity.status(201)
        .contentType(MediaType.APPLICATION_XML)
        .body(new OrganizationData(result));
  }
}
