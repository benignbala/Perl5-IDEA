/*
 * Copyright 2015 Alexandr Evstigneev
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.perl5.lang.embedded;

import com.intellij.lexer.Lexer;
import com.intellij.openapi.project.Project;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IFileElementType;
import com.intellij.psi.tree.TokenSet;
import com.perl5.lang.embedded.lexer.EmbeddedPerlLexerAdapter;
import com.perl5.lang.embedded.psi.EmbeddedPerlElementTypes;
import com.perl5.lang.embedded.psi.impl.EmbeddedPerlFileImpl;
import com.perl5.lang.perl.PerlParserDefinition;
import com.perl5.lang.perl.idea.stubs.PerlFileElementType;
import org.jetbrains.annotations.NotNull;

/**
 * Created by hurricup on 18.05.2015.
 */
public class EmbeddedPerlParserDefinition extends PerlParserDefinition implements EmbeddedPerlElementTypes
{
	public static final IFileElementType FILE = new PerlFileElementType("Embedded Perl5", EmbeddedPerlLanguage.INSTANCE);

	public static final TokenSet COMMENTS = TokenSet.orSet(PerlParserDefinition.COMMENTS,
			TokenSet.create(
					EMBED_TEMPLATE_BLOCK_HTML,
					EMBED_MARKER_OPEN,
					EMBED_MARKER_CLOSE
			));

	@NotNull
	@Override
	public TokenSet getCommentTokens()
	{
		return COMMENTS;
	}

	@NotNull
	@Override
	public Lexer createLexer(Project project)
	{
		return new EmbeddedPerlLexerAdapter(project);
	}

	@Override
	public IFileElementType getFileNodeType()
	{
		return FILE;
	}

	public PsiFile createFile(FileViewProvider viewProvider)
	{
		return new EmbeddedPerlFileImpl(viewProvider);
	}

}
