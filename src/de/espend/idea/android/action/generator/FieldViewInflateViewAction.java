package de.espend.idea.android.action.generator;

import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiLocalVariable;
import de.espend.idea.android.action.write.InflateThisExpressionAction;
import de.espend.idea.android.annotator.InflateViewAnnotator;
import org.jetbrains.annotations.NotNull;

public class FieldViewInflateViewAction extends AbstractInflateViewAction {

    @Override
    public void generate(InflateViewAnnotator.InflateContainer inflateContainer, Editor editor, @NotNull PsiFile file) {
        PsiLocalVariable psiLocalVariable = inflateContainer.getPsiLocalVariable();
        PsiFile xmlFile = inflateContainer.getXmlFile();

        new InflateThisExpressionAction(psiLocalVariable, xmlFile).invoke(file.getProject(), editor, file);
    }

}
