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
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * -----------------------------------------------------------------------/
 */
package org.perfcake.message.sender;

import org.perfcake.PerfCakeException;
import org.perfcake.message.Message;
import org.perfcake.reporting.MeasurementUnit;

import org.apache.log4j.Logger;

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

/**
 * The sender which queries LDAP server.
 *
 * @author vjuranek
 */
public class LdapSender extends AbstractSender {

   private static final Logger logger = Logger.getLogger(LdapSender.class);

   private LdapContext ctx = null;
   private String ldapUsername = null;
   private String ldapPassword = null;
   private SearchControls searchControls = new SearchControls();

   private String searchBase = null;
   private String filter = null;

   public String getLdapUsername() {
      return ldapUsername;
   }

   public LdapSender setLdapUsername(String ldapUsername) {
      this.ldapUsername = ldapUsername;
      return this;
   }

   public String getLdapPassword() {
      return ldapPassword;
   }

   public LdapSender setLdapPassword(String ldapPassword) {
      this.ldapPassword = ldapPassword;
      return this;
   }

   public String getSearchBase() {
      return searchBase;
   }

   public LdapSender setSearchBase(String searchBase) {
      this.searchBase = searchBase;
      return this;
   }

   public String getFilter() {
      return filter;
   }

   public LdapSender setFilter(String filter) {
      this.filter = filter;
      return this;
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

      if (logger.isDebugEnabled()) {
         logger.debug("Connecting to " + target);
      }
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
   public void preSend(final Message message, final Map<String, String> properties) throws Exception {
      super.preSend(message, properties);
      if (searchBase == null || filter == null) {
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
