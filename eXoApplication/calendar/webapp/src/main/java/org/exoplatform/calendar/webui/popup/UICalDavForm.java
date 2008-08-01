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
package org.exoplatform.calendar.webui.popup;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.exoplatform.calendar.CalendarUtils;
import org.exoplatform.calendar.service.Calendar;
import org.exoplatform.calendar.service.CalendarService;
import org.exoplatform.calendar.service.RssData;
import org.exoplatform.calendar.webui.UICalendarPortlet;
import org.exoplatform.calendar.webui.UIFormDateTimePicker;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.portal.webui.util.SessionProviderFactory;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.form.UIFormCheckBoxInput;
import org.exoplatform.webui.form.UIFormInputInfo;
import org.exoplatform.webui.form.UIFormInputWithActions;
import org.exoplatform.webui.form.UIFormStringInput;
import org.exoplatform.webui.form.UIFormTabPane;
import org.exoplatform.webui.form.UIFormTextAreaInput;
import org.exoplatform.webui.form.validator.MandatoryValidator;

/**
 * Created by The eXo Platform SARL
 * Author : Hung Nguyen
 *          hung.nguyen@exoplatform.com
 * Aus 01, 2007 2:48:18 PM 
 */
@ComponentConfig(
    lifecycle = UIFormLifecycle.class,
    template = "system:/groovy/webui/form/UIFormTabPane.gtmpl",
    events = {
      @EventConfig(listeners = UICalDavForm.GenerateActionListener.class),      
      @EventConfig(listeners = UICalDavForm.CancelActionListener.class, phase=Phase.DECODE)
    }
)
public class UICalDavForm extends UIFormTabPane implements UIPopupComponent{
  final static private String URL = "url".intern() ;
  final static private String DESCRIPTION = "description".intern() ;
  final static private String COPYRIGHT = "copyright".intern() ;
  final static private String TITLE = "title".intern() ;
  final static private String PUBLIC_DATE = "pubDate".intern() ;
  final static private String INFOR = "info".intern() ;
  final static private String MESSAGE = "message".intern() ;
  final static private String DESCRIPTIONS = "descriptions".intern() ;
  final static private String COPYRIGHTS = "copyrights".intern() ;
  final static private String INFO_TAB = "rssInfo".intern() ;
  final static private String CALENDAR_TAB = "rssCalendars".intern() ;
  public UICalDavForm() throws Exception{
    super("UICalDavForm");
    CalendarService calendarService = CalendarUtils.getCalendarService() ;
    String username = Util.getPortalRequestContext().getRemoteUser() ;
    UIFormInputWithActions rssInfo = new UIFormInputWithActions(INFO_TAB) ;
    rssInfo.addUIFormInput(new UIFormStringInput(TITLE, TITLE, "eXoCalendarCalDav").addValidator(MandatoryValidator.class)) ;
    String url = calendarService.getCalendarSetting(SessionProviderFactory.createSessionProvider(), username).getBaseURL();
    if(CalendarUtils.isEmpty(url)) url = CalendarUtils.getServerBaseUrl() + "calendar/iCalRss" ;
    rssInfo.addUIFormInput(new UIFormStringInput(URL, URL, url).addValidator(MandatoryValidator.class)) ;
    rssInfo.addUIFormInput(new UIFormTextAreaInput(DESCRIPTION, DESCRIPTION, null).addValidator(MandatoryValidator.class)) ;
    rssInfo.addUIFormInput(new UIFormStringInput(COPYRIGHT, COPYRIGHT, null).addValidator(MandatoryValidator.class)) ;
    rssInfo.addUIFormInput(new UIFormDateTimePicker(PUBLIC_DATE, PUBLIC_DATE, new Date(), false)) ;
    setSelectedTab(rssInfo.getId()) ;
    addUIFormInput(rssInfo) ;
    UIFormInputWithActions rssCalendars = new UIFormInputWithActions(CALENDAR_TAB) ;
    rssCalendars.addUIFormInput(new UIFormInputInfo(INFOR,INFOR, null)) ; 
    List<Calendar> calendars = calendarService.getUserCalendars(SessionProviderFactory.createSessionProvider(), username, true) ;
    for(Calendar calendar : calendars) {
      rssCalendars.addUIFormInput(new UIFormCheckBoxInput<Boolean>(calendar.getName(), calendar.getId(), true)) ;
    }
    addUIFormInput(rssCalendars) ;
  }
  public void init() throws Exception{
    UIFormInputWithActions rssInfo = getChildById("rssInfo") ;
    rssInfo.getUIFormTextAreaInput(DESCRIPTION).setValue(getLabel(DESCRIPTIONS)) ;
    rssInfo.getUIStringInput(COPYRIGHT).setValue(getLabel(COPYRIGHTS)) ;
    UIFormInputWithActions rssTab = getChildById("rssCalendars") ;
    rssTab.getUIFormInputInfo(INFOR).setValue(getLabel(MESSAGE)) ;
  }
  public void activate() throws Exception {}
  public void deActivate() throws Exception {}
  
