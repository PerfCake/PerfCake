<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xalan="http://xml.apache.org/xslt" xmlns:pc4="urn:perfcake:scenario:4.0" xmlns:pc5="urn:perfcake:scenario:5.0" version="1.0" exclude-result-prefixes="pc4 pc5 xalan">
   <xsl:output method="xml" version="1.0" encoding="utf-8" indent="yes" xalan:indent-amount="3" cdata-section-elements="pc5:validator"/>
   <xsl:template name="property" match="pc4:property">
      <property xmlns="urn:perfcake:scenario:5.0">
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
   <xsl:template name="header" match="pc4:header">
      <header xmlns="urn:perfcake:scenario:5.0">
         <xsl:attribute name="name">
            <xsl:value-of select="@name"/>
         </xsl:attribute>
         <xsl:attribute name="value">
            <xsl:value-of select="@value"/>
         </xsl:attribute>
      </header>
   </xsl:template>
   <xsl:template name="period" match="pc4:period">
      <period xmlns="urn:perfcake:scenario:5.0">
         <xsl:attribute name="type">
            <xsl:value-of select="@type"/>
         </xsl:attribute>
         <xsl:attribute name="value">
            <xsl:value-of select="@value"/>
         </xsl:attribute>
      </period>
   </xsl:template>
   <xsl:template match="/">
      <scenario xmlns="urn:perfcake:scenario:5.0">
         <xsl:if test="pc4:scenario/pc4:properties">
            <properties>
               <xsl:for-each select="pc4:scenario/pc4:properties/pc4:property">
                  <xsl:call-template name="property"/>
               </xsl:for-each>
            </properties>
         </xsl:if>
         <run>
            <xsl:attribute name="type">
               <xsl:value-of select="pc4:scenario/pc4:generator/pc4:run/@type"/>
            </xsl:attribute>
            <xsl:attribute name="value">
               <xsl:value-of select="pc4:scenario/pc4:generator/pc4:run/@value"/>
            </xsl:attribute>
         </run>
         <generator>
            <xsl:attribute name="class">
               <xsl:value-of select="pc4:scenario/pc4:generator/@class"/>
            </xsl:attribute>
            <xsl:attribute name="threads">
               <xsl:value-of select="pc4:scenario/pc4:generator/@threads"/>
            </xsl:attribute>
            <xsl:for-each select="pc4:scenario/pc4:generator/pc4:property">
               <xsl:call-template name="property"/>
            </xsl:for-each>
         </generator>
         <sender>
            <xsl:attribute name="class">
               <xsl:value-of select="pc4:scenario/pc4:sender/@class"/>
            </xsl:attribute>
            <target><xsl:value-of select="pc4:scenario/pc4:sender/pc4:property[@name='target']/@value"/></target>
            <xsl:for-each select="pc4:scenario/pc4:sender/pc4:property">
               <xsl:if test="not(@name = 'target')">
                  <xsl:call-template name="property"/>
               </xsl:if>
            </xsl:for-each>
         </sender>
         <xsl:if test="pc4:scenario/pc4:reporting">
            <reporting>
               <xsl:for-each select="pc4:scenario/pc4:reporting/pc4:reporter">
                  <reporter>
                     <xsl:attribute name="class">
                         <xsl:value-of select="@class"/>
                     </xsl:attribute>
                     <xsl:if test="@enabled">
                        <xsl:attribute name="enabled">
                           <xsl:value-of select="@enabled"/>
                        </xsl:attribute>
                     </xsl:if>
                     <xsl:for-each select="pc4:property">
                        <xsl:call-template name="property"/>
                     </xsl:for-each>
                     <xsl:for-each select="pc4:destination">
                        <destination>
                           <xsl:attribute name="class">
                              <xsl:value-of select="@class"/>
                           </xsl:attribute>
                           <xsl:if test="@enabled">
                              <xsl:attribute name="enabled">
                                 <xsl:value-of select="@enabled"/>
                              </xsl:attribute>
                           </xsl:if>
                           <xsl:for-each select="pc4:period">
                              <xsl:call-template name="period"/>
                           </xsl:for-each>
                           <xsl:for-each select="pc4:property">
                              <xsl:call-template name="property"/>
                           </xsl:for-each>
                        </destination>
                     </xsl:for-each>
                  </reporter>
               </xsl:for-each>
            </reporting>
         </xsl:if>
         <xsl:if test="pc4:scenario/pc4:messages">
            <messages>
               <xsl:for-each select="pc4:scenario/pc4:messages/pc4:message">
                  <message>
                     <xsl:if test="@uri">
                        <xsl:attribute name="uri">
                           <xsl:value-of select="@uri"/>
                        </xsl:attribute>
                     </xsl:if>
                     <xsl:if test="@content">
                        <xsl:attribute name="content">
                           <xsl:value-of select="@content"/>
                        </xsl:attribute>
                     </xsl:if>
                     <xsl:if test="@multiplicity">
                        <xsl:attribute name="multiplicity">
                           <xsl:value-of select="@multiplicity"/>
                        </xsl:attribute>
                     </xsl:if>
                     <xsl:for-each select="pc4:property">
                        <xsl:call-template name="property"/>
                     </xsl:for-each>
                     <xsl:for-each select="pc4:header">
                        <xsl:call-template name="header"/>
                     </xsl:for-each>
                     <xsl:for-each select="pc4:validatorRef">
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
         <xsl:if test="pc4:scenario/pc4:validation">
            <validation>
               <xsl:for-each select="pc4:scenario/pc4:validation/pc4:validator">
                  <validator>
                     <xsl:attribute name="id">
                        <xsl:value-of select="@id"/>
                     </xsl:attribute>
                     <xsl:attribute name="class">
                        <xsl:value-of select="@class"/>
                     </xsl:attribute>
                     <xsl:text><xsl:copy-of select="node()"/></xsl:text>
                  </validator>
               </xsl:for-each>
            </validation>
         </xsl:if>
      </scenario>
   </xsl:template>
</xsl:stylesheet>
