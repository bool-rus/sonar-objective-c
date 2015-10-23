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

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.Grammar;
import com.sonar.sslr.api.Preprocessor;
import com.sonar.sslr.api.PreprocessorAction;
import com.sonar.sslr.api.RecognitionException;
import com.sonar.sslr.api.Token;
import com.sonar.sslr.api.TokenType;
import com.sonar.sslr.api.Trivia;
import com.sonar.sslr.impl.Parser;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.objectivec.ObjectiveCConfiguration;
import org.sonar.objectivec.api.ObjectiveCTokenType;
import org.sonar.objectivec.lexer.ObjectiveCLexer;
import org.sonar.squidbridge.SquidAstVisitorContext;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.sonar.sslr.api.GenericTokenType.EOF;
import static com.sonar.sslr.api.GenericTokenType.IDENTIFIER;
import static org.sonar.objectivec.preprocessor.PreprocessorKeyword.HASH_IFDEF;
import static org.sonar.objectivec.preprocessor.PreprocessorKeyword.HASH_IFNDEF;
import static org.sonar.objectivec.api.ObjectiveCTokenType.PREPROCESSOR;
import static org.sonar.objectivec.api.ObjectiveCTokenType.STRING_LITERAL;

public class ObjectiveCPreprocessor extends Preprocessor {
    private class State {
        public boolean skipping;
        public int nestedIfdefs;
        public File includeUnderAnalysis;

        public State(File includeUnderAnalysis) {
            reset();
            this.includeUnderAnalysis = includeUnderAnalysis;
        }

        public final void reset() {
            skipping = false;
            nestedIfdefs = 0;
            includeUnderAnalysis = null;
        }
    }

    static class MismatchException extends Exception {
        private String why;

        MismatchException(String why) {
            this.why = why;
        }

        public String toString() {
            return why;
        }
    }

    class Macro {
        public Macro(String name, List<Token> params, List<Token> body, boolean variadic) {
            this.name = name;
            this.params = params;
            this.body = body;
            this.isVariadic = variadic;
        }

        public String toString() {
            return name
                    + (params == null ? "" : "(" + serialize(params, ", ") + (isVariadic ? "..." : "") + ")")
                    + " -> '" + serialize(body) + "'";
        }

        public boolean checkArgumentsCount(int count) {
            return isVariadic
                    ? count >= params.size() - 1
                    : count == params.size();
        }

        public String name;
        public List<Token> params;
        public List<Token> body;
        public boolean isVariadic;
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(ObjectiveCPreprocessor.class);
    private Parser<Grammar> pplineParser = null;
    private MapChain<String, Macro> macros = new MapChain<>();
    private Set<File> analysedFiles = new HashSet<>();
    private SourceCodeProvider codeProvider = new SourceCodeProvider();
    private SquidAstVisitorContext<Grammar> context;
    private ExpressionEvaluator ifExprEvaluator;
    private ObjectiveCConfiguration conf;

    public static class Include {
        private int line;
        private String path;

        Include(int line, String path) {
            this.line = line;
            this.path = path;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Include that = (Include) o;

            return line == that.line && !(path != null ? !path.equals(that.path) : that.path != null);
        }

        @Override
        public int hashCode() {
            return 31 * line + (path != null ? path.hashCode() : 0);
        }

        public String getPath() {
            return path;
        }

        public int getLine() {
            return line;
        }
    }

    private Multimap<String, Include> includedFiles = HashMultimap.create();
    private Multimap<String, Include> missingIncludeFiles = HashMultimap.create();

    // state which is not shared between files
    private State state = new State(null);
    private Deque<State> stateStack = new LinkedList<State>();

    public ObjectiveCPreprocessor(SquidAstVisitorContext<Grammar> context) {
        this(context, new ObjectiveCConfiguration());
    }

    public ObjectiveCPreprocessor(SquidAstVisitorContext<Grammar> context, ObjectiveCConfiguration conf) {
        this(context, conf, new SourceCodeProvider());
    }

    private void registerMacros(Map<String, String> standardMacros) {
        for (Map.Entry<String, String> entry : standardMacros.entrySet()) {
            Token bodyToken;
            try {
                bodyToken = Token.builder()
                        .setLine(1)
                        .setColumn(0)
                        .setURI(new java.net.URI(""))
                        .setValueAndOriginalValue(entry.getValue())
                        .setType(STRING_LITERAL)
                        .build();
            } catch (java.net.URISyntaxException e) {
                throw new RuntimeException(e);
            }

            macros.put(entry.getKey(), new Macro(entry.getKey(), null, Lists.newArrayList(bodyToken), false));
        }
    }

