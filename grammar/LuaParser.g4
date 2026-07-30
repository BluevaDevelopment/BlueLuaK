parser grammar LuaParser;

options { tokenVocab = LuaLexer; }

chunk
    : HASH? block EOF
    ;

block
    : stat* retstat?
    ;

stat
    : SEMI
    | label
    | BREAK
    | GOTO NAME
    | DO block END
    | WHILE exp DO block END
    | REPEAT block UNTIL exp
    | IF exp THEN block (ELSEIF exp THEN block)* (ELSE block)? END
    | FOR NAME ASSIGN exp COMMA exp (COMMA exp)? DO block END
    | FOR namelist IN explist DO block END
    | FUNCTION funcname funcbody
    | LOCAL FUNCTION NAME funcbody
    | LOCAL namelist (ASSIGN explist)?
    | varlist ASSIGN explist
    | functioncall
    ;

retstat
    : RETURN explist? SEMI?
    ;

label
    : DOUBLE_COLON NAME DOUBLE_COLON
    ;

funcname
    : NAME (DOT NAME)* (COLON NAME)?
    ;

varlist
    : variable (COMMA variable)*
    ;

namelist
    : NAME (COMMA NAME)*
    ;

explist
    : exp (COMMA exp)*
    ;

exp
    : orExp
    ;

orExp
    : andExp (OR andExp)*
    ;

andExp
    : compareExp (AND compareExp)*
    ;

compareExp
    : concatExp ((LT | GT | LE | GE | NE | EQ) concatExp)*
    ;

concatExp
    : addExp (CONCAT concatExp)?
    ;

addExp
    : multiplyExp ((PLUS | MINUS) multiplyExp)*
    ;

multiplyExp
    : unaryExp ((STAR | SLASH | PERCENT) unaryExp)*
    ;

unaryExp
    : (NOT | HASH | MINUS) unaryExp
    | powerExp
    ;

powerExp
    : simpleexp (POWER unaryExp)?
    ;

simpleexp
    : NIL
    | FALSE
    | TRUE
    | NUMBER
    | string
    | ELLIPSIS
    | functiondef
    | prefixexp
    | tableconstructor
    ;

functiondef
    : FUNCTION funcbody
    ;

prefixexp
    : (NAME | LPAREN exp RPAREN) postfix*
    ;

postfix
    : LBRACK exp RBRACK
    | DOT NAME
    | COLON NAME args
    | args
    ;

functioncall
    : (NAME | LPAREN exp RPAREN) postfix* callpostfix
    ;

callpostfix
    : COLON NAME args
    | args
    ;

variable
    : NAME
    | (NAME | LPAREN exp RPAREN) postfix* (LBRACK exp RBRACK | DOT NAME)
    ;

args
    : LPAREN explist? RPAREN
    | tableconstructor
    | string
    ;

funcbody
    : LPAREN parlist? RPAREN block END
    ;

parlist
    : namelist (COMMA ELLIPSIS)?
    | ELLIPSIS
    ;

tableconstructor
    : LBRACE fieldlist? RBRACE
    ;

fieldlist
    : field (fieldsep field)* fieldsep?
    ;

field
    : LBRACK exp RBRACK ASSIGN exp
    | NAME ASSIGN exp
    | exp
    ;

fieldsep
    : COMMA
    | SEMI
    ;

string
    : NORMAL_STRING
    | CHAR_STRING
    | LONG_STRING
    ;
