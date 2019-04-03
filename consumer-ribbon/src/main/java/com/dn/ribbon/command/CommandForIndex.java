package com.dn.ribbon.command;

import com.netflix.hystrix.*;
import org.springframework.web.client.RestTemplate;

public class CommandForIndex extends HystrixCommand<Object> {

    private final RestTemplate template;

    private String id;

    public CommandForIndex(String id, RestTemplate restTemplate) {
        super(Setter
                // 调用的 服务名
                .withGroupKey(HystrixCommandGroupKey.Factory.asKey("provider-service"))
                // 调用的 服务接口名
                .andCommandKey(HystrixCommandKey.Factory.asKey("user"))
                // 线程池命名，默认是 服务名，客户端调用者 为 每个 服务提供者 创建一个线程池
                .andThreadPoolKey(HystrixThreadPoolKey.Factory.asKey("provider-service"))
                // command 熔断相关参数配置
                .andCommandPropertiesDefaults(HystrixCommandProperties.Setter()
                                // 隔离模式：默认采用线程池隔离，还有一种信号量隔离方式 如下
                                // .withExecutionIsolationStrategy(HystrixCommandProperties.ExecutionIsolationStrategy.SEMAPHORE)
                                // 线程池隔离模式下 超过超时时间，会调用 客户端调用者降级方法
                                .withExecutionTimeoutInMilliseconds(1000)

                        // 信号量隔离的模式下，最大的请求数，与线程池大小的意义一样，一定时间内 并发请求数超过 信号量 超过的请求 调用 客户端调用者降级方法
                        // .withExecutionIsolationSemaphoreMaxConcurrentRequests(2)
                        // 熔断时间：熔断开启后，5秒后进入半开启状态，试探是否恢复正常，恢复正常则关闭 该服务该接口的熔断，否则继续开启 该服务该接口的熔断；
                        // .withCircuitBreakerSleepWindowInMilliseconds(5000)
                )
                // 线程池隔离模式下 线程池参数
                .andThreadPoolPropertiesDefaults(HystrixThreadPoolProperties.Setter()
                        // 客户端调用者 为某个服务 创建的 线程池大小，一定时间内 并发请求数超过 线程池大小 超过的请求 调用 客户端调用者降级方法
                        .withCoreSize(2)
                        //允许最大的缓冲区大小
                       .withMaxQueueSize(2)
                ));
        // super(HystrixCommandGroupKey.Factory.asKey("DnUser-command"),100);
        this.id = id;
        this.template = restTemplate;
    }

    @Override
    protected Object run() throws Exception {
        System.out.println("###command#######" + Thread.currentThread().toString());
        Object result =  template.getForObject("http://provider-service/user?id="+id+"",Object.class);
        System.out.println("###command结束#######" + Thread.currentThread().toString() + ">><>>>执行结果:" + result.toString());
        return result;
    }

    @Override
    protected Object getFallback() {
        //超时降级后，服务端依然会返回，并没有减轻服务端压力，因为客户端不能终止服务端 Http连接
        System.out.println("###降级啦####" + Thread.currentThread().toString());
        return "出錯了，我降級了";
        //降级的处理：
        //1.返回一个固定的值
        //2.去查询缓存
        //3.调用一个备用接口
    }
}
