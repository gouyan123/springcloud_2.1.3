----------------服务网关 Zuul-------------------------------------------------------------
什么是API 网关？
服务实例接口不对外开放，外部调用springcloud中的服务，需要通过 统一入口 网关来映射；
网关工作原理：网关解析uri请求，找到相匹配的服务，然后发起调用，取得结果后，将结果返回给调用者[类似Ngix反向代理]；

为什么需要 网关？
降低 微服务系统复杂性：网关给外部http请求 统一入口，使外部http请求与微服务内部服务实例解耦；

功能增强：附加如 权限校验，限速等机制；

重要概念：
路由表：url 与 服务 的映射关系；
路由定位器：将用户请求的url 与 路由表匹配，得到一个路由；

zuulServlet 受理用户请求；
zuulFilter 做路由定位，发起代理请求，返回结果；

举例如下：
创建 maven子模块 lesson-6-eureka服务作为 eureka服务端，并启动；
创建 maven子模块 lesson-6-config-server，作为 config服务端，并启动；
创建 maven子模块 lesson-6-sms-interface服务作为 eureka客户端[向eureka注册自己的服务,向eureka获取服务]，此处角色 向eureka服务端注册服务，并启动；
发送get请求 http://localhost:9002/sms 测试 短信服务lesson-6-sms-interface；

短信服务lesson-6-sms-interface 不应该对外暴露接口，这个服务接口属于内网，因此需要使用网关代理；
创建 maven子模块 lesson-6-zuul-server服务作为 eureka客户端[向eureka注册自己的服务,向eureka获取服务]，此处角色 向eureka服务端获取服务，并启动；
启动类使用@EnableZuulProxy，开启 网关zuul，使用@EnableCircuitBreaker 开启熔断，使用@EnableEurekaClient，开启eureka客户端；
配置文件 lesson-6-zuul-server.yml在config服务端 lesson-6-config-server的resoureces路径下：

2.怎么配置一个代理路由？
zuul的配置有 3种方式：
① 普通url请求路由配置；反向代理模式：不直接访问一个网址，而是通过一个服务器去访问；为静态路由：   路由服务端口
② eureka服务化的路由会自动配置（重点），为动态路由，不需要在配置文件中指定，例如http://localhost:8765/tony_api/lesson-6-sms-interface/sms，可以直接访问到
sms服务的 /sms接口，但是lesson-6-sms-interface服务是对内的，网关地址里面也不能写，需要配置 忽略ignored-services:
③ ribbon路由配置，为静态路由：


