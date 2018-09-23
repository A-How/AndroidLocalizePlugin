package action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import translate.lang.LANG;
import task.GetAndroidStringTask;
import task.TranslateTask;
import module.AndroidString;
import org.jetbrains.annotations.NotNull;
import ui.SelectLanguageDialog;

import java.util.List;

/**
 * @author airsaid
 */
public class ConvertAction extends AnAction implements SelectLanguageDialog.OnClickListener {

    private Project mProject;
    private VirtualFile mSelectFile;
    private List<AndroidString> mAndroidStrings;

    @Override
    public void actionPerformed(AnActionEvent e) {
        mProject = e.getData(CommonDataKeys.PROJECT);
        PsiFile file = e.getData(CommonDataKeys.PSI_FILE);
        GetAndroidStringTask getAndroidStringTask = new GetAndroidStringTask(mProject, "Load strings.xml...", file);
        getAndroidStringTask.setOnGetAndroidStringListener(new GetAndroidStringTask.OnGetAndroidStringListener() {
            @Override
            public void onGetSuccess(@NotNull List<AndroidString> list) {
                if (!isTranslatable(list)) {
                    Messages.showInfoMessage("strings.xml has no text to translate!", "Prompt");
                    return;
                }
                mAndroidStrings = list;
                showSelectLanguageDialog();
            }

            @Override
            public void onGetError(@NotNull Throwable error) {
                Messages.showErrorDialog("Load strings.xml error: " + error, "Error");
            }
        });
        getAndroidStringTask.queue();
    }

    private void showSelectLanguageDialog() {
        SelectLanguageDialog dialog = new SelectLanguageDialog(mProject);
        dialog.setOnClickListener(this);
        dialog.show();
    }

    @Override
    public void update(AnActionEvent e) {
        super.update(e);
        // The translation option is only show when strings.xml is selected.
        mSelectFile = e.getData(CommonDataKeys.VIRTUAL_FILE);
        boolean isSelectStringsFile = isSelectStringsFile(mSelectFile);
        e.getPresentation().setEnabledAndVisible(isSelectStringsFile);
    }

    /**
     * Verify that the selected file is a strings.xml file.
     *
     * @param file selected file
     * @return true: indicating that the selected file is the strings.xml file.
     */
    private boolean isSelectStringsFile(VirtualFile file) {
        if (file == null) return false;

        VirtualFile parent = file.getParent();
        if (parent == null) return false;

        String parentName = parent.getName();
        if (!"values".equals(parentName) && !"values-en".equals(parentName)) return false;

        return "strings.xml".equals(file.getName());
    }

    /**
     * Verify that there is a text in the strings.xml file that needs to be translated.
     *
     * @param list strings.xml text list.
     * @return true: there is text that needs to be translated.
     */
    private boolean isTranslatable(@NotNull List<AndroidString> list) {
        boolean isTranslate = false;
        for (AndroidString androidString : list) {
            if (androidString.isTranslatable()) {
                isTranslate = true;
                break;
            }
        }
        return isTranslate;
    }

    @Override
    public void onClickListener(List<LANG> selectLanguage) {
        TranslateTask translationTask = new TranslateTask(
                mProject, "In translation...", selectLanguage, mAndroidStrings, mSelectFile);
        translationTask.setOnTranslateListener(new TranslateTask.OnTranslateListener() {
            @Override
            public void onTranslateSuccess() {}

            @Override
            public void onTranslateError(Throwable e) {
                Messages.showErrorDialog("Translate error: " + e, "Error");
            }
        });
        translationTask.queue();
    }
}
