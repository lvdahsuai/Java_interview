--服务注册中心eureka  @EnableEurekaServer 
	-高可用组件,没有后端缓存 每一个势力注册之后需要向注册中心发送心跳
		(默认情况下eurekaServer 也是一个eureka Client )必须指定一个server,
		eureka.client.registerWithEureka：false和fetchRegistry：false来表明自己是一个eureka server
--服务提供者 eurekaclient  @EnableEurekaClient
	-在配置文件中注明自己的服务注册中心的地址
		spring.application.name,服务与服务之间相互调用一般都是根据这个name 

--服务消费者rest+ribbon
	-微服务架构中,业务都会被拆分成一个独立的服务,服务于服务的通讯是基于 http restful 
	-spring cloud 有两种服务调用方式 1.ribbon+restTemplate 2.feign 

	---ribbon 是一个负责均衡客户端 @EnableDiscoveryClient 控制htt和tcp的一些行为 feign集成了ribbon
		
			--需要在启动类注入一个bean restTemplate 通过@loadBalanced注解表明这个restTemlate开启负载均衡功能
				restTemplate.getForObject("http://SERVICE-HI/hi?name="+name,String.class); 调用
	---feign 启动类@EnableFeignClients开启feign服务,定义一个接口@feign("服务名"),来指定调用哪个服务
			@FeignClient(value = "service-hi")
			public interface SchedualServiceHi {
					@RequestMapping(value = "/hi",method = RequestMethod.GET)
					String sayHiFromClientOne(@RequestParam(value = "name") String name);
			}
			在外边有个controller 对外暴露一个接口来消费此次web

------------------------------------------------------
一个服务中心
web应用1,web应用2 向服务中心注册
消费者向服务中心注册 用restTemplate像web应用调用 ,因为用ribbon做了负载均衡 所以会轮流调用1,2应用



---------------------------------------------------------------------------------------------------

---断路器 Hystrix @HystrixCommand
	单个服务集群部署,如果单个服务出现问题,调用这个服务就会出现线程阻塞,如有大量请求涌入,servlet容器的线程资源
	被消耗完毕,导致服务瘫痪.服务于服务之间的依赖.故障会传播,造成雪崩

	--当特定的服务的调用的不可用达到一个阀值 (hystric是5s 20次) 断路器会被打开
		打开后 可用避免连锁,falllback方法可以直接返回一个固定值
	1.ribbo+restTemplate方式是 调用接口失败时,会快速短路 而不是等待响应超时,控制了线程柱塞
		@Service
		public class HelloService {

		@Autowired
		RestTemplate restTemplate;

		@HystrixCommand(fallbackMethod = "hiError")
		public String hiService(String name) {
			return restTemplate.getForObject("http://SERVICE-HI/hi?name="+name,String.class);
		}

		public String hiError(String name) {
			return "hi,"+name+",sorry,error!";
		}
	}

	2.Feign方式
		feign方式自带断路,没有打开 需配置feign.hystrix.enabled=true
		@FeignClient(value = "service-hi",fallback = SchedualServiceHiHystric.class)
		public interface SchedualServiceHi {
			 @RequestMapping(value = "/hi",method = RequestMethod.GET)
			String sayHiFromClientOne(@RequestParam(value = "name") String name);
		}
		SchedualServiceHiHystric需要实现SchedualServiceHi 接口，并注入到Ioc容器中
		@Component
		public class SchedualServiceHiHystric implements SchedualServiceHi {
			 @Override
			 public String sayHiFromClientOne(String name) {
				return "sorry "+name;
			}
		}
-----------------------------------
Histrix仪表盘 在启动内@EnableHystrixDashboard 
	监控断路器工作 [监控单个应用]
[监控整个项目]
@EnableTurbine
集群中所有应用的监控

------------------------------------------------------------------------------------------------

分布式配置中心

	方便配置文件统一管理, springcloud中 有分布式配置中心组件spring cloud config
	支持本地 也支持放在git仓库
	在组件中 有2个角色 1.config server 2.config client
--主程序@EnableConfigServer 开启
	配置文件:
		spring.application.name=config-server
		server.port=8888


		spring.cloud.config.server.git.uri=https://github.com/forezp/SpringcloudConfig/ //git仓库地址
		spring.cloud.config.server.git.searchPaths=respo //配置仓库路径
		spring.cloud.config.label=master //仓库分支
		spring.cloud.config.server.git.username=your username //用户名密码
		spring.cloud.config.server.git.password=your password


--client 

		spring.application.name=config-client
		spring.cloud.config.label=master //远程仓库分支
		spring.cloud.config.profile=dev/test/pro 开发/测试/正式环境
		spring.cloud.config.uri= http://localhost:8888/ //config-server 的地址
		server.port=8881

------------------------------------------------------------------------------------------
git------>config-server---------------->config-server
负载的 config-server git---->config-server1/2/3--->负载---->service1/2/3
------------------------------------------------------------------------------------------

消息总线
	---spring cloud bus 讲分布式的节点用轻量的消息代理链接起来.他可以用于广播配置文件的更改或者
		服务之间的通讯,也可用于监控,
	1.广播配置文件的更改
		在config-client 请求bus/refresh 然后不需要重启服务器,即可获得配置文件的更改后的信息
		/bus/refresh?destination=customers:** [刷新名为customers所有服务的配置文件]

		通知更改会有传播性 --client1请求bus/refresh 然后发送消息到消息总线 使得client集群所有应用
		得到此消息 
-----------------------------------------------------------------------------------------------

服务链路追踪 @EnableZipkinServer开启
服务追踪组件zipkin Spring cloud Sleuth 中集成了zipkin

需要被监控的应用配置
	server.port=8988
	spring.zipkin.base-url=http://localhost:9411 //zipkin server的 访问地址
	spring.application.name=service-hi

	-------------------------------------------------------------------------------------------
Eureka server 集群化
	当很多应用向eureka注册,负载非常高 需要将他集群化
配置:
server:
  port: 8769

spring:
  profiles: peer2
eureka:
  instance:
    hostname: peer2
  client:
    serviceUrl:
      defaultZone: http://peer1:8761/eureka/

使用defaultZone 使2个注册中心相互感应 ,数据做到同步



-----------------------------------------------------------------------------
docker部署 springboot项目



在高并发情况下,由于来不及同步处理,请求堵塞 ,大量的insert update 请求同时到达mysql,
直接导致无数的行数表锁,最后请求堆积过多,出发too many connections 错误,使用消息队列,我们可以
异步处理请求,缓解系统压力.













	