package io.devjoy.gitea.repository.k8s;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.stream.Stream;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.devjoy.gitea.domain.TokenService;
import io.devjoy.gitea.domain.service.AuthenticationService;
import io.devjoy.gitea.domain.service.GiteaApiService;
import io.devjoy.gitea.domain.service.GiteaPodExecService;
import io.devjoy.gitea.domain.service.ServiceException;
import io.devjoy.gitea.domain.service.UserService;
import io.devjoy.gitea.k8s.GiteaStatusUpdater;
import io.devjoy.gitea.repository.k8s.model.SecretReferenceSpec;
import io.devjoy.gitea.repository.service.RepositoryService;
import io.devjoy.gitea.util.PasswordService;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.api.model.SecretList;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.dsl.internal.HasMetadataOperation;
import io.fabric8.openshift.client.OpenShiftClient;

@ExtendWith(MockitoExtension.class)
class GiteaRepositoryReconcilerTest {
	//@Rule
	//public KubernetesServer server = new KubernetesServer();
	@Mock(answer = Answers.RETURNS_DEEP_STUBS)
	OpenShiftClient ocpClient;
	@Mock
	TokenService tokenService;
	@Mock
	RepositoryService repositoryService;
	@Mock
	PasswordService passwordService;
	@Mock
	GiteaPodExecService execService;
	@Mock
	UserService userService;
	@Mock
	AuthenticationService authService;
	@Mock
	GiteaApiService giteaApiService;
	@Mock
	GiteaStatusUpdater updater;
	@InjectMocks
	GiteaRepositoryReconciler reconciler;
	@Mock
	HasMetadataOperation<Secret, SecretList, Resource<Secret>> secretOp;
	@Mock
	HasMetadataOperation<Secret, SecretList, Resource<Secret>> secretOpNs1;
	@Mock
	HasMetadataOperation<Secret, SecretList, Resource<Secret>> secretOpNs2;
	@Mock
	HasMetadataOperation<Secret, SecretList, Resource<Secret>> secretOpNs3;
	@Mock
	Resource<Secret> ns1Secret1Op;
	@Mock
	Resource<Secret> ns2Secret1Op;
	@Mock
	Resource<Secret> ns3Secret3Op;
	@Mock
	Resource<Secret> ns3Secret4Op;
	@Mock
	Resource<Secret> ns3Secret5Op;
	
	@ParameterizedTest
	@MethodSource("provideSecretReferenceSpecs")
	void assureWebhookSecretsExist(Stream<SecretReferenceSpec> secretSpecs) {
		Secret ns1Secret1 = new SecretBuilder().withNewMetadata().withNamespace("ns1").withName("secret1").endMetadata().withData(new HashMap<>()).build();
		Secret ns2Secret1 = new SecretBuilder().withNewMetadata().withNamespace("ns2").withName("secret1").endMetadata().withData(new HashMap<>()).build();
		Secret ns3Secret3 = new SecretBuilder().withNewMetadata().withNamespace("ns3").withName("secret3").endMetadata().withData(new HashMap<>()).build();
		Secret ns3Secret4 = new SecretBuilder().withNewMetadata().withNamespace("ns3").withName("secret4").endMetadata().withData(new HashMap<>()).build();
		Secret ns3Secret5 = new SecretBuilder().withNewMetadata().withNamespace("ns3").withName("secret5").endMetadata().withData(new HashMap<>()).build();
		
		when(ocpClient.secrets()).thenReturn(secretOp);
		when(secretOp.inNamespace("ns1")).thenReturn(secretOpNs1);
		when(secretOp.inNamespace("ns2")).thenReturn(secretOpNs2);
		when(secretOp.inNamespace("ns3")).thenReturn(secretOpNs3);
		
		when(secretOpNs1.withName("secret1")).thenReturn(ns1Secret1Op);
		when(secretOpNs2.withName("secret1")).thenReturn(ns2Secret1Op);
		when(secretOpNs3.withName("secret3")).thenReturn(ns3Secret3Op);
		when(secretOpNs3.withName("secret4")).thenReturn(ns3Secret4Op);
		when(secretOpNs3.withName("secret5")).thenReturn(ns3Secret5Op);
		
		when(ns1Secret1Op.get()).thenReturn(ns1Secret1);
		when(ns2Secret1Op.get()).thenReturn(ns2Secret1);
		when(ns3Secret3Op.get()).thenReturn(ns3Secret3);
		when(ns3Secret4Op.get()).thenReturn(ns3Secret4);
		when(ns3Secret5Op.get()).thenReturn(ns3Secret5);
		reconciler.assureWebhookSecretsExist(secretSpecs);
		assertTrue("Secret must contain k1 but was " + ns1Secret1, ns1Secret1.getData().containsKey("k1"));
		assertTrue("Secret must contain k1 but was " + ns2Secret1, ns2Secret1.getData().containsKey("k1"));
		assertTrue("Secret must contain k2 but was " + ns2Secret1, ns2Secret1.getData().containsKey("k2"));
		assertTrue("Secret must contain k1 but was " + ns3Secret3, ns3Secret3.getData().containsKey("k1"));
		assertTrue("Secret must contain k1 but was " + ns3Secret4, ns3Secret4.getData().containsKey("k1"));
		assertTrue("Secret must contain k1 but was " + ns3Secret5, ns3Secret5.getData().containsKey("k1"));
	}
	