    public ObjectiveCPreprocessor(SquidAstVisitorContext<Grammar> context,
            ObjectiveCConfiguration conf,
            SourceCodeProvider sourceCodeProvider) {
        this.context = context;
        this.ifExprEvaluator = new ExpressionEvaluator(conf, this);
        this.conf = conf;

        codeProvider = sourceCodeProvider;
//        codeProvider.setIncludeRoots(conf.getBaseDir());

        pplineParser = PreprocessorParser.create(conf);
    }

    public Collection<Include> getIncludedFiles(File file) {
        return includedFiles.get(file.getPath());
    }

    public Collection<Include> getMissingIncludeFiles(File file) {
        return missingIncludeFiles.get(file.getPath());
    }

    private File currentContextFile;

    @Override
    public PreprocessorAction process(List<Token> tokens) {
        Token token = tokens.get(0);
        TokenType tokenType = token.getType();
        File file = getFileUnderAnalysis();
        String filePath = file == null ? token.getURI().toString() : file.getAbsolutePath();

        if (tokenType == PREPROCESSOR) {
            AstNode lineAst;
            try {
                lineAst = pplineParser.parse(token.getValue()).getFirstChild();
            } catch (RecognitionException e) {
                LOGGER.warn(String.format("Cannot parse %s, ignoring...", token.getValue()), e);
                return new PreprocessorAction(1, Lists.newArrayList(Trivia.createSkippedText(token)), new ArrayList<Token>());
            }

            String lineKind = lineAst.getName();

            if ("IFDEF_LINE".equals(lineKind)) {
                return handleIfdefLine(lineAst, token, filePath);
            } else if ("ELSE_LINE".equals(lineKind)) {
                return handleElseLine(lineAst, token, filePath);
            } else if ("ENDIF_LINE".equals(lineKind)) {
                return handleEndifLine(lineAst, token, filePath);
            } else if ("IF_LINE".equals(lineKind)) {
                return handleIfLine(lineAst, token, filePath);
            } else if ("ELIF_LINE".equals(lineKind)) {
                return handleElIfLine(lineAst, token, filePath);
            }

            if (inSkippingMode()) {
                return new PreprocessorAction(1, Lists.newArrayList(Trivia.createSkippedText(token)), new ArrayList<Token>());
            }

            if ("DEFINE_LINE".equals(lineKind)) {
                return handleDefineLine(lineAst, token, filePath);
            } else if ("INCLUDE_LINE".equals(lineKind)) {
                return handleIncludeLine(lineAst, token, filePath);
            } else if ("UNDEF_LINE".equals(lineKind)) {
                return handleUndefLine(lineAst, token, filePath);
            }

            // Ignore all other preprocessor directives (which are not handled explicitly)
            // and strip them from the stream

            return new PreprocessorAction(1, Lists.newArrayList(Trivia.createSkippedText(token)), new ArrayList<Token>());
        }

        if (tokenType != EOF) {
            if (inSkippingMode()) {
                return new PreprocessorAction(1, Lists.newArrayList(Trivia.createSkippedText(token)), new ArrayList<Token>());
            }

            if (tokenType != STRING_LITERAL && !ObjectiveCTokenType.numberLiterals().contains(tokenType)) {
                return handleIdentifiersAndKeywords(tokens, token, filePath);
            }
        }

        return PreprocessorAction.NO_OPERATION;
    }

    public void finishedPreprocessing(File file) {
        // From 16.3.5 "Scope of macro definitions":
        // A macro definition lasts (independent of block structure) until
        // a corresponding #undef directive is encountered or (if none
        // is encountered) until the end of the translation unit.

        LOGGER.debug("finished preprocessing '{}'", file);

        analysedFiles.clear();
        macros.clearLowPrio();
        state.reset();
        currentContextFile = null;
    }

    public String valueOf(String macroname) {
        String result = null;
        Macro macro = macros.get(macroname);
        if (macro != null) {
            result = serialize(macro.body);
        }
        return result;
    }

