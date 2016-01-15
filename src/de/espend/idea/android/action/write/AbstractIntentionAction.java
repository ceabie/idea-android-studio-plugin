package de.espend.idea.android.action.write;

import com.intellij.codeInsight.intention.impl.BaseIntentionAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiLocalVariable;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.codeStyle.SuggestedNameInfo;
import com.intellij.psi.codeStyle.VariableKind;
import de.espend.idea.android.AndroidView;
import de.espend.idea.android.action.SelectViewDialog;
import org.jetbrains.annotations.Nullable;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
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
    private JavaCodeStyleManager mCodeStyleManager;

    public AbstractIntentionAction(PsiLocalVariable psiLocalVariable, PsiFile xmlFile) {
        this.mXmlFile = xmlFile;
        this.mPsiElement = psiLocalVariable;
        this.mVariableName = psiLocalVariable.getName();
    }

    public AbstractIntentionAction(PsiElement psiElement, PsiFile xmlFile) {
        this.mXmlFile = xmlFile;
        this.mPsiElement = psiElement;
    }

    protected abstract void generateCode(List<AndroidView> androidViews);

    protected abstract VariableKind getVariableKind();

    public void showSelectDialog(List<AndroidView> androidViews, Project project) {
        if (mSelectViewDialog == null) {
            mSelectViewDialog = new SelectViewDialog();
        }

        mCodeStyleManager = JavaCodeStyleManager.getInstance(project);
        mAndroidViews = androidViews;

        updateTable();

        mSelectViewDialog.setTitle("Select Views");
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
        String[] headers = {"selected", "type", "id", "name"};
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
                        String name = viewPart.getFieldNameOrg();
                        SuggestedNameInfo nameInfo = mCodeStyleManager.suggestVariableName(getVariableKind(), name, null, null);
                        if (nameInfo != null) {
                            name = nameInfo.names[0];
                        }

                        viewPart.setFiledName(name);
                        cellData[i][j] = name;
                        break;
                }
            }
        }

        mTableModel = new DefaultTableModel(cellData, headers) {
            final Class[] typeArray = {Boolean.class, Object.class, Object.class, Object.class};

            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 0;
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
                }
            }
        });

        mSelectViewDialog.setModel(mTableModel);
    }

    /**
     * FindViewByMe 对话框回�?
     */
    private SelectViewDialog.onClickListener onClickListener = new SelectViewDialog.onClickListener() {
        @Override
        public void onAddRootView() {
//            generateCode();
        }

        @Override
        public void onGenerateCode() {
            generateCode(mAndroidViews);
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
        public void onSwitchAddRootView(boolean flag) {
//            isAddRootView = flag;
        }

        @Override
        public void onSwitchAddM(boolean isAddM) {
//            switchViewName(isAddM);
        }

        @Override
        public void onSwitchIsViewHolder(boolean viewHolder) {
//            isViewHolder = viewHolder;
//            generateCode();
        }

        @Override
        public void onFinish() {
            mAndroidViews = null;
//            viewSaxHandler = null;
            mSelectViewDialog = null;
        }
    };
}
