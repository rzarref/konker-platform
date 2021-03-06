package com.konkerlabs.platform.registry.api.test.web.controller;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.konkerlabs.platform.registry.api.config.WebMvcConfig;
import com.konkerlabs.platform.registry.api.model.ApplicationVO;
import com.konkerlabs.platform.registry.api.test.config.MongoTestConfig;
import com.konkerlabs.platform.registry.api.test.config.WebTestConfiguration;
import com.konkerlabs.platform.registry.api.web.controller.ApplicationRestController;
import com.konkerlabs.platform.registry.api.web.wrapper.CrudResponseAdvice;
import com.konkerlabs.platform.registry.business.model.Application;
import com.konkerlabs.platform.registry.business.model.Tenant;
import com.konkerlabs.platform.registry.business.services.api.ApplicationService;
import com.konkerlabs.platform.registry.business.services.api.ServiceResponseBuilder;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = ApplicationRestController.class)
@AutoConfigureMockMvc(secure = false)
@ContextConfiguration(classes = {
        WebTestConfiguration.class,
        MongoTestConfig.class,
        WebMvcConfig.class,
        CrudResponseAdvice.class
})
public class ApplicationRestControllerTest extends WebLayerTestContext {

    @Autowired
    private ApplicationService applicationService;

    @Autowired
    private Tenant tenant;

    private Application application1;

    private Application application2;

    @Before
    public void setUp() {
        application1 = Application.builder()
        		.name("smartff")
        		.friendlyName("Smart Frig")
        		.description("Description of smartff")
        		.qualifier(tenant.getName())
        		.registrationDate(Instant.now())
        		.build();
        	
        application2 = Application.builder()
        		.name("konkerff")
        		.friendlyName("Konker Frig")
        		.description("Description of konkerff")
        		.qualifier(tenant.getName())
        		.registrationDate(Instant.now())
        		.build();
    }

    @After
    public void tearDown() {
        Mockito.reset(applicationService);
    }

