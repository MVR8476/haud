# Process Statistics Library


## Table of Contents
1. [About](#about)
2. [Maven dependencis & Usages](#dependencis)
3. [Usage in Actual Project](#use)


## 1. About <a name="about"></a>
Using Process Statistics Library act as Watcher, which keeps the track on below activities
<ul>
		<li>a) REDIS server up or down and increase the (success/failure) connection  Statistics count.</li>
		<li>b) KAFKA server up or down and increase the (success/failure) connection  Statistics count.</li>
		<li>c) While dealing with REDIS (PUT, READ ,DELETE) Transactions, it increase the (success/failure) Transactions  Statistics count  </li>
		<li>d) Support to prometheus : -  <br>
		<div>
			It useful whenever we send Packets send to KAFKA and tries to consume it from KAFKA.<br>
			It keeps the records of 
				<ul>
					<li> The Packet sent timestamp</li>
					<li> The Packet received Kafka timestamp</li>
					<li> The Actual Packet received Kafka timestamp</li>
					<li> Update the number of request count recieved by KAFKA </li>
				</ul>
		</div></li>		
</ul>

## 2. Maven dependencis & Usages <a name="dependencis"></a>
configured  below maven dependecies in your pom.xml
```
<dependency>
  <groupId>com.haud.svalinn.lib</groupId>
  <artifactId>process-statistics-library</artifactId>
  <version>6288-RELEASE</version>
</dependency>
```


## 3. Usage in Actual Project <a name="use"></a>

####  FIRST STEP : - REDIS  configuration

you need to import com.haud.svalinn.lib.stats.KafkaRedisMonitor; in your Redis configuration file.

There are two types of configurations.
1. Redission 
2. RedisTemplate (RedisConnectionFactory)

if you are using Redisson then 
you need to check redission connection and base on that
need to set (super.isconnected=true;)
```java

import com.haud.svalinn.lib.stats.KafkaRedisMonitor;
import com.haud.svalinn.lib.stats.StatisticsManager;

public class MessageRepository extends KafkaRedisMonitor implements ConnectionListener {
	private Config config;
	RMap<Object, Object> map = null;
	RedissonClient client = null;
	private final Logger logger = LoggerFactory.getLogger(MessageRepository.class);
	private ScheduledExecutorService scheduledExecutorService = null;
	 

	@Autowired
	public MessageRepository(Config config) {
		this.config = config;
		checkRedisConnection();
	}

	private void checkRedisConnection() {

		String uuid = UUID.randomUUID().toString();
		scheduledExecutorService = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors());
		scheduledExecutorService.scheduleWithFixedDelay(new Runnable() {

			public void run() {
				logger.info("UUID [{}] trying to connect Redis Server...........!", uuid);
				initializeConnection(uuid);

			}
		}, 0, 10, TimeUnit.SECONDS);

	}

	private void initializeConnection(String uuid) {
		try {

			client = Redisson.create(config);
			client.getNodesGroup().addConnectionListener(this);
			scheduledExecutorService.shutdown();
			scheduledExecutorService.shutdownNow();
			scheduledExecutorService = null;
			super.isconnected=true;
			logger.info("UUID [{}] Redis Server Started ....!", uuid);
		} catch (Exception e) {
			logger.info("UUID [{}] Redis Connection Error [{}] ", uuid, e.getMessage(), e);

		}

	}

	public void deleteRedisMap(String redisTopic, String uuid) {
		if (redisTopic != null) {
			try {

				client.getKeys().delete(redisTopic);
				StatisticsManager.getInstance().incrementRedisSuccessfulStats(redisTopic);

			} catch (Exception e) {
				logger.info("UUID [{}] Redis Connection Error while deleting data [{}] ", uuid, e.getMessage(), e);
				StatisticsManager.getInstance().incrementRedisFailureStats(redisTopic);
			}

		}
	}

	public void deleteRedisMessageWithKey(String redisTopic, String redisTopicKey, String uuid) {
		if (redisTopicKey != null && redisTopic != null) {
			try {
				
				client.getMap(redisTopic).fastRemoveAsync(redisTopicKey);
				StatisticsManager.getInstance().incrementRedisSuccessfulStats(redisTopic);

			} catch (Exception e) {
				logger.info("UUID [{}] Redis Connection Error while deleting data [{}] ", uuid, e.getMessage(), e);
				StatisticsManager.getInstance().incrementRedisFailureStats(redisTopic);
			}
		}
	}

	public Object getRedisMessageWithKey(String redisTopic, String redisTopicKey, String uuid) {
		Object object = null;
		if (redisTopicKey != null && redisTopic != null) {
			try {

				object = client.getMap(redisTopic).get(redisTopicKey);
				StatisticsManager.getInstance().incrementRedisSuccessfulStats(redisTopic);

			} catch (Exception e) {
				logger.info("UUID [{}] Redis Connection Error while getting data [{}] ", uuid, e.getMessage(), e);
				StatisticsManager.getInstance().incrementRedisFailureStats(redisTopic);
			}

		}

		return object;
	}

	public void putRedisMessageWithKey(String redisTopic, String redisTopicKey, Object redisMessage, String uuid) {
		try {
			if (redisTopicKey != null && redisTopic != null) {

				map = client.getMap(redisTopic);
				map.fastPutAsync(redisTopicKey, redisMessage);
				StatisticsManager.getInstance().incrementRedisSuccessfulStats(redisTopic);

			}
		} catch (Exception e) {
			StatisticsManager.getInstance().incrementRedisFailureStats(redisTopic);
			logger.info("UUID [{}] Redis Connection Error while putting data  [{}] ", uuid, e.getMessage(), e);
		}
	}

	@Override
	public void onConnect(InetSocketAddress addr) {
		String uuid = UUID.randomUUID().toString();
		super.isconnected = true;
		logger.info("UUID [{}] Connected to Redis Server ...........", uuid);
	}

	@Override
	public void onDisconnect(InetSocketAddress addr) {
		String uuid = UUID.randomUUID().toString();
		try {

			client.getKeys();
			super.isconnected = true;

		} catch (Exception e) {
			super.isconnected = false;
			logger.info("UUID [{}] Disconnecting from Redis Server ...........", uuid);
		}
	}

	public boolean isIsconnected() {
		return super.isconnected;
	}

}

```

***EOF***