①普通url路由配置，在lesson-6-zuul-server.yml文件中，将外部请求path 路由到 内部实例url；支配routes还不够，还要配路由过期时间；
zuul:
  host:                             # 代理普通http请求的超时时间
    socket-timeout-millis: 2000
    connect-timeout-millis: 1000
    maxTotalConnections: 200        # http连接池大小
    maxPerRouteConnections: 20      # 每个host最大连接数
  routes:
    route1:                         # 路由key，将外部path 路由到 内部实例url；路由key名称自己随便定义；
      path: /oschina/**             # 外部path
      url: http://www.baidu.com     # 内部服务url

测试静态配置，访问zuul服务，发送get请求 http://localhost:8765/tony_api/oschina
②动态路由：不需要在 配置文件中配置，通过eureka直接路由，如果某个服务想屏蔽外部path路由，可以配置 ignored-services；
zuul:
  ignored-services: lesson-6-sms-interface

动态路由指与eureka集成的：lesson-6-sms-interface不对外开放接口，要想访问这个服务必须通过网关 zuul，发送get请求 http://localhost:8765/tony_api/lesson-6-sms-interface/sms
有返回值；路由是eureka中获取的，直接将 路径/lesson-6-sms-interface路由到 服务lesson-6-sms-interface；

③ribbon路由配置，在lesson-6-zuul-server.yml文件中；
zuul:
    route-service-by-ribbon:        # 路由key，将外部path 路由到 内部实例serviceId；路由key名称自己随便定义；
      path: /service-by-ribbon/**   # 外部path；
      serviceId: service-by-ribbon  # 内部服务；服务信息是通过ribbon负载均衡器配置的；
service-by-ribbon服务配置，在lesson-6-zuul-server.yml文件中；
service-by-ribbon:                                                                          # 服务名称
  listOfServers: http://www.csdn.net,http://www.baidu.com,http://www.dongnaoedu.com         # 服务实例列表
  ribbon:
    NFLoadBalancerRuleClassName: com.netflix.loadbalancer.RandomRule                        # 负载策略
    NIWSServerListClassName: com.netflix.loadbalancer.ConfigurationBasedServerList          # 设置它的服务实例信息来自配置文件, 而不是euereka
测试Ribbon配置，访问zuul服务，发送get请求 http://localhost:8765/tony_api/service-by-ribbon

3.动态刷新路由含义：动态 添加或者删除或者屏蔽 路由；

4.如何实现动态刷新路由方法：
a eureka会自动维护；
b 配置文件中的静态路由，修改 config-server服务中或者远程仓库中 lesson-6-zuul-server.yml文件中路由设置，然后发送post请求http://localhost:8765/refresh，其
中8765为lesson-6-zuul-server服务的端口，访问lesson-6-zuul-server服务的 /refresh端点，可以动态更新配置，不需要重启lesson-6-zuul-server服务；

5.降级策略如何配置？
①集成eureka，zuul是使用 hystrix + ribbon来调用服务的；注意超时时间的配置，包括ribbon超时和hystrix超时，配置在 lesson-6-zuul-server.yml文件中；
②实现 ZuulFallbackProvider接口；
# 注意项：
# 1、zuul环境下，信号量模式下并发量的大小zuul.semaphore.maxSemaphores的优先级高于 hystrix信号量并发量大小；
# 2、zuul环境下，资源隔离策略默认信号量zuul.ribbonIsolationStrategy的优先级高于 hystrix...；
# 3、zuul环境下，分组 固定为RibbonCommand；
# 4、zuul环境下，commandKey 对应每个服务的serviceId
hystrix:
  command:
    default:                                    # 这是默认的配置
      execution:
        timeout:
          enabled: true
        isolation:
          thread:
            timeoutInMilliseconds: 2000         # 命令执行超时时间
ribbon:
  ConnectTimeout: 1000                          # 配置ribbon默认的超时时间
  ReadTimeout: 2000
  OkToRetryOnAllOperations: true                # 是否开启重试
  MaxAutoRetriesNextServer: 1                   # 重试期间，实例切换次数
  MaxAutoRetries: 0                             # 当前实例重试次数

当请求超时的时候，要对服务进行降级，见com.dongnaoedu.springcloud.zuul.DefaultFallbackProvider类，该类实现ZuulFallbackProvider接口；
测试：访问lesson-6-sms-interface服务的 TestController#timeOut()方法，即 http://localhost:8765/tony_api/lesson-6-sms-interface/hystrix/timeout，设置
2s超时，该请求3s钟才会响应，因此当请求超过 2s时，会调用DefaultFallbackProvider类，返回降级结果；
---
Zuul大部分功能都是通过过滤器来实现的。Zuul中定义了四种标准过滤器类型，这些过滤器类型对应于请求的典型生命周期。
(1) PRE：这种过滤器在请求被路由之前调用。我们可利用这种过滤器实现身份验证、在集群中选择请求的微服务、记录调试信息等。
(2) ROUTING：这种过滤器将请求路由到微服务。这种过滤器用于构建发送给微服务的请求，并使用Apache HttpClient或Netfilx Ribbon请求微服务。
(3) POST：这种过滤器在路由到微服务以后执行。这种过滤器可用来为响应添加标准的HTTP Header、收集统计信息和指标、将响应从微服务发送给客户端等。
(4) ERROR：在其他阶段发生错误时执行该过滤器。
---
SpringCloud网关zuul原理：
路由结构：
zuul:
  route01:                  # 路由key
    path：/oschina/*        # 外部path
    serviceId: sms-service  # 内部服务名称

网关中两个重要概念：
1 路由：外部path 与 内部服务 的对应关系，不是精确对应，包含模糊匹配；
2 路由定位器 RouteLocator：将外部path 与 路由表匹配，得到一个路由；
3 zuulServlet：受理外部path；
4 zuulFilter：使用路由定位器实现路由定位，发起代理请求，返回结果；

---
看图 zuul-流程.png：
pre类型zuul filter 解析请求：在发起请求前 寻找路由，权限校验，自定义限流；route类型zuul filter 发送请求：会判断是调用普通 url，还是调用微服务，然后发起请求；
如果没有异常，post类型zuul filter则返回结果，因为是反向代理，调用服务的是zuulServlet，最终要将结果返回给客户端；
---
看图 zuul路由定位器、filter详解.png
看代码小技巧：通过 exception的堆栈信息来看代码调用链接（视频没演示），最终使用 debug打断点的形式讲解的 ；
在 ZuulServlet类的 service()方法上面 打断点，发送请求 http://localhost:8765/tony_api/oschina/ 到网关zuul，会停在断点，在debugger上面会出现所有方法的调用链；
调用链中发现 ZuulController；
要想知道ZuulServlet从哪里来的，就要找它的被调用处，查找被调用处快捷键 ctr + 左键；结果发现 ZuulServlet类在ZuulConfiguration类中被调用，再继续找
ZuulConfiguration在哪里被调用，即从哪里来，ctr + 左键；结果发现 EnableZuulServer注解上使用了@Import(ZuulConfiguration.class)，表示将 ZuulCongiguration
导入到 IOC容器；即在启动类使用@EnableZuulProxy激活代理时，会导入一系列 filter相关的实例bean；
************************************************************************************************************************************************
1、@Import(A.class)作用：当用到A实例bean时，再将其导入到 IOC容器；
2、@Import的三种使用方式：
通过查看@Import源码可以发现@Import注解只能注解在类上，以及唯一的参数value上可以配置3种类型的值Configuration，ImportSelector，ImportBeanDefinitionRegistrar
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Import {
    Class<?>[] value();
}

a、基于Configuration也就是直接 在Import()中填对应的class数组，当扫到 @Configuration时 会把@Import()中的 class都加载到 IOC容器；
@Import({Square.class,Circular.class})
@Configuration
public class MainConfig {}
运行结果：
bean名称为===mainConfig
bean名称为===com.zhang.bean.Square
bean名称为===com.zhang.bean.Circular

b，基于自定义ImportSelector的使用：
/**定义一个我自己的ImportSelector*/
public class MyImportSelector implements  ImportSelector{
    public String[] selectImports(AnnotationMetadata importingClassMetadata) {
        return new String[]{"com.zhang.bean.Triangle"};
    }
}
MainConfig注解配置修改如下：
@Import({Square.class,Circular.class,MyImportSelector.class})
@Configuration
public class MainConfig {}
运行结果：
bean名称为===mainConfig
bean名称为===com.zhang.bean.Square
bean名称为===com.zhang.bean.Circular
bean名称为===com.zhang.bean.Triangle
************************************************************************************************************************************************
ZuulConfiguration，但是代码中没有引入EnableZuulServer注解，而是在启动类上引入了@EnableZuulProxy，跟@EnableZuulProxy，发现引入了@Import(ZuulProxyConfiguration.class)
ZuulProxyConfiguration，ZuulProxyConfiguration继承了 ZuulConfiguration，当使用@EnableZuulProxy注解时，ZuulConfiguration就生效了，会加载ZuulProxyConfiguration
里面的配置，这些配置做了哪些事情呢？1 实例化了一个ZuulController
@Bean
public ZuulController zuulController() {return new ZuulController();}