    @Test
    public void shouldListApplications() throws Exception {
        List<Application> applications = new ArrayList<>();
        applications.add(application1);
        applications.add(application2);

        when(applicationService.findAll(tenant))
        	.thenReturn(
        			ServiceResponseBuilder
        				.<List<Application>>ok()
                		.withResult(applications)
                		.build());

        getMockMvc()
        .perform(MockMvcRequestBuilders
        		.get("/applications/")
        		.contentType("application/json")
        		.accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(content().contentType("application/json;charset=UTF-8"))
        .andExpect(jsonPath("$.code", is(HttpStatus.OK.value())))
        .andExpect(jsonPath("$.status", is("success")))
        .andExpect(jsonPath("$.timestamp",greaterThan(1400000000)))
        .andExpect(jsonPath("$.result", hasSize(2)))
        .andExpect(jsonPath("$.result[0].name", is("smartff")))
        .andExpect(jsonPath("$.result[0].friendlyName", is("Smart Frig")))
        .andExpect(jsonPath("$.result[0].description", is("Description of smartff")))
        .andExpect(jsonPath("$.result[1].name", is("konkerff")))
        .andExpect(jsonPath("$.result[1].friendlyName", is("Konker Frig")))
        .andExpect(jsonPath("$.result[1].description", is("Description of konkerff")));
    }

    @Test
    public void shouldReturnInternalErrorWhenListApplications() throws Exception {
        when(applicationService.findAll(tenant))
                .thenReturn(ServiceResponseBuilder.<List<Application>>error().build());

        getMockMvc()
        .perform(MockMvcRequestBuilders.get("/applications/")
        		.accept(MediaType.APPLICATION_JSON)
        		.contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().is5xxServerError())
        .andExpect(content().contentType("application/json;charset=UTF-8"))
        .andExpect(jsonPath("$.code", is(HttpStatus.INTERNAL_SERVER_ERROR.value())))
        .andExpect(jsonPath("$.status", is("error")))
        .andExpect(jsonPath("$.timestamp", greaterThan(1400000000)))
        .andExpect(jsonPath("$.messages").doesNotExist())
        .andExpect(jsonPath("$.result").doesNotExist());
    }

    @Test
    public void shouldReadApplicationByName() throws Exception {
        when(applicationService.getByApplicationName(tenant, application1.getName()))
        	.thenReturn(
        			ServiceResponseBuilder
        				.<Application>ok()
        				.withResult(application1)
        				.build());

        getMockMvc()
        .perform(MockMvcRequestBuilders
        		.get("/applications/" + application1.getName())
        		.contentType("application/json")
        		.accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(content().contentType("application/json;charset=UTF-8"))
        .andExpect(jsonPath("$.code", is(HttpStatus.OK.value())))
        .andExpect(jsonPath("$.status", is("success")))
        .andExpect(jsonPath("$.timestamp",greaterThan(1400000000)))
        .andExpect(jsonPath("$.result").isMap())
        .andExpect(jsonPath("$.result.name", is("smartff")))
        .andExpect(jsonPath("$.result.friendlyName", is("Smart Frig")))
        .andExpect(jsonPath("$.result.description", is("Description of smartff")));
    }

    @Test
    public void shouldReturnNotFoundWhenReadByName() throws Exception {
        when(applicationService.getByApplicationName(tenant, application1.getName()))
        	.thenReturn(
        			ServiceResponseBuilder
        				.<Application>error()
                		.withMessage(ApplicationService.Validations.APPLICATION_NOT_FOUND.getCode())
                		.build());

        getMockMvc()
        .perform(MockMvcRequestBuilders
        		.get("/applications/" + application1.getName())
        		.accept(MediaType.APPLICATION_JSON)
        		.contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().is4xxClientError())
        .andExpect(content().contentType("application/json;charset=UTF-8"))
        .andExpect(jsonPath("$.code", is(HttpStatus.NOT_FOUND.value())))
        .andExpect(jsonPath("$.status", is("error")))
        .andExpect(jsonPath("$.timestamp", greaterThan(1400000000)))
        .andExpect(jsonPath("$.messages[0]", is("Application not found")))
        .andExpect(jsonPath("$.result").doesNotExist());
    }

    @Test
    public void shouldCreateApplication() throws Exception {
        when(
        	applicationService
        		.register(org.mockito.Matchers.any(Tenant.class), org.mockito.Matchers.any(Application.class)))
        .thenReturn(ServiceResponseBuilder.<Application>ok().withResult(application1).build());

        getMockMvc()
        .perform(MockMvcRequestBuilders.post("/applications/")
        		.content(getJson(new ApplicationVO().apply(application1)))
        		.contentType("application/json")
        		.accept(MediaType.APPLICATION_JSON))
        .andExpect(status().is2xxSuccessful())
        .andExpect(content().contentType("application/json;charset=UTF-8"))
        .andExpect(jsonPath("$.code", is(HttpStatus.CREATED.value())))
        .andExpect(jsonPath("$.status", is("success")))
        .andExpect(jsonPath("$.timestamp",greaterThan(1400000000)))
        .andExpect(jsonPath("$.result").isMap())
        .andExpect(jsonPath("$.result.name", is("smartff")))
        .andExpect(jsonPath("$.result.friendlyName", is("Smart Frig")))
        .andExpect(jsonPath("$.result.description", is("Description of smartff")));
    }

    @Test
    public void shouldTryCreateApplicationWithBadRequest() throws Exception {
        when(
        	applicationService
        		.register(org.mockito.Matchers.any(Tenant.class), org.mockito.Matchers.any(Application.class)))
        .thenReturn(ServiceResponseBuilder
        				.<Application>error()
        				.withMessage(ApplicationService.Validations.APPLICATION_NOT_FOUND.getCode())
        				.build());

        getMockMvc()
        .perform(MockMvcRequestBuilders.post("/applications/")
        		.content(getJson(new ApplicationVO().apply(application1)))
        		.contentType(MediaType.APPLICATION_JSON)
        		.accept(MediaType.APPLICATION_JSON))
        .andExpect(status().is4xxClientError())
        .andExpect(content().contentType("application/json;charset=UTF-8"))
        .andExpect(jsonPath("$.code", is(HttpStatus.BAD_REQUEST.value())))
        .andExpect(jsonPath("$.status", is("error")))
        .andExpect(jsonPath("$.timestamp", greaterThan(1400000000)))
        .andExpect(jsonPath("$.messages").exists())
        .andExpect(jsonPath("$.result").doesNotExist());

    }

    @Test
    public void shouldUpdateApplication() throws Exception {
        when(
        	applicationService
        		.getByApplicationName(tenant, application1.getName()))
        .thenReturn(ServiceResponseBuilder.<Application>ok().withResult(application1).build());

        when(
        	applicationService
        		.update(org.mockito.Matchers.any(Tenant.class), org.mockito.Matchers.anyString(), org.mockito.Matchers.any(Application.class)))
        .thenReturn(ServiceResponseBuilder.<Application>ok().withResult(application1).build());

        getMockMvc()
        .perform(MockMvcRequestBuilders.put("/applications/" + application1.getName())
        		.content(getJson(new ApplicationVO().apply(application1)))
        		.contentType(MediaType.APPLICATION_JSON)
        		.accept(MediaType.APPLICATION_JSON))
        .andExpect(status().is2xxSuccessful())
        .andExpect(jsonPath("$.code", is(HttpStatus.OK.value())))
        .andExpect(jsonPath("$.status", is("success")))
        .andExpect(jsonPath("$.timestamp", greaterThan(1400000000)))
        .andExpect(jsonPath("$.result").doesNotExist());

    }

    @Test
    public void shouldReturnInternalErrorWhenUpdateApplication() throws Exception {
    	when(
    		applicationService
    			.getByApplicationName(tenant, application1.getName()))
    	.thenReturn(ServiceResponseBuilder.<Application>ok().withResult(application1).build());

    	when(
    		applicationService
    			.update(org.mockito.Matchers.any(Tenant.class), org.mockito.Matchers.anyString(), org.mockito.Matchers.any(Application.class)))
    	.thenReturn(ServiceResponseBuilder.<Application>error().build());

    	getMockMvc()
    	.perform(MockMvcRequestBuilders.put("/applications/" + application1.getName())
    			.content(getJson(new ApplicationVO().apply(application1)))
    			.contentType(MediaType.APPLICATION_JSON)
    			.accept(MediaType.APPLICATION_JSON))
    	.andExpect(status().is5xxServerError())
    	.andExpect(content().contentType("application/json;charset=UTF-8"))
    	.andExpect(jsonPath("$.code", is(HttpStatus.INTERNAL_SERVER_ERROR.value())))
    	.andExpect(jsonPath("$.status", is("error")))
    	.andExpect(jsonPath("$.timestamp", greaterThan(1400000000)))
    	.andExpect(jsonPath("$.messages").doesNotExist())
    	.andExpect(jsonPath("$.result").doesNotExist());
    }

    @Test
    public void shouldDeleteApplication() throws Exception {
        when(applicationService.remove(tenant, application1.getName()))
                .thenReturn(ServiceResponseBuilder.<Application>ok().build());

        getMockMvc()
        .perform(MockMvcRequestBuilders.delete("/applications/" + application1.getName())
        		.contentType("application/json")
        		.accept(MediaType.APPLICATION_JSON))
        .andExpect(status().is2xxSuccessful())
        .andExpect(content().contentType("application/json;charset=UTF-8"))
        .andExpect(jsonPath("$.code", is(HttpStatus.NO_CONTENT.value())))
        .andExpect(jsonPath("$.status", is("success")))
        .andExpect(jsonPath("$.timestamp",greaterThan(1400000000)))
        .andExpect(jsonPath("$.result").doesNotExist());
    }

    @Test
    public void shouldReturnInternalErrorWhenDeleteApplication() throws Exception {
    	when(applicationService.remove(tenant, application1.getName()))
    	.thenReturn(ServiceResponseBuilder
    					.<Application>error()
    					.withMessage(ApplicationService.Messages.APPLICATION_REMOVED_SUCCESSFULLY.getCode())
    					.build());

        getMockMvc()
        .perform(MockMvcRequestBuilders.delete("/applications/" + application1.getName())
        		.contentType("application/json")
        		.accept(MediaType.APPLICATION_JSON))
        .andExpect(status().is5xxServerError())
        .andExpect(content().contentType("application/json;charset=UTF-8"))
        .andExpect(jsonPath("$.code", is(HttpStatus.INTERNAL_SERVER_ERROR.value())))
        .andExpect(jsonPath("$.status", is("error")))
        .andExpect(jsonPath("$.timestamp",greaterThan(1400000000)))
        .andExpect(jsonPath("$.messages").exists())
        .andExpect(jsonPath("$.result").doesNotExist());
    }

    @Test
    public void shouldTryDeleteNonexistentApplication() throws Exception {
    	when(applicationService.remove(tenant, application1.getName()))
    	.thenReturn(
    			ServiceResponseBuilder
    				.<Application>error()
    				.withMessage(ApplicationService.Validations.APPLICATION_NOT_FOUND.getCode())
    				.build());

    	getMockMvc().perform(MockMvcRequestBuilders.delete("/applications/" + application1.getName())
    			.contentType("application/json")
    			.accept(MediaType.APPLICATION_JSON))
    	.andExpect(status().is4xxClientError())
    	.andExpect(content().contentType("application/json;charset=UTF-8"))
    	.andExpect(jsonPath("$.code", is(HttpStatus.NOT_FOUND.value())))
    	.andExpect(jsonPath("$.status", is("error")))
    	.andExpect(jsonPath("$.timestamp",greaterThan(1400000000)))
    	.andExpect(jsonPath("$.messages").exists())
    	.andExpect(jsonPath("$.result").doesNotExist());

    }
}
