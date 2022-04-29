/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl;

import static io.harness.annotations.dev.HarnessModule._955_ACCOUNT_MGMT;
import static io.harness.rule.OwnerRule.RAJ;
import static io.harness.rule.OwnerRule.XIN;

import static junit.framework.TestCase.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.authenticationservice.beans.AuthenticationInfo;
import io.harness.cache.HarnessCacheManager;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.DelegateConfiguration;
import io.harness.ff.FeatureFlagService;
import io.harness.ng.core.account.AuthenticationMechanism;
import io.harness.ng.core.account.OauthProviderType;
import io.harness.persistence.HQuery;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.beans.Account;
import software.wings.beans.Account.AccountKeys;
import software.wings.beans.sso.OauthSettings;
import software.wings.dl.GenericDbCache;
import software.wings.dl.WingsPersistence;
import software.wings.licensing.LicenseService;
import software.wings.security.saml.SSORequest;
import software.wings.security.saml.SamlClientService;
import software.wings.service.intfc.AuthService;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mongodb.morphia.query.Query;

@OwnedBy(HarnessTeam.DEL)
@TargetModule(_955_ACCOUNT_MGMT)
public class AccountServiceImplTest extends WingsBaseTest {
  @Mock WingsPersistence wingsPersistence;
  @Mock protected AuthService authService;
  @Mock protected HarnessCacheManager harnessCacheManager;
  @Mock private LicenseService licenseService;
  @Mock private FeatureFlagService featureFlagService;
  @Mock private GenericDbCache dbCache;
  @Mock private SSOSettingServiceImpl ssoSettingService;
  @Mock private SamlClientService samlClientService;

  @Mock Query<Account> accountQuery1;
  @Mock Query<Account> accountQuery2;

  @InjectMocks AccountServiceImpl accountService;

  private static final String INDIVIDUAL_VERSION = "individualVersion";
  private static final String INDIVIDUAL_ACCOUNT = "individualAccount";
  private static final String GLOBAL_ACCOUNT = "__GLOBAL_ACCOUNT_ID__";
  private static final String GLOBAL_VERSION = "globalVersion";
  private final String ACCOUNT_ID = "accountId";

  @Before
  public void setup() throws Exception {
    initMocks(this);
  }

  @Test
  @Owner(developers = XIN)
  @Category(UnitTests.class)
  public void getDelegateConfigurationFromGlobalAccount() {
    when(licenseService.isAccountDeleted(any())).thenReturn(false);
    when(featureFlagService.isEnabled(any(), any())).thenReturn(false);

    Account account = new Account();
    account.setUuid(INDIVIDUAL_ACCOUNT);

    Account globalAccount = new Account();
    globalAccount.setUuid(GLOBAL_ACCOUNT);

    String globalVersion = GLOBAL_VERSION;
    List<String> globalDelegateVersions = new ArrayList<>();
    globalDelegateVersions.add(globalVersion);
    DelegateConfiguration globalDelegateConfiguration =
        DelegateConfiguration.builder().delegateVersions(globalDelegateVersions).build();

    globalAccount.setDelegateConfiguration(globalDelegateConfiguration);

    when(wingsPersistence.createQuery(Account.class, EnumSet.of(HQuery.QueryChecks.VALIDATE)))
        .thenReturn(accountQuery1)
        .thenReturn(accountQuery2);
    when(accountQuery1.filter(AccountKeys.uuid, INDIVIDUAL_ACCOUNT)).thenReturn(accountQuery1);
    when(accountQuery1.project("delegateConfiguration", true)).thenReturn(accountQuery1);
    when(accountQuery1.get()).thenReturn(account);

    when(accountQuery2.filter(AccountKeys.uuid, GLOBAL_ACCOUNT)).thenReturn(accountQuery2);
    when(accountQuery2.project("delegateConfiguration", true)).thenReturn(accountQuery2);
    when(accountQuery2.get()).thenReturn(globalAccount);

    DelegateConfiguration resultConfiguration = accountService.getDelegateConfiguration(INDIVIDUAL_ACCOUNT);
    assertEquals(resultConfiguration, globalDelegateConfiguration);
  }

  @Test
  @Owner(developers = XIN)
  @Category(UnitTests.class)
  public void getDelegateConfigurationFromIndividualAccount() {
    when(licenseService.isAccountDeleted(any())).thenReturn(false);
    when(featureFlagService.isEnabled(any(), any())).thenReturn(false);

    Account account = new Account();
    account.setUuid(INDIVIDUAL_ACCOUNT);

    String version = INDIVIDUAL_VERSION;
    List<String> delegateVersions = new ArrayList<>();
    delegateVersions.add(version);
    DelegateConfiguration individualConfiguration =
        DelegateConfiguration.builder().delegateVersions(delegateVersions).build();
    account.setDelegateConfiguration(individualConfiguration);

    Account globalAccount = new Account();
    globalAccount.setUuid(GLOBAL_ACCOUNT);

    String globalVersion = GLOBAL_VERSION;
    List<String> globalDelegateVersions = new ArrayList<>();
    globalDelegateVersions.add(globalVersion);
    DelegateConfiguration globalConfiguration =
        DelegateConfiguration.builder().delegateVersions(globalDelegateVersions).build();
    globalAccount.setDelegateConfiguration(globalConfiguration);

    when(wingsPersistence.createQuery(Account.class, EnumSet.of(HQuery.QueryChecks.VALIDATE)))
        .thenReturn(accountQuery1)
        .thenReturn(accountQuery2);
    when(accountQuery1.filter(AccountKeys.uuid, INDIVIDUAL_ACCOUNT)).thenReturn(accountQuery1);
    when(accountQuery1.project("delegateConfiguration", true)).thenReturn(accountQuery1);
    when(accountQuery1.get()).thenReturn(account);

    when(accountQuery2.filter(AccountKeys.uuid, GLOBAL_ACCOUNT)).thenReturn(accountQuery2);
    when(accountQuery2.project("delegateConfiguration", true)).thenReturn(accountQuery2);
    when(accountQuery2.get()).thenReturn(globalAccount);

    DelegateConfiguration resultConfiguration = accountService.getDelegateConfiguration(INDIVIDUAL_ACCOUNT);

    assertEquals(resultConfiguration, individualConfiguration);
  }