.factory是入口，注解是入口；
跟 ZuulServlet类的 service()方法，如下：
@Override
public void service(javax.servlet.ServletRequest servletRequest, javax.servlet.ServletResponse servletResponse) throws ServletException, IOException {
    try {
        init((HttpServletRequest) servletRequest, (HttpServletResponse) servletResponse);
        RequestContext context = RequestContext.getCurrentContext();
        context.setZuulEngineRan();
        try {
            preRoute();     //运行 pre类型的zuulfilter，解析请求，路由 → 微服务；@EnableZuulProxy会加载所有zuulFilter；
        } catch (ZuulException e) {
            error(e);     //对应 error类型的 zuulfilter
            postRoute();
            return;
        }
        try {
            route();     //对应 route类型的 zuulfilter，发起请求，路由 → 微服务；
        } catch (ZuulException e) {
            error(e);
            postRoute();
            return;
        }
        try {
            postRoute();     //对应 post类型的 zuulfilter，发送响应，响应客户端；
        } catch (ZuulException e) {
            error(e);
            return;
        }
    } catch (Throwable e) {
        error(new ZuulException(e, 500, "UNHANDLED_EXCEPTION_" + e.getClass().getName()));
    } finally {
        RequestContext.getCurrentContext().unset();
    }
}
---
filter加载过程：
跟其中的preRoute()，该方法要获取所有filter，并执行各种类型的filter，那么filter哪里来的呢？看图 zuul路由定位器、filter详解.png； 了解 filter加载流程：
从 ZuulServlet的入口ZuulConfiguration（入口即调用处）和ZuulConfiguration的子类ZuulProxyConfiguration 找，可以发现ZuulProxyConfiguration中实例化了多个
filter，如下：
@Bean
public PreDecorationFilter preDecorationFilter(RouteLocator routeLocator, ProxyRequestHelper proxyRequestHelper) {... return new Filter()}
@Bean
public RibbonRoutingFilter ribbonRoutingFilter(ProxyRequestHelper helper,RibbonCommandFactory<?> ribbonCommandFactory) {... return new Filter()}
跟ZuulServlet类中 preRoute()方法到 runFilters(String sType)方法中的 List<ZuulFilter> list = FilterLoader.getInstance().getFiltersByType(sType);
跟 getFiltersByType(sType)方法，该方法从所有zuulFilter中获取 pre类型，route类型，post类型的zuulFilter，部分代码如下：
Collection<ZuulFilter> filters = filterRegistry.getAllFilters();
for (Iterator<ZuulFilter> iterator = filters.iterator(); iterator.hasNext(); ) {
    ZuulFilter filter = iterator.next();
    if (filter.filterType().equals(filterType)) {
        list.add(filter);
    }
}
发现所有 filter都注册到 filterRegistry中了(FilterRegistry filterRegistry = FilterRegistry.instance())；那么 filter什么时候注册进filterRegistry的呢？
在初始化的时候，通过 ZuulFilterInitializer注册进去的；ZuulConfiguration中实例化 zuulFilterInitializer代码如下：
@Configuration
protected static class ZuulFilterConfiguration {
    @Autowired
    private Map<String, ZuulFilter> filters;
    @Bean
    public ZuulFilterInitializer zuulFilterInitializer(CounterFactory counterFactory, TracerFactory tracerFactory) {
        FilterLoader filterLoader = FilterLoader.getInstance();
        FilterRegistry filterRegistry = FilterRegistry.instance();
        return new ZuulFilterInitializer(this.filters, counterFactory, tracerFactory, filterLoader, filterRegistry);
    }
}
@Autowired
private Map<String, ZuulFilter> filters; 注入所有 filter；new ZuulFilterInitializer()往 filterRegistry里面不断注册新的东西，代码如下：
@Override
public void contextInitialized(ServletContextEvent sce) {
    TracerFactory.initialize(tracerFactory);
    CounterFactory.initialize(counterFactory);
    for (Map.Entry<String, ZuulFilter> entry : this.filters.entrySet()) {
        filterRegistry.put(entry.getKey(), entry.getValue());       // 注册
    }
}
---
filter执行过程
看图 zuul路由定位器、filter详解.png；
filter执行器 FilterProcessor按类型执行filter runFilters(String sType)方法如下：
public Object runFilters(String sType) throws Throwable {
    if (RequestContext.getCurrentContext().debugRouting()) {
        Debug.addRoutingDebug("Invoking {" + sType + "} type filters");
    }
    boolean bResult = false;
    List<ZuulFilter> list = FilterLoader.getInstance().getFiltersByType(sType);
    if (list != null) {
        for (int i = 0; i < list.size(); i++) {
            ZuulFilter zuulFilter = list.get(i);
            Object result = processZuulFilter(zuulFilter);
            if (result != null && result instanceof Boolean) {
                bResult |= ((Boolean) result);
            }
        }
    }
    return bResult;
}
getFiltersByType(sType)方法，根据类型获取 filter并对filter进行排序；
processZuulFilter(zuulFilter);所有filter使用processZuulFilter()这个执行器；跟processZuulFilter()，里面filter.runFilter();表示判断filter是否执行；
runFilter()代码如下：
public ZuulFilterResult runFilter() {
    ZuulFilterResult zr = new ZuulFilterResult();
    if (!isFilterDisabled()) {              // 判断 外部配置文件 是否关闭了该zuulFilter；
        if (shouldFilter()) {               // 根据每个zuulFilter子类实现，来判断是否执行；
            Tracer t = TracerFactory.instance().startMicroTracer("ZUUL::" + this.getClass().getSimpleName());
            try {
                Object res = run();         // 每个 zuulFilter的实现来实现这个 run()
                zr = new ZuulFilterResult(res, ExecutionStatus.SUCCESS);
            } catch (Throwable e) {
                t.setName("ZUUL::" + this.getClass().getSimpleName() + " failed");
                zr = new ZuulFilterResult(ExecutionStatus.FAILED);
                zr.setException(e);
            } finally {
                t.stopAndLog();
            }
        } else {
            zr = new ZuulFilterResult(ExecutionStatus.SKIPPED);
        }
    }
    return zr;
}
if (!isFilterDisabled())判断filter在配置文件中是否被关闭；if (shouldFilter())判断filter是否需要执行，根据当前上下文；Object res = run();通过run()方法执行
filter，run()方法是一个接口方法，由每个filter的实现类来做的；
---
路由定位流程：
所有请求过来后，都要经过路由定位 PreDecorationFilter，跟进去，在shouldFilter()和run()方法中设置 断点，发送get请求 http://localhost:8765/tony_api/oschina/
到网关zuul，shouldFilter()方法用于判断这个filter是否需要执行，如果需要执行这个 filter就调用 run()方法，否则不调用run()方法；所有filter类都要继承ZuulFilter
并实现 shouldFilter()方法和 run()方法；run()方法如下：
@Override
public Object run() {
    RequestContext ctx = RequestContext.getCurrentContext();                                    //RequestContext.getCurrentContext();是线程变量
    final String requestURI = this.urlPathHelper.getPathWithinApplication(ctx.getRequest());    //解析请求，此处返回requestURI = /tony_api/oschina/
    //路由定位器routeLocator，根据路由匹配服务getMatchingRoute(requestURI)，返回路由对象 Route，里面封装Route如下：路由key，外部path，内部服务，路由即外部path和内部服务的映射关系
    //Route(id=route1,fullPath=/tony_api/oschina/,location="http://www.dongnaoedu.com")，其中location为路由的地址，!= null表示可以请求；
    Route route = this.routeLocator.getMatchingRoute(requestURI);
    if (route != null) {
        String location = route.getLocation();
        if (location != null) {
            ctx.put(REQUEST_URI_KEY, route.getPath());
            ctx.put(PROXY_KEY, route.getId());
            if (!route.isCustomSensitiveHeaders()) {
                this.proxyRequestHelper.addIgnoredHeaders(this.properties.getSensitiveHeaders().toArray(new String[0]));
            }
            else {
                this.proxyRequestHelper.addIgnoredHeaders(route.getSensitiveHeaders().toArray(new String[0]));
            }
            if (route.getRetryable() != null) {
                ctx.put(RETRYABLE_KEY, route.getRetryable());
            }

            if (location.startsWith(HTTP_SCHEME+":") || location.startsWith(HTTPS_SCHEME+":")) {    // location以http开头，往当前线程变量ctx中设置一个getUrl(location)
                ctx.setRouteHost(getUrl(location));
                ctx.addOriginResponseHeader(SERVICE_HEADER, location);
            }
            else if (location.startsWith(FORWARD_LOCATION_PREFIX)) {
                ctx.set(FORWARD_TO_KEY,StringUtils.cleanPath(location.substring(FORWARD_LOCATION_PREFIX.length()) + route.getPath()));
                ctx.setRouteHost(null);
                return null;
            }
            else {
                // set serviceId for use in filters.route.RibbonRequest
                ctx.set(SERVICE_ID_KEY, location);
                ctx.setRouteHost(null);
                ctx.addOriginResponseHeader(SERVICE_ID_HEADER, location);
            }
            if (this.properties.isAddProxyHeaders()) {
                addProxyHeaders(ctx, route);
                String xforwardedfor = ctx.getRequest().getHeader(X_FORWARDED_FOR_HEADER);
                String remoteAddr = ctx.getRequest().getRemoteAddr();
                if (xforwardedfor == null) {
                    xforwardedfor = remoteAddr;
                }
                else if (!xforwardedfor.contains(remoteAddr)) { // Prevent duplicates
                    xforwardedfor += ", " + remoteAddr;
                }
                ctx.addZuulRequestHeader(X_FORWARDED_FOR_HEADER, xforwardedfor);
            }
            if (this.properties.isAddHostHeader()) {
                ctx.addZuulRequestHeader(HttpHeaders.HOST, toHostHeader(ctx.getRequest()));
            }
        }
    }
    else {
        String fallBackUri = requestURI;
        String fallbackPrefix = this.dispatcherServletPath;
        if (RequestUtils.isZuulServletRequest()) {
            // remove the Zuul servletPath from the requestUri
            fallBackUri = fallBackUri.replaceFirst(this.properties.getServletPath(), "");
        }
        else {
            // remove the DispatcherServlet servletPath from the requestUri
            fallBackUri = fallBackUri.replaceFirst(this.dispatcherServletPath, "");
        }
        if (!fallBackUri.startsWith("/")) {
            fallBackUri = "/" + fallBackUri;
        }
        String forwardURI = fallbackPrefix + fallBackUri;
        forwardURI = forwardURI.replaceAll("//", "/");
        ctx.set(FORWARD_TO_KEY, forwardURI);
    }
    return null;
}
通过pre filter找到路由（路由即 外部path和内部服务的 映射关系）后，使用route filter执行请求，请求分为 2种：
1 普通url请求 SimpleHostRoutingFilter；
2 微服务请求 RibbonRoutingFilter；
所有filter都要继承 ZuulFilter，实现 shouldFilter()和run()方法，在SimpleHostRoutingFilter类的这2个方法和RibbonRoutingFilter类的这2个方法中打断点；发送get
请求http://localhost:8765/tony_api/oschina/，先进入PreDecorationFilter类的shouldFilter()方法，判断是否要执行，然后PreDecorationFilter类的run()方法进行
路由定位 RibbonRoutingFilter类shouldFilter()方法，判断不是调用微服务的请求，返回false，因此它的 run()方法也不会执行；又进入SimpleHostRoutingFilter类的
shouldFilter()方法，判断是调用普通url的请求，并调用它的 run()方法；run()方法如下：
@Override
public Object run() {
    RequestContext context = RequestContext.getCurrentContext();
    HttpServletRequest request = context.getRequest();
    MultiValueMap<String, String> headers = this.helper.buildZuulRequestHeaders(request);
    MultiValueMap<String, String> params = this.helper.buildZuulRequestQueryParams(request);
    String verb = getVerb(request);
    InputStream requestEntity = getRequestBody(request);
    if (request.getContentLength() < 0) {
        context.setChunkedRequestBody();
    }
    String uri = this.helper.buildZuulRequestURI(request);      //获取请求参数 ，做反向代理
    this.helper.addIgnoredHeaders();
    try {
        //跟forward()其中CloseableHttpResponse zuulResponse = forwardRequest(httpclient, httpHost,httpRequest);方法去请求，forwardRequest()通过
        //httpclient去请求，httpclient是由连接池进行管理的  @PostConstruct private void initialize() {...}，每个连接过来都是通过httpclient去请求，
        CloseableHttpResponse response = forward(this.httpClient, verb, uri, request,headers, params, requestEntity);
        setResponse(response);
    }...
    return null;
}
发送get请求 http://localhost:8765/tony_api/lesson-6-sms-interface/sms
先进入PreDecorationFilter类的shouldFilter()方法，判断是否要执行，然后PreDecorationFilter类的run()方法进行路由定位 RibbonRoutingFilter类shouldFilter()方
法，判断是调用微服务的请求，返回true，因此调用 run()方法；run()方法如下：
@Override
public Object run() {
    // 获取当前线程变量
    RequestContext context = RequestContext.getCurrentContext();
    this.helper.addIgnoredHeaders();
    try {
        // 设置上下文参数
        RibbonCommandContext commandContext = buildCommandContext(context);
        // forward通过ribbonCommandFactory#create构建一个命令，使用ribbon负载均衡器去调用服务；
        ClientHttpResponse response = forward(commandContext);
        setResponse(response);
        return response;
    }
    catch (ZuulException ex) {
        throw new ZuulRuntimeException(ex);
    }
    catch (Exception ex) {
        throw new ZuulRuntimeException(ex);
    }
}
forward方法如下：
protected ClientHttpResponse forward(RibbonCommandContext context) throws Exception {
    Map<String, Object> info = this.helper.debug(context.getMethod(), context.getUri(), context.getHeaders(), context.getParams(),context.getRequestEntity());
    RibbonCommand command = this.ribbonCommandFactory.create(context);          // 创建命令 command并执行命令command.execute()
    try {
        ClientHttpResponse response = command.execute();
        this.helper.appendDebug(info, response.getStatusCode().value(), response.getHeaders());
        return response;
    }
    catch (HystrixRuntimeException ex) {
        return handleException(info, ex);
    }
}
跟create(context)方法
@Override
public HttpClientRibbonCommand create(final RibbonCommandContext context) {
    ZuulFallbackProvider zuulFallbackProvider = getFallbackProvider(context.getServiceId());
    final String serviceId = context.getServiceId();
    // 创建 RibbonLoadBalancingHttpClient
    final RibbonLoadBalancingHttpClient client = this.clientFactory.getClient(serviceId, RibbonLoadBalancingHttpClient.class);
    client.setLoadBalancer(this.clientFactory.getLoadBalancer(serviceId));
    // 重点：
    return new HttpClientRibbonCommand(serviceId, client, context, zuulProperties, zuulFallbackProvider,clientFactory.getClientConfig(serviceId));
}
小结：每个外部path 被 ZuulServlet接收，先通过 PreDecorationFilter类进行资源定位，即 获取路由路径并与路由表匹配，获取真正服务url，然后通过
SimpleHostRoutingFilter或者RibbonRoutingFilter发起请求，请求普通url或者微服务，其中PreDecorationFilter和SimpleHostRoutingFilter和RibbonRoutingFilter
都继承ZuulFilter，实现 shouldFilter()方法和run()方法，shouldFilter()方法判断该filter是否执行，如果执行filter，通过run()方法执行；
PreDecorationFilter类的 run()方法中的 路由定位器：Route route = this.routeLocator.getMatchingRoute(requestURI);getMatchingRoute()如下：
当调用getMatchingRoute()方法时，该方法会获取所有路由(包括配置文件中配置的路由，通过eureka自动发现的路由)，然后过滤掉不需要代理的服务；
SimpleRouteLocator类中getRoutes()方法中使用locateRoutes()方法如下：
@Override
protected LinkedHashMap<String, ZuulRoute> locateRoutes() {
    LinkedHashMap<String, ZuulRoute> routesMap = new LinkedHashMap<String, ZuulRoute>();
    routesMap.putAll(super.locateRoutes());                                                     // super.locateRoutes()从配置文件中获取路由信息
    if (this.discovery != null) {
        Map<String, ZuulRoute> staticServices = new LinkedHashMap<String, ZuulRoute>();         // 从 eureka中获取路由信息
        for (ZuulRoute route : routesMap.values()) {
            String serviceId = route.getServiceId();
            if (serviceId == null) {
                serviceId = route.getId();
            }
            if (serviceId != null) {
                staticServices.put(serviceId, route);
            }
        }
        ...
    }
    return values;
}
---
lesson-6-zuul-server项目代码中，自定义 2个filter；TokenValidataFilter用于验权，这个filter在配置文件中可以禁用，禁用后就不会使用这个filter过滤了 如下：
# lesson-6-zuul-server.yml中 禁用自定义的token校验filter
zuul:
  TokenValidataFilter:
    pre:
      disable: true
