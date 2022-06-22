# auto-trace-spring

---

[阅读中文版本](README-CN.md)

The [`auto-trace-id`](https://github.com/i36lib/auto-trace-id) for springboot application which not running in java agent mode



### Usage

---
Import the maven dependency

````xml
<dependency>
    <groupId>cn.xlibs.trace</groupId>
    <artifactId>auto-trace-spring</artifactId>
    <version>1.0.0</version>
</dependency>
````

Hook before running the Springboot Application

```java
//...other import...
import cn.xlibs.trace.spring.AutoTrace;

@SpringBootApplication(...)
public class SpringApp {

    public static void main(String[] args) {
        AutoTrace.hookWith(args).beforeRunning(SpringApp.class);
        
        SpringApplication.run(SpringApp.class, args);
    }
}
```
