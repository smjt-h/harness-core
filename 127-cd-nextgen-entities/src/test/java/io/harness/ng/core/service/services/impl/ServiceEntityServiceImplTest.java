/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.core.service.services.impl;

import static io.harness.rule.OwnerRule.ARCHIT;
import static io.harness.rule.OwnerRule.DEEPAK;
import static io.harness.rule.OwnerRule.MOHIT_GARG;
import static io.harness.rule.OwnerRule.PRABU;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.ReferencedEntityException;
import io.harness.ng.core.EntityDetail;
import io.harness.ng.core.NGCoreTestBase;
import io.harness.ng.core.entitysetupusage.dto.EntitySetupUsageDTO;
import io.harness.ng.core.entitysetupusage.impl.EntitySetupUsageServiceImpl;
import io.harness.ng.core.service.dto.ServiceResponseDTO;
import io.harness.ng.core.service.entity.ServiceEntity;
import io.harness.ng.core.service.mappers.ServiceElementMapper;
import io.harness.ng.core.utils.CoreCriteriaUtils;
import io.harness.rule.Owner;
import io.harness.utils.PageUtils;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.joor.Reflect;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

@OwnedBy(HarnessTeam.CDC)
public class ServiceEntityServiceImplTest extends NGCoreTestBase {
  @Mock EntitySetupUsageServiceImpl entitySetupUsageService;
  @Inject @InjectMocks ServiceEntityServiceImpl serviceEntityService;
  private static final String ACCOUNT_ID = "ACCOUNT_ID";
  private static final String ORG_ID = "ORG_ID";
  private static final String PROJECT_ID = "PROJECT_ID";

  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Before
  public void setup() {
    entitySetupUsageService = Mockito.mock(EntitySetupUsageServiceImpl.class);
    Reflect.on(serviceEntityService).set("entitySetupUsageService", entitySetupUsageService);
  }

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testValidatePresenceOfRequiredFields() {
    assertThatThrownBy(() -> serviceEntityService.validatePresenceOfRequiredFields("", null, "2"))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("One of the required fields is null.");
  }

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testServiceLayer() {
    ServiceEntity serviceEntity = ServiceEntity.builder()
                                      .accountId("ACCOUNT_ID")
                                      .identifier("IDENTIFIER")
                                      .orgIdentifier("ORG_ID")
                                      .projectIdentifier("PROJECT_ID")
                                      .name("Service")
                                      .build();

    // Create operations
    ServiceEntity createdService = serviceEntityService.create(serviceEntity);
    assertThat(createdService).isNotNull();
    assertThat(createdService.getAccountId()).isEqualTo(serviceEntity.getAccountId());
    assertThat(createdService.getOrgIdentifier()).isEqualTo(serviceEntity.getOrgIdentifier());
    assertThat(createdService.getProjectIdentifier()).isEqualTo(serviceEntity.getProjectIdentifier());
    assertThat(createdService.getIdentifier()).isEqualTo(serviceEntity.getIdentifier());
    assertThat(createdService.getName()).isEqualTo(serviceEntity.getName());
    assertThat(createdService.getVersion()).isEqualTo(0L);

    // Get operations
    Optional<ServiceEntity> getService =
        serviceEntityService.get("ACCOUNT_ID", "ORG_ID", "PROJECT_ID", "IDENTIFIER", false);
    assertThat(getService).isPresent();
    assertThat(getService.get()).isEqualTo(createdService);

    // Update operations
    ServiceEntity updateServiceRequest = ServiceEntity.builder()
                                             .accountId("ACCOUNT_ID")
                                             .identifier("IDENTIFIER")
                                             .orgIdentifier("ORG_ID")
                                             .projectIdentifier("PROJECT_ID")
                                             .name("UPDATED_SERVICE")
                                             .description("NEW_DESCRIPTION")
                                             .build();
    ServiceEntity updatedServiceResponse = serviceEntityService.update(updateServiceRequest);
    assertThat(updatedServiceResponse.getAccountId()).isEqualTo(updateServiceRequest.getAccountId());
    assertThat(updatedServiceResponse.getOrgIdentifier()).isEqualTo(updateServiceRequest.getOrgIdentifier());
    assertThat(updatedServiceResponse.getProjectIdentifier()).isEqualTo(updateServiceRequest.getProjectIdentifier());
    assertThat(updatedServiceResponse.getIdentifier()).isEqualTo(updateServiceRequest.getIdentifier());
    assertThat(updatedServiceResponse.getName()).isEqualTo(updateServiceRequest.getName());
    assertThat(updatedServiceResponse.getDescription()).isEqualTo(updateServiceRequest.getDescription());
    assertThat(updatedServiceResponse.getVersion()).isEqualTo(1L);

    updateServiceRequest.setAccountId("NEW_ACCOUNT");
    assertThatThrownBy(() -> serviceEntityService.update(updateServiceRequest))
        .isInstanceOf(InvalidRequestException.class);
    updatedServiceResponse.setAccountId("ACCOUNT_ID");

    // Upsert operations
    ServiceEntity upsertServiceRequest = ServiceEntity.builder()
                                             .accountId("ACCOUNT_ID")
                                             .identifier("NEW_IDENTIFIER")
                                             .orgIdentifier("ORG_ID")
                                             .projectIdentifier("NEW_PROJECT")
                                             .name("UPSERTED_SERVICE")
                                             .description("NEW_DESCRIPTION")
                                             .build();
    ServiceEntity upsertService = serviceEntityService.upsert(upsertServiceRequest);
    assertThat(upsertService.getAccountId()).isEqualTo(upsertServiceRequest.getAccountId());
    assertThat(upsertService.getOrgIdentifier()).isEqualTo(upsertServiceRequest.getOrgIdentifier());
    assertThat(upsertService.getProjectIdentifier()).isEqualTo(upsertServiceRequest.getProjectIdentifier());
    assertThat(upsertService.getIdentifier()).isEqualTo(upsertServiceRequest.getIdentifier());
    assertThat(upsertService.getName()).isEqualTo(upsertServiceRequest.getName());
    assertThat(upsertService.getDescription()).isEqualTo(upsertServiceRequest.getDescription());

    // List services operations.
    Criteria criteriaFromServiceFilter =
        CoreCriteriaUtils.createCriteriaForGetList("ACCOUNT_ID", "ORG_ID", "PROJECT_ID", false);
    Pageable pageRequest = PageUtils.getPageRequest(0, 10, null);
    Page<ServiceEntity> list = serviceEntityService.list(criteriaFromServiceFilter, pageRequest);
    assertThat(list.getContent()).isNotNull();
    assertThat(list.getContent().size()).isEqualTo(1);
    assertThat(ServiceElementMapper.writeDTO(list.getContent().get(0)))
        .isEqualTo(ServiceElementMapper.writeDTO(updatedServiceResponse));

    criteriaFromServiceFilter = CoreCriteriaUtils.createCriteriaForGetList("ACCOUNT_ID", "ORG_ID", null, false);

    list = serviceEntityService.list(criteriaFromServiceFilter, pageRequest);
    assertThat(list.getContent()).isNotNull();
    assertThat(list.getContent().size()).isEqualTo(0);

    // Upsert operations for org level
    ServiceEntity upsertServiceRequestOrgLevel = ServiceEntity.builder()
                                                     .accountId("ACCOUNT_ID")
                                                     .identifier("NEW_IDENTIFIER")
                                                     .orgIdentifier("ORG_ID")
                                                     .name("UPSERTED_SERVICE")
                                                     .description("NEW_DESCRIPTION")
                                                     .build();
    upsertService = serviceEntityService.upsert(upsertServiceRequestOrgLevel);
    assertThat(upsertService.getAccountId()).isEqualTo(upsertServiceRequest.getAccountId());
    assertThat(upsertService.getOrgIdentifier()).isEqualTo(upsertServiceRequest.getOrgIdentifier());
    assertThat(upsertService.getProjectIdentifier()).isNull();
    assertThat(upsertService.getIdentifier()).isEqualTo(upsertServiceRequest.getIdentifier());
    assertThat(upsertService.getName()).isEqualTo(upsertServiceRequest.getName());
    assertThat(upsertService.getDescription()).isEqualTo(upsertServiceRequest.getDescription());

    criteriaFromServiceFilter = CoreCriteriaUtils.createCriteriaForGetList("ACCOUNT_ID", "ORG_ID", null, false);

    list = serviceEntityService.list(criteriaFromServiceFilter, pageRequest);
    assertThat(list.getContent()).isNotNull();
    assertThat(list.getContent().size()).isEqualTo(1);
    List<ServiceResponseDTO> dtoList =
        list.getContent().stream().map(ServiceElementMapper::writeDTO).collect(Collectors.toList());
    assertThat(dtoList).containsOnly(ServiceElementMapper.writeDTO(upsertService));

    // Delete operations
    when(entitySetupUsageService.listAllEntityUsage(anyInt(), anyInt(), anyString(), anyString(), any(), anyString()))
        .thenReturn(Page.empty());
    boolean delete = serviceEntityService.delete("ACCOUNT_ID", "ORG_ID", "PROJECT_ID", "IDENTIFIER", 1L);
    assertThat(delete).isTrue();

    Optional<ServiceEntity> deletedService =
        serviceEntityService.get("ACCOUNT_ID", "ORG_ID", "PROJECT_ID", "UPDATED_SERVICE", false);
    assertThat(deletedService.isPresent()).isFalse();
  }

