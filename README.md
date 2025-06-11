# Single-Use Token Interceptor (WSO2 IS 6.0.0)

This project includes a custom event listener `SingleUseTokenInterceptor` extending the `AbstractOAuthEventInterceptor` 
that intercepts token introspection requests to invalidate the access token through a `oauth2/revoke` HTTP request after validation.

---

### Configuration:

Add the below to the `<IS_HOME>/repository/conf/deployment.toml` file:
   ```toml
   [[event_listener]]
   enable = true
   id = "single_use_token_interceptor"
   type = "org.wso2.carbon.identity.oauth.event.OAuthEventInterceptor"
   name = "org.sample.single.use.token.interceptor.SingleUseTokenInterceptor"
   order = 9983
   ```

You can add custom properties to the event listener by adding it under `properties`:
   ```toml
   [[event_listener]]
   enable = true
   id = "single_use_token_interceptor"
   type = "org.wso2.carbon.identity.oauth.event.OAuthEventInterceptor"
   name = "org.sample.single.use.token.interceptor.SingleUseTokenInterceptor"
   order = 9983
   properties.UserAgent = "CustomOAuthTokenInterceptor"
   properties.IsBlocking = "false"
   ```

Then, read it using the `IdentityEventListenerConfig` object returned by `IdentityUtil::readEventListenerProperty`:
   ```java
   private String getProperty(final String key, final String defaultValue) {
      final IdentityEventListenerConfig identityEventListenerConfig = 
              IdentityUtil.readEventListenerProperty(OAuthEventInterceptor.class.getName(), SingleUseTokenInterceptor.class.getName());
      return identityEventListenerConfig == null ?
              defaultValue : identityEventListenerConfig.getProperties().getProperty(key);
   }
   ```
_Note: The first parameter for `IdentityUtil::readEventListenerProperty` should match the event listener type (i.e., `OAuthEventInterceptor`)._

----

### Usage

1. Build the project using Maven:
   ```bash
   mvn clean install
   ```
2. Copy the `target/single-use-token-interceptor-1.0.0-SNAPSHOT.jar` file to the `<IS_HOME>/repository/components/dropins` directory.
3. Add the configuration mentioned in the [_Configuration_](#configuration) section.
4. Start the server.

---

### Logging:

For this component's logs to be printed, you need to do the following steps in to the `<IS_HOME>/repository/conf/log4j2.properties` file:

1. Create a [Log4J2 Logger](https://logging.apache.org/log4j/2.x/manual/configuration.html#configuring-loggers) named `org-sample` mapped to the `org.sample` package:
   ```properties
   logger.org-sample.name = org.sample
   logger.org-sample.level = DEBUG
   ```
2. Add the new `org-sample` logger to the `loggers` variable:
   ```properties
   loggers = AUDIT_LOG, . . ., org-sample
   ```

### Debugging:

To debug this component while the Identity Server is running:

1. Run the Identity Server in debug mode:
   ```sh
   sh $IS_HOME/bin/wso2server.sh --debug 5005
   ```
2. Attach the JVM to your IDE:
    - For IntelliJ IDEA:
        - Go to `Run` > `Edit Configurations...`
        - Click on the `+` icon and select `Remote JVM Debug`.
        - Set the port to `5005` and click `OK`.
        - Add your breakpoints in the code.
        - Click on the green debug icon to start debugging.
    - For VSCode:
        - Go to the `Run` tab on the left sidebar.
        - Click on `create a launch.json file`.
        - Select `Java` and then select `Remote`.
        - Set the port to `5005` and click `OK`.
        - Add your breakpoints in the code.
        - Click on the green debug icon to start debugging.

_Note: The Identity Server's startup will be blocked until you connect the debugger._
