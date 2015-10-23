/*
 * SonarQube Objective-C (Community) :: Squid
 * Copyright (C) 2012-2016 OCTO Technology, Backelite, and contributors
 * mailto:sonarqube@googlegroups.com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.objectivec.preprocessor;

import com.sonar.sslr.api.Grammar;
import org.sonar.objectivec.api.ObjectiveCTokenType;
import org.sonar.sslr.grammar.GrammarRuleKey;
import org.sonar.sslr.grammar.LexerfulGrammarBuilder;

import static com.sonar.sslr.api.GenericTokenType.IDENTIFIER;
import static org.sonar.objectivec.preprocessor.PreprocessorKeyword.HASH_DEFINE;
import static org.sonar.objectivec.preprocessor.PreprocessorKeyword.HASH_ELIF;
import static org.sonar.objectivec.preprocessor.PreprocessorKeyword.HASH_ELSE;
import static org.sonar.objectivec.preprocessor.PreprocessorKeyword.HASH_ENDIF;
import static org.sonar.objectivec.preprocessor.PreprocessorKeyword.HASH_ERROR;
import static org.sonar.objectivec.preprocessor.PreprocessorKeyword.HASH_IF;
import static org.sonar.objectivec.preprocessor.PreprocessorKeyword.HASH_IFDEF;
import static org.sonar.objectivec.preprocessor.PreprocessorKeyword.HASH_IFNDEF;
import static org.sonar.objectivec.preprocessor.PreprocessorKeyword.HASH_IMPORT;
import static org.sonar.objectivec.preprocessor.PreprocessorKeyword.HASH_INCLUDE;
import static org.sonar.objectivec.preprocessor.PreprocessorKeyword.HASH_INCLUDE_NEXT;
import static org.sonar.objectivec.preprocessor.PreprocessorKeyword.HASH_LINE;
import static org.sonar.objectivec.preprocessor.PreprocessorKeyword.HASH_PRAGMA;
import static org.sonar.objectivec.preprocessor.PreprocessorKeyword.HASH_UNDEF;
import static org.sonar.objectivec.preprocessor.PreprocessorKeyword.HASH_WARNING;

/**
 * @author Matthew DeTullio
 * @since 2015-11-09
 */
public enum PreprocessorGrammar implements GrammarRuleKey {
    PREPROCESSOR_LINE,
    DEFINE_LINE,
    INCLUDE_LINE,
    INCLUDE_BODY,
    EXPANDED_INCLUDE_BODY,
    INCLUDE_BODY_QUOTED,
    INCLUDE_BODY_BRACKETED,
    INCLUDE_BODY_FREEFORM,
    IFDEF_LINE,
    REPLACEMENT_LIST,
    ARGUMENT_LIST,
    PARAMETER_LIST,
    VARIADICPARAMETER,
    PP_TOKEN,
    IF_LINE,
    ELIF_LINE,
    CONSTANT_EXPRESSION,
    PRIMARY_EXPRESSION,
    UNARY_EXPRESSION,
    UNARY_OPERATOR,
    MULTIPLICATIVE_EXPRESSION,
    ADDITIVE_EXPRESSION,
    SHIFT_EXPRESSION,
    STRING_PREFIX,
    RELATIONAL_EXPRESSION,
    EQUALITY_EXPRESSION,
    AND_EXPRESSION,
    EXCLUSIVE_OR_EXPRESSION,
    INCLUSIVE_OR_EXPRESSION,
    LOGICAL_AND_EXPRESSION,
    LOGICAL_OR_EXPRESSION,
    CONDITIONAL_EXPRESSION,
    EXPRESSION,
    BOOL,
    LITERAL,
    DEFINED_EXPRESSION,
    FUNCTIONLIKE_MACRO,
    FUNCTIONLIKE_MACRO_DEFINITION,
    OBJECTLIKE_MACRO_DEFINITION,
    ELSE_LINE,
    ENDIF_LINE,
    UNDEF_LINE,
    LINE_LINE,
    ERROR_LINE,
    PRAGMA_LINE,
    WARNING_LINE,
    ARGUMENT,
    SOMETHING_CONTAINING_PARENTHESES,
    SOMETHING_WITHOUT_PARENTHESES,
    ALL_BUT_LEFT_PAREN,
    ALL_BUT_RIGHT_PAREN,
    ALL_BUT_PAREN,
    ALL_BUT_COMMA;

    public static Grammar create() {
        LexerfulGrammarBuilder b = LexerfulGrammarBuilder.create();

        toplevelDefinitionGrammar(b);
        defineLineGrammar(b);
        includeLineGrammar(b);
        ifLineGrammar(b);
        allTheOtherLinesGrammar(b);

        b.setRootRule(PREPROCESSOR_LINE);

        return b.buildWithMemoizationOfMatchesForAllRules();
    }


