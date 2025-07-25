/*
 * Copyright (c) 2021-2024, WSO2 LLC. (http://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.application.mgt;

import org.apache.commons.lang.StringUtils;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.wso2.carbon.CarbonConstants;
import org.wso2.carbon.base.CarbonBaseConstants;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.context.RegistryType;
import org.wso2.carbon.context.internal.OSGiDataHolder;
import org.wso2.carbon.core.internal.CarbonCoreDataHolder;
import org.wso2.carbon.identity.application.common.ApplicationAuthenticatorService;
import org.wso2.carbon.identity.application.common.IdentityApplicationManagementClientException;
import org.wso2.carbon.identity.application.common.IdentityApplicationManagementException;
import org.wso2.carbon.identity.application.common.IdentityApplicationManagementServerException;
import org.wso2.carbon.identity.application.common.model.ApplicationBasicInfo;
import org.wso2.carbon.identity.application.common.model.AuthenticationStep;
import org.wso2.carbon.identity.application.common.model.Claim;
import org.wso2.carbon.identity.application.common.model.ClaimConfig;
import org.wso2.carbon.identity.application.common.model.ClaimMapping;
import org.wso2.carbon.identity.application.common.model.ClientAttestationMetaData;
import org.wso2.carbon.identity.application.common.model.FederatedAuthenticatorConfig;
import org.wso2.carbon.identity.application.common.model.GroupBasicInfo;
import org.wso2.carbon.identity.application.common.model.IdentityProvider;
import org.wso2.carbon.identity.application.common.model.InboundAuthenticationConfig;
import org.wso2.carbon.identity.application.common.model.InboundAuthenticationRequestConfig;
import org.wso2.carbon.identity.application.common.model.InboundProvisioningConfig;
import org.wso2.carbon.identity.application.common.model.LocalAndOutboundAuthenticationConfig;
import org.wso2.carbon.identity.application.common.model.LocalAuthenticatorConfig;
import org.wso2.carbon.identity.application.common.model.LocalRole;
import org.wso2.carbon.identity.application.common.model.OutboundProvisioningConfig;
import org.wso2.carbon.identity.application.common.model.PermissionsAndRoleConfig;
import org.wso2.carbon.identity.application.common.model.ProvisioningConnectorConfig;
import org.wso2.carbon.identity.application.common.model.RequestPathAuthenticatorConfig;
import org.wso2.carbon.identity.application.common.model.RoleMapping;
import org.wso2.carbon.identity.application.common.model.ServiceProvider;
import org.wso2.carbon.identity.application.common.model.ServiceProviderProperty;
import org.wso2.carbon.identity.application.common.model.SpTrustedAppMetadata;
import org.wso2.carbon.identity.application.common.model.TrustedApp;
import org.wso2.carbon.identity.application.common.util.IdentityApplicationConstants.PlatformType;
import org.wso2.carbon.identity.application.mgt.inbound.dto.ApplicationDTO;
import org.wso2.carbon.identity.application.mgt.inbound.dto.InboundProtocolConfigurationDTO;
import org.wso2.carbon.identity.application.mgt.inbound.dto.InboundProtocolsDTO;
import org.wso2.carbon.identity.application.mgt.inbound.protocol.ApplicationInboundAuthConfigHandler;
import org.wso2.carbon.identity.application.mgt.internal.ApplicationManagementServiceComponentHolder;
import org.wso2.carbon.identity.application.mgt.provider.ApplicationPermissionProvider;
import org.wso2.carbon.identity.application.mgt.provider.RegistryBasedApplicationPermissionProvider;
import org.wso2.carbon.identity.base.AuthenticatorPropertyConstants.DefinedByType;
import org.wso2.carbon.identity.certificate.management.exception.CertificateMgtClientException;
import org.wso2.carbon.identity.certificate.management.exception.CertificateMgtException;
import org.wso2.carbon.identity.certificate.management.exception.CertificateMgtServerException;
import org.wso2.carbon.identity.certificate.management.model.Certificate;
import org.wso2.carbon.identity.certificate.management.service.ApplicationCertificateManagementService;
import org.wso2.carbon.identity.common.testng.WithH2Database;
import org.wso2.carbon.identity.common.testng.realm.InMemoryRealmService;
import org.wso2.carbon.identity.common.testng.realm.MockUserStoreManager;
import org.wso2.carbon.identity.core.internal.IdentityCoreServiceDataHolder;
import org.wso2.carbon.identity.core.model.ExpressionNode;
import org.wso2.carbon.identity.core.model.Node;
import org.wso2.carbon.identity.core.model.OperationNode;
import org.wso2.carbon.identity.core.util.IdentityDatabaseUtil;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementServerException;
import org.wso2.carbon.identity.organization.management.service.util.OrganizationManagementUtil;
import org.wso2.carbon.identity.secret.mgt.core.SecretManager;
import org.wso2.carbon.identity.secret.mgt.core.SecretManagerImpl;
import org.wso2.carbon.identity.secret.mgt.core.SecretResolveManager;
import org.wso2.carbon.identity.secret.mgt.core.SecretResolveManagerImpl;
import org.wso2.carbon.identity.secret.mgt.core.dao.SecretDAO;
import org.wso2.carbon.identity.secret.mgt.core.dao.impl.SecretDAOImpl;
import org.wso2.carbon.identity.secret.mgt.core.exception.SecretManagementException;
import org.wso2.carbon.identity.secret.mgt.core.internal.SecretManagerComponentDataHolder;
import org.wso2.carbon.identity.secret.mgt.core.model.ResolvedSecret;
import org.wso2.carbon.identity.secret.mgt.core.model.Secret;
import org.wso2.carbon.idp.mgt.IdentityProviderManagementException;
import org.wso2.carbon.idp.mgt.dao.IdPManagementDAO;
import org.wso2.carbon.registry.core.Collection;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.internal.RegistryDataHolder;
import org.wso2.carbon.registry.core.service.RegistryService;
import org.wso2.carbon.registry.core.session.UserRegistry;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.api.UserStoreManager;
import org.wso2.carbon.user.core.UserRealm;
import org.wso2.carbon.user.core.common.AbstractUserStoreManager;
import org.wso2.carbon.user.core.common.Group;
import org.wso2.carbon.user.core.model.Condition;
import org.wso2.carbon.user.core.model.ExpressionCondition;
import org.wso2.carbon.user.core.service.RealmService;

import java.lang.reflect.Field;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.wso2.carbon.CarbonConstants.REGISTRY_SYSTEM_USERNAME;
import static org.wso2.carbon.identity.application.common.util.IdentityApplicationConstants.TEMPLATE_ID_SP_PROPERTY_NAME;
import static org.wso2.carbon.identity.application.common.util.IdentityApplicationConstants.TEMPLATE_VERSION_SP_PROPERTY_NAME;
import static org.wso2.carbon.identity.application.mgt.ApplicationConstants.ErrorMessage.ERROR_RETRIEVING_GROUP_LIST;
import static org.wso2.carbon.identity.application.mgt.ApplicationConstants.ErrorMessage.INVALID_GROUP_FILTER;
import static org.wso2.carbon.identity.application.mgt.ApplicationConstants.ErrorMessage.INVALID_USER_STORE_DOMAIN;
import static org.wso2.carbon.identity.application.mgt.ApplicationConstants.FILTER_CO;
import static org.wso2.carbon.identity.application.mgt.ApplicationConstants.IS_FRAGMENT_APP;
import static org.wso2.carbon.identity.application.mgt.ApplicationConstants.NAME;
import static org.wso2.carbon.identity.application.mgt.ApplicationConstants.PORTAL_NAMES_CONFIG_ELEMENT;
import static org.wso2.carbon.identity.application.mgt.ApplicationConstants.TRUSTED_APP_CONSENT_REQUIRED_PROPERTY;
import static org.wso2.carbon.identity.certificate.management.constant.CertificateMgtErrors.ERROR_INVALID_CERTIFICATE_CONTENT;
import static org.wso2.carbon.user.core.UserStoreConfigConstants.GROUP_NAME_ATTRIBUTE;
import static org.wso2.carbon.utils.multitenancy.MultitenantConstants.SUPER_TENANT_DOMAIN_NAME;
import static org.wso2.carbon.utils.multitenancy.MultitenantConstants.SUPER_TENANT_ID;

/*
  Unit tests for ApplicationManagementServiceImpl.
 */
@Test
@WithH2Database(jndiName = "jdbc/WSO2IdentityDB", files = {"dbscripts/identity.sql"})
public class ApplicationManagementServiceImplTest {

    private static final String SAMPLE_TENANT_DOMAIN = "tenant domain";
    private static final String APPLICATION_NAME_1 = "Test application1";
    private static final String APPLICATION_NAME_2 = "Test application2";
    private static final String APPLICATION_NAME_3 = "Test application3";
    private static final String APPLICATION_TEMPLATE_ID_1 = "Test_template_1";
    private static final String APPLICATION_TEMPLATE_ID_2 = "Test_template_2";
    private static final String APPLICATION_TEMPLATE_VERSION_1 = "v1.0.0";
    private static final String APPLICATION_TEMPLATE_VERSION_2 = "v1.0.1";
    private static final String APPLICATION_INBOUND_AUTH_KEY_1 = "Test_auth_key1";
    private static final String APPLICATION_INBOUND_AUTH_KEY_2 = "Test_auth_key2";
    private static final String APPLICATION_INBOUND_AUTH_KEY_OAUTH2 = "auth key";
    private static final String APPLICATION_NAME_FILTER_1 = "name ew application1";
    private static final String APPLICATION_NAME_FILTER_2 = "name co 2";
    private static final String APPLICATION_NAME_FILTER_3 = "name ew application3";
    private static final String APPLICATION_CLIENT_ID_FILTER = "clientId co %s";
    private static final String APPLICATION_ISSUER_FILTER = "issuer co %s";
    private static final String APPLICATION_NAME_OR_CLIENT_ID_FILTER = "name co sampleAppName or clientId eq %s";
    private static final String APPLICATION_NAME_AND_CLIENT_ID_FILTER = "name co application1 and clientId eq %s";
    private static final String APPLICATION_NAME_AND_CLIENT_ID_OR_ISSUER_FILTER =
            "name co application1 and clientId eq %s or issuer eq %s";
    private static final String APPLICATION_ISSUER_OR_ISSUER_FILTER = "issuer eq %s or issuer co %s";
    private static final String APPLICATION_NAME_OR_ISSUER_FILTER = "name co sampleAppName or issuer eq %s";
    private static final String APPLICATION_NAME_AND_ISSUER_FILTER = "name co application1 and issuer eq %s";
    private static final String IDP_NAME_1 = "Test IdP 1";
    private static final String IDP_NAME_2 = "Test IdP 2";
    private static final String USERNAME_1 = "user 1";
    private static final String USERNAME_2 = "user 2";
    private static final String RANDOM_STRING = "random string";
    private static final String ANDROID_PACKAGE_NAME_1 = "com.wso2.sample.mobile.application";
    private static final String ANDROID_PACKAGE_NAME_2 = "com.wso2.sample.mobile.application2";
    private static final String APPLE_APP_ID = "APPLETEAMID.com.wso2.mobile.sample";
    private static final String CERTIFICATE = "dummy_application_certificate";
    private static final String UPDATED_CERTIFICATE = "updated_dummy_application_certificate";
    private static final int CERTIFICATE_ID = 1;

    // B2B organization and application related constants.
    private static final String B2B_APPLICATION_NAME = "B2B Test application";
    private static final String ROOT_ORG_ID = "10084a8d-113f-4211-a0d5-efe36b082211";
    private static final String ROOT_TENANT_DOMAIN = "carbon.super";
    private static final int ROOT_TENANT_ID = -1234;
    private static final String L1_ORG_ID = "93d996f9-a5ba-4275-a52b-adaad9eba869";
    private static final int L1_TENANT_ID = 1;
    private static final String L2_ORG_ID = "30b701c6-e309-4241-b047-0c299c45d1a0";
    private static final int L2_TENANT_ID = 2;
    private static final String DEFAULT_DOMAIN_NAME = "PRIMARY";

    private IdPManagementDAO idPManagementDAO;
    private ApplicationManagementServiceImpl applicationManagementService;
    private ApplicationCertificateManagementService applicationCertificateManagementService;

    private String appIdWithCert;
    private ServiceProvider appWithCert;
    private Certificate certificate;
    private CertificateMgtServerException serverException;
    private CertificateMgtClientException clientException;
    private OrganizationManager organizationManager;
    private String rootAppId;
    private String l1AppId;
    private String l2AppId;

    @BeforeClass
    public void setup() throws RegistryException, UserStoreException, SecretManagementException {

        setupConfiguration();
        applicationManagementService = ApplicationManagementServiceImpl.getInstance();

        SecretManager secretManager = mock(SecretManagerImpl.class);
        Secret secret = mock(Secret.class);
        ApplicationManagementServiceComponentHolder.getInstance().setSecretManager(secretManager);
        when(secretManager.isSecretExist(anyString(), anyString())).thenReturn(false);
        when(secretManager.addSecret(anyString(), any())).thenAnswer(
                invocation -> invocation.getArguments()[1]);
        when(secretManager.updateSecretValue(anyString(), anyString(), anyString())).thenReturn(secret);
        ResolvedSecret resolvedSecret = new ResolvedSecret();
        resolvedSecret.setResolvedSecretValue("random_secret_value");
        SecretResolveManager secretResolveManager = mock(SecretResolveManagerImpl.class);
        ApplicationManagementServiceComponentHolder.getInstance().setSecretResolveManager(secretResolveManager);
        when(secretResolveManager.getResolvedSecret(anyString(), anyString())).thenReturn(resolvedSecret);
        SecretManagerComponentDataHolder.getInstance().setSecretManagementEnabled(true);
        SecretDAO secretDAO = new SecretDAOImpl();
        SecretManagerComponentDataHolder.getInstance().setSecretDAOS(Collections.singletonList(secretDAO));
        CarbonConstants.ENABLE_LEGACY_AUTHZ_RUNTIME = false;
        MockedStatic<DefinedByType> definedByType = mockStatic(DefinedByType.class);
        definedByType.when(() -> DefinedByType.valueOf(anyString())).thenReturn(DefinedByType.SYSTEM);

        applicationCertificateManagementService = mock(ApplicationCertificateManagementService.class);
        ApplicationManagementServiceComponentHolder.getInstance()
                .setApplicationCertificateMgtService(applicationCertificateManagementService);
        certificate = new Certificate.Builder()
                .id(String.valueOf(CERTIFICATE_ID))
                .name(APPLICATION_NAME_1)
                .certificateContent(CERTIFICATE)
                .build();
        serverException = new CertificateMgtServerException("server_error_message", "server_error_description", "65010",
                new Throwable());
        clientException = new CertificateMgtClientException(ERROR_INVALID_CERTIFICATE_CONTENT.getMessage(),
                ERROR_INVALID_CERTIFICATE_CONTENT.getDescription(), ERROR_INVALID_CERTIFICATE_CONTENT.getCode());

        organizationManager = mock(OrganizationManager.class);
        ApplicationManagementServiceComponentHolder.getInstance().setOrganizationManager(organizationManager);
    }

