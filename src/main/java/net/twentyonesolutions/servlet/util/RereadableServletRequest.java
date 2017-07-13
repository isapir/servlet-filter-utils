/**
 * Copyright (c) 2016 Igal Sapir
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the
 * Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package net.twentyonesolutions.servlet.util;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

/* provided by org.apache.tomcat:tomcat-coyote */
import org.apache.tomcat.util.http.fileupload.FileItem;
import org.apache.tomcat.util.http.fileupload.disk.DiskFileItemFactory;
import org.apache.tomcat.util.http.fileupload.servlet.ServletFileUpload;
import org.apache.tomcat.util.http.fileupload.servlet.ServletRequestContext;


public class RereadableServletRequest extends HttpServletRequestWrapper {

    private byte[] rawData;
    private MultiMap params;

    public static final String MULTIPART_FORMDATA_TYPE    = "multipart/form-data";
    public static final String DEFAULT_CHARACTER_ENCODING = "utf-8";
    public static final String tmpDirPath;
    public static final File tmpDir;

    static {

        // set tmpDir once so that we don't need to do it with every request
        tmpDirPath = System.getProperty("java.io.tmpdir") + File.separator + "servlet-file-upload";
        tmpDir = new File(tmpDirPath);
        if (!tmpDir.exists() && !tmpDir.mkdirs()){
            throw new RuntimeException("Failed to create temp directory " + tmpDir.getAbsolutePath());
        }
        System.out.println(RereadableServletRequest.class.getSimpleName() + " loaded with temp directory " + tmpDir.getAbsolutePath());
    }


    public RereadableServletRequest(ServletRequest req) {

        super((HttpServletRequest)req);
    }

    /**
     * Returns the value of a request parameter as a <code>String</code>, or
     * <code>null</code> if the parameter does not exist. Request parameters are
     * extra information sent with the request. For HTTP servlets, parameters
     * are contained in the query string or posted form data.
     * <p>
     * You should only use this method when you are sure the parameter has only
     * one value. If the parameter might have more than one value, use
     * {@link #getParameterValues}.
     * <p>
     * If you use this method with a multivalued parameter, the value returned
     * is equal to the first value in the array returned by
     * <code>getParameterValues</code>.
     * <p>
     * If the parameter data was sent in the request body, such as occurs with
     * an HTTP POST request, then reading the body directly via
     * {@link #getInputStream} or {@link #getReader} can interfere with the
     * execution of this method.
     *
     * @param name
     *            a <code>String</code> specifying the name of the parameter
     * @return a <code>String</code> representing the single value of the
     *         parameter
     * @see #getParameterValues
     */
    @Override
    public String getParameter(String name){
        return getParams().getValue(name);
    }

    /**
     * Returns a java.util.Map of the parameters of this request. Request
     * parameters are extra information sent with the request. For HTTP
     * servlets, parameters are contained in the query string or posted form
     * data.
     *
     * @return an immutable java.util.Map containing parameter names as keys and
     *         parameter values as map values. The keys in the parameter map are
     *         of type String. The values in the parameter map are of type
     *         String array.
     */
    @Override
    public Map getParameterMap(){
        return getParams().getMap();
    }

    /**
     * Returns an <code>Enumeration</code> of <code>String</code> objects
     * containing the names of the parameters contained in this request. If the
     * request has no parameters, the method returns an empty
     * <code>Enumeration</code>.
     *
     * @return an <code>Enumeration</code> of <code>String</code> objects, each
     *         <code>String</code> containing the name of a request parameter;
     *         or an empty <code>Enumeration</code> if the request has no
     *         parameters
     */
    public Enumeration<String> getParameterNames(){

        return Collections.enumeration(getParams().getMap().keySet());
    }

    /**
     * Returns an array of <code>String</code> objects containing all of the
     * values the given request parameter has, or <code>null</code> if the
     * parameter does not exist.
     * <p>
     * If the parameter has a single value, the array has a length of 1.
     *
     * @param name
     *            a <code>String</code> containing the name of the parameter
     *            whose value is requested
     * @return an array of <code>String</code> objects containing the parameter's
     *         values
     * @see #getParameter
     */
    public String[] getParameterValues(String name){

        List list = getParams().getValues(name);
        List<String> listOfStrings = new ArrayList(list.size());
        for (Object o : list)
            listOfStrings.add(o.toString());

        return listOfStrings.toArray(new String[listOfStrings.size()]);
    }

    /**
     * Retrieves the body of the request as binary data using
     * a {@link ServletInputStream}.  Either this method or
     * {@link #getReader} may be called to read the body, not both.
     *
     * @return a {@link ServletInputStream} object containing
     * the body of the request
     *
     * @exception IllegalStateException if the {@link #getReader} method
     * has already been called for this request
     *
     * @exception IOException if an input or output exception occurred
     */
    @Override
    public ServletInputStream getInputStream() throws IOException {

        if (rawData == null)
            initialize();

        return new ByteArrayServletInputStream(rawData);
    }