    private PreprocessorAction handleIfdefLine(AstNode ast, Token token, String filename) {
        if (state.skipping) {
            state.nestedIfdefs++;
        } else {
            Macro macro = macros.get(getMacroName(ast));
            TokenType tokType = ast.getToken().getType();
            if ((tokType == HASH_IFDEF && macro == null) || (tokType == HASH_IFNDEF && macro != null)) {
                LOGGER.trace("[{}:{}]: '{}' evaluated to false, skipping tokens that follow",
                        filename, token.getLine(), token.getValue());
                state.skipping = true;
            }
        }

        return new PreprocessorAction(1, Lists.newArrayList(Trivia.createSkippedText(token)), new ArrayList<Token>());
    }

    PreprocessorAction handleElseLine(AstNode ast, Token token, String filename) {
        if (state.nestedIfdefs == 0) {
            if (state.skipping) {
                LOGGER.trace("[{}:{}]: #else, returning to non-skipping mode", filename, token.getLine());
            } else {
                LOGGER.trace("[{}:{}]: skipping tokens inside the #else", filename, token.getLine());
            }

            state.skipping = !state.skipping;
        }

        return new PreprocessorAction(1, Lists.newArrayList(Trivia.createSkippedText(token)), new ArrayList<Token>());
    }

    PreprocessorAction handleEndifLine(AstNode ast, Token token, String filename) {
        if (state.nestedIfdefs > 0) {
            state.nestedIfdefs--;
        } else {
            if (state.skipping) {
                LOGGER.trace("[{}:{}]: #endif, returning to non-skipping mode", filename, token.getLine());
            }
            state.skipping = false;
        }

        return new PreprocessorAction(1, Lists.newArrayList(Trivia.createSkippedText(token)), new ArrayList<Token>());
    }

    PreprocessorAction handleIfLine(AstNode ast, Token token, String filename) {
        if (state.skipping) {
            state.nestedIfdefs++;
        } else {
            LOGGER.trace("[{}:{}]: handling #if line '{}'",
                    filename, token.getLine(), token.getValue());
            try {
                state.skipping = !ifExprEvaluator.eval(ast.getFirstDescendant(PreprocessorGrammar.CONSTANT_EXPRESSION));
            } catch (EvaluationException e) {
                LOGGER.error("[{}:{}]: error evaluating the expression {} assume 'true' ...",
                        filename, token.getLine(), token.getValue());
                LOGGER.error("{}", e);
                state.skipping = false;
            }

            if (state.skipping) {
                LOGGER.trace("[{}:{}]: '{}' evaluated to false, skipping tokens that follow",
                        filename, token.getLine(), token.getValue());
            }
        }

        return new PreprocessorAction(1, Lists.newArrayList(Trivia.createSkippedText(token)), new ArrayList<Token>());
    }

    PreprocessorAction handleElIfLine(AstNode ast, Token token, String filename) {
        // Handling of an elif line is similar to handling of an if line but
        // doesn't increase the nesting level
        if (state.nestedIfdefs == 0) {
            if (state.skipping) { //the preceeding clauses had been evaluated to false
                try {
                    LOGGER.trace("[{}:{}]: handling #elif line '{}'",
                            filename, token.getLine(), token.getValue());

                    // *this* preprocessor instance is used for evaluation, too.
                    // It *must not* be in skipping mode while evaluating expressions.
                    state.skipping = false;

                    state.skipping = !ifExprEvaluator.eval(ast.getFirstDescendant(PreprocessorGrammar.CONSTANT_EXPRESSION));
                } catch (EvaluationException e) {
                    LOGGER.error("[{}:{}]: error evaluating the expression {} assume 'true' ...",
                            filename, token.getLine(), token.getValue());
                    LOGGER.error("{}", e);
                    state.skipping = false;
                }

                if (state.skipping) {
                    LOGGER.trace("[{}:{}]: '{}' evaluated to false, skipping tokens that follow",
                            filename, token.getLine(), token.getValue());
                }
            } else {
                state.skipping = !state.skipping;
                LOGGER.trace("[{}:{}]: skipping tokens inside the #elif", filename, token.getLine());
            }
        }

        return new PreprocessorAction(1, Lists.newArrayList(Trivia.createSkippedText(token)), new ArrayList<Token>());
    }

