<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:xslout="bla"
	version="1.0">

<xsl:output method="xml" indent="yes"/>

<xsl:namespace-alias stylesheet-prefix="xslout" result-prefix="xsl"/>

<xsl:variable name="lowercase" select="'abcdefghijklmnopqrstuvwxyz'" />
<xsl:variable name="uppercase" select="'ABCDEFGHIJKLMNOPQRSTUVWXYZ'" />

<xsl:template match="definitions">
	<xslout:stylesheet version="1.0">
	
	<xslout:import href="CommonDicomSRValidationRules.xsl"/>
	
	<xslout:output method="text"/>

	<xslout:template match="/DicomStructuredReport">
	<xslout:choose>
	<xsl:apply-templates select="defineiod"/>
	<xslout:otherwise>
	<xslout:text>IOD (SOP Class) unrecognized</xslout:text><xslout:value-of select="$newline"/>
	</xslout:otherwise>
	</xslout:choose>
	</xslout:template>
	
	<xslout:template match="/DicomStructuredReport/DicomStructuredReportHeader">
		<xslout:apply-templates/>
	</xslout:template>
	
	<xslout:template match="text()"/>

	<xsl:apply-templates select="definecontentitemconstraints"/>

	<xsl:apply-templates select="definetemplate"/>

	<xsl:apply-templates select="usecontextgroups"/>

	</xslout:stylesheet>
</xsl:template>

<xsl:template match="defineiod">
	<xslout:when test="/DicomStructuredReport/DicomStructuredReportHeader/SOPClassUID/value = '{@sopclass}'">
		<xslout:text>Found <xsl:value-of select="@name"/> IOD</xslout:text><xslout:value-of select="$newline"/>
		<xsl:apply-templates select="invokecontentitemconstraints"/>
		<xsl:apply-templates select="invokeroottemplate"/>
		<!-- <xslout:text>IOD validation complete</xslout:text><xslout:value-of select="$newline"/> --> <!-- This is noew written in the invoking source code, after the 2nd pass -->
		<xslout:apply-templates/>
	</xslout:when>
</xsl:template>

<xsl:template match="invokecontentitemconstraints">
	<xslout:if test="$optionDescribeChecking='T'"><xslout:text>Checking IOD Content Item Constraints: <xsl:value-of select="@name"/></xslout:text><xslout:value-of select="$newline"/></xslout:if>
	<xslout:call-template name="{@name}"/>
</xsl:template>

<xsl:template match="definecontentitemconstraints">
	<xslout:template name="{@name}">
	<xsl:apply-templates select="parentcontentitem"/>
	</xslout:template>
</xsl:template>

<xsl:template match="parentcontentitem">
	<xslout:for-each select="//{@name}">
		<xslout:for-each select="*[@relationship]">
			<xslout:choose>
			<xsl:apply-templates select="permittedchildcontentitem"/>
			<xslout:otherwise>
				<xslout:call-template name="describeIllegalChildContentItem"><xslout:with-param name="parent" select=".."/><xslout:with-param name="child" select="."/></xslout:call-template>
			</xslout:otherwise>
			</xslout:choose>
		</xslout:for-each>
	</xslout:for-each>
</xsl:template>

<xsl:template match="permittedchildcontentitem">
	<xslout:when test="@relationship = '{@relationship}' and name(.) = '{@name}'">
		<xslout:call-template name="checkPermittedChildContentItemByValueRelationship"><xslout:with-param name="parent" select=".."/><xslout:with-param name="child" select="."/></xslout:call-template>
	</xslout:when>
	<xsl:if test="@byreference= 'T'">
	<xslout:when test="@relationship = '{@relationship}' and name(.) = 'reference' and name(key('idkey',@IDREF)) = '{@name}'">
		<xslout:call-template name="checkPermittedChildContentItemByReferenceRelationship"><xslout:with-param name="parent" select=".."/><xslout:with-param name="child" select="."/></xslout:call-template>
	</xslout:when>
	</xsl:if>
</xsl:template>