	@ParameterizedTest
	@MethodSource("provideSecretReferenceSpecs")
	void assureWebhookSecretsExistSecretNotFound(Stream<SecretReferenceSpec> secretSpecs) {
		Secret ns2Secret1 = new SecretBuilder().withNewMetadata().withNamespace("ns2").withName("secret1").endMetadata().withData(new HashMap<>()).build();
		
		when(ocpClient.secrets()).thenReturn(secretOp);
		when(secretOp.inNamespace("ns1")).thenReturn(secretOpNs1);
		when(secretOp.inNamespace("ns2")).thenReturn(secretOpNs2);
		//when(secretOp.inNamespace("ns3")).thenReturn(secretOpNs3);
		
		when(secretOpNs1.withName("secret1")).thenReturn(ns1Secret1Op);
		when(secretOpNs2.withName("secret1")).thenReturn(ns2Secret1Op);
		//when(secretOpNs3.withName("secret3")).thenReturn(ns3Secret3Op);
		//when(secretOpNs3.withName("secret4")).thenReturn(ns3Secret4Op);
		//when(secretOpNs3.withName("secret5")).thenReturn(ns3Secret5Op);
		
		when(ns1Secret1Op.get()).thenReturn(null);
		when(ns2Secret1Op.get()).thenReturn(ns2Secret1);
		//when(ns3Secret3Op.get()).thenReturn(ns3Secret3);
		//when(ns3Secret4Op.get()).thenReturn(ns3Secret4);
		//when(ns3Secret5Op.get()).thenReturn(ns3Secret5);
		try {
			reconciler.assureWebhookSecretsExist(secretSpecs);
			fail("Expected ServiceException because secret1 in ns1 not found");
		} catch (ServiceException e) {
		
		}
	}

	
	private static Stream<Stream<SecretReferenceSpec>> provideSecretReferenceSpecs() {
	    return Stream.of(
	    		Stream.of(
	    		SecretReferenceSpec.builder().withNamespace("ns1").withName("secret1").withKey("k1").build(),
	    		SecretReferenceSpec.builder().withNamespace("ns2").withName("secret1").withKey("k1").build(),
	    		SecretReferenceSpec.builder().withNamespace("ns2").withName("secret1").withKey("k2").build(),
	    		SecretReferenceSpec.builder().withNamespace("ns3").withName("secret3").withKey("k1").build(),
	    		SecretReferenceSpec.builder().withNamespace("ns3").withName("secret4").withKey("k1").build(),
	    		SecretReferenceSpec.builder().withNamespace("ns3").withName("secret5").withKey("k1").build())
	    );
	}
}
