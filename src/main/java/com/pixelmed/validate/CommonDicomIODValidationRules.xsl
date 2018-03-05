<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	version="1.0">

<xsl:variable name="newline">
<xsl:text>
</xsl:text>
</xsl:variable>

<xsl:variable name="optionDescribeChecking"><xsl:value-of select="$newline"/></xsl:variable>

<xsl:variable name="optionWarnOnMayBePresentOtherwiseAnyway">F</xsl:variable>

<xsl:template name="buildFullPathInInstanceToNamedNode">
	<xsl:param name="node"/>
	<xsl:if test="name($node/..) != 'DicomObject'">
		<xsl:call-template name="buildFullPathInInstanceToNamedNode">
			<xsl:with-param name="node" select="$node/.."/>
		</xsl:call-template>
	</xsl:if>
	<xsl:text>/</xsl:text><xsl:value-of select="name($node)"/>
</xsl:template>

<xsl:template name="buildFullPathInInstanceToNamedChildOfCurrentNode">
	<xsl:param name="node"/>
	<xsl:call-template name="buildFullPathInInstanceToCurrentNode"/>
	<xsl:value-of select="name($node)"/>
</xsl:template>

<xsl:template name="buildFullPathInInstanceToCurrentNode">
	<xsl:if test="name(.) != 'DicomObject'">
		<xsl:for-each select="..">
			<xsl:call-template name="buildFullPathInInstanceToCurrentNode"/>
		</xsl:for-each>
		<xsl:value-of select="name(.)"/>
		<xsl:if test="name(.) = 'Item' or name(.) = 'value'">
			<xsl:text>[</xsl:text><xsl:value-of select="@number"/><xsl:text>]</xsl:text>
		</xsl:if>
		<xsl:text>/</xsl:text>
	</xsl:if>
</xsl:template>