    @DataProvider(name = "addApplicationDataProvider")
    public Object[][] addApplicationDataProvider() {

        ServiceProvider serviceProvider1 = new ServiceProvider();
        serviceProvider1.setApplicationName(APPLICATION_NAME_1);

        ServiceProvider serviceProvider2 = new ServiceProvider();
        serviceProvider2.setApplicationName(APPLICATION_NAME_2);

        return new Object[][]{
                {serviceProvider1, SUPER_TENANT_DOMAIN_NAME, USERNAME_1},
                {serviceProvider2, SAMPLE_TENANT_DOMAIN, USERNAME_2}
        };
    }

    @Test(dataProvider = "addApplicationDataProvider")
    public void testAddApplication(Object serviceProvider, String tenantDomain, String username)
            throws Exception {

        ServiceProvider inputSP = (ServiceProvider) serviceProvider;
        addApplicationConfigurations(inputSP);

        // Adding new application.
        ServiceProvider addedSP = applicationManagementService.addApplication(inputSP, tenantDomain,
                username);

        Assert.assertEquals(addedSP.getApplicationName(), inputSP.getApplicationName());
        Assert.assertEquals(addedSP.getOwner().getUserName(), inputSP.getOwner().getUserName());

        //  Retrieving added application.
        ServiceProvider retrievedSP = applicationManagementService.getApplicationExcludingFileBasedSPs
                (inputSP.getApplicationName(), tenantDomain);

        Assert.assertEquals(retrievedSP.getApplicationID(), inputSP.getApplicationID());
        Assert.assertEquals(retrievedSP.getOwner().getUserName(), inputSP.getOwner().getUserName());
        Assert.assertEquals(retrievedSP.getApplicationVersion(),
                ApplicationConstants.ApplicationVersion.LATEST_APP_VERSION);
        Assert.assertFalse(retrievedSP.isManagementApp());

        // Deleting added application.
        applicationManagementService.deleteApplication(inputSP.getApplicationName(), tenantDomain, username);
    }

    @DataProvider(name = "addApplicationNullAppNameDataProvider")
    public Object[][] addApplicationNullAppNameDataProvider() {

        ServiceProvider serviceProvider1 = new ServiceProvider();

        ServiceProvider serviceProvider2 = new ServiceProvider();
        serviceProvider2.setApplicationName("");

        ServiceProvider serviceProvider3 = new ServiceProvider();
        serviceProvider3.setApplicationName(null);

        return new Object[][]{
                {serviceProvider1, SUPER_TENANT_DOMAIN_NAME, USERNAME_1},
                {serviceProvider2, SUPER_TENANT_DOMAIN_NAME, USERNAME_1},
                {serviceProvider3, SUPER_TENANT_DOMAIN_NAME, USERNAME_1}
        };
    }

    @Test(dataProvider = "addApplicationNullAppNameDataProvider")
    public void testAddApplicationWithNullAppName(Object serviceProvider, String tenantDomain, String username) {

        Assert.assertThrows(IdentityApplicationManagementClientException.class, () -> applicationManagementService.
                addApplication((ServiceProvider) serviceProvider, tenantDomain, username));
    }

    @DataProvider(name = "addApplicationInvalidAppNameDataProvider")
    public Object[][] addApplicationInvalidAppNameDataProvider() {

        ServiceProvider serviceProvider1 = new ServiceProvider();
        serviceProvider1.setApplicationName("@#!app");

        ServiceProvider serviceProvider2 = new ServiceProvider();
        serviceProvider2.setApplicationName("1234@");

        return new Object[][]{
                {serviceProvider1, SUPER_TENANT_DOMAIN_NAME, USERNAME_1},
                {serviceProvider2, SUPER_TENANT_DOMAIN_NAME, USERNAME_1}
        };
    }

    @Test(dataProvider = "addApplicationInvalidAppNameDataProvider")
    public void testAddApplicationWithInvalidAppName(Object serviceProvider, String tenantDomain, String username) {

        Assert.assertThrows(IdentityApplicationManagementClientException.class, () -> applicationManagementService.
                addApplication((ServiceProvider) serviceProvider, tenantDomain, username));
    }

    @DataProvider(name = "addApplicationWithExistingAppNameDataProvider")
    public Object[][] addApplicationWithExistingAppNameDataProvider() {

        ServiceProvider serviceProvider = new ServiceProvider();
        serviceProvider.setApplicationName(APPLICATION_NAME_1);

        ServiceProvider newServiceProvider = new ServiceProvider();
        newServiceProvider.setApplicationName(APPLICATION_NAME_1);

        return new Object[][]{
                {serviceProvider, newServiceProvider, SUPER_TENANT_DOMAIN_NAME, USERNAME_1}
        };
    }

    @Test(dataProvider = "addApplicationWithExistingAppNameDataProvider")
    public void testAddApplicationWithExistingAppName(Object serviceProvider, Object newServiceProvider,
                                                      String tenantDomain, String username) throws
            IdentityApplicationManagementException {

        applicationManagementService.addApplication((ServiceProvider) serviceProvider, tenantDomain, username);

        try {
            Assert.assertThrows(IdentityApplicationManagementClientException.class, () -> applicationManagementService.
                    addApplication((ServiceProvider) newServiceProvider, tenantDomain, username));
        } finally {
            applicationManagementService.deleteApplication(((ServiceProvider) serviceProvider).getApplicationName(),
                    tenantDomain, username);
        }
    }

    @DataProvider(name = "getApplicationDataProvider")
    public Object[][] getApplicationDataProvider() {

        ServiceProvider serviceProvider1 = new ServiceProvider();
        serviceProvider1.setApplicationName(APPLICATION_NAME_1);

        ServiceProvider serviceProvider2 = new ServiceProvider();
        serviceProvider2.setApplicationName(APPLICATION_NAME_2);

        return new Object[][]{
                {serviceProvider1, SUPER_TENANT_DOMAIN_NAME, USERNAME_1},
                {serviceProvider2, SAMPLE_TENANT_DOMAIN, USERNAME_2}
        };
    }

    @Test(dataProvider = "getApplicationDataProvider")
    public void testGetApplicationBasicInfoWithNameFilter(Object serviceProvider, String tenantDomain, String username)
            throws IdentityApplicationManagementException {

        ServiceProvider inputSP = (ServiceProvider) serviceProvider;

        // Adding application.
        ServiceProvider addedSP = applicationManagementService.addApplication(inputSP, tenantDomain,
                username);

        // Retrieving added application info.
        ApplicationBasicInfo[] applicationBasicInfo = applicationManagementService.getApplicationBasicInfo
                (tenantDomain, username, "name eq " + inputSP.getApplicationName());
        Assert.assertEquals(applicationBasicInfo[0].getApplicationName(), inputSP.getApplicationName());
        Assert.assertEquals(applicationBasicInfo[0].getApplicationName(), addedSP.getApplicationName());
        Assert.assertEquals(applicationBasicInfo[0].getApplicationVersion(),
                ApplicationConstants.ApplicationVersion.LATEST_APP_VERSION);

        // Deleting added application.
        applicationManagementService.deleteApplication(inputSP.getApplicationName(), tenantDomain, username);
    }

    @Test(dataProvider = "getApplicationDataProvider")
    public void testGetPaginatedApplicationBasicInfo(Object serviceProvider, String tenantDomain, String username)
            throws IdentityApplicationManagementException {

        ServiceProvider inputSP = (ServiceProvider) serviceProvider;

        // Adding application.
        ServiceProvider addedSP = applicationManagementService.addApplication(inputSP, tenantDomain, username);

        // Retrieving added application info.
        ApplicationBasicInfo[] applicationBasicInfo = applicationManagementService.getPaginatedApplicationBasicInfo
                (tenantDomain, username, 1, "name co " + inputSP.getApplicationName());
        Assert.assertEquals(applicationBasicInfo[0].getApplicationName(), inputSP.getApplicationName());
        Assert.assertEquals(applicationBasicInfo[0].getApplicationName(), addedSP.getApplicationName());
        Assert.assertEquals(applicationBasicInfo[0].getApplicationVersion(),
                ApplicationConstants.ApplicationVersion.LATEST_APP_VERSION);

        // Deleting added application.
        applicationManagementService.deleteApplication(inputSP.getApplicationName(), tenantDomain, username);
    }

    private void addApplications() throws IdentityApplicationManagementException {

        ServiceProvider serviceProvider1 = new ServiceProvider();
        serviceProvider1.setApplicationName(APPLICATION_NAME_1);

        ServiceProvider serviceProvider2 = new ServiceProvider();
        serviceProvider2.setApplicationName(APPLICATION_NAME_2);

        applicationManagementService.addApplication(serviceProvider1, SUPER_TENANT_DOMAIN_NAME, USERNAME_1);
        applicationManagementService.addApplication(serviceProvider2, SUPER_TENANT_DOMAIN_NAME, USERNAME_1);
    }

    @Test
    public void testGetAllApplicationBasicInfo() throws IdentityApplicationManagementException {

        addApplications();
        ApplicationBasicInfo[] applicationBasicInfo = applicationManagementService.getAllApplicationBasicInfo
                (SUPER_TENANT_DOMAIN_NAME, USERNAME_1);

        Assert.assertEquals(applicationBasicInfo.length, 2);
        Assert.assertEquals(applicationBasicInfo[0].getApplicationName(), APPLICATION_NAME_2);
        Assert.assertEquals(applicationBasicInfo[1].getApplicationName(), APPLICATION_NAME_1);

        // Deleting all added applications.
        applicationManagementService.deleteApplications(SUPER_TENANT_ID);
    }

    @Test
    public void testGetAllPaginatedApplicationBasicInfo() throws IdentityApplicationManagementException {

        addApplications();
        ApplicationBasicInfo[] applicationBasicInfo = applicationManagementService.getAllPaginatedApplicationBasicInfo
                (SUPER_TENANT_DOMAIN_NAME, USERNAME_1, 1);

        Assert.assertEquals(applicationBasicInfo.length, 2);
        Assert.assertEquals(applicationBasicInfo[0].getApplicationName(), APPLICATION_NAME_2);
        Assert.assertEquals(applicationBasicInfo[1].getApplicationName(), APPLICATION_NAME_1);

        // Deleting all added applications.
        applicationManagementService.deleteApplications(SUPER_TENANT_ID);
    }

    @Test
    public void testGetApplicationBasicInfoOffsetLimit() throws IdentityApplicationManagementException {

        addApplications();

        ApplicationBasicInfo[] applicationBasicInfo1 = applicationManagementService.getApplicationBasicInfo
                (SUPER_TENANT_DOMAIN_NAME, USERNAME_1, 0, 1);
        Assert.assertEquals(applicationBasicInfo1[0].getApplicationName(), APPLICATION_NAME_2);

        ApplicationBasicInfo[] applicationBasicInfo2 = applicationManagementService.getApplicationBasicInfo
                (SUPER_TENANT_DOMAIN_NAME, USERNAME_1, 1, 1);
        Assert.assertEquals(applicationBasicInfo2[0].getApplicationName(), APPLICATION_NAME_1);

        // Deleting all added applications.
        applicationManagementService.deleteApplications(SUPER_TENANT_ID);
    }

    @DataProvider(name = "getOAuthApplicationDataProvider")
    public Object[][] getOAuthApplicationDataProvider() {

        return new Object[][]{
            {APPLICATION_NAME_FILTER_1, APPLICATION_NAME_1},
            {APPLICATION_NAME_FILTER_2, APPLICATION_NAME_2},
            {String.format(APPLICATION_CLIENT_ID_FILTER, APPLICATION_INBOUND_AUTH_KEY_1), APPLICATION_NAME_1},
            {String.format(APPLICATION_CLIENT_ID_FILTER, APPLICATION_INBOUND_AUTH_KEY_2), APPLICATION_NAME_2},
            {String.format(APPLICATION_NAME_OR_CLIENT_ID_FILTER, APPLICATION_INBOUND_AUTH_KEY_1), APPLICATION_NAME_1},
            {String.format(APPLICATION_NAME_OR_CLIENT_ID_FILTER, APPLICATION_INBOUND_AUTH_KEY_2), APPLICATION_NAME_2},
            {String.format(APPLICATION_NAME_AND_CLIENT_ID_FILTER, APPLICATION_INBOUND_AUTH_KEY_1), APPLICATION_NAME_1},
            {String.format(APPLICATION_NAME_AND_CLIENT_ID_OR_ISSUER_FILTER, APPLICATION_INBOUND_AUTH_KEY_1,
                    APPLICATION_INBOUND_AUTH_KEY_1), APPLICATION_NAME_1}
        };
    }

    @Test(dataProvider = "getOAuthApplicationDataProvider")
    public void testGetOAuth2ApplicationBasicInfoWithFilterOffsetLimit(String filter, String expectedResult)
            throws IdentityApplicationManagementException {

        ServiceProvider inputSP1 = new ServiceProvider();
        inputSP1.setApplicationName(APPLICATION_NAME_1);
        addApplicationConfigurations(inputSP1);
        setApplicationInboundAuthConfigs(inputSP1, APPLICATION_INBOUND_AUTH_KEY_1, "oauth2");

        ServiceProvider inputSP2 = new ServiceProvider();
        inputSP2.setApplicationName(APPLICATION_NAME_2);
        addApplicationConfigurations(inputSP2);
        setApplicationInboundAuthConfigs(inputSP2, APPLICATION_INBOUND_AUTH_KEY_2, "oauth2");

        // Adding application.
        applicationManagementService.createApplication(inputSP1, SUPER_TENANT_DOMAIN_NAME, USERNAME_1);
        applicationManagementService.createApplication(inputSP2, SUPER_TENANT_DOMAIN_NAME, USERNAME_1);

        // Test get applications with filter.
        ApplicationBasicInfo[] applicationBasicInfo = applicationManagementService.getApplicationBasicInfo
                (SUPER_TENANT_DOMAIN_NAME, USERNAME_1, filter, 0, 5);
        Assert.assertEquals(applicationBasicInfo[0].getApplicationName(), expectedResult);

        // Deleting all added applications.
        applicationManagementService.deleteApplications(SUPER_TENANT_ID);
    }

    @DataProvider(name = "getAppsExcludingSystemPortalsDataProvider")
    public Object[][] getAppsExcludingSystemPortals() {

        return new Object[][]{
                {APPLICATION_NAME_FILTER_1, 1},
                {APPLICATION_NAME_FILTER_3, 0}
        };
    }

