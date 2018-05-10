--����ע������eureka  @EnableEurekaServer 
	-�߿������,û�к�˻��� ÿһ������ע��֮����Ҫ��ע�����ķ�������
		(Ĭ�������eurekaServer Ҳ��һ��eureka Client )����ָ��һ��server,
		eureka.client.registerWithEureka��false��fetchRegistry��false�������Լ���һ��eureka server
--�����ṩ�� eurekaclient  @EnableEurekaClient
	-�������ļ���ע���Լ��ķ���ע�����ĵĵ�ַ
		spring.application.name,���������֮���໥����һ�㶼�Ǹ������name 

--����������rest+ribbon
	-΢����ܹ���,ҵ�񶼻ᱻ��ֳ�һ�������ķ���,�����ڷ����ͨѶ�ǻ��� http restful 
	-spring cloud �����ַ�����÷�ʽ 1.ribbon+restTemplate 2.feign 

	---ribbon ��һ���������ͻ��� @EnableDiscoveryClient ����htt��tcp��һЩ��Ϊ feign������ribbon
		
			--��Ҫ��������ע��һ��bean restTemplate ͨ��@loadBalancedע��������restTemlate�������ؾ��⹦��
				restTemplate.getForObject("http://SERVICE-HI/hi?name="+name,String.class); ����
	---feign ������@EnableFeignClients����feign����,����һ���ӿ�@feign("������"),��ָ�������ĸ�����
			@FeignClient(value = "service-hi")
			public interface SchedualServiceHi {
					@RequestMapping(value = "/hi",method = RequestMethod.GET)
					String sayHiFromClientOne(@RequestParam(value = "name") String name);
			}
			������и�controller ���Ⱪ¶һ���ӿ������Ѵ˴�web

------------------------------------------------------
һ����������
webӦ��1,webӦ��2 ���������ע��
���������������ע�� ��restTemplate��webӦ�õ��� ,��Ϊ��ribbon���˸��ؾ��� ���Ի���������1,2Ӧ��



---------------------------------------------------------------------------------------------------

---��·�� Hystrix @HystrixCommand
	��������Ⱥ����,������������������,�����������ͻ�����߳�����,���д�������ӿ��,servlet�������߳���Դ
	���������,���·���̱��.�����ڷ���֮�������.���ϻᴫ��,���ѩ��

	--���ض��ķ���ĵ��õĲ����ôﵽһ����ֵ (hystric��5s 20��) ��·���ᱻ��
		�򿪺� ���ñ�������,falllback��������ֱ�ӷ���һ���̶�ֵ
	1.ribbo+restTemplate��ʽ�� ���ýӿ�ʧ��ʱ,����ٶ�· �����ǵȴ���Ӧ��ʱ,�������߳�����
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

	2.Feign��ʽ
		feign��ʽ�Դ���·,û�д� ������feign.hystrix.enabled=true
		@FeignClient(value = "service-hi",fallback = SchedualServiceHiHystric.class)
		public interface SchedualServiceHi {
			 @RequestMapping(value = "/hi",method = RequestMethod.GET)
			String sayHiFromClientOne(@RequestParam(value = "name") String name);
		}
		SchedualServiceHiHystric��Ҫʵ��SchedualServiceHi �ӿڣ���ע�뵽Ioc������
		@Component
		public class SchedualServiceHiHystric implements SchedualServiceHi {
			 @Override
			 public String sayHiFromClientOne(String name) {
				return "sorry "+name;
			}
		}
-----------------------------------
Histrix�Ǳ��� ��������@EnableHystrixDashboard 
	��ض�·������ [��ص���Ӧ��]
[���������Ŀ]
@EnableTurbine
��Ⱥ������Ӧ�õļ��

------------------------------------------------------------------------------------------------

�ֲ�ʽ��������

	���������ļ�ͳһ����, springcloud�� �зֲ�ʽ�����������spring cloud config
	֧�ֱ��� Ҳ֧�ַ���git�ֿ�
	������� ��2����ɫ 1.config server 2.config client
--������@EnableConfigServer ����
	�����ļ�:
		spring.application.name=config-server
		server.port=8888


		spring.cloud.config.server.git.uri=https://github.com/forezp/SpringcloudConfig/ //git�ֿ��ַ
		spring.cloud.config.server.git.searchPaths=respo //���òֿ�·��
		spring.cloud.config.label=master //�ֿ��֧
		spring.cloud.config.server.git.username=your username //�û�������
		spring.cloud.config.server.git.password=your password


--client 

		spring.application.name=config-client
		spring.cloud.config.label=master //Զ�ֿ̲��֧
		spring.cloud.config.profile=dev/test/pro ����/����/��ʽ����
		spring.cloud.config.uri= http://localhost:8888/ //config-server �ĵ�ַ
		server.port=8881

------------------------------------------------------------------------------------------
git------>config-server---------------->config-server
���ص� config-server git---->config-server1/2/3--->����---->service1/2/3
------------------------------------------------------------------------------------------

��Ϣ����
	---spring cloud bus ���ֲ�ʽ�Ľڵ�����������Ϣ������������.���������ڹ㲥�����ļ��ĸ��Ļ���
		����֮���ͨѶ,Ҳ�����ڼ��,
	1.�㲥�����ļ��ĸ���
		��config-client ����bus/refresh Ȼ����Ҫ����������,���ɻ�������ļ��ĸ��ĺ����Ϣ
		/bus/refresh?destination=customers:** [ˢ����Ϊcustomers���з���������ļ�]

		֪ͨ���Ļ��д����� --client1����bus/refresh Ȼ������Ϣ����Ϣ���� ʹ��client��Ⱥ����Ӧ��
		�õ�����Ϣ 
-----------------------------------------------------------------------------------------------

������·׷�� @EnableZipkinServer����
����׷�����zipkin Spring cloud Sleuth �м�����zipkin

��Ҫ����ص�Ӧ������
	server.port=8988
	spring.zipkin.base-url=http://localhost:9411 //zipkin server�� ���ʵ�ַ
	spring.application.name=service-hi

	-------------------------------------------------------------------------------------------
Eureka server ��Ⱥ��
	���ܶ�Ӧ����eurekaע��,���طǳ��� ��Ҫ������Ⱥ��
����:
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

ʹ��defaultZone ʹ2��ע�������໥��Ӧ ,��������ͬ��



-----------------------------------------------------------------------------
docker���� springboot��Ŀ



�ڸ߲��������,����������ͬ������,������� ,������insert update ����ͬʱ����mysql,
ֱ�ӵ�����������������,�������ѻ�����,����too many connections ����,ʹ����Ϣ����,���ǿ���
�첽��������,����ϵͳѹ��.













	