    private static void toplevelDefinitionGrammar(LexerfulGrammarBuilder b) {
        b.rule(PREPROCESSOR_LINE).is(
                b.firstOf(
                        DEFINE_LINE,
                        INCLUDE_LINE,
                        IFDEF_LINE,
                        IF_LINE,
                        ELIF_LINE,
                        ELSE_LINE,
                        ENDIF_LINE,
                        UNDEF_LINE,
                        LINE_LINE,
                        ERROR_LINE,
                        PRAGMA_LINE,
                        WARNING_LINE
                )
        );
    }

    private static void defineLineGrammar(LexerfulGrammarBuilder b) {
        b.rule(DEFINE_LINE).is(
                b.firstOf(
                        FUNCTIONLIKE_MACRO_DEFINITION,
                        OBJECTLIKE_MACRO_DEFINITION
                )
        );

        b.rule(FUNCTIONLIKE_MACRO_DEFINITION).is(
                b.firstOf(
                        b.sequence(HASH_DEFINE, /*b.oneOrMore(WS),*/ PP_TOKEN, "(", /*b.zeroOrMore(WS),*/ b.optional(PARAMETER_LIST), /*b.zeroOrMore(WS),*/ ")", b.optional(/*b.zeroOrMore(WS),*/ REPLACEMENT_LIST)),
                        b.sequence(HASH_DEFINE, /*b.oneOrMore(WS),*/ PP_TOKEN, "(", /*b.zeroOrMore(WS),*/ VARIADICPARAMETER, /*b.zeroOrMore(WS),*/ ")", b.optional(/*b.zeroOrMore(WS),*/ REPLACEMENT_LIST)),
                        b.sequence(HASH_DEFINE, /*b.oneOrMore(WS),*/ PP_TOKEN, "(", /*b.zeroOrMore(WS),*/ PARAMETER_LIST, /*b.zeroOrMore(WS),*/ ",", /*b.zeroOrMore(WS),*/ VARIADICPARAMETER, /*b.zeroOrMore(WS),*/ ")", b.optional(/*b.zeroOrMore(WS),*/ REPLACEMENT_LIST))
                )
        );

        b.rule(VARIADICPARAMETER).is(b.optional(IDENTIFIER), /*b.zeroOrMore(WS),*/ "...");

        b.rule(OBJECTLIKE_MACRO_DEFINITION).is(HASH_DEFINE, /*b.oneOrMore(WS),*/ PP_TOKEN, b.optional(/*b.oneOrMore(WS),*/ REPLACEMENT_LIST));

        b.rule(REPLACEMENT_LIST).is(
                b.oneOrMore(
                        b.firstOf(
                                "##",
                                b.sequence(b.optional(STRING_PREFIX), "#"),
                                PP_TOKEN
                        )
                )
        );

        b.rule(PARAMETER_LIST).is(IDENTIFIER, b.zeroOrMore(/*b.zeroOrMore(WS),*/ ",", /*b.zeroOrMore(WS),*/ IDENTIFIER, b.nextNot(/*b.zeroOrMore(WS),*/ "...")));
        b.rule(ARGUMENT_LIST).is(ARGUMENT, b.zeroOrMore(/*b.zeroOrMore(WS),*/ ",", /*b.zeroOrMore(WS),*/ ARGUMENT));
        b.rule(STRING_PREFIX).is(b.firstOf("L", "u8", "u", "U"));

        b.rule(ARGUMENT).is(
                b.firstOf(
                        b.oneOrMore(SOMETHING_CONTAINING_PARENTHESES),
                        SOMETHING_WITHOUT_PARENTHESES
                )
        );

        b.rule(SOMETHING_CONTAINING_PARENTHESES).is(
                b.zeroOrMore(ALL_BUT_PAREN),
                "(",
                b.firstOf(
                        SOMETHING_CONTAINING_PARENTHESES,
                        b.zeroOrMore(ALL_BUT_RIGHT_PAREN), ")"
                ),
                ALL_BUT_COMMA
        );

        b.rule(SOMETHING_WITHOUT_PARENTHESES).is(b.oneOrMore(b.nextNot(b.firstOf(",", ")", "(")), b.anyToken()));

        b.rule(ALL_BUT_LEFT_PAREN).is(b.nextNot("("), b.anyToken());
        b.rule(ALL_BUT_RIGHT_PAREN).is(b.nextNot(")"), b.anyToken());
        b.rule(ALL_BUT_PAREN).is(b.nextNot(b.firstOf("(", ")")), b.anyToken());
        b.rule(ALL_BUT_COMMA).is(b.nextNot(","), b.anyToken());

        b.rule(PP_TOKEN).is(b.anyToken());
    }

