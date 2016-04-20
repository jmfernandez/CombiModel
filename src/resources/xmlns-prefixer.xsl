<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    version="1.0"
>
    <xsl:template match="node()|@*">
        <xsl:copy>
            <xsl:apply-templates select="node()|@*"/>
        </xsl:copy>
    </xsl:template>
    
    <xsl:template match="*">
        <xsl:variable name="elemname">
            <xsl:choose>
                <xsl:when test="name() = local-name()">defns:<xsl:value-of select="local-name()"/></xsl:when>
                <xsl:otherwise><xsl:value-of select="name()"/></xsl:otherwise>
            </xsl:choose>
        </xsl:variable>
        <xsl:element name="{$elemname}" namespace="{namespace-uri()}">
            <!--
            <xsl:if test="namespace::*[name() = '']">
                <xsl:message>
                    Local name '<xsl:value-of select="name()" />' Prefix '<xsl:value-of select="namespace-uri()" />'
                </xsl:message>
            </xsl:if>
            -->
            <!-- Removing default namespace declarations -->
            <xsl:copy-of select="namespace::*[name() != '']"/>
            <xsl:apply-templates select="node()|@*"/>
        </xsl:element>
    </xsl:template>
</xsl:stylesheet>
