<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:xslout="bla"
	version="1.0">

<xsl:output method="xml" indent="yes"/>

<xsl:namespace-alias stylesheet-prefix="xslout" result-prefix="xsl"/>

<xsl:template match="definitions">
	<xslout:stylesheet version="1.0">
	
	<xslout:import href="CommonDicomIODValidationRules.xsl"/>
	
	<xslout:output method="text"/>

	<xslout:template match="/DicomObject">
	<xslout:choose>
	<xsl:apply-templates select="defineiod"/>
	<xslout:otherwise>
	<xslout:text>IOD (SOP Class) unrecognized</xslout:text><xslout:value-of select="$newline"/>
	</xslout:otherwise>
	</xslout:choose>
	</xslout:template>
	
	<xsl:apply-templates select="definemodule"/>
	<xsl:apply-templates select="definemacro"/>

	</xslout:stylesheet>
</xsl:template>

<xsl:template match="defineiod">
	<xslout:when test="/DicomObject/SOPClassUID/value = '{@sopclass}'">
	<xslout:text>Found <xsl:value-of select="@name"/> IOD</xslout:text><xslout:value-of select="$newline"/>
	<xsl:apply-templates select="invokemodule"/>
	<xslout:text>IOD validation complete</xslout:text><xslout:value-of select="$newline"/>
	</xslout:when>
</xsl:template>

<xsl:template match="invokemodule">
	<xsl:choose>
	<xsl:when test="@type='M'">
		<xslout:if test="$optionDescribeChecking='T'"><xslout:text>Checking Module: <xsl:value-of select="@name"/></xslout:text><xslout:value-of select="$newline"/></xslout:if>
		<xslout:call-template name="{@name}"/>
	</xsl:when>
	<xsl:when test="@type='C' or @type='U'">
		<xsl:choose>
		<xsl:when test="count(@nocondition) > 0">
			<xslout:if test="$optionDescribeChecking='T'"><xslout:text>Checking Module: <xsl:value-of select="@name"/></xslout:text><xslout:value-of select="$newline"/></xslout:if>
			<xslout:call-template name="{@name}"/>
		</xsl:when>
		<xsl:when test="count(@condition) > 0">
			<xslout:if test="{@condition}">
				<xslout:if test="$optionDescribeChecking='T'"><xslout:text>Checking Module (condition true): <xsl:value-of select="@name"/></xslout:text><xslout:value-of select="$newline"/></xslout:if>
				<xslout:call-template name="{@name}"/>
			</xslout:if>
		</xsl:when>
		</xsl:choose>
	</xsl:when>
	</xsl:choose>
</xsl:template>

<xsl:template match="definemodule">
	<xslout:template name="{@name}">
	<xsl:apply-templates/>
	</xslout:template>
</xsl:template>

<xsl:template match="includemacro">
	<xsl:choose>
	<xsl:when test="count(@type) = 0 or @type='M'">
		<xslout:if test="$optionDescribeChecking='T'"><xslout:text>Checking Macro: <xsl:value-of select="@name"/></xslout:text><xslout:value-of select="$newline"/></xslout:if>
		<xslout:call-template name="{@name}"/>
	</xsl:when>
	<xsl:when test="@type='C' or @type='U'">
		<xsl:choose>
		<xsl:when test="count(@nocondition) > 0">
			<xslout:if test="$optionDescribeChecking='T'"><xslout:text>Checking Macro: <xsl:value-of select="@name"/></xslout:text><xslout:value-of select="$newline"/></xslout:if>
			<xslout:call-template name="{@name}"/>
		</xsl:when>
		<xsl:when test="count(@condition) > 0">
			<xslout:if test="{@condition}">
				<xslout:if test="$optionDescribeChecking='T'"><xslout:text>Checking Macro (condition true): <xsl:value-of select="@name"/></xslout:text><xslout:value-of select="$newline"/></xslout:if>
				<xslout:call-template name="{@name}"/>
			</xslout:if>
		</xsl:when>
		</xsl:choose>
	</xsl:when>
	</xsl:choose>
</xsl:template>

<xsl:template match="definemacro">
	<xslout:template name="{@name}">
	<xsl:apply-templates/>
	</xslout:template>
</xsl:template>

