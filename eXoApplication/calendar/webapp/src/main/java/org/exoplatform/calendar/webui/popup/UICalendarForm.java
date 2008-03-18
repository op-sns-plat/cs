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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.exoplatform.calendar.CalendarUtils;
import org.exoplatform.calendar.Colors;
import org.exoplatform.calendar.service.Calendar;
import org.exoplatform.calendar.service.CalendarCategory;
import org.exoplatform.calendar.service.CalendarService;
import org.exoplatform.calendar.webui.UICalendarPortlet;
import org.exoplatform.calendar.webui.UICalendarWorkingContainer;
import org.exoplatform.calendar.webui.UIFormColorPicker;
import org.exoplatform.portal.webui.util.SessionProviderFactory;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.organization.Group;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.core.model.SelectItemOption;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.form.UIFormCheckBoxInput;
import org.exoplatform.webui.form.UIFormInputInfo;
import org.exoplatform.webui.form.UIFormInputWithActions;
import org.exoplatform.webui.form.UIFormSelectBox;
import org.exoplatform.webui.form.UIFormStringInput;
import org.exoplatform.webui.form.UIFormTabPane;
import org.exoplatform.webui.form.UIFormTextAreaInput;
import org.exoplatform.webui.form.UIFormInputWithActions.ActionData;
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
      @EventConfig(listeners = UICalendarForm.SaveActionListener.class),
      @EventConfig(listeners = UICalendarForm.AddCategoryActionListener.class,  phase=Phase.DECODE),
      @EventConfig(listeners = UICalendarForm.SelectPermissionActionListener.class, phase=Phase.DECODE),
      @EventConfig(listeners = UICalendarForm.ResetActionListener.class, phase=Phase.DECODE),
      @EventConfig(listeners = UICalendarForm.CancelActionListener.class, phase=Phase.DECODE)
    }
)
public class UICalendarForm extends UIFormTabPane implements UIPopupComponent, UISelector{
  final public static String DISPLAY_NAME = "displayName" ;
  final public static String DESCRIPTION = "description" ;
  final public static String CATEGORY = "category" ;
  final public static String SHARED_GROUPS = "sharedGroups" ;
  final public static String EDIT_PERMISSION = "editPermission" ;
  final public static String SELECT_COLOR = "selectColor" ;
  final public static String SELECT_GROUPS = "selectGroups" ;
  final public static String INPUT_CALENDAR = "calendarDetail".intern() ;
  final public static String INPUT_SHARE = "public".intern() ;
  final public static String TIMEZONE = "timeZone" ;
  final public static String LOCALE = "locale" ;
  final public static String PERMISSION_SUB = "_permission".intern() ;
  public Map<String, String> permission_ = new HashMap<String, String>() ;

