package de.espend.idea.android.utils;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.search.EverythingGlobalScope;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlTag;
import de.espend.idea.android.AndroidView;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;


public class AndroidUtils {

    @Nullable
    public static PsiFile findXmlResource(@Nullable PsiReferenceExpression referenceExpression) {
        if (referenceExpression == null) return null;

        PsiElement firstChild = referenceExpression.getFirstChild();
        if (firstChild == null || !"R.layout".equals(firstChild.getText())) {
            return null;
        }

        PsiElement lastChild = referenceExpression.getLastChild();
        if(lastChild == null) {
            return null;
        }

        String name = String.format("%s.xml", lastChild.getText());
        PsiFile[] foundFiles = FilenameIndex.getFilesByName(referenceExpression.getProject(), name, GlobalSearchScope.allScope(referenceExpression.getProject()));
        if (foundFiles.length <= 0) {
            return null;
        }

        return foundFiles[0];
    }

    public static List<AndroidView> getProjectViews(Project project) {

        List<AndroidView> androidViews = new ArrayList<AndroidView>();
        for(PsiFile psiFile: getLayoutFiles(project)) {
            androidViews.addAll(getIDsFromXML(psiFile));
        }

        return androidViews;
    }

    public static List<PsiFile> getLayoutFiles(Project project) {

        List<PsiFile> psiFileList = new ArrayList<PsiFile>();

        for (VirtualFile virtualFile : FilenameIndex.getAllFilesByExt(project, "xml")) {
            VirtualFile parent = virtualFile.getParent();
            if (parent != null && "layout".equals(parent.getName())) {
                String relative = VfsUtil.getRelativePath(virtualFile, project.getBaseDir(), '/');
                if (relative != null) {
                    PsiFile psiFile = PsiManager.getInstance(project).findFile(virtualFile);
                    if (psiFile != null) {
                        psiFileList.add(psiFile);
                    }
                }
            }
        }

        return psiFileList;
    }

    @Nullable
    public static PsiFile findXmlResource(Project project, String layoutName) {

        if (!layoutName.startsWith("R.layout.")) {
            return null;
        }

        layoutName = layoutName.substring("R.layout.".length());

        String name = String.format("%s.xml", layoutName);
        PsiFile[] foundFiles = FilenameIndex.getFilesByName(project, name, GlobalSearchScope.allScope(project));
        if (foundFiles.length <= 0) {
            return null;
        }

        return foundFiles[0];
    }

    @NotNull
    public static List<AndroidView> getIDsFromXML(@NotNull PsiFile file) {
        ArrayList<AndroidView> ret = new ArrayList<AndroidView>();
        return getIDsFromXML(file, ret);
    }

    private static List<AndroidView> getIDsFromXML(@NotNull PsiFile file, ArrayList<AndroidView> ret) {
        file.accept(new XmlRecursiveElementVisitor() {
            @Override
            public void visitElement(final PsiElement element) {
                super.visitElement(element);
                if (element instanceof XmlTag) {
                    XmlTag tag = (XmlTag) element;

                    if (tag.getName().equalsIgnoreCase("include")) {
                        XmlAttribute layout = tag.getAttribute("layout", null);

                        if (layout != null) {
                            Project project = file.getProject();
                            PsiFile include = findLayoutResource(file, project, getLayoutName(layout.getValue()));

                            if (include != null) {
                                getIDsFromXML(include, ret);
                                return;
                            }
                        }
                    }

                    // get element ID
                    XmlAttribute id = tag.getAttribute("android:id", null);
                    if (id == null) {
                        return;
                    }
                    final String val = id.getValue();
                    if (val == null) {
                        return;
                    }

                    // check if there is defined custom class
                    String name = tag.getName();
                    XmlAttribute clazz = tag.getAttribute("class", null);
                    if (clazz != null) {
                        name = clazz.getValue();
                    }

                    ret.add(new AndroidView(val, name, id));
                }

            }
        });

        return ret;
    }

    /**
     * Try to find layout XML file by name
     *
     * @param file
     * @param project
     * @param fileName
     * @return
     */
    public static PsiFile findLayoutResource(PsiFile file, Project project, String fileName) {
        String name = String.format("%s.xml", fileName);
        // restricting the search to the module of layout that includes the layout we are seaching for
        return resolveLayoutResourceFile(file, project, name);
    }

    private static PsiFile resolveLayoutResourceFile(PsiElement element, Project project, String name) {
        // restricting the search to the current module - searching the whole project could return wrong layouts
        Module module = ModuleUtil.findModuleForPsiElement(element);
        PsiFile[] files = null;
        if (module != null) {
            GlobalSearchScope moduleScope = module.getModuleWithDependenciesAndLibrariesScope(false);
            files = FilenameIndex.getFilesByName(project, name, moduleScope);
        }
        if (files == null || files.length <= 0) {
            // fallback to search through the whole project
            // useful when the project is not properly configured - when the resource directory is not configured
            files = FilenameIndex.getFilesByName(project, name, new EverythingGlobalScope(project));
            if (files.length <= 0) {
                return null; //no matching files
            }
        }

        // TODO - we have a problem here - we still can have multiple layouts (some coming from a dependency)
        // we need to resolve R class properly and find the proper layout for the R class
        return files[0];
    }

    /**
     * Get layout name from XML identifier (@layout/....)
     *
     * @param layout
     * @return
     */
    public static String getLayoutName(String layout) {
        if (layout == null || !layout.startsWith("@") || !layout.contains("/")) {
            return null; // it's not layout identifier
        }

        int pos = layout.indexOf('/');

        if (pos == -1 || pos == 0 || pos == layout.length() - 1) {
            return null; // not enough parts
        }

        return layout.substring(pos + 1);
    }

    @Nullable
    public static AndroidView getViewType(@NotNull PsiFile f, String findId) {

        // @TODO: replace dup for
        List<AndroidView> views = getIDsFromXML(f);

        for(AndroidView view: views) {
            if(findId.equals(view.getId())) {
                return view;
            }
        }

        return null;
    }

}
