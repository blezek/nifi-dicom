<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	version="1.0">

<xsl:variable name="newline">
<xsl:text>
</xsl:text>
</xsl:variable>

<xsl:param name="optionDescribeChecking" select="'F'"/>

<xsl:param name="optionMatchCaseOfCodeMeaning" select="'F'"/>

<xsl:param name="optionCheckTemplateID" select="'F'"/>

<xsl:variable name="optionEmbedMatchedLocationsInOutputWithElement" select="'item'"/>

<xsl:variable name="lowercase" select="'abcdefghijklmnopqrstuvwxyz'" />
<xsl:variable name="uppercase" select="'ABCDEFGHIJKLMNOPQRSTUVWXYZ'" />

<xsl:template name="describeCodeActual">
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

<xsl:template name="describeCodeWanted">
	<xsl:param name="cv"/>
	<xsl:param name="csd"/>
	<xsl:param name="cm"/>

	<xsl:variable name="escapedQuote">#</xsl:variable>
	<xsl:variable name="actualQuote">'</xsl:variable>
	<xsl:variable name="cmQuoteSubstituted"><xsl:value-of select="translate($cm,$escapedQuote,$actualQuote)"/></xsl:variable>       <!-- recover escaped quotes -->

	<xsl:text>(</xsl:text>
	<xsl:value-of select="$cv"/>
	<xsl:text>,</xsl:text>
	<xsl:value-of select="$csd"/>
	<xsl:text>,"</xsl:text>
	<xsl:value-of select="$cmQuoteSubstituted"/>
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
			<xsl:call-template name="describeCodeActual">
				<xsl:with-param name="cv" select="concept/@cv"/>
				<xsl:with-param name="csd" select="concept/@csd"/>
				<xsl:with-param name="cm" select="concept/@cm"/>
			</xsl:call-template>
		</xsl:when>
		</xsl:choose>
	</xsl:if>
</xsl:template>

<xsl:template name="buildDescriptionOfContentItem">
	<xsl:param name="node"/>
	<xsl:text>content item (</xsl:text><xsl:value-of select="substring($node/@ID,4)"/><xsl:text>: </xsl:text><xsl:value-of select="translate(name($node),$lowercase,$uppercase)"/><xsl:text>)</xsl:text>
</xsl:template>

<xsl:template name="buildDescriptionOfReferencedContentItem">
	<xsl:param name="node"/>
	<xsl:text>content item (</xsl:text><xsl:value-of select="substring($node/@IDREF,4)"/><xsl:text>: </xsl:text><xsl:value-of select="translate(name(key('idkey',$node/@IDREF)),$lowercase,$uppercase)"/><xsl:text>)</xsl:text>
</xsl:template>

<xsl:template name="checkPermittedChildContentItemByValueRelationship">
	<xsl:param name="parent"/>
	<xsl:param name="child"/>
	<xsl:if test="$optionDescribeChecking='T'">
		<xsl:text>Parent </xsl:text>
		<xsl:call-template name="buildDescriptionOfContentItem"><xsl:with-param name="node" select="$parent"/></xsl:call-template>
		<xsl:text> has good relationship </xsl:text><xsl:value-of select="$child/@relationship"/>
		<xsl:text> with child </xsl:text>
		<xsl:call-template name="buildDescriptionOfContentItem"><xsl:with-param name="node" select="$child"/></xsl:call-template>
		<xsl:value-of select="$newline"/>
	</xsl:if>
</xsl:template>

<xsl:template name="checkPermittedChildContentItemByReferenceRelationship">
	<xsl:param name="parent"/>
	<xsl:param name="child"/>
	<xsl:if test="$optionDescribeChecking='T'">
		<xsl:text>Parent </xsl:text>
		<xsl:call-template name="buildDescriptionOfContentItem"><xsl:with-param name="node" select="$parent"/></xsl:call-template>
		<xsl:text> has good relationship R-</xsl:text><xsl:value-of select="$child/@relationship"/>
		<xsl:text> with child </xsl:text>
		<xsl:call-template name="buildDescriptionOfReferencedContentItem"><xsl:with-param name="node" select="$child"/></xsl:call-template>
		<xsl:value-of select="$newline"/>
	</xsl:if>
</xsl:template>

<xsl:template name="describeIllegalChildContentItem">
	<xsl:param name="parent"/>
	<xsl:param name="child"/>
	<xsl:text>Parent </xsl:text>
	<xsl:call-template name="buildDescriptionOfContentItem"><xsl:with-param name="node" select=".."/></xsl:call-template>
	<xsl:text> has illegal relationship </xsl:text>
	<xsl:choose>
	<xsl:when test="name(.) = 'reference'">
		<xsl:text>R-</xsl:text><xsl:value-of select="./@relationship"/>
		<xsl:text> with child </xsl:text>
		<xsl:call-template name="buildDescriptionOfReferencedContentItem"><xsl:with-param name="node" select="."/></xsl:call-template>
	</xsl:when>
	<xsl:otherwise>
		<xsl:value-of select="./@relationship"/>
		<xsl:text> with child </xsl:text>
		<xsl:call-template name="buildDescriptionOfContentItem"><xsl:with-param name="node" select="."/></xsl:call-template>
	</xsl:otherwise>
	</xsl:choose>
	<xsl:value-of select="$newline"/>
</xsl:template>

<xsl:template name="CheckContentItem">
	<xsl:param name="description"/>
	<xsl:param name="row"/>
	<xsl:param name="relationship"/>
	<xsl:param name="byReference"/>
	<xsl:param name="valueType"/>
	<xsl:param name="conceptNameCID"/>
	<xsl:param name="cmConceptName"/>
	<xsl:param name="csdConceptName"/>
	<xsl:param name="cvConceptName"/>
	<xsl:param name="vmmin"/>
	<xsl:param name="vmmax"/>
	<xsl:param name="requiredType"/>
	<xsl:param name="conditionSatisfied"/>
	<xsl:param name="mbpo"/>
	<xsl:param name="matchRelationship"/>
	<xsl:param name="valueSetCID"/>
	<xsl:param name="valueSetBDE"/>
	<xsl:param name="cmValue"/>
	<xsl:param name="csdValue"/>
	<xsl:param name="cvValue"/>
	<xsl:param name="unitsCID"/>
	<xsl:param name="unitsBDE"/>
	<xsl:param name="cmUnits"/>
	<xsl:param name="csdUnits"/>
	<xsl:param name="cvUnits"/>
	<xsl:param name="graphicType"/>
	<xsl:param name="numpointsmin"/>
	<xsl:param name="numpointsmax"/>
	<xsl:param name="templatevmmin"/>
	<xsl:param name="templatevmmax"/>
	<xsl:param name="templateRequiredType"/>
	<xsl:param name="templateConditionSatisfied"/>
	<xsl:param name="templateMBPO"/>
	<xsl:param name="nestingLevel"/>
	<xsl:param name="templateID"/>

	<xsl:if test="$optionDescribeChecking='T'">
		<xsl:text>CheckContentItem: </xsl:text><xsl:value-of select="$description"/><xsl:text>: within </xsl:text><xsl:value-of select="substring-after(@ID,'ci_')"/><xsl:text>: </xsl:text><xsl:call-template name="buildFullPathInInstanceToCurrentNode"/><xsl:value-of select="$newline"/>
		<xsl:text>CheckContentItem: templateID = </xsl:text><xsl:value-of select="$templateID"/><xsl:value-of select="$newline"/>
		<xsl:text>CheckContentItem: row = </xsl:text><xsl:value-of select="$row"/><xsl:value-of select="$newline"/>
		<xsl:text>CheckContentItem: relationship = </xsl:text><xsl:value-of select="$relationship"/><xsl:value-of select="$newline"/>
		<xsl:text>CheckContentItem: byReference = </xsl:text><xsl:value-of select="$byReference"/><xsl:value-of select="$newline"/>
		<xsl:text>CheckContentItem: valueType = </xsl:text><xsl:value-of select="$valueType"/><xsl:value-of select="$newline"/>
		<xsl:text>CheckContentItem: matchRelationship = </xsl:text><xsl:value-of select="$matchRelationship"/><xsl:value-of select="$newline"/>
		<xsl:text>CheckContentItem: requiredType = </xsl:text><xsl:value-of select="$requiredType"/><xsl:value-of select="$newline"/>
		<xsl:text>CheckContentItem: conditionSatisfied = </xsl:text><xsl:value-of select="$conditionSatisfied"/><xsl:value-of select="$newline"/>
		<xsl:text>CheckContentItem: mbpo = </xsl:text><xsl:value-of select="$mbpo"/><xsl:value-of select="$newline"/>
		<xsl:text>CheckContentItem: vmmin = </xsl:text><xsl:value-of select="$vmmin"/><xsl:value-of select="$newline"/>
		<xsl:text>CheckContentItem: vmmax = </xsl:text><xsl:value-of select="$vmmax"/><xsl:value-of select="$newline"/>
		<xsl:text>CheckContentItem: templatevmmin = </xsl:text><xsl:value-of select="$templatevmmin"/><xsl:value-of select="$newline"/>
		<xsl:text>CheckContentItem: templatevmmax = </xsl:text><xsl:value-of select="$templatevmmax"/><xsl:value-of select="$newline"/>
		<xsl:text>CheckContentItem: templateRequiredType = </xsl:text><xsl:value-of select="$templateRequiredType"/><xsl:value-of select="$newline"/>
		<xsl:text>CheckContentItem: templateConditionSatisfied = </xsl:text><xsl:value-of select="$templateConditionSatisfied"/><xsl:value-of select="$newline"/>
		<xsl:text>CheckContentItem: templateMBPO = </xsl:text><xsl:value-of select="$templateMBPO"/><xsl:value-of select="$newline"/>
		<xsl:text>CheckContentItem: nestingLevel = </xsl:text><xsl:value-of select="$nestingLevel"/><xsl:value-of select="$newline"/>
	</xsl:if>
	
	<xsl:variable name="contextgroupcodesforconceptname" select="document('DicomContextGroupsSource.xml')/definecontextgroups/definecontextgroup[@cid=$conceptNameCID]/contextgroupcode/@cv"/>
	<xsl:text>Count of contextgroupcodesforconceptname for </xsl:text><xsl:value-of select="$conceptNameCID"/><xsl:text> is </xsl:text><xsl:value-of select="count($contextgroupcodesforconceptname)"/><xsl:value-of select="$newline"/>
	<!--<xsl:variable name="contextgroupcodevaluesforconceptname" select="string($contextgroupcodesforconceptname/@cv)"/>-->
	
	<xsl:variable name="contextgroupcodevaluesforconceptname">
		<xsl:text> </xsl:text>	<!-- initial "delimiter" since going to be using delimiters around match candidates later to assure exact not partial match -->
		<xsl:for-each select="document('DicomContextGroupsSource.xml')/definecontextgroups/definecontextgroup[@cid=$conceptNameCID]/contextgroupcode">
			<xsl:value-of select="@cv"/>
			<xsl:text>,</xsl:text>
			<!-- not really much point in calling getPreferredCodingSchemeDesignator, since probably used in template source anyway, and cannot call it within matching code in predicate later anyway :( -->
			<xsl:call-template name="getPreferredCodingSchemeDesignator">
				<xsl:with-param name="csd" select="@csd"/>
			</xsl:call-template>
			<xsl:text> </xsl:text>
		</xsl:for-each>
	</xsl:variable>
	<xsl:text>Value of contextgroupcodevaluesforconceptname for </xsl:text><xsl:value-of select="$conceptNameCID"/><xsl:text> is </xsl:text><xsl:value-of select="$contextgroupcodevaluesforconceptname"/><xsl:value-of select="$newline"/>

	<xsl:variable name="matchlistsamevaluetypeandrelationship" select="child::node()[
			(name() = $valueType or ($byReference = 'T' and name() = 'reference'))
		and ($matchRelationship != 'T' or (@relationship = $relationship and ($byReference = 'T' and name() = 'reference' or $byReference != 'T' and name() != 'reference')))
		]"/>
	<xsl:text>Count of matchlistsamevaluetypeandrelationship for </xsl:text><xsl:value-of select="$conceptNameCID"/><xsl:text> is </xsl:text><xsl:value-of select="count($matchlistsamevaluetypeandrelationship)"/><xsl:value-of select="$newline"/>
	
	<xsl:text>Candidates for match from matchlistsamevaluetypeandrelationship are </xsl:text>
	<xsl:for-each select="$matchlistsamevaluetypeandrelationship">
		<xsl:call-template name="describeCodeWanted">
			<xsl:with-param name="cv"  select="concept/@cv"/>
			<xsl:with-param name="csd" select="concept/@csd"/>
			<xsl:with-param name="cm"  select="concept/@cm"/>
		</xsl:call-template>
		<xsl:text>, </xsl:text>
	</xsl:for-each>
	<xsl:value-of select="$newline"/>
	
	<xsl:variable name="matchlistsamevaluetypeandrelationshipfilteredbyconcept" select="$matchlistsamevaluetypeandrelationship[
				(string-length($cvConceptName) &gt; 0 and concept/@cv = $cvConceptName and concept/@csd = $csdConceptName)
			 or	(string-length($conceptNameCID) &gt; 0 and contains($contextgroupcodevaluesforconceptname,concept/@cv))
		]"/>
	<xsl:text>Count of matchlistsamevaluetypeandrelationshipfilteredbyconcept for </xsl:text><xsl:value-of select="$conceptNameCID"/><xsl:text> is </xsl:text><xsl:value-of select="count($matchlistsamevaluetypeandrelationshipfilteredbyconcept)"/><xsl:value-of select="$newline"/>
	
	<xsl:for-each select="$matchlistsamevaluetypeandrelationship">
		<xsl:text>Contains </xsl:text><xsl:value-of select="concept/@cv"/><xsl:text> is </xsl:text><xsl:value-of select="contains($contextgroupcodevaluesforconceptname,concat(' ',concept/@cv,',',concept/@csd,' '))"/><xsl:value-of select="$newline"/>
	</xsl:for-each>
	
	<!-- Do NOT check node name matches valueType if by reference relationship, since node name will be reference; note also that the @relationship value will NOT be preceded by "R-"-->
	<!-- matchRelationship causes a match only if both the relationship and the mode (by-value or by-reference) are the same -->
	<!-- matchlist is NOT filtered by match to $conceptNameCID if present, since cannot select contents of CID in document('DicomContextGroupsSource.xml') by attributes using attributes of child::node() context :( -->
	<xsl:variable name="matchlist" select="child::node()[
			(name() = $valueType or ($byReference = 'T' and name() = 'reference'))
		and ($matchRelationship != 'T' or (@relationship = $relationship and ($byReference = 'T' and name() = 'reference' or $byReference != 'T' and name() != 'reference')))
		and (
				(string-length($cvConceptName) &gt; 0 and concept/@cv = $cvConceptName and concept/@csd = $csdConceptName)
			 or	(string-length($conceptNameCID) &gt; 0 and contains($contextgroupcodevaluesforconceptname,concat(' ',concept/@cv,',',concept/@csd,' ')))
			)
		]"/>
	<xsl:variable name="matchlistcount" select="count($matchlist)"/>
	<xsl:choose>
	<xsl:when test="$matchlistcount != 0">
		<xsl:if test="$optionDescribeChecking='T'">
			<xsl:text>CheckContentItem: Matched: </xsl:text>
			<xsl:value-of select="$valueType"/>
			<xsl:text> </xsl:text>
			<xsl:choose>
			<xsl:when test="string-length($cvConceptName) &gt; 0">
				<xsl:call-template name="describeCodeWanted">
					<xsl:with-param name="cv"  select="$cvConceptName"/>
					<xsl:with-param name="csd" select="$csdConceptName"/>
					<xsl:with-param name="cm"  select="$cmConceptName"/>
				</xsl:call-template>
			</xsl:when>
			<xsl:when test="string-length($conceptNameCID) &gt; 0">
				<xsl:text>CID </xsl:text><xsl:value-of select="$conceptNameCID"/>
			</xsl:when>
			</xsl:choose>
			<xsl:text> with </xsl:text>
			<xsl:value-of select="$matchlistcount"/><xsl:text> content items</xsl:text><xsl:value-of select="$newline"/>
		</xsl:if>
			
		<!-- <xsl:variable name="testvmmin" select="$vmmin * $templatevmmin"/> -->
		<!-- <xsl:variable name="testvmmax" select="$vmmax * $templatevmmax"/> -->
		
		<xsl:variable name="testvmmin">
			<xsl:choose>
			<xsl:when test="string-length($templatevmmin) = 0"><xsl:value-of select="$vmmin"/></xsl:when>
			<xsl:otherwise><xsl:value-of select="$vmmin * $templatevmmin"/></xsl:otherwise>
			</xsl:choose>
		</xsl:variable>
		
		<xsl:variable name="testvmmax">
			<xsl:choose>
			<xsl:when test="string-length($templatevmmax) = 0"><xsl:value-of select="$vmmax"/></xsl:when>
			<xsl:when test="$templatevmmax = 'n' or $vmmax = 'n'">n</xsl:when>
			<xsl:otherwise><xsl:value-of select="$vmmax * $templatevmmax"/></xsl:otherwise>
			</xsl:choose>
		</xsl:variable>
		
		<xsl:if test="$optionDescribeChecking='T'">
		<xsl:text>CheckContentItem: testvmmin = </xsl:text><xsl:value-of select="$testvmmin"/><xsl:value-of select="$newline"/>
		<xsl:text>CheckContentItem: testvmmax = </xsl:text><xsl:value-of select="$testvmmax"/><xsl:value-of select="$newline"/>
		</xsl:if>

		<xsl:if test="$matchlistcount &lt; $vmmin or ($vmmax != 'n' and $templatevmmax != 'n' and $matchlistcount &gt; $vmmax)">
			<xsl:text>Error: </xsl:text><xsl:value-of select="$description"/><xsl:text>: within </xsl:text><xsl:value-of select="substring-after(@ID,'ci_')"/><xsl:text>: </xsl:text><xsl:call-template name="buildFullPathInInstanceToCurrentNode"/><xsl:text>: Incorrect content item value multiplicity - found </xsl:text>
			<xsl:value-of select="$matchlistcount"/>
			<xsl:text> content items but expected </xsl:text>
			<xsl:choose>
			<xsl:when test="$vmmin = $vmmax">
				<xsl:value-of select="$vmmax"/>
			</xsl:when>
			<xsl:otherwise>
				<xsl:value-of select="$vmmin"/>
				<xsl:text>-</xsl:text>
				<xsl:value-of select="$vmmax"/>
			</xsl:otherwise>
			</xsl:choose>
			<xsl:value-of select="$newline"/>
		</xsl:if>

		<xsl:if test="$templateRequiredType = 'MC' and $templateConditionSatisfied = 'F' and $templateMBPO = 'F'">
			<xsl:text>Error: </xsl:text><xsl:value-of select="$description"/><xsl:text>: within </xsl:text><xsl:value-of select="substring-after(@ID,'ci_')"/><xsl:text>: </xsl:text><xsl:call-template name="buildFullPathInInstanceToCurrentNode"/><xsl:text>: Content item in included template present when template inclusion condition not satisfied</xsl:text>
			<xsl:value-of select="$newline"/>
		</xsl:if>

		<xsl:choose>
		<xsl:when test="$requiredType ='MC' or $requiredType ='UC'">
			<xsl:if test="$optionDescribeChecking='T'">
				<xsl:text>CheckContentItem: For </xsl:text><xsl:value-of select="$requiredType"/><xsl:text> content item, conditionSatisfied is </xsl:text><xsl:value-of select="$conditionSatisfied"/>
				<xsl:text> and mbpo is </xsl:text><xsl:value-of select="$mbpo"/><xsl:value-of select="$newline"/>
			</xsl:if>
			<xsl:if test="$conditionSatisfied != 'T' and $mbpo != 'T'">
				<xsl:text>Error: </xsl:text><xsl:value-of select="$description"/><xsl:text>: within </xsl:text><xsl:value-of select="substring-after(@ID,'ci_')"/><xsl:text>: </xsl:text><xsl:call-template name="buildFullPathInInstanceToCurrentNode"/><xsl:text>: Conditional content item present when condition not satisfied</xsl:text>
				<xsl:value-of select="$newline"/>
			</xsl:if>
		</xsl:when>
		</xsl:choose>
		
		<xsl:for-each select="$matchlist">
			<xsl:variable name="cv" select="concept/@cv"/>
			<xsl:variable name="conceptCSDPreferred">
				<xsl:call-template name="getPreferredCodingSchemeDesignator">
					<xsl:with-param name="csd" select="concept/@csd"/>
				</xsl:call-template>
			</xsl:variable>
			<!--<xsl:message><xsl:text>Have matchlist entry (</xsl:text><xsl:value-of select="$cv"/><xsl:text>,</xsl:text><xsl:value-of select="concept/@csd"/><xsl:text>[</xsl:text><xsl:value-of select="$conceptCSDPreferred"/><xsl:text>],</xsl:text><xsl:value-of select="concept/@cm"/><xsl:text>)</xsl:text></xsl:message>-->
			<xsl:variable name="foundCodeMeaning">
				<xsl:call-template name="findCodeMeaningInContextGroup">
					<xsl:with-param name="cid" select="$conceptNameCID"/>
					<xsl:with-param name="cv" select="$cv"/>
					<xsl:with-param name="csd" select="$conceptCSDPreferred"/>
				</xsl:call-template>
			</xsl:variable>
			<!--<xsl:message><xsl:text>Iterating through matchlist, foundCodeMeaning for concept = </xsl:text><xsl:value-of select="$foundCodeMeaning"/></xsl:message>-->
			<xsl:if test="string-length($conceptNameCID) = 0 or string-length($foundCodeMeaning) &gt; 0">
				<!--<xsl:message><xsl:text>Doing something</xsl:text></xsl:message>-->
				
				<xsl:variable name="location" select="substring-after(@ID,'ci_')"/>
				<xsl:if test="$optionEmbedMatchedLocationsInOutputWithElement">
					<!-- Note that we are writing text but embedding an XML element -->
					<xsl:text>&lt;</xsl:text><xsl:value-of select="$optionEmbedMatchedLocationsInOutputWithElement"/>
					<xsl:text> location=&apos;</xsl:text><xsl:value-of select="$location"/><xsl:text>&apos;</xsl:text>
					<xsl:text> description=&apos;</xsl:text><xsl:value-of select="$description"/><xsl:text>&apos;</xsl:text>
					<xsl:text> row=&apos;</xsl:text><xsl:value-of select="$row"/><xsl:text>&apos;</xsl:text>
					<xsl:text>/&gt;</xsl:text>
					<xsl:value-of select="$newline"/>
				</xsl:if>
				<xsl:if test="$optionDescribeChecking='T'">
					<xsl:text>Iterating on match: </xsl:text>
					<xsl:value-of select="$location"/>
					<xsl:text>: </xsl:text>
					<xsl:value-of select="name(.)"/>
					<xsl:text> </xsl:text>
					<xsl:call-template name="describeCodeActual">
						<xsl:with-param name="cv"  select="concept/@cv"/>
						<xsl:with-param name="csd" select="concept/@csd"/>
						<xsl:with-param name="cm"  select="concept/@cm"/>
					</xsl:call-template>
					<xsl:value-of select="$newline"/>
				</xsl:if>
				
				<xsl:variable name="locationdescription"><xsl:value-of select="$description"/><xsl:text>: </xsl:text><xsl:value-of select="$location"/><xsl:text>: </xsl:text><xsl:call-template name="buildFullPathInInstanceToCurrentNode"/></xsl:variable>
				
				<xsl:if test="$optionCheckTemplateID='T' and $row = 1 and $valueType = 'container' and name() = 'container' and string-length(@template) &gt; 0">
					<!--<xsl:message><xsl:text>Checking for template ID </xsl:text><xsl:value-of select="$templateID"/><xsl:text> in first row container - have encoded template ID </xsl:text><xsl:value-of select="@template"/></xsl:message>-->
					<xsl:if test="$templateID != @template">
						<xsl:text>Warning: </xsl:text><xsl:value-of select="$locationdescription"/><xsl:text>: template ID encoded in first row container </xsl:text><xsl:value-of select="@template"/><xsl:text> does not match expected template ID </xsl:text><xsl:value-of select="$templateID"/><xsl:text> - may be ambiguous template invocation</xsl:text><xsl:value-of select="$newline"/>
					</xsl:if>
				</xsl:if>
				
				<xsl:variable name="conceptCSDPreferred">
					<xsl:call-template name="getPreferredCodingSchemeDesignator">
						<xsl:with-param name="csd" select="concept/@csd"/>
					</xsl:call-template>
				</xsl:variable>
				
				<xsl:if test="$optionDescribeChecking='T'">
					<xsl:text>CheckContentItem: Preferred coding scheme designator for concept name </xsl:text>
					<xsl:call-template name="describeCodeActual">
						<xsl:with-param name="cv"  select="concept/@cv"/>
						<xsl:with-param name="csd" select="concept/@csd"/>
						<xsl:with-param name="cm"  select="concept/@cm"/>
					</xsl:call-template>
					<xsl:text> is </xsl:text>
					<xsl:value-of select="$conceptCSDPreferred"/>
					<xsl:value-of select="$newline"/>
				</xsl:if>
				
				<xsl:if test="string-length(concept/@csd)&gt;0 and $conceptCSDPreferred != concept/@csd">
					<xsl:call-template name="codingSchemeDesignatorIsDeprecated">
						<xsl:with-param name="locationdescription" select="$locationdescription"/>
						<xsl:with-param name="cm"  select="concept/@cm"/>
						<xsl:with-param name="cv" select="concept/@cv"/>
						<xsl:with-param name="csdDeprecated" select="concept/@csd"/>
						<xsl:with-param name="csdPreferred" select="$conceptCSDPreferred"/>
					</xsl:call-template>
				</xsl:if>
				
				<xsl:if test="$cmConceptName">
					<xsl:if test="$optionDescribeChecking='T'">
						<xsl:text>CheckContentItem: Checking code meaning for concept name against single value for </xsl:text>
						<xsl:call-template name="describeCodeActual">
							<xsl:with-param name="cv"  select="concept/@cv"/>
							<xsl:with-param name="csd" select="$conceptCSDPreferred"/>
							<xsl:with-param name="cm"  select="concept/@cm"/>
						</xsl:call-template>
						<xsl:value-of select="$newline"/>
					</xsl:if>
					<xsl:variable name="checkingCodeMeaningAgainst"><xsl:text>template concept name</xsl:text></xsl:variable>
					<xsl:call-template name="checkCodeMeaning">
						<xsl:with-param name="locationdescription" select="$locationdescription"/>
						<xsl:with-param name="cv" select="concept/@cv"/>
						<xsl:with-param name="csd" select="$conceptCSDPreferred"/>
						<xsl:with-param name="cmEncountered" select="concept/@cm"/>
						<xsl:with-param name="cmWanted" select="$cmConceptName"/>
						<xsl:with-param name="checkingAgainst" select="$checkingCodeMeaningAgainst"/>
					</xsl:call-template>
				</xsl:if>
				
				<xsl:if test="$conceptNameCID">
					<xsl:if test="$optionDescribeChecking='T'">
						<xsl:text>CheckContentItem: Checking code meaning for concept name against context group </xsl:text><xsl:value-of select="$conceptNameCID"/><xsl:text> </xsl:text>
						<xsl:call-template name="describeCodeActual">
							<xsl:with-param name="cv"  select="concept/@cv"/>
							<xsl:with-param name="csd" select="$conceptCSDPreferred"/>
							<xsl:with-param name="cm"  select="concept/@cm"/>
						</xsl:call-template>
						<xsl:value-of select="$newline"/>
					</xsl:if>
					<xsl:variable name="checkingCodeMeaningAgainst"><xsl:text>template concept name context group</xsl:text></xsl:variable>
					<xsl:variable name="cmWanted">
						<xsl:call-template name="findCodeMeaningInContextGroup">
							<xsl:with-param name="cid" select="$conceptNameCID"/>
							<xsl:with-param name="cv" select="concept/@cv"/>
							<xsl:with-param name="csd" select="$conceptCSDPreferred"/>
						</xsl:call-template>
					</xsl:variable>
					<xsl:call-template name="checkCodeMeaning">
						<xsl:with-param name="locationdescription" select="$locationdescription"/>
						<xsl:with-param name="cv" select="concept/@cv"/>
						<xsl:with-param name="csd" select="$conceptCSDPreferred"/>
						<xsl:with-param name="cmEncountered" select="concept/@cm"/>
						<xsl:with-param name="cmWanted" select="$cmWanted"/>
						<xsl:with-param name="checkingAgainst" select="$checkingCodeMeaningAgainst"/>
					</xsl:call-template>
				</xsl:if>
				
				<xsl:if test="$relationship">
					<xsl:if test="$optionDescribeChecking='T'">
						<xsl:text>Checking for relationship </xsl:text><xsl:value-of select="$relationship"/><xsl:value-of select="$newline"/>
					</xsl:if>
					<xsl:if test="$relationship != @relationship">
						<xsl:text>Error: </xsl:text><xsl:value-of select="$locationdescription"/><xsl:text>: Incorrect relationship - expected </xsl:text><xsl:value-of select="$relationship"/><xsl:text> - found </xsl:text><xsl:value-of select="@relationship"/><xsl:value-of select="$newline"/>
					</xsl:if>
				</xsl:if>
				
				<xsl:variable name="unitsCSDPreferred">
					<xsl:call-template name="getPreferredCodingSchemeDesignator">
						<xsl:with-param name="csd" select="units/@csd"/>
					</xsl:call-template>
				</xsl:variable>
				
				<xsl:if test="$optionDescribeChecking='T'">
					<xsl:text>CheckContentItem: Preferred coding scheme designator for units </xsl:text>
					<xsl:call-template name="describeCodeActual">
						<xsl:with-param name="cv"  select="units/@cv"/>
						<xsl:with-param name="csd" select="units/@csd"/>
						<xsl:with-param name="cm"  select="units/@cm"/>
					</xsl:call-template>
					<xsl:text> is </xsl:text>
					<xsl:value-of select="$unitsCSDPreferred"/>
					<xsl:value-of select="$newline"/>
				</xsl:if>
				
				<xsl:if test="string-length(units/@csd)&gt;0 and $unitsCSDPreferred != units/@csd">
					<xsl:call-template name="codingSchemeDesignatorIsDeprecated">
						<xsl:with-param name="locationdescription" select="$locationdescription"/>
						<xsl:with-param name="cm"  select="units/@cm"/>
						<xsl:with-param name="cv" select="units/@cv"/>
						<xsl:with-param name="csdDeprecated" select="units/@csd"/>
						<xsl:with-param name="csdPreferred" select="$unitsCSDPreferred"/>
					</xsl:call-template>
				</xsl:if>
				
				<xsl:if test="$cvUnits">
					<xsl:if test="$optionDescribeChecking='T'">
						<xsl:text>Checking for units </xsl:text>
						<xsl:call-template name="describeCodeWanted">
							<xsl:with-param name="cv" select="$cvUnits"/>
							<xsl:with-param name="csd" select="$csdUnits"/>
							<xsl:with-param name="cm" select="$cmUnits"/>
						</xsl:call-template>
						<xsl:text> against </xsl:text>
						<xsl:call-template name="describeCodeActual">
							<xsl:with-param name="cv" select="units/@cv"/>
							<xsl:with-param name="csd" select="$unitsCSDPreferred"/>
							<xsl:with-param name="cm" select="units/@cm"/>
						</xsl:call-template>
						<xsl:value-of select="$newline"/>
					</xsl:if>
					<xsl:if test="not($cvUnits = units/@cv and $csdUnits = units/@csd)">
						<xsl:text>Error: </xsl:text><xsl:value-of select="$locationdescription"/><xsl:text>: Incorrect units - expected </xsl:text>
						<xsl:call-template name="describeCodeWanted">
							<xsl:with-param name="cv" select="$cvUnits"/>
							<xsl:with-param name="csd" select="$csdUnits"/>
							<xsl:with-param name="cm" select="$cmUnits"/>
						</xsl:call-template>
						<xsl:text> - found </xsl:text>
						<xsl:call-template name="describeCodeActual">
							<xsl:with-param name="cv" select="units/@cv"/>
							<xsl:with-param name="csd" select="$unitsCSDPreferred"/>
							<xsl:with-param name="cm" select="units/@cm"/>
						</xsl:call-template>
						<xsl:value-of select="$newline"/>
					</xsl:if>
				</xsl:if>
				
				<xsl:if test="$unitsCID">
					<xsl:call-template name="checkCodeIsRecognized">
						<xsl:with-param name="locationdescription" select="$locationdescription"/>
						<xsl:with-param name="cm" select="units/@cm"/>
						<xsl:with-param name="csd" select="units/@csd"/>	<!-- this template handles the preferred CSD itself -->
						<xsl:with-param name="cv" select="units/@cv"/>
						<xsl:with-param name="cid" select="$unitsCID"/>
						<xsl:with-param name="bde" select="$unitsBDE"/>
					</xsl:call-template>
				</xsl:if>
				
				<xsl:variable name="valueCSDPreferred">
					<xsl:call-template name="getPreferredCodingSchemeDesignator">
						<xsl:with-param name="csd" select="value/@csd"/>
					</xsl:call-template>
				</xsl:variable>
				
				<xsl:if test="$optionDescribeChecking='T'">
					<xsl:text>CheckContentItem: Preferred coding scheme designator for value </xsl:text>
					<xsl:call-template name="describeCodeActual">
						<xsl:with-param name="cv"  select="value/@cv"/>
						<xsl:with-param name="csd" select="value/@csd"/>
						<xsl:with-param name="cm"  select="value/@cm"/>
					</xsl:call-template>
					<xsl:text> is </xsl:text>
					<xsl:value-of select="$valueCSDPreferred"/>
					<xsl:value-of select="$newline"/>
				</xsl:if>
				
				<xsl:if test="string-length(value/@csd)&gt;0 and $valueCSDPreferred != value/@csd">
					<xsl:call-template name="codingSchemeDesignatorIsDeprecated">
						<xsl:with-param name="locationdescription" select="$locationdescription"/>
						<xsl:with-param name="cm"  select="value/@cm"/>
						<xsl:with-param name="cv" select="value/@cv"/>
						<xsl:with-param name="csdDeprecated" select="value/@csd"/>
						<xsl:with-param name="csdPreferred" select="$valueCSDPreferred"/>
					</xsl:call-template>
				</xsl:if>
				
				<xsl:if test="$cvValue">
					<xsl:if test="$optionDescribeChecking='T'">
						<xsl:text>Checking for value </xsl:text>
						<xsl:call-template name="describeCodeWanted">
							<xsl:with-param name="cv" select="$cvValue"/>
							<xsl:with-param name="csd" select="$csdValue"/>
							<xsl:with-param name="cm" select="$cmValue"/>
						</xsl:call-template>
						<xsl:text> against </xsl:text>
						<xsl:call-template name="describeCodeActual">
							<xsl:with-param name="cv" select="value/@cv"/>
							<xsl:with-param name="csd" select="$valueCSDPreferred"/>
							<xsl:with-param name="cm" select="value/@cm"/>
						</xsl:call-template>
						<xsl:value-of select="$newline"/>
					</xsl:if>
					<xsl:if test="not($cvValue = value/@cv and $csdValue = value/@csd)">
						<xsl:text>Error: </xsl:text><xsl:value-of select="$locationdescription"/><xsl:text>: Incorrect value - expected </xsl:text>
						<xsl:call-template name="describeCodeWanted">
							<xsl:with-param name="cv" select="$cvValue"/>
							<xsl:with-param name="csd" select="$csdValue"/>
							<xsl:with-param name="cm" select="$cmValue"/>
						</xsl:call-template>
						<xsl:text> - found </xsl:text>
						<xsl:call-template name="describeCodeActual">
							<xsl:with-param name="cv" select="value/@cv"/>
							<xsl:with-param name="csd" select="$valueCSDPreferred"/>
							<xsl:with-param name="cm" select="value/@cm"/>
						</xsl:call-template>
						<xsl:value-of select="$newline"/>
					</xsl:if>
				</xsl:if>
				
				<xsl:if test="$valueSetCID">
					<xsl:call-template name="checkCodeIsRecognized">
						<xsl:with-param name="locationdescription" select="$locationdescription"/>
						<xsl:with-param name="cm" select="value/@cm"/>
						<xsl:with-param name="csd" select="value/@csd"/>	<!-- this template handles the preferred CSD itself -->
						<xsl:with-param name="cv" select="value/@cv"/>
						<xsl:with-param name="cid" select="$valueSetCID"/>
						<xsl:with-param name="bde" select="$valueSetBDE"/>
					</xsl:call-template>
				</xsl:if>
				
				<xsl:if test="$valueType = 'num' or $valueType = 'text' or $valueType = 'date' or $valueType = 'time' or $valueType = 'datetime' or $valueType = 'pname' or $valueType = 'uidref'">
					<xsl:if test="$optionDescribeChecking='T'">
						<xsl:text>Checking for presence of non-empty value for valueType = </xsl:text><xsl:value-of select="$valueType"/><xsl:value-of select="$newline"/>
					</xsl:if>
					<xsl:if test="string-length(normalize-space(value)) = '0'">
						<xsl:text>Error: </xsl:text><xsl:value-of select="$locationdescription"/><xsl:text>: Missing value</xsl:text><xsl:value-of select="$newline"/>
					</xsl:if>
				</xsl:if>
				
				<xsl:if test="$graphicType">
					<xsl:if test="$optionDescribeChecking='T'">
						<xsl:text>Checking for graphicType = </xsl:text><xsl:value-of select="$graphicType"/><xsl:value-of select="$newline"/>
					</xsl:if>
					<xsl:choose>
						<xsl:when test="$graphicType = 'POINT'">
							<xsl:if test="count(point) = 0">
								<xsl:text>Error: </xsl:text><xsl:value-of select="$locationdescription"/><xsl:text>: Incorrect graphic type - expected </xsl:text><xsl:value-of select="$graphicType"/><xsl:value-of select="$newline"/>
							</xsl:if>
						</xsl:when>
						<xsl:when test="$graphicType = 'MULTIPOINT'">
							<xsl:if test="count(multipoint) = 0">
								<xsl:text>Error: </xsl:text><xsl:value-of select="$locationdescription"/><xsl:text>: Incorrect graphic type - expected </xsl:text><xsl:value-of select="$graphicType"/><xsl:value-of select="$newline"/>
							</xsl:if>
						</xsl:when>
						<xsl:when test="$graphicType = 'POLYLINE'">
							<xsl:if test="count(polyline) = 0">
								<xsl:text>Error: </xsl:text><xsl:value-of select="$locationdescription"/><xsl:text>: Incorrect graphic type - expected </xsl:text><xsl:value-of select="$graphicType"/><xsl:value-of select="$newline"/>
							</xsl:if>
						</xsl:when>
						<xsl:when test="$graphicType = 'CIRCLE'">
							<xsl:if test="count(circle) = 0">
								<xsl:text>Error: </xsl:text><xsl:value-of select="$locationdescription"/><xsl:text>: Incorrect graphic type - expected </xsl:text><xsl:value-of select="$graphicType"/><xsl:value-of select="$newline"/>
							</xsl:if>
						</xsl:when>
						<xsl:when test="$graphicType = 'ELLIPSE'">
							<xsl:if test="count(ellipse) = 0">
								<xsl:text>Error: </xsl:text><xsl:value-of select="$locationdescription"/><xsl:text>: Incorrect graphic type - expected </xsl:text><xsl:value-of select="$graphicType"/><xsl:value-of select="$newline"/>
							</xsl:if>
						</xsl:when>
						<xsl:when test="$graphicType = 'ELLIPSE_OR_POLYLINE'">
							<xsl:if test="count(ellipse) = 0 and count(polyline) = 0">
								<xsl:text>Error: </xsl:text><xsl:value-of select="$locationdescription"/><xsl:text>: Incorrect graphic type - expected ELLIPSE or POLYLINE</xsl:text><xsl:value-of select="$newline"/>
							</xsl:if>
						</xsl:when>
						<xsl:when test="$graphicType = 'POLYLINE_CIRCLE_ELLIPSE'">
							<xsl:if test="count(ellipse) = 0 and count(circle) = 0 and count(polyline) = 0">
								<xsl:text>Error: </xsl:text><xsl:value-of select="$locationdescription"/><xsl:text>: Incorrect graphic type - expected ELLIPSE, CIRCLE or POLYLINE</xsl:text><xsl:value-of select="$newline"/>
							</xsl:if>
						</xsl:when>
						<xsl:when test="$graphicType = 'NOT_MULTIPOINT'">
							<xsl:if test="count(multipoint)&gt;0">
								<xsl:text>Error: </xsl:text><xsl:value-of select="$locationdescription"/><xsl:text>: Incorrect graphic type - expected not MULIPOINT</xsl:text><xsl:value-of select="$newline"/>
							</xsl:if>
						</xsl:when>
						<xsl:otherwise>
						</xsl:otherwise>
					</xsl:choose>
				</xsl:if>
				
				<xsl:variable name="countX" select="count(*/x)"/>
				<xsl:variable name="countY" select="count(*/y)"/>
				
				<xsl:if test="count(ellipse) &gt; 0">
					<xsl:if test="$optionDescribeChecking='T'">
						<xsl:text>Checking ellipse countX = </xsl:text><xsl:value-of select="$countX"/><xsl:text> and countY = </xsl:text><xsl:value-of select="$countY"/><xsl:value-of select="$newline"/>
					</xsl:if>
					<xsl:if test="$countX != 4">
						<xsl:text>Error: </xsl:text><xsl:value-of select="$locationdescription"/>
						<xsl:text>: number of coordinates incorrect for ELLIPSE - got </xsl:text><xsl:value-of select="$countX"/>
						<xsl:text> - expected 4</xsl:text>
						<xsl:value-of select="$newline"/>
					</xsl:if>
				</xsl:if>
				
				<xsl:if test="count(circle) &gt; 0">
					<xsl:if test="$optionDescribeChecking='T'">
						<xsl:text>Checking ellipse countX = </xsl:text><xsl:value-of select="$countX"/><xsl:text> and countY = </xsl:text><xsl:value-of select="$countY"/><xsl:value-of select="$newline"/>
					</xsl:if>
					<xsl:if test="$countX != 2">
						<xsl:text>Error: </xsl:text><xsl:value-of select="$locationdescription"/>
						<xsl:text>: number of coordinates incorrect for CIRCLE - got </xsl:text><xsl:value-of select="$countX"/>
						<xsl:text> - expected 2</xsl:text>
						<xsl:value-of select="$newline"/>
					</xsl:if>
				</xsl:if>
				
				<xsl:if test="count(point) &gt; 0">
					<xsl:if test="$optionDescribeChecking='T'">
						<xsl:text>Checking point countX = </xsl:text><xsl:value-of select="$countX"/><xsl:text> and countY = </xsl:text><xsl:value-of select="$countY"/><xsl:value-of select="$newline"/>
					</xsl:if>
					<xsl:if test="$countX != 1">
						<xsl:text>Error: </xsl:text><xsl:value-of select="$locationdescription"/>
						<xsl:text>: number of coordinates incorrect for POINT - got </xsl:text><xsl:value-of select="$countX"/>
						<xsl:text> - expected 1</xsl:text>
						<xsl:value-of select="$newline"/>
					</xsl:if>
				</xsl:if>
				
				<xsl:if test="$numpointsmin">
					<xsl:if test="$optionDescribeChecking='T'">
						<xsl:text>Checking for numpointsmin = </xsl:text><xsl:value-of select="$numpointsmin"/><xsl:text> and numpointsmax = </xsl:text><xsl:value-of select="$numpointsmax"/><xsl:value-of select="$newline"/>
					</xsl:if>
					<xsl:choose>
						<xsl:when test="$countX != $countY">
							<xsl:text>Error: </xsl:text><xsl:value-of select="$locationdescription"/><xsl:text>: number of X coordinates (</xsl:text><xsl:value-of select="$countX"/><xsl:text>) does not match number of Y coordinates (</xsl:text><xsl:value-of select="$countY"/><xsl:text>)</xsl:text><xsl:value-of select="$newline"/>
						</xsl:when>
						<xsl:otherwise>
							<xsl:if test="$countX &lt; $numpointsmin or ($numpointsmax != 'n' and $countX &gt; $numpointsmax)">
								<xsl:text>Error: </xsl:text><xsl:value-of select="$locationdescription"/>
								<xsl:text>: number of coordinates incorrect - got </xsl:text><xsl:value-of select="$countX"/>
								<xsl:text> - expected </xsl:text>
								<xsl:choose>
									<xsl:when test="$numpointsmin = $numpointsmax">
										<xsl:value-of select="$numpointsmax"/>
									</xsl:when>
									<xsl:otherwise>
										<xsl:value-of select="$numpointsmin"/>
										<xsl:text>-</xsl:text>
										<xsl:value-of select="$numpointsmax"/>
									</xsl:otherwise>
								</xsl:choose>
								<xsl:value-of select="$newline"/>
							</xsl:if>
						</xsl:otherwise>
					</xsl:choose>
				</xsl:if>
				
				
			</xsl:if>
		</xsl:for-each>

	</xsl:when>
	<xsl:otherwise>
		<xsl:if test="$optionDescribeChecking='T'">
			<xsl:text>CheckContentItem: Did not match: </xsl:text>
			<xsl:value-of select="$valueType"/>
			<xsl:text> </xsl:text>
			<xsl:choose>
			<xsl:when test="string-length($cvConceptName) &gt; 0">
				<xsl:call-template name="describeCodeWanted">
					<xsl:with-param name="cv"  select="$cvConceptName"/>
					<xsl:with-param name="csd" select="$csdConceptName"/>
					<xsl:with-param name="cm"  select="$cmConceptName"/>
				</xsl:call-template>
			</xsl:when>
			<xsl:when test="string-length($conceptNameCID) &gt; 0">
				<xsl:text>CID </xsl:text><xsl:value-of select="$conceptNameCID"/>
			</xsl:when>
			</xsl:choose>
			<xsl:value-of select="$newline"/>
		</xsl:if>
		<xsl:choose>
		<xsl:when test="$requiredType ='M' and ($nestingLevel &gt; 1 or $templateRequiredType = 'M' or ($templateRequiredType = 'MC' and $templateConditionSatisfied = 'T'))">
			<xsl:text>Error: </xsl:text><xsl:value-of select="$description"/><xsl:text>: within </xsl:text><xsl:value-of select="substring-after(@ID,'ci_')"/><xsl:text>: </xsl:text><xsl:call-template name="buildFullPathInInstanceToCurrentNode"/><xsl:text>: Missing required content item</xsl:text>
			<xsl:value-of select="$newline"/>
		</xsl:when>
		<xsl:when test="$requiredType ='MC' and ($nestingLevel &gt; 1 or $templateRequiredType = 'M' or ($templateRequiredType = 'MC' and $templateConditionSatisfied = 'T'))">
			<!-- <xsl:text>conditionSatisfied is </xsl:text><xsl:value-of select="$conditionSatisfied"/><xsl:value-of select="$newline"/> -->
			<xsl:if test="$conditionSatisfied = 'T'">
				<xsl:text>Error: </xsl:text><xsl:value-of select="$description"/><xsl:text>: within </xsl:text><xsl:value-of select="substring-after(@ID,'ci_')"/><xsl:text>: </xsl:text><xsl:call-template name="buildFullPathInInstanceToCurrentNode"/><xsl:text>: Missing conditional content item</xsl:text>
				<xsl:value-of select="$newline"/>
			</xsl:if>
		</xsl:when>
		</xsl:choose>
	</xsl:otherwise>
	</xsl:choose>