  @Test
  @Owner(developers = RAJ)
  @Category(UnitTests.class)
  public void getAuthenticationInfo_UserPassword() {
    buildAccountWithAuthMechanism(AuthenticationMechanism.USER_PASSWORD, false);
    assertEquals(AuthenticationInfo.builder()
                     .accountId(ACCOUNT_ID)
                     .authenticationMechanism(AuthenticationMechanism.USER_PASSWORD)
                     .oauthEnabled(false)
                     .build(),
        accountService.getAuthenticationInfo(ACCOUNT_ID));
  }

  @Test
  @Owner(developers = RAJ)
  @Category(UnitTests.class)
  public void getAuthenticationInfo_UserPasswordAndOauth() {
    buildAccountWithAuthMechanism(AuthenticationMechanism.USER_PASSWORD, true);
    when(ssoSettingService.getOauthSettingsByAccountId(ACCOUNT_ID))
        .thenReturn(OauthSettings.builder()
                        .accountId(ACCOUNT_ID)
                        .allowedProviders(ImmutableSet.of(OauthProviderType.GOOGLE))
                        .displayName("oauth")
                        .build());
    assertEquals(AuthenticationInfo.builder()
                     .accountId(ACCOUNT_ID)
                     .authenticationMechanism(AuthenticationMechanism.USER_PASSWORD)
                     .oauthEnabled(true)
                     .oauthProviders(ImmutableList.of(OauthProviderType.GOOGLE))
                     .build(),
        accountService.getAuthenticationInfo(ACCOUNT_ID));
  }

  @Test
  @Owner(developers = RAJ)
  @Category(UnitTests.class)
  public void getAuthenticationInfo_OauthOnly() {
    buildAccountWithAuthMechanism(AuthenticationMechanism.OAUTH, true);
    when(ssoSettingService.getOauthSettingsByAccountId(ACCOUNT_ID))
        .thenReturn(OauthSettings.builder()
                        .accountId(ACCOUNT_ID)
                        .allowedProviders(ImmutableSet.of(OauthProviderType.GOOGLE))
                        .displayName("oauth")
                        .build());
    assertEquals(AuthenticationInfo.builder()
                     .accountId(ACCOUNT_ID)
                     .authenticationMechanism(AuthenticationMechanism.OAUTH)
                     .oauthEnabled(true)
                     .oauthProviders(ImmutableList.of(OauthProviderType.GOOGLE))
                     .build(),
        accountService.getAuthenticationInfo(ACCOUNT_ID));
  }

  @Test
  @Owner(developers = RAJ)
  @Category(UnitTests.class)
  public void getAuthenticationInfo_LDAP() {
    buildAccountWithAuthMechanism(AuthenticationMechanism.LDAP, false);
    assertEquals(AuthenticationInfo.builder()
                     .accountId(ACCOUNT_ID)
                     .authenticationMechanism(AuthenticationMechanism.LDAP)
                     .oauthEnabled(false)
                     .build(),
        accountService.getAuthenticationInfo(ACCOUNT_ID));
  }

  @Test
  @Owner(developers = RAJ)
  @Category(UnitTests.class)
  public void getAuthenticationInfo_SAML() {
    Account account = buildAccountWithAuthMechanism(AuthenticationMechanism.SAML, false);
    when(samlClientService.generateSamlRequestFromAccount(account, false))
        .thenReturn(SSORequest.builder().idpRedirectUrl("testredirecturl").build());
    assertEquals(AuthenticationInfo.builder()
                     .accountId(ACCOUNT_ID)
                     .authenticationMechanism(AuthenticationMechanism.SAML)
                     .samlRedirectUrl("testredirecturl")
                     .build(),
        accountService.getAuthenticationInfo(ACCOUNT_ID));
  }

  private Account buildAccountWithAuthMechanism(
      AuthenticationMechanism authenticationMechanism, boolean isOauthEnabled) {
    Account account = new Account();
    account.setUuid(ACCOUNT_ID);
    account.setAuthenticationMechanism(authenticationMechanism);
    account.setOauthEnabled(isOauthEnabled);
    account.setCompanyName("test");
    account.setAccountName("testaccount");
    account.setAppId("testappid");
    wingsPersistence.save(account);
    when(dbCache.get(Account.class, ACCOUNT_ID)).thenReturn(account);
    return account;
  }
}