    PreprocessorAction handleDefineLine(AstNode ast, Token token, String filename) {
        // Here we have a define directive. Parse it and store the result in a dictionary.

        Macro macro = parseMacroDefinition(ast);
        if (macro != null) {
            LOGGER.trace("[{}:{}]: storing macro: '{}'", filename, token.getLine(), macro);
            macros.put(macro.name, macro);
        }

        return new PreprocessorAction(1, Lists.newArrayList(Trivia.createSkippedText(token)), new ArrayList<Token>());
    }

    private void parseIncludeLine(String includeLine, String filename) {
        AstNode includeAst = pplineParser.parse(includeLine);
        handleIncludeLine(includeAst, includeAst.getFirstDescendant(PreprocessorGrammar.INCLUDE_BODY_QUOTED).getToken(), filename);
    }

    PreprocessorAction handleIncludeLine(AstNode ast, Token token, String filename) {
        return new PreprocessorAction(1, Lists.newArrayList(Trivia.createSkippedText(token)), new ArrayList<Token>());
    }

    PreprocessorAction handleUndefLine(AstNode ast, Token token, String filename) {
        return new PreprocessorAction(1, Lists.newArrayList(Trivia.createSkippedText(token)), new ArrayList<Token>());
    }

    PreprocessorAction handleIdentifiersAndKeywords(List<Token> tokens, Token curr, String filename) {
        //
        // Every identifier and every keyword can be a macro instance.
        // Pipe the resulting string through a lexer to create proper Tokens
        // and to expand recursively all macros which may be in there.
        //

        PreprocessorAction ppaction = PreprocessorAction.NO_OPERATION;
        Macro macro = macros.get(curr.getValue());
        if (macro != null) {
            List<Token> replTokens = new LinkedList<Token>();
            int tokensConsumed = 0;

            if (macro.params == null) {
                tokensConsumed = 1;
                replTokens = new LinkedList<Token>(expandMacro(macro.name, serialize(evaluateHashhashOperators(macro.body))));
            } else {
                int tokensConsumedMatchingArgs = expandFunctionLikeMacro(macro.name,
                        tokens.subList(1, tokens.size()),
                        replTokens);
                if (tokensConsumedMatchingArgs > 0) {
                    tokensConsumed = 1 + tokensConsumedMatchingArgs;
                }
            }

            if (tokensConsumed > 0) {

                // Rescanning to expand function like macros, in case it requires consuming more tokens
                List<Token> outTokens = new LinkedList<Token>();
                macros.disable(macro.name);
                while (!replTokens.isEmpty()) {
                    Token c = replTokens.get(0);
                    PreprocessorAction action = PreprocessorAction.NO_OPERATION;
                    if (c.getType() == IDENTIFIER) {
                        List<Token> rest = new ArrayList(replTokens);
                        rest.addAll(tokens.subList(tokensConsumed, tokens.size()));
                        action = handleIdentifiersAndKeywords(rest, c, filename);
                    }
                    if (action == PreprocessorAction.NO_OPERATION) {
                        replTokens.remove(0);
                        outTokens.add(c);
                    } else {
                        outTokens.addAll(action.getTokensToInject());
                        int tokensConsumedRescanning = action.getNumberOfConsumedTokens();
                        if (tokensConsumedRescanning >= replTokens.size()) {
                            tokensConsumed += tokensConsumedRescanning - replTokens.size();
                            replTokens.clear();
                        } else {
                            replTokens.subList(0, tokensConsumedRescanning).clear();
                        }
                    }
                }
                replTokens = outTokens;
                macros.enable(macro.name);

                replTokens = reallocate(replTokens, curr);

                LOGGER.trace("[{}:{}]: replacing '" + curr.getValue()
                                + (tokensConsumed == 1
                                ? ""
                                : serialize(tokens.subList(1, tokensConsumed))) + "' -> '" + serialize(replTokens) + "'",
                        filename, curr.getLine());

                ppaction = new PreprocessorAction(
                        tokensConsumed,
                        Lists.newArrayList(Trivia.createSkippedText(tokens.subList(0, tokensConsumed))),
                        replTokens);
            }
        }

        return ppaction;
    }

    public String expandFunctionLikeMacro(String macroName, List<Token> restTokens) {
        List<Token> expansion = new LinkedList<Token>();
        expandFunctionLikeMacro(macroName, restTokens, expansion);
        return serialize(expansion);
    }

