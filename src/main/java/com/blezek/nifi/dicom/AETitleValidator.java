package com.blezek.nifi.dicom;

import org.apache.nifi.components.ValidationContext;
import org.apache.nifi.components.ValidationResult;
import org.apache.nifi.components.Validator;
import org.apache.nifi.expression.AttributeExpression.ResultType;

import java.util.regex.Pattern;

public class AETitleValidator implements Validator {

    @Override
    public ValidationResult validate(String subject, String value, ValidationContext context) {
	if (context.isExpressionLanguageSupported(subject) && context.isExpressionLanguagePresent(value)) {
	    final ResultType resultType = context.newExpressionLanguageCompiler().getResultType(value);
	    if (!resultType.equals(ResultType.STRING)) {
		return new ValidationResult.Builder().subject(subject).input(value).valid(false)
			.explanation("Expected Attribute Query to return type " + ResultType.STRING
				+ " but query returns type " + resultType)
			.build();
	    }
	}
	if (!value.equals("*") && (value.length() > 16 || !Pattern.matches("[a-zA-z0-9_-]*", value))) {
	    return new ValidationResult.Builder().subject(subject).input(value)
		    .explanation("AE Title must be blank or up to 16 charagters long, containing only [a-zA-Z0-9_-]")
		    .valid(false).build();
	}
	return new ValidationResult.Builder().subject(subject).input(value).explanation(null).valid(true).build();
    }
}