    /**
     * Retrieves the body of the request as character data using
     * a <code>BufferedReader</code>.  The reader translates the character
     * data according to the character encoding used on the body.
     * Either this method or {@link #getInputStream} may be called to read the
     * body, not both.
     *
     * @return a <code>BufferedReader</code> containing the body of the request
     *
     * @exception UnsupportedEncodingException  if the character set encoding
     * used is not supported and the text cannot be decoded
     *
     * @exception IllegalStateException if {@link #getInputStream} method
     * has been called on this request
     *
     * @exception IOException if an input or output exception occurred
     *
     * @see #getInputStream
     */
    @Override
    public BufferedReader getReader() throws IOException {

        if (rawData == null)
            initialize();

        return new BufferedReader(new InputStreamReader(new ByteArrayServletInputStream(rawData)));
    }

    /**
     * This method ensures that params was initialized before returning it
     *
     * @return params
     */
    private MultiMap getParams(){

        if (rawData == null)
            initialize();

        return this.params;
    }

    /**
     * This method reads the body of the original request and stores it in rawData.  It also initializes params and must
     * be called before any call that accesses params.  It is therefore advised to access params via <code>getParams()</code>
     * which calls this method if needed.
     */
    private void initialize(){

        if (rawData != null)
            return;

        ServletRequest request = super.getRequest();

        try {

            rawData = ServletUtils.readBytes(request.getInputStream());

            String encoding = request.getCharacterEncoding();
            if (encoding == null)
                encoding = DEFAULT_CHARACTER_ENCODING;

            String contentType = request.getContentType();
            boolean isMultiPart = (contentType != null) && contentType.startsWith(MULTIPART_FORMDATA_TYPE);

            if (isMultiPart) {

                params = new MultiMap();

                DiskFileItemFactory factory = new DiskFileItemFactory();
                factory.setSizeThreshold(64 * 1024);            // files larger than Threshold will be saved to tmpDir on disk
                factory.setRepository(tmpDir);

                ServletFileUpload upload = new ServletFileUpload(factory);
                upload.setSizeMax(10 * 1024 * 1024);            // files larger than Max will throw a FileUploadException

                ServletRequestContext uploadContext = new ServletRequestContext(this);
                List<FileItem> fileItems = upload.parseRequest(uploadContext);

                for (FileItem fileItem : fileItems){

                    if (fileItem.isFormField()){
                        this.params.add(fileItem.getFieldName(), fileItem.getString(encoding));
                    }
                    else {
                        this.params.add(fileItem.getFieldName(), fileItem);
//                        System.out.println(">>>> " + fileItem.getFieldName());
//                        System.out.println(">>>> " + fileItem);
                    }
                }
            }
            else {

                params = ServletUtils.decodeUrlString(ServletUtils.readText(this.getReader()), encoding);
            }
        }
        catch (Exception ex){

            ex.printStackTrace();
        }
    }


    /**
     * This class wraps the rawData to provide an InputStream that is returned from <code>getInputStream()</code> and <code>getReader()</code>.
     *
     * It uses the byte array that was read from the original request in <code>initialize()</code>.
     */
    private class ByteArrayServletInputStream extends ServletInputStream {

        private final ByteArrayInputStream inputStream;

        public ByteArrayServletInputStream(byte[] data){
            inputStream = new ByteArrayInputStream(data);
        }

        /**
         * Reads the next byte of data from the input stream. The value byte is
         * returned as an <code>int</code> in the range <code>0</code> to
         * <code>255</code>. If no byte is available because the end of the stream
         * has been reached, the value <code>-1</code> is returned. This method
         * blocks until input data is available, the end of the stream is detected,
         * or an exception is thrown.
         *
         * <p> A subclass must provide an implementation of this method.
         *
         * @return     the next byte of data, or <code>-1</code> if the end of the
         *             stream is reached.
         * @exception  IOException  if an I/O error occurs.
         */
        @Override
        public int read() throws IOException {
            return inputStream.read();
        }

        /**
         * Returns true when all the data from the inputStream has been read else
         * it returns false.
         *
         * @return <code>true</code> when all data for this particular request
         * has been read, otherwise returns <code>false</code>.
         * @since Servlet 3.1
         */
        @Override
        public boolean isFinished() {
            return inputStream.available() == 0;
        }

        /**
         * Returns true if data can be read without blocking else returns
         * false.
         *
         * @return <code>true</code> if data can be obtained without blocking,
         * otherwise returns <code>false</code>.
         * @since Servlet 3.1
         */
        @Override
        public boolean isReady() {
            return inputStream.available() > 0;
        }

        /**
         * Instructs the <code>ServletInputStream</code> to invoke the provided
         * {@link ReadListener} when it is possible to read
         *
         * @param readListener the {@link ReadListener} that should be notified
         *                     when it's possible to read.
         * @throws IllegalStateException if one of the following conditions is true
         *                               <ul>
         *                               <li>the associated request is neither upgraded nor the async started
         *                               <li>setReadListener is called more than once within the scope of the same request.
         *                               </ul>
         * @throws NullPointerException  if readListener is null
         * @since Servlet 3.1
         */
        @Override
        public void setReadListener(ReadListener readListener) {

            try {
                readListener.onDataAvailable();
            }
            catch (IOException ex){}
        }
    }


}