<xsl:template match="invokeroottemplate">
	<xsl:variable name="tidLabel">TID_<xsl:value-of select="@tid"/></xsl:variable>
	<xsl:variable name="tidToMatch">
		<xsl:choose>
			<xsl:when test="@template !=''">		<!-- template value encoded in object may be different from the label we use in our definitions-->
				<xsl:value-of select="@template"/>
			</xsl:when>
			<xsl:otherwise>
				<xsl:value-of select="@tid"/>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:variable>
	<xsl:choose>
	<xsl:when test="@tidRequired='T'">
		<xslout:if test="$optionDescribeChecking='T'"><xslout:text>Required Root Template: <xsl:value-of select="$tidLabel"/></xslout:text><xslout:value-of select="$newline"/></xslout:if>
		<xslout:for-each select="/DicomStructuredReport/DicomStructuredReportContent">
			<xslout:call-template name="{$tidLabel}"/>
		</xslout:for-each>
	</xsl:when>
	<xsl:when test="@cvDocumentTitle and @csdDocumentTitle and @cvProcedureReported and @csdProcedureReported">
		<xslout:if test="$optionDescribeChecking='T'"><xslout:text>Checking for presence of Root Template based on absence of Template Identification Sequence and instead using Document Title and Procedure Reported: Invoke <xsl:value-of select="$tidLabel"/></xslout:text><xslout:value-of select="$newline"/></xslout:if>
		<xslout:if test="count(/DicomStructuredReport/DicomStructuredReportContent/container/@template) = 0
		             and /DicomStructuredReport/DicomStructuredReportContent/container/concept/@cv = '{@cvDocumentTitle}'
					 and /DicomStructuredReport/DicomStructuredReportContent/container/concept/@csd = '{@csdDocumentTitle}'
					 and /DicomStructuredReport/DicomStructuredReportContent/container/code/concept/@cv = '121058'
					 and /DicomStructuredReport/DicomStructuredReportContent/container/code/concept/@csd = 'DCM'
					 and /DicomStructuredReport/DicomStructuredReportContent/container/code/value/@cv = '{@cvProcedureReported}'
					 and /DicomStructuredReport/DicomStructuredReportContent/container/code/value/@csd = '{@csdProcedureReported}'
					 ">
			<xslout:if test="$optionDescribeChecking='T'"><xslout:text>Condition satisfied for presence of Root Template based on absence of Template Identification Sequence and instead using Document Title and Procedure Reported: Invoke <xsl:value-of select="$tidLabel"/></xslout:text><xslout:value-of select="$newline"/></xslout:if>
			<xslout:for-each select="/DicomStructuredReport/DicomStructuredReportContent">
				<xslout:call-template name="{$tidLabel}"/>
			</xslout:for-each>
		</xslout:if>
	</xsl:when>
	<xsl:when test="@cvDocumentTitle and @csdDocumentTitle">
		<xslout:if test="$optionDescribeChecking='T'"><xslout:text>Checking for presence of Root Template based on absence of Template Identification Sequence and instead using Document Title (only): Invoke <xsl:value-of select="$tidLabel"/></xslout:text><xslout:value-of select="$newline"/></xslout:if>
		<xslout:if test="count(/DicomStructuredReport/DicomStructuredReportContent/container/@template) = 0
		             and /DicomStructuredReport/DicomStructuredReportContent/container/concept/@cv = '{@cvDocumentTitle}'
					 and /DicomStructuredReport/DicomStructuredReportContent/container/concept/@csd = '{@csdDocumentTitle}'
					 ">
			<xslout:if test="$optionDescribeChecking='T'"><xslout:text>Condition satisfied for presence of Root Template based on absence of Template Identification Sequence and instead using Document Title (only): Invoke <xsl:value-of select="$tidLabel"/></xslout:text><xslout:value-of select="$newline"/></xslout:if>
			<xslout:for-each select="/DicomStructuredReport/DicomStructuredReportContent">
				<xslout:call-template name="{$tidLabel}"/>
			</xslout:for-each>
		</xslout:if>
	</xsl:when>
	<xsl:when test="@tid and @templatemappingresource">
		<xslout:if test="$optionDescribeChecking='T'"><xslout:text>Checking for presence of Root Template based on presence of Template Identification Sequence (only): match <xsl:value-of select="$tidToMatch"/>: Invoke <xsl:value-of select="$tidLabel"/></xslout:text><xslout:value-of select="$newline"/></xslout:if>
		<xslout:if test="/DicomStructuredReport/DicomStructuredReportContent/container/@template = '{$tidToMatch}'
					 and /DicomStructuredReport/DicomStructuredReportContent/container/@templatemappingresource = '{@templatemappingresource}'
					 ">
			<xslout:if test="$optionDescribeChecking='T'"><xslout:text>Condition satisfied for presence of Root Template based on presence of Template Identification Sequence (only): match <xsl:value-of select="$tidToMatch"/>: Invoke <xsl:value-of select="$tidLabel"/></xslout:text><xslout:value-of select="$newline"/></xslout:if>
			<xslout:for-each select="/DicomStructuredReport/DicomStructuredReportContent">
				<xslout:call-template name="{$tidLabel}"/>
			</xslout:for-each>
		</xslout:if>
	</xsl:when>
	</xsl:choose>
