package de.espend.idea.android.action.write;

import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.VariableKind;
import com.intellij.psi.search.PsiElementProcessor;
import com.intellij.psi.util.PsiTreeUtil;
import de.espend.idea.android.AndroidView;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class InflateThisExpressionAction extends AbstractIntentionAction {

    public InflateThisExpressionAction(PsiLocalVariable psiLocalVariable, PsiFile xmlFile) {
        super(psiLocalVariable, xmlFile);
    }

    public InflateThisExpressionAction(PsiElement psiElement, PsiFile xmlFile) {
        super(psiElement, xmlFile);
    }

    @NotNull
    @Override
    public String getText() {
        return "Field View Variables";
    }

    @Override
    protected VariableKind getVariableKind() {
        return VariableKind.FIELD;
    }

    @Override
    protected PsiElement generateCode(List<AndroidView> androidViews) {
        if (androidViews == null || androidViews.size() == 0) {
            return null;
        }

        PsiStatement psiStatement = PsiTreeUtil.getParentOfType(mPsiElement, PsiStatement.class);
        if (psiStatement == null) {
            return null;
        }

        PsiElement psiParent = psiStatement.getParent();

        // collect this.foo = "" and (this.)foo = ""
        // collection already init variables
        Set<String> thisSet = getThisFields(psiParent);

        // collection local variables
        Set<String> localVariables = getLocalVariables(psiParent);

        // collection class field
        // check if we need to set them
        PsiClass psiClass = PsiTreeUtil.getParentOfType(psiStatement, PsiClass.class);
        Set<String> fieldSet = new HashSet<String>();
        for (PsiField field : psiClass.getFields()) {
            fieldSet.add(field.getName());
        }

        Project project = psiStatement.getProject();
        PsiElementFactory elementFactory = JavaPsiFacade.getElementFactory(project);

        for (AndroidView v : androidViews) {
            String fieldName = v.getFieldName();

            if (!fieldSet.contains(fieldName)) {
                String sb = "private " + v.getName() + " " + fieldName + ";";
                psiClass.add(elementFactory.createFieldFromText(sb, psiClass));
            }

            if (!thisSet.contains(fieldName)) {
                StringBuilder sbStatement = new StringBuilder();

                // check local variable
                if (localVariables.contains(fieldName)) {
                    sbStatement.append("this.");
                }

                sbStatement.append(fieldName).append(" = ");

                // if View element not cast
                if (v.isView()) {
                    sbStatement.append(v.getName());
                } else {
                    sbStatement.append('(').append(v.getName()).append(')');
                }

                // add layout variable
                if (mVariableName != null) {
                    sbStatement.append(mVariableName).append('.');
                }

                sbStatement.append("findViewById(").append(v.getId()).append(");");

                PsiStatement statementFromText = elementFactory.createStatementFromText(sbStatement.toString(), null);
                psiParent.addAfter(statementFromText, psiStatement);
            }
        }

        return psiParent;
    }

    @NotNull
    private static Set<String> getThisFields(PsiElement psiParent) {
        Set<String> thisSet = new HashSet<String>();
        PsiTreeUtil.processElements(psiParent, new PsiElementProcessor() {

            @Override
            public boolean execute(@NotNull PsiElement element) {

                if (element instanceof PsiThisExpression) {
                    attachFieldName(element.getParent());
                } else if (element instanceof PsiAssignmentExpression) {
                    attachFieldName(((PsiAssignmentExpression) element).getLExpression());
                }

                return true;
            }

            private void attachFieldName(PsiElement psiExpression) {

                if (!(psiExpression instanceof PsiReferenceExpression)) {
                    return;
                }

                PsiElement psiField = ((PsiReferenceExpression) psiExpression).resolve();
                if (psiField instanceof PsiField) {
                    thisSet.add(((PsiField) psiField).getName());
                }
            }
        });

        return thisSet;
    }
}
