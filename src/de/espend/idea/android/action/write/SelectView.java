package de.espend.idea.android.action.write;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.wm.WindowManager;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.util.List;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;

import de.espend.idea.android.AndroidView;
import de.espend.idea.android.action.SelectViewDialog;

/**
 * Created by Administrator on 2016/1/11.
 */
public class SelectView {
    List<AndroidView> mAndroidViews;
    private DefaultTableModel mTableModel;

    private SelectViewDialog mSelectViewDialog;

    public void showSelectDialog(List<AndroidView> androidViews, AnActionEvent anActionEvent) {
        if (mSelectViewDialog == null) {
            mSelectViewDialog = new SelectViewDialog();
        }

        mAndroidViews = androidViews;
        updateTable();
        mSelectViewDialog.setTitle("FindViewByMe");
        mSelectViewDialog.setOnClickListener(onClickListener);
        mSelectViewDialog.pack();
        mSelectViewDialog.setLocationRelativeTo(WindowManager.getInstance().getFrame(anActionEvent.getProject()));
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
                        cellData[i][j] = viewPart.getId();
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
//                    FindViewByMeAction.this.generateCode();
                }
            }
        });

        mSelectViewDialog.setModel(mTableModel);

//        generateCode();
    }

    /**
     * FindViewByMe 对话框回调
     */
    private SelectViewDialog.onClickListener onClickListener = new SelectViewDialog.onClickListener() {
        @Override
        public void onAddRootView() {
//            generateCode();
        }

        @Override
        public void onCopyCode() {
            Clipboard clip = Toolkit.getDefaultToolkit().getSystemClipboard();
            Transferable tText = new StringSelection(mSelectViewDialog.textCode.getText());
            clip.setContents(tText, null);
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
