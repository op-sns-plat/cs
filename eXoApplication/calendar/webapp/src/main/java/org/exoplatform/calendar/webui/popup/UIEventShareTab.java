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
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.exoplatform.calendar.webui.popup.UIEventForm.ParticipantStatus;
import org.exoplatform.commons.utils.ObjectPageList;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIGrid;
import org.exoplatform.webui.core.UIPageIterator;
import org.exoplatform.webui.core.lifecycle.Lifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormInputWithActions;
import org.exoplatform.webui.form.UIFormRadioBoxInput;

/**
 * Created by The eXo Platform SARL
 * Author : Pham Tuan
 *          tuan.pham@exoplatform.com
 * Aug 29, 2007  
 */

@ComponentConfig(
                 template = "app:/templates/calendar/webui/UIPopup/UIEventShareTab.gtmpl",
                 events = {
                     @EventConfig(listeners = UIEventShareTab.ShowPageActionListener.class, phase = Phase.PROCESS),
                     @EventConfig(listeners = UIEventForm.DeleteActionListener.class, confirm = "UIEventForm.msg.confirm-delete")
                 }
) 
public class UIEventShareTab extends UIFormInputWithActions {

  public static String[] BEAN_FIELD = {"participant","status"};
  private static String[] ACTION = {"Delete"} ;
  final public static String FIELD_SHARE = "shareEvent".intern() ;
  final public static String FIELD_STATUS = "status".intern() ;
  final public static String FIELD_SEND = "send".intern();
  final public static String FIELD_INFO =  "info".intern() ;
  final public static String FIELD_ANSWER = "answer".intern() ;
  private Map<String, List<ActionData>> actionField_ = new HashMap<String, List<ActionData>>() ;
  
  public UIEventShareTab(String id) throws Exception {
    super(id);
    setComponentConfig(getClass(), null) ;
    UIGrid categoryList = addChild(UIGrid.class, null , "UIParticipantList") ;
    categoryList.configure("participant", BEAN_FIELD, ACTION) ;
    categoryList.getUIPageIterator().setId("ParticipantListIterator");
    setParticipantStatusList(new LinkedList<ParticipantStatus>()) ;
  }
  protected UIForm getParentFrom() {
    return (UIForm)getParent() ;
  }
  
  public UIFormRadioBoxInput getUIFormRadioBoxInput (String id) {
    return findComponentById(id);
  }
  public Map<String, String> getParticipantStatus() {
    return ((UIEventForm) getParent()).participantStatus_ ;
  }
  public void setActionField(String fieldName, List<ActionData> actions) throws Exception {
    actionField_.put(fieldName, actions) ;
  }
  public List<ActionData> getActionField(String fieldName) {return actionField_.get(fieldName) ;}
  
  public List<ParticipantStatus> getData() throws Exception {
    return new LinkedList<ParticipantStatus>(getChild(UIGrid.class).getUIPageIterator().getCurrentPageData());
  }
  
  public UIPageIterator  getUIPageIterator() {  return getChild(UIGrid.class).getUIPageIterator() ; }
  
  public long getAvailablePage(){ return getChild(UIGrid.class).getUIPageIterator().getAvailablePage() ;}
  
  public long getCurrentPage() { return getChild(UIGrid.class).getUIPageIterator().getCurrentPage();}
  
  public void setParticipantStatusList(List<ParticipantStatus> participantStatusList) throws Exception {
    ObjectPageList objPageList = new ObjectPageList(participantStatusList, 10) ;
    getChild(UIGrid.class).getUIPageIterator().setPageList(objPageList) ;
  }
  protected void updateCurrentPage(int page) throws Exception{
    getChild(UIGrid.class).getUIPageIterator().setCurrentPage(page) ;
  }
  static  public class ShowPageActionListener extends EventListener<UIEventShareTab> {
    public void execute(Event<UIEventShareTab> event) throws Exception {
      UIEventShareTab uiEventShareTab = event.getSource() ;
      int page = Integer.parseInt(event.getRequestContext().getRequestParameter(OBJECTID)) ;
      uiEventShareTab.updateCurrentPage(page) ; 
      event.getRequestContext().addUIComponentToUpdateByAjax(uiEventShareTab);           
    }
  }
  
 
}