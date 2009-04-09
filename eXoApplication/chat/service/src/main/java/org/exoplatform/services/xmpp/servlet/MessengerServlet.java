/**
 * Copyright (C) 2003-2007 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.services.xmpp.servlet;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.rest.Connector;
import org.exoplatform.services.rest.MultivaluedMetadata;
import org.exoplatform.services.rest.ResourceDispatcher;
import org.exoplatform.services.rest.Response;
import org.exoplatform.services.rest.servlet.RequestFactory;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:vitaly.parfonov@gmail.com">Vitaly Parfonov</a>
 * @version $Id: $
 */
public class MessengerServlet extends HttpServlet implements Connector {

  /**
   * Generated by eclipse.
   */
  private static final long serialVersionUID = -5976447080959500615L;

  /**
   * 
   */
  private static final Log  LOGGER           = ExoLogger.getLogger("MessangerServlet");

  /**
   * 
   */
  private int               connectionTimeout;

  /**
   * 
   */
  private int               timeChekEvent;

  @Override
  public void init() throws ServletException {
    final String timeOut = this.getInitParameter("connection-timeout");
    final String timeChekEventStr = this.getInitParameter("time-check-event");
    if (timeOut != null)
      connectionTimeout = Integer.parseInt(timeOut);
    else {
      LOGGER.info("Connection timeout is not set, default 60000 ms");
      connectionTimeout = 60 * 1000;
    }
    if (timeChekEventStr != null)
      timeChekEvent = Integer.parseInt(timeChekEventStr);
    else {
      LOGGER.info("Timeout for cheking event is not set, default 3000 ms");
      timeChekEvent = 3 * 1000;
    }
  }

  @Override
  public void service(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException,
                                                                                       ServletException {
    httpRequest.setCharacterEncoding("UTF-8");
    ExoContainer container = ExoContainerContext.getCurrentContainer();
    LOGGER.debug("Current Container: " + container);
    ResourceDispatcher dispatcher = (ResourceDispatcher) container.getComponentInstanceOfType(ResourceDispatcher.class);
    LOGGER.debug("ResourceDispatcher: " + dispatcher);
    if (dispatcher == null) {
      throw new ServletException("ResourceDispatcher is null.");
    }
    try {
      /*
       * boolean waitingResponse; try { waitingResponse = new
       * Boolean(httpRequest.getParameter("waiting-response")); } catch
       * (Exception e) { waitingResponse = false; } if (waitingResponse) { //
       * long connection OutputStream out = httpResponse.getOutputStream(); long
       * start = System.currentTimeMillis(); long timeOut = start +
       * connectionTimeout; while (true) { Response response =
       * dispatcher.dispatch(RequestFactory .createRequest(httpRequest)); if
       * (response.getStatus() != HTTPStatus.NOT_MODIFIED) {
       * httpResponse.setStatus(response.getStatus());
       * tuneResponse(httpResponse, response.getResponseHeaders());
       * response.writeEntity(out); out.flush(); out.close(); break; } if
       * (System.currentTimeMillis() > timeOut) { httpResponse.setStatus(200);
       * // NO_CONETNT LOGGER.debug("Timeout " + System.currentTimeMillis());
       * break; } Thread.sleep(timeChekEvent); } } else { // simple connection
       */
      OutputStream out = httpResponse.getOutputStream();
      Response response = dispatcher.dispatch(RequestFactory.createRequest(httpRequest));
      httpResponse.setStatus(response.getStatus());
      tuneResponse(httpResponse, response.getResponseHeaders());
      response.writeEntity(out);
      out.flush();
      out.close();
      // }
    } catch (Exception e) {
      LOGGER.error("dispatch method error!");
      e.printStackTrace();
      httpResponse.sendError(500, "This request can't be serve by service.\n"
          + "Check request parameters and try again.");
    }
  }

  /**
   * Tune HTTP response.
   * 
   * @param httpResponse HTTP response.
   * @param responseHeaders HTTP response headers.
   */
  private void tuneResponse(HttpServletResponse httpResponse, MultivaluedMetadata responseHeaders) {
    if (responseHeaders != null) {
      HashMap<String, String> headers = responseHeaders.getAll();
      Set<String> keys = headers.keySet();
      Iterator<String> ikeys = keys.iterator();
      while (ikeys.hasNext()) {
        String key = ikeys.next();
        httpResponse.setHeader(key, headers.get(key));
      }
    }
  }

}