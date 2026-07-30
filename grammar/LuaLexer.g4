lexer grammar LuaLexer;

@members {
    private var longBracketLevel: Int = 0

    private fun openingLevel(): Int {
        val value = text ?: return -1
        val first = value.indexOf('[')
        val second = value.indexOf('[', first + 1)
        return second - first - 1
    }

    private fun closingLevel(): Int {
        val value = text ?: return -1
        val last = value.lastIndexOf(']')
        val previous = value.lastIndexOf(']', last - 1)
        return last - previous - 1
    }
}

AND: 'and';
BREAK: 'break';
DO: 'do';
ELSE: 'else';
ELSEIF: 'elseif';
END: 'end';
FALSE: 'false';
FOR: 'for';
FUNCTION: 'function';
GOTO: 'goto';
IF: 'if';
IN: 'in';
LOCAL: 'local';
NIL: 'nil';
NOT: 'not';
OR: 'or';
RETURN: 'return';
REPEAT: 'repeat';
THEN: 'then';
TRUE: 'true';
UNTIL: 'until';
WHILE: 'while';

ELLIPSIS: '...';
CONCAT: '..';
DOUBLE_COLON: '::';
LE: '<=';
GE: '>=';
EQ: '==';
NE: '~=';
ASSIGN: '=';
LT: '<';
GT: '>';
PLUS: '+';
MINUS: '-';
STAR: '*';
SLASH: '/';
PERCENT: '%';
POWER: '^';
HASH: '#';
LPAREN: '(';
RPAREN: ')';
LBRACE: '{';
RBRACE: '}';
LBRACK: '[';
RBRACK: ']';
SEMI: ';';
COLON: ':';
COMMA: ',';
DOT: '.';

NAME: [a-zA-Z_] [a-zA-Z_0-9]*;

NUMBER
    : '0' [xX] (HEX_DIGIT+ DOT HEX_DIGIT* | DOT HEX_DIGIT+ | HEX_DIGIT+) ([eEpP] [+-]? DIGIT+)?
    | DIGIT+ DOT DIGIT* EXPONENT?
    | DOT DIGIT+ EXPONENT?
    | DIGIT+ EXPONENT?
    ;

NORMAL_STRING: '"' (ESCAPE | ~["\\])* '"';
CHAR_STRING: '\'' (ESCAPE | ~['\\])* '\'';

BLOCK_COMMENT_START
    : '--' '[' '='* '[' { longBracketLevel = openingLevel() } -> more, pushMode(LONG_COMMENT_MODE)
    ;

LONG_STRING_START
    : '[' '='* '[' { longBracketLevel = openingLevel() } -> more, pushMode(LONG_STRING_MODE)
    ;

LINE_COMMENT: '--' ~[\r\n]* -> channel(HIDDEN);
SHEBANG: {line == 1 && charPositionInLine == 0}? '#' ~[\r\n]* -> channel(HIDDEN);
WS: [ \t\r\n\f]+ -> channel(HIDDEN);

fragment ESCAPE: '\\' .;
fragment EXPONENT: [eE] [+-]? DIGIT+;
fragment HEX_DIGIT: [0-9a-fA-F];
fragment DIGIT: [0-9];

mode LONG_STRING_MODE;
LONG_STRING
    : ']' '='* ']' { closingLevel() == longBracketLevel }? -> popMode
    ;
LONG_STRING_CONTENT: . -> more;

mode LONG_COMMENT_MODE;
BLOCK_COMMENT_END
    : ']' '='* ']' { closingLevel() == longBracketLevel }? -> channel(HIDDEN), popMode
    ;
BLOCK_COMMENT_CONTENT: . -> more;
