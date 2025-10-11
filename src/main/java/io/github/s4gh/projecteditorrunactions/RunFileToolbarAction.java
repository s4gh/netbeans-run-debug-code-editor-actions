package io.github.s4gh.projecteditorrunactions;

import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.ref.WeakReference;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.SwingUtilities;
import javax.swing.text.Document;

import org.netbeans.api.editor.EditorRegistry;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.spi.project.ActionProvider;

import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.awt.Actions;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.util.ContextAwareAction;
import org.openide.util.ImageUtilities;
import org.openide.util.Lookup;
import org.openide.util.NbBundle.Messages;
import org.openide.util.WeakListeners;
import org.openide.util.actions.Presenter;
import org.openide.util.lookup.Lookups;
import org.openide.windows.TopComponent;

@ActionID(
    category = "Editor",
    id = "io.github.s4gh.projecteditorrunactions.RunFileToolbarAction"
)
@ActionRegistration(
    displayName = "#CTL_RunFileToolbarAction",
    lazy = false
)
@ActionReference(
    path = "Editors/Toolbars/Default",
    position = 1600
)
@Messages("CTL_RunFileToolbarAction=Run File (Shift+F6)")
public final class RunFileToolbarAction extends AbstractAction
        implements ContextAwareAction, Presenter.Toolbar {

    private final PropertyChangeListener editorRegListener = new PropertyChangeListener() {
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            updateEnabledOnEDT();
        }
    };

    private final PropertyChangeListener winsysListener = new PropertyChangeListener() {
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            updateEnabledOnEDT();
        }
    };

    /** Weak reference to the toolbar button created for this action instance. */
    private volatile WeakReference<JButton> toolbarButtonRef = new WeakReference<>(null);

    public RunFileToolbarAction() {
        this(null);
    }

    /** We ignore the provided context intentionally; we always derive state from the active editor. */
    private RunFileToolbarAction(Lookup ignoredContext) {
        super(Bundle.CTL_RunFileToolbarAction());

        ImageIcon icon = ImageUtilities.loadImageIcon("icons/run.svg", false);
        putValue(Action.SMALL_ICON, icon);
//        putValue(Action.SHORT_DESCRIPTION, "Run File");
        // Hide text on toolbar buttons when connected
        putValue("hideActionText", Boolean.TRUE);

        // Listen for editor focus/tab changes.
        EditorRegistry.addPropertyChangeListener(
            WeakListeners.propertyChange(editorRegListener, EditorRegistry.class)
        );

        // Also listen to window system changes (e.g., activated TopComponent changes).
        TopComponent.Registry reg = TopComponent.getRegistry();
        reg.addPropertyChangeListener(
            WeakListeners.propertyChange(winsysListener, reg)
        );

        // Initial state
        updateEnabledOnEDT();
    }

    private void updateEnabledOnEDT() {
        if (SwingUtilities.isEventDispatchThread()) {
            updateEnabled();
        } else {
            SwingUtilities.invokeLater(this::updateEnabled);
        }
    }

    private static DataObject currentEditorDataObject() {
        JEditorPane pane = (JEditorPane) EditorRegistry.lastFocusedComponent();
        if (pane == null) {
            return null;
        }
        Document doc = pane.getDocument();
        if (doc == null) {
            return null;
        }

        Object sdp = doc.getProperty(Document.StreamDescriptionProperty);
        if (sdp instanceof DataObject) {
            return (DataObject) sdp;
        } else if (sdp instanceof FileObject) {
            try {
                return DataObject.find((FileObject) sdp);
            } catch (DataObjectNotFoundException ex) {
                return null;
            }
        }
        return null;
    }

    private void updateEnabled() {
        DataObject dobj = currentEditorDataObject();

        boolean canRun = false;
        if (dobj != null) {
            FileObject fo = dobj.getPrimaryFile();
            Project prj = FileOwnerQuery.getOwner(fo);
            if (prj != null) {
                ActionProvider ap = prj.getLookup().lookup(ActionProvider.class);
                if (ap != null) {
                    Lookup actionCtx = Lookups.fixed(dobj);
                    try {
                        canRun = ap.isActionEnabled(ActionProvider.COMMAND_RUN_SINGLE, actionCtx);
                    } catch (Exception ex) {
                        canRun = false;
                    }
                }
            }
        }
        setEnabled(canRun);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // (Optional) access the invoking button from the event source
        if (e != null && e.getSource() instanceof JButton) {
            // Store the last invoker as the toolbar button reference
            toolbarButtonRef = new WeakReference<>((JButton) e.getSource());
        }

        DataObject dobj = currentEditorDataObject();
        if (dobj == null) {
            return;
        }
        FileObject fo = dobj.getPrimaryFile();
        Project prj = FileOwnerQuery.getOwner(fo);
        if (prj == null) {
            return;
        }
        ActionProvider ap = prj.getLookup().lookup(ActionProvider.class);
        if (ap == null) {
            return;
        }

        // Run on the EDT like ActionProvider expects
        SwingUtilities.invokeLater(() -> {
            Lookup actionCtx = Lookups.fixed(dobj);
            if (ap.isActionEnabled(ActionProvider.COMMAND_RUN_SINGLE, actionCtx)) {
                ap.invokeAction(ActionProvider.COMMAND_RUN_SINGLE, actionCtx);
            }
        });
    }

    /** Provide a context-bound instance for editor/toolbar usage.
     *  We intentionally ignore the Lookup and still bind to the editor. */
    @Override
    public Action createContextAwareInstance(Lookup actionContext) {
        return new RunFileToolbarAction(actionContext);
    }

    // ===== Presenter.Toolbar =====

    @Override
    public JComponent getToolbarPresenter() {
        JButton btn = toolbarButtonRef.get();
        if (btn == null) {
            btn = new JButton();
            // Binds icon, text, tooltip, and enabled state to the Action
            Actions.connect(btn, this);
            btn.setFocusable(false);
            btn.putClientProperty("hideActionText", Boolean.TRUE);
            toolbarButtonRef = new WeakReference<>(btn);
        }
        return btn;
    }

    /** Returns the (most recently created or used) toolbar JButton, if still alive. */
    public JButton getToolbarButton() {
        return toolbarButtonRef.get();
    }
}