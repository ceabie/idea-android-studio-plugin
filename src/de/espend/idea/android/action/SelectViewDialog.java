package de.espend.idea.android.action;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.DefaultTableModel;
import java.awt.event.*;

public class SelectViewDialog extends JDialog {
    private JPanel contentPane;
    public JButton btnGenerator;
    public JButton btnClose;
    public JTable tableViews;
    public JButton btnSelectAll;
    public JButton btnSelectNone;
    public JButton btnNegativeSelect;
    private JCheckBox chbIsViewHolder;
    private onClickListener onClickListener;

    public SelectViewDialog() {
        setContentPane(contentPane);
        setModal(true);

        tableViews.setRowHeight(26);

        btnGenerator.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (onClickListener != null) {
                    onClickListener.onGenerateCode();
                }
                onCancel();
            }
        });

        chbIsViewHolder.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                if (onClickListener != null) {
                    onClickListener.onSwitchIsViewHolder(chbIsViewHolder.isSelected());
                }
            }
        });

        btnClose.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                SelectViewDialog.this.onCancel();
            }
        });

        btnSelectAll.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (onClickListener != null) {
                    onClickListener.onSelectAll();
                }
            }
        });

        btnSelectNone.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (onClickListener != null) {
                    onClickListener.onSelectNone();
                }
            }
        });

        btnNegativeSelect.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (onClickListener != null) {
                    onClickListener.onNegativeSelect();
                }
            }
        });

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        contentPane.registerKeyboardAction(
                new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        SelectViewDialog.this.onCancel();
                    }
                },
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

    }

    private void onCancel() {
        dispose();
        if (onClickListener != null) {
            onClickListener.onFinish();
        }
    }

    public interface onClickListener {

        void onGenerateCode();

        void onSelectAll();

        void onSelectNone();

        void onNegativeSelect();

        void onSwitchIsViewHolder(boolean isViewHolder);

        void onFinish();
    }

    public void setOnClickListener(SelectViewDialog.onClickListener onClickListener) {
        this.onClickListener = onClickListener;
    }

    public void setModel(DefaultTableModel model) {
        tableViews.setModel(model);
        tableViews.getColumnModel().getColumn(0).setPreferredWidth(20);
    }
}
