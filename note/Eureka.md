1、eureka是springcloud的核心；
2、配置中心，对各模块配置文件进行集中化管理；
3、消费者怎么去调用提供者，会用到 1个工具 2个组件 ribbon feign；
4、服务的 限流 熔断 机制；
5、网关组件，因为微服务一般不对外的，因此使用网关Zuul，将外部 http请求映射到 服务上；
6、微服务就需要知道事件驱动，目前，系统之间都是通过 http接口 调用的，但是有些情况下，可以通过 rabbitMQ
解耦的，springcloud提供了 消息总线组件，它集成了rabbitmq，kafka，来实现事件驱动，因此需要学习springcloud
中事件驱动怎么做；
7、请求到网关 Zuul，映射到 服务1 进行下单，是需要调到很多系统的，如果用户请求失败了，要去跟踪原因，传统
的日志只是单系统的，系统之间日志如何关联呢？需要 分布式服务调用追踪组件 sleuth；并使用ELK整理整个集群的
信息；
-----------------------------------------------------------------------------------------
为什么使用 eureka？
大型系统，会拆分成很多子系统；每个子系统有 2个功能：提供接口，调用接口，这样的子系统就是一个微服务；
每个微服务要部署多个实例，且会动态扩容，ip不固定，因此需要 eureka组件 进行服务的管理，eureka服务管理核心：服务 Id 为唯一标识；
******************************************************************************************
重点：1个服务，部署到多台机器上，形成 多个实例，且各个实例的 服务Id都相同；
******************************************************************************************

eureka结构：1个服务端 + 2个客户端(服务提供者，服务调用者)；
eureka服务端引入 spring-cloud-starter-eureka-server包；
eureka客户端引入 spring-cloud-starter-eureka包；

eureka原理分析：
1、服务提供者怎么注册到服务中心的？
2、注册中心怎么接收注册请求？
3、注册中心如何存储服务信息？
4、注册中心高可用机制是什么？
5、Eureka集群同步机制？
6、注册中心剔除服务的机制？
7、服务消费者如何获取服务信息？

创建maven子模块 eureka-server，pom.xml，pom.xml中需要导入 eureka的启动包；
配置 bootstrap.yml，该文件里面主要配置 服务名称，该配置文件在application.yml之前加载；
application.yml里面进行eureka相关配置；
启动 EurekaServerApplication类，该类上面需要加相关注解，即可启动springboot项目；

*****************************************************************************************
eureka服务端页面：前面是 服务Id，后面是实例id；一个服务部署到多台机器时，后面有多个实例Id*
*****************************************************************************************

创建maven子模块 lesson-1-sms-interface，作为服务提供者
浏览器：http://localhost:9002/，返回接口信息如下：
{
  "_links" : {
    "sms" : {
      "href" : "http://localhost:9002/sms{?page,size,sort}",
      "templated" : true
    },
    "profile" : {
      "href" : "http://localhost:9002/profile"
    }
  }
}
postman发送post请求 http://localhost:9002/sms ，发送的 json内容如下：
{
	"phone":"123",
	"content":"haha"
}
postman发送get请求 http://localhost:9002/sms/1，获取刚才发送的json数据；

创建maven子模块 lesson-1-website，作为服务调用者，调用 /test接口，里面调用 lesson-1-sms-interface服务的 /sms接口；
---
---eureka服务端 客户端源码分析
2、springcloud中 eureka服务端和客户端是怎么通过注解生效的？
eureka服务端初始化：
在项目的 External Libraries中找到spring-cloud-netflix-eureka-server，这个是 eureka服务端的包；不在spring-cloud-starter-eureka-server包；
springboot启动时，会扫描该包META-INF目录下的spring.factories，内容如下：
org.springframework.boot.autoconfigure.EnableAutoConfiguration=\
  org.springframework.cloud.netflix.eureka.server.EurekaServerAutoConfiguration     # eureka服务端初始化
该文件定义了初始化配置对应的类；可以通过 ctr + N 查找EurekaServerAutoConfiguration类；

eureka客户端初始化：
在项目的 External Libraries中找到spring-cloud-netflix-eureka-client，这个是 eureka 客户端的包；
springboot启动时，会扫描该包META-INF目录下的spring.factories，内容如下：