<xsl:template name="CheckAttributeVR">
	<xsl:param name="description"/>
	<xsl:param name="element"/>
	<xsl:param name="vr"/>
	<xsl:if test="count($element) = 1 and (($vr != 'XS' and $vr != 'OX' and $element/@vr != $vr) or ($vr = 'XS' and $element/@vr != 'US' and $element/@vr != 'SS') or ($vr = 'OX' and $element/@vr != 'OB' and $element/@vr != 'OW'))">
		<xsl:text>Error: </xsl:text><xsl:value-of select="$description"/><xsl:text>: </xsl:text><xsl:call-template name="buildFullPathInInstanceToNamedChildOfCurrentNode"><xsl:with-param name="node" select="$element"/></xsl:call-template><xsl:text>: Incorrect VR - got </xsl:text><xsl:value-of select="$element/@vr"/><xsl:text> but expected </xsl:text><xsl:value-of select="$vr"/><xsl:value-of select="$newline"/>
	</xsl:if>
	<xsl:if test="count($element) = 1">
		<xsl:if test="count($element) = 1 and count($element/value) &gt; 0">
			<xsl:for-each select="$element/value">
				<xsl:choose>
				<xsl:when test="$element/@vr = 'AE'">
					<xsl:if test="string-length(.) &gt; 0 and string-length() &gt; 0">
						<xsl:variable name="translatedstring1"><xsl:value-of select="translate(.,' !&quot;#$%&amp;()*+,-./0123456789:;&lt;=&gt;?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[]^_`abcdefghijklmnopqrstuvwxyz{|}~','')"/></xsl:variable>
						<xsl:variable name="translatedstring2"><xsl:value-of select='translate($translatedstring1,"&apos;","")'/></xsl:variable>
						<xsl:if test="string-length($translatedstring2) &gt; 0">
							<xsl:text>Error: </xsl:text><xsl:value-of select="$description"/><xsl:text>: </xsl:text><xsl:call-template name="buildFullPathInInstanceToCurrentNode"><xsl:with-param name="node" select="."/></xsl:call-template><xsl:text>: Illegal character(s) &apos;</xsl:text><xsl:value-of select="$translatedstring2"/><xsl:text>&apos; in value of </xsl:text><xsl:value-of select="$element/@vr"/><xsl:text> VR - value is :</xsl:text><xsl:value-of select="."/><xsl:text>:</xsl:text><xsl:value-of select="$newline"/>
						</xsl:if>
					</xsl:if>
				</xsl:when>
				<xsl:when test="$element/@vr = 'AS'">
					<xsl:if test="string-length(.) &gt; 0 and string-length(translate(substring(.,1,3),'0123456789','')) &gt; 0">
						<xsl:text>Error: </xsl:text><xsl:value-of select="$description"/><xsl:text>: </xsl:text><xsl:call-template name="buildFullPathInInstanceToCurrentNode"><xsl:with-param name="node" select="."/></xsl:call-template><xsl:text>: Illegal characters (other than [0-9]) in numeric component of </xsl:text><xsl:value-of select="$element/@vr"/><xsl:text> VR - value is :</xsl:text><xsl:value-of select="."/><xsl:text>:</xsl:text><xsl:value-of select="$newline"/>
					</xsl:if>
					<xsl:if test="string-length(.) &gt;= 4 and string-length(translate(substring(.,4,1),'DWMY','')) &gt; 0">
						<xsl:text>Error: </xsl:text><xsl:value-of select="$description"/><xsl:text>: </xsl:text><xsl:call-template name="buildFullPathInInstanceToCurrentNode"><xsl:with-param name="node" select="."/></xsl:call-template><xsl:text>: Illegal characters (other than [DWMY]) in units component of </xsl:text><xsl:value-of select="$element/@vr"/><xsl:text> VR - value is :</xsl:text><xsl:value-of select="."/><xsl:text>:</xsl:text><xsl:value-of select="$newline"/>
					</xsl:if>
				</xsl:when>
				<xsl:when test="$element/@vr = 'CS'">
					<xsl:if test="string-length(.) &gt; 0 and string-length() &gt; 0">
						<xsl:variable name="translatedstring"><xsl:value-of select="translate(.,' 0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ_','')"/></xsl:variable>
						<xsl:if test="string-length($translatedstring) &gt; 0">
							<xsl:text>Error: </xsl:text><xsl:value-of select="$description"/><xsl:text>: </xsl:text><xsl:call-template name="buildFullPathInInstanceToCurrentNode"><xsl:with-param name="node" select="."/></xsl:call-template><xsl:text>: Illegal character(s) &apos;</xsl:text><xsl:value-of select="$translatedstring"/><xsl:text>&apos; in value of </xsl:text><xsl:value-of select="$element/@vr"/><xsl:text> VR - value is :</xsl:text><xsl:value-of select="."/><xsl:text>:</xsl:text><xsl:value-of select="$newline"/>
						</xsl:if>
					</xsl:if>
				</xsl:when>
				<xsl:when test="$element/@vr = 'DA'">
					<xsl:if test="string-length(.) &gt; 0 and string-length() &gt; 0">
						<xsl:variable name="translatedstring"><xsl:value-of select="translate(.,'0123456789.','')"/></xsl:variable>
						<xsl:if test="string-length($translatedstring) &gt; 0">
							<xsl:text>Error: </xsl:text><xsl:value-of select="$description"/><xsl:text>: </xsl:text><xsl:call-template name="buildFullPathInInstanceToCurrentNode"><xsl:with-param name="node" select="."/></xsl:call-template><xsl:text>: Illegal character(s) &apos;</xsl:text><xsl:value-of select="$translatedstring"/><xsl:text>&apos; in value of </xsl:text><xsl:value-of select="$element/@vr"/><xsl:text> VR - value is :</xsl:text><xsl:value-of select="."/><xsl:text>:</xsl:text><xsl:value-of select="$newline"/>
						</xsl:if>
						<xsl:if test="contains(.,'.')">
							<xsl:text>Warning: </xsl:text><xsl:value-of select="$description"/><xsl:text>: </xsl:text><xsl:call-template name="buildFullPathInInstanceToCurrentNode"><xsl:with-param name="node" select="."/></xsl:call-template><xsl:text>: Deprecated date form contains &apos;.&apos; in value of </xsl:text><xsl:value-of select="$element/@vr"/><xsl:text> VR - value is :</xsl:text><xsl:value-of select="."/><xsl:text>:</xsl:text><xsl:value-of select="$newline"/>
						</xsl:if>
					</xsl:if>
				</xsl:when>
				<xsl:when test="$element/@vr = 'DS'">
					<xsl:if test="string-length(.) &gt; 0 and string-length() &gt; 0">
						<xsl:variable name="translatedstring"><xsl:value-of select="translate(.,'0123456789.+-Ee','')"/></xsl:variable>
						<xsl:if test="string-length($translatedstring) &gt; 0">
							<xsl:text>Error: </xsl:text><xsl:value-of select="$description"/><xsl:text>: </xsl:text><xsl:call-template name="buildFullPathInInstanceToCurrentNode"><xsl:with-param name="node" select="."/></xsl:call-template><xsl:text>: Illegal character(s) &apos;</xsl:text><xsl:value-of select="$translatedstring"/><xsl:text>&apos; in value of </xsl:text><xsl:value-of select="$element/@vr"/><xsl:text> VR - value is :</xsl:text><xsl:value-of select="."/><xsl:text>:</xsl:text><xsl:value-of select="$newline"/>
						</xsl:if>
					</xsl:if>
				</xsl:when>
				<xsl:when test="$element/@vr = 'DT'">
					<xsl:if test="string-length(.) &gt; 0 and string-length() &gt; 0">
						<xsl:variable name="translatedstring"><xsl:value-of select="translate(.,'0123456789.+-','')"/></xsl:variable>
						<xsl:if test="string-length($translatedstring) &gt; 0">
							<xsl:text>Error: </xsl:text><xsl:value-of select="$description"/><xsl:text>: </xsl:text><xsl:call-template name="buildFullPathInInstanceToCurrentNode"><xsl:with-param name="node" select="."/></xsl:call-template><xsl:text>: Illegal character(s) &apos;</xsl:text><xsl:value-of select="$translatedstring"/><xsl:text>&apos; in value of </xsl:text><xsl:value-of select="$element/@vr"/><xsl:text> VR - value is :</xsl:text><xsl:value-of select="."/><xsl:text>:</xsl:text><xsl:value-of select="$newline"/>
						</xsl:if>
					</xsl:if>
				</xsl:when>
				<xsl:when test="$element/@vr = 'IS'">
					<xsl:if test="string-length(.) &gt; 0 and string-length() &gt; 0">
						<xsl:variable name="translatedstring"><xsl:value-of select="translate(.,'0123456789+-','')"/></xsl:variable>
						<xsl:if test="string-length($translatedstring) &gt; 0">
							<xsl:text>Error: </xsl:text><xsl:value-of select="$description"/><xsl:text>: </xsl:text><xsl:call-template name="buildFullPathInInstanceToCurrentNode"><xsl:with-param name="node" select="."/></xsl:call-template><xsl:text>: Illegal character(s) &apos;</xsl:text><xsl:value-of select="$translatedstring"/><xsl:text>&apos; in value of </xsl:text><xsl:value-of select="$element/@vr"/><xsl:text> VR - value is :</xsl:text><xsl:value-of select="."/><xsl:text>:</xsl:text><xsl:value-of select="$newline"/>
						</xsl:if>
					</xsl:if>
				</xsl:when>
				<xsl:when test="$element/@vr = 'LO' or $element/@vr = 'SH'">
					<xsl:if test="string-length(.) &gt; 0 and string-length() &gt; 0">
						<xsl:variable name="translatedstring1"><xsl:value-of select="translate(.,' !&quot;#$%&amp;()*+,-./0123456789:;&lt;=&gt;?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[]^_`abcdefghijklmnopqrstuvwxyz{|}~','')"/></xsl:variable>
						<xsl:variable name="translatedstring2"><xsl:value-of select='translate($translatedstring1,"&apos;","")'/></xsl:variable>
						<xsl:if test="string-length($translatedstring2) &gt; 0">
							<xsl:text>Error: </xsl:text><xsl:value-of select="$description"/><xsl:text>: </xsl:text><xsl:call-template name="buildFullPathInInstanceToCurrentNode"><xsl:with-param name="node" select="."/></xsl:call-template><xsl:text>: Illegal character(s) &apos;</xsl:text><xsl:value-of select="$translatedstring2"/><xsl:text>&apos; in value of </xsl:text><xsl:value-of select="$element/@vr"/><xsl:text> VR - value is :</xsl:text><xsl:value-of select="."/><xsl:text>:</xsl:text><xsl:value-of select="$newline"/>
						</xsl:if>
					</xsl:if>
				</xsl:when>
				<xsl:when test="$element/@vr = 'PN'">
					<xsl:if test="string-length(.) &gt; 0 and string-length() &gt; 0">
						<xsl:variable name="translatedstring1"><xsl:value-of select="translate(.,' !&quot;#$%&amp;()*+,-./0123456789:;&lt;=&gt;?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[]^_`abcdefghijklmnopqrstuvwxyz{|}~','')"/></xsl:variable>
						<xsl:variable name="translatedstring2"><xsl:value-of select='translate($translatedstring1,"&apos;","")'/></xsl:variable>
						<xsl:if test="string-length($translatedstring2) &gt; 0">
							<xsl:text>Error: </xsl:text><xsl:value-of select="$description"/><xsl:text>: </xsl:text><xsl:call-template name="buildFullPathInInstanceToCurrentNode"><xsl:with-param name="node" select="."/></xsl:call-template><xsl:text>: Illegal character(s) &apos;</xsl:text><xsl:value-of select="$translatedstring2"/><xsl:text>&apos; in value of </xsl:text><xsl:value-of select="$element/@vr"/><xsl:text> VR - value is :</xsl:text><xsl:value-of select="."/><xsl:text>:</xsl:text><xsl:value-of select="$newline"/>
						</xsl:if>
					</xsl:if>
				</xsl:when>
				<xsl:when test="$element/@vr = 'TM'">
					<xsl:if test="string-length(.) &gt; 0 and string-length() &gt; 0">
						<xsl:variable name="translatedstring"><xsl:value-of select="translate(.,'0123456789.:','')"/></xsl:variable>
						<xsl:if test="string-length($translatedstring) &gt; 0">
							<xsl:text>Error: </xsl:text><xsl:value-of select="$description"/><xsl:text>: </xsl:text><xsl:call-template name="buildFullPathInInstanceToCurrentNode"><xsl:with-param name="node" select="."/></xsl:call-template><xsl:text>: Illegal character(s) &apos;</xsl:text><xsl:value-of select="$translatedstring"/><xsl:text>&apos; in value of </xsl:text><xsl:value-of select="$element/@vr"/><xsl:text> VR - value is :</xsl:text><xsl:value-of select="."/><xsl:text>:</xsl:text><xsl:value-of select="$newline"/>
						</xsl:if>
						<xsl:if test="contains(.,':')">
							<xsl:text>Warning: </xsl:text><xsl:value-of select="$description"/><xsl:text>: </xsl:text><xsl:call-template name="buildFullPathInInstanceToCurrentNode"><xsl:with-param name="node" select="."/></xsl:call-template><xsl:text>: Deprecated time form contains &apos;:&apos; in value of </xsl:text><xsl:value-of select="$element/@vr"/><xsl:text> VR - value is :</xsl:text><xsl:value-of select="."/><xsl:text>:</xsl:text><xsl:value-of select="$newline"/>
						</xsl:if>
					</xsl:if>
				</xsl:when>
				<xsl:when test="$element/@vr = 'UI'">
					<xsl:if test="string-length(.) &gt; 0 and string-length() &gt; 0">
						<xsl:variable name="translatedstring"><xsl:value-of select="translate(.,'0123456789.','')"/></xsl:variable>
						<xsl:if test="string-length($translatedstring) &gt; 0">
							<xsl:text>Error: </xsl:text><xsl:value-of select="$description"/><xsl:text>: </xsl:text><xsl:call-template name="buildFullPathInInstanceToCurrentNode"><xsl:with-param name="node" select="."/></xsl:call-template><xsl:text>: Illegal character(s) &apos;</xsl:text><xsl:value-of select="$translatedstring"/><xsl:text>&apos; in value of </xsl:text><xsl:value-of select="$element/@vr"/><xsl:text> VR - value is :</xsl:text><xsl:value-of select="."/><xsl:text>:</xsl:text><xsl:value-of select="$newline"/>
						</xsl:if>
					</xsl:if>
				</xsl:when>
				</xsl:choose>
				
				<xsl:choose>
				<xsl:when test="$element/@vr = 'AE' or $element/@vr = 'CS' or $element/@vr = 'DS' or $element/@vr = 'SH' or $element/@vr = 'TM'">
					<xsl:if test="string-length(.) &gt; 16">
						<xsl:text>Error: </xsl:text><xsl:value-of select="$description"/><xsl:text>: </xsl:text><xsl:call-template name="buildFullPathInInstanceToCurrentNode"><xsl:with-param name="node" select="."/></xsl:call-template><xsl:text>: Length invalid for </xsl:text><xsl:value-of select="$element/@vr"/><xsl:text> VR - is </xsl:text><xsl:value-of select="string-length(.)"/><xsl:text> but must be &lt;= 16 - value is :</xsl:text><xsl:value-of select="."/><xsl:text>:</xsl:text><xsl:value-of select="$newline"/>
					</xsl:if>
				</xsl:when>
				<xsl:when test="$element/@vr = 'AS'">
					<xsl:if test="string-length(.) != 4">
						<xsl:text>Error: </xsl:text><xsl:value-of select="$description"/><xsl:text>: </xsl:text><xsl:call-template name="buildFullPathInInstanceToCurrentNode"><xsl:with-param name="node" select="."/></xsl:call-template><xsl:text>: Length invalid for </xsl:text><xsl:value-of select="$element/@vr"/><xsl:text> VR - is </xsl:text><xsl:value-of select="string-length(.)"/><xsl:text> but must be == 4 - value is :</xsl:text><xsl:value-of select="."/><xsl:text>:</xsl:text><xsl:value-of select="$newline"/>
					</xsl:if>
				</xsl:when>
				<xsl:when test="$element/@vr = 'DA'">
					<xsl:if test="string-length(.) != 8 and string-length(.) != 10">
						<xsl:text>Error: </xsl:text><xsl:value-of select="$description"/><xsl:text>: </xsl:text><xsl:call-template name="buildFullPathInInstanceToCurrentNode"><xsl:with-param name="node" select="."/></xsl:call-template><xsl:text>: Length invalid for </xsl:text><xsl:value-of select="$element/@vr"/><xsl:text> VR - is </xsl:text><xsl:value-of select="string-length(.)"/><xsl:text> but must be == 8 or 10 - value is :</xsl:text><xsl:value-of select="."/><xsl:text>:</xsl:text><xsl:value-of select="$newline"/>
					</xsl:if>
				</xsl:when>
				<xsl:when test="$element/@vr = 'DT'">
					<xsl:if test="string-length(.) &gt; 26">
						<xsl:text>Error: </xsl:text><xsl:value-of select="$description"/><xsl:text>: </xsl:text><xsl:call-template name="buildFullPathInInstanceToCurrentNode"><xsl:with-param name="node" select="."/></xsl:call-template><xsl:text>: Length invalid for </xsl:text><xsl:value-of select="$element/@vr"/><xsl:text> VR - is </xsl:text><xsl:value-of select="string-length(.)"/><xsl:text> but must be &lt;= 26 - value is :</xsl:text><xsl:value-of select="."/><xsl:text>:</xsl:text><xsl:value-of select="$newline"/>
					</xsl:if>
				</xsl:when>
				<xsl:when test="$element/@vr = 'IS'">
					<xsl:if test="string-length(.) &gt; 12">
						<xsl:text>Error: </xsl:text><xsl:value-of select="$description"/><xsl:text>: </xsl:text><xsl:call-template name="buildFullPathInInstanceToCurrentNode"><xsl:with-param name="node" select="."/></xsl:call-template><xsl:text>: Length invalid for </xsl:text><xsl:value-of select="$element/@vr"/><xsl:text> VR - is </xsl:text><xsl:value-of select="string-length(.)"/><xsl:text> but must be &lt;= 12 - value is :</xsl:text><xsl:value-of select="."/><xsl:text>:</xsl:text><xsl:value-of select="$newline"/>
					</xsl:if>
				</xsl:when>
				<xsl:when test="$element/@vr = 'LO' or $element/@vr = 'PN' or $element/@vr = 'UI'">
					<xsl:if test="string-length(.) &gt; 64">
						<xsl:text>Error: </xsl:text><xsl:value-of select="$description"/><xsl:text>: </xsl:text><xsl:call-template name="buildFullPathInInstanceToCurrentNode"><xsl:with-param name="node" select="."/></xsl:call-template><xsl:text>: Length invalid for </xsl:text><xsl:value-of select="$element/@vr"/><xsl:text> VR - is </xsl:text><xsl:value-of select="string-length(.)"/><xsl:text> but must be &lt;= 64 - value is :</xsl:text><xsl:value-of select="."/><xsl:text>:</xsl:text><xsl:value-of select="$newline"/>
					</xsl:if>
				</xsl:when>
				<xsl:when test="$element/@vr = 'LT'">
					<xsl:if test="string-length(.) &gt; 10240">
						<xsl:text>Error: </xsl:text><xsl:value-of select="$description"/><xsl:text>: </xsl:text><xsl:call-template name="buildFullPathInInstanceToCurrentNode"><xsl:with-param name="node" select="."/></xsl:call-template><xsl:text>: Length invalid for </xsl:text><xsl:value-of select="$element/@vr"/><xsl:text> VR - is </xsl:text><xsl:value-of select="string-length(.)"/><xsl:text> but must be &lt;= 10240 - value is :</xsl:text><xsl:value-of select="."/><xsl:text>:</xsl:text><xsl:value-of select="$newline"/>
					</xsl:if>
				</xsl:when>
				<xsl:when test="$element/@vr = 'ST'">
					<xsl:if test="string-length(.) &gt; 1024">
						<xsl:text>Error: </xsl:text><xsl:value-of select="$description"/><xsl:text>: </xsl:text><xsl:call-template name="buildFullPathInInstanceToCurrentNode"><xsl:with-param name="node" select="."/></xsl:call-template><xsl:text>: Length invalid for </xsl:text><xsl:value-of select="$element/@vr"/><xsl:text> VR - is </xsl:text><xsl:value-of select="string-length(.)"/><xsl:text> but must be &lt;= 1024 - value is :</xsl:text><xsl:value-of select="."/><xsl:text>:</xsl:text><xsl:value-of select="$newline"/>
					</xsl:if>
				</xsl:when>
				</xsl:choose>
				
				<xsl:choose>
				<xsl:when test="$element/@vr = 'DA'">
					<xsl:choose>
					<xsl:when test="string-length(.) = 8">
						<xsl:if test="number(substring(.,1,4)) &gt; 2100 or number(substring(.,1,4)) &lt; 1900">
							<xsl:text>Warning: </xsl:text><xsl:value-of select="$description"/><xsl:text>: </xsl:text><xsl:call-template name="buildFullPathInInstanceToCurrentNode"><xsl:with-param name="node" select="."/></xsl:call-template><xsl:text>: Year component of value of </xsl:text><xsl:value-of select="$element/@vr"/><xsl:text> VR is unlikely since &gt; 2100 or &lt; 1900 - value is :</xsl:text><xsl:value-of select="."/><xsl:text>:</xsl:text><xsl:value-of select="$newline"/>
						</xsl:if>
						<xsl:if test="number(substring(.,5,2)) &gt; 12">
							<xsl:text>Error: </xsl:text><xsl:value-of select="$description"/><xsl:text>: </xsl:text><xsl:call-template name="buildFullPathInInstanceToCurrentNode"><xsl:with-param name="node" select="."/></xsl:call-template><xsl:text>: Month component of value of </xsl:text><xsl:value-of select="$element/@vr"/><xsl:text> VR is invalid since &gt; 12 - value is :</xsl:text><xsl:value-of select="."/><xsl:text>:</xsl:text><xsl:value-of select="$newline"/>
						</xsl:if>
						<xsl:if test="number(substring(.,7,2)) &gt; 31">
							<xsl:text>Error: </xsl:text><xsl:value-of select="$description"/><xsl:text>: </xsl:text><xsl:call-template name="buildFullPathInInstanceToCurrentNode"><xsl:with-param name="node" select="."/></xsl:call-template><xsl:text>: Day component of value of </xsl:text><xsl:value-of select="$element/@vr"/><xsl:text> VR is invalid since &gt; 31 - value is :</xsl:text><xsl:value-of select="."/><xsl:text>:</xsl:text><xsl:value-of select="$newline"/>
						</xsl:if>
					</xsl:when>
					<xsl:when test="string-length(.) = 10">
						<xsl:if test="number(substring(.,1,4)) &gt; 2100 or number(substring(.,1,4)) &lt; 1900">
							<xsl:text>Warning: </xsl:text><xsl:value-of select="$description"/><xsl:text>: </xsl:text><xsl:call-template name="buildFullPathInInstanceToCurrentNode"><xsl:with-param name="node" select="."/></xsl:call-template><xsl:text>: Year component of value of </xsl:text><xsl:value-of select="$element/@vr"/><xsl:text> VR is unlikely since &gt; 2100 or &lt; 1900 - value is :</xsl:text><xsl:value-of select="."/><xsl:text>:</xsl:text><xsl:value-of select="$newline"/>
						</xsl:if>
						<xsl:if test="number(substring(.,6,2)) &gt; 12">
							<xsl:text>Error: </xsl:text><xsl:value-of select="$description"/><xsl:text>: </xsl:text><xsl:call-template name="buildFullPathInInstanceToCurrentNode"><xsl:with-param name="node" select="."/></xsl:call-template><xsl:text>: Month component of value of </xsl:text><xsl:value-of select="$element/@vr"/><xsl:text> VR is invalid since &gt; 12 - value is :</xsl:text><xsl:value-of select="."/><xsl:text>:</xsl:text><xsl:value-of select="$newline"/>
						</xsl:if>
						<xsl:if test="number(substring(.,9,2)) &gt; 31">
							<xsl:text>Error: </xsl:text><xsl:value-of select="$description"/><xsl:text>: </xsl:text><xsl:call-template name="buildFullPathInInstanceToCurrentNode"><xsl:with-param name="node" select="."/></xsl:call-template><xsl:text>: Day component of value of </xsl:text><xsl:value-of select="$element/@vr"/><xsl:text> VR is invalid since &gt; 31 - value is :</xsl:text><xsl:value-of select="."/><xsl:text>:</xsl:text><xsl:value-of select="$newline"/>
						</xsl:if>
					</xsl:when>
					</xsl:choose>
				</xsl:when>
				<xsl:when test="$element/@vr = 'DT'">
					<xsl:if test="string-length(.) != 4 and string-length(.) != 6 and string-length(.) != 8 and string-length(.) != 10 and string-length(.) != 12 and string-length(.) &lt; 14">
						<xsl:text>Error: </xsl:text><xsl:value-of select="$description"/><xsl:text>: </xsl:text><xsl:call-template name="buildFullPathInInstanceToCurrentNode"><xsl:with-param name="node" select="."/></xsl:call-template><xsl:text>: Length invalid for </xsl:text><xsl:value-of select="$element/@vr"/><xsl:text> VR - is </xsl:text><xsl:value-of select="string-length(.)"/><xsl:text> but must be 4, 6, 8, 10, 12 or &gt;=14 - value is :</xsl:text><xsl:value-of select="."/><xsl:text>:</xsl:text><xsl:value-of select="$newline"/>
					</xsl:if>
					<xsl:if test="string-length(.) &gt;= 4">
						<xsl:if test="number(substring(.,1,4)) &gt; 2100 or number(substring(.,1,4)) &lt; 1900">
							<xsl:text>Warning: </xsl:text><xsl:value-of select="$description"/><xsl:text>: </xsl:text><xsl:call-template name="buildFullPathInInstanceToCurrentNode"><xsl:with-param name="node" select="."/></xsl:call-template><xsl:text>: Year component of value of </xsl:text><xsl:value-of select="$element/@vr"/><xsl:text> VR is unlikely since &gt; 2100 or &lt; 1900 - value is :</xsl:text><xsl:value-of select="."/><xsl:text>:</xsl:text><xsl:value-of select="$newline"/>
						</xsl:if>
					</xsl:if>
					<xsl:if test="string-length(.) &gt;= 6">
						<xsl:if test="number(substring(.,5,2)) &gt; 12">
							<xsl:text>Error: </xsl:text><xsl:value-of select="$description"/><xsl:text>: </xsl:text><xsl:call-template name="buildFullPathInInstanceToCurrentNode"><xsl:with-param name="node" select="."/></xsl:call-template><xsl:text>: Month component of value of </xsl:text><xsl:value-of select="$element/@vr"/><xsl:text> VR is invalid since &gt; 12 - value is :</xsl:text><xsl:value-of select="."/><xsl:text>:</xsl:text><xsl:value-of select="$newline"/>
						</xsl:if>
					</xsl:if>
					<xsl:if test="string-length(.) &gt;= 8">
						<xsl:if test="number(substring(.,7,2)) &gt; 31">
							<xsl:text>Error: </xsl:text><xsl:value-of select="$description"/><xsl:text>: </xsl:text><xsl:call-template name="buildFullPathInInstanceToCurrentNode"><xsl:with-param name="node" select="."/></xsl:call-template><xsl:text>: Day component of value of </xsl:text><xsl:value-of select="$element/@vr"/><xsl:text> VR is invalid since &gt; 31 - value is :</xsl:text><xsl:value-of select="."/><xsl:text>:</xsl:text><xsl:value-of select="$newline"/>
						</xsl:if>
					</xsl:if>
					<xsl:if test="string-length(.) &gt;= 10">
						<xsl:if test="number(substring(.,9,2)) &gt; 23">
							<xsl:text>Error: </xsl:text><xsl:value-of select="$description"/><xsl:text>: </xsl:text><xsl:call-template name="buildFullPathInInstanceToCurrentNode"><xsl:with-param name="node" select="."/></xsl:call-template><xsl:text>: Hour component of value of </xsl:text><xsl:value-of select="$element/@vr"/><xsl:text> VR is invalid since &gt; 23 - value is :</xsl:text><xsl:value-of select="."/><xsl:text>:</xsl:text><xsl:value-of select="$newline"/>
						</xsl:if>
					</xsl:if>
					<xsl:if test="string-length(.) &gt;= 12">
						<xsl:if test="number(substring(.,11,2)) &gt; 59">
							<xsl:text>Error: </xsl:text><xsl:value-of select="$description"/><xsl:text>: </xsl:text><xsl:call-template name="buildFullPathInInstanceToCurrentNode"><xsl:with-param name="node" select="."/></xsl:call-template><xsl:text>: Minute component of value of </xsl:text><xsl:value-of select="$element/@vr"/><xsl:text> VR is invalid since &gt; 59 - value is :</xsl:text><xsl:value-of select="."/><xsl:text>:</xsl:text><xsl:value-of select="$newline"/>
						</xsl:if>
					</xsl:if>
					<xsl:if test="string-length(.) &gt;= 14">
						<xsl:if test="number(substring(.,13,2)) &gt; 59">
							<xsl:text>Error: </xsl:text><xsl:value-of select="$description"/><xsl:text>: </xsl:text><xsl:call-template name="buildFullPathInInstanceToCurrentNode"><xsl:with-param name="node" select="."/></xsl:call-template><xsl:text>: Second component of value of </xsl:text><xsl:value-of select="$element/@vr"/><xsl:text> VR is invalid since &gt; 59 - value is :</xsl:text><xsl:value-of select="."/><xsl:text>:</xsl:text><xsl:value-of select="$newline"/>
						</xsl:if>
					</xsl:if>
					<xsl:if test="string-length(.) &gt; 14 and not(contains(.,'.')) and not(contains(.,'+')) and not(contains(.,'-'))">
						<xsl:text>Error: </xsl:text><xsl:value-of select="$description"/><xsl:text>: </xsl:text><xsl:call-template name="buildFullPathInInstanceToCurrentNode"><xsl:with-param name="node" select="."/></xsl:call-template><xsl:text>: Missing fractional seconds or timezone offset in </xsl:text><xsl:value-of select="$element/@vr"/><xsl:text> VR - value is :</xsl:text><xsl:value-of select="."/><xsl:text>:</xsl:text><xsl:value-of select="$newline"/>
					</xsl:if>
					<xsl:if test="contains(.,'.')">
						<xsl:if test="string-length(substring-before(.,'.')) != 14 or contains(substring-after(.,'.'),'.')">
							<xsl:text>Error: </xsl:text><xsl:value-of select="$description"/><xsl:text>: </xsl:text><xsl:call-template name="buildFullPathInInstanceToCurrentNode"><xsl:with-param name="node" select="."/></xsl:call-template><xsl:text>: Decimal point placed incorrectly in value of </xsl:text><xsl:value-of select="$element/@vr"/><xsl:text> VR - value is :</xsl:text><xsl:value-of select="."/><xsl:text>:</xsl:text><xsl:value-of select="$newline"/>
						</xsl:if>
					</xsl:if>
					<xsl:if test="contains(.,'+')">
						<xsl:variable name="timezoneoffset"><xsl:value-of select="substring-after(.,'+')"/></xsl:variable>
						<xsl:if test="string-length($timezoneoffset) != 4">
							<xsl:text>Error: </xsl:text><xsl:value-of select="$description"/><xsl:text>: </xsl:text><xsl:call-template name="buildFullPathInInstanceToCurrentNode"><xsl:with-param name="node" select="."/></xsl:call-template><xsl:text>: Timezone offset of value of </xsl:text><xsl:value-of select="$element/@vr"/><xsl:text> VR is wrong length - should be 4 digits - value is :</xsl:text><xsl:value-of select="."/><xsl:text>:</xsl:text><xsl:value-of select="$newline"/>
						</xsl:if>
						<xsl:if test="number(substring($timezoneoffset,1,2)) &gt; 23">
							<xsl:text>Error: </xsl:text><xsl:value-of select="$description"/><xsl:text>: </xsl:text><xsl:call-template name="buildFullPathInInstanceToCurrentNode"><xsl:with-param name="node" select="."/></xsl:call-template><xsl:text>: Hour component of timezone offset of value of </xsl:text><xsl:value-of select="$element/@vr"/><xsl:text> VR is invalid since &gt; 23 - value is :</xsl:text><xsl:value-of select="."/><xsl:text>:</xsl:text><xsl:value-of select="$newline"/>
						</xsl:if>
						<xsl:if test="number(substring($timezoneoffset,3,2)) &gt; 59">
							<xsl:text>Error: </xsl:text><xsl:value-of select="$description"/><xsl:text>: </xsl:text><xsl:call-template name="buildFullPathInInstanceToCurrentNode"><xsl:with-param name="node" select="."/></xsl:call-template><xsl:text>: Minute component of timezone offset of value of </xsl:text><xsl:value-of select="$element/@vr"/><xsl:text> VR is invalid since &gt; 59 - value is :</xsl:text><xsl:value-of select="."/><xsl:text>:</xsl:text><xsl:value-of select="$newline"/>
						</xsl:if>
					</xsl:if>
					<xsl:if test="contains(.,'-')">
						<xsl:variable name="timezoneoffset"><xsl:value-of select="substring-after(.,'-')"/></xsl:variable>
						<xsl:if test="string-length($timezoneoffset) != 4">
							<xsl:text>Error: </xsl:text><xsl:value-of select="$description"/><xsl:text>: </xsl:text><xsl:call-template name="buildFullPathInInstanceToCurrentNode"><xsl:with-param name="node" select="."/></xsl:call-template><xsl:text>: Timezone offset of value of </xsl:text><xsl:value-of select="$element/@vr"/><xsl:text> VR is wrong length - should be 4 digits - value is :</xsl:text><xsl:value-of select="."/><xsl:text>:</xsl:text><xsl:value-of select="$newline"/>
						</xsl:if>
						<xsl:if test="number(substring($timezoneoffset,1,2)) &gt; 23">
							<xsl:text>Error: </xsl:text><xsl:value-of select="$description"/><xsl:text>: </xsl:text><xsl:call-template name="buildFullPathInInstanceToCurrentNode"><xsl:with-param name="node" select="."/></xsl:call-template><xsl:text>: Hour component of timezone offset of value of </xsl:text><xsl:value-of select="$element/@vr"/><xsl:text> VR is invalid since &gt; 23 - value is :</xsl:text><xsl:value-of select="."/><xsl:text>:</xsl:text><xsl:value-of select="$newline"/>
						</xsl:if>
						<xsl:if test="number(substring($timezoneoffset,3,2)) &gt; 59">
							<xsl:text>Error: </xsl:text><xsl:value-of select="$description"/><xsl:text>: </xsl:text><xsl:call-template name="buildFullPathInInstanceToCurrentNode"><xsl:with-param name="node" select="."/></xsl:call-template><xsl:text>: Minute component of timezone offset of value of </xsl:text><xsl:value-of select="$element/@vr"/><xsl:text> VR is invalid since &gt; 59 - value is :</xsl:text><xsl:value-of select="."/><xsl:text>:</xsl:text><xsl:value-of select="$newline"/>
						</xsl:if>
					</xsl:if>
				</xsl:when>
				<xsl:when test="$element/@vr = 'TM'">
					<xsl:if test="string-length(.) &gt;= 2">
						<xsl:if test="number(substring(.,1,2)) &gt; 23">
							<xsl:text>Error: </xsl:text><xsl:value-of select="$description"/><xsl:text>: </xsl:text><xsl:call-template name="buildFullPathInInstanceToCurrentNode"><xsl:with-param name="node" select="."/></xsl:call-template><xsl:text>: Hour component of value of </xsl:text><xsl:value-of select="$element/@vr"/><xsl:text> VR is invalid since &gt; 23 - value is :</xsl:text><xsl:value-of select="."/><xsl:text>:</xsl:text><xsl:value-of select="$newline"/>
						</xsl:if>
					</xsl:if>
					<xsl:choose>
					<xsl:when test="substring(.,3,1) = ':'">
						<xsl:if test="string-length(.) &gt;= 5">
							<xsl:if test="number(substring(.,4,2)) &gt; 59">
								<xsl:text>Error: </xsl:text><xsl:value-of select="$description"/><xsl:text>: </xsl:text><xsl:call-template name="buildFullPathInInstanceToCurrentNode"><xsl:with-param name="node" select="."/></xsl:call-template><xsl:text>: Minute component of value of </xsl:text><xsl:value-of select="$element/@vr"/><xsl:text> VR is invalid since &gt; 59 - value is :</xsl:text><xsl:value-of select="."/><xsl:text>:</xsl:text><xsl:value-of select="$newline"/>
							</xsl:if>
						</xsl:if>
						<xsl:if test="string-length(.) &gt;= 8">
							<xsl:if test="number(substring(.,7,2)) &gt; 59">
								<xsl:text>Error: </xsl:text><xsl:value-of select="$description"/><xsl:text>: </xsl:text><xsl:call-template name="buildFullPathInInstanceToCurrentNode"><xsl:with-param name="node" select="."/></xsl:call-template><xsl:text>: Second component of value of </xsl:text><xsl:value-of select="$element/@vr"/><xsl:text> VR is invalid since &gt; 59 - value is :</xsl:text><xsl:value-of select="."/><xsl:text>:</xsl:text><xsl:value-of select="$newline"/>
							</xsl:if>
						</xsl:if>
						<xsl:if test="string-length(.) &gt; 8 and substring(.,9,1) != '.'">
							<xsl:text>Error: </xsl:text><xsl:value-of select="$description"/><xsl:text>: </xsl:text><xsl:call-template name="buildFullPathInInstanceToCurrentNode"><xsl:with-param name="node" select="."/></xsl:call-template><xsl:text>: Missing or incorrectly encoded fractional seconds in </xsl:text><xsl:value-of select="$element/@vr"/><xsl:text> VR - value is :</xsl:text><xsl:value-of select="."/><xsl:text>:</xsl:text><xsl:value-of select="$newline"/>
						</xsl:if>
					</xsl:when>
					<xsl:otherwise>
						<xsl:if test="string-length(.) &gt;= 4">
							<xsl:if test="number(substring(.,3,2)) &gt; 59">
								<xsl:text>Error: </xsl:text><xsl:value-of select="$description"/><xsl:text>: </xsl:text><xsl:call-template name="buildFullPathInInstanceToCurrentNode"><xsl:with-param name="node" select="."/></xsl:call-template><xsl:text>: Minute component of value of </xsl:text><xsl:value-of select="$element/@vr"/><xsl:text> VR is invalid since &gt; 59 - value is :</xsl:text><xsl:value-of select="."/><xsl:text>:</xsl:text><xsl:value-of select="$newline"/>
							</xsl:if>
						</xsl:if>
						<xsl:if test="string-length(.) &gt;= 6">
							<xsl:if test="number(substring(.,5,2)) &gt; 59">
								<xsl:text>Error: </xsl:text><xsl:value-of select="$description"/><xsl:text>: </xsl:text><xsl:call-template name="buildFullPathInInstanceToCurrentNode"><xsl:with-param name="node" select="."/></xsl:call-template><xsl:text>: Second component of value of </xsl:text><xsl:value-of select="$element/@vr"/><xsl:text> VR is invalid since &gt; 59 - value is :</xsl:text><xsl:value-of select="."/><xsl:text>:</xsl:text><xsl:value-of select="$newline"/>
							</xsl:if>
						</xsl:if>
						<xsl:if test="string-length(.) &gt; 6 and substring(.,7,1) != '.'">
							<xsl:text>Error: </xsl:text><xsl:value-of select="$description"/><xsl:text>: </xsl:text><xsl:call-template name="buildFullPathInInstanceToCurrentNode"><xsl:with-param name="node" select="."/></xsl:call-template><xsl:text>: Missing or incorrectly encoded fractional seconds in </xsl:text><xsl:value-of select="$element/@vr"/><xsl:text> VR - value is :</xsl:text><xsl:value-of select="."/><xsl:text>:</xsl:text><xsl:value-of select="$newline"/>
						</xsl:if>
					</xsl:otherwise>
					</xsl:choose>
				</xsl:when>
				</xsl:choose>
			</xsl:for-each>
		</xsl:if>
	</xsl:if>
