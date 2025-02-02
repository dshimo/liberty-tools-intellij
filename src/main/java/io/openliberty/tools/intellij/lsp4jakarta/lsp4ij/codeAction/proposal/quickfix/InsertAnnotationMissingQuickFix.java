/*******************************************************************************
 * Copyright (c) 2020, 2023 Red Hat Inc. and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *     IBM Corporation
 *******************************************************************************/
package io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.codeAction.proposal.quickfix;

import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.codeaction.JavaCodeActionContext;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.corrections.proposal.ChangeCorrectionProposal;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.corrections.proposal.InsertAnnotationProposal;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.Diagnostic;

import java.util.*;
import java.util.logging.Logger;

/**
 * QuickFix for inserting annotations.
 * Reused from https://github.com/eclipse/lsp4mp/blob/6f2d700a88a3262e39cc2ba04beedb429e162246/microprofile.jdt/org.eclipse.lsp4mp.jdt.core/src/main/java/org/eclipse/lsp4mp/jdt/core/java/codeaction/InsertAnnotationMissingQuickFix.java
 *
 * @author Angelo ZERR
 *
 */
public class InsertAnnotationMissingQuickFix {
    private static final Logger LOGGER = Logger.getLogger(InsertAnnotationMissingQuickFix.class.getName());
    private static final String ANNOTATION_KEY = "annotation";

    private final String[] annotations;

    private final boolean generateOnlyOneCodeAction;

    /**
     * Constructor for insert annotation quick fix.
     *
     * <p>
     * The participant will generate a CodeAction per annotation.
     * </p>
     *
     * @param annotations list of annotation to insert.
     */
    public InsertAnnotationMissingQuickFix(String... annotations) {
        this(false, annotations);
    }

    /**
     * Constructor for insert annotation quick fix.
     *
     * @param generateOnlyOneCodeAction true if the participant must generate a
     *                                  CodeAction which insert the list of
     *                                  annotation and false otherwise.
     * @param annotations               list of annotation to insert.
     */
    public InsertAnnotationMissingQuickFix(boolean generateOnlyOneCodeAction, String... annotations) {
        this.generateOnlyOneCodeAction = generateOnlyOneCodeAction;
        this.annotations = annotations;
    }

    public List<? extends CodeAction> getCodeActions(JavaCodeActionContext context, Diagnostic diagnostic) {
        List<CodeAction> codeActions = new ArrayList<>();
        insertAnnotations(diagnostic, context, codeActions);
        return codeActions;
    }

    protected static PsiModifierListOwner getBinding(PsiElement node) {
        PsiModifierListOwner binding = PsiTreeUtil.getParentOfType(node, PsiVariable.class);
        if (binding != null) {
            return binding;
        }
        binding = PsiTreeUtil.getParentOfType(node, PsiMethod.class);
        if (binding != null) {
            return binding;
        }
        return PsiTreeUtil.getParentOfType(node, PsiClass.class);
    }

    protected String[] getAnnotations() {
        return this.annotations;
    }

    protected void insertAnnotations(Diagnostic diagnostic, JavaCodeActionContext context,
                                     List<CodeAction> codeActions) {
        if (generateOnlyOneCodeAction) {
            insertAnnotation(diagnostic, context, codeActions, annotations);
        } else {
            for (String annotation : annotations) {
                JavaCodeActionContext annotationContext = context.copy();
                insertAnnotation(diagnostic, annotationContext, codeActions, annotation);
            }
        }
    }

    protected void insertAnnotation(Diagnostic diagnostic, JavaCodeActionContext context,
            List<CodeAction> codeActions, String... annotations) {
        String name = getLabel(annotations);
        PsiElement node = context.getCoveringNode();
        PsiModifierListOwner parentType = getBinding(node);

        ChangeCorrectionProposal proposal = new InsertAnnotationProposal(name, context.getCompilationUnit(),
                context.getASTRoot(), parentType, 0, context.getSource().getCompilationUnit(),
                annotations);
        CodeAction codeAction = context.convertToCodeAction(proposal, diagnostic);
        if (codeAction != null) {
            codeActions.add(codeAction);
        }
    }

    private static String getLabel(String[] annotations) {
        StringBuilder name = new StringBuilder("Insert ");
        for (int i = 0; i < annotations.length; i++) {
            String annotation = annotations[i];
            String annotationName = annotation.substring(annotation.lastIndexOf('.') + 1, annotation.length());
            if (i > 0) {
                name.append(", ");
            }
            name.append("@");
            name.append(annotationName);
        }
        return name.toString();
    }
}