org.springframework.boot.autoconfigure.EnableAutoConfiguration=\
org.springframework.cloud.netflix.eureka.config.EurekaClientConfigServerAutoConfiguration,\     #不用关注
org.springframework.cloud.netflix.eureka.config.EurekaDiscoveryClientConfigServiceAutoConfiguration,\     #不用关注
org.springframework.cloud.netflix.eureka.EurekaClientAutoConfiguration,\    # eureka客户端初始化
org.springframework.cloud.netflix.ribbon.eureka.RibbonEurekaAutoConfiguration     #不用关注，在Ribbon模块关注
org.springframework.cloud.bootstrap.BootstrapConfiguration=\
org.springframework.cloud.netflix.eureka.config.EurekaDiscoveryClientConfigServiceBootstrapConfiguration
org.springframework.cloud.client.discovery.EnableDiscoveryClient=\
org.springframework.cloud.netflix.eureka.EurekaDiscoveryClientConfiguration

org.springframework.cloud.client.discovery.EnableDiscoveryClient=\
org.springframework.cloud.netflix.eureka.EurekaDiscoveryClientConfiguration    # eureka客户端初始化

重点关注类：EurekaClientAutoConfiguration，EurekaDiscoveryClientConfiguration；
该文件定义了初始化配置对应的类；可以通过 ctr + N 查找EurekaClientAutoConfiguration类；

3、eureka客户端作为服务提供者，怎么注册到eureka服务端的？
将服务的信息(ip,port,serviceId,instanceId)注册到eureka服务端；
跟EurekaClientAutoConfiguration#eurekaApplicationInfoManager()方法，如下：
@Bean
@ConditionalOnMissingBean(value = {ApplicationInfoManager.class},search = SearchStrategy.CURRENT)
public ApplicationInfoManager eurekaApplicationInfoManager(EurekaInstanceConfig config) {
    InstanceInfo instanceInfo = (new InstanceInfoFactory()).create(config);
    return new ApplicationInfoManager(config, instanceInfo);
}
跟 create()方法，进入 InstanceInfoFactory类，方法如下：
public InstanceInfo create(EurekaInstanceConfig config) {
   ...
   builder.setNamespace(namespace).setAppName(config.getAppname()).
   setInstanceId(config.getInstanceId()).setAppGroupName(config.getAppGroupName()).
   setDataCenterInfo(config.getDataCenterInfo()).setIPAddr(config.getIpAddress()).
   setHostName(config.getHostName(false)).setPort(config.getNonSecurePort()).
   enablePort(PortType.UNSECURE, config.isNonSecurePortEnabled()).
   setSecurePort(config.getSecurePort()).
   enablePort(PortType.SECURE, config.getSecurePortEnabled()).
   setVIPAddress(config.getVirtualHostName()).
   setSecureVIPAddress(config.getSecureVirtualHostName()).
   setHomePageUrl(config.getHomePageUrlPath(), config.getHomePageUrl()).
   setStatusPageUrl(config.getStatusPageUrlPath(), config.getStatusPageUrl()).
   setHealthCheckUrls(config.getHealthCheckUrlPath(), config.getHealthCheckUrl(), config.getSecureHealthCheckUrl()).
   setASGName(config.getASGName());
   ...
}
其中，将配置文件中内容 封装到 config中，然后从 config中取出属性内容，存入 builder中；
EurekaInstanceConfig是一个接口，有3个重要实现，ctr + shift + 左键单击，选择EurekaInstanceConfigBean
代码如下：
@ConfigurationProperties("eureka.instance")
public class EurekaInstanceConfigBean implements CloudEurekaInstanceConfig, EnvironmentAware {

}
其中，@ConfigurationProperties("eureka.instance")是用来读取配置文件application.yml的，读取规则如下：
例如，读取 application.yml文件，内容如下：
eureka:
  client:
    registerWithEureka: true
    fetchRegistry: true
    serviceUrl:
      defaultZone: http://localhost:8761/eureka/
  instance:
    leaseRenewalIntervalInSeconds: 30
读取 eureka.instance开头的信息；

eureka配置类如下：
InstanceRegistryProperties，EurekaServerConfigBean，EurekaClientConfigBean，EurekaInstanceConfigBean，EurekaDashboardProperties；