</xsl:template>

<xsl:template name="CheckConditionalAttributeWhenConditionSatisfied">
	<xsl:param name="description"/>
	<xsl:param name="element"/>
	<xsl:if test="count($element) &lt; 1">
		<xsl:text>Error: </xsl:text><xsl:value-of select="$description"/><xsl:text>: </xsl:text><xsl:call-template name="buildFullPathInInstanceToNamedChildOfCurrentNode"><xsl:with-param name="node" select="$element"/></xsl:call-template><xsl:text>: Missing conditional attribute</xsl:text><xsl:value-of select="$newline"/>
	</xsl:if>
</xsl:template>

<xsl:template name="CheckConditionalAttributeWhenConditionNotSatisfied">
	<xsl:param name="description"/>
	<xsl:param name="element"/>
	<xsl:param name="mbpo"/>
	<xsl:if test="count($element) &gt; 0">
		<xsl:if test="$mbpo != 'T' or $optionWarnOnMayBePresentOtherwiseAnyway = 'T'">
			<xsl:choose>
			<xsl:when test="$mbpo = 'T'">
			<xsl:text>Warning</xsl:text>
			</xsl:when>
			<xsl:otherwise>
			<xsl:text>Error</xsl:text>
			</xsl:otherwise>
			</xsl:choose>
			<xsl:text>: </xsl:text>
			<xsl:value-of select="$description"/><xsl:text>: </xsl:text><xsl:call-template name="buildFullPathInInstanceToNamedChildOfCurrentNode"><xsl:with-param name="node" select="$element"/></xsl:call-template><xsl:text>: Conditional attribute present when condition not satisfied</xsl:text><xsl:value-of select="$newline"/>
		</xsl:if>
	</xsl:if>