<xsl:template match="attribute">
	<xsl:variable name="description"><xsl:call-template name="buildFullPathInDefinitionToCurrentNode"/></xsl:variable>
	<xslout:call-template name="CheckAttributeVR"><xslout:with-param name="description" select="'{$description}'"/><xslout:with-param name="element" select="{@name}"/><xslout:with-param name="vr" select="'{@vr}'"/></xslout:call-template>
	<xsl:choose>
	<xsl:when test="@type ='1'">
		<xslout:call-template name="CheckType1Attribute"><xslout:with-param name="description" select="'{$description}'"/><xslout:with-param name="element" select="{@name}"/><xslout:with-param name="vmmin" select="'{@vmmin}'"/><xslout:with-param name="vmmax" select="'{@vmmax}'"/><xslout:with-param name="vr" select="'{@vr}'"/></xslout:call-template>
	</xsl:when>
	<xsl:when test="@type ='2'">
		<xslout:call-template name="CheckType2Attribute"><xslout:with-param name="description" select="'{$description}'"/><xslout:with-param name="element" select="{@name}"/><xslout:with-param name="vmmin" select="'{@vmmin}'"/><xslout:with-param name="vmmax" select="'{@vmmax}'"/></xslout:call-template>
	</xsl:when>
	<xsl:when test="@type ='3'">
		<xslout:call-template name="CheckType3Attribute"><xslout:with-param name="description" select="'{$description}'"/><xslout:with-param name="element" select="{@name}"/><xslout:with-param name="vmmin" select="'{@vmmin}'"/><xslout:with-param name="vmmax" select="'{@vmmax}'"/></xslout:call-template>
	</xsl:when>
	<xsl:when test="@type ='1C'">
		<xsl:if test="count(@nocondition) = 0">
			<xslout:choose>
			<xslout:when test="{@condition}">
				<xslout:call-template name="CheckConditionalAttributeWhenConditionSatisfied"><xslout:with-param name="description" select="'{$description}'"/><xslout:with-param name="element" select="{@name}"/></xslout:call-template>
			</xslout:when>
			<xslout:otherwise>
				<xslout:call-template name="CheckConditionalAttributeWhenConditionNotSatisfied"><xslout:with-param name="description" select="'{$description}'"/><xslout:with-param name="element" select="{@name}"/><xslout:with-param name="mbpo" select="'{@mbpo}'"/></xslout:call-template>
			</xslout:otherwise>
			</xslout:choose>
		</xsl:if>
		<xslout:call-template name="CheckType1CAttributeRegardless"><xslout:with-param name="description" select="'{$description}'"/><xslout:with-param name="element" select="{@name}"/><xslout:with-param name="vmmin" select="'{@vmmin}'"/><xslout:with-param name="vmmax" select="'{@vmmax}'"/><xslout:with-param name="vr" select="'{@vr}'"/></xslout:call-template>
	</xsl:when>
	<xsl:when test="@type ='2C'">
		<xsl:if test="count(@nocondition) = 0">
			<xslout:choose>
			<xslout:when test="{@condition}">
				<xslout:call-template name="CheckConditionalAttributeWhenConditionSatisfied"><xslout:with-param name="description" select="'{$description}'"/><xslout:with-param name="element" select="{@name}"/></xslout:call-template>
			</xslout:when>
			<xslout:otherwise>
				<xslout:call-template name="CheckConditionalAttributeWhenConditionNotSatisfied"><xslout:with-param name="description" select="'{$description}'"/><xslout:with-param name="element" select="{@name}"/><xslout:with-param name="mbpo" select="'{@mbpo}'"/></xslout:call-template>
			</xslout:otherwise>
			</xslout:choose>
		</xsl:if>
		<xslout:call-template name="CheckType2CAttributeRegardless"><xslout:with-param name="description" select="'{$description}'"/><xslout:with-param name="element" select="{@name}"/><xslout:with-param name="vmmin" select="'{@vmmin}'"/><xslout:with-param name="vmmax" select="'{@vmmax}'"/></xslout:call-template>
	</xsl:when>
	<xsl:when test="@type ='3C'">
		<xsl:if test="count(@nocondition) = 0">
			<xslout:choose>
			<xslout:when test="{@condition}">
			</xslout:when>
			<xslout:otherwise>
				<xslout:call-template name="CheckConditionalAttributeWhenConditionNotSatisfied"><xslout:with-param name="description" select="'{$description}'"/><xslout:with-param name="element" select="{@name}"/><xslout:with-param name="mbpo" select="'F'"/></xslout:call-template>
			</xslout:otherwise>
			</xslout:choose>
		</xsl:if>
		<xslout:call-template name="CheckType3Attribute"><xslout:with-param name="description" select="'{$description}'"/><xslout:with-param name="element" select="{@name}"/><xslout:with-param name="vmmin" select="'{@vmmin}'"/><xslout:with-param name="vmmax" select="'{@vmmax}'"/></xslout:call-template>
	</xsl:when>
	<xsl:otherwise>
	</xsl:otherwise>
	</xsl:choose>
	<xsl:apply-templates/>
