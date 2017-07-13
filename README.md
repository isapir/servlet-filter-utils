# servlet-filter-utils
This mini-project provides utility classes and methods for Servlets and Filters

### RereadableServletRequestFilter


Specifically, it adds the `RereadableServletRequestFilter` which allows Servlets and Filters to inspect the body of an incoming Request without destroying the Request's body.

To use it, simply add the following to the Web Descriptor, web.xml:

    <filter>
      <filter-name>RereadableServletRequestFilter</filter-name>
      <filter-class>net.twentyonesolutions.servlet.filter.RereadableServletRequestFilter</filter-class>
    </filter>

    <filter-mapping>
      <filter-name>RereadableServletRequestFilter</filter-name>
      <url-pattern>/*</url-pattern>
    </filter-mapping>

This filter uses https://mvnrepository.com/artifact/org.apache.tomcat/tomcat-coyote for parsing the Request's body in case of 
a form/multi-part Request.  If you are using Tomcat, then this library should already be in your classpath and you do not need
anything else.  If you are not using Tomcat, simply add the library to your classpath.

### HttpSessionInitializerFilter

The `HttpSessionInitializerFilter` simply calls the servlet request's `getSession()` method, which initializes the HttpSession if it is not already initialized.

This is required if you want to use the HttpSession object in WebSockets a-la JSR-356, which returns `null` for `getSession()` if the HttpSession is not initialized beforehand.

To use it, simply add the following to your Web Descriptor, web.xml, and set the URL pattern appropriately.  The example below targets URLs that start with `/ws/`:

    <filter>
      <filter-name>HttpSessionInitializerFilter</filter-name>
      <filter-class>net.twentyonesolutions.servlet.filter.HttpSessionInitializerFilter</filter-class>
    </filter>

    <filter-mapping>
      <filter-name>HttpSessionInitializerFilter</filter-name>
      <url-pattern>/ws/*</url-pattern>
    </filter-mapping>