</xsl:template>

<xsl:template name="getPreferredCodingSchemeDesignator">
	<xsl:param name="csd"/>

	<xsl:choose>
	<xsl:when test="$csd = 'SNM3'">
		<xsl:text>SRT</xsl:text>
	</xsl:when>
	<xsl:when test="$csd = '99SDM'">
		<xsl:text>SRT</xsl:text>
	</xsl:when>
	<xsl:otherwise>
		<xsl:value-of select="$csd"/>
	</xsl:otherwise>
	</xsl:choose>

</xsl:template>

<xsl:template name="checkCodeIsRecognized">
	<xsl:param name="locationdescription"/>
	<xsl:param name="cm"/>
	<xsl:param name="csd"/>
	<xsl:param name="cv"/>
	<xsl:param name="cid"/>
	<xsl:param name="bde"/>

	<xsl:choose>
	<xsl:when test="$csd = 'SNM3'">
		<xsl:variable name="csdPreferred">SRT</xsl:variable>
		<xsl:call-template name="codingSchemeDesignatorIsDeprecated">
			<xsl:with-param name="locationdescription" select="$locationdescription"/>
			<xsl:with-param name="cm" select="$cm"/>
			<xsl:with-param name="cv" select="$cv"/>
			<xsl:with-param name="csdDeprecated" select="$csd"/>
			<xsl:with-param name="csdPreferred" select="$csdPreferred"/>
		</xsl:call-template>
		<xsl:call-template name="checkCodeIsInContextGroup">
			<xsl:with-param name="locationdescription" select="$locationdescription"/>
			<xsl:with-param name="cm" select="$cm"/>
			<xsl:with-param name="csd" select="$csdPreferred"/>
			<xsl:with-param name="cv" select="$cv"/>
			<xsl:with-param name="cid" select="$cid"/>
			<xsl:with-param name="bde" select="$bde"/>
		</xsl:call-template>
	</xsl:when>
	<xsl:when test="$csd = '99SDM'">
		<xsl:variable name="csdPreferred">SRT</xsl:variable>
		<xsl:call-template name="codingSchemeDesignatorIsDeprecated">
			<xsl:with-param name="locationdescription" select="$locationdescription"/>
			<xsl:with-param name="cm" select="$cm"/>
			<xsl:with-param name="cv" select="$cv"/>
			<xsl:with-param name="csdDeprecated" select="$csd"/>
			<xsl:with-param name="csdPreferred" select="$csdPreferred"/>
		</xsl:call-template>
		<xsl:call-template name="checkCodeIsInContextGroup">
			<xsl:with-param name="locationdescription" select="$locationdescription"/>
			<xsl:with-param name="cm" select="$cm"/>
			<xsl:with-param name="csd" select="$csdPreferred"/>
			<xsl:with-param name="cv" select="$cv"/>
			<xsl:with-param name="cid" select="$cid"/>
			<xsl:with-param name="bde" select="$bde"/>
		</xsl:call-template>
	</xsl:when>
	<xsl:when test="$csd = 'ISO639_1'">
		<xsl:variable name="csdPreferred">RFC3066</xsl:variable>
		<xsl:call-template name="codingSchemeDesignatorIsDeprecated">
			<xsl:with-param name="locationdescription" select="$locationdescription"/>
			<xsl:with-param name="cm" select="$cm"/>
			<xsl:with-param name="cv" select="$cv"/>
			<xsl:with-param name="csdDeprecated" select="$csd"/>
			<xsl:with-param name="csdPreferred" select="$csdPreferred"/>
		</xsl:call-template>
		<xsl:call-template name="checkCodeIsInContextGroup">
			<xsl:with-param name="locationdescription" select="$locationdescription"/>
			<xsl:with-param name="cm" select="$cm"/>
			<xsl:with-param name="csd" select="$csdPreferred"/>
			<xsl:with-param name="cv" select="$cv"/>
			<xsl:with-param name="cid" select="$cid"/>
			<xsl:with-param name="bde" select="$bde"/>
		</xsl:call-template>
	</xsl:when>
	<xsl:when test="$csd = 'ISO639_2'">
		<xsl:variable name="csdPreferred">RFC3066</xsl:variable>
		<xsl:call-template name="codingSchemeDesignatorIsDeprecated">
			<xsl:with-param name="locationdescription" select="$locationdescription"/>
			<xsl:with-param name="cm" select="$cm"/>
			<xsl:with-param name="cv" select="$cv"/>
			<xsl:with-param name="csdDeprecated" select="$csd"/>
			<xsl:with-param name="csdPreferred" select="$csdPreferred"/>
		</xsl:call-template>
		<xsl:call-template name="checkCodeIsInContextGroup">
			<xsl:with-param name="locationdescription" select="$locationdescription"/>
			<xsl:with-param name="cm" select="$cm"/>
			<xsl:with-param name="csd" select="$csdPreferred"/>
			<xsl:with-param name="cv" select="$cv"/>
			<xsl:with-param name="cid" select="$cid"/>
			<xsl:with-param name="bde" select="$bde"/>
		</xsl:call-template>
	</xsl:when>
	<xsl:otherwise>
		<xsl:call-template name="checkCodeIsInContextGroup">
			<xsl:with-param name="locationdescription" select="$locationdescription"/>
			<xsl:with-param name="cm" select="$cm"/>
			<xsl:with-param name="csd" select="$csd"/>
			<xsl:with-param name="cv" select="$cv"/>
			<xsl:with-param name="cid" select="$cid"/>
			<xsl:with-param name="bde" select="$bde"/>
		</xsl:call-template>
	</xsl:otherwise>
	</xsl:choose>