  public Map<String, Map<String, String>> perms_ = new HashMap<String, Map<String, String>>() ;
  private boolean isAddNew_ = true ;
  public String calendarId_ = null ;
  public UICalendarForm() throws Exception{
    super("UICalendarForm");

    UIFormInputWithActions calendarDetail = new UIFormInputWithActions(INPUT_CALENDAR) ;
    calendarDetail.addUIFormInput(new UIFormStringInput(DISPLAY_NAME, DISPLAY_NAME, null).addValidator(MandatoryValidator.class)) ;
    calendarDetail.addUIFormInput(new UIFormTextAreaInput(DESCRIPTION, DESCRIPTION, null)) ;
    calendarDetail.addUIFormInput(new UIFormSelectBox(CATEGORY, CATEGORY, getCategory())) ;
    calendarDetail.addUIFormInput(new UIFormSelectBox(LOCALE, LOCALE, getLocales())) ;
    calendarDetail.addUIFormInput(new UIFormSelectBox(TIMEZONE, TIMEZONE, getTimeZones())) ;
    calendarDetail.addUIFormInput(new UIFormColorPicker(SELECT_COLOR, SELECT_COLOR, Colors.COLORS)) ;
    //calendarDetail.addUIFormInput(new UIFormSelectBox(SELECT_COLOR, SELECT_COLOR, getColors())) ;
    List<ActionData> actions = new ArrayList<ActionData>() ;
    ActionData addCategory = new ActionData() ;
    addCategory.setActionListener("AddCategory") ;
    addCategory.setActionType(ActionData.TYPE_ICON) ;
    addCategory.setActionName("AddCategory") ;
    actions.add(addCategory) ;
    calendarDetail.setActionField(CATEGORY, actions) ;
    setSelectedTab(calendarDetail.getId()) ;
    addChild(calendarDetail) ;

    //UIFormInputWithActions sharing = new UIFormInputWithActions(INPUT_SHARE) ;
    UIGroupCalendarTab sharing = new UIGroupCalendarTab(INPUT_SHARE) ;
    sharing.addUIFormInput(new UIFormInputInfo(SELECT_GROUPS, SELECT_GROUPS, null)) ;
    sharing.addUIFormInput(new UIFormStringInput(EDIT_PERMISSION, null, null)) ;
    for(Object groupObj : getPublicGroups()) {
      String group = ((Group)groupObj).getId() ;
      if(sharing.getUIFormCheckBoxInput(group) != null)sharing.getUIFormCheckBoxInput(group).setChecked(false) ;
      else sharing.addUIFormInput(new UIFormCheckBoxInput<Boolean>(group, group, false)) ;
      if(sharing.getUIFormInputInfo(group+PERMISSION_SUB) == null) {
        sharing.addUIFormInput(new UIFormInputInfo(group+PERMISSION_SUB ,group+PERMISSION_SUB, null)) ;

        actions = new ArrayList<ActionData> () ;
        ActionData editPermission = new ActionData() ;
        editPermission.setActionListener("SelectPermission") ;
        editPermission.setActionName("SelectUser") ;
        editPermission.setActionParameter(UISelectComponent.TYPE_USER + ":" + group+PERMISSION_SUB) ;
        editPermission.setActionType(ActionData.TYPE_ICON) ;
        editPermission.setCssIconClass("SelectUserIcon") ;
        actions.add(editPermission) ;
        ActionData membershipPerm = new ActionData() ;
        membershipPerm.setActionListener("SelectPermission") ;
        membershipPerm.setActionName("SelectMemberShip") ;
        membershipPerm.setActionParameter(UISelectComponent.TYPE_MEMBERSHIP + ":" + group+PERMISSION_SUB) ;
        membershipPerm.setActionType(ActionData.TYPE_ICON) ;
        membershipPerm.setCssIconClass("SelectMemberIcon") ;
        actions.add(membershipPerm) ;
        sharing.setActionField(group+PERMISSION_SUB, actions) ;
      }
    }
    addChild(sharing) ;
  }

  /*@SuppressWarnings("unchecked")
  private List<SelectItemOption<String>> getColors() {
    List<SelectItemOption<String>> colors = new ArrayList<SelectItemOption<String>>() ;
    for(String color : Colors.COLORNAME) {
      colors.add(new SelectItemOption<String>(color, color)) ;
    }
    Collections.sort(colors, new CalendarUtils.SelectComparator()) ;
    return colors;
  }*/

