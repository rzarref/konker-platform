package com.konkerlabs.platform.registry.business.services;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.konkerlabs.platform.registry.business.model.AlertTriggerType;
import com.konkerlabs.platform.registry.business.model.Application;
import com.konkerlabs.platform.registry.business.model.DeviceModel;
import com.konkerlabs.platform.registry.business.model.Location;
import com.konkerlabs.platform.registry.business.model.SilenceTrigger;
import com.konkerlabs.platform.registry.business.model.Tenant;
import com.konkerlabs.platform.registry.business.model.validation.CommonValidations;
import com.konkerlabs.platform.registry.business.repositories.SilenceTriggerRepository;
import com.konkerlabs.platform.registry.business.services.api.ApplicationService;
import com.konkerlabs.platform.registry.business.services.api.ServiceResponse;
import com.konkerlabs.platform.registry.business.services.api.ServiceResponseBuilder;
import com.konkerlabs.platform.registry.business.services.api.SilenceTriggerService;

@Service
public class SilenceTriggerServiceImpl implements SilenceTriggerService {

    @Autowired
    private SilenceTriggerRepository silenceTriggerRepository;

    @Override
    public ServiceResponse<SilenceTrigger> findByTenantAndApplicationAndModelAndLocation(Tenant tenant,
            Application application, DeviceModel model, Location location) {

        ServiceResponse<SilenceTrigger> validationsResponse = validate(tenant, application);
        if (validationsResponse != null && !validationsResponse.isOk()) {
            return validationsResponse;
        }

        SilenceTrigger trigger = silenceTriggerRepository.findByTenantAndApplicationAndDeviceModelAndLocation(
                tenant.getId(), application.getName(), model.getId(), location.getId());

        if (trigger == null) {
            return ServiceResponseBuilder.<SilenceTrigger>error().withMessage(Validations.SILENCE_TRIGGER_NOT_FOUND.getCode()).withResult(trigger).build();
        } else {
            return ServiceResponseBuilder.<SilenceTrigger>ok().withResult(trigger).build();
        }

    }

    @Override
    public ServiceResponse<List<SilenceTrigger>> listByTenantAndApplication(Tenant tenant, Application application) {

        ServiceResponse<List<SilenceTrigger>> validationsResponse = validate(tenant, application);
        if (validationsResponse != null && !validationsResponse.isOk()) {
            return validationsResponse;
        }

        List<SilenceTrigger> triggers = silenceTriggerRepository.listByTenantIdAndApplicationName(tenant.getId(),
                application.getName());

        return ServiceResponseBuilder.<List<SilenceTrigger>>ok().withResult(triggers).build();

    }

    @Override
    public ServiceResponse<SilenceTrigger> save(Tenant tenant, Application application, SilenceTrigger trigger) {

        ServiceResponse<SilenceTrigger> validationsResponse = validate(tenant, application);
        if (validationsResponse != null && !validationsResponse.isOk()) {
            return validationsResponse;
        }

        Optional<Map<String, Object[]>> validations = trigger.applyValidations();
        if (validations.isPresent()) {
            return ServiceResponseBuilder.<SilenceTrigger>error().withMessages(validations.get()).build();
        }

        trigger.setTenant(tenant);
        trigger.setApplication(application);
        trigger.setGuid(UUID.randomUUID().toString());
        trigger.setType(AlertTriggerType.SILENCE);

        SilenceTrigger saved = silenceTriggerRepository.save(trigger);

        return ServiceResponseBuilder.<SilenceTrigger>ok().withResult(saved).build();

    }

    @Override
    public ServiceResponse<SilenceTrigger> update(Tenant tenant, Application application, String guid,
            SilenceTrigger trigger) {

        ServiceResponse<SilenceTrigger> validationsResponse = validate(tenant, application);
        if (validationsResponse != null && !validationsResponse.isOk()) {
            return validationsResponse;
        }

        Optional<Map<String, Object[]>> validations = trigger.applyValidations();
        if (validations.isPresent()) {
            return ServiceResponseBuilder.<SilenceTrigger>error().withMessages(validations.get()).build();
        }

        SilenceTrigger fromDb = silenceTriggerRepository.findByTenantIdApplicationIdAndGuid(tenant.getId(),
                application.getName(), guid);

        if (!Optional.ofNullable(fromDb).isPresent()) {
            return ServiceResponseBuilder.<SilenceTrigger>error()
                    .withMessage(Validations.SILENCE_TRIGGER_NOT_FOUND.getCode()).build();
        }

        fromDb.setMinutes(trigger.getMinutes());

        SilenceTrigger saved = silenceTriggerRepository.save(fromDb);

        return ServiceResponseBuilder.<SilenceTrigger>ok().withResult(saved).build();

    }

    @Override
    public ServiceResponse<SilenceTrigger> remove(Tenant tenant, Application application, String guid) {

        ServiceResponse<SilenceTrigger> validationsResponse = validate(tenant, application);
        if (validationsResponse != null && !validationsResponse.isOk()) {
            return validationsResponse;
        }

        SilenceTrigger fromDb = silenceTriggerRepository.findByTenantIdApplicationIdAndGuid(tenant.getId(),
                application.getName(), guid);

        if (!Optional.ofNullable(fromDb).isPresent()) {
            return ServiceResponseBuilder.<SilenceTrigger>error()
                    .withMessage(Validations.SILENCE_TRIGGER_NOT_FOUND.getCode()).build();
        }

        silenceTriggerRepository.delete(fromDb);

        return ServiceResponseBuilder.<SilenceTrigger>ok().withResult(fromDb).build();

    }

    private <T> ServiceResponse<T> validate(Tenant tenant, Application application) {

        if (!Optional.ofNullable(tenant).isPresent()) {
            return ServiceResponseBuilder.<T>error().withMessage(CommonValidations.TENANT_NULL.getCode()).build();
        }

        if (!Optional.ofNullable(application).isPresent()) {
            return ServiceResponseBuilder.<T>error()
                    .withMessage(ApplicationService.Validations.APPLICATION_NULL.getCode()).build();
        }

        return null;
    }

}
