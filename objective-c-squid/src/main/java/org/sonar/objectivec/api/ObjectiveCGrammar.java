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
package org.sonar.objectivec.api;

import com.sonar.sslr.api.Grammar;
import org.sonar.sslr.grammar.GrammarRuleKey;
import org.sonar.sslr.grammar.LexerfulGrammarBuilder;

import static com.sonar.sslr.api.GenericTokenType.EOF;
import static com.sonar.sslr.api.GenericTokenType.IDENTIFIER;
import static org.sonar.objectivec.api.ObjectiveCKeyword.ASSIGN;
import static org.sonar.objectivec.api.ObjectiveCKeyword.ATOMIC;
import static org.sonar.objectivec.api.ObjectiveCKeyword.AT_CLASS;
import static org.sonar.objectivec.api.ObjectiveCKeyword.AT_END;
import static org.sonar.objectivec.api.ObjectiveCKeyword.AT_IMPLEMENTATION;
import static org.sonar.objectivec.api.ObjectiveCKeyword.AT_INTERFACE;
import static org.sonar.objectivec.api.ObjectiveCKeyword.AT_PROPERTY;
import static org.sonar.objectivec.api.ObjectiveCKeyword.BOOL;
import static org.sonar.objectivec.api.ObjectiveCKeyword.BREAK;
import static org.sonar.objectivec.api.ObjectiveCKeyword.CHAR;
import static org.sonar.objectivec.api.ObjectiveCKeyword.CONST;
import static org.sonar.objectivec.api.ObjectiveCKeyword.CONTINUE;
import static org.sonar.objectivec.api.ObjectiveCKeyword.COPY;
import static org.sonar.objectivec.api.ObjectiveCKeyword.DOUBLE;
import static org.sonar.objectivec.api.ObjectiveCKeyword.ELSE;
import static org.sonar.objectivec.api.ObjectiveCKeyword.EXTERN;
import static org.sonar.objectivec.api.ObjectiveCKeyword.FLOAT;
import static org.sonar.objectivec.api.ObjectiveCKeyword.GETTER;
import static org.sonar.objectivec.api.ObjectiveCKeyword.ID;
import static org.sonar.objectivec.api.ObjectiveCKeyword.IF;
import static org.sonar.objectivec.api.ObjectiveCKeyword.INT;
import static org.sonar.objectivec.api.ObjectiveCKeyword.LONG;
import static org.sonar.objectivec.api.ObjectiveCKeyword.NONATOMIC;
import static org.sonar.objectivec.api.ObjectiveCKeyword.READONLY;
import static org.sonar.objectivec.api.ObjectiveCKeyword.READWRITE;
import static org.sonar.objectivec.api.ObjectiveCKeyword.RETAIN;
import static org.sonar.objectivec.api.ObjectiveCKeyword.RETURN;
import static org.sonar.objectivec.api.ObjectiveCKeyword.SETTER;
import static org.sonar.objectivec.api.ObjectiveCKeyword.SHORT;
import static org.sonar.objectivec.api.ObjectiveCKeyword.STATIC;
import static org.sonar.objectivec.api.ObjectiveCKeyword.STRONG;
import static org.sonar.objectivec.api.ObjectiveCKeyword.STRUCT;
import static org.sonar.objectivec.api.ObjectiveCKeyword.UNSIGNED;
import static org.sonar.objectivec.api.ObjectiveCKeyword.VOID;
import static org.sonar.objectivec.api.ObjectiveCKeyword.VOLATILE;
import static org.sonar.objectivec.api.ObjectiveCKeyword.WEAK;
import static org.sonar.objectivec.api.ObjectiveCKeyword.WHILE;
import static org.sonar.objectivec.api.ObjectiveCPunctuator.*;
import static org.sonar.objectivec.api.ObjectiveCPunctuator.BRACE_L;
import static org.sonar.objectivec.api.ObjectiveCPunctuator.COLON;
import static org.sonar.objectivec.api.ObjectiveCPunctuator.COMMA;
import static org.sonar.objectivec.api.ObjectiveCPunctuator.DEC;
import static org.sonar.objectivec.api.ObjectiveCPunctuator.DIV;
import static org.sonar.objectivec.api.ObjectiveCPunctuator.EQ;
import static org.sonar.objectivec.api.ObjectiveCPunctuator.EQEQ;
import static org.sonar.objectivec.api.ObjectiveCPunctuator.GT;
import static org.sonar.objectivec.api.ObjectiveCPunctuator.GTE;
import static org.sonar.objectivec.api.ObjectiveCPunctuator.INC;
import static org.sonar.objectivec.api.ObjectiveCPunctuator.LT;
import static org.sonar.objectivec.api.ObjectiveCPunctuator.LTE;
import static org.sonar.objectivec.api.ObjectiveCPunctuator.MINUS;
import static org.sonar.objectivec.api.ObjectiveCPunctuator.MUL;
import static org.sonar.objectivec.api.ObjectiveCPunctuator.NE;
import static org.sonar.objectivec.api.ObjectiveCPunctuator.PAREN_L;
import static org.sonar.objectivec.api.ObjectiveCPunctuator.PAREN_R;
import static org.sonar.objectivec.api.ObjectiveCPunctuator.PLUS;
import static org.sonar.objectivec.api.ObjectiveCPunctuator.SEMICOLON;
import static org.sonar.objectivec.api.ObjectiveCTokenType.CHARACTER_LITERAL;
import static org.sonar.objectivec.api.ObjectiveCTokenType.DOUBLE_LITERAL;
import static org.sonar.objectivec.api.ObjectiveCTokenType.FLOAT_LITERAL;
import static org.sonar.objectivec.api.ObjectiveCTokenType.INTEGER_LITERAL;
import static org.sonar.objectivec.api.ObjectiveCTokenType.LONG_LITERAL;
import static org.sonar.objectivec.api.ObjectiveCTokenType.STRING_LITERAL;

