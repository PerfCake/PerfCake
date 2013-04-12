#contains
[condition][]Message body contains "{str}".=m: Message (eval(ValidatorUtil.validateMessage(m, MessagePart.BODY, null, Operator.CONTAINS, "{str}")))

#equals
[condition][]Message body equals "{str}".=m: Message (eval(ValidatorUtil.validateMessage(m, MessagePart.BODY, null, Operator.EQUALS, "{str}")))