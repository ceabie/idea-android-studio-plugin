package de.espend.idea.android.action.write;

import com.intellij.codeInsight.actions.ReformatCodeProcessor;
import com.intellij.codeInsight.intention.impl.BaseIntentionAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiLocalVariable;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.codeStyle.SuggestedNameInfo;
import com.intellij.psi.codeStyle.VariableKind;
import com.intellij.util.DocumentUtil;
import com.intellij.util.IncorrectOperationException;
import de.espend.idea.android.AndroidView;
import de.espend.idea.android.action.SelectViewDialog;
import de.espend.idea.android.utils.AndroidUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import java.util.Iterator;
import java.util.List;

/**
 * @author chenxi.
 */
public abstract class AbstractIntentionAction extends BaseIntentionAction {

    final protected PsiFile mXmlFile;
    final protected PsiElement mPsiElement;

    @Nullable
    protected String mVariableName = null;

    private List<AndroidView> mAndroidViews;
    private DefaultTableModel mTableModel;
    private SelectViewDialog mSelectViewDialog;
    public static final String[] HEADERS = new String[]{"selected", "type", "id", "name"};

    public AbstractIntentionAction(PsiLocalVariable psiLocalVariable, PsiFile xmlFile) {
        this.mXmlFile = xmlFile;
        this.mPsiElement = psiLocalVariable;
        this.mVariableName = psiLocalVariable.getName();
    }

    public AbstractIntentionAction(PsiElement psiElement, PsiFile xmlFile) {
        this.mXmlFile = xmlFile;
        this.mPsiElement = psiElement;
    }

    protected abstract PsiElement generateCode(List<AndroidView> androidViews);

    protected abstract VariableKind getVariableKind();

    @NotNull
    @Override
    public String getFamilyName() {
        return "Android Studio Prettify";
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, PsiFile psiFile) throws IncorrectOperationException {
        List<AndroidView> androidViews = AndroidUtils.getIDsFromXML(mXmlFile);
        showSelectDialog(androidViews, project);
    }

    public void showSelectDialog(List<AndroidView> androidViews, Project project) {
        if (mSelectViewDialog == null) {
            mSelectViewDialog = new SelectViewDialog();
        }

        mAndroidViews = androidViews;

        JavaCodeStyleManager codeStyleManager = JavaCodeStyleManager.getInstance(project);

        int size = mAndroidViews.size();
        for (int i = 0; i < size; i++) {
            AndroidView viewPart = mAndroidViews.get(i);
            String name = viewPart.getFieldNameById();
            SuggestedNameInfo nameInfo = codeStyleManager.suggestVariableName(getVariableKind(), name, null, null);
            if (nameInfo != null) {
                name = nameInfo.names[0];
            }

            viewPart.setFiledName(name);
        }

        updateTable();

        mSelectViewDialog.setTitle(getText());
        mSelectViewDialog.setOnClickListener(onClickListener);
        mSelectViewDialog.pack();
        mSelectViewDialog.setLocationRelativeTo(WindowManager.getInstance().getFrame(project));
        mSelectViewDialog.setVisible(true);
    }

    public void updateTable() {
        if (mAndroidViews == null || mAndroidViews.size() == 0) {
            return;
        }

        int size = mAndroidViews.size();
        Object[][] cellData = new Object[size][4];

        for (int i = 0; i < size; i++) {
            AndroidView viewPart = mAndroidViews.get(i);
            for (int j = 0; j < 4; j++) {
                switch (j) {
                    case 0:
                        cellData[i][j] = viewPart.isSelected();
                        break;
                    case 1:
                        cellData[i][j] = viewPart.getClassName();
                        break;
                    case 2:
                        cellData[i][j] = viewPart.getId();
                        break;
                    case 3:
                        cellData[i][j] = viewPart.getFieldName();
                        break;
                }
            }
        }

        mTableModel = new DefaultTableModel(cellData, HEADERS) {
            final Class[] typeArray = {Boolean.class, String.class, String.class, String.class};

            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 0 || column == 3;
            }

            @SuppressWarnings("rawtypes")
            public Class getColumnClass(int column) {
                return typeArray[column];
            }
        };

        mTableModel.addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent event) {
                int row = event.getFirstRow();
                int column = event.getColumn();
                if (column == 0) {
                    Boolean isSelected = (Boolean) mTableModel.getValueAt(row, column);
                    mAndroidViews.get(row).setSelected(isSelected);
                } else if (column == 3) {
                    mAndroidViews.get(row).setFiledName(mTableModel.getValueAt(row, column).toString());
                }
            }
        });

        mSelectViewDialog.setModel(mTableModel);
    }

    /**
     * FindViewByMe 对话框
     */
    private SelectViewDialog.onClickListener onClickListener = new SelectViewDialog.onClickListener() {

        @Override
        public void onGenerateCode() {
            Iterator<AndroidView> iterator = mAndroidViews.iterator();
            while (iterator.hasNext()) {
                AndroidView next = iterator.next();
                if (next != null && !next.isSelected()) {
                    iterator.remove();
                }
            }

            DocumentUtil.writeInRunUndoTransparentAction(new Runnable() {
                @Override
                public void run() {
                    PsiElement psiElement = generateCode(mAndroidViews);
                    if (psiElement != null) {
                        PsiFile psiFile = mPsiElement.getContainingFile();
                        Project project = mPsiElement.getProject();

                        JavaCodeStyleManager styleManager = JavaCodeStyleManager.getInstance(project);
                        styleManager.shortenClassReferences(psiElement);
                        styleManager.optimizeImports(psiFile);

                        new ReformatCodeProcessor(project, psiFile, psiElement.getTextRange(), false).runWithoutProgress();
                    }
                }
            });
        }

        @Override
        public void onSelectAll() {
            for (AndroidView viewPart : mAndroidViews) {
                viewPart.setSelected(true);
            }
            updateTable();
        }

        @Override
        public void onSelectNone() {
            for (AndroidView viewPart : mAndroidViews) {
                viewPart.setSelected(false);
            }
            updateTable();
        }

        @Override
        public void onNegativeSelect() {
            for (AndroidView viewPart : mAndroidViews) {
                viewPart.setSelected(!viewPart.isSelected());
            }
            updateTable();
        }

        @Override
        public void onSwitchIsViewHolder(boolean viewHolder) {
//            isViewHolder = viewHolder;
//            generateCode();
        }

        @Override
        public void onFinish() {
            mAndroidViews = null;
            mSelectViewDialog = null;
        }
    };
}