TokenValidataFilter类中定义 shouldFilter()方法，用于判断这个 filter是否执行；例如，获取token的请求 http://localhost:8765/api/token/byPhone就不需要
token验证，在lesson-6-zuul-server.yml中配置如下：
# 以下是自定义的配置，配置的值会被注入到TonyConfigurationBean这个类
tony:
  zuul:
    tokenFilter:
      noAuthenticationRoutes:
        - uaa-token     # - 表示集合，整体表示不需要token过滤的 路由
zuul:
  routes:
    uaa-token:                                    # 定义一个路由，路由key为uaa-token，在做验权的时候需要用到
      path: /token/byPhone                        # 外部path
      serviceId: lesson-6-uaa-interface           # 内部服务
# jwt配置，在 application.yml中
token:
  jwt:
    key: a123456789
    iss: lesson-6-uaa-interface             # 发放者
    expm: 120                               # token有效期

lesson-6-uaa-interface服务中，定义com.dongnaoedu.springcloud.uaa.web.TokenController，代码如下：
// 获取一个根据手机号和密码获取token
@PostMapping("/token/byPhone")
public ResponseEntity<?> getTokenByPhone(@RequestBody User user) {
    // 这个实例中没有加入其它逻辑
    // TODO 你可以去数据库里面查有没有这个用户，密码对不对。如果密码不对你就不给他返回token。
    try {
        Thread.sleep(3000L);
    } catch (InterruptedException e) {
        e.printStackTrace();
    }
    return ResponseEntity.ok(new JWTToken(jwtTokenProvider.createToken(parseClaims(user))));
}
流程小结：访问网关 http://localhost:8765/api/token/byPhone，通过uaa-token，将外部path：token/byPhone 路由到 内部服务：lesson-6-uaa-interface的
TokenController类，路由的时候回执行 TokenValidataFilter类的过滤，shouldFilter()判断是否执行TokenValidataFilter的过滤，noAuthenticationRoutes配置了
该路由 不执行TokenValidataFilter类的过滤；路由到lesson-6-uaa-interface的TokenController类后，该类会去数据库取出 该请求的用户数据，并创建token返回；

