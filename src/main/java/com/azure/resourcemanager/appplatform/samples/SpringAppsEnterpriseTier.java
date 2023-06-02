package com.azure.resourcemanager.appplatform.samples;

import com.azure.core.credential.TokenCredential;
import com.azure.core.http.policy.HttpLogDetailLevel;
import com.azure.core.http.rest.PagedIterable;
import com.azure.core.management.AzureEnvironment;
import com.azure.core.management.Region;
import com.azure.core.management.exception.ManagementException;
import com.azure.core.management.profile.AzureProfile;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.resourcemanager.AzureResourceManager;
import com.azure.resourcemanager.appplatform.AppPlatformManager;
import com.azure.resourcemanager.appplatform.fluent.AppPlatformManagementClient;
import com.azure.resourcemanager.appplatform.fluent.models.BuildServiceAgentPoolResourceInner;
import com.azure.resourcemanager.appplatform.fluent.models.BuildServiceInner;
import com.azure.resourcemanager.appplatform.fluent.models.BuilderResourceInner;
import com.azure.resourcemanager.appplatform.models.SkuName;
import com.azure.resourcemanager.appplatform.models.SpringService;
import com.azure.resourcemanager.resources.models.ResourceGroup;
import com.azure.resourcemanager.samples.Utils;

public class SpringAppsEnterpriseTier {

	private static final String TENANT_ID = "";
	private static final String SUBSCRIPTION_ID = "";

	private static final Region DEFAULT_REGION = Region.US_EAST;

	private static AzureResourceManager azureResourceManager;

	/**
	 * Main entry point.
	 *
	 * @param args the parameters
	 */
	public static void main(String[] args) {		
		try {
			// =============================================================
			// Authenticate

			final AzureProfile profile = new AzureProfile(TENANT_ID, SUBSCRIPTION_ID, AzureEnvironment.AZURE);
			final TokenCredential credential = new DefaultAzureCredentialBuilder()
					.authorityHost(profile.getEnvironment().getActiveDirectoryEndpoint()).build();

			azureResourceManager = AzureResourceManager.configure().withLogLevel(HttpLogDetailLevel.BASIC)
					.authenticate(credential, profile).withSubscription(SUBSCRIPTION_ID);

			// Print selected subscription
			System.out.printf("Selected subscription %s%n", azureResourceManager.subscriptionId());

			String rgName = Utils.randomResourceName(azureResourceManager, "rg-", 24);
			String serviceName = Utils.randomResourceName(azureResourceManager, "demo-svc-", 24);

			// Get or create resource group
			ResourceGroup rg = getOrCreateResourceGroup(rgName);
			// Get or create spring apps service
			SpringService service = getOrCreateSpringAppsService(rg, serviceName);
						
			AppPlatformManager manager = AppPlatformManager.configure().withLogLevel(HttpLogDetailLevel.BASIC).authenticate(credential, profile);
			AppPlatformManagementClient client = manager.serviceClient();
		
			PagedIterable<BuildServiceInner> buildServices = client.getBuildServices().listBuildServices(rg.name(), service.name());
			BuildServiceInner buildService = buildServices.iterator().next();
			if(buildService != null) {
				System.out.printf("Get default build service %s%n", buildService.name());
			} else {
				throw new Exception("Cannot find default build service");
			}

			PagedIterable<BuilderResourceInner> builders = client.getBuildServiceBuilders().list(rg.name(), service.name(), "default");
			BuilderResourceInner builder = builders.iterator().next();
			if(builder != null) {
				System.out.printf("Get default builder %s%n", builder.name());
			} else {
				throw new Exception("Cannot find default builder");
			}

			PagedIterable<BuildServiceAgentPoolResourceInner> agentPools = client.getBuildServiceAgentPools().list(rg.name(), service.name(), "default");
			BuildServiceAgentPoolResourceInner agentPool = agentPools.iterator().next();
			if(agentPool != null) {
				System.out.printf("Get default agent pool %s%n", agentPool.name());
			} else {
				throw new Exception("Cannot find default agent pool");
			}

		} catch (Exception e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * Return resource group by name or create resource group if it does exist.
	 * 
	 * @param name
	 * @return
	 */
	private static ResourceGroup getOrCreateResourceGroup(String name) {
		ResourceGroup rg = null;
		try {
			rg = azureResourceManager.resourceGroups().getByName(name);
		} catch (ManagementException e) {
			System.out.printf("Creating resource group %s", name);

			rg = azureResourceManager.resourceGroups().define(name).withRegion(DEFAULT_REGION).create();

			System.out.printf("Created resource group %s", name);
		}
		return rg;
	}

	/**
	 * Return Azure Spring Apps service or create one if it doesn't exist.
	 * 
	 * @param rg
	 * @param name
	 * @return
	 */
	private static SpringService getOrCreateSpringAppsService(ResourceGroup rg, String name) {
		SpringService service = null;
		try {
			service = azureResourceManager.springServices().getByResourceGroup(rg.name(), name);
			System.out.printf("Got Azure Spring Apps service %s in resource group %s ...%n", name, rg.name());
		} catch (ManagementException e) {
			System.out.printf("Creating Azure Spring Apps service %s in resource group %s ...%n", name, rg.name());

			service = azureResourceManager.springServices().define(name).withRegion(DEFAULT_REGION)
					.withExistingResourceGroup(rg.name()).withSku(SkuName.E0).create();

			System.out.printf("Created Azure Spring Apps service %s%n", service.name());
		}
		return service;
	}
	
}