    private static void includeLineGrammar(LexerfulGrammarBuilder b) {
        b.rule(INCLUDE_LINE).is(
                b.firstOf(HASH_IMPORT, HASH_INCLUDE, HASH_INCLUDE_NEXT),
                /*b.zeroOrMore(WS),*/
                INCLUDE_BODY//,
                /*b.zeroOrMore(WS)*/
        );
        b.rule(INCLUDE_BODY).is(
                b.firstOf(
                        INCLUDE_BODY_QUOTED,
                        INCLUDE_BODY_BRACKETED,
                        INCLUDE_BODY_FREEFORM
                )
        );
        b.rule(EXPANDED_INCLUDE_BODY).is(
                b.firstOf(
                        INCLUDE_BODY_QUOTED,
                        INCLUDE_BODY_BRACKETED
                )
        );
        b.rule(INCLUDE_BODY_QUOTED).is(ObjectiveCTokenType.STRING_LITERAL);
        b.rule(INCLUDE_BODY_BRACKETED).is("<", b.oneOrMore(b.nextNot(">"), PP_TOKEN), ">");
        b.rule(INCLUDE_BODY_FREEFORM).is(b.oneOrMore(PP_TOKEN));
    }

    private static void allTheOtherLinesGrammar(LexerfulGrammarBuilder b) {
        b.rule(IFDEF_LINE).is(b.firstOf(HASH_IFDEF, HASH_IFNDEF), /*b.oneOrMore(WS),*/ IDENTIFIER/*, b.zeroOrMore(WS)*/);
        b.rule(ELSE_LINE).is(HASH_ELSE/*, b.zeroOrMore(WS)*/);
        b.rule(ENDIF_LINE).is(HASH_ENDIF/*, b.zeroOrMore(WS)*/);
        b.rule(UNDEF_LINE).is(HASH_UNDEF, /*b.oneOrMore(WS),*/ IDENTIFIER);
        b.rule(LINE_LINE).is(HASH_LINE, /*b.oneOrMore(WS),*/ b.oneOrMore(PP_TOKEN));
        b.rule(ERROR_LINE).is(HASH_ERROR, /*b.zeroOrMore(WS),*/ b.zeroOrMore(PP_TOKEN));
        b.rule(PRAGMA_LINE).is(HASH_PRAGMA, /*b.zeroOrMore(WS),*/ b.zeroOrMore(PP_TOKEN));
        b.rule(WARNING_LINE).is(HASH_WARNING, /*b.zeroOrMore(WS),*/ b.zeroOrMore(PP_TOKEN));
    }