</xsl:template>

<xsl:template match="sequence">
	<xsl:variable name="description"><xsl:call-template name="buildFullPathInDefinitionToCurrentNode"/></xsl:variable>
	<xsl:choose>
	<xsl:when test="@type ='1'">
		<xslout:call-template name="CheckType1SequenceAttribute"><xslout:with-param name="description" select="'{$description}'"/><xslout:with-param name="element" select="{@name}"/><xslout:with-param name="vmmin" select="'{@vmmin}'"/><xslout:with-param name="vmmax" select="'{@vmmax}'"/></xslout:call-template>
	</xsl:when>
	<xsl:when test="@type ='2'">
		<xslout:call-template name="CheckType2SequenceAttribute"><xslout:with-param name="description" select="'{$description}'"/><xslout:with-param name="element" select="{@name}"/><xslout:with-param name="vmmin" select="'{@vmmin}'"/><xslout:with-param name="vmmax" select="'{@vmmax}'"/></xslout:call-template>
	</xsl:when>
	<xsl:when test="@type ='3'">
		<xslout:call-template name="CheckType3SequenceAttribute"><xslout:with-param name="description" select="'{$description}'"/><xslout:with-param name="element" select="{@name}"/><xslout:with-param name="vmmin" select="'{@vmmin}'"/><xslout:with-param name="vmmax" select="'{@vmmax}'"/></xslout:call-template>
	</xsl:when>
	<xsl:when test="@type ='1C'">
		<xsl:if test="count(@nocondition) = 0">
			<xslout:choose>
			<xslout:when test="{@condition}">
				<xslout:call-template name="CheckConditionalAttributeWhenConditionSatisfied"><xslout:with-param name="description" select="'{$description}'"/><xslout:with-param name="element" select="{@name}"/></xslout:call-template>
			</xslout:when>
			<xslout:otherwise>
				<xslout:call-template name="CheckConditionalAttributeWhenConditionNotSatisfied"><xslout:with-param name="description" select="'{$description}'"/><xslout:with-param name="element" select="{@name}"/><xslout:with-param name="mbpo" select="'{@mbpo}'"/></xslout:call-template>
			</xslout:otherwise>
			</xslout:choose>
		</xsl:if>
		<xslout:call-template name="CheckType1CSequenceAttributeRegardless"><xslout:with-param name="description" select="'{$description}'"/><xslout:with-param name="element" select="{@name}"/><xslout:with-param name="vmmin" select="'{@vmmin}'"/><xslout:with-param name="vmmax" select="'{@vmmax}'"/></xslout:call-template>
	</xsl:when>
	<xsl:when test="@type ='2C'">
		<xsl:if test="count(@nocondition) = 0">
			<xslout:choose>
			<xslout:when test="{@condition}">
				<xslout:call-template name="CheckConditionalAttributeWhenConditionSatisfied"><xslout:with-param name="description" select="'{$description}'"/><xslout:with-param name="element" select="{@name}"/></xslout:call-template>
			</xslout:when>
			<xslout:otherwise>
				<xslout:call-template name="CheckConditionalAttributeWhenConditionNotSatisfied"><xslout:with-param name="description" select="'{$description}'"/><xslout:with-param name="element" select="{@name}"/><xslout:with-param name="mbpo" select="'{@mbpo}'"/></xslout:call-template>
			</xslout:otherwise>
			</xslout:choose>
		</xsl:if>
		<xslout:call-template name="CheckType2CSequenceAttributeRegardless"><xslout:with-param name="description" select="'{$description}'"/><xslout:with-param name="element" select="{@name}"/><xslout:with-param name="vmmin" select="'{@vmmin}'"/><xslout:with-param name="vmmax" select="'{@vmmax}'"/></xslout:call-template>
	</xsl:when>
	<xsl:when test="@type ='3C'">
		<xsl:if test="count(@nocondition) = 0">
			<xslout:choose>
			<xslout:when test="{@condition}">
			</xslout:when>
			<xslout:otherwise>
				<xslout:call-template name="CheckConditionalAttributeWhenConditionNotSatisfied"><xslout:with-param name="description" select="'{$description}'"/><xslout:with-param name="element" select="{@name}"/><xslout:with-param name="mbpo" select="'F'"/></xslout:call-template>
			</xslout:otherwise>
			</xslout:choose>
		</xsl:if>
		<xslout:call-template name="CheckType3SequenceAttribute"><xslout:with-param name="description" select="'{$description}'"/><xslout:with-param name="element" select="{@name}"/><xslout:with-param name="vmmin" select="'{@vmmin}'"/><xslout:with-param name="vmmax" select="'{@vmmax}'"/></xslout:call-template>
	</xsl:when>
	<xsl:otherwise>
	</xsl:otherwise>
	</xsl:choose>
	<xslout:for-each select="{@name}/Item">
		<xsl:apply-templates/>
	</xslout:for-each>
