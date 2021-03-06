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

package base;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.roots.*;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryTable;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.CharsetToolkit;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.testFramework.UsefulTestCase;
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase;
import com.intellij.testFramework.fixtures.impl.CodeInsightTestFixtureImpl;
import com.intellij.util.ObjectUtils;
import com.perl5.lang.perl.fileTypes.PerlFileTypeScript;
import com.perl5.lang.perl.idea.configuration.settings.PerlSharedSettings;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.picocontainer.MutablePicoContainer;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

/**
 * Created by hurricup on 04.03.2016.
 */
public abstract class PerlLightCodeInsightFixtureTestCase extends LightCodeInsightFixtureTestCase
{
	private static final String PERL_LIBRARY_NAME = "-perl-test-lib-";
	private static final String START_FOLD = "<fold\\stext=\'[^\']*\'(\\sexpand=\'[^\']*\')*>";
	private static final String END_FOLD = "</fold>";


	public static Application getApplication()
	{
		return ApplicationManager.getApplication();
	}

	public String getFileExtension()
	{
		return PerlFileTypeScript.EXTENSION_PL;
	}


	@Override
	protected void setUp() throws Exception
	{
		super.setUp();
		registerApplicationService(PerlSharedSettings.class, new PerlSharedSettings());
		setUpLibrary();
	}

	public String getTestResultsFilePath()
	{
		return getTestDataPath() + "/" + getTestName(true) + "." + getFileExtension() + ".txt";
	}

	protected void setUpLibrary()
	{
		ApplicationManager.getApplication().runWriteAction(() ->
		{
			ModifiableRootModel modifiableModel = ModuleRootManager.getInstance(myModule).getModifiableModel();

			for (OrderEntry entry : modifiableModel.getOrderEntries())
			{
				if (entry instanceof LibraryOrderEntry && StringUtil.equals(((LibraryOrderEntry) entry).getLibraryName(), PERL_LIBRARY_NAME))
				{
					modifiableModel.removeOrderEntry(entry);
				}
			}

//			TempDirTestFixture tempDirFixture = myFixture.getTempDirFixture();
//			tempDirFixture.copyAll("testData/testlib", "testlib");

			LibraryTable moduleLibraryTable = modifiableModel.getModuleLibraryTable();
			Library library = moduleLibraryTable.createLibrary(PERL_LIBRARY_NAME);
			Library.ModifiableModel libraryModifyableModel = library.getModifiableModel();
			VirtualFile libdir = LocalFileSystem.getInstance().refreshAndFindFileByPath("testData/testlib");
			assert libdir != null;
			libraryModifyableModel.addRoot(libdir, OrderRootType.CLASSES); // myFixture.findFileInTempDir("testlib")
			libraryModifyableModel.commit();
			modifiableModel.commit();
			CodeInsightTestFixtureImpl.ensureIndexesUpToDate(getProject());
		});
	}

	protected <T> void registerApplicationService(final Class<T> aClass, T object)
	{
		final MutablePicoContainer container = ((MutablePicoContainer) getApplication().getPicoContainer());
		container.registerComponentImplementation(object, aClass);
//		container.registerComponentInDisposer(serviceImplementation);

		Disposer.register(getProject(), new Disposable()
		{
			@Override
			public void dispose()
			{
				container.unregisterComponent(aClass.getName());
			}
		});
	}

	public void initWithPerlTidy()
	{
		initWithPerlTidy("perlTidy");
	}

