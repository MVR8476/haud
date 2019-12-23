# Process Statistics Library


## Table of Contents
1. [About](#about)
2. [Maven dependencis & Usages](#dependencis)
3. [How to use](#use)


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


## 3. How to use <a name="use"></a>


***EOF***
