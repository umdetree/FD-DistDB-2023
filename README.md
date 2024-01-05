# lockmgr

锁

# transaction

业务逻辑

# 运行

我们在环境`Java 17/18`下开发

到`src/transaction`目录下运行

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