  @Test
  @Owner(developers = DEEPAK)
  @Category(UnitTests.class)
  public void testBulkCreate() {
    List<ServiceEntity> serviceEntities = new ArrayList<>();
    for (int i = 0; i < 5; i++) {
      String serviceIdentifier = "identifier " + i;
      String serviceName = "serviceName " + i;
      ServiceEntity serviceEntity = createServiceEntity(serviceIdentifier, serviceName);
      serviceEntities.add(serviceEntity);
    }
    serviceEntityService.bulkCreate(ACCOUNT_ID, serviceEntities);
    for (int i = 0; i < 5; i++) {
      String serviceIdentifier = "identifier " + i;
      Optional<ServiceEntity> serviceEntitySaved =
          serviceEntityService.get(ACCOUNT_ID, ORG_ID, PROJECT_ID, serviceIdentifier, false);
      assertThat(serviceEntitySaved.isPresent()).isTrue();
    }
  }

  @Test
  @Owner(developers = MOHIT_GARG)
  @Category(UnitTests.class)
  public void testGetAllServices() {
    List<ServiceEntity> serviceEntities = new ArrayList<>();
    int pageSize = 1000;
    int numOfServices = pageSize * 2 + 100; // creating adhoc num of services, not in multiples of page size
    for (int i = 0; i < numOfServices; i++) {
      String serviceIdentifier = "identifier " + i;
      String serviceName = "serviceName " + i;
      ServiceEntity serviceEntity = createServiceEntity(serviceIdentifier, serviceName);
      serviceEntities.add(serviceEntity);
    }
    serviceEntityService.bulkCreate(ACCOUNT_ID, serviceEntities);

    List<ServiceEntity> serviceEntityList =
        serviceEntityService.getAllServices(ACCOUNT_ID, ORG_ID, PROJECT_ID, pageSize, true);
    assertThat(serviceEntityList.size()).isEqualTo(numOfServices);
  }

