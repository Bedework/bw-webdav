/* ********************************************************************
    Licensed to Jasig under one or more contributor license
    agreements. See the NOTICE file distributed with this work
    for additional information regarding copyright ownership.
    Jasig licenses this file to you under the Apache License,
    Version 2.0 (the "License"); you may not use this file
    except in compliance with the License. You may obtain a
    copy of the License at:
        
    http://www.apache.org/licenses/LICENSE-2.0
        
    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on
    an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied. See the License for the
    specific language governing permissions and limitations
    under the License.
*/
package edu.rpi.cct.webdav.servlet.shared;

import edu.rpi.cct.webdav.servlet.common.PropFindMethod;

import java.util.ArrayList;
import java.util.Collection;

import org.w3c.dom.Element;

/**
 * @author Mike Douglass
 */
public class PrincipalPropertySearch {
  /**
   */
  public static class PropertySearch {
    /**
     */
    public Collection<WebdavProperty> props;

    /**
     */
    public Element match;
  }

  /** Collection of PropertySearch objects.
   */
  public Collection<PropertySearch> propertySearches =
    new ArrayList<PropertySearch>();

  /** Properties to be returned
   */
  public PropFindMethod.PropRequest pr;

  /** If true the request is applied to each collection identified by the
      DAV:principal-collection-set property of the resource identified
      by the Request-URI.
      */
  public boolean applyToPrincipalCollectionSet;
}
