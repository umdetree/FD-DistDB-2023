# lockmgr

锁

# transaction

业务逻辑
实现了基础业务逻辑在内的一个分布式数据库，包括事务管理工具。
加入了gc功能，定时清理寿命过长的事务。

# How to start
## 使用idea 
1. 进入src/transaction
2. 依序启动：
TransactionManagerImpl.java
RMManagerCars.java
RMManagerCustomers.java
RMManagerFlights.java
RMManagerHotels.java
WorkflowControllerImpl.java
Client.java
## 使用make
```
cd src/transaction
make runtm
make runrmflights
make runrmrooms
make runrmcars
make runrmcustomers
make runwc
make runclient
```