</xsl:template>

<xsl:template match="includetemplate">
	<xsl:variable name="tidLabel">TID_<xsl:value-of select="@tid"/></xsl:variable>
	<xslout:if test="$optionDescribeChecking='T'"><xslout:text>Checking whether or not to include nested Template: <xsl:value-of select="$tidLabel"/></xslout:text><xslout:value-of select="$newline"/></xslout:if>
	<xslout:if test="$templateConditionSatisfied!='F'"> <!-- This is the condition on the PARENT template, if any -->
	<xslout:if test="$optionDescribeChecking='T'"><xslout:text>Including Template: <xsl:value-of select="$tidLabel"/></xslout:text><xslout:value-of select="$newline"/></xslout:if>
		<xsl:choose>
		<xsl:when test="@requiredType ='MC' or @requiredType ='UC'">
			<xslout:choose>
			<xslout:when test="{@condition}">
				<xslout:call-template name="{$tidLabel}">
					<xslout:with-param name="templatevmmin" select="'{@vmmin}'"/>
					<xslout:with-param name="templatevmmax" select="'{@vmmax}'"/>
					<xslout:with-param name="templateRequiredType" select="'{@requiredType}'"/>
					<xslout:with-param name="templateConditionSatisfied" select="'T'"/>
					<xslout:with-param name="templateMBPO" select="'{@mbpo}'"/>
				</xslout:call-template>
			</xslout:when>
			<xslout:otherwise>
				<xsl:if test="@matchCondition !='T'">
				<!-- Need to still call template since it may be present even if condition not satisfied, and check MBPO inside (unless matchCondition specified to force disambiguation with a conflicting sibling) -->
					<xslout:call-template name="{$tidLabel}">
						<xslout:with-param name="templatevmmin" select="'{@vmmin}'"/>
						<xslout:with-param name="templatevmmax" select="'{@vmmax}'"/>
						<xslout:with-param name="templateRequiredType" select="'{@requiredType}'"/>
						<xslout:with-param name="templateConditionSatisfied" select="'F'"/>
						<xslout:with-param name="templateMBPO" select="'{@mbpo}'"/>
					</xslout:call-template>
				</xsl:if>
			</xslout:otherwise>
			</xslout:choose>
		</xsl:when>
		<xsl:otherwise>
			<xslout:call-template name="{$tidLabel}">
					<xslout:with-param name="templatevmmin" select="'{@vmmin}'"/>
					<xslout:with-param name="templatevmmax" select="'{@vmmax}'"/>
				<xslout:with-param name="templateRequiredType" select="'{@requiredType}'"/>
			</xslout:call-template>
		</xsl:otherwise>
		</xsl:choose>
	</xslout:if>
</xsl:template>

