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
if you are  using RedisTemplate (RedisConnectionFactory), then 
you will need to set (super.redisConnectionFactory =redisConnectionFactory)
```java
import com.haud.svalinn.lib.stats.KafkaRedisMonitor;

public class RedisConnectionWatcher  implements ConnectionListener {

	private Config config;
	private RedissonClient redisson;
	private final Logger logger = LoggerFactory.getLogger(RedisConnectionWatcher.class);
	private ScheduledExecutorService scheduledExecutorService = null;
	private boolean isconnected = false;
	private RedisConnectionFactory redisConnectionFactory;
	
	 
	
	@Autowired
	public RedisConnectionWatcher(Config config) {
		this.config = config;
		initializeRedis();
	}

	private void initializeRedis() {
		String uuid = UUID.randomUUID().toString();
		scheduledExecutorService = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors());
		scheduledExecutorService.scheduleAtFixedRate(new Runnable() {

			public void run() {
				logger.info("UUID [{}] trying to connect Redis Server...........!", uuid);
				initializeConnection(uuid);

			}
		}, 0, 10, TimeUnit.SECONDS);

	}

	private void initializeConnection(String uuid) {
		try {
			redisson = Redisson.create(config);
			InitializingBeans(uuid);
			scheduledExecutorService.shutdown();
			scheduledExecutorService.shutdownNow();
			scheduledExecutorService = null;
			isconnected = true;
			logger.info("UUID [{}] Redis Server Started ....!", uuid);
		} catch (Exception e) {
			isconnected = false;
			logger.info("UUID [{}] Redis Server not Started [{}] ", uuid, e.getMessage(), e);

		}

	}

	public void InitializingBeans(String uuid) {
		logger.info("UUID [{}] Bean Initialization started ...........", uuid);
		new RedisConnector(new RedissonConnectionFactory(redisson));
		logger.info("UUID [{}] Bean Initialization completed ...........", uuid);

	}

	@Override
	public void onConnect(InetSocketAddress addr) {
		String uuid = UUID.randomUUID().toString();
		isconnected = true;
		logger.info("UUID [{}] Connected to Redis Server ...........", uuid);

	}

	@Override
	public void onDisconnect(InetSocketAddress addr) {
		String uuid = UUID.randomUUID().toString();
		isconnected = false;
		logger.info("UUID [{}] Disconnecting from Redis Server ...........", uuid);

	}

	public boolean isIsconnected() {
		return isconnected;
	}

	public RedisConnectionFactory getRedisConnectionFactory() {
		return this.redisConnectionFactory;
	}

	class RedisConnector extends KafkaRedisMonitor{

		public RedisConnector(RedisConnectionFactory redissonConnectionFactory) {
			super();
			this.redisConnectionFactory = redissonConnectionFactory;			
			super.redisConnectionFactory=redissonConnectionFactory;		 
		}

		 
	}
}

```

####  SECOND STEP : - increase REDIS (PUT, READ ,DELETE)  (success/failure) Transactions  Statistics count
```java

//To increate Scuccessful Statistics Increment count for ( PUT, READ ,DELETE) Transaction
StatisticsManager.getInstance().incrementRedisSuccessfulStats(redisTopic);

//To increate un scuccessful Statistics Increment count for ( PUT, READ ,DELETE) Transaction
StatisticsManager.getInstance().incrementRedisFailureStats(redisTopic);
```

####  THIRD STEP (prometheus): - increase The Packet sent timestamp (SENT & ACTUAL SENT)
from KAFKA MessageProducer.java you can increase The Packet sent timestamp 
```java

import com.haud.svalinn.lib.stats.StatisticsManager;
import com.haud.lte.service.entity.Diameter.HaudWrapper;

@Service
public class MessageProducer {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private KafkaTemplate<String, byte[]> kafkaTemplate;

	@Value(value = "${haud.kafka.message.producer.topic.name}")
	private String topicName;

	public void sendMessage(ProducerRecord<String, byte[]> message) {
	    /**
	     * newly added mayuresh ratnaparkhi 20-12-2019
	     * The packet sent to kafka time.
	     */
		StatisticsManager.getInstance().setPktSentToKafkaTime();
		ListenableFuture<SendResult<String, byte[]>> future = kafkaTemplate.send(message);

		future.addCallback(new ListenableFutureCallback<SendResult<String, byte[]>>() {

			@Override
			public void onSuccess(SendResult<String, byte[]> result) {
			    /**
			     * newly added mayuresh ratnaparkhi 20-12-2019
			     * The packet sent timestamp.
			     */
				StatisticsManager.getInstance().setPktSentTime();				
				logger.info("Request UUID [{}] Sent message [{}]", Thread.currentThread().getName(), message);
			}

			@Override
			public void onFailure(Throwable ex) {
				logger.error("Request UUID [{}] Unable to send message with error [{}]",
						Thread.currentThread().getName(), ex.getMessage());
			}
		});
	}

	public ProducerRecord<String, byte[]> publishResponse(HaudWrapper diameterWrapper, int partition, String msgKey) {
		logger.info("Request UUID [{}] publish Response for msgKey [{}] in partition [{}]",
				Thread.currentThread().getName(), msgKey, partition);

		return new ProducerRecord<>(topicName, partition, msgKey, diameterWrapper.toByteArray());

	}

}
```

