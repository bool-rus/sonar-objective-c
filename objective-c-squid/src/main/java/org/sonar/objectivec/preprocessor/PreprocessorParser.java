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
import com.sonar.sslr.impl.Parser;
import org.sonar.objectivec.ObjectiveCConfiguration;

public class PreprocessorParser {

    private PreprocessorParser() {
        // Prevent outside instantiation
    }

    public static Parser<Grammar> create() {
        return create(new ObjectiveCConfiguration());
    }

    public static Parser<Grammar> create(ObjectiveCConfiguration conf) {
        return Parser.builder(PreprocessorGrammar.create())
                .withLexer(PreprocessorLexer.create(conf))
                .build();
    }

}