shouldFilter()：判断 TokenValidataFilter是否执行，代码如下：
 @Override
public boolean shouldFilter() {
    new Exception().printStackTrace();
    RequestContext ctx = RequestContext.getCurrentContext();
    // 根据routeId，过滤掉不需要做权限校验的请求
    return !tonyConfigurationBean.getNoAuthenticationRoutes().contains(ctx.get("proxy"));
}
run()方法：如果shouldFilter()返回true，表示执行TokenValidataFilter，那么由run()方法执行，代码如下：
@Override
public Object run() {
    // zuul中，将当前请求的上下文信息存在线程变量中。取出来
    RequestContext ctx = RequestContext.getCurrentContext();
    // 从上下文中获取httprequest对象
    HttpServletRequest request = ctx.getRequest();
    // 从头部信息中获取Authentication的值，也就是我们的token
    String token = request.getHeader("Authorization");
    if(token == null) {
        forbidden();
        return null;
    }
    // 检验token是否正确；jwt：json web token；
    // 这里只是通过使用key对token进行解码是否成功，并没有对有效期、已经token里面的内容进行校验。
    Claims claims = jwtTokenProvider.parseToken(token);
    if (claims == null) {
        forbidden();
        return null;
    }
    // 可以将token内容输出出来看看
    logger.debug("当前请求的token内容是：{}", JSONObject.toJSONString(claims));
    // 拓展：可以在token里面塞一些其他的值，用来做路由验权。
    // 比如在UAAClaims对象中，存储这个token能访问哪些路由。如果当前这个请求对应的route，不在token中，就代表没有请求权限
    // 示例：uaaclaim中有一个scope数组值为[oschia,lession-6-sms-interface],那么就代表这个token只能用于这两个路由的访问
    return null;
}