</xsl:template>

<xsl:template name="codingSchemeDesignatorIsDeprecated">
	<xsl:param name="locationdescription"/>
	<xsl:param name="cm"/>
	<xsl:param name="cv"/>
	<xsl:param name="csdDeprecated"/>
	<xsl:param name="csdPreferred"/>
		<xsl:text>Warning: </xsl:text><xsl:value-of select="$locationdescription"/><xsl:text>: Code </xsl:text>
		<xsl:call-template name="describeCodeActual">
			<xsl:with-param name="cv" select="$cv"/>
			<xsl:with-param name="csd" select="$csdDeprecated"/>
			<xsl:with-param name="cm" select="$cm"/>
		</xsl:call-template>
		<xsl:text> uses deprecated Coding Scheme Designator </xsl:text><xsl:value-of select="$csdDeprecated"/>
		<xsl:text> - use </xsl:text><xsl:value-of select="$csdPreferred"/><xsl:text> instead</xsl:text>
		<xsl:value-of select="$newline"/>
</xsl:template>

<xsl:template name="findCodeMeaningInContextGroup">
	<xsl:param name="cid"/>
	<xsl:param name="cv"/>
	<xsl:param name="csd"/>
	<xsl:value-of select="document('DicomContextGroupsSource.xml')/definecontextgroups/definecontextgroup[@cid=$cid]/contextgroupcode[@cv=$cv and @csd=$csd]/@cm"/>
