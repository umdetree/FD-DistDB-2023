# lockmgr

锁

# transaction

业务逻辑
实现了基础业务逻辑在内的一个分布式数据库，包括事务管理工具。
加入了gc功能，定时清理寿命过长的事务。

# 运行

我们在环境`Java 17`下开发

到`src/transaction`目录下运行
## 使用make
```
make all
```

然后启动各项服务:
```
make runregistry
make runtm
make runrmflights
make runrmcars
make runrmrooms
make runrmcustomers
make runwc
```


最后运行用户代码

```
make runclient
```

## 使用idea 
依序启动：
TransactionManagerImpl.java
RMManagerCars.java
RMManagerCustomers.java
RMManagerFlights.java
RMManagerHotels.java
WorkflowControllerImpl.java
Client.java