    private int expandFunctionLikeMacro(String macroName, List<Token> restTokens, List<Token> expansion) {
        List<Token> replTokens = null;
        List<Token> arguments = new ArrayList<Token>();
        int tokensConsumedMatchingArgs = matchArguments(restTokens, arguments);

        Macro macro = macros.get(macroName);
        if (macro != null && macro.checkArgumentsCount(arguments.size())) {
            if (arguments.size() > macro.params.size()) {
                //Group all arguments into the last one
                List<Token> vaargs = arguments.subList(macro.params.size() - 1, arguments.size());
                Token firstToken = vaargs.get(0);
                arguments = arguments.subList(0, macro.params.size() - 1);
                arguments.add(Token.builder()
                        .setLine(firstToken.getLine())
                        .setColumn(firstToken.getColumn())
                        .setURI(firstToken.getURI())
                        .setValueAndOriginalValue(serialize(vaargs, ","))
                        .setType(STRING_LITERAL)
                        .build());
            }
            replTokens = replaceParams(macro.body, macro.params, arguments);
            replTokens = evaluateHashhashOperators(replTokens);
            expansion.addAll(expandMacro(macro.name, serialize(replTokens)));
        }

        return tokensConsumedMatchingArgs;
    }

    private List<Token> expandMacro(String macroName, String macroExpression) {
        // C++ standard 16.3.4/2 Macro Replacement - Rescanning and further replacement
        List<Token> tokens = null;
        macros.disable(macroName);
        try {
            tokens = stripEOF(ObjectiveCLexer.create(context, conf).lex(macroExpression));
        } finally {
            macros.enable(macroName);
        }
        return tokens;
    }

    private List<Token> stripEOF(List<Token> tokens) {
        if (tokens.get(tokens.size() - 1).getType() == EOF) {
            return tokens.subList(0, tokens.size() - 1);
        } else {
            return tokens;
        }
    }

    private String serialize(List<Token> tokens) {
        return serialize(tokens, " ");
    }

    private String serialize(List<Token> tokens, String spacer) {
        List<String> values = new LinkedList<String>();
        for (Token t : tokens) {
            values.add(t.getValue());
        }
        return StringUtils.join(values, spacer);
    }

    private int matchArguments(List<Token> tokens, List<Token> arguments) {
        List<Token> rest = tokens;
        try {
            rest = match(rest, "(");
        } catch (MismatchException me) {
            return 0;
        }

        try {
            do {
                rest = matchArgument(rest, arguments);
                try {
                    rest = match(rest, ",");
                } catch (MismatchException me) {
                    break;
                }
            } while (true);

            rest = match(rest, ")");
        } catch (MismatchException me) {
            LOGGER.error("{}", me);
            return 0;
        }

        return tokens.size() - rest.size();
    }

    private List<Token> match(List<Token> tokens, String str) throws MismatchException {
        if (!tokens.get(0).getValue().equals(str)) {
            throw new MismatchException("Mismatch: expected '" + str + "' got: '"
                    + tokens.get(0).getValue() + "'");
        }
        return tokens.subList(1, tokens.size());
    }

    private List<Token> matchArgument(List<Token> tokens, List<Token> arguments) throws MismatchException {
        int nestingLevel = 0;
        int tokensConsumed = 0;
        int noTokens = tokens.size();
        Token firstToken = tokens.get(0);
        Token currToken = firstToken;
        String curr = currToken.getValue();
        List<Token> matchedTokens = new LinkedList<Token>();

        while (true) {
            if (nestingLevel == 0 && (",".equals(curr) || ")".equals(curr))) {
                if (tokensConsumed > 0) {
                    arguments.add(Token.builder()
                            .setLine(firstToken.getLine())
                            .setColumn(firstToken.getColumn())
                            .setURI(firstToken.getURI())
                            .setValueAndOriginalValue(serialize(matchedTokens).trim())
                            .setType(STRING_LITERAL)
                            .build());
                }
                return tokens.subList(tokensConsumed, noTokens);
            }

            if ("(".equals(curr)) {
                nestingLevel++;
            } else if (")".equals(curr)) {
                nestingLevel--;
            }

            tokensConsumed++;
            if (tokensConsumed == noTokens) {
                throw new MismatchException("reached the end of the stream while matching a macro argument");
            }

            matchedTokens.add(currToken);
            currToken = tokens.get(tokensConsumed);
            curr = currToken.getValue();
        }
    }