</xsl:template>

<xsl:template name="checkCodeIsInContextGroup">
	<xsl:param name="locationdescription"/>
	<xsl:param name="cm"/>
	<xsl:param name="csd"/>
	<xsl:param name="cv"/>
	<xsl:param name="cid"/>
	<xsl:param name="bde"/>
	
	<xsl:if test="$optionDescribeChecking='T'">
		<xsl:text>Checking code </xsl:text>
		<xsl:call-template name="describeCodeActual">
			<xsl:with-param name="cv" select="$cv"/>
			<xsl:with-param name="csd" select="$csd"/>
			<xsl:with-param name="cm" select="$cm"/>
		</xsl:call-template>
		<xsl:text> is in context group </xsl:text><xsl:value-of select="$cid"/><xsl:value-of select="$newline"/>
	</xsl:if>

	<xsl:variable name="foundCodeMeaning">
		<xsl:call-template name="findCodeMeaningInContextGroup">
			<xsl:with-param name="cid" select="$cid"/>
			<xsl:with-param name="cv" select="$cv"/>
			<xsl:with-param name="csd" select="$csd"/>
		</xsl:call-template>
	</xsl:variable>
	
	<xsl:choose>
	<xsl:when test="string-length($foundCodeMeaning) = 0">
		<xsl:text>Error: </xsl:text><xsl:value-of select="$locationdescription"/><xsl:text>: Code </xsl:text>
		<xsl:call-template name="describeCodeActual">
			<xsl:with-param name="cv" select="$cv"/>
			<xsl:with-param name="csd" select="$csd"/>
			<xsl:with-param name="cm" select="$cm"/>
		</xsl:call-template>
		<xsl:text> not found in context group </xsl:text><xsl:value-of select="$cid"/>
		<xsl:value-of select="$newline"/>
	</xsl:when>
	<xsl:otherwise>
		<xsl:if test="$optionDescribeChecking='T'">
			<xsl:text>Found code </xsl:text>
			<xsl:call-template name="describeCodeActual">
				<xsl:with-param name="cv" select="$cv"/>
				<xsl:with-param name="csd" select="$csd"/>
				<xsl:with-param name="cm" select="$foundCodeMeaning"/>
			</xsl:call-template>
			<xsl:text> in context group </xsl:text><xsl:value-of select="$cid"/><xsl:value-of select="$newline"/>
		</xsl:if>
		<xsl:variable name="checkingAgainst"><xsl:text>context group </xsl:text><xsl:value-of select="$cid"/></xsl:variable>
		<xsl:call-template name="checkCodeMeaning">
			<xsl:with-param name="locationdescription" select="$locationdescription"/>
			<xsl:with-param name="cv" select="$cv"/>
			<xsl:with-param name="csd" select="$csd"/>
			<xsl:with-param name="cmEncountered" select="$cm"/>
			<xsl:with-param name="cmWanted" select="$foundCodeMeaning"/>
			<xsl:with-param name="checkingAgainst" select="$checkingAgainst"/>
		</xsl:call-template>
	</xsl:otherwise>
	</xsl:choose>