  public String[] getActions(){
    return new String[]{"Save", "Reset", "Cancel"} ;
  }
  private SessionProvider getSession()  {
    return SessionProviderFactory.createSessionProvider() ;
  }
  private  List<SelectItemOption<String>> getCategory() throws Exception {
    String username = Util.getPortalRequestContext().getRemoteUser() ;
    CalendarService calendarService = CalendarUtils.getCalendarService() ;
    List<CalendarCategory> categories = calendarService.getCategories(getSession(), username) ;
    List<SelectItemOption<String>> options = new ArrayList<SelectItemOption<String>>() ;
    for(CalendarCategory category : categories) {
      options.add(new SelectItemOption<String>(category.getName(), category.getId())) ;
    }
    return options ;
  }
  public void reloadCategory() throws Exception {
    UIFormInputWithActions calendarDetail = getChildById(INPUT_CALENDAR) ;
    calendarDetail.getUIFormSelectBox(CATEGORY).setOptions(getCategory()) ;
  }
  protected void lockCheckBoxFields(boolean isLock) throws Exception {
    UIFormInputWithActions shareTab = getChildById(INPUT_SHARE) ;
    for(String group : CalendarUtils.getAllGroups()) {
      UIFormCheckBoxInput uiInput = shareTab.getUIFormCheckBoxInput(group) ;
      if(uiInput != null) uiInput.setEnable(!isLock) ;
    }
    shareTab.getUIStringInput(EDIT_PERMISSION).setEditable(!isLock) ;
  }
  public void activate() throws Exception {
    // TODO Auto-generated method stub

  }
  public void deActivate() throws Exception {
    // TODO Auto-generated method stub
  }
  public void resetField(UICalendarForm uiForm) throws Exception {
    if(isAddNew_) {
      isAddNew_ = true ;
      calendarId_ = null ;
      setDisplayName(null) ;
      setDescription(null) ;
      setSelectedGroup(null) ;
      setLocale(null) ;
      setTimeZone(null) ;
      setSelectedColor(null) ;
      lockCheckBoxFields(false) ;
      UIGroupCalendarTab sharing = getChildById(INPUT_SHARE) ;
      String groupId = null ;
      for(Object obj : getPublicGroups()) {
        groupId = ((Group)obj).getId() ;
        UIFormCheckBoxInput input = getUIFormCheckBoxInput(((Group)obj).getId()) ;
        if(input != null && input.isChecked()) input.setChecked(false) ;
        UIFormInputInfo uiInputIfo = sharing.getUIFormInputInfo(groupId + PERMISSION_SUB);
        if(uiInputIfo != null && uiInputIfo.getValue() != null ) {
          System.out.println(uiInputIfo.getValue()) ;
          uiInputIfo.setValue(null) ;
        }
      }
    } else {
      String username = Util.getPortalRequestContext().getRemoteUser() ;
      Calendar calendar = CalendarUtils.getCalendarService().getUserCalendar(getSession(), username, calendarId_) ;
      init(calendar) ;
    }

  }
  public void init(Calendar calendar) throws Exception {
    //reset() ;
    isAddNew_ = false ;
    calendarId_ = calendar.getId() ;
    setDisplayName(calendar.getName()) ;
    setDescription(calendar.getDescription()) ;
    setSelectedGroup(calendar.getCategoryId()) ;
    setLocale(calendar.getLocale()) ;
    setTimeZone(calendar.getTimeZone()) ;
    setSelectedColor(calendar.getCalendarColor()) ;
    lockCheckBoxFields(true) ;
    UIFormInputWithActions sharing = getChildById(INPUT_SHARE) ;
    sharing.getUIStringInput(EDIT_PERMISSION).setEnable(false) ;
    sharing.setActionField(EDIT_PERMISSION, null) ;
  }

  protected String getDisplayName() {
    UIFormInputWithActions calendarDetail = getChildById(INPUT_CALENDAR) ;
    return calendarDetail.getUIStringInput(DISPLAY_NAME).getValue() ;
  }
  protected void setDisplayName(String value) {
    UIFormInputWithActions calendarDetail = getChildById(INPUT_CALENDAR) ;
    calendarDetail.getUIStringInput(DISPLAY_NAME).setValue(value) ;
  }

  protected String getDescription() {
    UIFormInputWithActions calendarDetail = getChildById(INPUT_CALENDAR) ;
    return calendarDetail.getUIFormTextAreaInput(DESCRIPTION).getValue() ;
  }
  protected void setDescription(String value) {
    UIFormInputWithActions calendarDetail = getChildById(INPUT_CALENDAR) ;
    calendarDetail.getUIFormTextAreaInput(DESCRIPTION).setValue(value) ;
  }
  protected String getSelectedGroup() {
    UIFormInputWithActions calendarDetail = getChildById(INPUT_CALENDAR) ;
    return calendarDetail.getUIFormSelectBox(CATEGORY).getValue() ;
  }
  public void setSelectedGroup(String value) {
    UIFormInputWithActions calendarDetail = getChildById(INPUT_CALENDAR) ;
    calendarDetail.getUIFormSelectBox(CATEGORY).setValue(value) ;
  }
  protected String getSelectedColor() {
    UIFormInputWithActions calendarDetail = getChildById(INPUT_CALENDAR) ;
    return calendarDetail.getChild(UIFormColorPicker.class).getValue() ;
  }
  protected void setSelectedColor(String value) {
    UIFormInputWithActions calendarDetail = getChildById(INPUT_CALENDAR) ;
    calendarDetail.getChild(UIFormColorPicker.class).setValue(value) ;
  }
  protected String getLocale() {
    UIFormInputWithActions calendarDetail = getChildById(INPUT_CALENDAR) ;
    return calendarDetail.getUIFormSelectBox(LOCALE).getValue() ;
  }
  protected void setLocale(String value) {
    UIFormInputWithActions calendarDetail = getChildById(INPUT_CALENDAR) ;
    calendarDetail.getUIFormSelectBox(LOCALE).setValue(value) ;
  }
  protected String getTimeZone() {
    UIFormInputWithActions calendarDetail = getChildById(INPUT_CALENDAR) ;
    return calendarDetail.getUIFormSelectBox(TIMEZONE).getValue() ;
  }

