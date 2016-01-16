package de.espend.idea.android;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AndroidView {

    private String mResId;
    private String mId;
    private String mName;
    private String mClassName;
    private PsiElement mXmlTarget;
    private boolean mIsView;

    private boolean mSelected = true;
    private String mFiledName;

    public boolean isView() {
        return mIsView;
    }

    // @android:id/ @+id/ @id/
    private static Pattern IdPattern = Pattern.compile("@(([\\w.]*?):?|\\+)?id/(\\w+)");

    public AndroidView(@NotNull String id, @NotNull String className, PsiElement xmlTarget) {
        mXmlTarget = xmlTarget;

        Matcher matcher = IdPattern.matcher(id);
        if (matcher.find()) {
            String packageStr = matcher.group(2);
            mId = matcher.group(3);

            if (packageStr != null && packageStr.length() > 0) {
                mResId = packageStr + ".R.id." + mId;
            } else {
                mResId = "R.id." + mId;
            }
        }
//        if (id.startsWith("@+id/")) {
//            mId = id.substring("@+id/".length());
//            mResId = ("R.id." + mId);
//        } else if (id.contains(":id/")) {
//
//            String[] s = id.split(":id/");
//            String packageStr = s[0].substring(1, s[0].length());
//            mId = s[1];
//            mResId = (packageStr + ".R.id." + mId);
//        }

        mClassName = className;

        if (className.contains(".")) {
            mName = className;
        } else {
            mIsView = "View".equals(className);
            if (mIsView || "ViewGroup".equals(className) || "SurfaceView".equals(className)
                    || "ViewStub".equals(className) || "TextureView".equals(className)) {
                mName = "android.view." + className;
            } else {
                mName = "android.widget." + className;
            }
        }
    }

    public PsiElement getXmlTarget() {
        return mXmlTarget;
    }

    public String getClassName() {
        return mClassName;
    }

    public String getId() {
        return mResId;
    }

    public String getName() {
        return mName;
    }

    public void setFiledName(String filedName) {
        mFiledName = filedName;
    }

    public String getFieldName() {
        return mFiledName;
    }

    public String getFieldNameById() {
        String filedNameById = mId;
        if (filedNameById.contains("_")) {
            try {
                String[] words = filedNameById.split("_");
                StringBuilder fieldName = new StringBuilder();
                for (String word : words) {
                    if (word != null && word.length() > 0) {
                        char[] ws = word.toCharArray();
                        if (ws[0] >= 'a' && ws[0] <= 'z') {
                            ws[0] += ('A' - 'a');
                        }
                        fieldName.append(ws);
                    }
                }
                filedNameById = fieldName.toString();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return filedNameById;
    }

    public boolean isSelected() {
        return mSelected;
    }

    public void setSelected(boolean selected) {
        mSelected = selected;
    }
}