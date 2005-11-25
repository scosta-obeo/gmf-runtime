/******************************************************************************
 * Copyright (c) 2002, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    IBM Corporation - initial API and implementation 
 ****************************************************************************/

package org.eclipse.gmf.runtime.diagram.ui.parts;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.draw2d.FigureCanvas;
import org.eclipse.draw2d.LightweightSystem;
import org.eclipse.draw2d.Viewport;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.parts.ScrollableThumbnail;
import org.eclipse.draw2d.parts.Thumbnail;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.gef.ContextMenuProvider;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPartFactory;
import org.eclipse.gef.EditPartViewer;
import org.eclipse.gef.KeyHandler;
import org.eclipse.gef.KeyStroke;
import org.eclipse.gef.LayerConstants;
import org.eclipse.gef.RootEditPart;
import org.eclipse.gef.SnapToGeometry;
import org.eclipse.gef.SnapToGrid;
import org.eclipse.gef.editparts.ZoomManager;
import org.eclipse.gef.rulers.RulerProvider;
import org.eclipse.gef.ui.actions.ActionRegistry;
import org.eclipse.gef.ui.actions.DirectEditAction;
import org.eclipse.gef.ui.actions.GEFActionConstants;
import org.eclipse.gef.ui.parts.ContentOutlinePage;
import org.eclipse.gef.ui.parts.GraphicalEditor;
import org.eclipse.gef.ui.parts.ScrollingGraphicalViewer;
import org.eclipse.gef.ui.parts.TreeViewer;
import org.eclipse.gef.ui.rulers.RulerComposite;
import org.eclipse.gmf.runtime.common.core.command.CommandManager;
import org.eclipse.gmf.runtime.common.core.util.Log;
import org.eclipse.gmf.runtime.common.core.util.Trace;
import org.eclipse.gmf.runtime.common.ui.action.ActionManager;
import org.eclipse.gmf.runtime.common.ui.services.editor.EditorService;
import org.eclipse.gmf.runtime.common.ui.services.marker.MarkerNavigationService;
import org.eclipse.gmf.runtime.diagram.core.internal.util.MEditingDomainGetter;
import org.eclipse.gmf.runtime.diagram.core.preferences.PreferencesHint;
import org.eclipse.gmf.runtime.diagram.core.util.ViewUtil;
import org.eclipse.gmf.runtime.diagram.ui.DiagramUIDebugOptions;
import org.eclipse.gmf.runtime.diagram.ui.DiagramUIPlugin;
import org.eclipse.gmf.runtime.diagram.ui.actions.ActionIds;
import org.eclipse.gmf.runtime.diagram.ui.editparts.DiagramEditPart;
import org.eclipse.gmf.runtime.diagram.ui.editparts.DiagramRootEditPart;
import org.eclipse.gmf.runtime.diagram.ui.editparts.IDiagramPreferenceSupport;
import org.eclipse.gmf.runtime.diagram.ui.internal.actions.InsertAction;
import org.eclipse.gmf.runtime.diagram.ui.internal.actions.PromptingDeleteAction;
import org.eclipse.gmf.runtime.diagram.ui.internal.actions.PromptingDeleteFromModelAction;
import org.eclipse.gmf.runtime.diagram.ui.internal.editparts.DiagramRootTreeEditPart;
import org.eclipse.gmf.runtime.diagram.ui.internal.editparts.TreeDiagramEditPart;
import org.eclipse.gmf.runtime.diagram.ui.internal.editparts.TreeEditPart;
import org.eclipse.gmf.runtime.diagram.ui.internal.l10n.DiagramUIPluginImages;
import org.eclipse.gmf.runtime.diagram.ui.internal.pagesetup.DefaultValues;
import org.eclipse.gmf.runtime.diagram.ui.internal.pagesetup.PageInfoHelper;
import org.eclipse.gmf.runtime.diagram.ui.internal.parts.DiagramGraphicalViewerKeyHandler;
import org.eclipse.gmf.runtime.diagram.ui.internal.properties.WorkspaceViewerProperties;
import org.eclipse.gmf.runtime.diagram.ui.internal.ruler.DiagramRuler;
import org.eclipse.gmf.runtime.diagram.ui.internal.ruler.DiagramRulerProvider;
import org.eclipse.gmf.runtime.diagram.ui.l10n.DiagramUIMessages;
import org.eclipse.gmf.runtime.diagram.ui.preferences.IPreferenceConstants;
import org.eclipse.gmf.runtime.diagram.ui.providers.DiagramContextMenuProvider;
import org.eclipse.gmf.runtime.diagram.ui.services.editpart.EditPartService;
import org.eclipse.gmf.runtime.emf.core.edit.MRunnable;
import org.eclipse.gmf.runtime.notation.Diagram;
import org.eclipse.gmf.runtime.notation.GuideStyle;
import org.eclipse.gmf.runtime.notation.NotationPackage;
import org.eclipse.gmf.runtime.notation.View;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceStore;
import org.eclipse.jface.util.Assert;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.part.IPageSite;
import org.eclipse.ui.part.IShowInSource;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.part.ShowInContext;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.eclipse.wst.common.ui.properties.internal.provisional.ITabbedPropertySheetPageContributor;