  public void setTimeZone(String value) {
    UIFormInputWithActions calendarDetail = getChildById(INPUT_CALENDAR) ;
    calendarDetail.getUIFormSelectBox(TIMEZONE).setValue(value) ;
  }
  @SuppressWarnings("unchecked")
  public void updateSelect(String selectField, String value) throws Exception {
    UIGroupCalendarTab shareTab = getChildById(INPUT_SHARE) ;
    UIFormInputInfo fieldInput = shareTab.getUIFormInputInfo(selectField) ;
    StringBuilder sb = new StringBuilder() ;
    Map<String, String> temp = new HashMap<String, String>() ;
    if(perms_.get(selectField) == null) {
      temp.put(value, value.split(CalendarUtils.COLON)[0]) ;
    } else {
      temp = perms_.get(selectField) ;
      temp.put(value, value.split(CalendarUtils.COLON)[0]) ;
    }
    perms_.put(selectField, temp) ;
    Map<String, String> tempMap = perms_.get(selectField) ;
    for(String s : tempMap.keySet()) {
      if(sb.length() > 0) sb.append(CalendarUtils.COMMA) ;
      sb.append(tempMap.get(s)) ;
    }
    fieldInput.setValue(sb.toString()) ;
    setSelectedTab(shareTab.getId()) ;
  }
  protected boolean isPublic() throws Exception{
    UIGroupCalendarTab sharing = getChildById(INPUT_SHARE) ;
    for(Object groupObj : getPublicGroups()) {
      String group = ((Group)groupObj).getId() ;
      UIFormCheckBoxInput checkBox = sharing.getUIFormCheckBoxInput(group) ;
      if( checkBox != null) {
        if(checkBox.isChecked()) {
          return true ;
        }
      }
    }
    return false ;
  }
  private Object[] getPublicGroups() throws Exception {
    OrganizationService organization = getApplicationComponent(OrganizationService.class) ;
    String currentUser = Util.getPortalRequestContext().getRemoteUser() ;
    return organization.getGroupHandler().findGroupsOfUser(currentUser).toArray() ;
  }

  @SuppressWarnings("unchecked")
  private List getSelectedGroups(String groupId) throws Exception {
    UIGroupCalendarTab sharing = getChildById(INPUT_SHARE) ;
    List groups = new ArrayList() ;
    Group g = (Group)getApplicationComponent(OrganizationService.class).getGroupHandler().findGroupById(groupId) ;
    UIFormCheckBoxInput<Boolean> input =  sharing.getUIFormCheckBoxInput(groupId) ;
    if(input != null && input.isChecked()) {
      groups.add(g) ;
    } 
    return groups  ;
  }
  private List<SelectItemOption<String>> getTimeZones() {
    return CalendarUtils.getTimeZoneSelectBoxOptions(TimeZone.getAvailableIDs()) ;
  } 
  public String getLabel(String id) {
    try {
      return super.getLabel(id) ;
    } catch (Exception e) {
      return id ;
    }
  }

  private List<SelectItemOption<String>> getLocales() {
    return CalendarUtils.getLocaleSelectBoxOptions(java.util.Calendar.getAvailableLocales()) ;
  }