    private void setupExcludeSystemPortalsEnv()
            throws IdentityApplicationManagementException {

        ServiceProvider serviceProvider1 = new ServiceProvider();
        serviceProvider1.setApplicationName(APPLICATION_NAME_1);

        ServiceProvider serviceProvider2 = new ServiceProvider();
        serviceProvider2.setApplicationName(APPLICATION_NAME_2);

        ServiceProvider serviceProvider3 = new ServiceProvider();
        serviceProvider3.setApplicationName(APPLICATION_NAME_3);

        applicationManagementService.addApplication(serviceProvider1, SUPER_TENANT_DOMAIN_NAME, USERNAME_1);
        applicationManagementService.addApplication(serviceProvider2, SUPER_TENANT_DOMAIN_NAME, USERNAME_1);
        applicationManagementService.addApplication(serviceProvider3, SUPER_TENANT_DOMAIN_NAME, USERNAME_1);
    }

    @Test
    public void testGetApplicationBasicInfoExcludingSystemPortals() throws IdentityApplicationManagementException {

        setupExcludeSystemPortalsEnv();
        try (MockedStatic<IdentityUtil> identityUtil = Mockito.mockStatic(IdentityUtil.class)) {
            List<String> systemApp = Arrays.asList(APPLICATION_NAME_3);
            identityUtil.when(() -> IdentityUtil.getPropertyAsList(PORTAL_NAMES_CONFIG_ELEMENT))
                    .thenReturn(systemApp);
            ApplicationBasicInfo[] applicationBasicInfo = applicationManagementService.getApplicationBasicInfo
                    (SUPER_TENANT_DOMAIN_NAME, USERNAME_1, "", 0, 10, true);
            Assert.assertEquals(applicationBasicInfo.length, 2);
        }
        // Deleting all added applications.
        applicationManagementService.deleteApplications(SUPER_TENANT_ID);
    }

    @Test(dataProvider = "getAppsExcludingSystemPortalsDataProvider")
    public void testGetApplicationBasicInfoWithFilterExcludingSystemPortals(String filter, int expectedResult)
            throws IdentityApplicationManagementException {

        setupExcludeSystemPortalsEnv();
        try (MockedStatic<IdentityUtil> identityUtil = Mockito.mockStatic(IdentityUtil.class)) {
            List<String> systemApp = Arrays.asList(APPLICATION_NAME_3);
            identityUtil.when(() -> IdentityUtil.getPropertyAsList(PORTAL_NAMES_CONFIG_ELEMENT))
                    .thenReturn(systemApp);
            ApplicationBasicInfo[] applicationBasicInfo = applicationManagementService.getApplicationBasicInfo
                    (SUPER_TENANT_DOMAIN_NAME, USERNAME_1, filter, 0, 10, true);
            Assert.assertEquals(applicationBasicInfo.length, expectedResult);
        }
        // Deleting all added applications.
        applicationManagementService.deleteApplications(SUPER_TENANT_ID);
    }

    @Test
    public void testGetCountOfApplicationsExcludingSystemPortals()
            throws IdentityApplicationManagementException {

        setupExcludeSystemPortalsEnv();
        try (MockedStatic<IdentityUtil> identityUtil = Mockito.mockStatic(IdentityUtil.class)) {
            List<String> systemApp = Arrays.asList(APPLICATION_NAME_3);
            identityUtil.when(() -> IdentityUtil.getPropertyAsList(PORTAL_NAMES_CONFIG_ELEMENT))
                    .thenReturn(systemApp);
            Assert.assertEquals(
                    applicationManagementService.getCountOfApplications(SUPER_TENANT_DOMAIN_NAME, USERNAME_1,
                            "", true), 2);
        }
        // Deleting all added applications.
        applicationManagementService.deleteApplications(SUPER_TENANT_ID);
    }

    @Test(dataProvider = "getAppsExcludingSystemPortalsDataProvider")
    public void testGetCountOfApplicationsWithFilterExcludingSystemPortals(String filter, int expectedResult)
            throws IdentityApplicationManagementException {

        setupExcludeSystemPortalsEnv();
        try (MockedStatic<IdentityUtil> identityUtil = Mockito.mockStatic(IdentityUtil.class)) {
            List<String> systemApp = Arrays.asList(APPLICATION_NAME_3);
            identityUtil.when(() -> IdentityUtil.getPropertyAsList(PORTAL_NAMES_CONFIG_ELEMENT))
                    .thenReturn(systemApp);
            Assert.assertEquals(
                    applicationManagementService.getCountOfApplications(SUPER_TENANT_DOMAIN_NAME, USERNAME_1,
                            filter, true), expectedResult);
        }
        // Deleting all added applications.
        applicationManagementService.deleteApplications(SUPER_TENANT_ID);
    }

    @DataProvider(name = "organizationDataProvider")
    public Object[][] getOrganizationDataProvider() {

        return new Object[][]{
                {false},
                {true}
        };
    }

    @Test(dataProvider = "organizationDataProvider")
    public void testCreateAndGetApplicationWithProtocolService(boolean isOrganization) throws
            IdentityApplicationManagementException {
        
        ApplicationDTO.Builder applicationDTOBuilder = new ApplicationDTO.Builder();
        ServiceProvider inputSP1 = new ServiceProvider();
        inputSP1.setApplicationName(APPLICATION_NAME_1);
        // Adding application configurations except inbound protocol configurations.
        addApplicationConfigurations(inputSP1);
        applicationDTOBuilder.serviceProvider(inputSP1);
        
        // Creating OAuth2 inbound protocol configurations.
        InboundProtocolsDTO inbounds = setInboundProtocol();
        inbounds.addProtocolConfiguration(() -> ApplicationConstants.StandardInboundProtocols.SAML2);
        applicationDTOBuilder.inboundProtocolConfigurationDto(inbounds);

        try (MockedStatic<OrganizationManagementUtil> organizationMgtUtilMockedStatic =
                     mockStatic(OrganizationManagementUtil.class)) {
            organizationMgtUtilMockedStatic.when(() -> OrganizationManagementUtil.isOrganization(anyString())).
                    thenReturn(isOrganization);
            // Mocking protocol service.
            ApplicationInboundAuthConfigHandler customSAML2InboundAuthConfigHandler =
                    customSAML2InboundAuthConfigHandler(isOrganization);
            ApplicationInboundAuthConfigHandler customOAuth2InboundAuthConfigHandler =
                    customOAuth2InboundAuthConfigHandler();
            ApplicationManagementServiceComponentHolder.getInstance().addApplicationInboundAuthConfigHandler(
                    customSAML2InboundAuthConfigHandler);
            if (isOrganization) {
                ApplicationManagementServiceComponentHolder.getInstance().addApplicationInboundAuthConfigHandler(
                        customOAuth2InboundAuthConfigHandler);
            }
            // Creating application.
            applicationManagementService.createApplication(applicationDTOBuilder.build(), SUPER_TENANT_DOMAIN_NAME,
                    USERNAME_1);
            ApplicationManagementServiceComponentHolder.getInstance().removeApplicationInboundConfigHandler(
                    customSAML2InboundAuthConfigHandler);
            ApplicationManagementServiceComponentHolder.getInstance().removeApplicationInboundConfigHandler(
                    customOAuth2InboundAuthConfigHandler);
        }
        ServiceProvider applicationByResourceId = applicationManagementService.getApplicationByResourceId(inputSP1
                .getApplicationResourceId(), SUPER_TENANT_DOMAIN_NAME);
        Assert.assertEquals(applicationByResourceId.getApplicationName(), APPLICATION_NAME_1);
        if (!isOrganization) {
            /*
             There should be 2 inbound protocol configurations. The one that already exists and the one that
             is created.
            */
            Assert.assertEquals(applicationByResourceId.getInboundAuthenticationConfig()
                    .getInboundAuthenticationRequestConfigs().length, 2);
            for (InboundAuthenticationRequestConfig inboundAuthenticationRequestConfig : applicationByResourceId
                    .getInboundAuthenticationConfig().getInboundAuthenticationRequestConfigs()) {
                /*
                 This is the existing inbound protocol configuration. Validate the existing inbound protocol
                 configuration is unchanged.
                */
                if (ApplicationConstants.StandardInboundProtocols.OAUTH2.equals(inboundAuthenticationRequestConfig
                        .getInboundAuthType())) {
                    Assert.assertEquals(inboundAuthenticationRequestConfig.getInboundAuthKey(),
                            APPLICATION_INBOUND_AUTH_KEY_OAUTH2);
                }
                /*
                 This is the newly created inbound protocol configuration. Validate the newly created inbound
                 protocol is added.
                */
                if (ApplicationConstants.StandardInboundProtocols.SAML2.equals(inboundAuthenticationRequestConfig
                        .getInboundAuthType())) {
                    Assert.assertEquals(inboundAuthenticationRequestConfig.getInboundAuthKey(),
                            APPLICATION_INBOUND_AUTH_KEY_1);
                }
            }
        } else {
            /*
             There should be 1 inbound protocol configurations since in the sub organization level, saml inbound
             protocol configurations are not allowed.
            */
            Assert.assertEquals(applicationByResourceId.getInboundAuthenticationConfig()
                    .getInboundAuthenticationRequestConfigs().length, 1);
            InboundAuthenticationRequestConfig inboundAuthenticationRequestConfig = applicationByResourceId
                    .getInboundAuthenticationConfig().getInboundAuthenticationRequestConfigs()[0];
            Assert.assertEquals(inboundAuthenticationRequestConfig.getInboundAuthKey(),
                    APPLICATION_INBOUND_AUTH_KEY_OAUTH2);
        }
        applicationManagementService.deleteApplications(SUPER_TENANT_ID);
    }
    
    private ApplicationInboundAuthConfigHandler customSAML2InboundAuthConfigHandler(boolean isOrganization) {
        
        return new ApplicationInboundAuthConfigHandler() {
            @Override
            public boolean canHandle(InboundProtocolsDTO inboundProtocolsDTO) {
                
                return !isOrganization;
            }
            
            @Override
            public boolean canHandle(String protocolName) {
                
                return ApplicationConstants.StandardInboundProtocols.SAML2.equals(protocolName);
            }
            
            @Override
            public InboundAuthenticationRequestConfig handleConfigCreation(ServiceProvider serviceProvider,
                                                                           InboundProtocolsDTO inboundProtocolsDTO)
                    throws IdentityApplicationManagementException {
                
                InboundAuthenticationRequestConfig inboundAuthenticationRequestConfig = new
                        InboundAuthenticationRequestConfig();
                inboundAuthenticationRequestConfig.setInboundAuthKey(APPLICATION_INBOUND_AUTH_KEY_1);
                inboundAuthenticationRequestConfig.setInboundAuthType(
                        ApplicationConstants.StandardInboundProtocols.SAML2);
                return inboundAuthenticationRequestConfig;
            }
            
            @Override
            public InboundAuthenticationRequestConfig handleConfigUpdate(
                    ServiceProvider application, InboundProtocolConfigurationDTO inboundProtocolsDTO)
                    throws IdentityApplicationManagementException {
                
                return null;
            }
            
            @Override
            public void handleConfigDeletion(String appId) throws IdentityApplicationManagementException {
            
            }
            
            @Override
            public InboundProtocolConfigurationDTO handleConfigRetrieval(String appId)
                    throws IdentityApplicationManagementException {
                
                return null;
            }
        };
    }

    private ApplicationInboundAuthConfigHandler customOAuth2InboundAuthConfigHandler() {

        return new ApplicationInboundAuthConfigHandler() {
            @Override
            public boolean canHandle(InboundProtocolsDTO inboundProtocolsDTO) {

                return true;
            }

            @Override
            public boolean canHandle(String protocolName) {

                return ApplicationConstants.StandardInboundProtocols.OAUTH2.equals(protocolName);
            }

            @Override
            public InboundAuthenticationRequestConfig handleConfigCreation(ServiceProvider serviceProvider,
                                                                           InboundProtocolsDTO inboundProtocolsDTO)
                    throws IdentityApplicationManagementException {

                InboundAuthenticationRequestConfig inboundAuthenticationRequestConfig = new
                        InboundAuthenticationRequestConfig();
                inboundAuthenticationRequestConfig.setInboundAuthKey(APPLICATION_INBOUND_AUTH_KEY_OAUTH2);
                inboundAuthenticationRequestConfig.setInboundAuthType(
                        ApplicationConstants.StandardInboundProtocols.OAUTH2);
                return inboundAuthenticationRequestConfig;
            }

            @Override
            public InboundAuthenticationRequestConfig handleConfigUpdate(
                    ServiceProvider application, InboundProtocolConfigurationDTO inboundProtocolsDTO)
                    throws IdentityApplicationManagementException {

                return null;
            }

            @Override
            public void handleConfigDeletion(String appId) throws IdentityApplicationManagementException {

            }

            @Override
            public InboundProtocolConfigurationDTO handleConfigRetrieval(String appId)
                    throws IdentityApplicationManagementException {

                return null;
            }
        };
    }

    @DataProvider(name = "getSAMLApplicationDataProvider")
    public Object[][] getSAMLApplicationDataProvider() {

        return new Object[][]{
            {APPLICATION_NAME_FILTER_1, APPLICATION_NAME_1},
            {APPLICATION_NAME_FILTER_2, APPLICATION_NAME_2},
            {String.format(APPLICATION_ISSUER_FILTER, APPLICATION_INBOUND_AUTH_KEY_1), APPLICATION_NAME_1},
            {String.format(APPLICATION_ISSUER_FILTER, APPLICATION_INBOUND_AUTH_KEY_2), APPLICATION_NAME_2},
            {String.format(APPLICATION_NAME_OR_ISSUER_FILTER, APPLICATION_INBOUND_AUTH_KEY_1), APPLICATION_NAME_1},
            {String.format(APPLICATION_NAME_OR_ISSUER_FILTER, APPLICATION_INBOUND_AUTH_KEY_2), APPLICATION_NAME_2},
            {String.format(APPLICATION_NAME_AND_ISSUER_FILTER, APPLICATION_INBOUND_AUTH_KEY_1), APPLICATION_NAME_1},
            {String.format(APPLICATION_NAME_AND_CLIENT_ID_OR_ISSUER_FILTER, APPLICATION_INBOUND_AUTH_KEY_1,
                    APPLICATION_INBOUND_AUTH_KEY_1), APPLICATION_NAME_1},
        };
    }

    @Test(dataProvider = "getSAMLApplicationDataProvider")
    public void testGetSAMLApplicationBasicInfoWithFilterOffsetLimit(String filter, String expectedResult)
            throws IdentityApplicationManagementException {

        ServiceProvider inputSP1 = new ServiceProvider();
        inputSP1.setApplicationName(APPLICATION_NAME_1);
        addApplicationConfigurations(inputSP1);
        setApplicationInboundAuthConfigs(inputSP1, APPLICATION_INBOUND_AUTH_KEY_1, "samlsso");

        ServiceProvider inputSP2 = new ServiceProvider();
        inputSP2.setApplicationName(APPLICATION_NAME_2);
        addApplicationConfigurations(inputSP2);
        setApplicationInboundAuthConfigs(inputSP2, APPLICATION_INBOUND_AUTH_KEY_2, "samlsso");

        // Adding application.
        applicationManagementService.createApplication(inputSP1, SUPER_TENANT_DOMAIN_NAME, USERNAME_1);
        applicationManagementService.createApplication(inputSP2, SUPER_TENANT_DOMAIN_NAME, USERNAME_1);

        // Test get applications with filter.
        ApplicationBasicInfo[] applicationBasicInfo = applicationManagementService.getApplicationBasicInfo
                (SUPER_TENANT_DOMAIN_NAME, USERNAME_1, filter, 0, 5);
        Assert.assertEquals(applicationBasicInfo[0].getApplicationName(), expectedResult);

        // Deleting all added applications.
        applicationManagementService.deleteApplications(SUPER_TENANT_ID);
    }

