#contains
[condition][]Message body contains "{str}".=m: Message (eval(m.getProperty(RulesValidator.RULES_ORIGINAL_MESSAGE) == null && ValidatorUtil.validateMessage(m, MessagePart.BODY, null, Operator.CONTAINS, "{str}")))
[condition][]Message header "{name}" contains "{str}".=m: Message (eval(m.getProperty(RulesValidator.RULES_ORIGINAL_MESSAGE) == null && ValidatorUtil.validateMessage(m, MessagePart.HEADER, "{name}", Operator.CONTAINS, "{str}")))
[condition][]Message property "{name}" contains "{str}".=m: Message (eval(m.getProperty(RulesValidator.RULES_ORIGINAL_MESSAGE) == null && ValidatorUtil.validateMessage(m, MessagePart.PROPERTY, "{name}", Operator.CONTAINS, "{str}")))

#equals
[condition][]Message body equals "{str}".=m: Message (eval(m.getProperty(RulesValidator.RULES_ORIGINAL_MESSAGE) == null && ValidatorUtil.validateMessage(m, MessagePart.BODY, null, Operator.EQUALS, "{str}")))
[condition][]Message header "{name}" equals "{str}".=m: Message (eval(m.getProperty(RulesValidator.RULES_ORIGINAL_MESSAGE) == null && ValidatorUtil.validateMessage(m, MessagePart.HEADER, "{name}", Operator.EQUALS, "{str}")))
[condition][]Message property "{name}" equals "{str}".=m: Message (eval(m.getProperty(RulesValidator.RULES_ORIGINAL_MESSAGE) == null && ValidatorUtil.validateMessage(m, MessagePart.PROPERTY, "{name}", Operator.EQUALS, "{str}")))

#matches
[condition][]Message body matches "{str}".=m: Message (eval(m.getProperty(RulesValidator.RULES_ORIGINAL_MESSAGE) == null && ValidatorUtil.validateMessage(m, MessagePart.BODY, null, Operator.MATCHES, "{str}")))
[condition][]Message header "{name}" matches "{str}".=m: Message (eval(m.getProperty(RulesValidator.RULES_ORIGINAL_MESSAGE) == null && ValidatorUtil.validateMessage(m, MessagePart.HEADER, "{name}", Operator.MATCHES, "{str}")))
[condition][]Message property "{name}" matches "{str}".=m: Message (eval(m.getProperty(RulesValidator.RULES_ORIGINAL_MESSAGE) == null && ValidatorUtil.validateMessage(m, MessagePart.PROPERTY, "{name}", Operator.MATCHES, "{str}")))

#startsWith
[condition][]Message body starts with "{str}".=m: Message (eval(m.getProperty(RulesValidator.RULES_ORIGINAL_MESSAGE) == null && ValidatorUtil.validateMessage(m, MessagePart.BODY, null, Operator.STARTS_WITH, "{str}")))
[condition][]Message header "{name}" starts with "{str}".=m: Message (eval(m.getProperty(RulesValidator.RULES_ORIGINAL_MESSAGE) == null && ValidatorUtil.validateMessage(m, MessagePart.HEADER, "{name}", Operator.STARTS_WITH, "{str}")))
[condition][]Message property "{name}" starts with "{str}".=m: Message (eval(m.getProperty(RulesValidator.RULES_ORIGINAL_MESSAGE) == null && ValidatorUtil.validateMessage(m, MessagePart.PROPERTY, "{name}", Operator.STARTS_WITH, "{str}")))

#endWith
[condition][]Message body ends with "{str}".=m: Message (eval(m.getProperty(RulesValidator.RULES_ORIGINAL_MESSAGE) == null && ValidatorUtil.validateMessage(m, MessagePart.BODY, null, Operator.ENDS_WITH, "{str}")))
[condition][]Message header "{name}" ends with "{str}".=m: Message (eval(m.getProperty(RulesValidator.RULES_ORIGINAL_MESSAGE) == null && ValidatorUtil.validateMessage(m, MessagePart.HEADER, "{name}", Operator.ENDS_WITH, "{str}")))
[condition][]Message property "{name}" ends with "{str}".=m: Message (eval(m.getProperty(RulesValidator.RULES_ORIGINAL_MESSAGE) == null && ValidatorUtil.validateMessage(m, MessagePart.PROPERTY, "{name}", Operator.ENDS_WITH, "{str}")))

#exists
[condition][]Message body exists.=m: Message (eval(m.getProperty(RulesValidator.RULES_ORIGINAL_MESSAGE) == null && ValidatorUtil.validateMessage(m, MessagePart.BODY, null, Operator.EXISTS, null)))
[condition][]Message header "{name}" exists.=m: Message (eval(m.getProperty(RulesValidator.RULES_ORIGINAL_MESSAGE) == null && ValidatorUtil.validateMessage(m, MessagePart.HEADER, "{name}", Operator.EXISTS, "")))
[condition][]Message property "{name}" exists.=m: Message (eval(m.getProperty(RulesValidator.RULES_ORIGINAL_MESSAGE) == null && ValidatorUtil.validateMessage(m, MessagePart.PROPERTY, "{name}", Operator.EXISTS, "")))