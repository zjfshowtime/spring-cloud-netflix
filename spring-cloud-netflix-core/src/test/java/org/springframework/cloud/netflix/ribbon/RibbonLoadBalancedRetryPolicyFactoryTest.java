package org.springframework.cloud.netflix.ribbon;

import com.netflix.client.DefaultLoadBalancerRetryHandler;
import com.netflix.client.config.CommonClientConfigKey;
import com.netflix.client.config.IClientConfig;
import com.netflix.loadbalancer.BaseLoadBalancer;
import com.netflix.loadbalancer.LoadBalancerStats;
import com.netflix.loadbalancer.Server;
import com.netflix.loadbalancer.ServerStats;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancedRetryContext;
import org.springframework.cloud.client.loadbalancer.LoadBalancedRetryPolicy;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.cloud.netflix.ribbon.RibbonLoadBalancerClient.RibbonServer;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author Ryan Baxter
 */
public class RibbonLoadBalancedRetryPolicyFactoryTest {

    @Mock
    private SpringClientFactory clientFactory;

    @Mock
    private BaseLoadBalancer loadBalancer;

    @Mock
    private LoadBalancerStats loadBalancerStats;

    @Mock
    private ServerStats serverStats;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        given(this.clientFactory.getLoadBalancerContext(anyString())).willReturn(
                new RibbonLoadBalancerContext(this.loadBalancer));
        given(this.clientFactory.getInstance(anyString(), eq(ServerIntrospector.class)))
                .willReturn(new DefaultServerIntrospector() {
                    @Override
                    public Map<String, String> getMetadata(Server server) {
                        return Collections.singletonMap("mykey", "myvalue");
                    }
                });

    }

    @After
    public void tearDown() throws Exception {}

    @Test
    public void testGetRetryPolicyNoRetry() throws Exception {
        int sameServer = 0;
        int nextServer = 0;
        boolean retryOnAllOps = false;
        RibbonServer server = getRibbonServer();
        IClientConfig config = mock(IClientConfig.class);
        doReturn(sameServer).when(config).get(eq(CommonClientConfigKey.MaxAutoRetries), anyInt());
        doReturn(sameServer).when(config).getPropertyAsInteger(eq(CommonClientConfigKey.MaxAutoRetries), anyInt());
        doReturn(nextServer).when(config).get(eq(CommonClientConfigKey.MaxAutoRetriesNextServer), anyInt());
        doReturn(nextServer).when(config).getPropertyAsInteger(eq(CommonClientConfigKey.MaxAutoRetriesNextServer), anyInt());
        doReturn(retryOnAllOps).when(config).get(eq(CommonClientConfigKey.OkToRetryOnAllOperations), anyBoolean());
        doReturn(retryOnAllOps).when(config).getPropertyAsBoolean(eq(CommonClientConfigKey.OkToRetryOnAllOperations), anyBoolean());
        doReturn(server.getServiceId()).when(config).getClientName();
        doReturn(config).when(clientFactory).getClientConfig(eq(server.getServiceId()));
        clientFactory.getLoadBalancerContext(server.getServiceId()).setRetryHandler(new DefaultLoadBalancerRetryHandler(config));
        RibbonLoadBalancerClient client = getRibbonLoadBalancerClient(server);
        RibbonLoadBalancedRetryPolicyFactory factory = new RibbonLoadBalancedRetryPolicyFactory(clientFactory);
        LoadBalancedRetryPolicy policy = factory.create(server.getServiceId(), client);
        HttpRequest request = mock(HttpRequest.class);
        doReturn(HttpMethod.GET).when(request).getMethod();
        LoadBalancedRetryContext context = new LoadBalancedRetryContext(null, request);
        assertThat(policy.canRetryNextServer(context), is(true));
        assertThat(policy.canRetrySameServer(context), is(false));
    }

    @Test
    public void testGetRetryPolicyNotGet() throws Exception {
        int sameServer = 3;
        int nextServer = 3;
        boolean retryOnAllOps = false;
        RibbonServer server = getRibbonServer();
        IClientConfig config = mock(IClientConfig.class);
        doReturn(sameServer).when(config).get(eq(CommonClientConfigKey.MaxAutoRetries), anyInt());
        doReturn(sameServer).when(config).getPropertyAsInteger(eq(CommonClientConfigKey.MaxAutoRetries), anyInt());
        doReturn(nextServer).when(config).get(eq(CommonClientConfigKey.MaxAutoRetriesNextServer), anyInt());
        doReturn(nextServer).when(config).getPropertyAsInteger(eq(CommonClientConfigKey.MaxAutoRetriesNextServer), anyInt());
        doReturn(retryOnAllOps).when(config).get(eq(CommonClientConfigKey.OkToRetryOnAllOperations), anyBoolean());
        doReturn(retryOnAllOps).when(config).getPropertyAsBoolean(eq(CommonClientConfigKey.OkToRetryOnAllOperations), anyBoolean());
        doReturn(server.getServiceId()).when(config).getClientName();
        doReturn(config).when(clientFactory).getClientConfig(eq(server.getServiceId()));
        clientFactory.getLoadBalancerContext(server.getServiceId()).setRetryHandler(new DefaultLoadBalancerRetryHandler(config));
        RibbonLoadBalancerClient client = getRibbonLoadBalancerClient(server);
        RibbonLoadBalancedRetryPolicyFactory factory = new RibbonLoadBalancedRetryPolicyFactory(clientFactory);
        LoadBalancedRetryPolicy policy = factory.create(server.getServiceId(), client);
        HttpRequest request = mock(HttpRequest.class);
        doReturn(HttpMethod.POST).when(request).getMethod();
        LoadBalancedRetryContext context = new LoadBalancedRetryContext(null, request);
        assertThat(policy.canRetryNextServer(context), is(false));
        assertThat(policy.canRetrySameServer(context), is(false));
    }

    @Test
    public void testGetRetryPolicyRetryOnNonGet() throws Exception {
        int sameServer = 3;
        int nextServer = 3;
        boolean retryOnAllOps = true;
        RibbonServer server = getRibbonServer();
        IClientConfig config = mock(IClientConfig.class);
        doReturn(sameServer).when(config).get(eq(CommonClientConfigKey.MaxAutoRetries), anyInt());
        doReturn(sameServer).when(config).getPropertyAsInteger(eq(CommonClientConfigKey.MaxAutoRetries), anyInt());
        doReturn(nextServer).when(config).get(eq(CommonClientConfigKey.MaxAutoRetriesNextServer), anyInt());
        doReturn(nextServer).when(config).getPropertyAsInteger(eq(CommonClientConfigKey.MaxAutoRetriesNextServer), anyInt());
        doReturn(retryOnAllOps).when(config).get(eq(CommonClientConfigKey.OkToRetryOnAllOperations), anyBoolean());
        doReturn(retryOnAllOps).when(config).getPropertyAsBoolean(eq(CommonClientConfigKey.OkToRetryOnAllOperations), anyBoolean());
        doReturn(server.getServiceId()).when(config).getClientName();
        doReturn(config).when(clientFactory).getClientConfig(eq(server.getServiceId()));
        clientFactory.getLoadBalancerContext(server.getServiceId()).initWithNiwsConfig(config);
        RibbonLoadBalancerClient client = getRibbonLoadBalancerClient(server);
        RibbonLoadBalancedRetryPolicyFactory factory = new RibbonLoadBalancedRetryPolicyFactory(clientFactory);
        LoadBalancedRetryPolicy policy = factory.create(server.getServiceId(), client);
        HttpRequest request = mock(HttpRequest.class);
        doReturn(HttpMethod.POST).when(request).getMethod();
        LoadBalancedRetryContext context = new LoadBalancedRetryContext(null, request);
        assertThat(policy.canRetryNextServer(context), is(true));
        assertThat(policy.canRetrySameServer(context), is(true));
    }

    @Test
    public void testGetRetryPolicyRetryCount() throws Exception {
        int sameServer = 3;
        int nextServer = 3;
        RibbonServer server = getRibbonServer();
        IClientConfig config = mock(IClientConfig.class);
        doReturn(sameServer).when(config).get(eq(CommonClientConfigKey.MaxAutoRetries), anyInt());
        doReturn(nextServer).when(config).get(eq(CommonClientConfigKey.MaxAutoRetriesNextServer), anyInt());
        doReturn(false).when(config).get(eq(CommonClientConfigKey.OkToRetryOnAllOperations), eq(false));
        doReturn(config).when(clientFactory).getClientConfig(eq(server.getServiceId()));
        clientFactory.getLoadBalancerContext(server.getServiceId()).setRetryHandler(new DefaultLoadBalancerRetryHandler(config));
        RibbonLoadBalancerClient client = getRibbonLoadBalancerClient(server);
        RibbonLoadBalancedRetryPolicyFactory factory = new RibbonLoadBalancedRetryPolicyFactory(clientFactory);
        LoadBalancedRetryPolicy policy = factory.create(server.getServiceId(), client);
        HttpRequest request = mock(HttpRequest.class);
        doReturn(HttpMethod.GET).when(request).getMethod();
        LoadBalancedRetryContext context = spy(new LoadBalancedRetryContext(null, request));
        //Loop through as if we are retrying a request until we exhaust the number of retries
        //outer loop is for next server retries
        //inner loop is for same server retries
        for(int i = 0; i < nextServer + 1; i++) {
            //iterate once time beyond the same server retry limit to cause us to reset
            //the same sever counter and increment the next server counter
            for(int j = 0; j < sameServer + 1; j++) {
                if(j < 3) {
                    assertThat(policy.canRetrySameServer(context), is(true));
                } else {
                    assertThat(policy.canRetrySameServer(context), is(false));
                }
                policy.registerThrowable(context, new IOException());
            }
            if(i < 3) {
                assertThat(policy.canRetryNextServer(context), is(true));
            } else {
                assertThat(policy.canRetryNextServer(context), is(false));
            }
        }
        assertThat(context.isExhaustedOnly(), is(true));
        verify(context, times(4)).setServiceInstance(any(ServiceInstance.class));
    }

    protected RibbonLoadBalancerClient getRibbonLoadBalancerClient(
            RibbonServer ribbonServer) {
        given(this.loadBalancer.getName()).willReturn(ribbonServer.getServiceId());
        given(this.loadBalancer.chooseServer(anyObject())).willReturn(
                ribbonServer.getServer());
        given(this.loadBalancer.getLoadBalancerStats())
                .willReturn(this.loadBalancerStats);
        given(this.loadBalancerStats.getSingleServerStat(ribbonServer.getServer()))
                .willReturn(this.serverStats);
        given(this.clientFactory.getLoadBalancer(this.loadBalancer.getName()))
                .willReturn(this.loadBalancer);
        return new RibbonLoadBalancerClient(this.clientFactory);
    }

    protected RibbonServer getRibbonServer() {
        return new RibbonServer("testService", new Server("myhost", 9080), false,
                Collections.singletonMap("mykey", "myvalue"));
    }

}