    @Test()
    public void testGetApplicationUUIDFromName()
            throws IdentityApplicationManagementException {

        ServiceProvider serviceProvider = new ServiceProvider();
        serviceProvider.setApplicationName(APPLICATION_NAME_1);
        // Adding new application.
        String resourceId = applicationManagementService.createApplication(serviceProvider,
                SUPER_TENANT_DOMAIN_NAME, USERNAME_1);

        // Retrieving added application info.
        String applicationUUID = applicationManagementService.getApplicationUUIDByName(
                serviceProvider.getApplicationName(), SUPER_TENANT_DOMAIN_NAME);
        Assert.assertEquals(applicationUUID, resourceId);

        // Deleting added application.
        applicationManagementService.deleteApplications(SUPER_TENANT_ID);
    }

    @Test
    public void testGetConfiguredAuthenticators() throws IdentityApplicationManagementException {

        ServiceProvider inputSP1 = new ServiceProvider();
        inputSP1.setApplicationName(APPLICATION_NAME_1);
        addApplicationConfigurations(inputSP1);

        // Adding application.
        applicationManagementService.createApplication(inputSP1, SUPER_TENANT_DOMAIN_NAME, USERNAME_1);

        ApplicationBasicInfo applicationBasicInfo = applicationManagementService
                .getApplicationBasicInfoByName(APPLICATION_NAME_1, SUPER_TENANT_DOMAIN_NAME);
        String resourceID = applicationBasicInfo.getApplicationResourceId();
        AuthenticationStep[] steps = applicationManagementService.getConfiguredAuthenticators(resourceID,
                SUPER_TENANT_DOMAIN_NAME);

        Assert.assertEquals(steps.length, 1);
        Assert.assertEquals(steps[0].getStepOrder(), 1);
        applicationManagementService.deleteApplication(APPLICATION_NAME_1, SUPER_TENANT_DOMAIN_NAME, USERNAME_1);
    }


    @Test
    public void testGetCountOfAllApplications() throws IdentityApplicationManagementException {

        addApplications();
        Assert.assertEquals(applicationManagementService.getCountOfAllApplications(SUPER_TENANT_DOMAIN_NAME,
                USERNAME_1), 2);

        // Deleting all added applications.
        applicationManagementService.deleteApplications(SUPER_TENANT_ID);
    }

    @DataProvider(name = "getOAuthApplicationCountDataProvider")
    public Object[][] getOAuthApplicationCountDataProvider() {

        return new Object[][]{
            {APPLICATION_NAME_FILTER_1, 1},
            {APPLICATION_NAME_FILTER_2, 1},
            {String.format(APPLICATION_CLIENT_ID_FILTER, APPLICATION_INBOUND_AUTH_KEY_1), 1},
            {String.format(APPLICATION_CLIENT_ID_FILTER, RANDOM_STRING), 0},
            {String.format(APPLICATION_NAME_OR_CLIENT_ID_FILTER, APPLICATION_INBOUND_AUTH_KEY_1), 1},
            {String.format(APPLICATION_CLIENT_ID_FILTER, RANDOM_STRING), 0},
            {String.format(APPLICATION_NAME_AND_CLIENT_ID_FILTER, APPLICATION_INBOUND_AUTH_KEY_1), 1},
            {String.format(APPLICATION_NAME_AND_CLIENT_ID_FILTER, APPLICATION_INBOUND_AUTH_KEY_2), 0},
            {String.format(APPLICATION_NAME_AND_CLIENT_ID_OR_ISSUER_FILTER, APPLICATION_INBOUND_AUTH_KEY_1,
                    APPLICATION_INBOUND_AUTH_KEY_2), 1},
            {String.format(APPLICATION_NAME_AND_CLIENT_ID_OR_ISSUER_FILTER, APPLICATION_INBOUND_AUTH_KEY_2,
                    APPLICATION_INBOUND_AUTH_KEY_1), 0}
        };
    }

    @Test(dataProvider = "getOAuthApplicationCountDataProvider")
    public void testGetCountOfOAuth2ApplicationsWithFilter(String filter, int expectedResult)
            throws IdentityApplicationManagementException {

        ServiceProvider inputSP1 = new ServiceProvider();
        inputSP1.setApplicationName(APPLICATION_NAME_1);
        addApplicationConfigurations(inputSP1);
        setApplicationInboundAuthConfigs(inputSP1, APPLICATION_INBOUND_AUTH_KEY_1, "oauth2");

        ServiceProvider inputSP2 = new ServiceProvider();
        inputSP2.setApplicationName(APPLICATION_NAME_2);
        addApplicationConfigurations(inputSP2);
        setApplicationInboundAuthConfigs(inputSP2, APPLICATION_INBOUND_AUTH_KEY_2, "oauth2");

        // Adding application.
        applicationManagementService.createApplication(inputSP1, SUPER_TENANT_DOMAIN_NAME, USERNAME_1);
        applicationManagementService.createApplication(inputSP2, SUPER_TENANT_DOMAIN_NAME, USERNAME_1);

        // Test get count of applications with filter.
        Assert.assertEquals(applicationManagementService.getCountOfApplications(SUPER_TENANT_DOMAIN_NAME, USERNAME_1,
                filter), expectedResult);

        // Deleting all added applications.
        applicationManagementService.deleteApplications(SUPER_TENANT_ID);
    }

    @DataProvider(name = "getSAMLApplicationCountDataProvider")
    public Object[][] getSAMLApplicationCountDataProvider() {

        return new Object[][]{
            {APPLICATION_NAME_FILTER_1, 1},
            {APPLICATION_NAME_FILTER_2, 1},
            {String.format(APPLICATION_ISSUER_FILTER, APPLICATION_INBOUND_AUTH_KEY_1), 1},
            {String.format(APPLICATION_ISSUER_FILTER, RANDOM_STRING), 0},
            {String.format(APPLICATION_NAME_OR_ISSUER_FILTER, APPLICATION_INBOUND_AUTH_KEY_1), 1},
            {String.format(APPLICATION_ISSUER_FILTER, RANDOM_STRING), 0},
            {String.format(APPLICATION_NAME_AND_ISSUER_FILTER, APPLICATION_INBOUND_AUTH_KEY_1), 1},
            {String.format(APPLICATION_NAME_AND_ISSUER_FILTER, APPLICATION_INBOUND_AUTH_KEY_2), 0},
            {String.format(APPLICATION_NAME_AND_CLIENT_ID_OR_ISSUER_FILTER, APPLICATION_INBOUND_AUTH_KEY_1,
                    APPLICATION_INBOUND_AUTH_KEY_2), 1},
            {String.format(APPLICATION_NAME_AND_CLIENT_ID_OR_ISSUER_FILTER, APPLICATION_INBOUND_AUTH_KEY_2,
                    APPLICATION_INBOUND_AUTH_KEY_1), 1},
            {String.format(APPLICATION_ISSUER_OR_ISSUER_FILTER, APPLICATION_INBOUND_AUTH_KEY_2,
                    APPLICATION_INBOUND_AUTH_KEY_1), 2}
        };
    }

    @Test(dataProvider = "getSAMLApplicationCountDataProvider")
    public void testGetCountOfSAMLApplicationsWithFilter(String filter, int expectedResult)
            throws IdentityApplicationManagementException {

        ServiceProvider inputSP1 = new ServiceProvider();
        inputSP1.setApplicationName(APPLICATION_NAME_1);
        addApplicationConfigurations(inputSP1);
        setApplicationInboundAuthConfigs(inputSP1, APPLICATION_INBOUND_AUTH_KEY_1, "samlsso");

        ServiceProvider inputSP2 = new ServiceProvider();
        inputSP2.setApplicationName(APPLICATION_NAME_2);
        addApplicationConfigurations(inputSP2);
        setApplicationInboundAuthConfigs(inputSP2, APPLICATION_INBOUND_AUTH_KEY_2, "samlsso");

        // Adding application.
        applicationManagementService.createApplication(inputSP1, SUPER_TENANT_DOMAIN_NAME, USERNAME_1);
        applicationManagementService.createApplication(inputSP2, SUPER_TENANT_DOMAIN_NAME, USERNAME_1);

        // Test get count of applications with filter.
        Assert.assertEquals(applicationManagementService.getCountOfApplications(SUPER_TENANT_DOMAIN_NAME, USERNAME_1,
                filter), expectedResult);

        // Deleting all added applications.
        applicationManagementService.deleteApplications(SUPER_TENANT_ID);
    }

    @DataProvider(name = "getIdentityProviderDataProvider")
    public Object[][] getIdentityProviderDataProvider() {

        IdentityProvider idp1 = new IdentityProvider();
        idp1.setIdentityProviderName(IDP_NAME_1);

        IdentityProvider idp2 = new IdentityProvider();
        idp2.setIdentityProviderName(IDP_NAME_2);

        return new Object[][]{
                {idp1, SUPER_TENANT_DOMAIN_NAME, SUPER_TENANT_ID},
                {idp2, SUPER_TENANT_DOMAIN_NAME, SUPER_TENANT_ID}
        };
    }

    @Test(dataProvider = "getIdentityProviderDataProvider")
    public void testGetIdentityProvider(Object idp, String tenantDomain, int tenantId) throws
            IdentityApplicationManagementException,
            IdentityProviderManagementException {

        idPManagementDAO = new IdPManagementDAO();
        idPManagementDAO.addIdP((IdentityProvider) idp, tenantId);

        IdentityProvider identityProvider = applicationManagementService.getIdentityProvider
                (((IdentityProvider) idp).getIdentityProviderName(), tenantDomain);
        Assert.assertEquals(identityProvider.getIdentityProviderName(), ((IdentityProvider) idp).
                getIdentityProviderName());

        // Deleting added identity provider.
        idPManagementDAO.deleteIdP(identityProvider.getIdentityProviderName(), tenantId, tenantDomain);
    }

    private void addIdentityProviders() throws IdentityProviderManagementException {

        idPManagementDAO = new IdPManagementDAO();

        IdentityProvider idp1 = new IdentityProvider();
        idp1.setIdentityProviderName(IDP_NAME_1);

        IdentityProvider idp2 = new IdentityProvider();
        idp2.setIdentityProviderName(IDP_NAME_2);

        idPManagementDAO.addIdP(idp1, SUPER_TENANT_ID);
        idPManagementDAO.addIdP(idp2, SUPER_TENANT_ID);
    }

    @Test
    public void testGetAllIdentityProviders() throws IdentityApplicationManagementException,
            IdentityProviderManagementException {

        addIdentityProviders();

        IdentityProvider[] identityProviders = applicationManagementService.
                getAllIdentityProviders(SUPER_TENANT_DOMAIN_NAME);
        Assert.assertEquals(identityProviders[0].getIdentityProviderName(), IDP_NAME_1);
        Assert.assertEquals(identityProviders[1].getIdentityProviderName(), IDP_NAME_2);

        // Deleting added all identity providers.
        idPManagementDAO.deleteIdPs(SUPER_TENANT_ID);
    }

    @Test
    public void testGetAllLocalAuthenticators() throws IdentityApplicationManagementException {

        ApplicationAuthenticatorService appAuthenticatorService = ApplicationAuthenticatorService.getInstance();
        LocalAuthenticatorConfig localAuthenticatorConfig = new LocalAuthenticatorConfig();
        appAuthenticatorService.addLocalAuthenticator(localAuthenticatorConfig);

        LocalAuthenticatorConfig[] localAuthenticatorConfigs = applicationManagementService.getAllLocalAuthenticators
                (SUPER_TENANT_DOMAIN_NAME);

        Assert.assertEquals(localAuthenticatorConfigs[0], localAuthenticatorConfig);
    }

    @Test
    public void testGetAllRequestPathAuthenticators() throws IdentityApplicationManagementException {

        ApplicationAuthenticatorService appAuthenticatorService = ApplicationAuthenticatorService.getInstance();
        RequestPathAuthenticatorConfig requestPathAuthenticatorConfig = new RequestPathAuthenticatorConfig();
        appAuthenticatorService.addRequestPathAuthenticator(requestPathAuthenticatorConfig);

        RequestPathAuthenticatorConfig[] requestPathAuthenticatorConfigs = applicationManagementService.
                getAllRequestPathAuthenticators(SUPER_TENANT_DOMAIN_NAME);

        Assert.assertEquals(requestPathAuthenticatorConfigs[0], requestPathAuthenticatorConfig);
    }

    @Test(dataProvider = "addApplicationDataProvider")
    public void testCreateApplication(Object serviceProvider, String tenantDomain, String username) throws
            IdentityApplicationManagementException {

        // Adding application.
        addApplicationConfigurations((ServiceProvider) serviceProvider);
        String resourceId = applicationManagementService.createApplication((ServiceProvider) serviceProvider,
                tenantDomain, username);

        // Retrieving application by ResourceId.
        ServiceProvider expectedSP = applicationManagementService.getApplicationByResourceId(resourceId,
                tenantDomain);
        Assert.assertEquals(resourceId, expectedSP.getApplicationResourceId());

        ApplicationBasicInfo applicationBasicInfo = applicationManagementService.getApplicationBasicInfoByResourceId
                (resourceId, tenantDomain);
        Assert.assertEquals(applicationBasicInfo.getApplicationName(), ((ServiceProvider) serviceProvider).
                getApplicationName());

        // Deleting added application.
        applicationManagementService.deleteApplicationByResourceId(resourceId, tenantDomain, username);
    }

    @Test
    public void testGetServiceProviderByClientId() throws IdentityApplicationManagementException {

        ServiceProvider inputSP = new ServiceProvider();
        inputSP.setApplicationName(APPLICATION_NAME_1);
        addApplicationConfigurations(inputSP);

        // Adding application.
        applicationManagementService.createApplication(inputSP, SUPER_TENANT_DOMAIN_NAME, USERNAME_1);

        // Retrieving application by ResourceId.
        ServiceProvider actual = applicationManagementService.getServiceProviderByClientId("auth key",
                "oauth2", SUPER_TENANT_DOMAIN_NAME);

        Assert.assertEquals(actual.getApplicationName(), inputSP.getApplicationName());
        Assert.assertEquals(actual.getOwner().getUserName(), USERNAME_1);
        Assert.assertEquals(actual.getDescription(), inputSP.getDescription());
        Assert.assertEquals(actual.getApplicationVersion(), ApplicationConstants.ApplicationVersion.LATEST_APP_VERSION);

        // Deleting all added application.
        applicationManagementService.deleteApplications(SUPER_TENANT_ID);
    }