<xsl:template match="definetemplate">
	<xsl:variable name="tidLabel">TID_<xsl:value-of select="@tid"/></xsl:variable>
	<xslout:template name="{$tidLabel}">
	<xslout:param name="templatevmmin"/>
	<xslout:param name="templatevmmax"/>
	<xslout:param name="templateRequiredType"/>
	<xslout:param name="templateConditionSatisfied"/>
	<xslout:param name="templateMBPO"/>
	<xslout:variable name="templateID"><xsl:value-of select="@tid"/></xslout:variable>	<!-- used by XSL generated by templatecontentitem -->
	<xslout:if test="$optionDescribeChecking='T'">
		<xslout:text>Checking Template: <xsl:value-of select="$tidLabel"/> (<xsl:value-of select="@name"/>)</xslout:text><xslout:value-of select="$newline"/>
		<xslout:text>Checking Template: templatevmmin = </xslout:text><xslout:value-of select="$templatevmmin"/><xslout:value-of select="$newline"/>
		<xslout:text>Checking Template: templatevmmax = </xslout:text><xslout:value-of select="$templatevmmax"/><xslout:value-of select="$newline"/>
		<xslout:text>Checking Template: templateRequiredType = </xslout:text><xslout:value-of select="$templateRequiredType"/><xslout:value-of select="$newline"/>
		<xslout:text>Checking Template: templateConditionSatisfied = </xslout:text><xslout:value-of select="$templateConditionSatisfied"/><xslout:value-of select="$newline"/>
		<xslout:text>Checking Template: templateMBPO = </xslout:text><xslout:value-of select="$templateMBPO"/><xslout:value-of select="$newline"/>
	</xslout:if>
	<xsl:if test="@root='T'">
		<xslout:text>Found Root Template <xsl:value-of select="$tidLabel"/> (<xsl:value-of select="@name"/>)</xslout:text><xslout:value-of select="$newline"/>
	</xsl:if>
		<xsl:apply-templates/>
	<xsl:if test="@root='T'">
		<xslout:text>Root Template Validation Complete</xslout:text><xslout:value-of select="$newline"/>
	</xsl:if>
	</xslout:template>
</xsl:template>

