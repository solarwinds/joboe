To test on the Spring 4 MVC webapp, use URLs as below:

@Controller:
http://localhost:8080/test-spring-mvc4/hello?name=abc

@RestController:
http://localhost:8080/test-spring-mvc4/greeting

Asynchronous Controller:
http://localhost:8080/test-spring-mvc4/async?duration=5 (duration param is optional)

Deferred Controller:
http://localhost:8080/test-spring-mvc4/deferred?duration=5 (duration param is optional)
 