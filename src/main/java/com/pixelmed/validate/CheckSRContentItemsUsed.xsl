<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<xsl:output method="text"/>

<xsl:variable name="newline">
<xsl:text>
</xsl:text>
</xsl:variable>

<xsl:param name="optionCheckAmbiguousTemplate" select="'F'"/>

<xsl:param name="optionCheckContentItemOrder" select="'F'"/>

<xsl:variable name="lowercase" select="'abcdefghijklmnopqrstuvwxyz'" />
<xsl:variable name="uppercase" select="'ABCDEFGHIJKLMNOPQRSTUVWXYZ'" />

<xsl:template name="describeCode">
	<xsl:param name="cv"/>
	<xsl:param name="csd"/>
	<xsl:param name="cm"/>
	
	<xsl:text>(</xsl:text>
	<xsl:value-of select="$cv"/>
	<xsl:text>,</xsl:text>
	<xsl:value-of select="$csd"/>
	<xsl:text>,"</xsl:text>
	<xsl:value-of select="$cm"/>
	<xsl:text>")</xsl:text>
</xsl:template>

<xsl:template name="buildFullPathInInstanceToCurrentNode">
	<xsl:if test="name(.) != 'DicomStructuredReportContent'">
		<xsl:for-each select="..">
			<xsl:call-template name="buildFullPathInInstanceToCurrentNode"/>
			<xsl:text>/</xsl:text>
		</xsl:for-each>
		<xsl:value-of select="translate(name(.),$lowercase,$uppercase)"/>
		<xsl:choose>
		<xsl:when test="string-length(concept/@cv) != 0">
			<xsl:text> </xsl:text>
			<xsl:call-template name="describeCode">
				<xsl:with-param name="cv" select="concept/@cv"/>
				<xsl:with-param name="csd" select="concept/@csd"/>
				<xsl:with-param name="cm" select="concept/@cm"/>
			</xsl:call-template>
		</xsl:when>
		</xsl:choose>
	</xsl:if>
</xsl:template>

<xsl:variable name="foundItemsDocument" select="document('FoundItems.xml')/founditems"/>

<xsl:template match="node()[@ID]">
	<xsl:variable name="currentnode" select="."/>
	<xsl:variable name="location" select="substring-after(@ID,'ci_')"/>
	<!--<xsl:message><xsl:text>Current location = </xsl:text><xsl:value-of select="$location"/></xsl:message>-->
	<xsl:variable name="locationdescription"><xsl:value-of select="$location"/><xsl:text>: </xsl:text><xsl:call-template name="buildFullPathInInstanceToCurrentNode"/></xsl:variable>
	<xsl:variable name="predecessor" select="preceding-sibling::node()[@ID][1]"/>	<!-- is reverse axis, so want the first -->
	<xsl:variable name="predecessorlocation" select="substring-after($predecessor/@ID,'ci_')"/>
	<!--<xsl:message><xsl:text>Predecessor location = </xsl:text><xsl:value-of select="$predecessorlocation"/></xsl:message>-->
	
	<xsl:variable name="items" select="$foundItemsDocument/item[@location = $location]"/>	<!-- using <xsl:key/> did not return node-set (despite remembering to set external document context with <xsl:for-each/>), so give up and do it the hard way :( -->
	<!--<xsl:message>
		<xsl:text>founditems: lookup returns = </xsl:text><xsl:value-of select="$newline"/>
		<xsl:for-each select="$items">
			<xsl:text>(location = </xsl:text>
			<xsl:value-of select="@location"/>
			<xsl:text>, description = </xsl:text>
			<xsl:value-of select="@description"/>
			<xsl:text>, row = </xsl:text>
			<xsl:value-of select="@row"/>
			<xsl:text>)</xsl:text>
			<xsl:value-of select="$newline"/>
		</xsl:for-each>
	</xsl:message>-->
	<!--<xsl:message><xsl:text>2nd pass: lookup returns = </xsl:text><xsl:value-of select="count($items)"/></xsl:message>--> <!-- test against zero; "not(boolean($items))" also works -->
	<xsl:if test="count($items) = 0">
		<xsl:text>Warning: </xsl:text><xsl:value-of select="$locationdescription"/><xsl:text>: Content Item not in template</xsl:text><xsl:value-of select="$newline"/>
	</xsl:if>
	<xsl:if test="$optionCheckAmbiguousTemplate = 'T' and count($items) &gt; 1">
		<xsl:for-each select="$items">
			<xsl:text>Warning: </xsl:text><xsl:value-of select="$locationdescription"/><xsl:text>: Ambiguous template matches (more than one row matches in same or different template): </xsl:text><xsl:value-of select="@description"/><xsl:value-of select="$newline"/>
		</xsl:for-each>
	</xsl:if>
	<xsl:if test="$optionCheckContentItemOrder = 'T' and count($items) &gt;= 1">
		<!-- check order of rows in same template -->
		<xsl:if test="boolean($predecessor)">
			<!--<xsl:message><xsl:text>Predecessor location from preceding-sibling:: is </xsl:text><xsl:value-of select="$predecessorlocation"/></xsl:message>-->
			<xsl:variable name="predecessoritems" select="$foundItemsDocument/item[@location = $predecessorlocation]"/>
			<xsl:if test="count($predecessoritems) &gt;= 1">
				<!-- check even if ambiguous matches for either current (test) or predecessor -->
				<xsl:for-each select="$items">
					<xsl:variable name="testlocation" select="@location"/> <!-- call these variables test rather than current to avoid confusion with actual current node that entire template is working on -->
					<xsl:variable name="testdescription" select="@description"/>
					<xsl:variable name="testtemplate" select="substring-before(@description,'/')"/>
					<xsl:variable name="testtemplaterow" select="@row"/>
					<xsl:for-each select="$predecessoritems">
						<xsl:variable name="predecessorlocationfromitem" select="@location"/>
						<xsl:variable name="predecessortemplate" select="substring-before(@description,'/')"/>
						<xsl:variable name="predecessortemplaterow" select="@row"/>
						<!--<xsl:message><xsl:text>Comparing order of current location </xsl:text><xsl:value-of select="$testlocation"/><xsl:text> template </xsl:text><xsl:value-of select="$testtemplate"/><xsl:text> row </xsl:text><xsl:value-of select="$testtemplaterow"/><xsl:text> and predecessor location </xsl:text><xsl:value-of select="$predecessorlocationfromitem"/><xsl:text> template </xsl:text><xsl:value-of select="$predecessortemplate"/><xsl:text> row </xsl:text><xsl:value-of select="$predecessortemplaterow"/></xsl:message>-->
						<xsl:if test="$testtemplate = $predecessortemplate">
							<!--<xsl:message><xsl:text>Have same template</xsl:text></xsl:message>-->
							<xsl:if test="$predecessortemplaterow &gt; $testtemplaterow">
								<!--<xsl:message><xsl:text>Warning: </xsl:text><xsl:value-of select="$locationdescription"/><xsl:text>: Content Item not in template row order for </xsl:text><xsl:value-of select="$testdescription"/></xsl:message>-->
								<xsl:text>Warning: </xsl:text><xsl:value-of select="$locationdescription"/><xsl:text>: Content Item not in template row order for </xsl:text><xsl:value-of select="$testdescription"/><xsl:value-of select="$newline"/>
							</xsl:if>
						</xsl:if>
					</xsl:for-each>
				</xsl:for-each>
			</xsl:if>
		</xsl:if>
	</xsl:if>
	<xsl:apply-templates/>
</xsl:template>

<xsl:template match="text()"/>

</xsl:stylesheet>