  @Test
  @Owner(developers = PRABU)
  @Category(UnitTests.class)
  public void testGetAllNonDeletedServices() {
    List<ServiceEntity> serviceEntities = new ArrayList<>();
    int numOfServices = 20;
    for (int i = 0; i < numOfServices; i++) {
      String serviceIdentifier = "identifier " + i;
      String serviceName = "serviceName " + i;
      ServiceEntity serviceEntity = createServiceEntity(serviceIdentifier, serviceName);
      serviceEntity.setDeleted(i % 2 == 0); // Every alternate service is deleted
      serviceEntities.add(serviceEntity);
    }
    serviceEntityService.bulkCreate(ACCOUNT_ID, serviceEntities);

    List<ServiceEntity> serviceEntityList =
        serviceEntityService.getAllNonDeletedServices(ACCOUNT_ID, ORG_ID, PROJECT_ID);
    assertThat(serviceEntityList.size()).isEqualTo(numOfServices / 2);
  }

  @Test
  @Owner(developers = MOHIT_GARG)
  @Category(UnitTests.class)
  public void testGetActiveServiceCount() {
    List<ServiceEntity> serviceEntities = new ArrayList<>();
    for (int i = 1; i <= 20; i++) {
      String serviceIdentifier = "identifier " + i;
      String serviceName = "serviceName " + i;
      ServiceEntity serviceEntity = createServiceEntity(serviceIdentifier, serviceName);
      serviceEntity.setCreatedAt((long) i);
      if (i % 5 == 0) {
        serviceEntity.setDeleted(true);
        serviceEntity.setDeletedAt((long) (i + 5));
      }
      serviceEntities.add(serviceEntity);
    }
    serviceEntityService.bulkCreate(ACCOUNT_ID, serviceEntities);
    Integer activeServiceCount =
        serviceEntityService.findActiveServicesCountAtGivenTimestamp(ACCOUNT_ID, ORG_ID, PROJECT_ID, 16);
    assertThat(activeServiceCount).isEqualTo(16 - 2);
  }