    private List<Token> replaceParams(List<Token> body, List<Token> parameters, List<Token> arguments) {
        // Replace all parameters by according arguments
        // "Stringify" the argument if the according parameter is preceded by an #

        List<Token> newTokens = new ArrayList<Token>();
        if (!body.isEmpty()) {
            List<String> defParamValues = new ArrayList<String>();
            for (Token t : parameters) {
                defParamValues.add(t.getValue());
            }

            boolean tokenPastingLeftOp = false;
            boolean tokenPastingRightOp = false;

            for (int i = 0; i < body.size(); ++i) {
                Token curr = body.get(i);
                int index = defParamValues.indexOf(curr.getValue());
                if (index == -1) {
                    newTokens.add(curr);
                } else if (index == arguments.size()) {
                    // EXTENSION: GCC's special meaning of token paste operator
                    // If variable argument is left out then the comma before the paste operator will be deleted
                    int j = i;
                    if (j == 0 || !"##".equals(body.get(--j).getValue())) {
                        continue;
                    }
                    int k = j;
                    if (j > 0 && ",".equals(body.get(j - 1).getValue())) {
                        newTokens.remove(newTokens.size() - 1 + j - i); //remove the comma
                        newTokens.remove(newTokens.size() - 1 + k - i); //remove the paste operator
                    }
                } else if (index < arguments.size()) {
                    // token pasting operator?
                    int j = i + 1;
                    if (j < body.size() && "##".equals(body.get(j).getValue())) {
                        tokenPastingLeftOp = true;
                    }
                    // in case of token pasting operator do not fully expand
                    Token replacement = arguments.get(index);
                    String newValue;
                    if (tokenPastingLeftOp) {
                        newValue = replacement.getValue();
                        tokenPastingLeftOp = false;
                        tokenPastingRightOp = true;
                    } else if (tokenPastingRightOp) {
                        newValue = replacement.getValue();
                        tokenPastingLeftOp = false;
                        tokenPastingRightOp = false;
                    } else {
                        if (i > 0 && "#".equals(body.get(i - 1).getValue())) {
                            // If the token is a macro, the macro is not expanded - the macro name is converted into a string.
                            newTokens.remove(newTokens.size() - 1);
                            newValue = encloseWithQuotes(quote(replacement.getValue()));
                        } else {
                            // otherwise the arguments have to be fully expanded before expanding the body of the macro
                            newValue = serialize(expandMacro("", replacement.getValue()));
                        }
                    }

                    newTokens.add(Token.builder()
                            .setLine(replacement.getLine())
                            .setColumn(replacement.getColumn())
                            .setURI(replacement.getURI())
                            .setValueAndOriginalValue(newValue)
                            .setType(replacement.getType())
                            .setGeneratedCode(true)
                            .build());
                }
            }
        }

        return newTokens;
    }

    private List<Token> evaluateHashhashOperators(List<Token> tokens) {
        List<Token> newTokens = new ArrayList<Token>();

        Iterator<Token> it = tokens.iterator();
        while (it.hasNext()) {
            Token curr = it.next();
            if ("##".equals(curr.getValue())) {
                Token pred = predConcatToken(newTokens);
                Token succ = succConcatToken(it);
                newTokens.add(Token.builder()
                        .setLine(pred.getLine())
                        .setColumn(pred.getColumn())
                        .setURI(pred.getURI())
                        .setValueAndOriginalValue(pred.getValue() + succ.getValue())
                        .setType(pred.getType())
                        .setGeneratedCode(true)
                        .build());
            } else {
                newTokens.add(curr);
            }
        }

        return newTokens;
    }

    private Token predConcatToken(List<Token> tokens) {
        while (!tokens.isEmpty()) {
            Token last = tokens.remove(tokens.size() - 1);
            if (!tokens.isEmpty()) {
                Token pred = tokens.get(tokens.size() - 1);
                if (!pred.hasTrivia()) {
                    // Needed to paste tokens 0 and x back together after #define N(hex) 0x ## hex
                    tokens.remove(tokens.size() - 1);
                    String replacement = pred.getValue() + last.getValue();
                    last = Token.builder()
                            .setLine(pred.getLine())
                            .setColumn(pred.getColumn())
                            .setURI(pred.getURI())
                            .setValueAndOriginalValue(replacement)
                            .setType(pred.getType())
                            .setGeneratedCode(true)
                            .build();
                }
            }
            return last;
        }
        return null;
    }

