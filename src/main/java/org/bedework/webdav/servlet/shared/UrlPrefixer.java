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
package org.bedework.webdav.servlet.shared;

/** Prefix urls - or not depending on internal state
*
* @author douglm
*/
public interface UrlPrefixer {
  /** Return an appropriately prefixed url. The parameter url will be
   * absolute or relative. If relative it may be prefixed with the context
   * path which we need to remove.
   *
   * <p>We're doing this because some clients don't handle absolute urls
   * (a violation of the spec)
   *
   * @param val to prefix
   * @return String
   * @throws RuntimeException on error
   */
  String prefix(String val);
}