  private ServiceEntity createServiceEntity(String identifier, String name) {
    return ServiceEntity.builder()
        .accountId(ACCOUNT_ID)
        .identifier(identifier)
        .orgIdentifier(ORG_ID)
        .projectIdentifier(PROJECT_ID)
        .name(name)
        .deleted(false)
        .build();
  }

  @Test
  @Owner(developers = DEEPAK)
  @Category(UnitTests.class)
  public void testGetDuplicateServiceExistsErrorMessage() {
    String errorMessage = "Bulk write operation error on server localhost:27017. "
        + "Write errors: [BulkWriteError{index=0, code=11000, "
        + "message='E11000 duplicate key error collection: ng-harness.servicesNG "
        + "index: unique_accountId_organizationIdentifier_projectIdentifier_serviceIdentifier "
        + "dup key: { accountId: \"kmpySmUISimoRrJL6NL73w\", orgIdentifier: \"default\", "
        + "projectIdentifier: \"Nofar\", identifier: \"service_5\" }'";
    String errorMessageToBeShownToUser =
        serviceEntityService.getDuplicateServiceExistsErrorMessage("kmpySmUISimoRrJL6NL73w", errorMessage);
    assertThat(errorMessageToBeShownToUser)
        .isEqualTo(
            "Service [service_5] under Project[Nofar], Organization [default] in Account [kmpySmUISimoRrJL6NL73w] already exists");
  }

  @Test
  @Owner(developers = PRABU)
  @Category(UnitTests.class)
  public void testErrorMessageWhenServiceIsReferenced() {
    List<EntitySetupUsageDTO> referencedByEntities = Arrays.asList(getEntitySetupUsageDTO());
    when(entitySetupUsageService.listAllEntityUsage(anyInt(), anyInt(), anyString(), anyString(), any(), anyString()))
        .thenReturn(new PageImpl<>(referencedByEntities));
    assertThatThrownBy(() -> serviceEntityService.delete(ACCOUNT_ID, ORG_ID, PROJECT_ID, "SERVICE", 0L))
        .isInstanceOf(ReferencedEntityException.class)
        .hasMessage(
            "The service SERVICE cannot be deleted because it is being referenced in 1 entity. To delete your service, please remove the reference service from these entities.");

    referencedByEntities = Arrays.asList(getEntitySetupUsageDTO(), getEntitySetupUsageDTO());
    when(entitySetupUsageService.listAllEntityUsage(anyInt(), anyInt(), anyString(), anyString(), any(), anyString()))
        .thenReturn(new PageImpl<>(referencedByEntities));
    assertThatThrownBy(() -> serviceEntityService.delete(ACCOUNT_ID, ORG_ID, PROJECT_ID, "SERVICE", 0L))
        .isInstanceOf(ReferencedEntityException.class)
        .hasMessage(
            "The service SERVICE cannot be deleted because it is being referenced in 2 entities. To delete your service, please remove the reference service from these entities.");
  }

  private EntitySetupUsageDTO getEntitySetupUsageDTO() {
    return EntitySetupUsageDTO.builder().referredByEntity(EntityDetail.builder().build()).build();
  }
}
