/*
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
package org.exoplatform.contact.webui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.exoplatform.contact.ContactUtils;
import org.exoplatform.contact.service.Contact;
import org.exoplatform.contact.service.ContactFilter;
import org.exoplatform.contact.service.ContactService;
import org.exoplatform.contact.service.JCRPageList;
import org.exoplatform.contact.service.SharedAddressBook;
import org.exoplatform.contact.service.Tag;
import org.exoplatform.contact.service.impl.JCRDataStorage;
import org.exoplatform.contact.service.impl.NewUserListener;
import org.exoplatform.contact.webui.popup.UIComposeForm;
import org.exoplatform.contact.webui.popup.UIContactPreviewForm;
import org.exoplatform.contact.webui.popup.UIExportForm;
import org.exoplatform.contact.webui.popup.UIMoveContactsForm;
import org.exoplatform.contact.webui.popup.UIPopupComponent;
import org.exoplatform.contact.webui.popup.UISharedForm;
import org.exoplatform.contact.webui.popup.UITagForm;
import org.exoplatform.contact.webui.popup.UICategorySelect;
import org.exoplatform.contact.webui.popup.UIContactForm;
import org.exoplatform.contact.webui.popup.UIPopupAction;
import org.exoplatform.contact.webui.popup.UIPopupContainer;
import org.exoplatform.download.DownloadService;
import org.exoplatform.portal.webui.util.SessionProviderFactory;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormCheckBoxInput;

/**
 * Created by The eXo Platform SARL
 * Author : Hung Nguyen
 *          hung.nguyen@exoplatform.com
 * Aus 01, 2007 2:48:18 PM 
 */

@ComponentConfig(
    lifecycle = UIFormLifecycle.class,
    template =  "app:/templates/contact/webui/UIContacts.gtmpl",
    events = {
        @EventConfig(listeners = UIContacts.EditContactActionListener.class),
        @EventConfig(listeners = UIContacts.SendEmailActionListener.class),
        @EventConfig(listeners = UIContacts.InstantMessageActionListener.class),
        @EventConfig(listeners = UIContacts.TagActionListener.class),
        @EventConfig(listeners = UIContacts.MoveContactsActionListener.class),
        @EventConfig(listeners = UIContacts.DNDContactsActionListener.class),
        @EventConfig(listeners = UIContacts.DNDContactsToTagActionListener.class),
        @EventConfig(listeners = UIContacts.DeleteContactsActionListener.class
            , confirm = "UIContacts.msg.confirm-delete"),
        @EventConfig(listeners = UIContacts.SelectedContactActionListener.class), 
        @EventConfig(listeners = UIContacts.CopyContactActionListener.class),
        @EventConfig(listeners = UIContacts.ViewDetailsActionListener.class),
        @EventConfig(listeners = UIContacts.SortActionListener.class),
        @EventConfig(listeners = UIContacts.FirstPageActionListener.class),
        @EventConfig(listeners = UIContacts.PreviousPageActionListener.class),
        @EventConfig(listeners = UIContacts.NextPageActionListener.class),
        @EventConfig(listeners = UIContacts.LastPageActionListener.class),
        @EventConfig(listeners = UIContacts.ExportContactActionListener.class),
        @EventConfig(listeners = UIContacts.CancelActionListener.class),
        @EventConfig(listeners = UIContacts.SelectTagActionListener.class),
        @EventConfig(listeners = UIContacts.SharedContactsActionListener.class),
        @EventConfig(listeners = UIContacts.CloseSearchActionListener.class)
    }
)

public class UIContacts extends UIForm implements UIPopupComponent {
  public boolean viewContactsList = true ;
  private String selectedTag_ = null ;
  private LinkedHashMap<String, Contact> contactMap = new LinkedHashMap<String, Contact> () ;
  private String selectedGroup = null ;
  private String selectedContact = null ;
  private JCRPageList pageList_ = null ;
  private String sortedBy_ = null;
  private boolean isAscending_ = true;
  private String viewQuery_ = null;
  public static String fullName = "fullName".intern() ;
  public static String emailAddress = "emailAddress".intern() ;
  public static String jobTitle = "jobTitle".intern() ;
  private boolean isSearchResult = false ;
  private boolean defaultNameSorted = true ;
 
  public UIContacts() throws Exception { } 
  public String[] getActions() { return new String[] {"Cancel"} ; }
  public void activate() throws Exception { }
  public void deActivate() throws Exception { } 
  
  public boolean isDisplaySearchResult() {return isSearchResult ;}
  public void setDisplaySearchResult(boolean search) { isSearchResult = search ; }
  
  public void setAscending(boolean isAsc) { isAscending_ = isAsc ; }
  public boolean isAscending() {return isAscending_ ; }
  public void setSortedBy(String s) { sortedBy_ = s ; }
  public String getSortedBy() { return sortedBy_ ; }
  public String getViewQuery() {return viewQuery_ ; }
  public void setViewQuery(String view) {viewQuery_ = view ;}
  