/**
 * @author melaasar
 *
 * A generic diagram editor with no palette.
 * DiagramEditorWithPalette will provide a palette.
 */
public abstract class DiagramEditor
	extends GraphicalEditor
	implements IDiagramWorkbenchPart, ITabbedPropertySheetPageContributor, IShowInSource {

	/**
	 * teh ID of the outline
	 */
	protected static final int ID_OUTLINE = 0;

	/**
	 * the id of the over view
	 */
	protected static final int ID_OVERVIEW = 1;

	/**
	 * the work space viewer preference store
	 */
	protected PreferenceStore workspaceViewerPreferenceStore = null;

	/**
	 * A diagram outline page
	 */
	class DiagramOutlinePage
		extends ContentOutlinePage
		implements IAdaptable {

		private PageBook pageBook;

		private Control outline;

		private Canvas overview;

		private IAction showOutlineAction, showOverviewAction;

		private boolean overviewInitialized;

		private Thumbnail thumbnail;

		private DisposeListener disposeListener;

		/**
		 * @param viewer
		 */
		public DiagramOutlinePage(EditPartViewer viewer) {
			super(viewer);
		}

		/* (non-Javadoc)
		 * @see org.eclipse.ui.part.Page#init(org.eclipse.ui.part.IPageSite)
		 */
		public void init(IPageSite pageSite) {
			super.init(pageSite);
			ActionRegistry registry = getActionRegistry();
			IActionBars bars = pageSite.getActionBars();
			String id = ActionFactory.UNDO.getId();
			bars.setGlobalActionHandler(id, registry.getAction(id));
			id = ActionFactory.REDO.getId();
			bars.setGlobalActionHandler(id, registry.getAction(id));
			id = ActionFactory.DELETE.getId();
			bars.setGlobalActionHandler(id, registry.getAction(id));
			bars.updateActionBars();

			// Toolbar refresh to solve linux defect RATLC525198 
			bars.getToolBarManager().markDirty();
		}

		/**
		 * configures the outline viewer
		 */
		protected void configureOutlineViewer() {
			getViewer().setEditDomain(getEditDomain());
			getViewer().setEditPartFactory(new EditPartFactory() {

				public EditPart createEditPart(EditPart context, Object model) {
					if (model instanceof Diagram) {
						return new TreeDiagramEditPart(model);
					} else {
						return new TreeEditPart(model);
					}
				}
			});
			// No support for a context menu on the outline view for
			// release 6.0.  See RATLC00529151, RATLC00529144
			// The selected item is a TreeEditPart which is not an
			// IGraphicalEditPart and many actions/commands don't support it
//			ContextMenuProvider provider = new DiagramContextMenuProvider(
//				DiagramEditor.this, getViewer());
//			getViewer().setContextMenu(provider);
//			this.getSite().registerContextMenu(
//				ActionIds.DIAGRAM_OUTLINE_CONTEXT_MENU, provider,
//				this.getSite().getSelectionProvider());
			
			getViewer().setKeyHandler(getKeyHandler());
			//getViewer().addDropTargetListener(
			//  new LogicTemplateTransferDropTargetListener(getViewer()));
			IToolBarManager tbm = this.getSite().getActionBars()
				.getToolBarManager();
			showOutlineAction = new Action() {

				public void run() {
					showPage(ID_OUTLINE);
				}
			};
			showOutlineAction.setImageDescriptor(DiagramUIPluginImages.DESC_OUTLINE);
			tbm.add(showOutlineAction);
			showOverviewAction = new Action() {

				public void run() {
					showPage(ID_OVERVIEW);
				}
			};
			showOverviewAction.setImageDescriptor(DiagramUIPluginImages.DESC_OVERVIEW);
			tbm.add(showOverviewAction);
			showPage(getDefaultOutlineViewMode());
		}

		public void createControl(Composite parent) {
			pageBook = new PageBook(parent, SWT.NONE);
			outline = getViewer().createControl(pageBook);
			overview = new Canvas(pageBook, SWT.NONE);
			pageBook.showPage(outline);
			configureOutlineViewer();
			hookOutlineViewer();
			initializeOutlineViewer();
		}

		public void dispose() {
			unhookOutlineViewer();
			if (thumbnail != null) {
				thumbnail.deactivate();
			}
			this.overviewInitialized = false;
			super.dispose();
		}

		public Object getAdapter(Class type) {
			//	if (type == ZoomManager.class)
			//		return getZoomManager();
			return null;
		}

		public Control getControl() {
			return pageBook;
		}

		/**
		 * hook the outline viewer
		 */
		protected void hookOutlineViewer() {
			getSelectionSynchronizer().addViewer(getViewer());
		}

		/**
		 * initialize the outline viewer
		 */
		protected void initializeOutlineViewer() {
			MEditingDomainGetter.getMEditingDomain(getDiagram()).runAsRead(new MRunnable() {

				public Object run() {
					getViewer().setContents(getDiagram());
					return null;
				}
			});
		}

		/**
		 * initialize the overview
		 */
		protected void initializeOverview() {
			LightweightSystem lws = new LightweightSystem(overview);
			RootEditPart rep = getGraphicalViewer().getRootEditPart();
			DiagramRootEditPart root = (DiagramRootEditPart) rep;
			thumbnail = new ScrollableThumbnail((Viewport) root.getFigure());
			//thumbnail.setSource(root.getLayer(LayerConstants.PRINTABLE_LAYERS));
			thumbnail.setSource(root.getLayer(LayerConstants.SCALABLE_LAYERS));

			lws.setContents(thumbnail);
			disposeListener = new DisposeListener() {

				public void widgetDisposed(DisposeEvent e) {
					if (thumbnail != null) {
						thumbnail.deactivate();
						thumbnail = null;
					}
				}
			};
			getEditor().addDisposeListener(disposeListener);
			this.overviewInitialized = true;
		}

		/**
		 * show page with a specific ID, possibel values are 
		 * ID_OUTLINE and ID_OVERVIEW
		 * @param id
		 */
		protected void showPage(int id) {
			if (id == ID_OUTLINE) {
				showOutlineAction.setChecked(true);
				showOverviewAction.setChecked(false);
				pageBook.showPage(outline);
				if (thumbnail != null)
					thumbnail.setVisible(false);
			} else if (id == ID_OVERVIEW) {
				if (!overviewInitialized)
					initializeOverview();
				showOutlineAction.setChecked(false);
				showOverviewAction.setChecked(true);
				pageBook.showPage(overview);
				thumbnail.setVisible(true);
			}
		}

		/**
		 * unhook the outline viewer
		 */
		protected void unhookOutlineViewer() {
			getSelectionSynchronizer().removeViewer(getViewer());
			if (disposeListener != null && getEditor() != null
				&& !getEditor().isDisposed())
				getEditor().removeDisposeListener(disposeListener);
		}

		/**
		 * getter for the editor conrolo
		 * @return <code>Control</code>
		 */
		protected Control getEditor() {
			return getGraphicalViewer().getControl();
		}

	}

	/** The key handler */
	private KeyHandler keyHandler;

	/** The workbench site
	 *  This variable overrides another one defined in <code>org.eclipse.ui.part<code>
	 *  This is needed to override <code>setSite</code> to simply set the site, rather than also
	 *  initializing the actions like <code>setSite</code> override in <code>org.eclipse.gef.ui.parts</code>
	 */
	private IWorkbenchPartSite partSite;

	/** The RulerComposite used to enhance the graphical viewer to display
	 *  rulers
	 */
	private RulerComposite rulerComposite;

	/**
	 * Returns this editor's outline-page default display mode. 
	 * @return int the integer value indicating the content-outline-page dispaly mode 
	 */
	protected int getDefaultOutlineViewMode() {
		return ID_OVERVIEW;
	}

	/**
	 * @return Returns the rulerComp.
	 */
	protected RulerComposite getRulerComposite() {
		return rulerComposite;
	}

	/**
	 * @param rulerComp The rulerComp to set.
	 */
	protected void setRulerComposite(RulerComposite rulerComp) {
		this.rulerComposite = rulerComp;
	}
	/**
	 * Creates a new DiagramEditor instance
	 */
	public DiagramEditor() {
		createDiagramEditDomain();
	}

	/**
	 * @see org.eclipse.gmf.runtime.diagram.ui.parts.IDiagramWorkbenchPart#getDiagramEditDomain()
	 */
	public IDiagramEditDomain getDiagramEditDomain() {
		return (IDiagramEditDomain) getEditDomain();
	}

	/**
	 * @see org.eclipse.gmf.runtime.diagram.ui.parts.IDiagramWorkbenchPart#getDiagramGraphicalViewer()
	 */
	public IDiagramGraphicalViewer getDiagramGraphicalViewer() {
		return (IDiagramGraphicalViewer) getGraphicalViewer();
	}

	/**
	 * @see org.eclipse.gmf.runtime.diagram.ui.parts.IDiagramWorkbenchPart#getDiagram()
	 */
	public Diagram getDiagram() {
		if (getEditorInput() != null)
			return ((IDiagramEditorInput) getEditorInput()).getDiagram();
		return null;
	}

	/**
	 * @see org.eclipse.gmf.runtime.diagram.ui.parts.IDiagramWorkbenchPart#getDiagramEditPart()
	 */
	public DiagramEditPart getDiagramEditPart() {
		return (DiagramEditPart) getDiagramGraphicalViewer().getContents();
	}

	/**
	 * @see org.eclipse.core.runtime.IAdaptable#getAdapter(Class)
	 */
	public Object getAdapter(Class type) {		
		if (type == IContentOutlinePage.class) {
			TreeViewer viewer = new TreeViewer();
			viewer.setRootEditPart(new DiagramRootTreeEditPart());
			return new DiagramOutlinePage(viewer);
		}
		if (type == CommandManager.class)
			return getCommandManager();
		if (ActionManager.class == type)
			return getActionManager();
		if (IDiagramEditDomain.class == type)
			return getDiagramEditDomain();
		if (type == ZoomManager.class)
			return getZoomManager();
		return super.getAdapter(type);

	}

	/**
	 * @see org.eclipse.ui.IEditorPart#init(IEditorSite, IEditorInput)
	 */
	public void init(IEditorSite site, IEditorInput input)
		throws PartInitException {
		try {
			super.init(site, input);
			EditorService.getInstance().registerEditor(this);
		} catch (Exception e) {
			//	RATLC 524435
			//	As discussed with Steve, Removing the Log and removing the 
			//	standard message and replacing it with one obtained from the
			//	Exception.
			Trace.catching(DiagramUIPlugin.getInstance(), DiagramUIDebugOptions.EXCEPTIONS_CATCHING, getClass(), "init", e); //$NON-NLS-1$
			throw new PartInitException(e.getMessage(), e);
		}
	}

	/**
	 * Disposes this editor by:
	 * <br> 3. Stops all registered listeners
	 * @see org.eclipse.ui.IWorkbenchPart#dispose()
	 */
	public void dispose() {
		persistViewerSettings();
		EditorService.getInstance().unregisterEditor(DiagramEditor.this);
		stopListening();

		/*
		 * RATLC00527385 DiagramRulerProvider wasn't uninitialized on dispose of the editor.
		 */
		DiagramRulerProvider vertProvider = (DiagramRulerProvider) getDiagramGraphicalViewer()
			.getProperty(RulerProvider.PROPERTY_VERTICAL_RULER);
		if (vertProvider != null)
			vertProvider.uninit();

		// Set the Horizontal Ruler properties
		DiagramRulerProvider horzProvider = (DiagramRulerProvider) getDiagramGraphicalViewer()
			.getProperty(RulerProvider.PROPERTY_HORIZONTAL_RULER);
		if (horzProvider != null)
			horzProvider.uninit();
		super.dispose();
	}

	/**
	 * Returns the KeyHandler with common bindings for both the Outline and Graphical Views.
	 * For example, delete is a common action.
	 * @return KeyHandler
	 */
	protected KeyHandler getKeyHandler() {
		if (keyHandler == null) {
			keyHandler = new KeyHandler();

			ActionRegistry registry = getActionRegistry();
			IAction action;

			action = new PromptingDeleteAction(this);
			action.setText(DiagramUIMessages.DiagramEditor_Delete_from_Diagram); 
			registry.registerAction(action);
			getSelectionActions().add(action.getId());

			action = new InsertAction(this);
			action.setText(""); //$NON-NLS-1$ // no text necessary since this is not a visible action
			registry.registerAction(action);
			getSelectionActions().add(action.getId());

			PromptingDeleteFromModelAction deleteModelAction = new PromptingDeleteFromModelAction(
				this);
			deleteModelAction.init();

			registry.registerAction(deleteModelAction);

			action = new DirectEditAction((IWorkbenchPart) this);
			registry.registerAction(action);
			getSelectionActions().add(action.getId());

			keyHandler.put(KeyStroke.getPressed(SWT.INSERT, 0),
				getActionRegistry().getAction(InsertAction.ID));
			keyHandler.put(KeyStroke.getPressed(SWT.DEL, 127, 0),
				getActionRegistry().getAction(ActionFactory.DELETE.getId()));
			keyHandler.put(KeyStroke.getPressed(SWT.BS, 8, 0),
				getActionRegistry().getAction(ActionFactory.DELETE.getId()));
			keyHandler.put(/* CTRL + D */
			KeyStroke.getPressed((char) 0x4, 100, SWT.CTRL),
				getActionRegistry().getAction(
					ActionIds.ACTION_DELETE_FROM_MODEL));
			keyHandler.put(KeyStroke.getPressed(SWT.F2, 0), getActionRegistry()
				.getAction(GEFActionConstants.DIRECT_EDIT));
		}
		return keyHandler;
	}

	/**
	 * @see org.eclipse.gef.ui.parts.GraphicalEditor#createGraphicalViewer(Composite)
	 */
	protected void createGraphicalViewer(Composite parent) {
		setRulerComposite(new RulerComposite(parent, SWT.NONE));

		ScrollingGraphicalViewer sGViewer = createScrollingGraphicalViewer();					
		sGViewer.createControl(getRulerComposite());
		setGraphicalViewer(sGViewer);
		hookGraphicalViewer();
		configureGraphicalViewer();
		initializeGraphicalViewer(); 
		getRulerComposite()
			.setGraphicalViewer((ScrollingGraphicalViewer) getGraphicalViewer());
	}

	/**
	 * Creates a ScrollingGraphicalViewer without the drop adapter which  
	 * excludes drag and drop functionality from other defined views (XML)
	 * Subclasses must override this method to include the DnD functionality
	 * 
	 * @return ScrollingGraphicalViewer
	 */
	protected ScrollingGraphicalViewer createScrollingGraphicalViewer() {
		return new DiagramGraphicalViewer();
	}

	/**
	 * Configures the graphical viewer (the primary viewer of the editor)
	 */
	protected void configureGraphicalViewer() {
		super.configureGraphicalViewer();
		
		IDiagramGraphicalViewer viewer = getDiagramGraphicalViewer();
		
		RootEditPart rootEP = EditPartService.getInstance()
			.createRootEditPart(getDiagram());
		if (rootEP instanceof IDiagramPreferenceSupport) {
			((IDiagramPreferenceSupport) rootEP)
				.setPreferencesHint(getPreferencesHint());
		}

		if (getDiagramGraphicalViewer() instanceof DiagramGraphicalViewer) {
			((DiagramGraphicalViewer) getDiagramGraphicalViewer())
				.hookWorkspacePreferenceStore(getWorkspaceViewerPreferenceStore());
		}

		viewer.setRootEditPart(rootEP);
		
		viewer.setEditPartFactory(EditPartService.getInstance());
		ContextMenuProvider provider = new DiagramContextMenuProvider(this,
			viewer);
		viewer.setContextMenu(provider);
		getSite().registerContextMenu(ActionIds.DIAGRAM_EDITOR_CONTEXT_MENU,
			provider, viewer);
		viewer.setKeyHandler(new DiagramGraphicalViewerKeyHandler(viewer)
			.setParent(getKeyHandler()));
		((FigureCanvas) viewer.getControl())
			.setScrollBarVisibility(FigureCanvas.ALWAYS);
	}

	/**
	 * @see org.eclipse.gef.ui.parts.GraphicalEditor#initializeGraphicalViewer()
	 */
	protected void initializeGraphicalViewer() {
		initializeGraphicalViewerContents();
	}

	/**
	 * @see org.eclipse.gef.ui.parts.GraphicalEditor#initializeGraphicalViewer()
	 */
	protected void initializeGraphicalViewerContents() {
		getDiagramGraphicalViewer().setContents(getDiagram());
		initializeContents(getDiagramEditPart());
	}

	/**
	 * Creates a diagram edit domain
	 */
	protected void createDiagramEditDomain() {
		setEditDomain(new DiagramEditDomain(this));
		configureDiagramEditDomain();
	}

	/**
	 * Configures a diagram edit domain
	 */
	protected void configureDiagramEditDomain() {
		getEditDomain().setCommandStack(
			new DiagramCommandStack(getDiagramEditDomain()));
	}

	/**
	 * @see org.eclipse.ui.part.EditorPart#setInput(IEditorInput)
	 */
	protected void setInput(IEditorInput input) {
		stopListening();
		super.setInput(input);
		if(input != null) {
			Assert.isNotNull(getDiagram(), "Couldn't load/create diagram view"); //$NON-NLS-1$
		}
		startListening();

	}

	/** 
	 * Do nothing
	 * @see org.eclipse.gef.ui.parts.GraphicalEditor#initializeActionRegistry()
	 */
	protected void createActions() {
		// null impl.
	}

	/**
	 * A utility to close the editor
	 * @param save
	 */
	protected void closeEditor(final boolean save) {
		// Make this call synchronously to avoid the following sequence:
		// Select me, select the model editor, close the model editor, 
		// closes the model, fires events causing me to close, 
		// if actual close is delayed using an async call then eclipse 
		// tries to set the selection back to me when the model editor
		// finishes being disposed, but model has been closed so I
		// am no longer connected to the model, NullPointerExceptions occur.
		try {
			getSite().getPage().closeEditor(DiagramEditor.this, save);
		} catch (SWTException e) {
			// TODO remove when "Widget is disposed" exceptions are fixed.
			// Temporarily catch SWT exceptions here to allow model server event
			// processing to continue.
			Trace.catching(DiagramUIPlugin.getInstance(),
				DiagramUIDebugOptions.EXCEPTIONS_CATCHING, this.getClass(),
				"closeEditor", e); //$NON-NLS-1$
			Log.error(DiagramUIPlugin.getInstance(), IStatus.ERROR, e
				.getMessage(), e);
		}
	}

	/**
	 * Installs all the listeners needed by the editor
	 */
	protected void startListening() {
		// do nothing
	}

	/**
	 * Removes all the listeners used by the editor
	 */
	protected void stopListening() {
		// do nothing		
	}

	/**
	 * Clears the contents of the graphical viewer
	 */
	protected void clearGraphicalViewerContents() {
		if (getDiagramGraphicalViewer().getContents() != null) {
			getDiagramGraphicalViewer().getContents().removeNotify();
		}
		getDiagramGraphicalViewer().setContents(null);
	}

	/**
	 * Gets the action manager for this diagram editor. The action manager's 
	 * command manager is used by my edit domain's command stack when executing
	 * commands. This is the action manager that is returned when I am asked
	 * to adapt to an <code>ActionManager</code>.
	 * 
	 * @return the action manager
	 */
	protected ActionManager getActionManager() {
		return getDiagramEditDomain().getActionManager();
	}

	/**
	 * A utility method to return the zoom manager from the graphical viewer's root
	 * @return the zoom manager
	 */
	protected ZoomManager getZoomManager() {
		return ((DiagramRootEditPart) getRootEditPart()).getZoomManager();
	}

	private RootEditPart getRootEditPart() {
		return getGraphicalViewer().getRootEditPart();		
	}
	
	/**
	 * Convenience method to access the command manager associated with my
	 * action manager. This command manager is used by my edit domain's 
	 * command stack when executing commands.
	 * 
	 * @return the command manager
	 */
	protected CommandManager getCommandManager() {
		return getActionManager().getCommandManager();
	}

	/**
	 * go to a specific marker
	 * @param marker marker to use
	 */
	public final void gotoMarker(IMarker marker) {
		MarkerNavigationService.getInstance().gotoMarker(this, marker);
	}

	/**
	 * 
	 * @return The getRulerComposite(), which is the graphical control
	 */
	protected Control getGraphicalControl() {
		return getRulerComposite();

	}

	/**
	 * @see org.eclipse.ui.IWorkbenchPart#getSite()
	 */
	public IWorkbenchPartSite getSite() {
		return partSite;
	}

	/**
	 * @see org.eclipse.ui.part.WorkbenchPart#setSite(IWorkbenchPartSite)
	 */
	protected void setSite(IWorkbenchPartSite site) {
		this.partSite = site;

	}

	/* (non-Javadoc)
	 * @see org.eclipse.gmf.runtime.common.ui.properties.ITabbedPropertySheetPageContributor#getContributorId()
	 */
	public String getContributorId() {
		return "org.eclipse.gmf.runtime.diagram.ui.properties"; //$NON-NLS-1$
	}

	/**
	 * Adds the default preferences to the specified preference store.
	 * @param store store to use
	 * @param preferencesHint
	 *            The preference hint that is to be used to find the appropriate
	 *            preference store from which to retrieve diagram preference
	 *            values. The preference hint is mapped to a preference store in
	 *            the preference registry <@link DiagramPreferencesRegistry>.
	 */
	public static void addDefaultPreferences(PreferenceStore store, PreferencesHint preferencesHint) {
		store.setValue(WorkspaceViewerProperties.ZOOM, 1.0);
		store.setValue(WorkspaceViewerProperties.VIEWPAGEBREAKS, false);

		IPreferenceStore globalPreferenceStore = (IPreferenceStore) preferencesHint.getPreferenceStore();

		// Initialize with the global settings
		boolean viewGrid = globalPreferenceStore
			.getBoolean(IPreferenceConstants.PREF_SHOW_GRID);

		boolean snapToGrid = globalPreferenceStore
			.getBoolean(IPreferenceConstants.PREF_SNAP_TO_GRID);

		boolean viewRulers = globalPreferenceStore
			.getBoolean(IPreferenceConstants.PREF_SHOW_RULERS);

		// Set defaults for Grid
		store.setValue(WorkspaceViewerProperties.VIEWGRID, viewGrid);
		store.setValue(WorkspaceViewerProperties.SNAPTOGRID, snapToGrid);

		// Set defaults for Rulers
		store.setValue(WorkspaceViewerProperties.VIEWRULERS, viewRulers);

		// Set defaults for Page Setup Dialog
		//PSDialog.initDefaultPreferences(store);
		
		//PSDefault.initDefaultPSPreferencePagePreferences(globalPreferenceStore);
		
		//String pageType = PageInfoHelper.getPrinterPageType();
		//String pageSize = PageInfoHelper.getLocaleSpecificPageType();
		//Point2D.Double point = (Point2D.Double) nnn.getPaperSizesInInchesMap().get(pageSize);
		
		//double[] marginSizes = nnn.getDefaultMarginSizes();
		
		//store.setValue(WorkspaceViewerProperties.USE_WORKSPACE_PRINT_SETTINGS, true);
		//store.setValue(WorkspaceViewerProperties.USE_DIAGRAM_PRINT_SETTINGS, false);
		
		//store.setValue(WorkspaceViewerProperties.USE_INCHES, true);
		//store.setValue(WorkspaceViewerProperties.USE_MILLIMETRES, false);
		
		//store.setValue(WorkspaceViewerProperties.PAGE_ORIENTATION_LANDSCAPE, false);
		//store.setValue(WorkspaceViewerProperties.PAGE_ORIENTATION_PORTRAIT, true);
		
		//store.setValue(WorkspaceViewerProperties.PAGE_TYPE, pageSize);
		
		//store.setValue(WorkspaceViewerProperties.PAGE_WIDTH, point.x);
		//store.setValue(WorkspaceViewerProperties.PAGE_HEIGHT, point.y);
		
		//store.setValue(WorkspaceViewerProperties.MARGIN_LEFT, marginSizes[0]);
		//store.setValue(WorkspaceViewerProperties.MARGIN_TOP, marginSizes[1]);
		//store.setValue(WorkspaceViewerProperties.MARGIN_RIGHT, marginSizes[2]);
		//store.setValue(WorkspaceViewerProperties.MARGIN_BOTTOM, marginSizes[3]);
		
		// Initialize printing defaults
		store.setValue(WorkspaceViewerProperties.PREF_USE_WORKSPACE_SETTINGS, DefaultValues.DEFAULT_USE_WORKSPACE_SETTINGS);
		store.setValue(WorkspaceViewerProperties.PREF_USE_DIAGRAM_SETTINGS, DefaultValues.DEFAULT_USE_DIAGRAM_SETTINGS);
			
		store.setValue(WorkspaceViewerProperties.PREF_USE_INCHES, DefaultValues.DEFAULT_INCHES);
		store.setValue(WorkspaceViewerProperties.PREF_USE_MILLIM, DefaultValues.DEFAULT_MILLIM);
		
		store.setValue(WorkspaceViewerProperties.PREF_USE_PORTRAIT, DefaultValues.DEFAULT_PORTRAIT);
		store.setValue(WorkspaceViewerProperties.PREF_USE_LANDSCAPE, DefaultValues.DEFAULT_LANDSCAPE);
		
		store.setValue(WorkspaceViewerProperties.PREF_PAGE_SIZE, DefaultValues.getLocaleSpecificPageType().getName());
		store.setValue(WorkspaceViewerProperties.PREF_PAGE_WIDTH, DefaultValues.getLocaleSpecificPageType().getWidth());
		store.setValue(WorkspaceViewerProperties.PREF_PAGE_HEIGHT, DefaultValues.getLocaleSpecificPageType().getHeight());
		
		store.setValue(WorkspaceViewerProperties.PREF_MARGIN_TOP, DefaultValues.DEFAULT_MARGIN_TOP);
		store.setValue(WorkspaceViewerProperties.PREF_MARGIN_BOTTOM, DefaultValues.DEFAULT_MARGIN_BOTTOM);
		store.setValue(WorkspaceViewerProperties.PREF_MARGIN_LEFT, DefaultValues.DEFAULT_MARGIN_LEFT);
		store.setValue(WorkspaceViewerProperties.PREF_MARGIN_RIGHT, DefaultValues.DEFAULT_MARGIN_RIGHT);
		
	}

	/**
	 * Returns the workspace viewer <code>PreferenceStore</code>
	 * @return	the workspace viewer <code>PreferenceStore</code>
	 */
	public PreferenceStore getWorkspaceViewerPreferenceStore() {
		if (workspaceViewerPreferenceStore != null) {
			return workspaceViewerPreferenceStore;
		} else {
			// Try to load it
			IPath path = ((DiagramUIPlugin) DiagramUIPlugin.getInstance())
				.getStateLocation();
			String id = ViewUtil.getIdStr(getDiagram());

			String fileName = path.toString() + "/" + id;//$NON-NLS-1$
			java.io.File file = new File(fileName);
			workspaceViewerPreferenceStore = new PreferenceStore(fileName);
			if (file.exists()) {
				// Load it
				try {
					workspaceViewerPreferenceStore.load();
				} catch (Exception e) {
					// Create the default
					addDefaultPreferences();
				}
			} else {
				// Create it
				addDefaultPreferences();
			}
			return workspaceViewerPreferenceStore;
		}
	}

	/**
	 * Adds the default preferences to the workspace viewer preference store.
	 */
	protected void addDefaultPreferences() {
		addDefaultPreferences(workspaceViewerPreferenceStore, getPreferencesHint());
	}

	/**
	 * Persists the viewer settings to which this RootEditPart belongs. This method should
	 * be called when the diagram is being disposed.
	 */
	public void persistViewerSettings() {
		Viewport viewport = getDiagramEditPart().getViewport();
		if (viewport != null) {
			int x = viewport.getHorizontalRangeModel().getValue();
			int y = viewport.getVerticalRangeModel().getValue();
			getWorkspaceViewerPreferenceStore().setValue(
				WorkspaceViewerProperties.VIEWPORTX, x);
			getWorkspaceViewerPreferenceStore().setValue(
				WorkspaceViewerProperties.VIEWPORTY, y);
		}
		getWorkspaceViewerPreferenceStore().setValue(
			WorkspaceViewerProperties.ZOOM, getZoomManager().getZoom());

		// Write the settings, if necessary
		try {
			if (getWorkspaceViewerPreferenceStore().needsSaving())
				getWorkspaceViewerPreferenceStore().save();
		} catch (IOException ioe) {
			Trace.catching(DiagramUIPlugin.getInstance(),
				DiagramUIDebugOptions.EXCEPTIONS_CATCHING,
				PageInfoHelper.class, "persistViewerSettings", //$NON-NLS-1$
				ioe);
		}
	}


	/**
	 * Initializes the viewer's state from the workspace preference store.
	 * @param editpart
	 */
	private void initializeContents(EditPart editpart) {
		getZoomManager().setZoom(
			getWorkspaceViewerPreferenceStore().getDouble(
				WorkspaceViewerProperties.ZOOM));

		getDiagramEditPart().refreshPageBreaks();
		
		// Update the range model of the viewport
		((DiagramEditPart) editpart).getViewport().validate();		
		if (editpart instanceof DiagramEditPart) {
			int x = getWorkspaceViewerPreferenceStore().getInt(
				WorkspaceViewerProperties.VIEWPORTX);
			int y = getWorkspaceViewerPreferenceStore().getInt(
				WorkspaceViewerProperties.VIEWPORTY);
			((DiagramEditPart) editpart).getViewport()
				.getHorizontalRangeModel().setValue(x);
			((DiagramEditPart) editpart).getViewport().getVerticalRangeModel()
				.setValue(y);
		}

		// Get the Ruler Units properties
		int rulerUnits = ((IPreferenceStore) ((DiagramRootEditPart) getRootEditPart()).getPreferencesHint().getPreferenceStore())
			.getInt(IPreferenceConstants.PREF_RULER_UNITS);

		// Get the Guide Style
		GuideStyle guideStyle = (GuideStyle) getDiagram().getStyle(
			NotationPackage.eINSTANCE.getGuideStyle());
		
		
		if (guideStyle != null) {

			RootEditPart rep = getGraphicalViewer().getRootEditPart();
			DiagramRootEditPart root = (DiagramRootEditPart) rep;
			
			// Set the Vertical Ruler properties
			DiagramRuler verticalRuler = ((DiagramRootEditPart) getRootEditPart()).getVerticalRuler();
			verticalRuler.setGuideStyle(guideStyle);
			verticalRuler.setUnit(rulerUnits);
			DiagramRulerProvider vertProvider = new DiagramRulerProvider(
				verticalRuler, root.getMapMode());
			vertProvider.init();
			getDiagramGraphicalViewer().setProperty(
				RulerProvider.PROPERTY_VERTICAL_RULER, vertProvider);
	
			// Set the Horizontal Ruler properties
			DiagramRuler horizontalRuler = ((DiagramRootEditPart) getRootEditPart()).getHorizontalRuler();
			horizontalRuler.setGuideStyle(guideStyle);
			horizontalRuler.setUnit(rulerUnits);
			DiagramRulerProvider horzProvider = new DiagramRulerProvider(
				horizontalRuler, root.getMapMode());
			horzProvider.init();
			getDiagramGraphicalViewer().setProperty(
				RulerProvider.PROPERTY_HORIZONTAL_RULER, horzProvider);
	
			// Show/Hide Rulers
			getDiagramGraphicalViewer().setProperty(
				RulerProvider.PROPERTY_RULER_VISIBILITY,
				Boolean.valueOf(getWorkspaceViewerPreferenceStore().getBoolean(
					WorkspaceViewerProperties.VIEWRULERS)));
			
		}

		// Snap to Grid
		getDiagramGraphicalViewer().setProperty(
			SnapToGeometry.PROPERTY_SNAP_ENABLED,
			Boolean.valueOf(getWorkspaceViewerPreferenceStore().getBoolean(
				WorkspaceViewerProperties.SNAPTOGRID)));

		// Hide/Show Grid
		getDiagramGraphicalViewer().setProperty(
			SnapToGrid.PROPERTY_GRID_ENABLED,
			Boolean.valueOf(getWorkspaceViewerPreferenceStore().getBoolean(
				WorkspaceViewerProperties.VIEWGRID)));
		getDiagramGraphicalViewer().setProperty(
			SnapToGrid.PROPERTY_GRID_VISIBLE,
			Boolean.valueOf(getWorkspaceViewerPreferenceStore().getBoolean(
				WorkspaceViewerProperties.VIEWGRID)));

		// Grid Origin (always 0, 0)
		Point origin = new Point();
		getDiagramGraphicalViewer().setProperty(
			SnapToGrid.PROPERTY_GRID_ORIGIN, origin);

		// Grid Spacing
		double dSpacing = ((DiagramRootEditPart) getDiagramEditPart().getRoot())
			.getGridSpacing();
		((DiagramRootEditPart) getDiagramEditPart().getRoot())
			.setGridSpacing(dSpacing);
	}

	/**
	 * Returns the elements in the given selection.
	 * @param selection  the selection
	 * @return a list of <code>EObject</code>
	 */
	protected List getElements(final ISelection selection) {
		if (selection instanceof IStructuredSelection) {

			return (List) MEditingDomainGetter.getMEditingDomain(
				((IStructuredSelection) selection).toList()).runAsRead(
				new MRunnable() {

					public Object run() {
						List retval = new ArrayList();
						if (selection instanceof IStructuredSelection) {
							IStructuredSelection structuredSelection = (IStructuredSelection) selection;

							for (Iterator i = structuredSelection.iterator(); i
								.hasNext();) {
								Object next = i.next();

								View view = (View) ((IAdaptable) next)
									.getAdapter(View.class);
								if (view != null) {
									EObject eObject = ViewUtil
										.resolveSemanticElement(view);
									if (eObject != null) {
										retval.add(eObject);
									} else {
										retval.add(view);
									}
								}
							}
						}
						return retval;
					}
				});
		}
		return Collections.EMPTY_LIST;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.IShowInSource#getShowInContext()
	 */
	public ShowInContext getShowInContext() {

		ISelection selection = getGraphicalViewer().getSelection();
		return new ShowInContext( null, selection );
	}
	
	/**
	 * Gets the preferences hint that will be used to determine which preference
	 * store to use when retrieving diagram preferences for this diagram editor.
	 * The preference hint is mapped to a preference store in the preference
	 * registry <@link DiagramPreferencesRegistry>. By default, this method
	 * returns a preference hint configured with the id of the editor. If a
	 * preference store has not been registered against this editor id in the
	 * diagram preferences registry, then the default values will be used.
	 * 
	 * @return the preferences hint to be used to configure the
	 *         <code>RootEditPart</code>
	 */
	protected PreferencesHint getPreferencesHint() {
		return new PreferencesHint(getEditorSite().getId());
	};
}
