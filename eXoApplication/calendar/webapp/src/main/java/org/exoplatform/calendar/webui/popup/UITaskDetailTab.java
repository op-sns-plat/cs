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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.exoplatform.calendar.CalendarUtils;
import org.exoplatform.calendar.SessionsUtils;
import org.exoplatform.calendar.service.Attachment;
import org.exoplatform.calendar.service.Calendar;
import org.exoplatform.calendar.service.CalendarEvent;
import org.exoplatform.calendar.service.CalendarService;
import org.exoplatform.calendar.webui.UIFormComboBox;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.model.SelectItemOption;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormCheckBoxInput;
import org.exoplatform.webui.form.UIFormDateTimeInput;
import org.exoplatform.webui.form.UIFormInputInfo;
import org.exoplatform.webui.form.UIFormInputWithActions;
import org.exoplatform.webui.form.UIFormSelectBox;
import org.exoplatform.webui.form.UIFormStringInput;
import org.exoplatform.webui.form.UIFormTextAreaInput;

/**
 * Created by The eXo Platform SARL
 * Author : Pham Tuan
 *          tuan.pham@exoplatform.com
 * Aug 29, 2007  
 */

@ComponentConfig(
    template = "app:/templates/calendar/webui/UIPopup/UITaskDetailTab.gtmpl"
) 
public class UITaskDetailTab extends UIFormInputWithActions {

  final public static String FIELD_EVENT = "eventName".intern() ;
  final public static String FIELD_CALENDAR = "calendar".intern() ;
  final public static String FIELD_CATEGORY = "category".intern() ;
  final public static String FIELD_FROM = "from".intern() ;
  final public static String FIELD_TO = "to".intern() ;
  final public static String FIELD_FROM_TIME = "fromTime".intern() ;
  final public static String FIELD_TO_TIME = "toTime".intern() ;

  final public static String FIELD_CHECKALL = "allDay".intern() ;
  // final public static String FIELD_REPEAT = "repeat".intern() ;
  final public static String FIELD_DELEGATION = "delegation".intern() ;

  final public static String FIELD_PRIORITY = "priority".intern() ; 
  final public static String FIELD_DESCRIPTION = "description".intern() ;
  final public static String FIELD_STATUS = "status".intern() ;
  final static public String FIELD_ATTACHMENTS = "attachments".intern() ;
  
  protected List<Attachment> attachments_ = new ArrayList<Attachment>() ;
  private Map<String, List<ActionData>> actionField_ ;
  public UITaskDetailTab(String arg0) throws Exception {
    super(arg0);
    setComponentConfig(getClass(), null) ;
    actionField_ = new HashMap<String, List<ActionData>>() ;
    //CalendarSetting calendarSetting = getAncestorOfType(UICalendarPortlet.class).getCalendarSetting() ;
    List<SelectItemOption<String>> options = new ArrayList<SelectItemOption<String>>() ;
    addUIFormInput(new UIFormStringInput(FIELD_EVENT, FIELD_EVENT, null)) ;
    addUIFormInput(new UIFormTextAreaInput(FIELD_DESCRIPTION, FIELD_DESCRIPTION, null)) ;
    addUIFormInput(new UIFormSelectBox(FIELD_CALENDAR, FIELD_CALENDAR, null)) ;
    addUIFormInput(new UIFormSelectBox(FIELD_CATEGORY, FIELD_CATEGORY, UIEventForm.getCategory())) ;
    addUIFormInput(new UIFormSelectBox(FIELD_STATUS, FIELD_STATUS, getStatus())) ;

    ActionData addCategoryAction = new ActionData() ;
    addCategoryAction.setActionType(ActionData.TYPE_ICON) ;
    addCategoryAction.setActionName(UIEventForm.ACT_ADDCATEGORY) ;
    addCategoryAction.setActionListener(UIEventForm.ACT_ADDCATEGORY) ;
    List<ActionData> addCategoryActions = new ArrayList<ActionData>() ;
    addCategoryActions.add(addCategoryAction) ;
    setActionField(FIELD_CATEGORY, addCategoryActions) ;

    addUIFormInput(new UIFormInputInfo(FIELD_ATTACHMENTS, FIELD_ATTACHMENTS, null)) ;
    setActionField(FIELD_ATTACHMENTS, getUploadFileList()) ;

    addUIFormInput(new UIFormDateTimeInput(FIELD_FROM, FIELD_FROM, new Date(), false));
    
    addUIFormInput(new UIFormComboBox(FIELD_FROM_TIME, FIELD_FROM_TIME, options));
    addUIFormInput(new UIFormComboBox(FIELD_TO_TIME, FIELD_TO_TIME,  options));
   
    addUIFormInput(new UIFormDateTimeInput(FIELD_TO, FIELD_TO, new Date(), false));
    addUIFormInput(new UIFormCheckBoxInput<Boolean>(FIELD_CHECKALL, FIELD_CHECKALL, null));
    addUIFormInput(new UIFormStringInput(FIELD_DELEGATION, FIELD_DELEGATION, null));
    // addUIFormInput(new UIFormSelectBox(FIELD_REPEAT, FIELD_REPEAT, getRepeater())) ;
    addUIFormInput(new UIFormSelectBox(FIELD_PRIORITY, FIELD_PRIORITY, getPriority())) ;

    ActionData addEmailAddress = new ActionData() ;
    addEmailAddress.setActionType(ActionData.TYPE_ICON) ;
    addEmailAddress.setActionName(UITaskForm.ACT_ADDEMAIL) ;
    addEmailAddress.setActionListener(UITaskForm.ACT_ADDEMAIL) ;

    List<ActionData> addMailActions = new ArrayList<ActionData>() ;
    addMailActions.add(addEmailAddress) ;


    ActionData selectUser = new ActionData() ;
    selectUser.setActionType(ActionData.TYPE_ICON) ;
    addEmailAddress.setActionName(UITaskForm.ACT_SELECTUSER) ;
    selectUser.setActionListener(UITaskForm.ACT_SELECTUSER) ;

    List<ActionData> selectUsers = new ArrayList<ActionData>() ;
    selectUsers.add(selectUser) ;
    setActionField(FIELD_DELEGATION, selectUsers) ;

  }
  
