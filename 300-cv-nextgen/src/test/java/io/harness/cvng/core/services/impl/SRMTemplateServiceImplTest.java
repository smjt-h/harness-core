/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.core.services.impl;

import static io.harness.rule.OwnerRule.ABHIJITH;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.harness.CvNextGenTestBase;
import io.harness.category.element.UnitTests;
import io.harness.cvng.BuilderFactory;
import io.harness.cvng.core.beans.Scope;
import io.harness.cvng.core.beans.params.ProjectParams;
import io.harness.cvng.core.beans.template.SRMTemplateDTO;
import io.harness.cvng.core.services.api.SRMTemplateService;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class SRMTemplateServiceImplTest extends CvNextGenTestBase {
  @Inject SRMTemplateService srmTemplateService;
  BuilderFactory builderFactory;

  @Before
  public void setUp() throws Exception {
    builderFactory = BuilderFactory.getDefault();
  }

  @Test
  @Owner(developers = ABHIJITH)
  @Category(UnitTests.class)
  public void testList() {
    srmTemplateService.create(
        builderFactory.getProjectParams(), builderFactory.srmTemplateDTOBuilder().identifier("id1").build());
    srmTemplateService.create(builderFactory.getProjectParams(),
        builderFactory.srmTemplateDTOBuilder().identifier("id2").scope(Scope.ORG).build());
    srmTemplateService.create(builderFactory.getProjectParams(),
        builderFactory.srmTemplateDTOBuilder().identifier("id3").scope(Scope.ACCOUNT).build());
    srmTemplateService.create(
        ProjectParams.builder().accountIdentifier("a1").orgIdentifier("o1").projectIdentifier("p1").build(),
        builderFactory.srmTemplateDTOBuilder().identifier("id3").build());

    PageResponse<SRMTemplateDTO> result = srmTemplateService.list(
        builderFactory.getProjectParams(), null, null, PageRequest.builder().pageSize(3).pageIndex(0).build());
    assertThat(result.getTotalItems()).isEqualTo(3);
    assertThat(result.getTotalPages()).isEqualTo(1);
    assertThat(result.getContent().stream().map(template -> template.getIdentifier()).collect(Collectors.toList()))
        .contains("id1", "id2", "id3");
  }

  @Test
  @Owner(developers = ABHIJITH)
  @Category(UnitTests.class)
  public void testList_withMultiPages() {
    srmTemplateService.create(
        builderFactory.getProjectParams(), builderFactory.srmTemplateDTOBuilder().identifier("id1").build());
    srmTemplateService.create(builderFactory.getProjectParams(),
        builderFactory.srmTemplateDTOBuilder().identifier("id2").scope(Scope.ORG).build());
    srmTemplateService.create(builderFactory.getProjectParams(),
        builderFactory.srmTemplateDTOBuilder().identifier("id3").scope(Scope.ACCOUNT).build());
    srmTemplateService.create(
        ProjectParams.builder().accountIdentifier("a1").orgIdentifier("o1").projectIdentifier("p1").build(),
        builderFactory.srmTemplateDTOBuilder().identifier("id3").build());

    PageResponse<SRMTemplateDTO> firstPage = srmTemplateService.list(
        builderFactory.getProjectParams(), null, null, PageRequest.builder().pageSize(2).pageIndex(0).build());
    assertThat(firstPage.getTotalItems()).isEqualTo(3);
    assertThat(firstPage.getPageSize()).isEqualTo(2);
    assertThat(firstPage.getTotalPages()).isEqualTo(2);
    assertThat(firstPage.getContent().stream().map(template -> template.getIdentifier()).collect(Collectors.toList()))
        .contains("id3", "id2");

    PageResponse<SRMTemplateDTO> secondPage = srmTemplateService.list(
        builderFactory.getProjectParams(), null, null, PageRequest.builder().pageSize(2).pageIndex(1).build());
    assertThat(secondPage.getTotalItems()).isEqualTo(3);
    assertThat(secondPage.getPageSize()).isEqualTo(2);
    assertThat(secondPage.getPageItemCount()).isEqualTo(1);
    assertThat(secondPage.getTotalPages()).isEqualTo(2);
    assertThat(secondPage.getContent().stream().map(template -> template.getIdentifier()).collect(Collectors.toList()))
        .contains("id1");
  }

  @Test
  @Owner(developers = ABHIJITH)
  @Category(UnitTests.class)
  public void testList_withSearchText() {
    srmTemplateService.create(builderFactory.getProjectParams(),
        builderFactory.srmTemplateDTOBuilder().identifier("id1").name("abcdefg").build());
    srmTemplateService.create(builderFactory.getProjectParams(),
        builderFactory.srmTemplateDTOBuilder().identifier("id2").name("zxcvb").scope(Scope.ORG).build());
    srmTemplateService.create(builderFactory.getProjectParams(),
        builderFactory.srmTemplateDTOBuilder().identifier("id3").name("abcosod").scope(Scope.ACCOUNT).build());

    PageResponse<SRMTemplateDTO> result = srmTemplateService.list(
        builderFactory.getProjectParams(), null, "abc", PageRequest.builder().pageSize(3).pageIndex(0).build());
    assertThat(result.getTotalItems()).isEqualTo(2);
    assertThat(result.getTotalPages()).isEqualTo(1);
    assertThat(result.getContent().stream().map(template -> template.getIdentifier()).collect(Collectors.toList()))
        .contains("id1", "id3");
  }

  @Test
  @Owner(developers = ABHIJITH)
  @Category(UnitTests.class)
  public void testGet() {
    SRMTemplateDTO srmTemplateDTO = builderFactory.srmTemplateDTOBuilder().build();
    srmTemplateService.create(builderFactory.getProjectParams(), srmTemplateDTO);
    SRMTemplateDTO result = srmTemplateService.get(
        builderFactory.getProjectParams().getAccountIdentifier(), srmTemplateDTO.getFullyQualifiedIdentifier());
    assertThat(result).isEqualTo(srmTemplateDTO);
  }

  @Test
  @Owner(developers = ABHIJITH)
  @Category(UnitTests.class)
  public void testCreate_projectScope() {
    SRMTemplateDTO srmTemplateDTO = builderFactory.srmTemplateDTOBuilder().build();
    SRMTemplateDTO result = srmTemplateService.create(builderFactory.getProjectParams(), srmTemplateDTO);
    assertThat(result).isEqualTo(srmTemplateDTO);
  }

  @Test
  @Owner(developers = ABHIJITH)
  @Category(UnitTests.class)
  public void testCreate_orgScope() {
    SRMTemplateDTO srmTemplateDTO = builderFactory.srmTemplateDTOBuilder().scope(Scope.ORG).build();
    SRMTemplateDTO result = srmTemplateService.create(builderFactory.getProjectParams(), srmTemplateDTO);
    assertThat(result.getProjectIdentifier()).isNull();
    assertThat(result.getFullyQualifiedIdentifier())
        .isEqualTo(StringUtils.joinWith("/", builderFactory.getProjectParams().getAccountIdentifier(),
            builderFactory.getProjectParams().getOrgIdentifier(), srmTemplateDTO.getIdentifier()));
  }

  @Test
  @Owner(developers = ABHIJITH)
  @Category(UnitTests.class)
  public void testCreate_accountScope() {
    SRMTemplateDTO srmTemplateDTO = builderFactory.srmTemplateDTOBuilder().scope(Scope.ACCOUNT).build();
    SRMTemplateDTO result = srmTemplateService.create(builderFactory.getProjectParams(), srmTemplateDTO);
    assertThat(result.getProjectIdentifier()).isNull();
    assertThat(result.getOrgIdentifier()).isNull();
    assertThat(result.getFullyQualifiedIdentifier())
        .isEqualTo(StringUtils.joinWith(
            "/", builderFactory.getProjectParams().getAccountIdentifier(), srmTemplateDTO.getIdentifier()));
  }

  @Test
  @Owner(developers = ABHIJITH)
  @Category(UnitTests.class)
  public void testUpdate() {
    SRMTemplateDTO srmTemplateDTO = builderFactory.srmTemplateDTOBuilder().build();
    SRMTemplateDTO result = srmTemplateService.create(builderFactory.getProjectParams(), srmTemplateDTO);

    SRMTemplateDTO updatingTemplate = builderFactory.srmTemplateDTOBuilder()
                                          .name("newName")
                                          .yamlTemplate("newYamlTemplate")
                                          .version("newVersion")
                                          .build();
    srmTemplateService.update(
        builderFactory.getProjectParams(), srmTemplateDTO.getFullyQualifiedIdentifier(), updatingTemplate);
    SRMTemplateDTO templateFromDb = srmTemplateService.get(
        builderFactory.getProjectParams().getAccountIdentifier(), srmTemplateDTO.getFullyQualifiedIdentifier());
    assertThat(templateFromDb).isEqualTo(updatingTemplate);
  }

  @Test
  @Owner(developers = ABHIJITH)
  @Category(UnitTests.class)
  public void testDelete() {
    SRMTemplateDTO srmTemplateDTO = builderFactory.srmTemplateDTOBuilder().build();
    SRMTemplateDTO result = srmTemplateService.create(builderFactory.getProjectParams(), srmTemplateDTO);
    SRMTemplateDTO templateFromDb = srmTemplateService.get(
        builderFactory.getProjectParams().getAccountIdentifier(), srmTemplateDTO.getFullyQualifiedIdentifier());
    assertThat(templateFromDb).isNotNull();
    srmTemplateService.delete(
        builderFactory.getProjectParams().getAccountIdentifier(), srmTemplateDTO.getFullyQualifiedIdentifier());
    assertThatThrownBy(()
                           -> srmTemplateService.get(builderFactory.getProjectParams().getAccountIdentifier(),
                               srmTemplateDTO.getFullyQualifiedIdentifier()))
        .hasMessageContaining(
            "Unable to find a template with identifier : " + srmTemplateDTO.getFullyQualifiedIdentifier());
  }
}