</xsl:template>

<xsl:template name="CheckType1CAttributeRegardless">
	<xsl:param name="description"/>
	<xsl:param name="element"/>
	<xsl:param name="vmmin"/>
	<xsl:param name="vmmax"/>
	<xsl:param name="vr"/>
	<xsl:if test="count($element) &gt; 1">
		<xsl:text>Error: </xsl:text><xsl:value-of select="$description"/><xsl:text>: </xsl:text><xsl:call-template name="buildFullPathInInstanceToNamedChildOfCurrentNode"><xsl:with-param name="node" select="$element"/></xsl:call-template><xsl:text>: Duplicate attribute</xsl:text><xsl:value-of select="$newline"/>
	</xsl:if>
	<xsl:if test="count($element) = 1 and $vr != 'OB' and $vr != 'OW' and $vr != 'OX' and $vr != 'OF'">
		<xsl:if test="(string-length($vmmin) &gt; 0 and count($element/value) &lt; $vmmin) or (string-length($vmmax) &gt; 0 and $vmmax != 'n' and count($element/value) &gt; $vmmax)">
			<xsl:text>Error: </xsl:text><xsl:value-of select="$description"/><xsl:text>: </xsl:text><xsl:call-template name="buildFullPathInInstanceToNamedChildOfCurrentNode"><xsl:with-param name="node" select="$element"/></xsl:call-template><xsl:text>: Incorrect value multiplicity - got </xsl:text><xsl:value-of select="count($element/value)"/><xsl:text> but expected </xsl:text><xsl:value-of select="$vmmin"/><xsl:text>-</xsl:text><xsl:value-of select="$vmmax"/><xsl:value-of select="$newline"/>
		</xsl:if>
		<xsl:if test="string-length($element/value) = 0">
			<xsl:text>Error: </xsl:text><xsl:value-of select="$description"/><xsl:text>: </xsl:text><xsl:call-template name="buildFullPathInInstanceToNamedChildOfCurrentNode"><xsl:with-param name="node" select="$element"/></xsl:call-template><xsl:text>: Type 1C attribute may not be zero length when present</xsl:text><xsl:value-of select="$newline"/>
		</xsl:if>
	</xsl:if>