####  FOURTH STEP (prometheus): - increase The Packet received timestamp ( Topic (partition) received & ACTUAL received)

1. This is used to update number of request recieved on this topic in statistics information.
```java

 // This is used to update number of request recieved on this topic in statistics information.
 
 messages.parallelStream().map(message -> {
			return message;
		}).forEachOrdered(message -> {		
			int partition = 0;
			// each message contains kafka topic partition info in x-reply-partition(in byte
			// format) header.Response is submitted to the queue on the same partition.
			if (message.headers().lastHeader(replyPartition) != null) {
				String partStr = new String(message.headers().lastHeader(replyPartition).value());
				if (!partStr.isBlank() && partStr.chars().allMatch(Character::isDigit))
					partition = Integer.parseInt(partStr);
			}
			uuid = UUID.randomUUID().toString();
			processStartTime = System.currentTimeMillis();
			// This is used to update number of request recieved on this topic in statistics
			// information.
			StatisticsManager.getInstance().incrementRequestTopicRcvdStats(partition);
			
```


2. This is used to The packet received timestamp.
```java
 
	diameter = Diameter.HaudWrapper.parseFrom(message.value());
	
	/**
	 *  
	 * The packet received timestamp.
	 */
	StatisticsManager.getInstance().setPktRcvdTime();
			
```

3.  Update packet delay stats based on the timestamps set in DiaMessageInfo.
```java

	/**
	 *  
	 * Update packet delay stats based on the timestamps set in DiaMessageInfo			     *
	 *  
	 */
		StatisticsManager.getInstance().updatePacketDelayStatus();	
```
			  
####  Complete Example (prometheus): -

```java

/*
	 * Gets packets from
	 * 
	 * @Class MessageListener.class parses protobuff to java object and forwards
	 * message to respective handler methods on the basis of command code
	 */
	public void processBatch(List<ConsumerRecord<String, byte[]>> messages) {

		for (ConsumerRecord<String, byte[]> message : messages) 
		{
			messages.parallelStream().map(message -> {
				return message;
			}).forEachOrdered(message -> {		
				int partition = 0;
				// each message contains kafka topic partition info in x-reply-partition(in byte
				// format) header.Response is submitted to the queue on the same partition.
				if (message.headers().lastHeader(replyPartition) != null) {
					String partStr = new String(message.headers().lastHeader(replyPartition).value());
					if (!partStr.isBlank() && partStr.chars().allMatch(Character::isDigit))
						partition = Integer.parseInt(partStr);
				}
				uuid = UUID.randomUUID().toString();
				processStartTime = System.currentTimeMillis();
				// This is used to update number of request recieved on this topic in statistics
				// information.
				StatisticsManager.getInstance().incrementRequestTopicRcvdStats(partition);
				
				try 
				{
				
					diameter = Diameter.HaudWrapper.parseFrom(message.value());
					
					/**
					 *  
					 * The packet received timestamp.
					 */
					StatisticsManager.getInstance().setPktRcvdTime();
									
					
					// This is used to log request processing time --start--
					TimingHandler handler = new TimingHandler(System.currentTimeMillis());
					Constants.transactions.put(diameter.getTrxId(), handler);
					// --end--
					
					
					logger.info("Request UUID [{}] recevied  on partition [{}] ", uuid, message.partition());
					DiameterMsgPayload msgPayload = diameter.getDiameterMsg();
					
								
					/**
					 *  
					 * Update packet delay stats based on the timestamps set in DiaMessageInfo			     
					 *  
					 */
					StatisticsManager.getInstance().updatePacketDelayStatus();				

					 

				} catch (InvalidProtocolBufferException e) {
					logger.error(ErrorMessages.PROTOBUFF_PARSE_ERROR_MSG, uuid, e);
					ArrayList<OutputProperties> outputProperties = new ArrayList<>();
					outputProperties.add(OutputProperties.newBuilder()
							.setKey(Constants.CAT2_CHECKS + ErrorMessages.PROTOBUFF_PARSE_ERROR_CODE)
							.setValue(ErrorMessages.PROTOBUFF_PARSE_ERROR_MSG).build());
					decisionModule.submit(partition, "", uuid, outputProperties, processStartTime, diameter, false);
				}
			});
		}
	}
```
 
***EOF***
