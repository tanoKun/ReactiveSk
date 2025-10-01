grammar DynamicClassDefinition;

@header {
package com.github.tanokun.reactivesk.compiler.frontend.parser.antlr;
}

tokens { INDENT, DEDENT }

/*
 * =================================================================
 * Parser Rules
 * =================================================================
 */

program
    : (topLevelElement | NEWLINE)* EOF
    ;

topLevelElement
    : classDef
    | skipBlock
    ;

skipBlock
    : notClassHeader (NEWLINE section)?
    ;

notClassHeader
    : { _input.LA(1) != CLASS && _input.LA(1) != EOF }? ~NEWLINE+
    ;

classDef
    : CLASS name=Identifier
      (LPAREN2 constructorParameters=constructorParamList? RPAREN2)?
      COLON (NEWLINE INDENT classBody DEDENT)?
    ;

classBody
    : classMember+
    ;

classMember
    // 各メンバーは、それ自身の定義。空行もメンバーとして許容する。
    : fieldSection
    | initializer
    | function
    | NEWLINE
    ;

fieldSection
    : FIELD COLON NEWLINE
      (INDENT
          (field (NEWLINE+ field)* NEWLINE?)?
       DEDENT
      )?
    ;

field
    : accessModifiers? declaration argument
    ;

initializer
    : INIT (THROWS LPAREN2 throwsList RPAREN2)? COLON (NEWLINE section)?
    ;

function
    : accessModifiers?
      FUNCTION name=Identifier
      LPAREN functionArguments? RPAREN
      functionReturn?
      (THROWS LPAREN2 throwsList RPAREN2)?
      COLON
      (NEWLINE section)?
    ;

constructorParamList
    : constructorParameter (COMMA constructorParameter)*
    ;

constructorParameter
    : constructorParameterNotProperty
    | constructorParameterAsProperty
    ;

constructorParameterNotProperty
    : argument
    ;

constructorParameterAsProperty
    : accessModifiers? declaration argument
    ;

throwsList
    : throw (COMMA throw)*
    ;

throw
    : Identifier
    ;

argument
    : name=Identifier COLON type
    ;

type
    : typeName=Identifier
    | ARRAY OF arrayType=Identifier
    ;

declaration
    : VAL
    | VAR
    | FACTOR
    ;

functionReturn
    : DCOLON returnType=type
    | COLON  returnType=type
    ;

functionArguments
    : argument (COMMA argument)*
    ;

accessModifiers
    : PRIVATE
    ;

section
    : INDENT sectionItems DEDENT
    | INDENT DEDENT
    ;

sectionItems
    : (section | rawToken)+
    ;

rawToken
    : ~(INDENT | DEDENT)
    ;

/*------------------------------------------------------------------
 * Lexer Rules (変更なし)
 *------------------------------------------------------------------*/
CLASS: 'class';
FIELD: 'field';
FUNCTION: 'function';
INIT: 'init';
VAL: 'val';
VAR: 'var';
FACTOR: 'factor';
ARRAY: 'array';
OF: 'of';
PRIVATE: 'private';
THROWS: 'throws';

Identifier: [a-zA-Z_] [a-zA-Z_0-9]*;
LPAREN: '(';
RPAREN: ')';
LPAREN2: '[';
RPAREN2: ']';
COLON: ':';
DCOLON: '::';
COMMA: ',';
STRING: '"' (~["\r\n])*? '"';

COMMENT: '#' ~[\r\n]* -> skip;

NEWLINE: ( '\r'? '\n' | '\r' )+;

WS: [` \t]+ -> skip;

ANY_CHAR: .;