/**
 * @author Matthew DeTullio
 * @since 2015-11-09
 */
public enum ObjectiveCGrammar implements GrammarRuleKey {
    BIN_TYPE,
    BIN_FUNCTION_DEFINITION,
    BIN_PARAMETER,
    BIN_ARGUMENT,
    BIN_VARIABLE_DEFINITION,
    BIN_FUNCTION_REFERENCE,
    BIN_VARIABLE_REFERENCE,
    BIN_MODIFIER,

    COMPILATION_UNIT,
    DEFINITION,
    CLASS_DEFINITION,
    IMPLEMENTATION_DEFINITION,
    IMPLEMENTATION_MEMBER,
    INTERFACE_DEFINITION,
    INTERFACE_MEMBER,
    PROPERTY_DEFINITION,
    PROPERTY_MODIFIER,
    STRUCT_DEFINITION,
    STRUCT_MEMBER,
    FUNCTION_DEFINITION,
    OBJC_FUNCTION_SPECIFICATION,
    OBJC_FUNCTION_DECLARATION,
    OBJC_FUNCTION_DEFINITION,
    VARIABLE_DEFINITION,
    PARAMETERS_LIST,
    PARAMETER_DECLARATION,
    COMPOUND_STATEMENT,
    VARIABLE_INITIALIZER,
    ARGUMENT_EXPRESSION_LIST,

    STATEMENT,
    EXPRESSION_STATEMENT,
    RETURN_STATEMENT,
    CONTINUE_STATEMENT,
    BREAK_STATEMENT,
    IF_STATEMENT,
    WHILE_STATEMENT,
    CONDITION_CLAUSE,
    ELSE_CLAUSE,
    NO_COMPLEXITY_STATEMENT,

    EXPRESSION,
    ASSIGNMENT_EXPRESSION,
    RELATIONAL_EXPRESSION,
    RELATIONAL_OPERATOR,
    ADDITIVE_EXPRESSION,
    ADDITIVE_OPERATOR,
    MULTIPLICATIVE_EXPRESSION,
    MULTIPLICATIVE_OPERATOR,
    UNARY_EXPRESSION,
    UNARY_OPERATOR,
    POSTFIX_EXPRESSION,
    POSTFIX_OPERATOR,
    PRIMARY_EXPRESSION, GARBAGE;

    public static Grammar create() {
        LexerfulGrammarBuilder b = LexerfulGrammarBuilder.create();

        bins(b);
        miscellaneous(b);
        statements(b);
        expressions(b);

        b.setRootRule(COMPILATION_UNIT);

        return b.buildWithMemoizationOfMatchesForAllRules();
    }

    private static void bins(LexerfulGrammarBuilder b) {
        b.rule(BIN_TYPE).is(b.firstOf(
                BOOL,
                CHAR,
                DOUBLE,
                FLOAT,
                ID,
                INT,
                LONG,
                SHORT,
                VOID,
                IDENTIFIER));

        b.rule(BIN_PARAMETER).is(IDENTIFIER);

        b.rule(BIN_ARGUMENT).is(IDENTIFIER);

        b.rule(BIN_FUNCTION_DEFINITION).is(IDENTIFIER);

        b.rule(BIN_VARIABLE_DEFINITION).is(IDENTIFIER);

        b.rule(BIN_FUNCTION_REFERENCE).is(IDENTIFIER);

        b.rule(BIN_VARIABLE_REFERENCE).is(IDENTIFIER);

        b.rule(BIN_MODIFIER).is(b.firstOf(
                CONST,
                STATIC,
                EXTERN,
                VOLATILE,
                UNSIGNED));
    }

