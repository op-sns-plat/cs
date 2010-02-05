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
 **/
package org.exoplatform.chatbar.webui;

import javax.portlet.PortletRequest;

import org.exoplatform.container.PortalContainer;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.cometd.EXoCSContinuationBayeux;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.application.portlet.PortletRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.UIPortletApplication;
import org.exoplatform.webui.core.lifecycle.UIApplicationLifecycle;
import org.exoplatform.ws.frameworks.cometd.ContinuationService;
import org.mortbay.cometd.AbstractBayeux;

/**
 * Author : pham tuan
 *          tuan.pham@exoplatform.com
 * May 04, 2009
 */
@ComponentConfig(
   lifecycle = UIApplicationLifecycle.class, 
   template = "app:/templates/chatbar/webui/UIChatBarPortlet.gtmpl"

)
public class UIChatBarPortlet extends UIPortletApplication {
  private String windowId; 
  public UIChatBarPortlet() throws Exception {
    PortletRequestContext context = (PortletRequestContext)  WebuiRequestContext.getCurrentInstance() ;
    PortletRequest prequest = context.getRequest() ;
    windowId = prequest.getWindowID() ;
  }
  
  public String getId() {
    return windowId ;
  }
  
  public String getRemoteUser() {
    return Util.getPortalRequestContext().getRemoteUser() ;
  }
  
  public String getUserToken() {
	  try {
    return this.getContinuationService().getUserToken(this.getRemoteUser());
	  } catch (Exception e) {
		  System.out.println("\n\n can not get UserToken");
		  return "" ;
	  }
  }
  
  protected ContinuationService getContinuationService() {
    ContinuationService continuation = (ContinuationService) PortalContainer.getInstance().getComponentInstanceOfType(ContinuationService.class);
    return continuation;
  }
  
  protected String getRestContextName() {
    return PortalContainer.getInstance().getRestContextName();
  }
  
  protected String getCometdContextName() {
    String cometdContextName = "cometd";
    try {
      EXoCSContinuationBayeux bayeux = (EXoCSContinuationBayeux) PortalContainer.getInstance()
                                                                                .getComponentInstanceOfType(AbstractBayeux.class);
      return (bayeux == null ? "cometd" : bayeux.getCometdContextName());
    } catch (Exception e) {
    }
    return cometdContextName;
  }
}