    private static void ifLineGrammar(LexerfulGrammarBuilder b) {
        b.rule(IF_LINE).is(HASH_IF, /*b.zeroOrMore(WS),*/ CONSTANT_EXPRESSION/*, b.zeroOrMore(WS)*/);
        b.rule(ELIF_LINE).is(HASH_ELIF, /*b.zeroOrMore(WS),*/ CONSTANT_EXPRESSION/*, b.zeroOrMore(WS)*/);

        b.rule(CONSTANT_EXPRESSION).is(CONDITIONAL_EXPRESSION);

        b.rule(CONDITIONAL_EXPRESSION).is(
                b.firstOf(
                        b.sequence(LOGICAL_OR_EXPRESSION, /*b.zeroOrMore(WS),*/ "?", /*b.zeroOrMore(WS),*/ b.optional(EXPRESSION), /*b.zeroOrMore(WS),*/ ":", /*b.zeroOrMore(WS),*/ CONDITIONAL_EXPRESSION),
                        LOGICAL_OR_EXPRESSION
                )
        ).skipIfOneChild();

        b.rule(LOGICAL_OR_EXPRESSION).is(LOGICAL_AND_EXPRESSION, b.zeroOrMore(/*b.zeroOrMore(WS),*/ "||", /*b.zeroOrMore(WS),*/ LOGICAL_AND_EXPRESSION)).skipIfOneChild();

        b.rule(LOGICAL_AND_EXPRESSION).is(INCLUSIVE_OR_EXPRESSION, b.zeroOrMore(/*b.zeroOrMore(WS),*/ "&&", /*b.zeroOrMore(WS),*/ INCLUSIVE_OR_EXPRESSION)).skipIfOneChild();

        b.rule(INCLUSIVE_OR_EXPRESSION).is(EXCLUSIVE_OR_EXPRESSION, b.zeroOrMore(/*b.zeroOrMore(WS),*/ "|", /*b.zeroOrMore(WS),*/ EXCLUSIVE_OR_EXPRESSION)).skipIfOneChild();

        b.rule(EXCLUSIVE_OR_EXPRESSION).is(AND_EXPRESSION, b.zeroOrMore(/*b.zeroOrMore(WS),*/ "^", /*b.zeroOrMore(WS),*/ AND_EXPRESSION)).skipIfOneChild();

        b.rule(AND_EXPRESSION).is(EQUALITY_EXPRESSION, b.zeroOrMore(/*b.zeroOrMore(WS),*/ "&", /*b.zeroOrMore(WS),*/ EQUALITY_EXPRESSION)).skipIfOneChild();

        b.rule(EQUALITY_EXPRESSION).is(RELATIONAL_EXPRESSION, b.zeroOrMore(/*b.zeroOrMore(WS),*/ b.firstOf("==", "!="), /*b.zeroOrMore(WS),*/ RELATIONAL_EXPRESSION)).skipIfOneChild();

        b.rule(RELATIONAL_EXPRESSION).is(SHIFT_EXPRESSION, b.zeroOrMore(/*b.zeroOrMore(WS),*/ b.firstOf("<", ">", "<=", ">="), /*b.zeroOrMore(WS),*/ SHIFT_EXPRESSION)).skipIfOneChild();

        b.rule(SHIFT_EXPRESSION).is(ADDITIVE_EXPRESSION, b.zeroOrMore(/*b.zeroOrMore(WS),*/ b.firstOf("<<", ">>"), /*b.zeroOrMore(WS),*/ ADDITIVE_EXPRESSION)).skipIfOneChild();

        b.rule(ADDITIVE_EXPRESSION).is(MULTIPLICATIVE_EXPRESSION, b.zeroOrMore(/*b.zeroOrMore(WS),*/ b.firstOf("+", "-"), /*b.zeroOrMore(WS),*/ MULTIPLICATIVE_EXPRESSION)).skipIfOneChild();

        b.rule(MULTIPLICATIVE_EXPRESSION).is(UNARY_EXPRESSION, b.zeroOrMore(/*b.zeroOrMore(WS),*/ b.firstOf("*", "/", "%"), /*b.zeroOrMore(WS),*/ UNARY_EXPRESSION)).skipIfOneChild();

        b.rule(UNARY_EXPRESSION).is(
                b.firstOf(
                        b.sequence(UNARY_OPERATOR, /*b.zeroOrMore(WS),*/ MULTIPLICATIVE_EXPRESSION),
                        PRIMARY_EXPRESSION
                )
        ).skipIfOneChild();

        b.rule(UNARY_OPERATOR).is(
                b.firstOf("+", "-", "!", "~")
        );

        b.rule(PRIMARY_EXPRESSION).is(
                b.firstOf(
                        LITERAL,
                        b.sequence("(", /*b.zeroOrMore(WS),*/ EXPRESSION, /*b.zeroOrMore(WS),*/ ")"),
                        DEFINED_EXPRESSION,
                        FUNCTIONLIKE_MACRO,
                        IDENTIFIER
                )
        ).skipIfOneChild();

        b.rule(LITERAL).is(
                b.firstOf(
                        ObjectiveCTokenType.CHARACTER_LITERAL,
                        ObjectiveCTokenType.STRING_LITERAL,
                        ObjectiveCTokenType.FLOAT_LITERAL,
                        ObjectiveCTokenType.DOUBLE_LITERAL,
                        ObjectiveCTokenType.LONG_LITERAL,
                        ObjectiveCTokenType.INTEGER_LITERAL,
                        BOOL
                )
        );

        b.rule(BOOL).is(
                b.firstOf(
                        "true",
                        "false"
                )
        );

        b.rule(EXPRESSION).is(CONDITIONAL_EXPRESSION, b.zeroOrMore(/*b.zeroOrMore(WS),*/ ",", /*b.zeroOrMore(WS),*/ CONDITIONAL_EXPRESSION));

        b.rule(DEFINED_EXPRESSION).is(
                "defined",
                b.firstOf(
                        b.sequence(/*b.zeroOrMore(WS),*/ "(", /*b.zeroOrMore(WS),*/ IDENTIFIER, /*b.zeroOrMore(WS),*/ ")"),
                        /*b.sequence(b.oneOrMore(WS),*/ IDENTIFIER/*)*/
                )
        );

        b.rule(FUNCTIONLIKE_MACRO).is(IDENTIFIER, /*b.zeroOrMore(WS),*/ "(", /*b.zeroOrMore(WS),*/ b.optional(b.nextNot(")"), ARGUMENT_LIST), /*b.zeroOrMore(WS),*/ ")");
    }
}