  private List<SelectItemOption<String>> getStatus() {
    List<SelectItemOption<String>> status = new ArrayList<SelectItemOption<String>>() ;
    for(String taskStatus : CalendarEvent.TASK_STATUS) {
      status.add(new SelectItemOption<String>(taskStatus, taskStatus)) ;
    }
    return status ;
  }
  protected UIForm getParentFrom() {
    return (UIForm)getParent() ;
  }

  public List<ActionData> getUploadFileList() { 
    List<ActionData> uploadedFiles = new ArrayList<ActionData>() ;
    for(Attachment attachdata : attachments_) {
      ActionData fileUpload = new ActionData() ;
      fileUpload.setActionListener("") ;
      fileUpload.setActionType(ActionData.TYPE_ICON) ;
      fileUpload.setCssIconClass("AttachmentIcon ZipFileIcon") ;
      fileUpload.setActionName(attachdata.getName() + " ("+attachdata.getSize()+" Kb)" ) ;
      fileUpload.setShowLabel(true) ;
      uploadedFiles.add(fileUpload) ;
      ActionData removeAction = new ActionData() ;
      removeAction.setActionListener(UIEventForm.ACT_REMOVE) ;
      removeAction.setActionName(UIEventForm.ACT_REMOVE);
      removeAction.setActionParameter(attachdata.getId());
      removeAction.setActionType(ActionData.TYPE_LINK) ;
      removeAction.setBreakLine(true) ;
      uploadedFiles.add(removeAction) ;
    }
    return uploadedFiles ;
  }

  public void addToUploadFileList(Attachment attachfile) {
    attachments_.add(attachfile) ;
  }
  public void removeFromUploadFileList(Attachment attachfile) {
    attachments_.remove(attachfile);
  }  
  public void refreshUploadFileList() throws Exception {
    setActionField(FIELD_ATTACHMENTS, getUploadFileList()) ;
  }
  protected List<Attachment> getAttachments() { 
    return attachments_ ;
  }
  protected void setAttachments(List<Attachment> attachment) { 
    attachments_ = attachment ;
  }

  private List<SelectItemOption<String>> getCalendar() throws Exception {
    List<SelectItemOption<String>> options = new ArrayList<SelectItemOption<String>>() ;
    CalendarService calendarService = CalendarUtils.getCalendarService() ;
    String username = Util.getPortalRequestContext().getRemoteUser() ;
    List<Calendar> calendars = calendarService.getUserCalendars(SessionsUtils.getSessionProvider(), username, true) ;
    for(Calendar c : calendars) {
      options.add(new SelectItemOption<String>(c.getName(), c.getId())) ;
    }
    return options ;
  }
  private List<SelectItemOption<String>> getPriority() throws Exception {
    List<SelectItemOption<String>> options = new ArrayList<SelectItemOption<String>>() ;
    options.add(new SelectItemOption<String>("none", "0")) ;
    options.add(new SelectItemOption<String>("normal", "2")) ;
    options.add(new SelectItemOption<String>("high", "1")) ;
    options.add(new SelectItemOption<String>("low", "3")) ;
    return options ;
  }
  private List<SelectItemOption<String>> getRepeater() {
    List<SelectItemOption<String>> options = new ArrayList<SelectItemOption<String>>() ;
    for(String s : CalendarEvent.REPEATTYPES) {
      options.add(new SelectItemOption<String>(s,s)) ;
    }
    return options ;
  }
  /*private List<SelectItemOption<String>> getReminder() {
    List<SelectItemOption<String>> options = new ArrayList<SelectItemOption<String>>() ;
    for(String rmdType : Reminder.REMINDER_TYPES) {
      options.add(new SelectItemOption<String>(rmdType, rmdType)) ;
    }
    return options ;
  }*/
  
  public void setActionField(String fieldName, List<ActionData> actions) throws Exception {
    actionField_.put(fieldName, actions) ;
  }
  
  public List<ActionData> getActionField(String fieldName) {return actionField_.get(fieldName) ;}

  public UIFormComboBox getUIFormComboBox(String id) {
    // TODO Auto-generated method stub
    return findComponentById(id);
  }  
  
}
