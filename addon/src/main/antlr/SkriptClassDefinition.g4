grammar SkriptClassDefinition;

program
    : classDef* EOF
    ;

classDef
    : CLASS Identifier (LPAREN classArgs? RPAREN)? classBody
    ;

classArgs
    : fieldDef (COMMA fieldDef)*
    ;

arg
    : Identifier COLON type
    ;

type
    : Identifier
    | ARRAY OF Identifier
    ;

classBody
    : LBRACE classMember* RBRACE
    ;

classMember
    : initBlock
    | functionDef
    ;

fieldDef
    : accessModifiers? (VAL | VAR) arg
    ;

initBlock
    : INIT LBRACE rawContent* RBRACE
    ;

functionDef
    : accessModifiers? FUNCTION Identifier LPAREN funcArgs? RPAREN (COLON type)? LBRACE rawContent* RBRACE
    ;

funcArgs
    : arg (COMMA arg)*
    ;

rawContent
    : LBRACE rawContent* RBRACE
    | ~(LBRACE | RBRACE)
    ;

accessModifiers
    : PRIVATE
    ;


LBRACE: '{';
RBRACE: '}';
CLASS: 'class';
FUNCTION: 'function';
INIT: 'init';
VAL: 'val';
VAR: 'var';
ARRAY: 'array';
OF: 'of';
PRIVATE: 'private';
Identifier: [a-zA-Z_] [a-zA-Z_0-9]*;
LPAREN: '(';
RPAREN: ')';
COLON: ':';
COMMA: ',';
STRING: '"' (~["\r\n])*? '"';
NEWLINE: ( '\r'? '\n' | '\r' )+ -> skip;
WS: [ \t]+ -> skip;
ANY_CHAR: . ;