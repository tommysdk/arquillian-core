package org.jboss.arquillian.junit;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jboss.arquillian.api.TestResult;
import org.jboss.arquillian.api.TestResult.Status;
import org.jboss.arquillian.impl.TestResultImpl;
import org.junit.runner.JUnitCore;
import org.junit.runner.Request;
import org.junit.runner.Result;

public class ServletTestRunner extends HttpServlet
{

   private static final long serialVersionUID = 1L;

   @Override
   protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
   {
      String methodName = null;
      if (req.getParameter("methodName") != null)
      {
         methodName = req.getParameter("methodName");
      }
      Class<?> testClass = null;
      if (req.getParameter("className") != null)
      {
         String className = req.getParameter("className");
         try {
            testClass = Thread.currentThread().getContextClassLoader().loadClass(className);
         } catch (Exception e) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Could not load test class");
         }
      } else {
         resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "No class specified");
         return;
      }

      String outputMode;
      if (req.getParameter("outputMode") != null)
      {
         outputMode = (String) req.getParameter("outputMode");
      }
      else
      {
         outputMode = "html";
      }
      if (outputMode.equals("serializedObject") && methodName == null)
      {
         resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "No method name specified");
         return;
      }

      JUnitCore runner = new JUnitCore();
      Result result = runner.run(
            Request.method(
                  testClass, 
                  methodName));
      
      if (outputMode.equals("html"))
      {
         resp.setContentType("text/html");
         resp.setStatus(HttpServletResponse.SC_OK);
         PrintWriter writer = resp.getWriter();
         writer.write("<html>\n");
         writer.write("<head><title>TCK Report</title></head>\n");
         writer.write("<body>\n");
         writer.write("<h2>Configuration</h2>\n");
         writer.write("<table>\n");
         writer.write("<tr>\n");
         writer.write("<td><b>Method</b></td><td><b>Status</b></td>\n");
         writer.write("</tr>\n");

         writer.write("</table>\n");
         writer.write("<h2>Tests</h2>\n");
         writer.write("<table>\n");
         writer.write("<tr>\n");
         writer.write("<td><b>Method</b></td><td><b>Status</b></td>\n");
         writer.write("</tr>\n");

         writer.write("</table>\n");
         writer.write("</body>\n");
      }
      else if (outputMode.equals("serializedObject"))
      {
         ObjectOutputStream oos = new ObjectOutputStream(resp.getOutputStream());
         TestResult testResult = convertToTestResult(result);
         
         oos.writeObject(testResult);
         resp.setStatus(HttpServletResponse.SC_OK);
         oos.flush();
         oos.close();
      }
      else
      {
         resp.sendError(500, "No report format specified");
      }
   }
   
   private TestResult convertToTestResult(Result result) 
   {
      Status status = Status.PASSED;
      Throwable throwable = null;
      
      if(result.getFailureCount() > 0) 
      {
         status = Status.FAILED;
         throwable = result.getFailures().get(0).getException();
      }
      if(result.getIgnoreCount() > 0) 
      {
         status = Status.SKIPPED;
      }
      return new TestResultImpl(status, throwable);
   }
}