跟EurekaClientAutoConfiguration#eurekaClient() ，实例化DiscoveryClient的一个子类，代码如下：
@Bean(destroyMethod = "shutdown")
@ConditionalOnMissingBean(value = EurekaClient.class, search = SearchStrategy.CURRENT)
public EurekaClient eurekaClient(ApplicationInfoManager manager, EurekaClientConfig config) {
    //跟 new CloudEurekaClient()，代码如下
    return new CloudEurekaClient(manager, config, this.optionalArgs, this.context);
}
CloudEurekaClient构造方法如下：
public CloudEurekaClient(ApplicationInfoManager applicationInfoManager,EurekaClientConfig config,DiscoveryClientOptionalArgs args,ApplicationEventPublisher publisher) {
    //跟super()，代码如下
    super(applicationInfoManager, config, args);
    this.applicationInfoManager = applicationInfoManager;
    this.publisher = publisher;
    this.eurekaTransportField = ReflectionUtils.findField(DiscoveryClient.class, "eurekaTransport");
    ReflectionUtils.makeAccessible(this.eurekaTransportField);
}
super()代码如下，发现CloudEurekaClient是DiscoveryClient的子类；
@Deprecated
public DiscoveryClient(ApplicationInfoManager applicationInfoManager, final EurekaClientConfig config, DiscoveryClientOptionalArgs args) {
    //跟this()，代码如下：
    this(applicationInfoManager, config, (AbstractDiscoveryClientOptionalArgs) args);
}
this()代码：
@Inject
DiscoveryClient(ApplicationInfoManager applicationInfoManager, EurekaClientConfig config, AbstractDiscoveryClientOptionalArgs args, Provider<BackupRegistry> backupRegistryProvider) {
    if (args != null) {
        this.healthCheckHandlerProvider = args.healthCheckHandlerProvider;
        this.healthCheckCallbackProvider = args.healthCheckCallbackProvider;
        this.eventListeners.addAll(args.getEventListeners());
    } else {
        this.healthCheckCallbackProvider = null;
        this.healthCheckHandlerProvider = null;
    }

    this.applicationInfoManager = applicationInfoManager;
    InstanceInfo myInfo = applicationInfoManager.getInfo();

    clientConfig = config;
    staticClientConfig = clientConfig;
    transportConfig = config.getTransportConfig();
    instanceInfo = myInfo;
    if (myInfo != null) {
        appPathIdentifier = instanceInfo.getAppName() + "/" + instanceInfo.getId();
    } else {
        logger.warn("Setting instanceInfo to a passed in null value");
    }

    this.backupRegistryProvider = backupRegistryProvider;

    this.urlRandomizer = new EndpointUtils.InstanceInfoBasedUrlRandomizer(instanceInfo);
    localRegionApps.set(new Applications());

    fetchRegistryGeneration = new AtomicLong(0);

    remoteRegionsToFetch = new AtomicReference<String>(clientConfig.fetchRegistryForRemoteRegions());
    remoteRegionsRef = new AtomicReference<>(remoteRegionsToFetch.get() == null ? null : remoteRegionsToFetch.get().split(","));

    if (config.shouldFetchRegistry()) {
        this.registryStalenessMonitor = new ThresholdLevelsMetric(this, METRIC_REGISTRY_PREFIX + "lastUpdateSec_", new long[]{15L, 30L, 60L, 120L, 240L, 480L});
    } else {
        this.registryStalenessMonitor = ThresholdLevelsMetric.NO_OP_METRIC;
    }

    if (config.shouldRegisterWithEureka()) {
        this.heartbeatStalenessMonitor = new ThresholdLevelsMetric(this, METRIC_REGISTRATION_PREFIX + "lastHeartbeatSec_", new long[]{15L, 30L, 60L, 120L, 240L, 480L});
    } else {
        this.heartbeatStalenessMonitor = ThresholdLevelsMetric.NO_OP_METRIC;
    }

    logger.info("Initializing Eureka in region {}", clientConfig.getRegion());

    if (!config.shouldRegisterWithEureka() && !config.shouldFetchRegistry()) {
        logger.info("Client configured to neither register nor query for data.");
        scheduler = null;
        heartbeatExecutor = null;
        cacheRefreshExecutor = null;
        eurekaTransport = null;
        instanceRegionChecker = new InstanceRegionChecker(new PropertyBasedAzToRegionMapper(config), clientConfig.getRegion());

        // This is a bit of hack to allow for existing code using DiscoveryManager.getInstance()
        // to work with DI'd DiscoveryClient
        DiscoveryManager.getInstance().setDiscoveryClient(this);
        DiscoveryManager.getInstance().setEurekaClientConfig(config);

        initTimestampMs = System.currentTimeMillis();

        logger.info("Discovery Client initialized at timestamp {} with initial instances count: {}", initTimestampMs, this.getApplications().size());
        return;  // no need to setup up an network tasks and we are done
    }

    try {
        scheduler = Executors.newScheduledThreadPool(3,
                new ThreadFactoryBuilder()
                        .setNameFormat("DiscoveryClient-%d")
                        .setDaemon(true)
                        .build());

        heartbeatExecutor = new ThreadPoolExecutor(
                1, clientConfig.getHeartbeatExecutorThreadPoolSize(), 0, TimeUnit.SECONDS,
                new SynchronousQueue<Runnable>(),
                new ThreadFactoryBuilder()
                        .setNameFormat("DiscoveryClient-HeartbeatExecutor-%d")
                        .setDaemon(true)
                        .build()
        );  // use direct handoff

        cacheRefreshExecutor = new ThreadPoolExecutor(
                1, clientConfig.getCacheRefreshExecutorThreadPoolSize(), 0, TimeUnit.SECONDS,
                new SynchronousQueue<Runnable>(),
                new ThreadFactoryBuilder()
                        .setNameFormat("DiscoveryClient-CacheRefreshExecutor-%d")
                        .setDaemon(true)
                        .build()
        );  // use direct handoff

        eurekaTransport = new EurekaTransport();
        scheduleServerEndpointTask(eurekaTransport, args);

        AzToRegionMapper azToRegionMapper;
        if (clientConfig.shouldUseDnsForFetchingServiceUrls()) {
            azToRegionMapper = new DNSBasedAzToRegionMapper(clientConfig);
        } else {
            azToRegionMapper = new PropertyBasedAzToRegionMapper(clientConfig);
        }
        if (null != remoteRegionsToFetch.get()) {
            azToRegionMapper.setRegionsToFetch(remoteRegionsToFetch.get().split(","));
        }
        instanceRegionChecker = new InstanceRegionChecker(azToRegionMapper, clientConfig.getRegion());
    } catch (Throwable e) {
        throw new RuntimeException("Failed to initialize DiscoveryClient!", e);
    }

    if (clientConfig.shouldFetchRegistry() && !fetchRegistry(false)) {
        fetchRegistryFromBackup();
    }
    //初始化 定时任务；跟initScheduledTasks()
    initScheduledTasks();
    try {
        Monitors.registerObject(this);
    } catch (Throwable e) {
        logger.warn("Cannot register timers", e);
    }

    // This is a bit of hack to allow for existing code using DiscoveryManager.getInstance()
    // to work with DI'd DiscoveryClient
    DiscoveryManager.getInstance().setDiscoveryClient(this);
    DiscoveryManager.getInstance().setEurekaClientConfig(config);

    initTimestampMs = System.currentTimeMillis();
    logger.info("Discovery Client initialized at timestamp {} with initial instances count: {}",
            initTimestampMs, this.getApplications().size());
}
DiscoveryClient类在实例化后，开启了定时任务renew，DiscoveryClient#initScheduledTasks

