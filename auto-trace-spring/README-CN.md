# auto-trace-spring

---

[Read this in English](README.md)

为非Java Agent启动方式的Spingboot应用而准备的[`auto-trace-id`](https://github.com/i36lib/auto-trace-id)版本。



### 使用方法

---
引入Maven依赖

````xml
<dependency>
    <groupId>cn.xlibs.trace</groupId>
    <artifactId>auto-trace-spring</artifactId>
    <version>1.0.0</version>
</dependency>
````

在Springboot应用启动前注册hook

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