</xsl:template>

<xsl:template match="dumpvalue">
	<xslout:text>Dump value: <xsl:value-of select="@message"/>: </xslout:text><xslout:value-of select="{@execute}"/><xslout:value-of select="$newline"/>
</xsl:template>

<xsl:template match="dumpname">
	<xslout:text>Dump name: <xsl:value-of select="@what"/>: </xslout:text><xslout:value-of select="name({@what})"/><xslout:value-of select="$newline"/>
</xsl:template>

<xsl:template match="verify">
	<xslout:if test="count({../@name}) &gt; 0">
		<xsl:variable name="description">
			<xsl:call-template name="buildFullPathInDefinitionToSelectedNode">
				<xsl:with-param name="node" select=".."/>
			</xsl:call-template>
		</xsl:variable>
		<xslout:if test="{@test}">
			<xslout:text><xsl:value-of select="@status"/>: <xsl:value-of select="$description"/>: </xslout:text>
			<xslout:call-template name="buildFullPathInInstanceToNamedChildOfCurrentNode">
				<xslout:with-param name="node" select="{../@name}"/>
			</xslout:call-template>
			<xslout:text>: <xsl:value-of select="@message"/></xslout:text><xslout:value-of select="$newline"/>
		</xslout:if>
	</xslout:if>
</xsl:template>

<xsl:template match="verifyanywhere">
		<xsl:variable name="description">
			<xsl:call-template name="buildFullPathInDefinitionToSelectedNode">
				<xsl:with-param name="node" select=".."/>
			</xsl:call-template>
		</xsl:variable>
		<xslout:if test="{@test}">
			<xslout:text><xsl:value-of select="@status"/>: <xsl:value-of select="$description"/>: </xslout:text>
			<xslout:call-template name="buildFullPathInInstanceToNamedChildOfCurrentNode">
				<xslout:with-param name="node" select="{../@name}"/>
			</xslout:call-template>
			<xslout:text>: <xsl:value-of select="@message"/></xslout:text><xslout:value-of select="$newline"/>
		</xslout:if>
</xsl:template>