<xsl:template match="templatecontentitem">

	<xsl:variable name="escapedQuote">#</xsl:variable>
	<xsl:variable name="actualQuote">'</xsl:variable>
	<xsl:variable name="cmConceptNameQuoteSubstituted"><xsl:value-of select="translate(@cmConceptName,$actualQuote,$escapedQuote)"/></xsl:variable>       <!-- escape quotes -->
	<xsl:variable name="cmValueQuoteSubstituted"><xsl:value-of select="translate(@cmValue,$actualQuote,$escapedQuote)"/></xsl:variable>					  <!-- escape quotes -->
	<xsl:variable name="cmUnitsQuoteSubstituted"><xsl:value-of select="translate(@cmUnits,$actualQuote,$escapedQuote)"/></xsl:variable>					  <!-- escape quotes -->

	<xsl:variable name="nestingLevel" select="count(ancestor-or-self::templatecontentitem)"/>

	<xsl:variable name="description">
		<xsl:call-template name="buildFullPathInDefinitionToCurrentContentItem">
			<xsl:with-param name="cmConceptNameQuoteSubstituted" select="$cmConceptNameQuoteSubstituted"/>
		</xsl:call-template>
	</xsl:variable>
	<xslout:if test="$optionDescribeChecking='T'"><xslout:text>Checking for content item: <xsl:value-of select="$description"/></xslout:text><xslout:value-of select="$newline"/></xslout:if>
	
	<xsl:variable name="valueType"><xsl:value-of select="translate(@valueType,$uppercase,$lowercase)"/></xsl:variable>       <!-- need lower case ValueType -->
	<xsl:variable name="relationship"><xsl:choose><xsl:when test="starts-with(translate(@relationship,$lowercase,$uppercase),'R-')"><xsl:value-of select="substring-after(translate(@relationship,$lowercase,$uppercase),'R-')"/></xsl:when><xsl:otherwise><xsl:value-of select="translate(@relationship,$lowercase,$uppercase)"/></xsl:otherwise></xsl:choose></xsl:variable> <!-- need upper case relationship without by reference prefix-->
	<xsl:variable name="byReference"><xsl:choose><xsl:when test="starts-with(translate(@relationship,$lowercase,$uppercase),'R-')">T</xsl:when><xsl:otherwise>F</xsl:otherwise></xsl:choose></xsl:variable>
	
	<xsl:choose>
	<xsl:when test="@requiredType ='MC' or @requiredType ='UC'">
		<xslout:choose>
		<xslout:when test="{@condition}">
			<xslout:call-template name="CheckContentItem">
				<xslout:with-param name="description" select="'{$description}'"/>
				<xslout:with-param name="row" select="'{@row}'"/>
				<xslout:with-param name="relationship" select="'{$relationship}'"/>
				<xslout:with-param name="byReference" select="'{$byReference}'"/>
				<xslout:with-param name="valueType" select="'{$valueType}'"/>
				<xslout:with-param name="conceptNameCID" select="'{@conceptNameCID}'"/>
				<xslout:with-param name="cmConceptName" select="'{$cmConceptNameQuoteSubstituted}'"/>
				<xslout:with-param name="csdConceptName" select="'{@csdConceptName}'"/>
				<xslout:with-param name="cvConceptName" select="'{@cvConceptName}'"/>
				<xslout:with-param name="vmmin" select="'{@vmmin}'"/>
				<xslout:with-param name="vmmax" select="'{@vmmax}'"/>
				<xslout:with-param name="requiredType" select="'{@requiredType}'"/>
				<xslout:with-param name="conditionSatisfied" select="'T'"/>
				<xslout:with-param name="mbpo" select="'{@mbpo}'"/>
				<xslout:with-param name="matchRelationship" select="'{@matchRelationship}'"/>
				<xslout:with-param name="valueSetCID" select="'{@valueSetCID}'"/>
				<xslout:with-param name="valueSetBDE" select="'{@valueSetBDE}'"/>
				<xslout:with-param name="cmValue" select="'{$cmValueQuoteSubstituted}'"/>
				<xslout:with-param name="csdValue" select="'{@csdValue}'"/>
				<xslout:with-param name="cvValue" select="'{@cvValue}'"/>
				<xslout:with-param name="unitsCID" select="'{@unitsCID}'"/>
				<xslout:with-param name="unitsBDE" select="'{@unitsBDE}'"/>
				<xslout:with-param name="cmUnits" select="'{$cmUnitsQuoteSubstituted}'"/>
				<xslout:with-param name="csdUnits" select="'{@csdUnits}'"/>
				<xslout:with-param name="cvUnits" select="'{@cvUnits}'"/>
				<xslout:with-param name="graphicType" select="'{@graphicType}'"/>
				<xslout:with-param name="numpointsmin" select="'{@numpointsmin}'"/>
				<xslout:with-param name="templatevmmin" select="$templatevmmin"/>
				<xslout:with-param name="templatevmmax" select="$templatevmmax"/>
				<xslout:with-param name="templateRequiredType" select="$templateRequiredType"/>
				<xslout:with-param name="templateConditionSatisfied" select="$templateConditionSatisfied"/>
				<xslout:with-param name="templateMBPO" select="$templateMBPO"/>
				<xslout:with-param name="nestingLevel" select="'{$nestingLevel}'"/>
				<xslout:with-param name="templateID" select="$templateID"/>
			</xslout:call-template>
		</xslout:when>
		<xslout:otherwise>
			<xslout:call-template name="CheckContentItem">
				<xslout:with-param name="description" select="'{$description}'"/>
				<xslout:with-param name="row" select="'{@row}'"/>
				<xslout:with-param name="relationship" select="'{$relationship}'"/>
				<xslout:with-param name="byReference" select="'{$byReference}'"/>
				<xslout:with-param name="valueType" select="'{$valueType}'"/>
				<xslout:with-param name="conceptNameCID" select="'{@conceptNameCID}'"/>
				<xslout:with-param name="cmConceptName" select="'{$cmConceptNameQuoteSubstituted}'"/>
				<xslout:with-param name="csdConceptName" select="'{@csdConceptName}'"/>
				<xslout:with-param name="cvConceptName" select="'{@cvConceptName}'"/>
				<xslout:with-param name="vmmin" select="'{@vmmin}'"/>
				<xslout:with-param name="vmmax" select="'{@vmmax}'"/>
				<xslout:with-param name="requiredType" select="'{@requiredType}'"/>
				<xslout:with-param name="conditionSatisfied" select="'F'"/>
				<xslout:with-param name="mbpo" select="'{@mbpo}'"/>
				<xslout:with-param name="matchRelationship" select="'{@matchRelationship}'"/>
				<xslout:with-param name="valueSetCID" select="'{@valueSetCID}'"/>
				<xslout:with-param name="valueSetBDE" select="'{@valueSetBDE}'"/>
				<xslout:with-param name="cmValue" select="'{$cmValueQuoteSubstituted}'"/>
				<xslout:with-param name="csdValue" select="'{@csdValue}'"/>
				<xslout:with-param name="cvValue" select="'{@cvValue}'"/>
				<xslout:with-param name="unitsCID" select="'{@unitsCID}'"/>
				<xslout:with-param name="unitsBDE" select="'{@unitsBDE}'"/>
				<xslout:with-param name="cmUnits" select="'{$cmUnitsQuoteSubstituted}'"/>
				<xslout:with-param name="csdUnits" select="'{@csdUnits}'"/>
				<xslout:with-param name="cvUnits" select="'{@cvUnits}'"/>
				<xslout:with-param name="graphicType" select="'{@graphicType}'"/>
				<xslout:with-param name="numpointsmin" select="'{@numpointsmin}'"/>
				<xslout:with-param name="numpointsmax" select="'{@numpointsmax}'"/>
				<xslout:with-param name="templatevmmin" select="$templatevmmin"/>
				<xslout:with-param name="templatevmmax" select="$templatevmmax"/>
				<xslout:with-param name="templateRequiredType" select="$templateRequiredType"/>
				<xslout:with-param name="templateConditionSatisfied" select="$templateConditionSatisfied"/>
				<xslout:with-param name="templateMBPO" select="$templateMBPO"/>
				<xslout:with-param name="nestingLevel" select="'{$nestingLevel}'"/>
				<xslout:with-param name="templateID" select="$templateID"/>
			</xslout:call-template>
		</xslout:otherwise>
		</xslout:choose>
	</xsl:when>
	<xsl:otherwise>
			<xslout:call-template name="CheckContentItem">
				<xslout:with-param name="description" select="'{$description}'"/>
				<xslout:with-param name="row" select="'{@row}'"/>
				<xslout:with-param name="relationship" select="'{$relationship}'"/>
				<xslout:with-param name="byReference" select="'{$byReference}'"/>
				<xslout:with-param name="valueType" select="'{$valueType}'"/>
				<xslout:with-param name="conceptNameCID" select="'{@conceptNameCID}'"/>
				<xslout:with-param name="cmConceptName" select="'{$cmConceptNameQuoteSubstituted}'"/>
				<xslout:with-param name="csdConceptName" select="'{@csdConceptName}'"/>
				<xslout:with-param name="cvConceptName" select="'{@cvConceptName}'"/>
				<xslout:with-param name="vmmin" select="'{@vmmin}'"/>
				<xslout:with-param name="vmmax" select="'{@vmmax}'"/>
				<xslout:with-param name="requiredType" select="'{@requiredType}'"/>
				<xslout:with-param name="matchRelationship" select="'{@matchRelationship}'"/>
				<xslout:with-param name="valueSetCID" select="'{@valueSetCID}'"/>
				<xslout:with-param name="valueSetBDE" select="'{@valueSetBDE}'"/>
				<xslout:with-param name="cmValue" select="'{$cmValueQuoteSubstituted}'"/>
				<xslout:with-param name="csdValue" select="'{@csdValue}'"/>
				<xslout:with-param name="cvValue" select="'{@cvValue}'"/>
				<xslout:with-param name="unitsCID" select="'{@unitsCID}'"/>
				<xslout:with-param name="unitsBDE" select="'{@unitsBDE}'"/>
				<xslout:with-param name="cmUnits" select="'{$cmUnitsQuoteSubstituted}'"/>
				<xslout:with-param name="csdUnits" select="'{@csdUnits}'"/>
				<xslout:with-param name="cvUnits" select="'{@cvUnits}'"/>
				<xslout:with-param name="graphicType" select="'{@graphicType}'"/>
				<xslout:with-param name="numpointsmin" select="'{@numpointsmin}'"/>
				<xslout:with-param name="numpointsmax" select="'{@numpointsmax}'"/>
				<xslout:with-param name="templatevmmin" select="$templatevmmin"/>
				<xslout:with-param name="templatevmmax" select="$templatevmmax"/>
				<xslout:with-param name="templateRequiredType" select="$templateRequiredType"/>
				<xslout:with-param name="templateConditionSatisfied" select="$templateConditionSatisfied"/>
				<xslout:with-param name="templateMBPO" select="$templateMBPO"/>
				<xslout:with-param name="nestingLevel" select="'{$nestingLevel}'"/>
				<xslout:with-param name="templateID" select="$templateID"/>
			</xslout:call-template>
	</xsl:otherwise>
	</xsl:choose>
	
	<xsl:call-template name="iterateOverChildren">
		<xsl:with-param name="description" select="$description"/>
		<xsl:with-param name="cvConceptName" select="@cvConceptName"/>
		<xsl:with-param name="csdConceptName" select="@csdConceptName"/>
		<xsl:with-param name="conceptNameCID" select="@conceptNameCID"/>
		<xsl:with-param name="valueType" select="$valueType"/>
	</xsl:call-template>

