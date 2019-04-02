---分布式配置中心
*****************************************************************************************
服务的配置文件 = 程序里面的application.yml + github上的 项目名.yml
*****************************************************************************************
场景：
微服务中，服务多，每个服务有多个实例，实例更多多，分别管理自己配置文件的话，维护困难，因此，需要使用分布
式配置中心，统一管理各个服务的配置文件；
服务 = 提供http接口 + 调用http接口；
使用spring cloud config作为分布式配置中心，特点如下：
服务端：存储方式(本地，git)，配置文件读取方式(环境化，多项目仓库)，安全性(访问密码，加密存储)，自动刷新(内容修改时)；
客户端：加载服务端配置，动态刷新配置，@refreshscope作用域刷新，集成消息总线；

客户端  →获取配置信息→  服务端  →获取配置信息→  git仓库；(客户端找服务端获取配置信息，服务端找git获取
配置信息)

配置中心服务端 就是一个服务，也要注册到 eureka上；

创建 maven子模块 lesson-2-config-server，作为 config服务端，它要把配置文件存在自己本地或者git仓
库；pom.xml中引入 spring-cloud-config-server；
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-config-server</artifactId>
</dependency>
config服务端 也是一个服务，也要注册到 eurak服务端，因此，需要启动 lesson-2-eureka服务；
springboot启动类如下：
/**启动SpringBoot，相当于将web项目发布到tomcat中，并启动tomcat*/
@SpringBootApplication
/**启动 config服务端*/
@EnableConfigServer
/**启动eureka客户端，作为服务提供者，还是消费者，取决于application.yml中eureka.client的配置*/
@EnableEurekaClient
public class ConfigServerApplication {
	public static void main(String[] args) {
		new SpringApplicationBuilder(ConfigServerApplication.class).web(true).run(args);
	}
}
lesson-2-config-server服务作为config服务端 配置文件 bootstrap.yml application.yml，详情见注解；

config客户端服务有 2个，分别是lesson-2-sms-sys和lesson-2-sms-webmvc，先看lesson-2-sms-sys
pom.xml中引入 spring-cloud-starter-config；
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-config</artifactId>
</dependency>
lesson-2-sms-sys服务作为 config客户端，配置文件 bootstrap.yml application.yml，详情见注解；
在 github的 SpringCloudConfig项目中 /respo路径下，创建 lesson-2-sms-sys.yml文件，内容：
tony:
     configString: success
启动类 SmsServiceApplication，详情见注解
对外提供接口类 SmsController，详情见注解
分别启动 lesson-2-eureka，lesson-2-config-server，lesson-2-sms-sys
访问 config服务端里面的lesson-2-sms-sys.yml配置文件：
浏览器 http://localhost:8888/lesson-2-sms-sys/default，返回内容如下：
{"name":"lesson-2-sms-sys","profiles":["default"],"label":null,"version":null,"state":null,
"propertySources":[{"name":"https://github.com/gouyan123/SpringCloudConfig/respo/lesson-2-sms-sys.yml",
"source":{"tony.configString":"success"}}]}

访问 lesson-2-sms-sys服务中 SmsController类提供的 /test接口：http://localhost:8080/test，返回
configString的值：success，说明 @Value("${tony.configString}")从 config服务端 配置文件
lesson-2-sms-sys.yml中获取内容，注意 该服务为 lesson-2-sms-sys服务，该服务只能从 config服务端
获取 名称相应的配置文件，即 lesson-2-sms-sys.yml；
修改github中lesson-2-sms-sys.yml里面的配置信息，修改后内容如下：
tony:
     configString: success
config服务端从github获取配置文件，看一下config服务端是否已更新，访问config服务端：
浏览器 http://localhost:8888/lesson-2-sms-sys/default，返回内容发现 config服务端已更新；
cofig客户端获取配置信息：http://localhost:8080/test，发现没有更新，config客户端需要调用 刷新接口：
http://localhost:8080/refresh，用post方式，返回 "status":401；
因为 lesson-2-sms-sys服务 的application.yml中只配了eureka，因此需要在远程github的配置文件
lesson-2-sms-sys.yml中加入如下内容：
info:
  name: lesson-2-sms-interface
server:
  port: 9002
# 调用refresh的时候不校验权限
management:
  security:
    enabled: false
tony:
  configString: success

注意：这里 port=9002，而lesson-2-sms-sys服务的application.yml里面没有配置 port，现在
lesson-2-sms-sys服务的 port就是 9002了，如果没有lesson-2-sms-sys.yml中的server.port=9002，
那么 lesson-2-sms-sys服务的port默认为8080；
重启 lesson-2-config-server，lesson-2-sms-sys
访问 lesson-2-sms-sys服务中 SmsController类提供的 /test接口：http://localhost:9002/test，注意
此处使用的是远程github中lesson-2-sms-sys.yml里面的port，不是默认端口了，证明lesson-2-sms-sys服
务启动时 加载的是远程github中的 lesson-2-sms-sys.yml配置文件，通过名称映射；
*****************************************************************************************
lesson-2-sms-sys服务启动时 加载的是远程github中的 lesson-2-sms-sys.yml配置文件，通过名称映射；
*****************************************************************************************
远程github的配置文件lesson-2-sms-sys.yml中还需要加入 rabbitmq的配置，如下：
spring:
  rabbitmq:
    host: 47.100.49.95
    username: gouyan
    # 密文需要以{cipher}做为标识
    password: '{cipher}31010f99731d4bd8aa7e3ce76152b5686265e1160043aac7cf769c3c8e1bb7ef'