<xsl:template match="enumeratedvalueslist">
	<xslout:if test="count({../@name}/value) &gt; 0">
		<xsl:variable name="description">
			<xsl:call-template name="buildFullPathInDefinitionToSelectedNode">
				<xsl:with-param name="node" select=".."/>
			</xsl:call-template>
		</xsl:variable>
		<xslout:variable name="valuewasfound">
			<xslout:choose>
			<xsl:for-each select="enumeratedvalue">
				<xsl:choose>
				<xsl:when test="count(../@valueselector) &gt; 0">
					<xslout:when test="{../../@name}/value[@number={../@valueselector}] = '{@value}'">T</xslout:when>
				</xsl:when>
				<xsl:otherwise>
					<xslout:when test="{../../@name}/value = '{@value}'">T</xslout:when>
				</xsl:otherwise>
				</xsl:choose>
			</xsl:for-each>
			<xslout:otherwise>F</xslout:otherwise>
			</xslout:choose>
		</xslout:variable>
		<xslout:if test="$valuewasfound = 'F'">
			<xslout:text>Error: <xsl:value-of select="$description"/>: </xslout:text>
			<xslout:call-template name="buildFullPathInInstanceToNamedChildOfCurrentNode">
				<xslout:with-param name="node" select="{../@name}"/>
			</xslout:call-template>			
			<xsl:choose>
			<xsl:when test="count(@valueselector) &gt; 0">
				<xslout:text>/value[</xslout:text><xslout:value-of select="{@valueselector}"/><xslout:text>]: </xslout:text>
				<xslout:text>Unrecognized enumerated value :</xslout:text><xslout:value-of select="{../@name}/value[@number={@valueselector}]"/><xslout:text>:</xslout:text><xslout:value-of select="$newline"/>
			</xsl:when>
			<xsl:otherwise>
				<xslout:text>: Unrecognized enumerated value :</xslout:text><xslout:value-of select="{../@name}/value"/><xslout:text>:</xslout:text><xslout:value-of select="$newline"/>
			</xsl:otherwise>
			</xsl:choose>
		</xslout:if>
	</xslout:if>
</xsl:template>

<xsl:template match="definedtermslist">
	<xslout:if test="count({../@name}/value) &gt; 0">
		<xsl:variable name="description">
			<xsl:call-template name="buildFullPathInDefinitionToSelectedNode">
				<xsl:with-param name="node" select=".."/>
			</xsl:call-template>
		</xsl:variable>
		<xslout:variable name="valuewasfound">
			<xslout:choose>
			<xsl:for-each select="definedterm">
				<xsl:choose>
				<xsl:when test="@value = ''">	<!-- handle zero length defined term specially, in that absence is equivalent to presence with an empty value -->
					<xsl:choose>
					<xsl:when test="count(../@valueselector) &gt; 0">
						<xslout:when test="count({../../@name}/value[@number={../@valueselector}]) = 0 or {../../@name}/value[@number={../@valueselector}] = ''">T</xslout:when>
					</xsl:when>
					<xsl:otherwise>
						<xslout:when test="count({../../@name}/value) = 0 or {../../@name}/value = ''">T</xslout:when>
					</xsl:otherwise>
					</xsl:choose>
				</xsl:when>
				<xsl:otherwise>
					<xsl:choose>
					<xsl:when test="count(../@valueselector) &gt; 0">
						<xslout:when test="{../../@name}/value[@number={../@valueselector}] = '{@value}'">T</xslout:when>
					</xsl:when>
					<xsl:otherwise>
						<xslout:when test="{../../@name}/value = '{@value}'">T</xslout:when>
					</xsl:otherwise>
					</xsl:choose>
				</xsl:otherwise>
				</xsl:choose>
			</xsl:for-each>
			<xslout:otherwise>F</xslout:otherwise>
			</xslout:choose>
		</xslout:variable>
		<xslout:if test="$valuewasfound = 'F'">
			<xslout:text>Warning: <xsl:value-of select="$description"/>: </xslout:text>
			<xslout:call-template name="buildFullPathInInstanceToNamedChildOfCurrentNode">
				<xslout:with-param name="node" select="{../@name}"/>
			</xslout:call-template>
			<xsl:choose>
			<xsl:when test="count(@valueselector) &gt; 0">
				<xslout:text>/value[</xslout:text><xslout:value-of select="{@valueselector}"/><xslout:text>]: </xslout:text>
				<xslout:text>Unrecognized defined term :</xslout:text><xslout:value-of select="{../@name}/value[@number={@valueselector}]"/><xslout:text>:</xslout:text><xslout:value-of select="$newline"/>
			</xsl:when>
			<xsl:otherwise>
				<xslout:text>: Unrecognized defined term :</xslout:text><xslout:value-of select="{../@name}/value"/><xslout:text>:</xslout:text><xslout:value-of select="$newline"/>
			</xsl:otherwise>
			</xsl:choose>
		</xslout:if>
	</xslout:if>
</xsl:template>

<xsl:template name="buildFullPathInDefinitionToSelectedNode">
	<xsl:param name="node"/>
	<xsl:for-each select="$node">
		<xsl:call-template name="buildFullPathInDefinitionToCurrentNode"/>
	</xsl:for-each>
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
