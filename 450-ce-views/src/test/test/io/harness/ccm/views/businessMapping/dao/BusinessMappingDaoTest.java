/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ccm.views.businessMapping.dao;

import static io.harness.persistence.HQuery.excludeValidate;
import static io.harness.rule.OwnerRule.SAHILDEEP;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.ccm.views.businessMapping.entities.BusinessMapping;
import io.harness.ccm.views.businessMapping.entities.BusinessMapping.BusinessMappingKeys;
import io.harness.ccm.views.businessMapping.helper.BusinessMappingHelper;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;

import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mongodb.morphia.query.FieldEnd;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

@RunWith(MockitoJUnitRunner.class)
public class BusinessMappingDaoTest extends CategoryTest {
  @Mock private HPersistence hPersistence;
  @Mock private Query<BusinessMapping> query;
  @Mock private FieldEnd fieldEnd;
  @Mock private UpdateOperations<BusinessMapping> updateOperations;
  @InjectMocks private BusinessMappingDao businessMappingDao;
  private BusinessMapping businessMapping;

  @Before
  public void setUp() {
    businessMapping = BusinessMappingHelper.getBusinessMapping(BusinessMappingHelper.TEST_ID);
  }

  @Test
  @Owner(developers = SAHILDEEP)
  @Category(UnitTests.class)
  public void testSave() {
    when(hPersistence.save(any(BusinessMapping.class))).thenReturn(BusinessMappingHelper.TEST_ID);
    final BusinessMapping response = businessMappingDao.save(businessMapping);
    verify(hPersistence).save(businessMapping);
    assertThat(response).isEqualTo(businessMapping);
  }

  @Test
  @Owner(developers = SAHILDEEP)
  @Category(UnitTests.class)
  public void testFindByAccountId() {
    final List<BusinessMapping> businessMappings = Collections.singletonList(businessMapping);
    when(hPersistence.createQuery(BusinessMapping.class)).thenReturn(query);
    when(query.filter(BusinessMappingKeys.accountId, BusinessMappingHelper.TEST_ACCOUNT_ID)).thenReturn(query);
    when(query.asList()).thenReturn(businessMappings);
    final List<BusinessMapping> response = businessMappingDao.findByAccountId(BusinessMappingHelper.TEST_ACCOUNT_ID);
    verify(hPersistence).createQuery(BusinessMapping.class);
    verify(query).filter(BusinessMappingKeys.accountId, BusinessMappingHelper.TEST_ACCOUNT_ID);
    verify(query).asList();
    assertThat(response).isEqualTo(businessMappings);
  }

  @Test
  @Owner(developers = SAHILDEEP)
  @Category(UnitTests.class)
  public void testGetByUUID() {
    when(hPersistence.createQuery(BusinessMapping.class, excludeValidate)).thenReturn(query);
    when(query.filter(BusinessMappingKeys.uuid, BusinessMappingHelper.TEST_ID)).thenReturn(query);
    when(query.get()).thenReturn(businessMapping);
    final BusinessMapping response = businessMappingDao.get(BusinessMappingHelper.TEST_ID);
    verify(hPersistence).createQuery(BusinessMapping.class, excludeValidate);
    verify(query).filter(BusinessMappingKeys.uuid, BusinessMappingHelper.TEST_ID);
    verify(query).get();
    assertThat(response).isEqualTo(businessMapping);
  }

  @Test
  @Owner(developers = SAHILDEEP)
  @Category(UnitTests.class)
  public void testGetByUUIDAndAccountId() {
    when(hPersistence.createQuery(BusinessMapping.class, excludeValidate)).thenReturn(query);
    when(query.filter(BusinessMappingKeys.uuid, BusinessMappingHelper.TEST_ID)).thenReturn(query);
    when(query.filter(BusinessMappingKeys.accountId, BusinessMappingHelper.TEST_ACCOUNT_ID)).thenReturn(query);
    when(query.get()).thenReturn(businessMapping);
    final BusinessMapping response =
        businessMappingDao.get(BusinessMappingHelper.TEST_ID, BusinessMappingHelper.TEST_ACCOUNT_ID);
    verify(hPersistence).createQuery(BusinessMapping.class, excludeValidate);
    verify(query).filter(BusinessMappingKeys.uuid, BusinessMappingHelper.TEST_ID);
    verify(query).filter(BusinessMappingKeys.accountId, BusinessMappingHelper.TEST_ACCOUNT_ID);
    verify(query).get();
    assertThat(response).isEqualTo(businessMapping);
  }

  @Test
  @Owner(developers = SAHILDEEP)
  @Category(UnitTests.class)
  public void testUpdate() {
    when(hPersistence.createQuery(BusinessMapping.class)).thenReturn(query);
    when(hPersistence.createUpdateOperations(BusinessMapping.class)).thenReturn(updateOperations);
    when(query.field(BusinessMappingKeys.accountId)).thenReturn(fieldEnd);
    when(fieldEnd.equal(BusinessMappingHelper.TEST_ACCOUNT_ID)).thenReturn(query);
    when(query.field(BusinessMappingKeys.uuid)).thenReturn(fieldEnd);
    when(fieldEnd.equal(BusinessMappingHelper.TEST_ID)).thenReturn(query);
    final BusinessMapping response = businessMappingDao.update(businessMapping);
    verify(hPersistence).createQuery(BusinessMapping.class);
    verify(hPersistence).createUpdateOperations(BusinessMapping.class);
    verify(hPersistence).update(query, updateOperations);
    assertThat(response).isEqualTo(businessMapping);
  }

  @Test
  @Owner(developers = SAHILDEEP)
  @Category(UnitTests.class)
  public void testDelete() {
    when(hPersistence.createQuery(BusinessMapping.class)).thenReturn(query);
    when(query.field(BusinessMappingKeys.accountId)).thenReturn(fieldEnd);
    when(fieldEnd.equal(BusinessMappingHelper.TEST_ACCOUNT_ID)).thenReturn(query);
    when(query.field(BusinessMappingKeys.uuid)).thenReturn(fieldEnd);
    when(fieldEnd.equal(BusinessMappingHelper.TEST_ID)).thenReturn(query);
    when(hPersistence.delete(query)).thenReturn(true);
    final boolean response =
        businessMappingDao.delete(BusinessMappingHelper.TEST_ID, BusinessMappingHelper.TEST_ACCOUNT_ID);
    verify(hPersistence).createQuery(BusinessMapping.class);
    verify(hPersistence).delete(query);
    assertThat(response).isTrue();
  }
}
