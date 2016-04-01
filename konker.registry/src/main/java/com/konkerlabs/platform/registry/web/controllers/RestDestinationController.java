package com.konkerlabs.platform.registry.web.controllers;

import com.konkerlabs.platform.registry.business.model.RestDestination;
import com.konkerlabs.platform.registry.business.model.Tenant;
import com.konkerlabs.platform.registry.business.services.api.RestDestinationService;
import com.konkerlabs.platform.registry.business.services.api.ServiceResponse;
import com.konkerlabs.platform.registry.web.forms.RestDestinationForm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.function.Supplier;

import static java.text.MessageFormat.format;

@Controller
@Scope("request")
@RequestMapping("destinations/rest")
public class RestDestinationController {

    @Autowired
    private RestDestinationService restDestinationService;
    @Autowired
    private Tenant tenant;

    @RequestMapping
    public ModelAndView index() {
        return new ModelAndView("destinations/rest/index")
            .addObject("allDestinations", restDestinationService.findAll(tenant).getResult());
    }

    @RequestMapping("new")
    public ModelAndView newDestination() {
        return new ModelAndView("destinations/rest/form")
                .addObject("destination", new RestDestinationForm())
                .addObject("action", "/destinations/rest/save");
    }

    @RequestMapping(value = "save", method = RequestMethod.POST)
    public ModelAndView saveNew(@ModelAttribute("destinationForm") RestDestinationForm destinationForm,
                                RedirectAttributes redirectAttributes) {
        return doSave(
                () -> restDestinationService.register(tenant,destinationForm.toModel()),
                destinationForm,
                redirectAttributes, "");
    }

    @RequestMapping(value = "/{guid}", method = RequestMethod.GET)
    public ModelAndView show(@PathVariable("guid") String guid) {
        return new ModelAndView("destinations/rest/show")
            .addObject("destination",new RestDestinationForm()
                    .fillFrom(restDestinationService.getByGUID(tenant,guid).getResult()));
    }

    @RequestMapping("/{guid}/edit")
    public ModelAndView edit(@PathVariable("guid") String guid) {
        return new ModelAndView("destinations/rest/form")
                .addObject("destination",new RestDestinationForm()
                        .fillFrom(restDestinationService.getByGUID(tenant,guid).getResult()))
                .addObject("action",format("/destinations/rest/{0}",guid))
                .addObject("method", "put");
    }

    @RequestMapping(value = "/{guid}", method = RequestMethod.PUT)
    public ModelAndView saveEdit(@PathVariable String guid,
                                @ModelAttribute("destinationForm") RestDestinationForm destinationForm,
                                RedirectAttributes redirectAttributes) {
        return doSave(
                () -> restDestinationService.update(tenant,guid,destinationForm.toModel()),
                destinationForm,
                redirectAttributes, "put");
    }

    private ModelAndView doSave(Supplier<ServiceResponse<RestDestination>> responseSupplier,
                                RestDestinationForm destinationForm,
                                RedirectAttributes redirectAttributes, String method) {

        ServiceResponse<RestDestination> serviceResponse = responseSupplier.get();

        switch (serviceResponse.getStatus()) {
            case ERROR: {
                return new ModelAndView("destinations/rest/form")
                        .addObject("errors", serviceResponse.getResponseMessages())
                        .addObject("method", method)
                        .addObject("destination", destinationForm);
            }
            default: {
                redirectAttributes.addFlashAttribute("message", "Destination saved successfully");
                return new ModelAndView(
                        format("redirect:/destinations/rest/{0}", serviceResponse.getResult().getGuid())
                );
            }
        }
    }
}