</xsl:template>

<xsl:template name="iterateOverChildren">
	<xsl:param name="description"/>
	<xsl:param name="cvConceptName"/>
	<xsl:param name="csdConceptName"/>
	<xsl:param name="conceptNameCID"/>
	<xsl:param name="valueType"/>
	<xsl:choose>
	<xsl:when test="string-length($cvConceptName) &gt; 0">
		<xsl:if test="count(child::*) &gt; 0">	<!-- Do not emit empty for-each statement or Saxon will warn about it, so anticipate when apply-templates will do nothing (000876)-->
			<xslout:for-each select="child::node()[name() = '{$valueType}' and concept/@cv = '{$cvConceptName}' and concept/@csd = '{$csdConceptName}']">
				<xsl:apply-templates/>
			</xslout:for-each>
		</xsl:if>
	</xsl:when>
	<xsl:when test="string-length($conceptNameCID) &gt; 0">
		<xsl:if test="count(child::*) &gt; 0">	<!-- Do not emit empty for-each statement or Saxon will warn about it, so anticipate when apply-templates will do nothing (000876)-->
			<xsl:variable name="currentNode" select="."/>
			<xsl:variable name="contextGroupWanted" select="document('DicomContextGroupsSource.xml')/definecontextgroups/definecontextgroup[@cid=$conceptNameCID]"/>
			<xsl:variable name="contextGroupCodes" select="$contextGroupWanted/contextgroupcode"/>
			<xsl:choose>
			<xsl:when test="count($contextGroupCodes) &gt; 0">
				<xsl:variable name="selectExpression">
					<xsl:text>child::node()[</xsl:text>
					<xsl:call-template name="buildConceptNameCIDPredicateExpression">
						<xsl:with-param name="valueType" select="$valueType"/>
						<xsl:with-param name="contextGroupCodes" select="$contextGroupCodes"/>
					</xsl:call-template>
					<xsl:text>]</xsl:text>
				</xsl:variable>
				<xslout:for-each select="{$selectExpression}">
					<xsl:apply-templates select="$currentNode/*"/>
				</xslout:for-each>
			</xsl:when>
			<xsl:otherwise>
				<xslout:text>Internal Error: </xslout:text><xsl:value-of select="$description"/><xslout:text>: Concept Name CID is empty or missing - </xslout:text><xsl:value-of select="$currentNode/@conceptNameCID"/><xslout:value-of select="$newline"/>
			</xsl:otherwise>
			</xsl:choose>
		</xsl:if>
	</xsl:when>
	<xsl:otherwise>
		<xsl:if test="count(child::*) &gt; 0">	<!-- Do not emit empty for-each statement or Saxon will warn about it, so anticipate when apply-templates will do nothing (000876)-->
			<xslout:for-each select="child::node()[name() = '{$valueType}']">
				<xsl:apply-templates/>
			</xslout:for-each>
		</xsl:if>
	</xsl:otherwise>
	</xsl:choose>