</xsl:template>

<xsl:template name="checkCodeMeaning">
	<xsl:param name="locationdescription"/>
	<xsl:param name="cv"/>
	<xsl:param name="csd"/>
	<xsl:param name="cmEncountered"/>
	<xsl:param name="cmWanted"/>
	<xsl:param name="checkingAgainst"/>

	<xsl:variable name="escapedQuote">#</xsl:variable>
	<xsl:variable name="actualQuote">'</xsl:variable>
	<xsl:variable name="cmWantedQuoteSubstituted"><xsl:value-of select="translate($cmWanted,$escapedQuote,$actualQuote)"/></xsl:variable>       <!-- recover escaped quotes -->

	<xsl:if test="($optionMatchCaseOfCodeMeaning='T' and $cmEncountered != $cmWantedQuoteSubstituted)
			   or ($optionMatchCaseOfCodeMeaning='F' and translate($cmEncountered,$lowercase,$uppercase) != translate($cmWantedQuoteSubstituted,$lowercase,$uppercase))">
		<xsl:text>Warning: </xsl:text><xsl:value-of select="$locationdescription"/><xsl:text>: Code </xsl:text>
		<xsl:call-template name="describeCodeActual">
			<xsl:with-param name="cv" select="$cv"/>
			<xsl:with-param name="csd" select="$csd"/>
			<xsl:with-param name="cm" select="$cmEncountered"/>
		</xsl:call-template>
		<xsl:text> has different code meaning ("</xsl:text><xsl:value-of select="$cmEncountered"/><xsl:text>")</xsl:text>
		<xsl:text> than code meaning in </xsl:text><xsl:value-of select="$checkingAgainst"/><xsl:text> ("</xsl:text><xsl:value-of select="$cmWantedQuoteSubstituted"/><xsl:text>")</xsl:text>
		<xsl:value-of select="$newline"/>
	</xsl:if>
</xsl:template>

<xsl:key name="idkey" match="text" use="@ID"/>
<xsl:key name="idkey" match="num" use="@ID"/>
<xsl:key name="idkey" match="code" use="@ID"/>
<xsl:key name="idkey" match="datetime" use="@ID"/>
<xsl:key name="idkey" match="date" use="@ID"/>
<xsl:key name="idkey" match="time" use="@ID"/>
<xsl:key name="idkey" match="uidref" use="@ID"/>
<xsl:key name="idkey" match="pname" use="@ID"/>
<xsl:key name="idkey" match="composite" use="@ID"/>
<xsl:key name="idkey" match="image" use="@ID"/>
<xsl:key name="idkey" match="waveform" use="@ID"/>
<xsl:key name="idkey" match="scoord" use="@ID"/>
<xsl:key name="idkey" match="tcoord" use="@ID"/>
<xsl:key name="idkey" match="container" use="@ID"/>

</xsl:stylesheet>