    @DataProvider(name = "testAddApplicationWithIsManagementApplicationData")
    public Object[][] testAddApplicationWithIsManagementApplicationData() {


        return new Object[][]{
                {true},
                {false}
        };
    }

    @Test(dataProvider = "testAddApplicationWithIsManagementApplicationData")
    public void testAddApplicationWithIsManagementApplication(boolean isManagementApp) throws Exception {

        ServiceProvider inputSP = new ServiceProvider();
        inputSP.setApplicationName(APPLICATION_NAME_1);

        addApplicationConfigurations(inputSP);
        inputSP.setManagementApp(isManagementApp);

        // Adding new application.
        ServiceProvider addedSP = applicationManagementService.addApplication(inputSP, SUPER_TENANT_DOMAIN_NAME,
                REGISTRY_SYSTEM_USERNAME);
        Assert.assertEquals(addedSP.isManagementApp(), isManagementApp);


        //  Retrieving added application.
        ServiceProvider retrievedSP = applicationManagementService.getApplicationExcludingFileBasedSPs
                (inputSP.getApplicationName(), SUPER_TENANT_DOMAIN_NAME);
        Assert.assertEquals(retrievedSP.isManagementApp(), isManagementApp);

        // Updating the application by changing the isManagementApplication flag. It should not be changed.
        inputSP.setManagementApp(!isManagementApp);

        applicationManagementService.updateApplication(inputSP, SUPER_TENANT_DOMAIN_NAME, REGISTRY_SYSTEM_USERNAME);

        retrievedSP = applicationManagementService.getApplicationExcludingFileBasedSPs
                (inputSP.getApplicationName(), SUPER_TENANT_DOMAIN_NAME);

        Assert.assertEquals(retrievedSP.isManagementApp(), isManagementApp);

        // Deleting added application.
        applicationManagementService.deleteApplication(inputSP.getApplicationName(), SUPER_TENANT_DOMAIN_NAME,
                REGISTRY_SYSTEM_USERNAME);
    }

    @DataProvider(name = "testAddApplicationWithAPIBasedAuthenticationData")
    public Object[][] testAddApplicationWithAPIBasedAuthenticationData() {


        return new Object[][]{
                {true},
                {false}
        };
    }

    @Test(dataProvider = "testAddApplicationWithAPIBasedAuthenticationData")
    public void testAddApplicationWithAPIBasedAuthentication(boolean isAPIBasedAuthenticationEnabled) throws Exception {


        ServiceProvider inputSP = new ServiceProvider();
        inputSP.setApplicationName(APPLICATION_NAME_1);

        addApplicationConfigurations(inputSP);
        inputSP.setAPIBasedAuthenticationEnabled(isAPIBasedAuthenticationEnabled);

        // Adding new application.
        ServiceProvider addedSP = applicationManagementService.addApplication(inputSP, SUPER_TENANT_DOMAIN_NAME,
                REGISTRY_SYSTEM_USERNAME);
        Assert.assertEquals(addedSP.isAPIBasedAuthenticationEnabled(), isAPIBasedAuthenticationEnabled);


        //  Retrieving added application.
        ServiceProvider retrievedSP = applicationManagementService.getApplicationExcludingFileBasedSPs
                (inputSP.getApplicationName(), SUPER_TENANT_DOMAIN_NAME);
        Assert.assertEquals(retrievedSP.isAPIBasedAuthenticationEnabled(), isAPIBasedAuthenticationEnabled);

        // Updating the application by changing the isManagementApplication flag. It should be changed.
        inputSP.setAPIBasedAuthenticationEnabled(!isAPIBasedAuthenticationEnabled);

        applicationManagementService.updateApplication(inputSP, SUPER_TENANT_DOMAIN_NAME, REGISTRY_SYSTEM_USERNAME);

        retrievedSP = applicationManagementService.getApplicationExcludingFileBasedSPs
                (inputSP.getApplicationName(), SUPER_TENANT_DOMAIN_NAME);

        Assert.assertEquals(retrievedSP.isAPIBasedAuthenticationEnabled(), !isAPIBasedAuthenticationEnabled);

        // Deleting added application.
        applicationManagementService.deleteApplication(inputSP.getApplicationName(), SUPER_TENANT_DOMAIN_NAME,
                REGISTRY_SYSTEM_USERNAME);
    }

    @DataProvider(name = "testAddApplicationWithAttestationData")
    public Object[][] testAddApplicationWithAttestationData() {


        return new Object[][]{
                {true, ANDROID_PACKAGE_NAME_1, "sampleCredentials", APPLE_APP_ID}
        };
    }

    @Test(dataProvider = "testAddApplicationWithAttestationData")
    public void testAddApplicationWithAttestationData(boolean isAttestationEnabled,
                                                      String androidPackageName,
                                                      String androidCredentials,
                                                      String appleAppId) throws Exception {

        ResolvedSecret resolvedSecret = new ResolvedSecret();
        resolvedSecret.setResolvedSecretValue(androidCredentials);
        SecretResolveManager secretResolveManager = mock(SecretResolveManagerImpl.class);
        ApplicationManagementServiceComponentHolder.getInstance().setSecretResolveManager(secretResolveManager);
        when(secretResolveManager.getResolvedSecret(anyString(), anyString())).thenReturn(resolvedSecret);

        ServiceProvider inputSP = new ServiceProvider();
        inputSP.setApplicationName(APPLICATION_NAME_1);

        addApplicationConfigurations(inputSP);
        ClientAttestationMetaData clientAttestationMetaData = new ClientAttestationMetaData();
        clientAttestationMetaData.setAttestationEnabled(isAttestationEnabled);
        clientAttestationMetaData.setAndroidPackageName(androidPackageName);
        clientAttestationMetaData.setAppleAppId(appleAppId);
        clientAttestationMetaData.setAndroidAttestationServiceCredentials(androidCredentials);
        inputSP.setClientAttestationMetaData(clientAttestationMetaData);

        // Adding new application.
        ServiceProvider addedSP = applicationManagementService.addApplication(inputSP, SUPER_TENANT_DOMAIN_NAME,
                REGISTRY_SYSTEM_USERNAME);
        Assert.assertEquals(addedSP.getClientAttestationMetaData().isAttestationEnabled(), isAttestationEnabled);
        Assert.assertEquals(addedSP.getClientAttestationMetaData().getAndroidPackageName(), androidPackageName);
        Assert.assertEquals(addedSP.getClientAttestationMetaData().getAndroidAttestationServiceCredentials(),
                androidCredentials);
        Assert.assertEquals(addedSP.getClientAttestationMetaData().getAppleAppId(),
                appleAppId);

        SecretManager secretManager = mock(SecretManagerImpl.class);
        when(secretManager.isSecretExist(anyString(), anyString())).thenReturn(true);
        ApplicationManagementServiceComponentHolder.getInstance().setSecretManager(secretManager);

        //  Retrieving added application.
        ServiceProvider retrievedSP = applicationManagementService.getApplicationExcludingFileBasedSPs
                (inputSP.getApplicationName(), SUPER_TENANT_DOMAIN_NAME);
        Assert.assertEquals(retrievedSP.getClientAttestationMetaData().isAttestationEnabled(), isAttestationEnabled);
        Assert.assertEquals(retrievedSP.getClientAttestationMetaData().getAndroidPackageName(), androidPackageName);
        Assert.assertEquals(retrievedSP.getClientAttestationMetaData().getAppleAppId(), appleAppId);
        Assert.assertEquals(retrievedSP.getClientAttestationMetaData().getAndroidAttestationServiceCredentials(),
                androidCredentials);
        // Updating the application by changing the isManagementApplication flag. It should be changed.
        ClientAttestationMetaData clientAttestationMetaData2 = new ClientAttestationMetaData();
        clientAttestationMetaData2.setAttestationEnabled(!isAttestationEnabled);
        clientAttestationMetaData2.setAndroidPackageName(null);
        clientAttestationMetaData2.setAppleAppId(null);
        clientAttestationMetaData2.setAndroidAttestationServiceCredentials(null);
        inputSP.setClientAttestationMetaData(clientAttestationMetaData2);
        applicationManagementService.updateApplication(inputSP, SUPER_TENANT_DOMAIN_NAME, REGISTRY_SYSTEM_USERNAME);

        retrievedSP = applicationManagementService.getApplicationExcludingFileBasedSPs
                (inputSP.getApplicationName(), SUPER_TENANT_DOMAIN_NAME);

        Assert.assertEquals(retrievedSP.getClientAttestationMetaData().isAttestationEnabled(), !isAttestationEnabled);
        Assert.assertNull(retrievedSP.getClientAttestationMetaData().getAndroidAttestationServiceCredentials());
        // Deleting added application.
        applicationManagementService.deleteApplication(inputSP.getApplicationName(), SUPER_TENANT_DOMAIN_NAME,
                REGISTRY_SYSTEM_USERNAME);
    }

    @DataProvider(name = "trustedAppMetadataDataProvider")
    public Object[][] trustedAppMetadataDataProvider() {

        String[] thumbprints1 = {"sampleThumbprint1"};
        String[] thumbprints2 = {"sampleThumbprint1", "sampleThumbprint2"};

        return new Object[][]{
                {ANDROID_PACKAGE_NAME_1, thumbprints1, APPLE_APP_ID, false, false,
                        true},
                {ANDROID_PACKAGE_NAME_1, thumbprints2, null, false, false, true},
                {null, null, APPLE_APP_ID, false, false, true},
                // Check if consent property is handled correctly.
                {ANDROID_PACKAGE_NAME_1, thumbprints1, APPLE_APP_ID, false, true,
                        true},
                {ANDROID_PACKAGE_NAME_1, thumbprints1, APPLE_APP_ID, true, true,
                        true},
                {ANDROID_PACKAGE_NAME_1, thumbprints1, APPLE_APP_ID, true, false,
                        false}
        };
    }

    @Test(dataProvider = "trustedAppMetadataDataProvider")
    public void testTrustedAppMetadata(String androidPackageName, String[] androidThumbprints, String appleAppId,
                                       boolean isConsentRequired, boolean isConsentGranted, boolean expectedConsent)
            throws Exception {

        ServiceProvider inputSP = new ServiceProvider();
        inputSP.setApplicationName(APPLICATION_NAME_1);
        addApplicationConfigurations(inputSP);

        SpTrustedAppMetadata trustedAppMetadata = new SpTrustedAppMetadata();
        trustedAppMetadata.setAndroidPackageName(androidPackageName);
        trustedAppMetadata.setAppleAppId(appleAppId);
        trustedAppMetadata.setAndroidThumbprints(androidThumbprints);
        trustedAppMetadata.setIsFidoTrusted(true);
        trustedAppMetadata.setIsConsentGranted(isConsentGranted);
        inputSP.setTrustedAppMetadata(trustedAppMetadata);

        try (MockedStatic<IdentityUtil> identityUtil = Mockito.mockStatic(IdentityUtil.class,
                Mockito.CALLS_REAL_METHODS)) {
            identityUtil.when(() -> IdentityUtil.getProperty(TRUSTED_APP_CONSENT_REQUIRED_PROPERTY)).
                    thenReturn(String.valueOf(isConsentRequired));
            // Adding new application.
            String addedSpId = applicationManagementService.createApplication(inputSP, SUPER_TENANT_DOMAIN_NAME,
                    REGISTRY_SYSTEM_USERNAME);
            ServiceProvider retrievedSP = applicationManagementService.getApplicationByResourceId(addedSpId,
                    SUPER_TENANT_DOMAIN_NAME);

            Assert.assertEquals(retrievedSP.getTrustedAppMetadata().getAndroidPackageName(), androidPackageName);
            Assert.assertEquals(retrievedSP.getTrustedAppMetadata().getAndroidThumbprints(), androidThumbprints);
            Assert.assertEquals(retrievedSP.getTrustedAppMetadata().getAppleAppId(), appleAppId);
            Assert.assertTrue(retrievedSP.getTrustedAppMetadata().getIsFidoTrusted());
            Assert.assertEquals(retrievedSP.getTrustedAppMetadata().getIsConsentGranted(), expectedConsent);

            // Deleting added application.
            applicationManagementService.deleteApplication(inputSP.getApplicationName(), SUPER_TENANT_DOMAIN_NAME,
                    REGISTRY_SYSTEM_USERNAME);
        }
    }

    @DataProvider(name = "testGetTrustedAppsDataProvider")
    public Object[][] testGetTrustedAppsDataProvider() {

        SpTrustedAppMetadata trustedAppMetadata1 = new SpTrustedAppMetadata();
        trustedAppMetadata1.setAndroidPackageName(ANDROID_PACKAGE_NAME_1);
        trustedAppMetadata1.setAppleAppId(APPLE_APP_ID);
        String[] thumbprints1 = {"sampleThumbprint1"};
        trustedAppMetadata1.setAndroidThumbprints(thumbprints1);
        trustedAppMetadata1.setIsFidoTrusted(true);

        SpTrustedAppMetadata trustedAppMetadata2 = new SpTrustedAppMetadata();
        trustedAppMetadata2.setAndroidPackageName(ANDROID_PACKAGE_NAME_2);
        trustedAppMetadata2.setAppleAppId(null);
        String[] thumbprints2 = {"sampleThumbprint1", "sampleThumbprint2"};
        trustedAppMetadata2.setAndroidThumbprints(thumbprints2);
        trustedAppMetadata2.setIsFidoTrusted(true);

        return new Object[][]{
                {trustedAppMetadata1, trustedAppMetadata2, 1},
                {trustedAppMetadata1, trustedAppMetadata1, 2}
        };
    }

