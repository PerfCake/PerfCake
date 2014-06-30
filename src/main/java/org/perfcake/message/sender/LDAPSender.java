/*
 * -----------------------------------------------------------------------\
 * PerfCake
 *  
 * Copyright (C) 2010 - 2013 the original author or authors.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * -----------------------------------------------------------------------/
 */
package org.perfcake.message.sender;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Map;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;

import org.apache.log4j.Logger;
import org.perfcake.PerfCakeException;
import org.perfcake.message.Message;
import org.perfcake.reporting.MeasurementUnit;

/**
 * 
 * The sender which queries LDAP server.
 * 
 * @author vjuranek
 * 
 */
public class LDAPSender extends AbstractSender {

   private static final Logger logger = Logger.getLogger(LDAPSender.class);
   private static final String SEARCH_BASE_PROP_NAME = "searchBase";
   private static final String FILTER_PROP_NAME = "filter";

   private LdapContext ctx = null;
   private String ldapUsername = null;
   private String ldapPassword = null;
   private SearchControls searchControls = new SearchControls();
   
   private String searchBase = null;
   private String filter = null;

   public String getLdapUsername() {
      return ldapUsername;
   }

   public void setLdapUsername(String ldapUsername) {
      this.ldapUsername = ldapUsername;
   }

   public String getLdapPassword() {
      return ldapPassword;
   }

   public void setLdapPassword(String ldapPassword) {
      this.ldapPassword = ldapPassword;
   }

   @Override
   public void init() throws Exception {
      Hashtable<String, Object> env = new Hashtable<String, Object>();
      env.put(Context.SECURITY_AUTHENTICATION, "simple");
      if (ldapUsername != null) {
         env.put(Context.SECURITY_PRINCIPAL, ldapUsername);
      }
      if (ldapPassword != null) {
         env.put(Context.SECURITY_CREDENTIALS, ldapPassword);
      }
      env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
      env.put(Context.PROVIDER_URL, target);

      logger.debug("Connecting to " + target);
      ctx = new InitialLdapContext(env, null);

      searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
   }

   @Override
   public void close() throws PerfCakeException {
      try {
         ctx.close();
      } catch (NamingException e) {
         throw new PerfCakeException("Failed to close LDAP context.", e.getCause());
      }
   }

   @Override
   public void preSend(final Message message, final Map<String, String> properties) throws PerfCakeException {
      searchBase = message.getProperty(SEARCH_BASE_PROP_NAME);
      filter = message.getProperty(FILTER_PROP_NAME);
      if(searchBase == null || filter == null) {
         throw new PerfCakeException("LDAP search base or filter is not set. Both properties have to be set up");
      }
   }
   
   @Override
   public Serializable doSend(final Message message, final Map<String, String> properties, final MeasurementUnit mu) throws Exception {
      NamingEnumeration<SearchResult> results = ctx.search(searchBase, filter, searchControls);
      ArrayList<SearchResult> res = new ArrayList<SearchResult>();
      while (results.hasMoreElements()) {
         res.add(results.nextElement());
      }
      return res;
   }
}