    private static void miscellaneous(LexerfulGrammarBuilder b) {
        b.rule(COMPILATION_UNIT).is(b.zeroOrMore(DEFINITION), EOF);

        b.rule(DEFINITION).is(b.firstOf(
                CLASS_DEFINITION,
                IMPLEMENTATION_DEFINITION,
                INTERFACE_DEFINITION,
                STRUCT_DEFINITION,
                FUNCTION_DEFINITION,
                VARIABLE_DEFINITION,
                STATEMENT,
                b.sequence(b.nextNot(EOF), GARBAGE)));

        b.rule(GARBAGE).is(b.anyToken());

        b.rule(CLASS_DEFINITION).is(AT_CLASS, IDENTIFIER, SEMICOLON);

        b.rule(IMPLEMENTATION_MEMBER).is(b.firstOf(
                PROPERTY_DEFINITION,
                OBJC_FUNCTION_DEFINITION,
                b.sequence(b.nextNot(AT_END), GARBAGE)));

        // TODO: extract common ?template? identifier after interface/class identifier
        b.rule(IMPLEMENTATION_DEFINITION).is(AT_IMPLEMENTATION, IDENTIFIER, b.optional(PAREN_L, IDENTIFIER, PAREN_R),
                b.zeroOrMore(IMPLEMENTATION_MEMBER),
                AT_END);

        b.rule(INTERFACE_MEMBER).is(b.firstOf(
                PROPERTY_DEFINITION,
                OBJC_FUNCTION_DECLARATION,
                b.sequence(b.nextNot(AT_END), GARBAGE)));

        // TODO: extract common ?template? identifier after interface/class identifier
        b.rule(INTERFACE_DEFINITION).is(AT_INTERFACE, IDENTIFIER, b.firstOf(b.sequence(PAREN_L, IDENTIFIER, PAREN_R), b.sequence(COLON, IDENTIFIER)),
                b.optional(PAREN_L, VARIABLE_DEFINITION, PAREN_R),
                b.zeroOrMore(INTERFACE_MEMBER),
                AT_END);

        b.rule(PROPERTY_DEFINITION).is(AT_PROPERTY,
                b.optional(PAREN_L,
                        PROPERTY_MODIFIER, b.zeroOrMore(COMMA, PROPERTY_MODIFIER),
                        PAREN_R),
                BIN_TYPE, BIN_VARIABLE_DEFINITION, SEMICOLON);

        b.rule(PROPERTY_MODIFIER).is(b.firstOf(
                ASSIGN,
                ATOMIC,
                COPY,
                // TODO might be more complex than just identifier
                b.sequence(GETTER, EQ, IDENTIFIER),
                NONATOMIC,
                READONLY,
                READWRITE,
                RETAIN,
                // TODO might be more complex than just identifier
                b.sequence(SETTER, EQ, IDENTIFIER),
                STRONG,
                WEAK));

        b.rule(STRUCT_DEFINITION).is(STRUCT, IDENTIFIER, PAREN_L, b.oneOrMore(STRUCT_MEMBER, SEMICOLON), PAREN_R);

        b.rule(STRUCT_MEMBER).is(BIN_TYPE, IDENTIFIER);

        b.rule(FUNCTION_DEFINITION).is(BIN_TYPE, b.zeroOrMore(BIN_MODIFIER), BIN_FUNCTION_DEFINITION, PAREN_L, b.optional(PARAMETERS_LIST), PAREN_R, COMPOUND_STATEMENT);

        b.rule(OBJC_FUNCTION_SPECIFICATION).is(
                b.firstOf(MINUS, PLUS),
                PAREN_L, BIN_TYPE, PAREN_R,
                b.firstOf(PARAMETERS_LIST, IDENTIFIER));

        b.rule(OBJC_FUNCTION_DECLARATION).is(OBJC_FUNCTION_SPECIFICATION, SEMICOLON);

        b.rule(OBJC_FUNCTION_DEFINITION).is(OBJC_FUNCTION_SPECIFICATION, COMPOUND_STATEMENT);

        b.rule(VARIABLE_DEFINITION).is(b.zeroOrMore(BIN_MODIFIER), BIN_TYPE, b.zeroOrMore(BIN_MODIFIER), BIN_VARIABLE_DEFINITION, b.optional(VARIABLE_INITIALIZER), SEMICOLON);

        b.rule(PARAMETERS_LIST).is(b.oneOrMore(PARAMETER_DECLARATION));

        b.rule(PARAMETER_DECLARATION).is(BIN_PARAMETER, PAREN_L, BIN_TYPE, PAREN_R, BIN_ARGUMENT);

        b.rule(COMPOUND_STATEMENT).is(
                BRACE_L,
                b.zeroOrMore(b.firstOf(
                        VARIABLE_DEFINITION,
                        STATEMENT,
                        b.sequence(b.nextNot(BRACE_R), GARBAGE))),
                BRACE_R);

        b.rule(VARIABLE_INITIALIZER).is(EQ, EXPRESSION);

        b.rule(ARGUMENT_EXPRESSION_LIST).is(EXPRESSION, b.zeroOrMore(COMMA, EXPRESSION));
    }

