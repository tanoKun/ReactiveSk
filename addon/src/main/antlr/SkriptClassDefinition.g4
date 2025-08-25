grammar SkriptClassDefinition;

// INDENT/DEDENT をパーサーが認識する仮想トークンとして定義
tokens { INDENT, DEDENT }

/*------------------------------------------------------------------
 * Parser Rules (以前の正しいバージョン)
 *------------------------------------------------------------------*/

program
    // ★★★ トップレベルの要素が0個以上繰り返される、と定義 ★★★
    : topLevelElement* EOF
    ;

topLevelElement
    // ★★★ トップレベルには、クラス定義か、それ以外の行が存在する ★★★
    : classDef
    | otherLine
    ;

classDef
    : CLASS name=Identifier (LPAREN classArgs? RPAREN)? COLON NEWLINE
      INDENT
      classBody
      DEDENT
    ;

otherLine
    // `~CLASS` は「CLASSトークンではない、任意のトークン」という意味
    // NEWLINEまたはEOFが来るまで、行のトークンをすべて消費する
    : ~CLASS ( ~NEWLINE )* NEWLINE?
    ;

classArgs: fieldDef (COMMA fieldDef)*;

arg: name=Identifier COLON type;

type:
    typeName=Identifier
    | ARRAY OF arrayType=Identifier; // 'array of' はパーサーで解釈

classBody: classMember*;

classMember: fieldBlock | initBlock | functionDef;

fieldBlock:
    FIELD COLON NEWLINE
    INDENT fieldDef+ DEDENT;

fieldDef: accessModifiers? mutability=(VAL | VAR) arg NEWLINE?;

initBlock:
    INIT COLON NEWLINE
    INDENT rawBody DEDENT;

functionDef:
    accessModifiers? FUNCTION name=Identifier LPAREN funcArgs? RPAREN (DCOLON returnType=type)? COLON NEWLINE
    INDENT rawBody DEDENT;

funcArgs: arg (COMMA arg)*;

rawBody: (~DEDENT)+;

accessModifiers: PRIVATE;

/*------------------------------------------------------------------
 * Lexer Rules (以前の正しいバージョン)
 *------------------------------------------------------------------*/
CLASS: 'class';
FIELD: 'field';
FUNCTION: 'function';
INIT: 'init';
VAL: 'val';
VAR: 'var';
ARRAY: 'array'; // 'array' と 'of' は別々のトークン
OF: 'of';
PRIVATE: 'private';

Identifier: [a-zA-Z_] [a-zA-Z_0-9]*;
LPAREN: '(';
RPAREN: ')';
COLON: ':';
DCOLON: '::';
COMMA: ',';
STRING: '"' (~["\r\n])*? '"';

COMMENT
    : '#' ~[\r\n]* -> skip
    ;

// NEWLINE はパーサーに渡す必要がある
NEWLINE: ( '\r'? '\n' | '\r' )+;

// WS (空白) はカスタムレキサーが手動で処理するため、ここではスキップする
WS: [ \t]+ -> skip;

ANY_CHAR: .;