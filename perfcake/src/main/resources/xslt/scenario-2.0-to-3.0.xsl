<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xalan="http://xml.apache.org/xslt" xmlns:pc2="urn:perfcake:scenario:2.0" xmlns:pc3="urn:perfcake:scenario:3.0" version="1.0" exclude-result-prefixes="pc2 pc3 xalan">
   <xsl:output method="xml" version="1.0" encoding="utf-8" indent="yes" xalan:indent-amount="3" cdata-section-elements="pc3:validator"/>
   <xsl:template name="property" match="pc2:property">
      <property xmlns="urn:perfcake:scenario:3.0">
         <xsl:attribute name="name">
            <xsl:value-of select="@name"/>
         </xsl:attribute>
         <xsl:choose>
            <xsl:when test="@value">
               <xsl:attribute name="value">
               <xsl:value-of select="@value"/>
            </xsl:attribute>
            </xsl:when>
            <xsl:otherwise>
               <xsl:copy-of select="node()"/>
            </xsl:otherwise>
         </xsl:choose>
      </property>
   </xsl:template>
   <xsl:template name="header" match="pc2:header">
      <header xmlns="urn:perfcake:scenario:3.0">
         <xsl:attribute name="name">
            <xsl:value-of select="@name"/>
         </xsl:attribute>
         <xsl:attribute name="value">
            <xsl:value-of select="@value"/>
         </xsl:attribute>
      </header>
   </xsl:template>
   <xsl:template name="period" match="pc2:period">
      <period xmlns="urn:perfcake:scenario:3.0">
         <xsl:attribute name="type">
            <xsl:value-of select="@type"/>
         </xsl:attribute>
         <xsl:attribute name="value">
            <xsl:value-of select="@value"/>
         </xsl:attribute>
      </period>
   </xsl:template>
   <xsl:template match="/">
      <scenario xmlns="urn:perfcake:scenario:3.0">
         <xsl:if test="pc2:scenario/pc2:properties">
            <properties>
               <xsl:for-each select="pc2:scenario/pc2:properties/pc2:property">
                  <xsl:call-template name="property"/>
               </xsl:for-each>
            </properties>
         </xsl:if>
         <generator>
            <xsl:attribute name="class">
               <xsl:value-of select="pc2:scenario/pc2:generator/@class"/>
            </xsl:attribute>
            <xsl:attribute name="threads">
               <xsl:value-of select="pc2:scenario/pc2:generator/@threads"/>
            </xsl:attribute>
            <run>
               <xsl:attribute name="type">
                  <xsl:value-of select="pc2:scenario/pc2:generator/pc2:run/@type"/>
               </xsl:attribute>
               <xsl:attribute name="value">
                  <xsl:value-of select="pc2:scenario/pc2:generator/pc2:run/@value"/>
               </xsl:attribute>
            </run>
            <xsl:for-each select="pc2:scenario/pc2:generator/pc2:property">
               <xsl:call-template name="property"/>
            </xsl:for-each>
         </generator>
         <sender>
            <xsl:attribute name="class">
               <xsl:choose>
                  <xsl:when test="pc2:scenario/pc2:sender/@class = 'HTTPSender'">HttpSender</xsl:when>
                  <xsl:when test="pc2:scenario/pc2:sender/@class = 'HTTPSSender'">HttpsSender</xsl:when>
                  <xsl:when test="pc2:scenario/pc2:sender/@class = 'JDBCSender'">JdbcSender</xsl:when>
                  <xsl:when test="pc2:scenario/pc2:sender/@class = 'JMSSender'">JmsSender</xsl:when>
                  <xsl:when test="pc2:scenario/pc2:sender/@class = 'RequestResponseJMSSender'">RequestResponseJmsSender</xsl:when>
                  <xsl:when test="pc2:scenario/pc2:sender/@class = 'SOAPSocketSender'">SoapSocketSender</xsl:when>
                  <xsl:when test="pc2:scenario/pc2:sender/@class = 'SSLSocketSender'">SslSocketSender</xsl:when>
                  <xsl:otherwise>
                     <xsl:value-of select="pc2:scenario/pc2:sender/@class"/>
                  </xsl:otherwise>
               </xsl:choose>
            </xsl:attribute>
            <xsl:for-each select="pc2:scenario/pc2:sender/pc2:property">
               <xsl:call-template name="property"/>
            </xsl:for-each>
            <xsl:if test="pc2:scenario/pc2:sender/@class = 'RequestResponseJMSSender'">
               <xsl:if test="pc2:scenario/pc2:sender/pc2:property[@name='username']">
                  <property name="responseUsername">
                     <xsl:attribute name="value">
                     <xsl:value-of select="pc2:scenario/pc2:sender/pc2:property[@name='username']/@value"/>
                  </xsl:attribute>
                  </property>
               </xsl:if>
               <xsl:if test="pc2:scenario/pc2:sender/pc2:property[@name='password']">
                  <property name="responsePassword">
                     <xsl:attribute name="value">
                     <xsl:value-of select="pc2:scenario/pc2:sender/pc2:property[@name='password']/@value"/>
                  </xsl:attribute>
                  </property>
               </xsl:if>
               <property name="responseJndiSecurityPrincipal">
                  <xsl:attribute name="value">
                     <xsl:value-of select="pc2:scenario/pc2:sender/pc2:property[@name='jndiSecurityPrincipal']/@value"/>
                  </xsl:attribute>
               </property>
               <property name="responseJndiSecurityCredentials">
                  <xsl:attribute name="value">
                     <xsl:value-of select="pc2:scenario/pc2:sender/pc2:property[@name='jndiSecurityCredentials']/@value"/>
                  </xsl:attribute>
               </property>
            </xsl:if>
         </sender>
         <xsl:if test="pc2:scenario/pc2:reporting">
            <reporting>
               <xsl:for-each select="pc2:scenario/pc2:reporting/pc2:reporter">
                  <reporter>
                     <xsl:choose>
                        <xsl:when test="@class = 'AverageThroughputReporter'">
                           <xsl:attribute name="class">ThroughputStatsReporter</xsl:attribute>
                           <xsl:if test="@enabled">
                              <xsl:attribute name="enabled">
                                 <xsl:value-of select="@enabled"/>
                              </xsl:attribute>
                           </xsl:if>
                           <property name="minimumEnabled" value="false"/>
                           <property name="maximumEnabled" value="false"/>
                        </xsl:when>
                        <xsl:when test="@class = 'ResponseTimeReporter'">
                           <xsl:attribute name="class">ResponseTimeStatsReporter</xsl:attribute>
                           <xsl:if test="@enabled">
                              <xsl:attribute name="enabled">
                                 <xsl:value-of select="@enabled"/>
                              </xsl:attribute>
                           </xsl:if>
                           <property name="minimumEnabled" value="false"/>
                           <property name="maximumEnabled" value="false"/>
                        </xsl:when>
                        <xsl:when test="@class = 'ResponseTimeReporter'">
                           <xsl:attribute name="class">WindowResponseTimeStatsReporter</xsl:attribute>
                           <xsl:if test="@enabled">
                              <xsl:attribute name="enabled">
                                 <xsl:value-of select="@enabled"/>
                              </xsl:attribute>
                           </xsl:if>
                           <property name="minimumEnabled" value="false"/>
                           <property name="maximumEnabled" value="false"/>
                        </xsl:when>
                        <xsl:otherwise>
                           <xsl:attribute name="class">
                              <xsl:value-of select="@class"/>
                           </xsl:attribute>
                           <xsl:if test="@enabled">
                              <xsl:attribute name="enabled">
                                 <xsl:value-of select="@enabled"/>
                              </xsl:attribute>
                           </xsl:if>
                        </xsl:otherwise>
                     </xsl:choose>
                     <xsl:for-each select="pc2:property">
                        <xsl:call-template name="property"/>
                     </xsl:for-each>
                     <xsl:for-each select="pc2:destination">
                        <destination>
                           <xsl:choose>
                              <xsl:when test="@class = 'CSVDestination'">
                                 <xsl:attribute name="class">CsvDestination</xsl:attribute>
                              </xsl:when>
                              <xsl:otherwise>
                                 <xsl:attribute name="class">
                                    <xsl:value-of select="@class"/>
                                 </xsl:attribute>
                              </xsl:otherwise>
                           </xsl:choose>
                           <xsl:for-each select="pc2:period">
                              <xsl:call-template name="period"/>
                           </xsl:for-each>
                           <xsl:for-each select="pc2:property">
                              <xsl:call-template name="property"/>
                           </xsl:for-each>
                        </destination>
                     </xsl:for-each>
                  </reporter>
               </xsl:for-each>
            </reporting>
         </xsl:if>
         <xsl:if test="pc2:scenario/pc2:messages">
            <messages>
               <xsl:for-each select="pc2:scenario/pc2:messages/pc2:message">
                  <message>
                     <xsl:attribute name="uri">
                        <xsl:value-of select="@uri"/>
                     </xsl:attribute>
                     <xsl:if test="@multiplicity">
                        <xsl:attribute name="multiplicity">
                           <xsl:value-of select="@multiplicity"/>
                        </xsl:attribute>
                     </xsl:if>
                     <xsl:for-each select="pc2:property">
                        <xsl:call-template name="property"/>
                     </xsl:for-each>
                     <xsl:for-each select="pc2:header">
                        <xsl:call-template name="header"/>
                     </xsl:for-each>
                     <xsl:for-each select="pc2:validatorRef">
                        <validatorRef>
                           <xsl:attribute name="id">
                              <xsl:value-of select="@id"/>
                           </xsl:attribute>
                        </validatorRef>
                     </xsl:for-each>
                  </message>
               </xsl:for-each>
            </messages>
         </xsl:if>
         <xsl:if test="pc2:scenario/pc2:validation">
            <validation>
               <xsl:for-each select="pc2:scenario/pc2:validation/pc2:validator">
                  <validator>
                     <xsl:attribute name="id">
                        <xsl:value-of select="@id"/>
                     </xsl:attribute>
                     <xsl:attribute name="class">
                        <xsl:choose>
                           <xsl:when test="@class = 'TextMessageValidator'">RegExpValidator</xsl:when>
                           <xsl:when test="@class = 'RulesMessageValidator'">RulesValidator</xsl:when>
                           <xsl:otherwise>
                              <xsl:value-of select="@class"/>
                           </xsl:otherwise>
                        </xsl:choose>
                     </xsl:attribute>
                     <xsl:text><xsl:copy-of select="node()"/></xsl:text>
                  </validator>
               </xsl:for-each>
            </validation>
         </xsl:if>
      </scenario>
   </xsl:template>
</xsl:stylesheet>