    @Test(dataProvider = "testGetTrustedAppsDataProvider")
    public void testGetTrustedApps(SpTrustedAppMetadata trustedAppMetadata1, SpTrustedAppMetadata trustedAppMetadata2,
                                   int appleTrustedAppCount) throws Exception {

        // Adding 2 applications with different trusted app metadata.
        ServiceProvider serviceProvider1 = new ServiceProvider();
        serviceProvider1.setApplicationName(APPLICATION_NAME_1);
        addApplicationConfigurations(serviceProvider1);
        serviceProvider1.setTrustedAppMetadata(trustedAppMetadata1);
        applicationManagementService.createApplication(serviceProvider1, SUPER_TENANT_DOMAIN_NAME,
                REGISTRY_SYSTEM_USERNAME);

        ServiceProvider serviceProvider2 = new ServiceProvider();
        serviceProvider2.setApplicationName(APPLICATION_NAME_2);
        addApplicationConfigurations(serviceProvider2);
        serviceProvider2.setTrustedAppMetadata(trustedAppMetadata2);
        applicationManagementService.createApplication(serviceProvider2, SUPER_TENANT_DOMAIN_NAME,
                REGISTRY_SYSTEM_USERNAME);

        // Get trusted apps for android and apple platforms.
        List<TrustedApp> androidTrustedApps = applicationManagementService.getTrustedApps(PlatformType.ANDROID);
        List<TrustedApp> appleTrustedApps = applicationManagementService.getTrustedApps(PlatformType.IOS);

        Assert.assertEquals(androidTrustedApps.size(), 2);
        Assert.assertEquals(appleTrustedApps.size(), appleTrustedAppCount);

        Assert.assertEquals(androidTrustedApps.get(0).getAppIdentifier(), trustedAppMetadata1.getAndroidPackageName());
        Assert.assertEquals(androidTrustedApps.get(1).getAppIdentifier(), trustedAppMetadata2.getAndroidPackageName());
        Assert.assertEquals(androidTrustedApps.get(0).getThumbprints(), trustedAppMetadata1.getAndroidThumbprints());
        Assert.assertEquals(androidTrustedApps.get(1).getThumbprints(), trustedAppMetadata2.getAndroidThumbprints());

        Assert.assertEquals(appleTrustedApps.get(0).getAppIdentifier(), trustedAppMetadata1.getAppleAppId());
        Assert.assertEquals(appleTrustedApps.get(0).getThumbprints(), new String[0]);

        // Deleting all added applications.
        applicationManagementService.deleteApplications(SUPER_TENANT_ID);
    }

    @DataProvider(name = "addApplicationWithTemplateIdAndTemplateVersionData")
    public Object[][] addApplicationWithTemplateIdAndTemplateVersionData() {

        return new Object[][] {
                {APPLICATION_TEMPLATE_ID_1, APPLICATION_TEMPLATE_VERSION_1},
                {null, null}
        };
    }

    @Test(dataProvider = "addApplicationWithTemplateIdAndTemplateVersionData")
    public void addApplicationWithTemplateIdAndTemplateVersionData(String templateId, String templateVersion)
            throws Exception {

        String expectedTemplateId = templateId != null ? templateId : "";
        String expectedTemplateVersion = templateVersion != null ? templateVersion : "";

        ServiceProvider inputSP = new ServiceProvider();
        inputSP.setApplicationName(APPLICATION_NAME_1);

        addApplicationConfigurations(inputSP);
        inputSP.setTemplateId(templateId);
        inputSP.setTemplateVersion(templateVersion);

        // Adding new application.
        String resourceId = applicationManagementService.createApplication(inputSP, SUPER_TENANT_DOMAIN_NAME,
                REGISTRY_SYSTEM_USERNAME);

        //  Retrieving added application.
        ServiceProvider retrievedSP =
                applicationManagementService.getApplicationByResourceId(resourceId, SUPER_TENANT_DOMAIN_NAME);
        Assert.assertEquals(retrievedSP.getTemplateId(), expectedTemplateId);
        Assert.assertEquals(retrievedSP.getTemplateVersion(), expectedTemplateVersion);

        // Retrieving application with required attributes as templateId and templateVersion.
        List<String> requiredAttributes =
                Arrays.asList(TEMPLATE_ID_SP_PROPERTY_NAME, TEMPLATE_VERSION_SP_PROPERTY_NAME);
        ServiceProvider retrievedSPWithRequiredAttributes =
                applicationManagementService.getApplicationWithRequiredAttributes(retrievedSP.getApplicationID(),
                        requiredAttributes);
        Assert.assertEquals(retrievedSPWithRequiredAttributes.getTemplateId(), expectedTemplateId);
        Assert.assertEquals(retrievedSPWithRequiredAttributes.getTemplateVersion(), expectedTemplateVersion);

        // Updating the application by changing the templateId and templateVersion. It should be changed.
        inputSP.setTemplateId(APPLICATION_TEMPLATE_ID_2);
        inputSP.setTemplateVersion(APPLICATION_TEMPLATE_VERSION_2);

        applicationManagementService.updateApplicationByResourceId(resourceId, inputSP, SUPER_TENANT_DOMAIN_NAME,
                REGISTRY_SYSTEM_USERNAME);

        retrievedSP = applicationManagementService.getApplicationByResourceId(resourceId, SUPER_TENANT_DOMAIN_NAME);

        Assert.assertEquals(retrievedSP.getTemplateId(), APPLICATION_TEMPLATE_ID_2);
        Assert.assertEquals(retrievedSP.getTemplateVersion(), APPLICATION_TEMPLATE_VERSION_2);

        // Deleting added application.
        applicationManagementService.deleteApplication(inputSP.getApplicationName(), SUPER_TENANT_DOMAIN_NAME,
                REGISTRY_SYSTEM_USERNAME);
    }

    @Test(groups = "certificate", priority = 1)
    public void testAddApplicationWithCertificate() throws CertificateMgtException {

        ServiceProvider inputSP = new ServiceProvider();
        inputSP.setApplicationName(APPLICATION_NAME_1);
        inputSP.setCertificateContent(certificate.getCertificateContent());
        addApplicationConfigurations(inputSP);

        doReturn(0).when(applicationCertificateManagementService).addCertificate(any(), anyString());
        doReturn(certificate).when(applicationCertificateManagementService).getCertificateByName(any(), anyString());
        try {
            appIdWithCert = applicationManagementService.createApplication(inputSP, SUPER_TENANT_DOMAIN_NAME,
                    REGISTRY_SYSTEM_USERNAME);
        } catch (Exception e) {
            Assert.fail("Application addition with certificate addition should be successful without any exceptions");
        }
    }

    @Test(groups = "certificate", priority = 2, dependsOnMethods = "testAddApplicationWithCertificate")
    public void testGetApplicationWithServerErrorWhenRetrievingCert() throws CertificateMgtException {

        doThrow(serverException).when(applicationCertificateManagementService).getCertificate(anyInt(), anyString());
        try {
            applicationManagementService.getApplicationByResourceId(appIdWithCert, SUPER_TENANT_DOMAIN_NAME);
            Assert.fail("Successful retrieval of the application without an exception is considered as a failure");
        } catch (IdentityApplicationManagementException e) {
            Assert.assertEquals(e.getClass(), IdentityApplicationManagementServerException.class);
            Assert.assertTrue(e.getMessage().contains("Error while retrieving application with resourceId: "
                    + appIdWithCert + " in tenantDomain: " + SUPER_TENANT_DOMAIN_NAME));
            for (Throwable cause = e.getCause(); cause != null; cause = cause.getCause()) {
                if (cause instanceof CertificateMgtServerException) {
                    return;
                }
            }
            Assert.fail("Expected cause of type CertificateMgtServerException was not found in the exception chain");
        }
    }

    @Test(groups = "certificate", priority = 3, dependsOnMethods = "testAddApplicationWithCertificate")
    public void testGetApplicationWithCertificate() throws IdentityApplicationManagementException,
            CertificateMgtException {

        reset(applicationCertificateManagementService);
        when(applicationCertificateManagementService.getCertificate(anyInt(), anyString())).thenReturn(certificate);
        appWithCert = applicationManagementService.getApplicationByResourceId(appIdWithCert, SUPER_TENANT_DOMAIN_NAME);

        Optional<String> certificateValue = Arrays.stream(appWithCert.getSpProperties())
                .filter(prop -> "CERTIFICATE".equals(prop.getName()))
                .map(ServiceProviderProperty::getValue)
                .findFirst();
        Assert.assertTrue(certificateValue.isPresent(), "Certificate property not found");
        Assert.assertEquals(Integer.parseInt(certificateValue.get()), CERTIFICATE_ID);
        Assert.assertEquals(appWithCert.getCertificateContent(), certificate.getCertificateContent());
    }

    @Test(groups = "certificate", priority = 4, dependsOnMethods = {"testAddApplicationWithCertificate",
            "testGetApplicationWithCertificate"})
    public void testUpdateApplicationWithCertificate() throws CertificateMgtException {

        doNothing().when(applicationCertificateManagementService).updateCertificateContent(anyInt(), anyString(),
                anyString());
        try {
            appWithCert.setCertificateContent(UPDATED_CERTIFICATE);
            applicationManagementService.updateApplicationByResourceId(appIdWithCert, appWithCert,
                    SUPER_TENANT_DOMAIN_NAME, REGISTRY_SYSTEM_USERNAME);
        } catch (Exception e) {
            Assert.fail("Application update with certificate update should be successful without any exceptions");
        }
    }

    @Test(groups = "certificate", priority = 5, dependsOnMethods = {"testAddApplicationWithCertificate",
            "testGetApplicationWithCertificate"})
    public void testUpdateApplicationWithServerErrorWhenUpdatingCert() throws CertificateMgtException {

        reset(applicationCertificateManagementService);
        doThrow(serverException).when(applicationCertificateManagementService).updateCertificateContent(anyInt(),
                anyString(), anyString());
        try {
            appWithCert.setCertificateContent(UPDATED_CERTIFICATE);
            applicationManagementService.updateApplicationByResourceId(appIdWithCert, appWithCert,
                    SUPER_TENANT_DOMAIN_NAME, REGISTRY_SYSTEM_USERNAME);
            Assert.fail("Successful update of the application without an exception is considered as a failure");
        } catch (IdentityApplicationManagementException e) {
            Assert.assertEquals(e.getClass(), IdentityApplicationManagementServerException.class);
            Assert.assertTrue(e.getMessage().contains("Error while updating application with resourceId: "
                    + appIdWithCert + " in tenantDomain: " + SUPER_TENANT_DOMAIN_NAME));
            for (Throwable cause = e.getCause(); cause != null; cause = cause.getCause()) {
                if (cause instanceof CertificateMgtServerException) {
                    return;
                }
            }
            Assert.fail("Expected cause of type CertificateMgtServerException was not found in the exception chain");
        }
    }

    @Test(groups = "certificate", priority = 6, dependsOnMethods = {"testAddApplicationWithCertificate",
            "testGetApplicationWithCertificate"})
    public void testUpdateApplicationWithClientErrorWhenUpdatingCert() throws CertificateMgtException {

        reset(applicationCertificateManagementService);
        doThrow(clientException).when(applicationCertificateManagementService).updateCertificateContent(anyInt(),
                anyString(), anyString());
        try {
            appWithCert.setCertificateContent(UPDATED_CERTIFICATE);
            applicationManagementService.updateApplicationByResourceId(appIdWithCert, appWithCert,
                    SUPER_TENANT_DOMAIN_NAME, REGISTRY_SYSTEM_USERNAME);
            Assert.fail("Successful update of the application without an exception is considered as a failure");
        } catch (IdentityApplicationManagementException e) {
            Assert.assertEquals(e.getClass(), IdentityApplicationManagementClientException.class);
            Assert.assertEquals(e.getMessage(), clientException.getDescription());
            for (Throwable cause = e.getCause(); cause != null; cause = cause.getCause()) {
                if (cause instanceof CertificateMgtClientException) {
                    return;
                }
            }
            Assert.fail("Expected cause of type CertificateMgtClientException was not found in the exception chain");
        }
    }

    @Test(groups = "certificate", priority = 7, dependsOnMethods = {"testAddApplicationWithCertificate",
            "testGetApplicationWithCertificate"})
    public void testUpdateApplicationWithServerErrorWhenDeletingCert() throws CertificateMgtException {

        doThrow(serverException).when(applicationCertificateManagementService).deleteCertificate(anyInt(), anyString());
        try {
            appWithCert.setCertificateContent(StringUtils.EMPTY);
            applicationManagementService.updateApplicationByResourceId(appIdWithCert, appWithCert,
                    SUPER_TENANT_DOMAIN_NAME, REGISTRY_SYSTEM_USERNAME);
            Assert.fail("Successful update of the application without an exception is considered as a failure");
        } catch (IdentityApplicationManagementException e) {
            Assert.assertEquals(e.getClass(), IdentityApplicationManagementServerException.class);
            Assert.assertTrue(e.getMessage().contains("Error while updating application with resourceId: "
                    + appIdWithCert + " in tenantDomain: " + SUPER_TENANT_DOMAIN_NAME));
            for (Throwable cause = e.getCause(); cause != null; cause = cause.getCause()) {
                if (cause instanceof CertificateMgtServerException) {
                    return;
                }
            }
            Assert.fail("Expected cause of type CertificateMgtServerException was not found in the exception chain");
        }
    }

    @Test(groups = "certificate", priority = 8, dependsOnMethods = {"testAddApplicationWithCertificate",
            "testGetApplicationWithCertificate"})
    public void testDeleteApplicationWithCertificate() throws IdentityApplicationManagementException,
            CertificateMgtException {

        reset(applicationCertificateManagementService);
        doNothing().when(applicationCertificateManagementService).deleteCertificate(anyInt(), anyString());
        try {
            appWithCert.setCertificateContent(StringUtils.EMPTY);
            applicationManagementService.updateApplicationByResourceId(appIdWithCert, appWithCert,
                    SUPER_TENANT_DOMAIN_NAME, REGISTRY_SYSTEM_USERNAME);
        } catch (Exception e) {
            Assert.fail("Application update with certificate deletion should be successful without any exceptions");
        }

        appWithCert = applicationManagementService.getApplicationByResourceId(appIdWithCert, SUPER_TENANT_DOMAIN_NAME);
        Optional<String> certificateValue = Arrays.stream(appWithCert.getSpProperties())
                .filter(prop -> "CERTIFICATE".equals(prop.getName()))
                .map(ServiceProviderProperty::getValue)
                .findFirst();
        Assert.assertFalse(certificateValue.isPresent(), "Certificate property should be removed");
        Assert.assertNull(appWithCert.getCertificateContent());

        // Deleting added application.
        doNothing().when(applicationCertificateManagementService).deleteCertificate(anyInt(), anyString());
        applicationManagementService.deleteApplication(appWithCert.getApplicationName(),
                SUPER_TENANT_DOMAIN_NAME, REGISTRY_SYSTEM_USERNAME);
    }

    @Test(groups = "certificate", priority = 9)
    public void testAddApplicationWithServerErrorWhenAddingCert() throws CertificateMgtException {

        ServiceProvider inputSP = new ServiceProvider();
        inputSP.setApplicationName(APPLICATION_NAME_1);
        inputSP.setCertificateContent(certificate.getCertificateContent());
        addApplicationConfigurations(inputSP);

        reset(applicationCertificateManagementService);
        doThrow(serverException).when(applicationCertificateManagementService).addCertificate(any(), anyString());
        try {
            appIdWithCert = applicationManagementService.createApplication(inputSP, SUPER_TENANT_DOMAIN_NAME,
                    REGISTRY_SYSTEM_USERNAME);
            Assert.fail("Successful creation of the application without an exception is considered as a failure");
        } catch (IdentityApplicationManagementException e) {
            Assert.assertTrue(e.getMessage().contains("Error while creating an application: "
                    + inputSP.getApplicationName() + " in tenantDomain: " + SUPER_TENANT_DOMAIN_NAME));
            for (Throwable cause = e.getCause(); cause != null; cause = cause.getCause()) {
                if (cause instanceof CertificateMgtServerException) {
                    return;
                }
            }
            Assert.fail("Expected cause of type CertificateMgtServerException was not found in the exception chain");
        }
    }

