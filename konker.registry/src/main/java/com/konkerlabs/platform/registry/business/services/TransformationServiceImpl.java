package com.konkerlabs.platform.registry.business.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.konkerlabs.platform.registry.business.services.api.ServiceResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.konkerlabs.platform.registry.business.model.EventRoute;
import com.konkerlabs.platform.registry.business.model.Tenant;
import com.konkerlabs.platform.registry.business.model.Transformation;
import com.konkerlabs.platform.registry.business.model.validation.CommonValidations;
import com.konkerlabs.platform.registry.business.repositories.EventRouteRepository;
import com.konkerlabs.platform.registry.business.repositories.TenantRepository;
import com.konkerlabs.platform.registry.business.repositories.TransformationRepository;
import com.konkerlabs.platform.registry.business.services.api.ServiceResponseBuilder;
import com.konkerlabs.platform.registry.business.services.api.TransformationService;

@Service
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class TransformationServiceImpl implements TransformationService {

    @Autowired
    private TenantRepository tenantRepository;
    @Autowired
    private TransformationRepository transformationRepository;
    @Autowired
    private EventRouteRepository eventRouteRepository;

    @Override
    public ServiceResponse<List<Transformation>> getAll(Tenant tenant) {
        return ServiceResponseBuilder.<List<Transformation>>ok()
            .withResult(transformationRepository.findAllByTenantId(tenant.getId())).build();
    }
    

    @Override
    public ServiceResponse<Transformation> register(Tenant tenant, Transformation transformation) {
        if (!Optional.ofNullable(tenant).isPresent())
            return ServiceResponseBuilder.<Transformation>error()
                    .withMessage(CommonValidations.TENANT_NULL.getCode()).build();

        if (!Optional.ofNullable(transformation).isPresent())
            return ServiceResponseBuilder.<Transformation>error()
                    .withMessage(CommonValidations.RECORD_NULL.getCode()).build();

        if (!tenantRepository.exists(tenant.getId()))
            return ServiceResponseBuilder.<Transformation>error()
                    .withMessage(CommonValidations.TENANT_DOES_NOT_EXIST.getCode()).build();

        transformation.setTenant(tenant);
        transformation.setGuid(UUID.randomUUID().toString());

        Optional<Map<String, Object[]>> validations = transformation.applyValidations();

        if (validations.isPresent())
            return ServiceResponseBuilder.<Transformation>error()
                    .withMessages(validations.get()).build();

        if (Optional.ofNullable(transformationRepository.findByName(tenant.getId(),transformation.getName()))
                .filter(transformations -> !transformations.isEmpty()).isPresent()) {
            return ServiceResponseBuilder.<Transformation>error()
                    .withMessage(Validations.TRANSFORMATION_NAME_IN_USE.getCode()).build();
        }

        Transformation saved = transformationRepository.save(transformation);

        return ServiceResponseBuilder.<Transformation>ok()
                .withResult(saved).<Transformation>build();
    }

	@Override
	public ServiceResponse<Transformation> get(Tenant tenant, String guid) {
		return ServiceResponseBuilder.<Transformation> ok()
				.withResult(transformationRepository.findByTenantIdAndTransformationGuid(tenant.getId(), guid)).build();
	}

    @Override
    public ServiceResponse<Transformation> update(Tenant tenant, String guid, Transformation transformation) {
        if (!Optional.ofNullable(tenant).isPresent())
            return ServiceResponseBuilder.<Transformation>error()
                    .withMessage(CommonValidations.TENANT_NULL.getCode()).build();

        if (!Optional.ofNullable(transformation).isPresent())
            return ServiceResponseBuilder.<Transformation>error()
                    .withMessage(CommonValidations.RECORD_NULL.getCode()).build();

        if (!tenantRepository.exists(tenant.getId()))
            return ServiceResponseBuilder.<Transformation>error()
                    .withMessage(CommonValidations.TENANT_DOES_NOT_EXIST.getCode()).build();

        Transformation fromDb = get(tenant, guid).getResult();

        if (!Optional.ofNullable(fromDb).isPresent())
            return ServiceResponseBuilder.<Transformation>error()
                    .withMessage(Validations.TRANSFORMATION_NOT_FOUND.getCode()).build();

        fromDb.setName(transformation.getName());
        fromDb.setDescription(transformation.getDescription());
        fromDb.setSteps(transformation.getSteps());

        Optional<Map<String, Object[]>> validations = fromDb.applyValidations();

        if (validations.isPresent())
            return ServiceResponseBuilder.<Transformation>error()
                    .withMessages(validations.get()).build();

        if (Optional.ofNullable(transformationRepository.findByName(fromDb.getTenant().getId(),fromDb.getName()))
                .filter(transformations -> !transformations.isEmpty())
                .orElseGet(ArrayList<Transformation>::new)
                .stream().anyMatch(transformation1 -> !transformation1.getId().equals(fromDb.getId()))) {
            return ServiceResponseBuilder.<Transformation>error()
                    .withMessage(Validations.TRANSFORMATION_NAME_IN_USE.getCode()).build();
        }


        Transformation saved = transformationRepository.save(fromDb);

        return ServiceResponseBuilder.<Transformation>ok()
                .withResult(saved).build();
    }

	@Override
	public ServiceResponse<Transformation> remove(Tenant tenant, String transformationGuid) {

		Transformation transformation = transformationRepository.findByGuid(transformationGuid);

		List<EventRoute> eventRoutes = eventRouteRepository.findByTenantIdAndTransformationId(tenant.getId(),
				transformation.getId());

		if (!eventRoutes.isEmpty()) {
			return ServiceResponseBuilder.<Transformation> error()
					.withMessage(Validations.TRANSFORMATION_HAS_ROUTE.getCode()).build();
		}

		if (Optional.ofNullable(transformation).isPresent()
				&& !transformation.getTenant().getId().equals(tenant.getId())) {
			return ServiceResponseBuilder.<Transformation> error()
					.withMessage(Validations.TRANSFORMATION_BELONG_ANOTHER_TENANT.getCode()).build();
		}

		transformationRepository.delete(transformation);

		return ServiceResponseBuilder.<Transformation> ok().build();
	}
}