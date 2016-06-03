/*
 * Copyright 2016 Alexandr Evstigneev
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

package com.perl5.lang.perl.extensions.packageprocessor.impl;

import com.perl5.lang.perl.extensions.packageprocessor.*;
import com.perl5.lang.perl.internals.PerlStrictMask;
import com.perl5.lang.perl.internals.PerlWarningsMask;
import com.perl5.lang.perl.psi.PerlUseStatement;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by hurricup on 29.03.2016.
 */
public class DancerPackageProcessor extends PerlPackageProcessorBase implements
		PerlStrictProvider,
		PerlUtfProvider,
		PerlWarningsProvider
{
	private static final List<PerlExportDescriptor> EXPORT_DESCRIPTORS = new ArrayList<PerlExportDescriptor>();

	static
	{
		for (String keyword : PerlDancerDSL.DSL_KEYWORDS)
		{
			EXPORT_DESCRIPTORS.add(new PerlExportDescriptor(keyword, "Dancer"));
		}
	}

	@Override
	public PerlStrictMask getStrictMask(PerlUseStatement useStatement, PerlStrictMask currentMask)
	{
		// fixme implement modification
		return currentMask.clone();
	}

	@Override
	public PerlWarningsMask getWarningMask(PerlUseStatement useStatement, PerlWarningsMask currentMask)
	{
		// fixme implement modification
		return currentMask.clone();
	}

	@NotNull
	@Override
	public List<PerlExportDescriptor> getImports(@NotNull PerlUseStatement useStatement)
	{
		return getExportDescriptors();
	}

	public List<PerlExportDescriptor> getExportDescriptors()
	{
		return EXPORT_DESCRIPTORS;
	}
}
