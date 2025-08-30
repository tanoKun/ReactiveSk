grammar SkriptClassDefinition;

tokens { INDENT, DEDENT }

/*------------------------------------------------------------------
 * Parser Rules
 *------------------------------------------------------------------*/

program
    : (classDef | skipTopLevel)* skipTailWithoutNewline? EOF
    ;

// トップレベルは class 以外をスキップ(行＋直後のインデントブロック丸ごと)
skipTopLevel
    : notClassHeader block?
    ;

// 「CLASS で始まらない」1行を NEWLINE まで消費(EOF はここでは消費しない)
notClassHeader
    : { _input.LA(1) != CLASS }? (~NEWLINE)* NEWLINE
    ;

// ファイル末尾が改行なしで「class 以外」で終わるケースをスキップ
skipTailWithoutNewline
    : { _input.LA(1) != CLASS }? (~NEWLINE)+
    ;

classDef
    : CLASS name=Identifier
      (LPAREN2 constructorParams=constructorParamList? RPAREN2)?
      COLON
      (NEWLINE
       INDENT
       classBody
       DEDENT
      )?
    ;

constructorParamList
    : constructorParam (COMMA constructorParam)*
    ;

constructorParam
    : (accessModifiers? declaration)? arg
    ;

throwsList
    : throwsParam (COMMA throwsParam)*
    ;

throwsParam
    : throw=Identifier*
    ;

arg
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

classBody
    : classMember*
    ;

classMember
    : fieldSection
    | initSection
    | functionDef
    ;

// field セクションは空許容、ブロック自体も任意
fieldSection
    : FIELD COLON NEWLINE
      (INDENT fieldDef* DEDENT)?
    ;

fieldDef
    : accessModifiers? declaration arg NEWLINE
    ;

// init はヘッダのみ解析。本体は任意のブロック(空も可)をスキップ
initSection
    : INIT (THROWS LPAREN2 throwsList RPAREN2)? COLON NEWLINE
      block?
    ;

// function はヘッダ解析(戻り型「: / ::」どちらも可、throws 可)
// 本体は任意のブロック(空も可)をスキップ
functionDef
    : accessModifiers?
      FUNCTION name=Identifier
      LPAREN funcArgs? RPAREN
      functionReturn?
      (THROWS LPAREN2 throwsList RPAREN2)?
      COLON NEWLINE
      block?
    ;

functionReturn
    : DCOLON returnType=type
    | COLON  returnType=type
    ;

funcArgs
    : arg (COMMA arg)*
    ;

// 汎用ブロック(入れ子対応)。空ブロックも INDENT DEDENT で表現
block
    : INDENT blockItems DEDENT
    | INDENT DEDENT
    ;

blockItems
    : (block | rawToken)+
    ;

// INDENT/DEDENT 以外の任意トークン
rawToken
    : ~(INDENT | DEDENT)
    ;

accessModifiers
    : PRIVATE
    ;

/*------------------------------------------------------------------
 * Lexer Rules
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

// NEWLINE はインデント処理に必要
NEWLINE: ( '\r'? '\n' | '\r' )+;

// スペース/タブはスキップ(INDENT/DEDENT はカスタムレキサーで生成)
WS: [ \t]+ -> skip;

// その他の単一文字もトークン化
ANY_CHAR: .;