</xsl:template>

<xsl:template name="CheckType1Attribute">
	<xsl:param name="description"/>
	<xsl:param name="element"/>
	<xsl:param name="vmmin"/>
	<xsl:param name="vmmax"/>
	<xsl:param name="vr"/>
	<xsl:if test="count($element) &lt; 1">
		<xsl:text>Error: </xsl:text><xsl:value-of select="$description"/><xsl:text>: </xsl:text><xsl:call-template name="buildFullPathInInstanceToNamedChildOfCurrentNode"><xsl:with-param name="node" select="$element"/></xsl:call-template><xsl:text>: Missing required attribute (Type 1)</xsl:text><xsl:value-of select="$newline"/>
	</xsl:if>
	<xsl:if test="count($element) &gt; 1">
		<xsl:text>Error: </xsl:text><xsl:value-of select="$description"/><xsl:text>: </xsl:text><xsl:call-template name="buildFullPathInInstanceToNamedChildOfCurrentNode"><xsl:with-param name="node" select="$element"/></xsl:call-template><xsl:text>: Duplicate attribute</xsl:text><xsl:value-of select="$newline"/>
	</xsl:if>
	<xsl:if test="count($element) = 1 and $vr != 'OB' and $vr != 'OW' and $vr != 'OX' and $vr != 'OF'">
		<xsl:if test="(string-length($vmmin) &gt; 0 and count($element/value) &lt; $vmmin) or (string-length($vmmax) &gt; 0 and $vmmax != 'n' and count($element/value) &gt; $vmmax)">
			<xsl:text>Error: </xsl:text><xsl:value-of select="$description"/><xsl:text>: </xsl:text><xsl:call-template name="buildFullPathInInstanceToNamedChildOfCurrentNode"><xsl:with-param name="node" select="$element"/></xsl:call-template><xsl:text>: Incorrect value multiplicity - got </xsl:text><xsl:value-of select="count($element/value)"/><xsl:text> but expected </xsl:text><xsl:value-of select="$vmmin"/><xsl:text>-</xsl:text><xsl:value-of select="$vmmax"/><xsl:value-of select="$newline"/>
		</xsl:if>
		<xsl:if test="string-length($element/value) = 0">
			<xsl:text>Error: </xsl:text><xsl:value-of select="$description"/><xsl:text>: </xsl:text><xsl:call-template name="buildFullPathInInstanceToNamedChildOfCurrentNode"><xsl:with-param name="node" select="$element"/></xsl:call-template><xsl:text>: Type 1 attribute may not be zero length</xsl:text><xsl:value-of select="$newline"/>
		</xsl:if>
	</xsl:if>