  public void setContacts(JCRPageList pageList) throws Exception {
    pageList_ = pageList ;
    updateList() ; 
  }
  public JCRPageList getContactPageList() { return pageList_ ; }
  
  public boolean isAscName() { return FullNameComparator.isAsc ; }
  public boolean isAscEmail() { return EmailComparator.isAsc ; }
  public boolean isAscJob() { return JobTitleComparator.isAsc ; }
  public void setDefaultNameSorted(boolean name) { defaultNameSorted = name ; }
  public boolean isNameSorted() { return defaultNameSorted ; }
  
  public void setContact(List<Contact> contacts, boolean isUpdate) throws Exception{
    pageList_.setContact(contacts, isUpdate) ; 
  }
  public void updateList() throws Exception {
    getChildren().clear() ;
    contactMap.clear();
    UIContactPreview contactPreview = 
      getAncestorOfType(UIContactContainer.class).getChild(UIContactPreview.class) ;
    if(pageList_ != null) {
      List<Contact> contactList = pageList_.getPage(pageList_.getCurrentPage(),ContactUtils.getCurrentUser()) ;
      if(contactList.size() == 0 && pageList_.getCurrentPage() > 1)
        contactList = pageList_.getPage(pageList_.getCurrentPage() - 1,ContactUtils.getCurrentUser()) ;
      for(Contact contact : contactList) {
        UIFormCheckBoxInput<Boolean> checkbox = new UIFormCheckBoxInput<Boolean>(contact.getId(),contact.getId(), false) ;
        addUIFormInput(checkbox);
        contactMap.put(contact.getId(), contact) ; 
      }
      Contact[] array = contactMap.values().toArray(new Contact[]{}) ;
      if (array.length > 0) {
        Contact firstContact = array[0] ;        
        contactPreview.setContact(firstContact) ;
        selectedContact = firstContact.getId() ;
      } else contactPreview.setContact(null) ;
    } else contactPreview.setContact(null) ;
  }
  
  public Contact[] getContacts() throws Exception {
    return contactMap.values().toArray(new Contact[]{}) ;
  }
  public LinkedHashMap<String, Contact> getContactMap() { return contactMap ;}
  
  public void setSelectedContact(String s) { selectedContact = s ; }
  public String getSelectedContact() { return selectedContact ; }
  
  public void setSelectedGroup(String s) { selectedGroup = s ; }
  public String getSelectedGroup() { return selectedGroup ; }
  
  public void setViewContactsList(boolean list) { viewContactsList = list ; }
  public boolean getViewContactsList() {
    if (viewContactsList) {
      getAncestorOfType(UIContactContainer.class).getChild(UIContactPreview.class).setRendered(true) ;
    } else {
      getAncestorOfType(UIContactContainer.class).getChild(UIContactPreview.class).setRendered(false) ;
    }
    return viewContactsList ; 
  }
  
  public List<String> getCheckedContacts() throws Exception {
    List<String> checkedContacts = new ArrayList<String>() ;
    for (Contact contact : getContacts()) {
      UIFormCheckBoxInput uiCheckBox = getChildById(contact.getId()) ;
      if(uiCheckBox != null && uiCheckBox.isChecked()) {
        checkedContacts.add(contact.getId()) ;
      } 
    }
    return checkedContacts ;
  }
  
  public DownloadService getDownloadService() { 
    return getApplicationComponent(DownloadService.class) ; 
  }
  
  public void setPageList(JCRPageList pageList, long page) throws Exception {
    getChildren().clear();
    pageList_ = pageList ;
    for (Contact contact : pageList.getPage(page, ContactUtils.getCurrentUser())) {
      addUIFormInput(new UIFormCheckBoxInput<Boolean>(contact.getId(),contact.getId(), false)) ;
    }
  }
  
  public String getSelectedTag() {return selectedTag_ ;}
  public void setSelectedTag(String tagId) {selectedTag_ = tagId ;}
  
  public Map<String, Tag> getTagMap() {
    return getAncestorOfType(UIWorkingContainer.class)
      .findFirstComponentOfType(UITags.class).getTagMap() ;
  }
  
  public Map<String, String> getPrivateGroupMap() {
    return getAncestorOfType(UIWorkingContainer.class)
      .findFirstComponentOfType(UIAddressBooks.class).getPrivateGroupMap() ;
  }
  public Map<String, SharedAddressBook> getSharedGroupMap() throws Exception {
    return getAncestorOfType(UIWorkingContainer.class)
      .findFirstComponentOfType(UIAddressBooks.class).getSharedGroups() ;
  }
  
