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

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.TokenType;

public enum ObjectiveCPunctuator implements TokenType {
    // Basic arithmetic operators
    PLUS("+"),
    MINUS("-"),
    MUL("*"),
    DIV("/"),
    MODULO("%"),
    INC("++"),
    DEC("--"),
    EQ("="),

    // Comparison/relational operators
    EQEQ("=="),
    NE("!="),
    LT("<"),
    GT(">"),
    LTE("<="),
    GTE(">="),

    // Logical operators
    NOT("!"),
    AND("&&"),
    OR("||"),

    // Bitwise Operators
    BW_NOT("~"),
    BW_AND("&"),
    BW_OR("|"),
    BW_XOR("^"),
    BW_LSHIFT("<<"),
    BW_RSHIFT(">>"),

    // Compound assignment operators
    PLUS_EQ("+="),
    MINUS_EQ("-="),
    MUL_EQ("*="),
    DIV_EQ("/="),
    MODULO_EQ("%="),
    BW_AND_EQ("&="),
    BW_OR_EQ("|="),
    BW_XOR_EQ("^="),
    BW_LSHIFT_EQ("<<="),
    BW_RSHIFT_EQ(">>="),

    // Member and pointer operators
    ARROW("->"), // ARROW?
    DOT("."), // DOT?
    DOT_STAR(".*"), // DOT_MUL?
    ARROW_STAR("->*"), // ARROW_MUL?

    // Delimiters
    SEMICOLON(";"),
    COLON(":"),
    COMMA(","),
    DOUBLECOLON("::"),
    PAREN_L("("),
    PAREN_R(")"),
    BRACE_L("{"),
    BRACE_R("}"),
    BRACKET_L("["),
    BRACKET_R("]"),

    // Other operators
    QUEST("?"),
    ELLIPSIS("...");

    private final String value;

    /*private*/ ObjectiveCPunctuator(String word) {
        this.value = word;
    }

    @Override
    public String getName() {
        return name();
    }

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public boolean hasToBeSkippedFromAst(AstNode node) {
        return false;
    }

}