</xsl:template>

<xsl:template name="buildConceptNameCIDPredicateExpression">
	<xsl:param name="valueType"/>
	<xsl:param name="contextGroupCodes"/>
	
	<xsl:variable name="first" select="$contextGroupCodes[1]"/>
	<xsl:text>(name() = '</xsl:text><xsl:value-of select="$valueType"/><xsl:text>' and concept/@cv = '</xsl:text><xsl:value-of select="$first/@cv"/><xsl:text>' and concept/@csd = '</xsl:text><xsl:value-of select="$first/@csd"/><xsl:text>')</xsl:text>
	<xsl:variable name="rest" select="$contextGroupCodes[position() != 1]"/>
	<xsl:if test="$rest">
		<xsl:text> or </xsl:text>
		<xsl:call-template name="buildConceptNameCIDPredicateExpression">
			<xsl:with-param name="valueType" select="$valueType"/>
			<xsl:with-param name="contextGroupCodes" select="$rest"/>
		</xsl:call-template>
	</xsl:if>
</xsl:template>

<xsl:template match="verify">
	<xsl:variable name="escapedQuote">#</xsl:variable>
	<xsl:variable name="actualQuote">'</xsl:variable>
	<xsl:variable name="cmConceptNameQuoteSubstituted"><xsl:value-of select="translate(@cmConceptName,$actualQuote,$escapedQuote)"/></xsl:variable>       <!-- escape quotes -->
	<xsl:variable name="description">
		<xsl:call-template name="buildFullPathInDefinitionToCurrentContentItem">
			<xsl:with-param name="cmConceptNameQuoteSubstituted" select="$cmConceptNameQuoteSubstituted"/>
		</xsl:call-template>
	</xsl:variable>
	<xslout:if test="{@test}">
		<xslout:text><xsl:value-of select="@status"/>: <xsl:value-of select="$description"/>: </xslout:text>
		<xslout:call-template name="buildFullPathInInstanceToCurrentNode"/>
		<xslout:text>: <xsl:value-of select="@message"/></xslout:text><xslout:value-of select="$newline"/>
	</xslout:if>
