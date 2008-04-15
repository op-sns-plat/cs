/*
 * Copyright (C) 2003-2008 eXo Platform SAS.
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
package org.exoplatform.mail.service;

/**
 * Created by The eXo Platform SAS
 * Author : Nam Phung
 *          phunghainam@gmail.com
 * Apr 1, 2008  
 */
public class CheckingInfo {
  private int totalMsg_ ;
  private int fetching_ ;
  private int statusCode_ ;
  private boolean hasChanged_ ;
  
  public int getTotalMsg() {  return totalMsg_ ; } ;
  public void setTotalMsg(int totalMsg) { 
    totalMsg_ = totalMsg ; 
    hasChanged_ = true ;
  }
  
  public int getFetching() { return fetching_ ; }
  public void setFetching(int in) { 
    fetching_ = in ; 
    hasChanged_ = true ;
  }
  
  public int getStatusCode() { return statusCode_ ; }
  public void setStatusCode(int code) { 
    statusCode_ = code ; 
    hasChanged_ = true ;
  }
  
  public boolean hasChanged() { return hasChanged_ ; }
  public void setHasChanged(boolean b) { hasChanged_ = b ; }
}