    private static void statements(LexerfulGrammarBuilder b) {
        b.rule(STATEMENT).is(b.firstOf(
                EXPRESSION_STATEMENT,
                COMPOUND_STATEMENT,
                RETURN_STATEMENT,
                CONTINUE_STATEMENT,
                BREAK_STATEMENT,
                IF_STATEMENT,
                WHILE_STATEMENT,
                NO_COMPLEXITY_STATEMENT));

        b.rule(EXPRESSION_STATEMENT).is(EXPRESSION, SEMICOLON);

        b.rule(RETURN_STATEMENT).is(RETURN, EXPRESSION, SEMICOLON);

        b.rule(CONTINUE_STATEMENT).is(CONTINUE, SEMICOLON);

        b.rule(BREAK_STATEMENT).is(BREAK, SEMICOLON);

        b.rule(IF_STATEMENT).is(IF, CONDITION_CLAUSE, STATEMENT, b.optional(ELSE_CLAUSE));

        b.rule(WHILE_STATEMENT).is(WHILE, CONDITION_CLAUSE, STATEMENT);

        b.rule(CONDITION_CLAUSE).is(PAREN_L, EXPRESSION, PAREN_R);

        b.rule(ELSE_CLAUSE).is(ELSE, STATEMENT);

        b.rule(NO_COMPLEXITY_STATEMENT).is("nocomplexity", STATEMENT);
    }

    private static void expressions(LexerfulGrammarBuilder b) {
        b.rule(EXPRESSION).is(ASSIGNMENT_EXPRESSION);

        b.rule(ASSIGNMENT_EXPRESSION).is(RELATIONAL_EXPRESSION, b.optional(EQ, RELATIONAL_EXPRESSION)).skipIfOneChild();

        b.rule(RELATIONAL_EXPRESSION).is(ADDITIVE_EXPRESSION, b.optional(RELATIONAL_OPERATOR, RELATIONAL_EXPRESSION)).skipIfOneChild();

        b.rule(RELATIONAL_OPERATOR).is(b.firstOf(
                EQEQ,
                NE,
                LT,
                LTE,
                GT,
                GTE));

        b.rule(ADDITIVE_EXPRESSION).is(MULTIPLICATIVE_EXPRESSION, b.optional(ADDITIVE_OPERATOR, ADDITIVE_EXPRESSION)).skipIfOneChild();

        b.rule(ADDITIVE_OPERATOR).is(b.firstOf(
                PLUS,
                MINUS));

        b.rule(MULTIPLICATIVE_EXPRESSION).is(UNARY_EXPRESSION, b.optional(MULTIPLICATIVE_OPERATOR, MULTIPLICATIVE_EXPRESSION)).skipIfOneChild();

        b.rule(MULTIPLICATIVE_OPERATOR).is(b.firstOf(
                MUL,
                DIV));

        b.rule(UNARY_EXPRESSION).is(b.firstOf(
                b.sequence(UNARY_OPERATOR, PRIMARY_EXPRESSION),
                POSTFIX_EXPRESSION)).skipIfOneChild();

        b.rule(UNARY_OPERATOR).is(b.firstOf(
                INC,
                DEC));

        b.rule(POSTFIX_EXPRESSION).is(b.firstOf(
                b.sequence(PRIMARY_EXPRESSION, POSTFIX_OPERATOR),
                b.sequence(BIN_FUNCTION_REFERENCE, PAREN_L, b.optional(ARGUMENT_EXPRESSION_LIST), PAREN_R),
                PRIMARY_EXPRESSION)).skipIfOneChild();

        b.rule(POSTFIX_OPERATOR).is(b.firstOf(
                INC,
                DEC));

        b.rule(PRIMARY_EXPRESSION).is(b.firstOf(
                CHARACTER_LITERAL,
                STRING_LITERAL,
                FLOAT_LITERAL,
                DOUBLE_LITERAL,
                LONG_LITERAL,
                INTEGER_LITERAL,
                BIN_VARIABLE_REFERENCE,
                b.sequence(PAREN_L, EXPRESSION, PAREN_R)));
    }
}