  public String getWebDAVServerPrefix() throws Exception {    
   String prefixWebDAV = CalendarUtils.getServerBaseUrl() + 
           getPortalName() + "/rest/jcr/"+getRepository().getConfiguration().getName() +"/" + getWorkspaceName()  ;
    return prefixWebDAV ;
  }
  public String getPortalName() {
    PortalContainer pcontainer =  PortalContainer.getInstance() ;
    return pcontainer.getPortalContainerInfo().getContainerName() ;  
  }
  public String getWorkspaceName() throws Exception {
    return getRepository().getConfiguration().getDefaultWorkspaceName() ;
  }
  private ManageableRepository getRepository() throws Exception{         
    RepositoryService repositoryService  = getApplicationComponent(RepositoryService.class) ;      
    return repositoryService.getCurrentRepository() ;
  }
  private UIFormDateTimePicker getUIDateTimePicker(String id) {
    UIFormInputWithActions rssInfo = getChildById("rssInfo") ;
    return rssInfo.getChildById(id) ;
  }
  static  public class GenerateActionListener extends EventListener<UICalDavForm> {
    public void execute(Event<UICalDavForm> event) throws Exception {
      UICalDavForm uiForm = event.getSource() ;
      UIApplication uiApp = uiForm.getAncestorOfType(UIApplication.class) ;
      CalendarService calendarService = CalendarUtils.getCalendarService();
      UIFormInputWithActions rssCalendars = uiForm.getChildById("rssCalendars") ;
      List<UIComponent> children = rssCalendars.getChildren() ;
      List<String> calendarIds = new ArrayList<String> () ;
      for(UIComponent child : children) {
        if(child instanceof UIFormCheckBoxInput) {
          if(((UIFormCheckBoxInput)child).isChecked()) {
            calendarIds.add(((UIFormCheckBoxInput)child).getBindingField()) ;
          }
        }
      }
      if(calendarIds.size() <= 0) {
        uiForm.setSelectedTab(CALENDAR_TAB) ;
        event.getRequestContext().addUIComponentToUpdateByAjax(uiForm.getParent()) ;
        uiApp.addMessage(new ApplicationMessage("UIRssForm.msg.there-is-not-calendar", null)) ;
        event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
        return ;
      }
      RssData rssData = new RssData() ;
      String tempName = uiForm.getUIStringInput(UICalDavForm.TITLE).getValue() ;
      if(tempName != null && tempName.length() > 0) {
        if(!CalendarUtils.isNameValid(tempName, CalendarUtils.SPECIALCHARACTER)) {
          uiForm.setSelectedTab(INFO_TAB) ;
          event.getRequestContext().addUIComponentToUpdateByAjax(uiForm.getParent()) ;
          uiApp.addMessage(new ApplicationMessage("UIRssForm.msg.feed-name-invalid", null)) ;
          event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
          return ;
        }
        if(tempName.length() > 4 && tempName.substring(tempName.length() - 4).equals(".rss")) rssData.setName(tempName);
        else rssData.setName(tempName + ".rss") ;
      }else {
        rssData.setName("eXoCalendar.rss") ;
      }      
      rssData.setUrl(uiForm.getUIStringInput(UICalDavForm.URL).getValue()) ;
      rssData.setDescription(uiForm.getUIStringInput(UICalDavForm.DESCRIPTION).getValue()) ;
      rssData.setCopyright(uiForm.getUIStringInput(UICalDavForm.COPYRIGHT).getValue()) ;
      rssData.setLink(uiForm.getWebDAVServerPrefix()) ;
      String title = uiForm.getUIStringInput(UICalDavForm.TITLE).getValue() ;
      rssData.setTitle(title) ;
      rssData.setVersion("rss_2.0") ;
      if(uiForm.getUIDateTimePicker(UICalDavForm.PUBLIC_DATE).getCalendar() != null)
      rssData.setPubDate(uiForm.getUIDateTimePicker(UICalDavForm.PUBLIC_DATE).getCalendar().getTime()) ;
      int result = calendarService.generateCalDav(SessionProviderFactory.createSystemProvider(), Util.getPortalRequestContext().getRemoteUser(), calendarIds, rssData) ;
      if(result < 0) {
        uiApp.addMessage(new ApplicationMessage("UIRssForm.msg.no-data-generated", null)) ;
        event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
        return ;
      }
      UICalendarPortlet calendarPortlet = uiForm.getAncestorOfType(UICalendarPortlet.class) ;
      calendarPortlet.cancelAction() ;  
      Object[] object = new Object[]{title} ;
      uiApp.addMessage(new ApplicationMessage("UIRssForm.msg.feed-has-been-generated", object)) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
    }
  }

  static  public class CancelActionListener extends EventListener<UICalDavForm> {
    public void execute(Event<UICalDavForm> event) throws Exception {
      UICalDavForm uiForm = event.getSource() ;
      UICalendarPortlet calendarPortlet = uiForm.getAncestorOfType(UICalendarPortlet.class) ;
      calendarPortlet.cancelAction() ;
    }
  }  
}