/**Initializes all scheduled tasks.*/
private void initScheduledTasks() {
    ...
    if (clientConfig.shouldRegisterWithEureka()) {
        int renewalIntervalInSecs = instanceInfo.getLeaseInfo().getRenewalIntervalInSecs();
        int expBackOffBound = clientConfig.getHeartbeatExecutorExponentialBackOffBound();
        logger.info("Starting heartbeat executor: " + "renew interval is: " + renewalIntervalInSecs);
        // Heartbeat timer 心跳检测：每隔几秒，执行一次new HeartbeatThread()线程；
        scheduler.schedule(
                new TimedSupervisorTask(
                        "heartbeat",
                        scheduler,
                        heartbeatExecutor,
                        renewalIntervalInSecs,
                        TimeUnit.SECONDS,
                        expBackOffBound,
                        //跟 HeartbeatThread()构造方法
                        new HeartbeatThread()
                ),
                renewalIntervalInSecs, TimeUnit.SECONDS);
    ...
}
跟 new HeartbeatThread()；renew()方法发送心跳，返回404，代表没有注册，则进入注册流程，
private class HeartbeatThread implements Runnable {
    public void run() {
        //跟 renew()
        if (renew()) {
            lastSuccessfulHeartbeatTimestamp = System.currentTimeMillis();
        }
    }
}
DiscoveryClient#renew()：
boolean renew() {
    EurekaHttpResponse<InstanceInfo> httpResponse;
    try {
        httpResponse = eurekaTransport.registrationClient.sendHeartBeat(instanceInfo.getAppName(), instanceInfo.getId(), instanceInfo, null);
        //如果返回404，则eureka客户端 重新向eureka服务端注册；
        if (httpResponse.getStatusCode() == 404) {
            REREGISTER_COUNTER.increment();
            #跟 DiscoveryClient#register()方法
            return register();
        }
        return httpResponse.getStatusCode() == 200;
    } catch (Throwable e) {
        return false;
    }
}
至此，服务提供者已注册到服务中心；

4、服务中心怎么接收注册请求？
通过rest接口接收，通过jersey框架实现这些rest接口；通过@EnableEurekaServer这个注解开启 eureka server服务端；
初始化的过程：spring集成 eureka server原生包中 Jersey RESTful接口，Jersey中的Path就像spring中的 requestMapping；
；在eureka server初始化过程中，eureka server什么时候把 rest接口注册上去的？
Eureka托管github地址：https://github.com/Netflix/eureka；→ wiki → Eureka REST operations选项：
通过EurekaServerAutoConfiguration#jerseyApplication() 处理 eureka客户端注册请求：
@Bean
public javax.ws.rs.core.Application jerseyApplication(Environment environment, ResourceLoader resourceLoader) {
    ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false, environment);
    // Filter to include only classes that have a particular annotation.
    provider.addIncludeFilter(new AnnotationTypeFilter(Path.class));
    provider.addIncludeFilter(new AnnotationTypeFilter(Provider.class));
    // Find classes in Eureka packages (or subpackages)
    Set<Class<?>> classes = new HashSet<Class<?>>();
    //扫描 eureka下所有的包，包括com.netflix.discovery，com.netflix.eureka，扫描包中所有的类；
    for (String basePackage : EUREKA_PACKAGES) {
        Set<BeanDefinition> beans = provider.findCandidateComponents(basePackage);
        for (BeanDefinition bd : beans) {
            Class<?> cls = ClassUtils.resolveClassName(bd.getBeanClassName(),resourceLoader.getClassLoader());
            classes.add(cls);
        }
    }

    // Construct the Jersey ResourceConfig
    Map<String, Object> propsAndFeatures = new HashMap<String, Object>();
    propsAndFeatures.put(
            // Skip static content used by the webapp
            ServletContainer.PROPERTY_WEB_PAGE_CONTENT_REGEX,
            EurekaConstants.DEFAULT_PREFIX + "/(fonts|images|css|js)/.*");
    DefaultResourceConfig rc = new DefaultResourceConfig(classes);
    rc.setPropertiesAndFeatures(propsAndFeatures);
    return rc;
}
springcloud将 注册信息扫描出来，创建一个实例 javax.ws.rs.core.Application，该Application实际是 FilterRegistrationBean，EurekaServerAutoConfiguration#jerseyFilterRegistration()的返回值；
@Bean
public FilterRegistrationBean jerseyFilterRegistration(javax.ws.rs.core.Application eurekaJerseyApp) {
    //动态加载 filter，不需要xml配置filter了
    FilterRegistrationBean bean = new FilterRegistrationBean();
    bean.setFilter(new ServletContainer(eurekaJerseyApp));
    bean.setOrder(Ordered.LOWEST_PRECEDENCE);
    bean.setUrlPatterns(Collections.singletonList(EurekaConstants.DEFAULT_PREFIX + "/*"));
    return bean;
}

接收eureka 客户端请求：
注册时调用该方法：ApplicationResource#addInstance()，发送post请求时调用该 addInstance()方法，因为springcloud集成了 eureka中的 jersey；
@POST
@Consumes({"application/json", "application/xml"})
public Response addInstance(InstanceInfo info,@HeaderParam(PeerEurekaNode.HEADER_REPLICATION) String isReplication) {
    ......
}
受理请求：InstanceRegistry#register() 发布 EurekaInstanceRegisteredEvent事件，并调用父类方法 PeerAwareInstanceRegistryImpl#register()
集群同步：replicateToPeers；
最后调用 AbstractInstanceRegistry.registry()，将eureka客户端 发送过来的实例信息存在 ConcurrentHashMap中；

6、服务中心自身是怎么实现高可用的？ peer2peer 对等eureka server实例，各eureka server相互注册，共享实例信息；

7、服务集群之间怎么同步信息？如何去重？
eureka server初始化时，维护了一个 PeerEurekaNodes.peerEurekaNodeUrls对象，