</xsl:template>

<xsl:template name="CheckType2CAttributeRegardless">
	<xsl:param name="description"/>
	<xsl:param name="element"/>
	<xsl:param name="vmmin"/>
	<xsl:param name="vmmax"/>
	<xsl:if test="count($element) &gt; 1">
		<xsl:text>Error: </xsl:text><xsl:value-of select="$description"/><xsl:text>: </xsl:text><xsl:call-template name="buildFullPathInInstanceToNamedChildOfCurrentNode"><xsl:with-param name="node" select="$element"/></xsl:call-template><xsl:text>: Duplicate attribute</xsl:text><xsl:value-of select="$newline"/>
	</xsl:if>
	<xsl:if test="count($element) = 1">
		<xsl:if test="(string-length($vmmin) &gt; 0 and count($element/value) &lt; $vmmin and not (count($element/value) = 0 and $vmmin = 1)) or (string-length($vmmax) &gt; 0 and $vmmax != 'n' and count($element/value) &gt; $vmmax)">
			<xsl:text>Error: </xsl:text><xsl:value-of select="$description"/><xsl:text>: </xsl:text><xsl:call-template name="buildFullPathInInstanceToNamedChildOfCurrentNode"><xsl:with-param name="node" select="$element"/></xsl:call-template><xsl:text>: Incorrect value multiplicity - got </xsl:text><xsl:value-of select="count($element/value)"/><xsl:text> but expected </xsl:text><xsl:value-of select="$vmmin"/><xsl:text>-</xsl:text><xsl:value-of select="$vmmax"/><xsl:value-of select="$newline"/>
		</xsl:if>
	</xsl:if>