	public void initWithPerlTidy(@NotNull String targetName)
	{
		try
		{
			initWithFileContent(targetName, getFileExtension(), FileUtil.loadFile(new File("testData", "perlTidy.code"), CharsetToolkit.UTF8, true).trim());
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}


	public void initWithFileSmart()
	{
		initWithFileSmart(getTestName(true));
	}

	public void initWithFileSmart(String filename)
	{
		try
		{
			initWithFile(filename, getFileExtension());
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}

	public void initWithTextSmart(String content)
	{
		try
		{
			initWithFileContent("test", getFileExtension(), content);
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}

	@Deprecated // use initWithFileSmart
	public void initWithFileAsScript(String filename)
	{
		try
		{
			initWithFile(filename, PerlFileTypeScript.EXTENSION_PL);
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}

	public void initWithFile(String filename, String extension) throws IOException
	{
		initWithFile(filename, extension, filename + ".code");
	}

	public void initWithFile(String targetFileName, String targetFileExtension, String sourceFileNameWithExtension) throws IOException
	{
		initWithFileContent(targetFileName, targetFileExtension, FileUtil.loadFile(new File(getTestDataPath(), sourceFileNameWithExtension), CharsetToolkit.UTF8, true));
	}

	public void initWithFileContent(String filename, String extension, String content) throws IOException
	{
		myFixture.configureByText(filename + "." + extension, content);
	}

	@NotNull
	protected <T extends PsiElement> T getElementAtCaret(@NotNull Class<T> clazz)
	{
		int offset = myFixture.getEditor().getCaretModel().getOffset();
		PsiElement focused = myFixture.getFile().findElementAt(offset);
		return ObjectUtils.assertNotNull(PsiTreeUtil.getParentOfType(focused, clazz, false));
	}

	@NotNull
	protected <T extends PsiElement> T getElementAtCaret(int caretIndex, @NotNull Class<T> clazz)
	{
		CaretModel caretModel = myFixture.getEditor().getCaretModel();
		assertTrue(caretModel.getCaretCount() > caretIndex);
		Caret caret = caretModel.getAllCarets().get(caretIndex);
		int offset = caret.getOffset();
		PsiElement focused = myFixture.getFile().findElementAt(offset);
		return ObjectUtils.assertNotNull(PsiTreeUtil.getParentOfType(focused, clazz, false));
	}

	protected void testFoldingRegions(@NotNull String verificationFileName, LanguageFileType fileType)
	{
		testFoldingRegions(verificationFileName, false, fileType);
	}

	protected void testFoldingRegions(@NotNull String verificationFileName, boolean doCheckCollapseStatus, LanguageFileType fileType)
	{
		String expectedContent;
		try
		{
			expectedContent = FileUtil.loadFile(new File(getTestDataPath() + "/" + verificationFileName + ".code"));
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
		Assert.assertNotNull(expectedContent);

		expectedContent = StringUtil.replace(expectedContent, "\r", "");
		final String cleanContent = expectedContent.replaceAll(START_FOLD, "").replaceAll(END_FOLD, "");

		myFixture.configureByText(fileType, cleanContent);
		final String actual = ((CodeInsightTestFixtureImpl) myFixture).getFoldingDescription(doCheckCollapseStatus);

		Assert.assertEquals(expectedContent, actual);
	}

	protected void testSmartKeyFile(String filename, char typed)
	{
		initWithFileSmart(filename);
		myFixture.type(typed);
		checkResultByFile(filename);
	}

	protected void testSmartKey(String original, char typed, String expected)
	{
		initWithTextSmart(original);
		myFixture.type(typed);
		myFixture.checkResult(expected);
	}

	protected void testLiveTemplateFile(String filename)
	{
		initWithFileSmart(filename);
		myFixture.performEditorAction(IdeActions.ACTION_EXPAND_LIVE_TEMPLATE_BY_TAB);
		checkResultByFile(filename);
	}


	protected void checkResultByFile(String filenameWithoutExtension)
	{
		String checkFileName = filenameWithoutExtension + ".txt";


		myFixture.checkResultByFile(checkFileName);
	}

	public void assertLookupIs(String... pattern)
	{
		assertLookupIs(Arrays.asList(pattern));
	}

	public void assertLookupIs(List<String> expected)
	{
		List<String> lookups = myFixture.getLookupElementStrings();
		assertNotNull(lookups);
		UsefulTestCase.assertSameElements(lookups, expected);
	}


	public void assertNotContainsLookupElements(String... pattern)
	{
		assertNotContainsLookupElements(Arrays.asList(pattern));
	}

	public void assertNotContainsLookupElements(List<String> pattern)
	{
		List<String> strings = myFixture.getLookupElementStrings();
		assertNotNull(strings);
		assertDoesntContain(new HashSet<Object>(strings), pattern);
	}

	protected void doFormatTest()
	{
		doFormatTest(getTestName(false));
	}

	protected void doFormatTest(String filename)
	{
		doFormatTest(filename, "");
	}

	protected void doFormatTest(String filename, String resultSuffix)
	{
		initWithFileSmart(filename);
		new WriteCommandAction.Simple(getProject())
		{
			@Override
			protected void run() throws Throwable
			{
				PsiFile file = myFixture.getFile();
				TextRange rangeToUse = file.getTextRange();
				CodeStyleManager.getInstance(getProject()).reformatText(file, rangeToUse.getStartOffset(), rangeToUse.getEndOffset());
				// 	CodeStyleManager.getInstance(getProject()).reformat(myFixture.getFile());
			}
		}.execute();

		String resultFileName = getTestDataPath() + "/" + filename + resultSuffix + ".txt";
		UsefulTestCase.assertSameLinesWithFile(resultFileName, myFixture.getFile().getText());
//		myFixture.checkResultByFile(resultFileName);
	}

}
