/*******************************************************************************
 * Copyright (c) 2019-2020 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package io.openliberty.tools.intellij.lsp4mp4ij.psi.internal.core.ls;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.JsonRpcHelpers;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.PsiUtils;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.utils.IPsiUtils;
import io.openliberty.tools.intellij.lsp4mp.lsp4ij.LSPIJUtils;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4mp.commons.ClasspathKind;
import org.eclipse.lsp4mp.commons.DocumentFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.net.URL;
import java.util.Scanner;

/**
 * {@link IPsiUtils} implementation.
 *
 * @see <a href="https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.core/src/main/java/com/redhat/microprofile/jdt/internal/core/ls/JDTUtilsLSImpl.java">https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.core/src/main/java/com/redhat/microprofile/jdt/internal/core/ls/JDTUtilsLSImpl.java</a>
 */
public class PsiUtilsLSImpl implements IPsiUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(PsiUtilsLSImpl.class);
    private final Project project;
    private final Module module;

    public static IPsiUtils getInstance(Project project) {
        return new PsiUtilsLSImpl(project);
    }

    private PsiUtilsLSImpl(Project project, Module module) {
        this.project = project;
        this.module = module;
    }

    private PsiUtilsLSImpl(Project project) {
        this(project, null);
    }

    public Project getProject() {
        return project;
    }

    public Module getModule() {
        return module;
    }

    public IPsiUtils refine(Module module) {
        return new PsiUtilsLSImpl(module.getProject(), module);
    }

    @Override
    public Module getModule(VirtualFile file) {
        if (file != null) {
            return ProjectFileIndex.getInstance(project).getModuleForFile(file, false);
        }
        return null;
    }

    @Override
    public Module getModule(String uri) throws IOException {
        VirtualFile file = findFile(uri);
        return file!=null?getModule(file):null;
    }

    @Override
    public PsiClass findClass(Module module, String className) {
        JavaPsiFacade facade = JavaPsiFacade.getInstance(module.getProject());
        return facade.findClass(className, GlobalSearchScope.allScope(module.getProject()));
    }

    @Override
    public void discoverSource(PsiFile classFile) {
        //TODO: implements discoverSource
    }

    @Override
    public Location toLocation(PsiElement psiMember) {
        PsiElement sourceElement = psiMember instanceof PsiNameIdentifierOwner ? ((PsiNameIdentifierOwner) psiMember).getNameIdentifier().getNavigationElement() : psiMember.getNavigationElement();
        if (sourceElement != null) {
            PsiFile file = sourceElement.getContainingFile();
            Location location = new Location();
            location.setUri(VfsUtilCore.convertToURL(file.getVirtualFile().getUrl()).toExternalForm());
            Document document = PsiDocumentManager.getInstance(psiMember.getProject()).getDocument(file);
            TextRange range = sourceElement.getTextRange();
            int startLine = document.getLineNumber(range.getStartOffset());
            int startLineOffset = document.getLineStartOffset(startLine);
            int endLine = document.getLineNumber(range.getEndOffset());
            int endLineOffset = document.getLineStartOffset(endLine);
            location.setRange(new Range(LSPIJUtils.toPosition(range.getStartOffset(), document), LSPIJUtils.toPosition(range.getEndOffset(), document)));
            return location;
        }
        return null;
    }

    @Override
    public VirtualFile findFile(String uri) throws IOException {
        //return LocalFileSystem.getInstance().refreshAndFindFileByIoFile(Paths.get(new URI(uri)).toFile());
        return VfsUtil.findFileByURL(new URL(uri));
    }

    @Override
    public String getJavadoc(PsiMethod method, DocumentFormat documentFormat) {
//        boolean markdown = DocumentFormat.Markdown.equals(documentFormat);
//        Reader reader = markdown ? JavadocContentAccess.getMarkdownContentReader(method)
//                : JavadocContentAccess.getPlainTextContentReader(method);
//        return reader != null ? toString(reader) : null;
        // TODO not needed for lsp4mp
        return null;
    }

    // @Override
    // public String getJavadoc(PsiMember method, com.redhat.qute.commons.DocumentFormat documentFormat) {
    //     boolean markdown = DocumentFormat.Markdown.equals(documentFormat);
    //     Reader reader = markdown ? JavadocContentAccess.getMarkdownContentReader(method)
    //             : JavadocContentAccess.getPlainTextContentReader(method);
    //     return reader != null ? toString(reader) : null;
    // }

    private static String toString(Reader reader) {
        try (Scanner s = new Scanner(reader)) {
            s.useDelimiter("\\A");
            return s.hasNext() ? s.next() : "";
        }
    }

    @Override
    public Range toRange(PsiElement element, int offset, int length) {
        return PsiUtils.toRange(element, offset, length);
    }

    @Override
    public Range toRange(Document document, int offset, int length) {
        return PsiUtils.toRange(document, offset, length);
    }

    @Override
    public int toOffset(Document document, int line, int character) {
        return JsonRpcHelpers.toOffset(document, line, character);
    }

    @Override
    public int toOffset(PsiFile file, int line, int character) {
        Document document = PsiDocumentManager.getInstance(file.getProject()).getDocument(file);
        return document!=null?toOffset(document, line, character):0;
    }

    @Override
    public PsiFile resolveCompilationUnit(String uri) {
        try {
            VirtualFile file = findFile(uri);
            if (file != null) {
                return PsiManager.getInstance(getModule(file).getProject()).findFile(file);
            }
        } catch (IOException e) {
            LOGGER.error(e.getLocalizedMessage(), e);
        }
        return null;
    }

    @Override
    public PsiFile resolveClassFile(String uri) {
        return resolveCompilationUnit(uri);
    }

    public static ClasspathKind getClasspathKind(VirtualFile file, Module module) {
        return ModuleRootManager.getInstance(module).getFileIndex().isInTestSourceContent(file)?ClasspathKind.TEST:ClasspathKind.SRC;
    }

    public static String getProjectURI(Module module) {
        return module.getModuleFilePath();
    }

    @Override
    public String toUri(PsiFile typeRoot) {
        return VfsUtil.toUri(typeRoot.getVirtualFile().getUrl()).toString();
    }

    @Override
    public boolean isHiddenGeneratedElement(PsiElement element) {
        return PsiUtils.isHiddenGeneratedElement(element);
    }
}
