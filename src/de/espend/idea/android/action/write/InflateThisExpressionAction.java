package de.espend.idea.android.action.write;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.codeStyle.VariableKind;
import com.intellij.psi.search.PsiElementProcessor;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.DocumentUtil;
import com.intellij.util.IncorrectOperationException;
import de.espend.idea.android.AndroidView;
import de.espend.idea.android.utils.AndroidUtils;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class InflateThisExpressionAction extends AbstractIntentionAction {

    private Set<String> mThisSet;

    public InflateThisExpressionAction(PsiLocalVariable psiLocalVariable, PsiFile xmlFile) {
        super(psiLocalVariable, xmlFile);
    }

    public InflateThisExpressionAction(PsiElement psiElement, PsiFile xmlFile) {
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

                PsiStatement psiStatement = PsiTreeUtil.getParentOfType(mPsiElement, PsiStatement.class);
                if(psiStatement == null) {
                    return;
                }

                // collect this.foo = "" and (this.)foo = ""
                // collection already init variables
                mThisSet = new HashSet<String>();
                PsiTreeUtil.processElements(psiStatement.getParent(), new PsiElementProcessor() {

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
                            mThisSet.add(((PsiField) psiField).getName());
                        }
                    }
                });

                showSelectDialog(androidViews, project);
            }
        });

    }

    @NotNull
    @Override
    public String getText() {
        return "Field View Variables";
    }

    @Override
    protected void generateCode(List<AndroidView> androidViews) {
        PsiStatement psiStatement = PsiTreeUtil.getParentOfType(mPsiElement, PsiStatement.class);
        if(psiStatement == null) {
            return;
        }

        // collection class field
        // check if we need to set them
        PsiClass psiClass = PsiTreeUtil.getParentOfType(psiStatement, PsiClass.class);
        Set<String> fieldSet = new HashSet<String>();
        for(PsiField field: psiClass.getFields()) {
            fieldSet.add(field.getName());
        }

        PsiElementFactory elementFactory = JavaPsiFacade.getElementFactory(psiStatement.getProject());

        for (AndroidView v: androidViews) {
            String fieldName = v.getFieldName();
            if(!fieldSet.contains(fieldName)) {
                String sb = "private " + v.getName() + " " + fieldName + ";";
                psiClass.add(elementFactory.createFieldFromText(sb, psiClass));
            }

            if(!mThisSet.contains(fieldName)) {
                String sb1;
                String castCls = "";
                if (!v.isView()) {
                    castCls = "(" + v.getName() + ") ";
                }

                if(mVariableName != null) {
                    sb1 = String.format("this.%s = %s%s.findViewById(%s);", fieldName, castCls, mVariableName, v.getId());
                } else {
                    sb1 = String.format("this.%s = %sfindViewById(%s);", fieldName, castCls, v.getId());
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
        return VariableKind.FIELD;
    }
}