    private Token succConcatToken(Iterator<Token> it) {
        Token succ = null;
        while (it.hasNext()) {
            succ = it.next();
            if (!"##".equals(succ.getValue())) {
                break;
            }
        }
        return succ;
    }

    private String quote(String str) {
        StringBuilder result = new StringBuilder(2 * str.length());
        boolean addBlank = false;
        boolean ignoreNextBlank = false;
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '_') { // token
                if (addBlank) {
                    result.append(' ');
                    addBlank = false;
                }
                result.append(c);
            } else { // special characters
                switch (c) {
                    case ' ':
                        if (ignoreNextBlank) {
                            ignoreNextBlank = false;
                        } else {
                            addBlank = true;
                        }
                        break;
                    case '\"':
                        if (addBlank) {
                            result.append(' ');
                            addBlank = false;
                        }
                        result.append("\\\"");
                        break;
                    case '\\':
                        result.append("\\\\");
                        addBlank = false;
                        ignoreNextBlank = true;
                        break;
                    default: // operator
                        result.append(c);
                        addBlank = false;
                        ignoreNextBlank = true;
                        break;
                }
            }
        }
        return result.toString();
    }

    private String encloseWithQuotes(String str) {
        return "\"" + str + "\"";
    }

    private List<Token> reallocate(List<Token> tokens, Token token) {
        List<Token> reallocated = new LinkedList<Token>();
        int currColumn = token.getColumn();
        for (Token t : tokens) {
            reallocated.add(Token.builder()
                    .setLine(token.getLine())
                    .setColumn(currColumn)
                    .setURI(token.getURI())
                    .setValueAndOriginalValue(t.getValue())
                    .setType(t.getType())
                    .setGeneratedCode(true)
                    .build());
            currColumn += t.getValue().length() + 1;
        }

        return reallocated;
    }

    private Macro parseMacroDefinition(String macroDef) {
        return parseMacroDefinition(pplineParser.parse(macroDef)
                .getFirstDescendant(PreprocessorGrammar.DEFINE_LINE));
    }

    private Macro parseMacroDefinition(AstNode defineLineAst) {
        AstNode ast = defineLineAst.getFirstChild();
        AstNode nameNode = ast.getFirstDescendant(PreprocessorGrammar.PP_TOKEN);
        String macroName = nameNode.getTokenValue();

        AstNode paramList = ast.getFirstDescendant(PreprocessorGrammar.PARAMETER_LIST);
        List<Token> macroParams = paramList == null
                ? "OBJECTLIKE_MACRO_DEFINITION".equals(ast.getName()) ? null : new LinkedList<Token>()
                : getParams(paramList);

        AstNode vaargs = ast.getFirstDescendant(PreprocessorGrammar.VARIADICPARAMETER);
        if (vaargs != null) {
            AstNode identifier = vaargs.getFirstChild(IDENTIFIER);
            macroParams.add(identifier == null
                    ? Token.builder()
                    .setLine(vaargs.getToken().getLine())
                    .setColumn(vaargs.getToken().getColumn())
                    .setURI(vaargs.getToken().getURI())
                    .setValueAndOriginalValue("__VA_ARGS__")
                    .setType(IDENTIFIER)
                    .setGeneratedCode(true)
                    .build()
                    : identifier.getToken());
        }

        AstNode replList = ast.getFirstDescendant(PreprocessorGrammar.REPLACEMENT_LIST);
        List<Token> macroBody = replList == null
                ? new LinkedList<Token>()
                : replList.getTokens().subList(0, replList.getTokens().size() - 1);

        return new Macro(macroName, macroParams, macroBody, vaargs != null);
    }

    private List<Token> getParams(AstNode identListAst) {
        List<Token> params = new ArrayList<Token>();
        if (identListAst != null) {
            for (AstNode node : identListAst.getChildren(IDENTIFIER)) {
                params.add(node.getToken());
            }
        }

        return params;
    }

    private String getMacroName(AstNode ast) {
        return ast.getFirstDescendant(IDENTIFIER).getTokenValue();
    }

    private String stripQuotes(String str) {
        return str.substring(1, str.length() - 1);
    }

    private File getFileUnderAnalysis() {
        if (state.includeUnderAnalysis == null) {
            return context.getFile();
        }
        return state.includeUnderAnalysis;
    }

    private boolean inSkippingMode() {
        return state.skipping;
    }
}
