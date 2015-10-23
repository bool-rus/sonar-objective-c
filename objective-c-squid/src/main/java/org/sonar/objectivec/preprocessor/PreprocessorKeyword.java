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

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.TokenType;

public enum PreprocessorKeyword implements TokenType {
    HASH_DEFINE("#define"),
    HASH_ELIF("#elif"),
    HASH_ELSE("#else"),
    HASH_ENDIF("#endif"),
    HASH_ERROR("#error"),
    HASH_IF("#if"),
    HASH_IFDEF("#ifdef"),
    HASH_IFNDEF("#ifndef"),
    HASH_IMPORT("#import"),
    HASH_INCLUDE("#include"),
    HASH_INCLUDE_NEXT("#include_next"),
    HASH_LINE("#line"),
    HASH_PRAGMA("#pragma"),
    HASH_UNDEF("#undef"),
    HASH_WARNING("#warning");

    private final String value;

    /*private*/ PreprocessorKeyword(String value) {
        this.value = value;
    }

    public String getName() {
        return name();
    }

    public String getValue() {
        return value;
    }

    public boolean hasToBeSkippedFromAst(AstNode node) {
        return false;
    }

    public static String[] keywordValues() {
        PreprocessorKeyword[] keywordsEnum = PreprocessorKeyword.values();
        String[] keywords = new String[keywordsEnum.length];
        for (int i = 0; i < keywords.length; i++) {
            keywords[i] = keywordsEnum[i].getValue();
        }
        return keywords;
    }

}