</xsl:template>

<xsl:template name="CheckType2Attribute">
	<xsl:param name="description"/>
	<xsl:param name="element"/>
	<xsl:param name="vmmin"/>
	<xsl:param name="vmmax"/>
	<xsl:if test="count($element) &lt; 1">
		<xsl:text>Error: </xsl:text><xsl:value-of select="$description"/><xsl:text>: </xsl:text><xsl:call-template name="buildFullPathInInstanceToNamedChildOfCurrentNode"><xsl:with-param name="node" select="$element"/></xsl:call-template><xsl:text>: Missing required attribute (Type 2)</xsl:text><xsl:value-of select="$newline"/>
	</xsl:if>
	<xsl:if test="count($element) &gt; 1">
		<xsl:text>Error: </xsl:text><xsl:value-of select="$description"/><xsl:text>: </xsl:text><xsl:call-template name="buildFullPathInInstanceToNamedChildOfCurrentNode"><xsl:with-param name="node" select="$element"/></xsl:call-template><xsl:text>: Duplicate attribute</xsl:text><xsl:value-of select="$newline"/>
	</xsl:if>
	<xsl:if test="count($element) = 1">
		<xsl:if test="(string-length($vmmin) &gt; 0 and count($element/value) &lt; $vmmin and not (count($element/value) = 0 and $vmmin = 1)) or (string-length($vmmax) &gt; 0 and $vmmax != 'n' and count($element/value) &gt; $vmmax)">
			<xsl:text>Error: </xsl:text><xsl:value-of select="$description"/><xsl:text>: </xsl:text><xsl:call-template name="buildFullPathInInstanceToNamedChildOfCurrentNode"><xsl:with-param name="node" select="$element"/></xsl:call-template><xsl:text>: Incorrect value multiplicity - got </xsl:text><xsl:value-of select="count($element/value)"/><xsl:text> but expected </xsl:text><xsl:value-of select="$vmmin"/><xsl:text>-</xsl:text><xsl:value-of select="$vmmax"/><xsl:value-of select="$newline"/>
		</xsl:if>
	</xsl:if>
</xsl:template>

<xsl:template name="CheckType3Attribute">
	<xsl:param name="description"/>
	<xsl:param name="element"/>
	<xsl:param name="vmmin"/>
	<xsl:param name="vmmax"/>
	<xsl:if test="count($element) &gt; 1">
		<xsl:text>Error: </xsl:text><xsl:value-of select="$description"/><xsl:text>: </xsl:text><xsl:call-template name="buildFullPathInInstanceToNamedChildOfCurrentNode"><xsl:with-param name="node" select="$element"/></xsl:call-template><xsl:text>: Duplicate attribute</xsl:text><xsl:value-of select="$newline"/>
	</xsl:if>
	<xsl:if test="count($element) = 1">
		<xsl:if test="(string-length($vmmin) &gt; 0 and count($element/value) &lt; $vmmin and not (count($element/value) = 0 and $vmmin = 1)) or (string-length($vmmax) &gt; 0 and $vmmax != 'n' and count($element/value) &gt; $vmmax)">
			<xsl:text>Error: </xsl:text><xsl:value-of select="$description"/><xsl:text>: </xsl:text><xsl:call-template name="buildFullPathInInstanceToNamedChildOfCurrentNode"><xsl:with-param name="node" select="$element"/></xsl:call-template><xsl:text>: Incorrect value multiplicity - got </xsl:text><xsl:value-of select="count($element/value)"/><xsl:text> but expected </xsl:text><xsl:value-of select="$vmmin"/><xsl:text>-</xsl:text><xsl:value-of select="$vmmax"/><xsl:value-of select="$newline"/>
		</xsl:if>
	</xsl:if>
</xsl:template>

<xsl:template name="CheckType1CSequenceAttributeRegardless">
	<xsl:param name="description"/>
	<xsl:param name="element"/>
	<xsl:param name="vmmin"/>
	<xsl:param name="vmmax"/>
	<xsl:if test="count($element) &gt; 1">
		<xsl:text>Error: </xsl:text><xsl:value-of select="$description"/><xsl:text>: </xsl:text><xsl:call-template name="buildFullPathInInstanceToNamedChildOfCurrentNode"><xsl:with-param name="node" select="$element"/></xsl:call-template><xsl:text>: Duplicate attribute</xsl:text><xsl:value-of select="$newline"/>
	</xsl:if>
	<xsl:if test="count($element) = 1">
		<xsl:if test="(string-length($vmmin) &gt; 0 and count($element/Item) &lt; $vmmin) or (string-length($vmmax) &gt; 0 and $vmmax != 'n' and count($element/Item) &gt; $vmmax)">
			<xsl:text>Error: </xsl:text><xsl:value-of select="$description"/><xsl:text>: </xsl:text><xsl:call-template name="buildFullPathInInstanceToNamedChildOfCurrentNode"><xsl:with-param name="node" select="$element"/></xsl:call-template><xsl:text>: Incorrect number of sequence items - got </xsl:text><xsl:value-of select="count($element/Item)"/><xsl:text> but expected </xsl:text><xsl:value-of select="$vmmin"/><xsl:text>-</xsl:text><xsl:value-of select="$vmmax"/><xsl:value-of select="$newline"/>
		</xsl:if>
		<xsl:if test="count($element/Item) = 0">
			<xsl:text>Error: </xsl:text><xsl:value-of select="$description"/><xsl:text>: </xsl:text><xsl:call-template name="buildFullPathInInstanceToNamedChildOfCurrentNode"><xsl:with-param name="node" select="$element"/></xsl:call-template><xsl:text>: Type 1C sequence attribute must have at least one item when present</xsl:text><xsl:value-of select="$newline"/>
		</xsl:if>
	</xsl:if>
</xsl:template>