  public boolean checkExistContacts(List<String> contactIds) throws Exception {
    ContactService contactService = ContactUtils.getContactService() ;
    String username = ContactUtils.getCurrentUser() ;
    SessionProvider sessionProvider = SessionProviderFactory.createSessionProvider() ;
    for (String contactId : contactIds) {
      if (contactService.getContact(sessionProvider, username, contactId) == null
          && contactService.getPublicContact(contactId) == null
          && contactService.getSharedContact(sessionProvider, username, contactId) == null)
        return false ;
    }
    return true ;
  }
  public String getDefaultGroup() { return NewUserListener.DEFAULTGROUP ;}
  /*
  public boolean isPublic(String contactId) {
    if ( contactMap.get(contactId).getContactType().equals(JCRDataStorage.PUBLIC)) return true ;
    return false ;
  }
  */
  static public class EditContactActionListener extends EventListener<UIContacts> {
    public void execute(Event<UIContacts> event) throws Exception {
      UIContacts uiContacts = event.getSource();
      String contactId = event.getRequestContext().getRequestParameter(OBJECTID);
      List<String> contactIds = new ArrayList<String>() ;
      contactIds.add(contactId) ;
      
      /*
      if (uiContacts.isSearchResult && !uiContacts.checkExistContacts(contactIds)){
        UIApplication uiApp = uiContacts.getAncestorOfType(UIApplication.class) ;
        uiApp.addMessage(new ApplicationMessage("UIContacts.msg.contact-deleted", null)) ;
        event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
        return ;
      }
      */
      
      UIContactPortlet contactPortlet = uiContacts.getAncestorOfType(UIContactPortlet.class) ;
      UIPopupAction popupAction = contactPortlet.getChild(UIPopupAction.class) ;
      UIPopupContainer popupContainer =  popupAction.activate(UIPopupContainer.class, 800) ;
      popupContainer.setId("AddNewContact");
      UICategorySelect uiCategorySelect = popupContainer.addChild(UICategorySelect.class, null, null) ;
      UIContactForm uiContactForm = popupContainer.addChild(UIContactForm.class, null, null) ;      
      Contact contact = uiContacts.contactMap.get(contactId) ;
      uiCategorySelect.setPrivateGroupMap(contactPortlet
          .findFirstComponentOfType(UIAddressBooks.class).getPrivateGroupMap()) ;
      uiCategorySelect.addCategories() ;
      uiCategorySelect.setValue(contact.getAddressBook()[0]) ;
      if (contact.getContactType().equals("1"))  uiCategorySelect.disableSelect() ;
      uiContactForm.setValues(contact);
      uiContactForm.setNew(false) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(popupAction) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiContacts.getParent()) ;
    }
  } 
  
  static public class InstantMessageActionListener extends EventListener<UIContacts> {
    public void execute(Event<UIContacts> event) throws Exception {
      UIContacts uiContacts = event.getSource();
      UIApplication uiApp = uiContacts.getAncestorOfType(UIApplication.class) ;
      uiApp.addMessage(new ApplicationMessage("UIContacts.msg.not-already", null)) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
      return ;
    }
  }
  
  static public class TagActionListener extends EventListener<UIContacts> {
    public void execute(Event<UIContacts> event) throws Exception {
      UIContacts uiContacts = event.getSource() ;
      String contactId = event.getRequestContext().getRequestParameter(OBJECTID);
      List<String> contactIds = new ArrayList<String>();
      if (!ContactUtils.isEmpty(contactId)) contactIds.add(contactId) ;
      else {
        contactIds = uiContacts.getCheckedContacts() ;
        if (contactIds.size() == 0) {
          UIApplication uiApp = uiContacts.getAncestorOfType(UIApplication.class) ;
          uiApp.addMessage(new ApplicationMessage("UIContacts.msg.checkContact-required", null)) ;
          event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
          return ;
        }
      }
      if (uiContacts.isSearchResult && !uiContacts.checkExistContacts(contactIds)){
        UIApplication uiApp = uiContacts.getAncestorOfType(UIApplication.class) ;
        uiApp.addMessage(new ApplicationMessage("UIContacts.msg.contact-deleted", null)) ;
        event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
        return ;
      }      
      UIContactPortlet contactPortlet = uiContacts.getAncestorOfType(UIContactPortlet.class) ;
      UIPopupAction popupAction = contactPortlet.getChild(UIPopupAction.class) ;
      UITagForm uiTagForm = popupAction.activate(UITagForm.class, 600) ;
      List<Contact> contacts = new ArrayList<Contact>() ;
      for (String id : contactIds) { contacts.add(uiContacts.contactMap.get(id)) ; }
      uiTagForm.setContacts(contacts) ;
      uiTagForm.update() ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiContacts.getParent()) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(popupAction) ;
    }
  }
  
  static public class DNDContactsToTagActionListener extends EventListener<UIContacts> {
    public void execute(Event<UIContacts> event) throws Exception {
      UIContacts uiContacts = event.getSource();
      String tagId = event.getRequestContext().getRequestParameter(OBJECTID);   
      @SuppressWarnings("unused")
      String type = event.getRequestContext().getRequestParameter("contactType");
      List<String> contactIds = new ArrayList<String>();
      @SuppressWarnings("unused")
      UIApplication uiApp = uiContacts.getAncestorOfType(UIApplication.class) ;
      contactIds = uiContacts.getCheckedContacts() ;
      if (uiContacts.isSearchResult && !uiContacts.checkExistContacts(contactIds)){
        uiApp.addMessage(new ApplicationMessage("UIContacts.msg.contact-deleted", null)) ;
        event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
        return ;
      }    
      ContactUtils.getContactService().addTag(
          SessionProviderFactory.createSystemProvider(), ContactUtils.getCurrentUser(), contactIds, tagId);
      
      // when select shared contacts 
      if(uiContacts.isDisplaySearchResult() || (ContactUtils.isEmpty(
          uiContacts.getSelectedGroup()) && ContactUtils.isEmpty(uiContacts.getSelectedTag()))) {
        List<Contact> contacts = new ArrayList<Contact>() ;
        for (String contactId : contactIds) {
          Contact contact = uiContacts.contactMap.get(contactId) ;
          String[] tags = contact.getTags() ;
          if (tags != null && tags.length > 1) {
            List<String> newTags = new ArrayList<String>() ;
            for (String tag : tags) newTags.add(tag) ;
            newTags.add(tagId) ;
            contact.setTags(newTags.toArray(new String[] {})) ;
          }
          else {
            contact.setTags(new String[] {tagId}) ;
          }
          contacts.add(contact) ;
        }
        uiContacts.setContact(contacts, true) ;
      }
      uiContacts.updateList() ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiContacts.getParent()) ;
    }
  }
  
  static public class MoveContactsActionListener extends EventListener<UIContacts> {
    public void execute(Event<UIContacts> event) throws Exception {
      UIContacts uiContacts = event.getSource();
      String contactId = event.getRequestContext().getRequestParameter(OBJECTID);
      List<String> contactIds = new ArrayList<String>();
      UIApplication uiApp = uiContacts.getAncestorOfType(UIApplication.class) ;
      if (!ContactUtils.isEmpty(contactId) && !contactId.equals("null")) {
        contactIds.add(contactId) ;
      } else {
        contactIds = uiContacts.getCheckedContacts() ;
        if (contactIds.size() == 0) {          
          uiApp.addMessage(new ApplicationMessage("UIContacts.msg.checkContact-required", null)) ;
          event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
          return ;
        }
      } 
      if (uiContacts.isSearchResult && !uiContacts.checkExistContacts(contactIds)){
        uiApp.addMessage(new ApplicationMessage("UIContacts.msg.contact-deleted", null)) ;
        event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
        return ;
      } 
      Map<String, Contact> movedContacts = new HashMap<String, Contact>() ;
      for (String id : contactIds) {
        Contact contact = uiContacts.contactMap.get(id) ;
        if (contact.getContactType().equals(JCRDataStorage.PUBLIC)
            || contact.getContactType().equals(JCRDataStorage.SHARED)
            || contact.getId().equals(ContactUtils.getCurrentUser())) {
          uiApp.addMessage(new ApplicationMessage("UIContacts.msg.cannot-move", null
              , ApplicationMessage.WARNING)) ;
          event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
          return ;
        }
        movedContacts.put(id, contact) ;
      }
      UIContactPortlet uiContactPortlet = uiContacts.getAncestorOfType(UIContactPortlet.class) ;
      UIPopupAction popupAction = uiContactPortlet.getChild(UIPopupAction.class) ;
      UIMoveContactsForm uiMoveForm = popupAction.activate(UIMoveContactsForm.class, 540) ;
      uiMoveForm.setContacts(movedContacts) ;
      UIAddressBooks addressBooks = uiContactPortlet.findFirstComponentOfType(UIAddressBooks.class) ;
      uiMoveForm.setPrivateGroupMap(addressBooks.getPrivateGroupMap()) ;
      uiMoveForm.setSharedGroupMap(addressBooks.getSharedGroups()) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiContacts.getParent());
      event.getRequestContext().addUIComponentToUpdateByAjax(popupAction) ;     
    }
  }
  
  static public class DNDContactsActionListener extends EventListener<UIContacts> {
    public void execute(Event<UIContacts> event) throws Exception {
      UIContacts uiContacts = event.getSource();
      String addressBookId = event.getRequestContext().getRequestParameter(OBJECTID);
      String type = event.getRequestContext().getRequestParameter("addressType");
      String[] addressBooks = {addressBookId} ;
      List<String> contactIds = uiContacts.getCheckedContacts() ;
      List<Contact> contacts = new ArrayList<Contact>();
      @SuppressWarnings("unused")
      UIApplication uiApp = uiContacts.getAncestorOfType(UIApplication.class) ;
      if (uiContacts.isSearchResult && !uiContacts.checkExistContacts(contactIds)){
        uiApp.addMessage(new ApplicationMessage("UIContacts.msg.contact-deleted", null)) ;
        event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
        return ;
      }
      String username = ContactUtils.getCurrentUser() ;
      for(String contactId : contactIds) {
      	Contact contact = uiContacts.contactMap.get(contactId) ;
        if (contact.getContactType().equals(JCRDataStorage.PUBLIC) 
            || contact.getContactType().equals(JCRDataStorage.SHARED)
            || contact.getId().equals(username)){
          uiApp.addMessage(new ApplicationMessage("UIContacts.msg.cannot-move", null
              , ApplicationMessage.WARNING)) ;
          event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;

          // add
          event.getRequestContext().addUIComponentToUpdateByAjax(uiContacts.getParent()) ;
          return ;
        }
      	if(contact != null) {
      		contact.setAddressBook(addressBooks) ;
      		contacts.add(contact) ;
      	}
      }     
      if(contacts.size() == 0) return ;
      ContactService contactService = ContactUtils.getContactService() ;
      SessionProvider sessionProvider = SessionProviderFactory.createSessionProvider() ;
      
      contactService.moveContacts(sessionProvider, username, contacts, type); 
      
      // update addressbook when search
      if (uiContacts.isSearchResult) {
        for (String contactId : contactIds) {
          uiContacts.contactMap.get(contactId).setContactType(type) ;
        }
      } 
      uiContacts.updateList() ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiContacts.getParent()) ;
      
      // to hide public AddressBook if contactPageList=null ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiContacts
          .getAncestorOfType(UIWorkingContainer.class).findFirstComponentOfType(UIAddressBooks.class)) ;
    }
  }
  
  static public class CopyContactActionListener extends EventListener<UIContacts> {
    public void execute(Event<UIContacts> event) throws Exception {
      UIContacts uiContacts = event.getSource() ;
      String contactId = event.getRequestContext().getRequestParameter(OBJECTID);
      List<String> checkedContact = new ArrayList<String>() ; 
      if (!ContactUtils.isEmpty(contactId)) {
        checkedContact.add(contactId) ;
      } else {
        checkedContact =  uiContacts.getCheckedContacts() ;
        if (checkedContact.size() < 1) {
          UIApplication uiApp = uiContacts.getAncestorOfType(UIApplication.class) ;
          uiApp.addMessage(new ApplicationMessage("UIContacts.msg.checkContact-required", null,
              ApplicationMessage.WARNING)) ;
          event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
          return ;        
        }
      }
      UIAddressBooks uiAddressBooks = uiContacts.getAncestorOfType(
          UIWorkingContainer.class).findFirstComponentOfType(UIAddressBooks.class) ;     
      uiAddressBooks.setCopyAddress(null) ;
      List<Contact> copyContacts = new ArrayList<Contact>();
      for (String contact : checkedContact) {
        copyContacts.add(uiContacts.contactMap.get(contact)) ;
      }  
      uiAddressBooks.setCopyContacts(copyContacts) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiAddressBooks) ;
    }
  }
  
  
  static public class DeleteContactsActionListener extends EventListener<UIContacts> {
    public void execute(Event<UIContacts> event) throws Exception {
      UIContacts uiContacts = event.getSource();
      String contactId = event.getRequestContext().getRequestParameter(OBJECTID);
      List<String> contactIds = new ArrayList<String>();
      if (!ContactUtils.isEmpty(contactId) && !contactId.toString().equals("null")) {
        contactIds.add(contactId) ;
      } else {
        contactIds = uiContacts.getCheckedContacts() ;
        if (contactIds.size() == 0) {
          UIApplication uiApp = uiContacts.getAncestorOfType(UIApplication.class) ;
          uiApp.addMessage(new ApplicationMessage("UIContacts.msg.checkContact-required", null,
              ApplicationMessage.WARNING)) ;
          event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
          return ;
        }
      }
      if (uiContacts.isSearchResult && !uiContacts.checkExistContacts(contactIds)){
        UIApplication uiApp = uiContacts.getAncestorOfType(UIApplication.class) ;
        uiApp.addMessage(new ApplicationMessage("UIContacts.msg.contact-deleted", null,
            ApplicationMessage.WARNING)) ;
        event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
        return ;
      }
      
      
      //
      
      UIWorkingContainer uiWorkingContainer = uiContacts.getAncestorOfType(UIWorkingContainer.class) ;
      ContactService contactService = ContactUtils.getContactService() ;
      String username = ContactUtils.getCurrentUser() ;
      contactService.removeContacts(SessionProviderFactory.createSystemProvider(), username, contactIds) ;
      if(uiContacts.isSearchResult) {
        List<Contact> contacts = new ArrayList<Contact>();
        for(String id : contactIds) {
          contacts.add(uiContacts.contactMap.get(id)) ;
        }
        uiContacts.setContact(contacts, false) ;
      }
//      if(contactIds.contains(uiContactPreview.getContact().getId())) 
//        uiContactPreview.setContact(null) ;
    
      if (uiContacts.getSelectedTag() != null) {
        String tagName = uiWorkingContainer.findFirstComponentOfType(UITags.class).getSelectedTag() ;
        uiContacts.setContacts(contactService
            .getContactPageListByTag(SessionProviderFactory.createSystemProvider(), username, tagName)) ;
      } else {
        uiContacts.updateList() ;
      }
//      if (uiContacts.isDisplaySearchResult()) {
//        for (String contact : contactIds)
//          uiContacts.pageList_.getAll().remove(contact) ;        
//      }
      event.getRequestContext().addUIComponentToUpdateByAjax(uiWorkingContainer) ;
    }
  } 
  
  static public class ExportContactActionListener extends EventListener<UIContacts> {
    public void execute(Event<UIContacts> event) throws Exception {
      UIContacts uiContacts = event.getSource();
      String contactId = event.getRequestContext().getRequestParameter(OBJECTID);
      UIContactPortlet uiContactPortlet = uiContacts.getAncestorOfType(UIContactPortlet.class);
      UIPopupAction uiPopupAction = uiContactPortlet.getChild(UIPopupAction.class);
      UIExportForm uiExportForm = uiPopupAction.activate(UIExportForm.class, 500) ;
      uiExportForm.setId("ExportForm");
      Contact contact = uiContacts.contactMap.get(contactId) ;
      uiExportForm.setContacts(new Contact[] { contact }) ;
      uiExportForm.updateList();
      event.getRequestContext()
        .addUIComponentToUpdateByAjax(uiContactPortlet.findFirstComponentOfType(UIContactContainer.class));
      event.getRequestContext().addUIComponentToUpdateByAjax(uiPopupAction);
    }
  }
  
  static public class SelectedContactActionListener extends EventListener<UIContacts> {
    public void execute(Event<UIContacts> event) throws Exception {
      UIContacts uiContacts = event.getSource();
      String contactId = event.getRequestContext().getRequestParameter(OBJECTID);
      uiContacts.setSelectedContact(contactId) ;
      UIContactContainer uiContactContainer = uiContacts.getAncestorOfType(UIContactContainer.class);
      UIContactPreview uiContactPreview = uiContactContainer.findFirstComponentOfType(UIContactPreview.class);
      uiContactPreview.setContact(uiContacts.contactMap.get(contactId));
      uiContactPreview.setRendered(true) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiContactContainer);   
    }
  }
  
  static public class ViewDetailsActionListener extends EventListener<UIContacts> {
    public void execute(Event<UIContacts> event) throws Exception {
      UIContacts uiContacts = event.getSource();
      String contactId = event.getRequestContext().getRequestParameter(OBJECTID);
      UIContactPortlet contactPortlet = uiContacts.getAncestorOfType(UIContactPortlet.class) ;
      UIPopupAction popupAction = contactPortlet.getChild(UIPopupAction.class) ;
      UIPopupContainer uiPopupContainer = popupAction.activate(UIPopupContainer.class, 700) ;
      uiPopupContainer.setId("ContactDetails") ;  
      UIContactPreviewForm uiContactPreviewForm = uiPopupContainer.addChild(UIContactPreviewForm.class, null, null) ; 
      uiContactPreviewForm.setContact(uiContacts.contactMap.get(contactId)) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(popupAction) ;  
      event.getRequestContext().addUIComponentToUpdateByAjax(uiContacts.getParent()) ;
    }
  } 
  
  static public class FirstPageActionListener extends EventListener<UIContacts> {
    public void execute(Event<UIContacts> event) throws Exception {
      UIContacts uiContacts = event.getSource() ; 
      JCRPageList pageList = uiContacts.getContactPageList();
      if (pageList != null) {
        uiContacts.setPageList(pageList, 1) ;
        uiContacts.updateList() ;
        event.getRequestContext().addUIComponentToUpdateByAjax(uiContacts.getParent());
      }
    }
  }
  
  static public class PreviousPageActionListener extends EventListener<UIContacts> {
    public void execute(Event<UIContacts> event) throws Exception {
      UIContacts uiContacts = event.getSource() ; 
      JCRPageList pageList = uiContacts.getContactPageList(); 
      if (pageList != null && pageList.getCurrentPage() > 1){
        uiContacts.setPageList(pageList, pageList.getCurrentPage() - 1);
        uiContacts.updateList() ;
        event.getRequestContext().addUIComponentToUpdateByAjax(uiContacts.getParent());
      }      
    }
  }
  
  static public class NextPageActionListener extends EventListener<UIContacts> {
    public void execute(Event<UIContacts> event) throws Exception {
      UIContacts uiContacts = event.getSource() ; 
      JCRPageList pageList = uiContacts.getContactPageList() ; 
      if (pageList != null && pageList.getCurrentPage() < pageList.getAvailablePage()){
        uiContacts.setPageList(pageList, pageList.getCurrentPage() + 1);
        uiContacts.updateList() ;
        event.getRequestContext().addUIComponentToUpdateByAjax(uiContacts.getParent());
      }      
    }
  }
  
  static public class LastPageActionListener extends EventListener<UIContacts> {
    public void execute(Event<UIContacts> event) throws Exception {
      UIContacts uiContacts = event.getSource() ; 
      JCRPageList pageList = uiContacts.getContactPageList(); 
      if (pageList != null) {
        uiContacts.setPageList(pageList, pageList.getAvailablePage());
        uiContacts.updateList() ;
        event.getRequestContext().addUIComponentToUpdateByAjax(uiContacts.getParent());
      }      
    }
  }
  
  static  public class CancelActionListener extends EventListener<UIContacts> {
    public void execute(Event<UIContacts> event) throws Exception {
      UIContacts uiContacts = event.getSource() ;
      UIPopupAction uiPopupAction = uiContacts.getAncestorOfType(UIPopupAction.class) ;
      uiPopupAction.deActivate() ;
    }
  }
  
  static public class SortActionListener extends EventListener<UIContacts> {
    @SuppressWarnings("unchecked")
    public void execute(Event<UIContacts> event) throws Exception {
      UIContacts uiContacts = event.getSource() ;
      String sortedBy = event.getRequestContext().getRequestParameter(OBJECTID) ;
      uiContacts.setAscending(!uiContacts.isAscending_);
      uiContacts.setSortedBy(sortedBy);
      uiContacts.setDefaultNameSorted(false) ;
      
      JCRPageList pageList = null ;
      String group = uiContacts.selectedGroup ;
      if (!ContactUtils.isEmpty(group)) {
        ContactFilter filter = new ContactFilter() ;
        filter.setViewQuery(uiContacts.getViewQuery());        
        filter.setAscending(uiContacts.isAscending_);
        filter.setOrderBy(sortedBy);
        filter.setCategories(new String[] { group } ) ;

        String type = null;
        UIAddressBooks addressBooks = uiContacts.getAncestorOfType(
            UIWorkingContainer.class).findFirstComponentOfType(UIAddressBooks.class) ;
        if (addressBooks.getPrivateGroupMap().containsKey(group)) type = JCRDataStorage.PRIVATE ;
        else if (addressBooks.getSharedGroups().containsKey(group)) type = JCRDataStorage.SHARED ;
        else type = JCRDataStorage.PUBLIC ;
        
        //else if (addressBooks.getPublicGroupMap().containsKey(group)) type = JCRDataStorage.PUBLIC ;
        
        if(type != null)
          pageList = ContactUtils.getContactService().getContactPageListByGroup(
            SessionProviderFactory.createSystemProvider(),ContactUtils.getCurrentUser(), filter, type) ; 
      } else {      //selected group = null ;
          pageList = uiContacts.pageList_ ;
          if (pageList != null) {
            List<Contact> contacts = new ArrayList<Contact>() ;
            contacts = pageList.getAll() ;
            if (uiContacts.getSortedBy().equals(UIContacts.fullName)) {
              FullNameComparator.isAsc = (!FullNameComparator.isAsc) ;
              Collections.sort(contacts, new FullNameComparator()) ;
            } else if (uiContacts.getSortedBy().equals(UIContacts.emailAddress)) {
              EmailComparator.isAsc = (!EmailComparator.isAsc) ;
              Collections.sort(contacts, new EmailComparator()) ;
            } else if (uiContacts.getSortedBy().equals(UIContacts.jobTitle)) {
              JobTitleComparator.isAsc = (!JobTitleComparator.isAsc) ;
              Collections.sort(contacts, new JobTitleComparator()) ;
            }  
            pageList.setList(contacts) ;
          }
      }
      uiContacts.setContacts(pageList) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiContacts.getParent());
    }
  }
  
  static public class FullNameComparator implements Comparator {
    public static boolean isAsc ;
    public int compare(Object o1, Object o2) throws ClassCastException {
      String name1 = ((Contact) o1).getFullName() ;
      String name2 = ((Contact) o2).getFullName() ;
      if (isAsc == true) return name1.compareToIgnoreCase(name2) ;
      else return name2.compareToIgnoreCase(name1) ;
    }
  }
  static public class EmailComparator implements Comparator {
    private static boolean isAsc ;
    public int compare(Object o1, Object o2) throws ClassCastException {
      String email1 = ((Contact) o1).getEmailAddress() ;
      String email2 = ((Contact) o2).getEmailAddress() ;
      if (ContactUtils.isEmpty(email1) || ContactUtils.isEmpty(email2)) return 0 ;
      if (isAsc == true) return email1.compareToIgnoreCase(email2) ;
      else return email2.compareToIgnoreCase(email1) ;
    }
  }
  static public class JobTitleComparator implements Comparator {
    private static boolean isAsc ;
    public int compare(Object o1, Object o2) throws ClassCastException {
      String job1 = ((Contact) o1).getJobTitle() ;
      String job2 = ((Contact) o2).getJobTitle() ;
      if (ContactUtils.isEmpty(job1) || ContactUtils.isEmpty(job2)) return 0 ;
      if (isAsc == true) return job1.compareToIgnoreCase(job2) ;
      else return job2.compareToIgnoreCase(job1) ;
    }
  }
  
  static public class CloseSearchActionListener extends EventListener<UIContacts> {
    public void execute(Event<UIContacts> event) throws Exception {
      UIContacts uiContacts = event.getSource() ;
      uiContacts.setDisplaySearchResult(false) ;
      uiContacts.setContacts(null) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiContacts.getParent()) ;
    }
  }
  
  static public class SelectTagActionListener extends EventListener<UIContacts> {
    public void execute(Event<UIContacts> event) throws Exception {
      UIContacts uiContacts = event.getSource() ;
      String tagId = event.getRequestContext().getRequestParameter(OBJECTID) ; 
      UIWorkingContainer uiWorkingContainer = uiContacts.getAncestorOfType(UIWorkingContainer.class) ;
      uiWorkingContainer.findFirstComponentOfType(UIAddressBooks.class).setSelectedGroup(null) ;
      UITags tags = uiWorkingContainer.findFirstComponentOfType(UITags.class) ;
      tags.setSelectedTag(tagId) ;
      uiContacts.setContacts(ContactUtils.getContactService()
        .getContactPageListByTag(SessionProviderFactory.createSystemProvider(), ContactUtils.getCurrentUser(), tagId)) ;
      uiContacts.setSelectedGroup(null) ;
      uiContacts.setSelectedTag(tagId) ;
      uiContacts.setDisplaySearchResult(false) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiWorkingContainer) ;
    }
  }
  

 
  static public class SendEmailActionListener extends EventListener<UIContacts> {
    public void execute(Event<UIContacts> event) throws Exception {
      UIContacts uiContacts = event.getSource() ;
      String objectId = event.getRequestContext().getRequestParameter(OBJECTID);
      String emails = null ;
      
      if (!ContactUtils.isEmpty(objectId)) {
        if (uiContacts.contactMap.containsKey(objectId))
          emails = uiContacts.contactMap.get(objectId).getEmailAddress() ;
        else emails = objectId ;
      } else {
        List<String> contactIds = uiContacts.getCheckedContacts() ;
        if (contactIds.size() < 1) {
          UIApplication uiApp = uiContacts.getAncestorOfType(UIApplication.class) ;
          uiApp.addMessage(new ApplicationMessage("UIContacts.msg.checkContact-required", null,
              ApplicationMessage.WARNING)) ;
          event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
          return ;
        }
        StringBuffer buffer = new StringBuffer(uiContacts.contactMap.get(contactIds.get(0)).getEmailAddress()) ; 
        for (int i = 1; i < contactIds.size(); i ++) {
          buffer.append(", " + uiContacts.contactMap.get(contactIds.get(i)).getEmailAddress()) ;
        }
        emails = buffer.toString() ;
      }
      if (ContactUtils.isEmpty(emails)) {
        UIApplication uiApp = uiContacts.getAncestorOfType(UIApplication.class) ;
        uiApp.addMessage(new ApplicationMessage("UIContacts.msg.no-email-found", null,
            ApplicationMessage.WARNING)) ;
        event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
        return ;        
      }
      UIContactPortlet contactPortlet = uiContacts.getAncestorOfType(UIContactPortlet.class) ;
      UIPopupAction popupAction = contactPortlet.getChild(UIPopupAction.class) ;
      UIComposeForm uiComposeForm = popupAction.activate(UIComposeForm.class, 850) ;  
      uiComposeForm.init(emails) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(popupAction) ;  
      event.getRequestContext().addUIComponentToUpdateByAjax(uiContacts.getParent()) ;
    }
  }
  
  static public class SharedContactsActionListener extends EventListener<UIContacts> {
    public void execute(Event<UIContacts> event) throws Exception {
      UIContacts uiContacts = event.getSource() ;
      List<String> contacts = uiContacts.getCheckedContacts() ;
      Map<String, String> mapContacts = new LinkedHashMap<String, String>() ;
      for (String contactId : contacts) {
        if (uiContacts.contactMap.get(contactId).getContactType().equals(JCRDataStorage.PUBLIC)) {
          UIApplication uiApp = uiContacts.getAncestorOfType(UIApplication.class) ;
          uiApp.addMessage(new ApplicationMessage("UIContacts.msg.cannot-share", null
              , ApplicationMessage.WARNING)) ;
          event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
          return ;
        }
        mapContacts.put(contactId, uiContacts.contactMap.get(contactId).getFullName()) ;
      }      
      UIContactPortlet contactPortlet = uiContacts.getAncestorOfType(UIContactPortlet.class) ;
      UIPopupAction popupAction = contactPortlet.getChild(UIPopupAction.class) ;
      UIPopupContainer uiPopupContainer = popupAction.activate(UIPopupContainer.class, 600) ;
      uiPopupContainer.setId("UISharedContacts") ;
      UISharedForm uiSharedForm = uiPopupContainer.addChild(UISharedForm.class, null, null) ;  
      uiSharedForm.init(false) ;      
      uiSharedForm.setSharedContacts(mapContacts) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(popupAction) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiContacts.getParent());      
    }
  }
}
