package lab.people.config;

import java.util.List;
import lab.people.api.Errors;
import lab.people.api.PeopleController;
import lab.people.client.OrganizationClient;
import lab.people.repository.PeopleStore;
import lab.people.service.PeopleService;
import org.springframework.context.annotation.*;
import org.springframework.http.converter.*;
import org.springframework.http.converter.xml.Jaxb2RootElementHttpMessageConverter;
import org.springframework.web.servlet.config.annotation.*;

@Configuration
@EnableWebMvc
public class WebConfig implements WebMvcConfigurer {
  @Bean(destroyMethod = "close")
  PeopleStore peopleStore() {
    return new PeopleStore();
  }

  @Bean
  OrganizationClient organizationClient() {
    return new OrganizationClient(
        System.getProperty("organizations.url", "https://localhost:18081"),
        System.getProperty("organizations.caCert"));
  }

  @Bean
  PeopleService peopleService(PeopleStore store, OrganizationClient client) {
    return new PeopleService(store, client);
  }

  @Bean
  PeopleController peopleController(PeopleService service) {
    return new PeopleController(service);
  }

  @Bean
  Errors errors() {
    return new Errors();
  }

  @Override
  public void addCorsMappings(CorsRegistry registry) {
    registry.addMapping("/**")
        .allowedOrigins(System.getProperty("client.origin", "https://localhost:22081"))
        .allowedMethods("GET", "POST", "PATCH", "DELETE")
        .allowedHeaders("Content-Type", "Accept");
  }

  @Override
  public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
    converters.add(new Jaxb2RootElementHttpMessageConverter());
  }
}