<xsl:template name="CheckType1SequenceAttribute">
	<xsl:param name="description"/>
	<xsl:param name="element"/>
	<xsl:param name="vmmin"/>
	<xsl:param name="vmmax"/>
	<xsl:if test="count($element) &lt; 1">
		<xsl:text>Error: </xsl:text><xsl:value-of select="$description"/><xsl:text>: </xsl:text><xsl:call-template name="buildFullPathInInstanceToNamedChildOfCurrentNode"><xsl:with-param name="node" select="$element"/></xsl:call-template><xsl:text>: Missing required sequence attribute (Type 1)</xsl:text><xsl:value-of select="$newline"/>
	</xsl:if>
	<xsl:if test="count($element) &gt; 1">
		<xsl:text>Error: </xsl:text><xsl:value-of select="$description"/><xsl:text>: </xsl:text><xsl:call-template name="buildFullPathInInstanceToNamedChildOfCurrentNode"><xsl:with-param name="node" select="$element"/></xsl:call-template><xsl:text>: Duplicate attribute</xsl:text><xsl:value-of select="$newline"/>
	</xsl:if>
	<xsl:if test="count($element) = 1">
		<xsl:if test="(string-length($vmmin) &gt; 0 and count($element/Item) &lt; $vmmin) or (string-length($vmmax) &gt; 0 and $vmmax != 'n' and count($element/Item) &gt; $vmmax)">
			<xsl:text>Error: </xsl:text><xsl:value-of select="$description"/><xsl:text>: </xsl:text><xsl:call-template name="buildFullPathInInstanceToNamedChildOfCurrentNode"><xsl:with-param name="node" select="$element"/></xsl:call-template><xsl:text>: Incorrect number of sequence items - got </xsl:text><xsl:value-of select="count($element/Item)"/><xsl:text> but expected </xsl:text><xsl:value-of select="$vmmin"/><xsl:text>-</xsl:text><xsl:value-of select="$vmmax"/><xsl:value-of select="$newline"/>
		</xsl:if>
		<xsl:if test="count($element/Item) = 0">
			<xsl:text>Error: </xsl:text><xsl:value-of select="$description"/><xsl:text>: </xsl:text><xsl:call-template name="buildFullPathInInstanceToNamedChildOfCurrentNode"><xsl:with-param name="node" select="$element"/></xsl:call-template><xsl:text>: Type 1 sequence attribute must have at least one item</xsl:text><xsl:value-of select="$newline"/>
		</xsl:if>
	</xsl:if>
</xsl:template>

<xsl:template name="CheckType2CSequenceAttributeRegardless">
	<xsl:param name="description"/>
	<xsl:param name="element"/>
	<xsl:param name="vmmin"/>
	<xsl:param name="vmmax"/>
	<xsl:if test="count($element) &gt; 1">
		<xsl:text>Error: </xsl:text><xsl:value-of select="$description"/><xsl:text>: </xsl:text><xsl:call-template name="buildFullPathInInstanceToNamedChildOfCurrentNode"><xsl:with-param name="node" select="$element"/></xsl:call-template><xsl:text>: Duplicate attribute</xsl:text><xsl:value-of select="$newline"/>
	</xsl:if>
	<xsl:if test="count($element) = 1">
		<xsl:if test="(string-length($vmmin) &gt; 0 and count($element/Item) &lt; $vmmin) or (string-length($vmmax) &gt; 0 and $vmmax != 'n' and count($element/Item) &gt; $vmmax)">
			<xsl:text>Error: </xsl:text><xsl:value-of select="$description"/><xsl:text>: </xsl:text><xsl:call-template name="buildFullPathInInstanceToNamedChildOfCurrentNode"><xsl:with-param name="node" select="$element"/></xsl:call-template><xsl:text>: Incorrect number of sequence items - got </xsl:text><xsl:value-of select="count($element/Item)"/><xsl:text> but expected </xsl:text><xsl:value-of select="$vmmin"/><xsl:text>-</xsl:text><xsl:value-of select="$vmmax"/><xsl:value-of select="$newline"/>
		</xsl:if>
	</xsl:if>
</xsl:template>

<xsl:template name="CheckType2SequenceAttribute">
	<xsl:param name="description"/>
	<xsl:param name="element"/>
	<xsl:param name="vmmin"/>
	<xsl:param name="vmmax"/>
	<xsl:if test="count($element) &lt; 1">
		<xsl:text>Error: </xsl:text><xsl:value-of select="$description"/><xsl:text>: </xsl:text><xsl:call-template name="buildFullPathInInstanceToNamedChildOfCurrentNode"><xsl:with-param name="node" select="$element"/></xsl:call-template><xsl:text>: Missing required sequence attribute (Type 2)</xsl:text><xsl:value-of select="$newline"/>
	</xsl:if>
	<xsl:if test="count($element) &gt; 1">
		<xsl:text>Error: </xsl:text><xsl:value-of select="$description"/><xsl:text>: </xsl:text><xsl:call-template name="buildFullPathInInstanceToNamedChildOfCurrentNode"><xsl:with-param name="node" select="$element"/></xsl:call-template><xsl:text>: Duplicate attribute</xsl:text><xsl:value-of select="$newline"/>
	</xsl:if>
	<xsl:if test="count($element) = 1">
		<xsl:if test="(string-length($vmmin) &gt; 0 and count($element/Item) &lt; $vmmin) or (string-length($vmmax) &gt; 0 and $vmmax != 'n' and count($element/Item) &gt; $vmmax)">
			<xsl:text>Error: </xsl:text><xsl:value-of select="$description"/><xsl:text>: </xsl:text><xsl:call-template name="buildFullPathInInstanceToNamedChildOfCurrentNode"><xsl:with-param name="node" select="$element"/></xsl:call-template><xsl:text>: Incorrect number of sequence items - got </xsl:text><xsl:value-of select="count($element/Item)"/><xsl:text> but expected </xsl:text><xsl:value-of select="$vmmin"/><xsl:text>-</xsl:text><xsl:value-of select="$vmmax"/><xsl:value-of select="$newline"/>
		</xsl:if>
	</xsl:if>
</xsl:template>

<xsl:template name="CheckType3SequenceAttribute">
	<xsl:param name="description"/>
	<xsl:param name="element"/>
	<xsl:param name="vmmin"/>
	<xsl:param name="vmmax"/>
	<xsl:if test="count($element) &gt; 1"><xsl:text>Error: </xsl:text><xsl:value-of select="$description"/><xsl:text>: </xsl:text><xsl:call-template name="buildFullPathInInstanceToNamedChildOfCurrentNode"><xsl:with-param name="node" select="$element"/></xsl:call-template><xsl:text>: Duplicate attribute</xsl:text><xsl:value-of select="$newline"/></xsl:if>
	<xsl:if test="count($element) = 1">
		<xsl:if test="(string-length($vmmin) &gt; 0 and count($element/Item) &lt; $vmmin) or (string-length($vmmax) &gt; 0 and $vmmax != 'n' and count($element/Item) &gt; $vmmax)">
			<xsl:text>Error: </xsl:text><xsl:value-of select="$description"/><xsl:text>: </xsl:text><xsl:call-template name="buildFullPathInInstanceToNamedChildOfCurrentNode"><xsl:with-param name="node" select="$element"/></xsl:call-template><xsl:text>: Incorrect number of sequence items - got </xsl:text><xsl:value-of select="count($element/Item)"/><xsl:text> but expected </xsl:text><xsl:value-of select="$vmmin"/><xsl:text>-</xsl:text><xsl:value-of select="$vmmax"/><xsl:value-of select="$newline"/>
		</xsl:if>
	</xsl:if>
</xsl:template>

<xsl:template name="CheckFirstValueIfPresentIsGreaterThan">
	<xsl:param name="description"/>
	<xsl:param name="element"/>
	<xsl:param name="value"/>
	<xsl:if test="count($element/value) &gt; 0 and $element/value[@number=1] &lt; $value">
		<xsl:text>Error: </xsl:text><xsl:value-of select="$description"/><xsl:text>: </xsl:text><xsl:call-template name="buildFullPathInInstanceToNamedChildOfCurrentNode"><xsl:with-param name="node" select="$element"/></xsl:call-template><xsl:text>: Value must be greater than </xsl:text><xsl:value-of select="$value"/><xsl:text> - got </xsl:text><xsl:value-of select="$element/value[@number=1]"/><xsl:value-of select="$newline"/>
	</xsl:if>
</xsl:template>

</xsl:stylesheet>
