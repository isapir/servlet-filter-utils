# servlet-filter-utils
This mini-project provides utility classes and methods for Servlets and Filters

Specifically, it adds the class `net.twentyonesolutions.servlet.util.RereadableServletRequest` which allows Servlet filters 
to inspect the body of an incoming Request without destroying the Request's body.

To use it, simply wrap the incoming Request with `RereadableServletRequest` and use the wrapper to retrieve the Request's body
and pass it along the Filter Chain.

The project uses https://mvnrepository.com/artifact/org.apache.tomcat/tomcat-coyote for parsing the Request's body in case of 
a form/multi-part Request.  If you are using Tomcat, then this library should already be in your classpath and you do not need
anything else.  If you are not using Tomcat, simply add the library to your classpath.
