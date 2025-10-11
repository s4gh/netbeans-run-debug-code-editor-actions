//package io.github.s4gh.projecteditorrunactions;
//
//import java.awt.event.ActionEvent;
//import java.awt.event.InputEvent;
//import java.awt.event.KeyEvent;
//import java.beans.PropertyChangeEvent;
//import java.beans.PropertyChangeListener;
//import javax.swing.AbstractAction;
//import javax.swing.Action;
//import javax.swing.ImageIcon;
//import javax.swing.JButton;
//import javax.swing.SwingUtilities;
//import javax.swing.JEditorPane;
//import javax.swing.KeyStroke;
//import javax.swing.text.Document;
//
//import org.netbeans.api.editor.EditorRegistry;
//import org.netbeans.api.project.FileOwnerQuery;
//import org.netbeans.api.project.Project;
//import org.netbeans.spi.project.ActionProvider;
//
//import org.openide.awt.ActionID;
//import org.openide.awt.ActionReference;
//import org.openide.awt.ActionRegistration;
//import org.openide.filesystems.FileObject;
//import org.openide.loaders.DataObject;
//import org.openide.loaders.DataObjectNotFoundException;
//import org.openide.util.ContextAwareAction;
//import org.openide.util.ImageUtilities;
//import org.openide.util.Lookup;
//import org.openide.util.NbBundle.Messages;
//import org.openide.util.lookup.Lookups;
//import org.openide.util.WeakListeners;
//import org.openide.windows.TopComponent;
//
//@ActionID(
//    category = "Editor",
//    id = "io.github.s4gh.projecteditorrunactions.RunFileToolbarAction"
//)
//@ActionRegistration(
//    displayName = "#CTL_RunFileToolbarAction",
//    lazy = false
//)
//@ActionReference(
//    path = "Editors/Toolbars/Default",
//    position = 1600
//)
//@Messages("CTL_RunFileToolbarAction=Run File")
//public final class RunFileToolbarAction1 extends AbstractAction implements ContextAwareAction {
//
//    private final PropertyChangeListener editorRegListener = new PropertyChangeListener() {
//        @Override
//        public void propertyChange(PropertyChangeEvent evt) {
//            // Any change in editor registry may affect which file is active.
//            // Recompute enabled state on the EDT.
//            updateEnabledOnEDT();
//        }
//    };
//
//    private final PropertyChangeListener winsysListener = new PropertyChangeListener() {
//        @Override
//        public void propertyChange(PropertyChangeEvent evt) {
//            // Activated TopComponent, opened editors, etc. changed.
//            updateEnabledOnEDT();
//        }
//    };
//
//    public RunFileToolbarAction1() {
//        this(null);
//    }
//
//    /** We ignore the provided context intentionally; we always derive state from the active editor. */
//    private RunFileToolbarAction1(Lookup ignoredContext) {
//        super(Bundle.CTL_RunFileToolbarAction());
//        
//        ImageIcon icon = ImageUtilities.loadImageIcon("icons/run.svg", false);
//        putValue(Action.SMALL_ICON, icon);
//        putValue(Action.SHORT_DESCRIPTION, "Run File");
//        
//        // Listen for editor focus/tab changes.
//        EditorRegistry.addPropertyChangeListener(
//                WeakListeners.propertyChange(editorRegListener, EditorRegistry.class));
//
//        // Also listen to window system changes (e.g., activated TopComponent changes).
//        TopComponent.Registry reg = TopComponent.getRegistry();
//        reg.addPropertyChangeListener(
//                WeakListeners.propertyChange(winsysListener, reg));
//
//        // Initial state
//        updateEnabledOnEDT();
//    }
//
//    private void updateEnabledOnEDT() {
//        if (SwingUtilities.isEventDispatchThread()) {
//            updateEnabled();
//        } else {
//            SwingUtilities.invokeLater(this::updateEnabled);
//        }
//    }
//
//    private static DataObject currentEditorDataObject() {
//        // Always use the last focused editor component, regardless of Projects view selection
//        JEditorPane pane = (JEditorPane) EditorRegistry.lastFocusedComponent();
//        if (pane == null) {
//            return null;
//        }
//        Document doc = pane.getDocument();
//        if (doc == null) {
//            return null;
//        }
//
//        // NetBeans stores the origin (DataObject or FileObject) in the document’s StreamDescriptionProperty
//        Object sdp = doc.getProperty(Document.StreamDescriptionProperty);
//        if (sdp instanceof DataObject) {
//            return (DataObject) sdp;
//        } else if (sdp instanceof FileObject) {
//            try {
//                return DataObject.find((FileObject) sdp);
//            } catch (DataObjectNotFoundException ex) {
//                return null;
//            }
//        }
//
//        // Some editors may not set it; nothing we can do
//        return null;
//    }
//
//    private void updateEnabled() {
//        DataObject dobj = currentEditorDataObject();
//
//        boolean canRun = false;
//        if (dobj != null) {
//            FileObject fo = dobj.getPrimaryFile();
//            Project prj = FileOwnerQuery.getOwner(fo);
//            if (prj != null) {
//                ActionProvider ap = prj.getLookup().lookup(ActionProvider.class);
//                if (ap != null) {
//                    Lookup actionCtx = Lookups.fixed(dobj);
//                    try {
//                        canRun = ap.isActionEnabled(ActionProvider.COMMAND_RUN_SINGLE, actionCtx);
//                    } catch (Exception ex) {
//                        // Defensive: some project types might throw for unsupported files
//                        canRun = false;
//                    }
//                }
//            }
//        }
//        setEnabled(canRun);
//    }
//
//    @Override
//    public void actionPerformed(ActionEvent e) {
//        DataObject dobj = currentEditorDataObject();
//        if (dobj == null) {
//            return;
//        }
//        FileObject fo = dobj.getPrimaryFile();
//        Project prj = FileOwnerQuery.getOwner(fo);
//        if (prj == null) {
//            return;
//        }
//        ActionProvider ap = prj.getLookup().lookup(ActionProvider.class);
//        if (ap == null) {
//            return;
//        }
//
//        // Run on the EDT like ActionProvider expects
//        SwingUtilities.invokeLater(() -> {
//            Lookup actionCtx = Lookups.fixed(dobj);
//            if (ap.isActionEnabled(ActionProvider.COMMAND_RUN_SINGLE, actionCtx)) {
//                ap.invokeAction(ActionProvider.COMMAND_RUN_SINGLE, actionCtx);
//            }
//        });
//    }
//
//
//    /** Provide a context-bound instance for editor/toolbar usage.
//     *  We intentionally ignore the Lookup and still bind to the editor. */
//    @Override
//    public Action createContextAwareInstance(Lookup actionContext) {
//        return new RunFileToolbarAction1(actionContext);
//    }
//}