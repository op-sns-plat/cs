/***************************************************************************
 * Copyright 2001-2006 The eXo Platform SARL         All rights reserved.  *
 * Please look at license.txt in info directory for more license detail.   *
 **************************************************************************/
package org.exoplatform.mail.webui.popup;

import java.util.ArrayList;
import java.util.List;

import org.exoplatform.mail.MailUtils;
import org.exoplatform.mail.service.MailService;
import org.exoplatform.mail.service.Tag;
import org.exoplatform.mail.service.Utils;
import org.exoplatform.mail.webui.UIMailPortlet;
import org.exoplatform.mail.webui.UISelectAccount;
import org.exoplatform.mail.webui.UITagContainer;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.core.model.SelectItemOption;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormSelectBox;
import org.exoplatform.webui.form.UIFormStringInput;
import org.exoplatform.webui.form.UIFormTextAreaInput;


/**
 * Created by The eXo Platform SARL
 * Author : Hai Nguyen      
 * Aus 01, 2007 2:48:18 PM 
 */
@ComponentConfig(
    lifecycle = UIFormLifecycle.class,
    template = "system:/groovy/webui/form/UIForm.gtmpl",
    events = {
      @EventConfig(listeners = UIRenameTagForm.SaveActionListener.class), 
      @EventConfig(listeners = UIRenameTagForm.CancelActionListener.class)
    }
)
public class UIRenameTagForm extends UIForm implements UIPopupComponent {

  final public static String NEW_TAG_NAME = "newTagName" ;
  final public static String DESCRIPTION = "description" ;
  final public static String COLOR = "color" ;
  
  private String tagId;
  public UIRenameTagForm() {       
    addUIFormInput(new UIFormStringInput(NEW_TAG_NAME, NEW_TAG_NAME, null)) ;
    List<SelectItemOption<String>> tagColors = new ArrayList<SelectItemOption<String>>();
    for (String color : Utils.TAG_COLOR) {
      tagColors.add(new SelectItemOption<String>(color, color));
    }
    UIFormSelectBox selectColor = new UIFormSelectBox(COLOR, COLOR, tagColors);
    addUIFormInput(new UIFormTextAreaInput(DESCRIPTION,DESCRIPTION,null)) ;    
    addUIFormInput(selectColor);
  }
  
  public String getTagId() throws Exception { return tagId; }
  
  public void setTag(String tagId) throws Exception {
    this.tagId = tagId;
    
    MailService mailSrv = getApplicationComponent(MailService.class);
    String username = MailUtils.getCurrentUser();
    String accountId = getAncestorOfType(UIMailPortlet.class).findFirstComponentOfType(UISelectAccount.class).getSelectedValue();
    List<Tag> tagList= mailSrv.getTags(username, accountId);
    
    if (tagList.isEmpty()) return;   
    
    for (Tag tag : tagList) {      
      if (tag.getId().equals(tagId)){
        getUIStringInput(NEW_TAG_NAME).setValue(tag.getName()); 
        getUIFormTextAreaInput(DESCRIPTION).setValue(tag.getDescription());
        getUIFormSelectBox(COLOR).setValue(tag.getColor());       
      }
    }
  }
  
  public void activate() throws Exception {}
  public void deActivate() throws Exception{}
 
  static  public class SaveActionListener extends EventListener<UIRenameTagForm> {
    public void execute(Event<UIRenameTagForm> event) throws Exception {
      System.out.println("SaveActionListener() ");
      UIRenameTagForm uiRenameTagForm  = event.getSource() ;
      UIMailPortlet uiMailPortlet = uiRenameTagForm.getAncestorOfType(UIMailPortlet.class);
      MailService mailService = uiRenameTagForm.getApplicationComponent(MailService.class) ;

      String username = uiMailPortlet.getCurrentUser() ;
      String accountId =  uiMailPortlet.findFirstComponentOfType(UISelectAccount.class).getSelectedValue() ;
      String tagId = uiRenameTagForm.getTagId();
      String newTagName = uiRenameTagForm.getUIStringInput(NEW_TAG_NAME).getValue() ;
      String description = uiRenameTagForm.getUIFormTextAreaInput(DESCRIPTION).getValue() ;
      System.out.println("description :"+description);
      String color = uiRenameTagForm.getUIFormSelectBox(COLOR).getValue(); 

      UIApplication uiApp = uiRenameTagForm.getAncestorOfType(UIApplication.class) ;
      UITagContainer uiTagContainer = uiMailPortlet.findFirstComponentOfType(UITagContainer.class) ;

      if(Utils.isEmptyField(newTagName)) {
        uiApp.addMessage(new ApplicationMessage("UITagForm.msg.name-required", null)) ;
        event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
        return ;
      }

      try {      
        uiRenameTagForm.setTag(tagId);        
        List<Tag> tagList = mailService.getTags(username, accountId);
        for (Tag tag : tagList) {
          if(tag.getName().equals(newTagName)&&!tag.getId().equals(tagId)) {
            uiApp.addMessage(new ApplicationMessage("UITagForm.msg.Tag-exist", new Object[]{newTagName})) ;
            event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
            return ;
          }
          if (tag.getId().equals(tagId)){
            tag.setName(newTagName);
            tag.setColor(color);
            tag.setDescription(description);
            mailService.updateTag(username, accountId, tag);
          }
        }
      } catch (Exception e){
        uiApp.addMessage(new ApplicationMessage("UIRenameTagForm.msg.error-rename-tag", null)) ;
        e.printStackTrace() ;
      }
      uiRenameTagForm.getAncestorOfType(UIPopupAction.class).deActivate() ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiRenameTagForm.getAncestorOfType(UIPopupAction.class)) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiTagContainer) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
    }
  }
  
  static  public class CancelActionListener extends EventListener<UIRenameTagForm> {
    public void execute(Event<UIRenameTagForm> event) throws Exception {
      UIRenameTagForm uiForm = event.getSource() ;
      uiForm.getAncestorOfType(UIPopupAction.class).deActivate() ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiForm.getAncestorOfType(UIPopupAction.class)) ;
    }
  }
}