    @Test(groups = "certificate", priority = 10)
    public void testAddApplicationWithClientErrorWhenAddingCert() throws CertificateMgtException {

        ServiceProvider inputSP = new ServiceProvider();
        inputSP.setApplicationName(APPLICATION_NAME_1);
        inputSP.setCertificateContent(certificate.getCertificateContent());
        addApplicationConfigurations(inputSP);

        reset(applicationCertificateManagementService);
        doThrow(clientException).when(applicationCertificateManagementService).addCertificate(any(), anyString());
        try {
            appIdWithCert = applicationManagementService.createApplication(inputSP, SUPER_TENANT_DOMAIN_NAME,
                    REGISTRY_SYSTEM_USERNAME);
            Assert.fail("Successful creation of the application without an exception is considered as a failure");
        } catch (IdentityApplicationManagementException e) {
            Assert.assertEquals(e.getClass(), IdentityApplicationManagementClientException.class);
            Assert.assertEquals(e.getMessage(), clientException.getDescription());
            for (Throwable cause = e.getCause(); cause != null; cause = cause.getCause()) {
                if (cause instanceof CertificateMgtClientException) {
                    return;
                }
            }
            Assert.fail("Expected cause of type CertificateMgtClientException was not found in the exception chain");
        }
    }

    @Test(groups = "certificate", priority = 11)
    public void testAddApplicationWithServerErrorWhenRetrievingCertByName() throws CertificateMgtException {

        ServiceProvider inputSP = new ServiceProvider();
        inputSP.setApplicationName(APPLICATION_NAME_1);
        inputSP.setCertificateContent(certificate.getCertificateContent());
        addApplicationConfigurations(inputSP);

        reset(applicationCertificateManagementService);
        doReturn(0).when(applicationCertificateManagementService).addCertificate(any(), anyString());
        doThrow(serverException).when(applicationCertificateManagementService).getCertificateByName(any(),
                anyString());
        try {
            appIdWithCert = applicationManagementService.createApplication(inputSP, SUPER_TENANT_DOMAIN_NAME,
                    REGISTRY_SYSTEM_USERNAME);
            Assert.fail("Successful creation of the application without an exception is considered as a failure");
        } catch (IdentityApplicationManagementException e) {
            Assert.assertTrue(e.getMessage().contains("Error while creating an application: "
                    + inputSP.getApplicationName() + " in tenantDomain: " + SUPER_TENANT_DOMAIN_NAME));
            for (Throwable cause = e.getCause(); cause != null; cause = cause.getCause()) {
                if (cause instanceof CertificateMgtServerException) {
                    return;
                }
            }
            Assert.fail("Expected cause of type CertificateMgtServerException was not found in the exception chain");
        }
    }

    @Test(groups = "certificate", priority = 12)
    public void testAddCertificateToExistingApplication() throws IdentityApplicationManagementException,
            CertificateMgtException {

        ServiceProvider inputSP = new ServiceProvider();
        inputSP.setApplicationName(APPLICATION_NAME_2);
        addApplicationConfigurations(inputSP);

        String appIdWithoutCert = applicationManagementService.createApplication(inputSP, SUPER_TENANT_DOMAIN_NAME,
                REGISTRY_SYSTEM_USERNAME);
        ServiceProvider appWithoutCert = applicationManagementService.getApplicationByResourceId(appIdWithoutCert,
                SUPER_TENANT_DOMAIN_NAME);

        reset(applicationCertificateManagementService);
        doReturn(2).when(applicationCertificateManagementService).addCertificate(any(), anyString());

        try {
            appWithoutCert.setCertificateContent(certificate.getCertificateContent());
            applicationManagementService.updateApplicationByResourceId(appIdWithoutCert, appWithoutCert,
                    SUPER_TENANT_DOMAIN_NAME, REGISTRY_SYSTEM_USERNAME);
        } catch (Exception e) {
            Assert.fail("Application update with certificate addition should be successful without any exceptions");
        }

        // Deleting added application.
        doNothing().when(applicationCertificateManagementService).deleteCertificate(anyInt(), anyString());
        applicationManagementService.deleteApplication(appWithoutCert.getApplicationName(),
                SUPER_TENANT_DOMAIN_NAME, REGISTRY_SYSTEM_USERNAME);
    }

    @Test(groups = "b2b-shared-apps", priority = 13)
    public void testGetAncestorAppIdsOfChildApp() throws Exception {

        createB2BTestApp();
        mockAncestorOrganizationRetrieval(L2_ORG_ID, L1_ORG_ID, ROOT_ORG_ID);

        Map<String, String> resolvedAncestorAppIds =
                applicationManagementService.getAncestorAppIds(l2AppId, L2_ORG_ID);

        Assert.assertNotNull(resolvedAncestorAppIds);
        Assert.assertEquals(resolvedAncestorAppIds.size(), 3);
        Assert.assertEquals(resolvedAncestorAppIds.get(L2_ORG_ID), l2AppId);
        Assert.assertEquals(resolvedAncestorAppIds.get(L1_ORG_ID), l1AppId);
        Assert.assertEquals(resolvedAncestorAppIds.get(ROOT_ORG_ID), rootAppId);
    }

    @Test(groups = "b2b-shared-apps", priority = 14, dependsOnMethods = "testGetAncestorAppIdsOfChildApp")
    public void testGetAncestorAppIdsOfParentApp() throws Exception {

        mockAncestorOrganizationRetrieval(L1_ORG_ID, ROOT_ORG_ID);

        Map<String, String> resolvedAncestorAppIds =
                applicationManagementService.getAncestorAppIds(l1AppId, L1_ORG_ID);

        Assert.assertNotNull(resolvedAncestorAppIds);
        Assert.assertEquals(resolvedAncestorAppIds.size(), 2);
        Assert.assertEquals(resolvedAncestorAppIds.get(L1_ORG_ID), l1AppId);
        Assert.assertEquals(resolvedAncestorAppIds.get(ROOT_ORG_ID), rootAppId);
    }

    @Test(groups = "b2b-shared-apps", priority = 15, dependsOnMethods = "testGetAncestorAppIdsOfChildApp")
    public void testGetAncestorAppIdsOfRootApp() throws Exception {

        when(organizationManager.resolveTenantDomain(ROOT_ORG_ID)).thenReturn(ROOT_TENANT_DOMAIN);

        Map<String, String> resolvedAncestorAppIds =
                applicationManagementService.getAncestorAppIds(rootAppId, ROOT_ORG_ID);

        Assert.assertNotNull(resolvedAncestorAppIds);
        Assert.assertEquals(resolvedAncestorAppIds.size(), 1);
        Assert.assertEquals(resolvedAncestorAppIds.get(ROOT_ORG_ID), rootAppId);
    }

    @Test(groups = "b2b-shared-apps", priority = 16, dependsOnMethods = "testGetAncestorAppIdsOfChildApp")
    public void testGetAncestorAppIdsOfInvalidApp() throws Exception {

        when(organizationManager.resolveTenantDomain(ROOT_ORG_ID)).thenReturn(ROOT_TENANT_DOMAIN);

        Map<String, String> resolvedAncestorAppIds =
                applicationManagementService.getAncestorAppIds("invalid-app-id", ROOT_ORG_ID);

        Assert.assertNotNull(resolvedAncestorAppIds);
        Assert.assertEquals(resolvedAncestorAppIds.size(), 0);
    }

    @Test(groups = "b2b-shared-apps", priority = 17, dependsOnMethods = "testGetAncestorAppIdsOfChildApp")
    public void testServerExceptionsWhileRetrievingAncestorAppIds() throws Exception {

        // Server exceptions while retrieving ancestor organization ids of level 2 organization.
        when(organizationManager.getAncestorOrganizationIds(L2_ORG_ID))
                .thenThrow(OrganizationManagementServerException.class);
        Assert.assertThrows(IdentityApplicationManagementException.class, () -> {
            applicationManagementService.getAncestorAppIds(l2AppId, L2_ORG_ID);
        });

        // Server exceptions while retrieving ancestor organization ids of level 1 organization.
        when(organizationManager.getAncestorOrganizationIds(L1_ORG_ID))
                .thenThrow(OrganizationManagementServerException.class);
        Assert.assertThrows(IdentityApplicationManagementException.class, () -> {
            applicationManagementService.getAncestorAppIds(l1AppId, L1_ORG_ID);
        });

        // Server exceptions while resolving tenant domain of root organization.
        when(organizationManager.resolveTenantDomain(ROOT_ORG_ID)).thenThrow(OrganizationManagementException.class);
        Assert.assertThrows(IdentityApplicationManagementException.class, () -> {
            applicationManagementService.getAncestorAppIds(rootAppId, ROOT_ORG_ID);
        });
    }

    @Test(groups = "b2b-shared-apps", priority = 18, dependsOnMethods = "testGetAncestorAppIdsOfChildApp")
    public void testGetSharedAppId() throws Exception {

        String l1SharedAppId = applicationManagementService.getSharedAppId(rootAppId, ROOT_ORG_ID, L1_ORG_ID);
        Assert.assertEquals(l1SharedAppId, l1AppId, "Incorrect shared app ID for level 1 organization.");

        String l2SharedAppId = applicationManagementService.getSharedAppId(rootAppId, ROOT_ORG_ID, L2_ORG_ID);
        Assert.assertEquals(l2SharedAppId, l2AppId, "Incorrect shared app ID for level 2 organization.");
    }

    @DataProvider(name = "testGetGroupsDataProvider")
    public Object[][] testGetGroupsDataProvider() {

        ExpressionNode validExpressionNode = new ExpressionNode();
        validExpressionNode.setOperation(FILTER_CO);
        validExpressionNode.setAttributeValue(NAME);
        validExpressionNode.setValue("test-value");
        ExpressionNode invalidExpressionNode1 = new ExpressionNode();
        invalidExpressionNode1.setAttributeValue("wrong-filter-attr");
        invalidExpressionNode1.setOperation(FILTER_CO);
        ExpressionNode invalidExpressionNode2 = new ExpressionNode();
        invalidExpressionNode2.setAttributeValue(NAME);
        invalidExpressionNode2.setOperation("wrong-filter-op");
        OperationNode operationNode = new OperationNode(FILTER_CO);
        Group group1 = new Group("test-group-id-1", "test-group-name-1");
        Group group2 = new Group("test-group-id-2", "test-group-name-2");

        return new Object[][] {
                {ROOT_TENANT_DOMAIN, DEFAULT_DOMAIN_NAME, null, new ArrayList<>(Arrays.asList(group1, group2)), null,
                        null},
                {ROOT_TENANT_DOMAIN, null, null, new ArrayList<>(Arrays.asList(group1, group2)), null, null},
                {ROOT_TENANT_DOMAIN, DEFAULT_DOMAIN_NAME, operationNode, null, null, INVALID_GROUP_FILTER.getCode()},
                {ROOT_TENANT_DOMAIN, DEFAULT_DOMAIN_NAME, invalidExpressionNode1, null, null,
                        INVALID_GROUP_FILTER.getCode()},
                {ROOT_TENANT_DOMAIN, DEFAULT_DOMAIN_NAME, invalidExpressionNode2, null, null,
                        INVALID_GROUP_FILTER.getCode()},
                {ROOT_TENANT_DOMAIN, DEFAULT_DOMAIN_NAME, validExpressionNode,
                        new ArrayList<>(Arrays.asList(group1, group2)), null, null},
                {ROOT_TENANT_DOMAIN, "wrong-domain-name", validExpressionNode, null, null,
                        INVALID_USER_STORE_DOMAIN.getCode()},
                {ROOT_TENANT_DOMAIN, DEFAULT_DOMAIN_NAME, null, null,
                        new org.wso2.carbon.user.core.UserStoreException(), ERROR_RETRIEVING_GROUP_LIST.getCode()}
        };
    }

    @Test(description = "Test the group listing functionality of the application management service.",
            dataProvider = "testGetGroupsDataProvider")
    public void testGetGroups(String tenantDomain, String domainName, Node filter, List<Group> userStoreGroupsResponse,
                              org.wso2.carbon.user.core.UserStoreException userStoreGroupsRequestException,
                              String expectedErrorCode) throws UserStoreException {

        AbstractUserStoreManager mockAbstractUserStoreManager = mock(AbstractUserStoreManager.class);
        RealmService mockRealmService = mock(RealmService.class);
        UserRealm mockUserRealmService = mock(UserRealm.class);
        ApplicationManagementServiceComponentHolder mockApplicationManagementServiceComponentHolder =
                mock(ApplicationManagementServiceComponentHolder.class);
        try (MockedStatic<ApplicationManagementServiceComponentHolder> applicationManagementServiceComponentHolder =
                     mockStatic(ApplicationManagementServiceComponentHolder.class)) {
            applicationManagementServiceComponentHolder.when(ApplicationManagementServiceComponentHolder::getInstance)
                    .thenReturn(mockApplicationManagementServiceComponentHolder);
            when(mockApplicationManagementServiceComponentHolder.getRealmService()).thenReturn(mockRealmService);
            when(mockRealmService.getTenantUserRealm(eq(SUPER_TENANT_ID))).thenReturn(mockUserRealmService);
            when(mockUserRealmService.getUserStoreManager()).thenReturn(mockAbstractUserStoreManager);
            when(mockAbstractUserStoreManager.getSecondaryUserStoreManager(eq(DEFAULT_DOMAIN_NAME))).thenReturn(
                    mockAbstractUserStoreManager);
            if (userStoreGroupsResponse != null) {
                when(mockAbstractUserStoreManager.listGroups(nullable(Condition.class), nullable(String.class),
                        anyInt(), anyInt(), nullable(String.class), nullable(String.class))).thenReturn(
                        userStoreGroupsResponse);
            } else if (userStoreGroupsRequestException != null) {
                when(mockAbstractUserStoreManager.listGroups(nullable(Condition.class), nullable(String.class),
                        anyInt(), anyInt(), nullable(String.class), nullable(String.class))).thenThrow(
                        userStoreGroupsRequestException);
            }
            try {
                List<GroupBasicInfo> groups = applicationManagementService.getGroups(tenantDomain, domainName, filter);
                for (int i = 0; i < groups.size(); i++) {
                    assertEquals(groups.get(i).getId(), userStoreGroupsResponse.get(i).getGroupID());
                    assertEquals(groups.get(i).getName(), userStoreGroupsResponse.get(i).getGroupName());
                }
            } catch (IdentityApplicationManagementException e) {
                assertEquals(e.getErrorCode(), expectedErrorCode);
            }
            if (userStoreGroupsResponse != null || userStoreGroupsRequestException != null) {
                ArgumentCaptor<Condition> conditionArgumentCaptor = ArgumentCaptor.forClass(Condition.class);
                ArgumentCaptor<String> userDomainArgumentCaptor = ArgumentCaptor.forClass(String.class);
                verify(mockAbstractUserStoreManager).listGroups(conditionArgumentCaptor.capture(),
                        userDomainArgumentCaptor.capture(),
                        eq(100), eq(0), nullable(String.class), nullable(String.class));
                if (filter == null) {
                    assertEquals(conditionArgumentCaptor.getValue().getOperation(), "SW");
                    assertEquals(((ExpressionCondition) conditionArgumentCaptor.getValue()).getAttributeName(),
                            GROUP_NAME_ATTRIBUTE);
                    assertEquals(((ExpressionCondition) conditionArgumentCaptor.getValue()).getAttributeValue(),
                            StringUtils.EMPTY);
                } else if (filter instanceof OperationNode) {
                    assertEquals(conditionArgumentCaptor.getValue().getOperation(),
                            conditionArgumentCaptor.getValue().getOperation());
                } else {
                    assertEquals(conditionArgumentCaptor.getValue().getOperation(), StringUtils.upperCase(FILTER_CO));
                    assertEquals(((ExpressionCondition) conditionArgumentCaptor.getValue()).getAttributeName(),
                            GROUP_NAME_ATTRIBUTE);
                    assertEquals(((ExpressionCondition) conditionArgumentCaptor.getValue()).getAttributeValue(),
                            ((ExpressionNode) filter).getValue());
                }
                assertEquals(userDomainArgumentCaptor.getValue(), DEFAULT_DOMAIN_NAME);
            }
        }
    }