这里，密码需要使用密文，使用 config服务端的 /encrypt接口进行加密：http://localhost:8888/encrypt，
用post方式发送，content中输入rabbitmq的密码原文 123456，得到密文：...
此处 获取密文发生错误，所以关闭 加密功能，直接使用 123456；
访问 lesson-2-sms-sys服务中 SmsController类提供的 /test接口：http://localhost:9002/test，返回
success，此时再去修改github中lesson-2-sms-sys中tony.configString=haha，post方式 访问更新接口
/refresh，再访问 /test接口，发现返回数据已变化；
以上使用 更新接口 /refresh为手动更新；
config服务端，即远程github中 lesson-2-sms-sys.yml实现 redis连接池的热加载，该配置文件中增加配置：
redis:
  host: localhost
  port: 6379
观察 RedisController类，@RefreshScope注解的地方，当远程配置文件刷新时，会被自动刷新；
package com.dongnaoedu.springcloud.service;
public class RedisController {
	@Autowired
	Environment env;

	@Bean
	/**@RefreshScope 将该方法定义为刷新范围，当refresh刷新配置时，JedisPool这个实例会被刷新*/
	@RefreshScope
	public JedisPool jedisPool() {
		String host = env.getProperty("redis.host");
		int port = env.getProperty("redis.port", Integer.class);
		return new JedisPool(host, port);
	}
}
启动本地 redis，设置 set a tony，调用 http://localhost:9002/redis/get/a接口，获取路径变量a对应的
值，返回 tony；

以上是 单个服务刷新，怎么全局刷新呢？通过rabbitmq，当 1个服务刷新配置时，通知其他服务也刷新该配置；

全局刷新接口：http://localhost:9002/bus/refresh

启动lesson-2-sms-webmvc服务，要先在远程github创建它的配置文件lesson-2-sms-webmvc.yml，然后启动；
lesson

自动刷新：
以上都是通过 /bus/refresh接口，/refresh接口 刷新的，怎么自动刷新呢？
github提交更新(commit)时，触发一个事件，发送http请求到 config-server monitor组件，该组件为
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-config-monitor</artifactId>
</dependency>
该组件通过spring cloud bus消息总线 将通知发送给各服务对应的API
具体配置：github → Repository(SpringCloudConfig) → Settings → Integrations → Webhooks →
Push events → URL(http://localhost:8888/monitor config服务端/monitor接口)

环境化配置：
服务lesson-2-sms-sys作为config客户端：
①如果 服务程序里application.yml配置文件定义，spring.profiles.active=dev
②那么 服务启动时，加载github里面的 lesson-2-sms-sys-dev.yml配置文件

docker查看其启动的rabbitmq的 IP？
docker-machine.exe env default，然后通过浏览器访问 rabbitmq

SpringCloudBus消息总线原理：
git调用config-server的/monitor端口，将更新事件发送给config-server，config-server调用actuator的/refresh端口，/refresh端口将 更新事件发送到SpringCloudBus，
各个config客户端订阅更新事件，当获取更新事件后，去config-server获取更新；

源码分析：
找入口：springboot入口都在 META-INFO里的spring.factories中，在 org.springframework.cloud:spring-cloud-config-server:1.3.1.RELEASE包的META-INFO目录下，
spring.factories文件内容如下：
# Bootstrap components
org.springframework.cloud.bootstrap.BootstrapConfiguration=\
org.springframework.cloud.config.server.bootstrap.ConfigServerBootstrapConfiguration
# Application listeners
org.springframework.context.ApplicationListener=\
org.springframework.cloud.config.server.bootstrap.ConfigServerBootstrapApplicationListener
# Autoconfiguration
org.springframework.boot.autoconfigure.EnableAutoConfiguration=\
# 用于 config-server
org.springframework.cloud.config.server.config.ConfigServerAutoConfiguration,\
# 用于加密
org.springframework.cloud.config.server.config.EncryptionAutoConfiguration,\
org.springframework.cloud.config.server.config.SingleEncryptorAutoConfiguration
看源码过程中，不知道对象是怎么实例化的时，可以在以上反射类中找；一般来说，启动系统就需要用到的类，在这几个初始化配置类中都会进行实例化；
重要的配置类：
PropertySourceBootstrapProperties、ConfigClientProperties、ConfigServerProperties、KeyProperties，配置文件中配置信息都解析到这些类对象中；
config各个客户端如何读取config服务端呢？
config客户端启动时访问config服务端提供的http接口，获取配置信息，解析后加载到spring Environment；config服务端提供的http接口如下：/{application}/{profile}/{label}
PropertySourceBootstrapConfiguration类实现ApplicationContextInitiallizer<ConfigurableApplicationContext>接口，覆写initialize()方法，当上下文
ConfigurableApplicationContext中的bean加载完毕后，就会进行初始化；Spring启动的时候会在所有属性源PropertySource中查找配置文件，并且有优先级；PropertySource
属性源是一个接口，其有多个实现类，代表不同的属性源的解析；

config服务端怎么做的呢？
定义 EnvironmentController类，接收config客户端的http请求，通过EnvironmentRepository去远程仓库获取配置文件信息，EnvironmentRepository是一个底层接口，通过
不同实现类，去不同仓库获取配置文件，例如，NativeEnvironmentRepository类去本地获取配置文件，JGitEnvironmentRepository去git仓库获取配置文件；
通过NativeEnvironmentRepository实现类；

/monitor刷新原理？
依赖spring-cloud-config-monitor.jar，其中定义了PropertyPathEndpoint类，是一个@RestController类，接收http请求；