</xsl:template>

<xsl:template name="buildFullPathInDefinitionToCurrentContentItem">
	<xsl:param name="cmConceptNameQuoteSubstituted"/>
	<xsl:if test="name(.) != 'definitions'">
		<xsl:for-each select="..">
			<xsl:variable name="escapedQuote">#</xsl:variable>
			<xsl:variable name="actualQuote">'</xsl:variable>
			<xsl:variable name="cmConceptNameQuoteSubstituted"><xsl:value-of select="translate(@cmConceptName,$actualQuote,$escapedQuote)"/></xsl:variable>       <!-- escape quotes -->
			<xsl:call-template name="buildFullPathInDefinitionToCurrentContentItem">
				<xsl:with-param name="cmConceptNameQuoteSubstituted" select="$cmConceptNameQuoteSubstituted"/>
			</xsl:call-template>
		</xsl:for-each>
		<xsl:choose>
		<xsl:when test="string-length(@row) != 0">
			<xsl:text>/</xsl:text>
			<xsl:text>[Row </xsl:text>
			<xsl:value-of select="@row"/>
			<xsl:text>] </xsl:text>
			<xsl:value-of select="translate(@valueType,$lowercase,$uppercase)"/>
			<xsl:choose>
			<xsl:when test="string-length(@conceptNameCID) != 0">
				<xsl:text> CID </xsl:text>
				<xsl:value-of select="@conceptNameCID"/>
			</xsl:when>
			<xsl:when test="string-length(@cvConceptName) != 0">
				<xsl:text> (</xsl:text>
				<xsl:value-of select="@cvConceptName"/>
				<xsl:text>,</xsl:text>
				<xsl:value-of select="@csdConceptName"/>
				<xsl:text>,"</xsl:text>
				<xsl:value-of select="$cmConceptNameQuoteSubstituted"/>
				<xsl:text>")</xsl:text>
			</xsl:when>
			<xsl:otherwise>
				<xsl:text> *</xsl:text>
			</xsl:otherwise>
			</xsl:choose>
		</xsl:when>
		<xsl:when test="string-length(@name) != 0">
			<xsl:if test="string-length(@tid) != 0">
				<xsl:text>Template </xsl:text>
				<xsl:value-of select="@tid"/>
				<xsl:text> </xsl:text>
			</xsl:if>
			<xsl:value-of select="@name"/>
		</xsl:when>
		</xsl:choose>
	</xsl:if>
</xsl:template>

<xsl:template name="buildFullPathInDefinitionToCurrentNode">
	<xsl:if test="name(.) != 'definitions'">
		<xsl:for-each select="..">
			<xsl:call-template name="buildFullPathInDefinitionToCurrentNode"/>
		</xsl:for-each>
		<xsl:value-of select="@name"/>
		<xsl:text>/</xsl:text>
	</xsl:if>
</xsl:template>

</xsl:stylesheet>