    private void addApplicationConfigurations(ServiceProvider serviceProvider) {

        serviceProvider.setDescription("Created for testing");
        serviceProvider.setSaasApp(TRUE);

        // Inbound Authentication Configurations.
        setApplicationInboundAuthConfigs(serviceProvider, "auth key", "oauth2");

        // Inbound Provisioning Configurations.
        InboundProvisioningConfig provisioningConfig = new InboundProvisioningConfig();
        provisioningConfig.setProvisioningUserStore("UserStore");
        serviceProvider.setInboundProvisioningConfig(provisioningConfig);

        // OutBound Provisioning Configurations.
        IdentityProvider provisioningIdP = new IdentityProvider();
        provisioningIdP.setIdentityProviderName("Provisioning IdP");
        OutboundProvisioningConfig outboundProvisioningConfig = new OutboundProvisioningConfig();
        outboundProvisioningConfig.setProvisioningIdentityProviders(new IdentityProvider[]{provisioningIdP});
        ProvisioningConnectorConfig provisioningConnectorConfig = new ProvisioningConnectorConfig();
        provisioningConnectorConfig.setName("Provisioning connector");
        provisioningIdP.setDefaultProvisioningConnectorConfig(provisioningConnectorConfig);
        serviceProvider.setOutboundProvisioningConfig(outboundProvisioningConfig);

        // Local And OutBound Authentication Configuration.
        LocalAndOutboundAuthenticationConfig authenticationConfig = new LocalAndOutboundAuthenticationConfig();
        AuthenticationStep authenticationStep = new AuthenticationStep();
        IdentityProvider identityProvider = new IdentityProvider();
        identityProvider.setIdentityProviderName(IDP_NAME_1);
        FederatedAuthenticatorConfig federatedAuthenticatorConfig = new FederatedAuthenticatorConfig();
        federatedAuthenticatorConfig.setName("Federated authenticator");
        federatedAuthenticatorConfig.setDefinedByType(DefinedByType.SYSTEM);
        identityProvider.setFederatedAuthenticatorConfigs(new FederatedAuthenticatorConfig[]
                {federatedAuthenticatorConfig});
        authenticationStep.setFederatedIdentityProviders(new IdentityProvider[]{identityProvider});
        LocalAuthenticatorConfig localAuthenticatorConfig = new LocalAuthenticatorConfig();
        localAuthenticatorConfig.setDefinedByType(DefinedByType.SYSTEM);
        localAuthenticatorConfig.setName("Local authenticator");
        authenticationStep.setLocalAuthenticatorConfigs(new LocalAuthenticatorConfig[]{localAuthenticatorConfig});
        authenticationConfig.setAuthenticationSteps(new AuthenticationStep[]{authenticationStep});
        serviceProvider.setLocalAndOutBoundAuthenticationConfig(authenticationConfig);

        // Request Path Authenticator Configuration.
        RequestPathAuthenticatorConfig requestPathAuthenticatorConfig = new RequestPathAuthenticatorConfig();
        requestPathAuthenticatorConfig.setName("Request path authenticator");
        requestPathAuthenticatorConfig.setDefinedByType(DefinedByType.SYSTEM);
        serviceProvider.setRequestPathAuthenticatorConfigs(new RequestPathAuthenticatorConfig[]{
                requestPathAuthenticatorConfig});

        // Claim Configurations.
        ClaimConfig claimConfig = new ClaimConfig();
        claimConfig.setRoleClaimURI("Role claim uri");
        claimConfig.setSpClaimDialects(new String[]{"SP claim dialect"});
        ClaimMapping claimMapping = new ClaimMapping();
        Claim localClaim = new Claim();
        localClaim.setClaimUri("Local claim uri");
        Claim remoteClaim = new Claim();
        remoteClaim.setClaimUri("Remote claim uri");
        claimMapping.setLocalClaim(localClaim);
        claimMapping.setRemoteClaim(remoteClaim);
        claimConfig.setClaimMappings(new ClaimMapping[]{claimMapping});
        serviceProvider.setClaimConfig(claimConfig);

        // Permission Role Configurations.
        PermissionsAndRoleConfig permissionsAndRoleConfig = new PermissionsAndRoleConfig();
        RoleMapping roleMapping = new RoleMapping();
        LocalRole localRole = new LocalRole("Local role");
        roleMapping.setLocalRole(localRole);
        roleMapping.setRemoteRole("Remote role");
        RoleMapping[] roleMappings = new RoleMapping[]{roleMapping};
        permissionsAndRoleConfig.setRoleMappings(roleMappings);
    }

    private void setApplicationInboundAuthConfigs(ServiceProvider serviceProvider, String authKey, String authType) {

        InboundAuthenticationConfig inboundAuthenticationConfig = new InboundAuthenticationConfig();
        InboundAuthenticationRequestConfig authRequestConfig = new InboundAuthenticationRequestConfig();
        authRequestConfig.setInboundAuthKey(authKey);
        authRequestConfig.setInboundAuthType(authType);
        InboundAuthenticationRequestConfig[] authRequests = new InboundAuthenticationRequestConfig[]
                {authRequestConfig};
        inboundAuthenticationConfig.setInboundAuthenticationRequestConfigs(authRequests);
        serviceProvider.setInboundAuthenticationConfig(inboundAuthenticationConfig);
    }
    
    private InboundProtocolsDTO setInboundProtocol() {

        InboundProtocolsDTO inboundProtocolsDTO = new InboundProtocolsDTO();
        inboundProtocolsDTO.addProtocolConfiguration(() -> "oauth2");
        return inboundProtocolsDTO;
    }

    private void setupConfiguration() throws UserStoreException, RegistryException {

        String carbonHome = Paths.get(System.getProperty("user.dir"), "target", "test-classes", "repository").
                toString();
        System.setProperty(CarbonBaseConstants.CARBON_HOME, carbonHome);
        System.setProperty(CarbonBaseConstants.CARBON_CONFIG_DIR_PATH, Paths.get(carbonHome, "conf").toString());

        PrivilegedCarbonContext.startTenantFlow();
        PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain(SUPER_TENANT_DOMAIN_NAME);
        PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantId(SUPER_TENANT_ID);
        PrivilegedCarbonContext.getThreadLocalCarbonContext();

        // Configure RealmService.
        PrivilegedCarbonContext.startTenantFlow();
        PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantId(SUPER_TENANT_ID);
        PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain(SUPER_TENANT_DOMAIN_NAME);
        InMemoryRealmService testSessionRealmService = new InMemoryRealmService(SUPER_TENANT_ID);
        UserStoreManager userStoreManager = testSessionRealmService.getTenantUserRealm(SUPER_TENANT_ID)
                .getUserStoreManager();
        ((MockUserStoreManager) userStoreManager)
                .addSecondaryUserStoreManager("PRIMARY", (MockUserStoreManager) userStoreManager);
        IdentityTenantUtil.setRealmService(testSessionRealmService);
        RegistryDataHolder.getInstance().setRealmService(testSessionRealmService);
        OSGiDataHolder.getInstance().setUserRealmService(testSessionRealmService);
        IdentityCoreServiceDataHolder.getInstance().setRealmService(testSessionRealmService);
        ApplicationManagementServiceComponentHolder holder = ApplicationManagementServiceComponentHolder.getInstance();
        setInstanceValue(testSessionRealmService, RealmService.class, ApplicationManagementServiceComponentHolder.class,
                holder);
        setInstanceValue(new RegistryBasedApplicationPermissionProvider(), ApplicationPermissionProvider.class,
                ApplicationManagementServiceComponentHolder.class, holder);

       // Configure Registry Service.
        RegistryService mockRegistryService = mock(RegistryService.class);
        UserRegistry mockRegistry = mock(UserRegistry.class);
        when(mockRegistryService.getGovernanceUserRegistry(anyString(), anyInt())).thenReturn(mockRegistry);
        OSGiDataHolder.getInstance().setRegistryService(mockRegistryService);
        CarbonCoreDataHolder.getInstance().setRegistryService(mockRegistryService);
        PrivilegedCarbonContext.getThreadLocalCarbonContext()
                .setRegistry(RegistryType.USER_GOVERNANCE, mockRegistryService.getRegistry());
        when(mockRegistry.resourceExists(anyString())).thenReturn(FALSE);
        Collection mockPermissionNode = mock(Collection.class);
        when(mockRegistry.newCollection()).thenReturn(mockPermissionNode);
        when(mockRegistry.get(anyString())).thenReturn(mockPermissionNode);
        when(CarbonContext.getThreadLocalCarbonContext().getRegistry(
                RegistryType.USER_GOVERNANCE)).thenReturn(mockRegistry);
        when(mockRegistry.resourceExists(anyString())).thenReturn(FALSE);
    }

    private void setInstanceValue(Object value, Class valueType, Class clazz, Object instance) {

        for (Field field1 : clazz.getDeclaredFields()) {
            if (field1.getType().isAssignableFrom(valueType)) {
                field1.setAccessible(true);

                if (java.lang.reflect.Modifier.isStatic(field1.getModifiers())) {
                    setInternalState(clazz, field1.getName(), value);
                } else if (instance != null) {
                    setInternalState(instance, field1.getName(), value);
                }
            }
        }
    }

    private static void setInternalState(Object target, String field, Object value) {

        Class targetClass = target.getClass();

        try {
            Field declaredField = targetClass.getDeclaredField(field);
            declaredField.setAccessible(true);
            declaredField.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException("Unable to set internal state on a private field.", e);
        }
    }

    private static void setInternalState(Class c, String field, Object value) {

        try {
            Field declaredField = c.getDeclaredField(field);
            declaredField.setAccessible(true);
            declaredField.set(null, value);
        } catch (Exception e) {
            throw new RuntimeException("Unable to set internal state on a private field.", e);
        }
    }

    private void mockAncestorOrganizationRetrieval(String orgId, String ...ancestorOrgIds)
            throws OrganizationManagementException {

        List<String> ancestorOrganizationIds = new ArrayList<>();
        ancestorOrganizationIds.add(orgId);
        if (ancestorOrgIds != null && ancestorOrgIds.length > 0) {
            ancestorOrganizationIds.addAll(Arrays.asList(ancestorOrgIds));
        }

        when(organizationManager.getAncestorOrganizationIds(orgId)).thenReturn(ancestorOrganizationIds);
    }

    private void createB2BTestApp() throws Exception {

        ServiceProvider serviceProvider = new ServiceProvider();
        serviceProvider.setApplicationName(B2B_APPLICATION_NAME);
        addApplicationConfigurations(serviceProvider);

        // Since the app is the main app, it is not a fragment app.
        ServiceProviderProperty isFragmentAppProperty = new ServiceProviderProperty();
        isFragmentAppProperty.setName(IS_FRAGMENT_APP);
        isFragmentAppProperty.setValue("false");
        serviceProvider.setSpProperties(new ServiceProviderProperty[]{isFragmentAppProperty});

        try (MockedStatic<IdentityTenantUtil> mockedIdentityTenantUtil = Mockito.mockStatic(IdentityTenantUtil.class)) {
            mockedIdentityTenantUtil.when(() -> IdentityTenantUtil.getTenantId(ROOT_TENANT_DOMAIN))
                    .thenReturn(ROOT_TENANT_ID);
            mockedIdentityTenantUtil.when(() -> IdentityTenantUtil.getTenantId(L1_ORG_ID)).thenReturn(L1_TENANT_ID);
            mockedIdentityTenantUtil.when(() -> IdentityTenantUtil.getTenantId(L2_ORG_ID)).thenReturn(L2_TENANT_ID);
            mockedIdentityTenantUtil.when(IdentityTenantUtil::getRealmService)
                    .thenAnswer(InvocationOnMock::callRealMethod);

            rootAppId = applicationManagementService.createApplication(serviceProvider, ROOT_TENANT_DOMAIN,
                    REGISTRY_SYSTEM_USERNAME);
            l1AppId = shareApplication(rootAppId, ROOT_ORG_ID, L1_ORG_ID);
            l2AppId = shareApplication(rootAppId, ROOT_ORG_ID, L2_ORG_ID);
        }
    }

    private String shareApplication(String mainAppId, String ownerOrgId, String sharedOrgId)
            throws Exception {

        ServiceProvider serviceProvider = new ServiceProvider();
        serviceProvider.setApplicationName(B2B_APPLICATION_NAME);
        addApplicationConfigurations(serviceProvider);

        // Since the app is shared, it is a fragment app.
        ServiceProviderProperty isFragmentAppProperty = new ServiceProviderProperty();
        isFragmentAppProperty.setName(IS_FRAGMENT_APP);
        isFragmentAppProperty.setValue("true");
        serviceProvider.setSpProperties(new ServiceProviderProperty[]{isFragmentAppProperty});

        String sharedAppId = applicationManagementService.createApplication(serviceProvider, sharedOrgId,
                REGISTRY_SYSTEM_USERNAME);

        String query = "INSERT INTO SP_SHARED_APP (MAIN_APP_ID, OWNER_ORG_ID, SHARED_APP_ID, SHARED_ORG_ID) " +
                "VALUES (?, ?, ?, ?)";
        try (Connection connection = IdentityDatabaseUtil.getDBConnection(false)) {
            try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                preparedStatement.setString(1, mainAppId);
                preparedStatement.setString(2, ownerOrgId);
                preparedStatement.setString(3, sharedAppId);
                preparedStatement.setString(4, sharedOrgId);
                preparedStatement.executeUpdate();
            }
        }

        return sharedAppId;
    }
}
