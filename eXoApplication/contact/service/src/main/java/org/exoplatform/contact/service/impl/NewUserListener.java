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
package org.exoplatform.contact.service.impl;

import org.exoplatform.contact.service.ContactService;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.UserEventListener;

/**
 * Created by The eXo Platform SARL
 * Author : Hung Nguyen Quang
 *          hung.nguyen@exoplatform.com
 * Nov 23, 2007 3:09:21 PM
 */
public class NewUserListener extends UserEventListener {
  private ContactService cservice_;

  public static String   DEFAULTGROUP       = "default".intern();

  public static String   ADDRESSESGROUP     = "addresses".intern();

  public static String   ADDRESSESGROUPNAME = "collected-email-adresses".intern();

  public static String   DEFAULTGROUPNAME   = "My contacts".intern();

  public static String   DEFAULTGROUPDES    = "Default address book".intern();

  public NewUserListener(ContactService cservice, NodeHierarchyCreator nodeHierarchyCreator) throws Exception {
    cservice_ = cservice;
  }

  public void postSave(User user, boolean isNew) throws Exception {
    cservice_.registerNewUser(user, isNew);
  }

  public void preDelete(User user) throws Exception {
  }
}