  static  public class AddCategoryActionListener extends EventListener<UICalendarForm> {
    public void execute(Event<UICalendarForm> event) throws Exception {
      UICalendarForm uiForm = event.getSource() ;
      uiForm.setSelectedTab(INPUT_CALENDAR) ;
      UIPopupContainer uiPopupContainer = uiForm.getAncestorOfType(UIPopupContainer.class) ;
      UIPopupAction uiChildPopup = uiPopupContainer.getChild(UIPopupAction.class);
      uiChildPopup.activate(UICalendarCategoryManager.class, 500) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiChildPopup) ;
    }
  }

  static  public class SelectPermissionActionListener extends EventListener<UICalendarForm> {
    public void execute(Event<UICalendarForm> event) throws Exception {
      UICalendarForm uiForm = event.getSource() ;
      uiForm.setSelectedTab(INPUT_SHARE) ;
      String value = event.getRequestContext().getRequestParameter(OBJECTID) ;
      String permType = value.split(CalendarUtils.COLON)[0] ;
      String permissionIsChecked = value.split(CalendarUtils.COLON)[1].split(PERMISSION_SUB)[0] ;
      UIFormCheckBoxInput checkBox = uiForm.getUIFormCheckBoxInput(permissionIsChecked) ;
      if(!checkBox.isChecked()) {
        UIApplication app = uiForm.getAncestorOfType(UIApplication.class) ;
        app.addMessage(new ApplicationMessage("UICalendarForm.msg.checkbox-notchecked", new String[]{permissionIsChecked}, ApplicationMessage.WARNING)) ;
        event.getRequestContext().addUIComponentToUpdateByAjax(app.getUIPopupMessages()) ;
        return ;
      }
      if(!uiForm.isPublic()) {
        UIApplication app = uiForm.getAncestorOfType(UIApplication.class) ;
        app.addMessage(new ApplicationMessage("UICalendarForm.msg.checkbox-public-notchecked", null)) ;
        event.getRequestContext().addUIComponentToUpdateByAjax(app.getUIPopupMessages()) ;
        return ;
      }
      UIGroupSelector uiGroupSelector = uiForm.createUIComponent(UIGroupSelector.class, null, null);
      uiGroupSelector.setType(permType) ;
      uiGroupSelector.setSelectedGroups(uiForm.getSelectedGroups(value.split(CalendarUtils.COLON)[1].split(PERMISSION_SUB)[0])) ;
      uiGroupSelector.setComponent(uiForm, new String[] {value.split(CalendarUtils.COLON)[1]});
      UIPopupContainer uiPopupContainer = uiForm.getAncestorOfType(UIPopupContainer.class) ;
      UIPopupAction uiChildPopup = uiPopupContainer.getChild(UIPopupAction.class) ;
      uiChildPopup.activate(uiGroupSelector, 500, 0, true) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiForm.getParent()) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiChildPopup) ;
    }
  }
  static  public class ResetActionListener extends EventListener<UICalendarForm> {
    public void execute(Event<UICalendarForm> event) throws Exception {
      System.out.println("\n\n ResetActionListener");
      UICalendarForm uiForm = event.getSource() ;
      uiForm.resetField(uiForm) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiForm.getParent()) ;
    }
  }
  static  public class SaveActionListener extends EventListener<UICalendarForm> {
    public void execute(Event<UICalendarForm> event) throws Exception {
      UICalendarForm uiForm = event.getSource() ;
      UIApplication uiApp = uiForm.getAncestorOfType(UIApplication.class) ;
      String displayName = uiForm.getUIStringInput(DISPLAY_NAME).getValue() ;
      if(!CalendarUtils.isNameValid(displayName, CalendarUtils.SPECIALCHARACTER)){
        uiApp.addMessage(new ApplicationMessage("UICalendarForm.msg.name-invalid", null, ApplicationMessage.WARNING) ) ;
        event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
        return ;
      }
      String username = Util.getPortalRequestContext().getRemoteUser() ;
      CalendarService calendarService = CalendarUtils.getCalendarService() ;
      SessionProvider sProvider = SessionProviderFactory.createSystemProvider() ;
      boolean isPublic = uiForm.isPublic() ;
      Calendar calendar = null ;
      if(isPublic) {
        Object[] groupList = uiForm.getPublicGroups() ;
        List<String> selected = new ArrayList<String>() ;
        for(Object groupObj : groupList) {
          String groupId = ((Group)groupObj).getId() ;
          if(uiForm.getUIFormCheckBoxInput(groupId)!= null && uiForm.getUIFormCheckBoxInput(groupId).isChecked()) { 
            selected.add(groupId) ;
          }
        }
        if(selected.size() < 1){
          uiApp.addMessage(new ApplicationMessage("UICalendarForm.msg.group-empty", null, ApplicationMessage.WARNING) ) ;
          event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
          return ;
        }

        calendar = new Calendar() ;
        if(!uiForm.isAddNew_) calendar.setId(uiForm.calendarId_) ;
        calendar.setName(displayName) ;
        calendar.setDescription(uiForm.getUIFormTextAreaInput(DESCRIPTION).getValue()) ;
        calendar.setCategoryId(uiForm.getUIFormSelectBox(CATEGORY).getValue()) ;

        calendar.setPublic(isPublic) ;
        calendar.setLocale(uiForm.getLocale()) ;
        calendar.setTimeZone(uiForm.getTimeZone()) ;
        calendar.setCalendarColor(uiForm.getSelectedColor()) ;
        calendar.setGroups(selected.toArray((new String[]{})));
        List<String> listPermission = new ArrayList<String>() ;

        for(String groupIdSelected : selected) {
          //String editPermission = uiForm.perms_.get(groupIdSelected + PERMISSION_SUB).keySet() ;
         if(uiForm.perms_.get(groupIdSelected + PERMISSION_SUB) != null)
          for(String s : uiForm.perms_.get(groupIdSelected + PERMISSION_SUB).keySet()){
            if(!CalendarUtils.isEmpty(s)) listPermission.add(s) ;
          }
          listPermission.add(CalendarUtils.getCurrentUser()) ;
        }        
        calendar.setEditPermission(listPermission.toArray(new String[]{})) ;
        calendarService.savePublicCalendar(sProvider, calendar, uiForm.isAddNew_, username) ;
      }else {
        if(CalendarUtils.isEmpty(uiForm.getUIFormSelectBox(CATEGORY).getValue())) {
          uiApp.addMessage(new ApplicationMessage("UICalendarForm.msg.category-empty", null, ApplicationMessage.WARNING) ) ;
          event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
          return ;
        }
        calendar = new Calendar() ;
        if(!uiForm.isAddNew_) calendar.setId(uiForm.calendarId_) ;
        calendar.setName(displayName) ;
        calendar.setDescription(uiForm.getUIFormTextAreaInput(DESCRIPTION).getValue()) ;
        calendar.setCategoryId(uiForm.getUIFormSelectBox(CATEGORY).getValue()) ;
        calendar.setPublic(isPublic) ;
        calendar.setLocale(uiForm.getLocale()) ;
        calendar.setTimeZone(uiForm.getTimeZone()) ;
        calendar.setCalendarColor(uiForm.getSelectedColor()) ;
        calendar.setGroups(new String[]{CalendarUtils.PRIVATE_TYPE}) ;
        calendarService.saveUserCalendar(sProvider, username, calendar, uiForm.isAddNew_) ;    
      }

      UICalendarPortlet calendarPortlet = uiForm.getAncestorOfType(UICalendarPortlet.class) ;
      calendarPortlet.setCalendarSetting(null) ;
      calendarPortlet.cancelAction() ;
      UICalendarWorkingContainer uiWorkingContainer = calendarPortlet.getChild(UICalendarWorkingContainer.class) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiWorkingContainer) ; ;
    }
  }
  static  public class CancelActionListener extends EventListener<UICalendarForm> {
    public void execute(Event<UICalendarForm> event) throws Exception {
      UICalendarForm uiForm = event.getSource() ;
      UICalendarPortlet calendarPortlet = uiForm.getAncestorOfType(UICalendarPortlet.class) ;
      calendarPortlet.cancelAction() ;
    }
  }
}
