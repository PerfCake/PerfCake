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
import org.perfcake.util.properties.MandatoryProperty;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;

/**
 * Queries LDAP server.
 *
 * @author Vojtěch Juránek
 */
public class LdapSender extends AbstractSender {

   private static final Logger logger = LogManager.getLogger(LdapSender.class);

   private LdapContext ctx = null;
   private String ldapUsername = null;
   private String ldapPassword = null;
   private final SearchControls searchControls = new SearchControls();

   @MandatoryProperty
   private String searchBase = null;

   @MandatoryProperty
   private String filter = null;

   /**
    * Gets the LDAP username.
    *
    * @return The LDAP username.
    */
   public String getLdapUsername() {
      return ldapUsername;
   }

   /**
    * Sets the LDAP username.
    *
    * @param ldapUsername
    *       The LDAP username.
    * @return Instance of this to support fluent API.
    */
   public LdapSender setLdapUsername(final String ldapUsername) {
      this.ldapUsername = ldapUsername;
      return this;
   }

   /**
    * Gets the LDAP password.
    *
    * @return The LDAP password.
    */
   public String getLdapPassword() {
      return ldapPassword;
   }

   /**
    * Sets the LDAP password.
    *
    * @param ldapPassword
    *       The LDAP password.
    * @return Instance of this to support fluent API.
    */
   public LdapSender setLdapPassword(final String ldapPassword) {
      this.ldapPassword = ldapPassword;
      return this;
   }

   /**
    * Gets the LDAP search base.
    *
    * @return The LDAP search base.
    */
   public String getSearchBase() {
      return searchBase;
   }

   /**
    * Sets the LDAP search base.
    *
    * @param searchBase
    *       The LDAP search base.
    * @return Instance of this to support fluent API.
    */
   public LdapSender setSearchBase(final String searchBase) {
      this.searchBase = searchBase;
      return this;
   }

   /**
    * Gets the LDAP filter.
    *
    * @return The LDAP filter.
    */
   public String getFilter() {
      return filter;
   }

   /**
    * Sets the LDAP filter.
    *
    * @param filter
    *       The LDAP filter.
    * @return Instance of this to support fluent API.
    */
   public LdapSender setFilter(final String filter) {
      this.filter = filter;
      return this;
   }

   @Override
   public void doInit(final Properties messageAttributes) throws PerfCakeException {
      final Hashtable<String, Object> env = new Hashtable<String, Object>();
      env.put(Context.SECURITY_AUTHENTICATION, "simple");
      if (ldapUsername != null) {
         env.put(Context.SECURITY_PRINCIPAL, ldapUsername);
      }
      if (ldapPassword != null) {
         env.put(Context.SECURITY_CREDENTIALS, ldapPassword);
      }
      env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
      env.put(Context.PROVIDER_URL, safeGetTarget(messageAttributes));

      if (logger.isTraceEnabled()) {
         logger.trace("Connecting to " + safeGetTarget(messageAttributes));
      }
      try {
         ctx = new InitialLdapContext(env, null);
      } catch (NamingException e) {
         throw new PerfCakeException("Cannot create LDAP context: ", e);
      }

      searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
   }

   @Override
   public void doClose() throws PerfCakeException {
      try {
         ctx.close();
      } catch (final NamingException e) {
         throw new PerfCakeException("Failed to close LDAP context.", e.getCause());
      }
   }

   @Override
   public void preSend(final Message message, final Map<String, String> properties, final Properties messageAttributes) throws Exception {
      super.preSend(message, properties, messageAttributes);
      if (searchBase == null || filter == null) {
         throw new PerfCakeException("LDAP search base or filter is not set. Both properties have to be set up");
      }
   }

   @Override
   public Serializable doSend(final Message message, final Map<String, String> properties, final MeasurementUnit measurementUnit) throws Exception {
      final NamingEnumeration<SearchResult> results = ctx.search(searchBase, filter, searchControls);
      final ArrayList<SearchResult> res = new ArrayList<SearchResult>();
      while (results.hasMoreElements()) {
         res.add(results.nextElement());
      }
      return res;
   }
}
