package de.espend.idea.android.action.write;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.codeStyle.VariableKind;
import com.intellij.psi.util.PsiElementFilter;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.DocumentUtil;
import com.intellij.util.IncorrectOperationException;
import de.espend.idea.android.AndroidView;
import de.espend.idea.android.utils.AndroidUtils;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class InflateLocalVariableAction extends AbstractIntentionAction {

    public InflateLocalVariableAction(PsiLocalVariable psiLocalVariable, PsiFile xmlFile) {
        super(psiLocalVariable, xmlFile);
    }

    public InflateLocalVariableAction(PsiElement psiElement, PsiFile xmlFile) {
        super(psiElement, xmlFile);
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return "Android Studio Prettify";
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile psiFile) {
        return true;
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, PsiFile psiFile) throws IncorrectOperationException {
        DocumentUtil.writeInRunUndoTransparentAction(new Runnable() {
            @Override
            public void run() {
                List<AndroidView> androidViews = AndroidUtils.getIDsFromXML(mXmlFile);
                showSelectDialog(androidViews, project);
            }
        });
    }

    @NotNull
    @Override
    public String getText() {
        return "Local View Variables";
    }

    @Override
    protected void generateCode(List<AndroidView> androidViews) {
        if (androidViews == null || androidViews.size() == 0) {
            return;
        }

        PsiStatement psiStatement = PsiTreeUtil.getParentOfType(mPsiElement, PsiStatement.class);
        if (psiStatement == null) {
            return;
        }

        PsiElementFactory elementFactory = JavaPsiFacade.getElementFactory(psiStatement.getProject());
        PsiElement[] localVariables = PsiTreeUtil.collectElements(psiStatement.getParent(), new PsiElementFilter() {
            @Override
            public boolean isAccepted(PsiElement element) {
                return element instanceof PsiLocalVariable;
            }
        });

        Set<String> variables = new HashSet<String>();
        for (PsiElement localVariable : localVariables) {
            variables.add(((PsiLocalVariable) localVariable).getName());
        }

        for (AndroidView v : androidViews) {
            String fieldName = v.getFieldName();
            if (v.isSelected() && !variables.contains(fieldName)) {
                String sb1;

                String castCls = "";
                if (!v.isView()) {
                    castCls = "(" + v.getName() + ") ";
                }

                if (mVariableName != null) {
                    sb1 = String.format("%s %s = %s%s.findViewById(%s);", v.getName(), fieldName, castCls, mVariableName, v.getId());
                } else {
                    sb1 = String.format("%s %s = %sfindViewById(%s);", v.getName(), fieldName, castCls, v.getId());
                }

                PsiStatement statementFromText = elementFactory.createStatementFromText(sb1, null);
                psiStatement.getParent().addAfter(statementFromText, psiStatement);
            }
        }

        JavaCodeStyleManager.getInstance(psiStatement.getProject()).shortenClassReferences(psiStatement.getParent());
        new ReformatAndOptimizeImportsProcessor(psiStatement.getProject(), psiStatement.getContainingFile(), true).run();

    }

    @Override
    protected VariableKind getVariableKind() {
        return VariableKind.LOCAL_VARIABLE;
    }

}
