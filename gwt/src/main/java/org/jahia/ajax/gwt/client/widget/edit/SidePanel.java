package org.jahia.ajax.gwt.client.widget.edit;

import com.allen_sauer.gwt.log.client.Log;
import com.extjs.gxt.ui.client.Style;
import com.extjs.gxt.ui.client.data.BaseTreeLoader;
import com.extjs.gxt.ui.client.data.RpcProxy;
import com.extjs.gxt.ui.client.data.TreeLoader;
import com.extjs.gxt.ui.client.dnd.GridDragSource;
import com.extjs.gxt.ui.client.dnd.TreePanelDragSource;
import com.extjs.gxt.ui.client.dnd.DragSource;
import com.extjs.gxt.ui.client.event.*;
import com.extjs.gxt.ui.client.store.ListStore;
import com.extjs.gxt.ui.client.store.TreeStore;
import com.extjs.gxt.ui.client.widget.*;
import com.extjs.gxt.ui.client.widget.button.Button;
import com.extjs.gxt.ui.client.widget.form.*;
import com.extjs.gxt.ui.client.widget.grid.*;
import com.extjs.gxt.ui.client.widget.layout.FitLayout;
import com.extjs.gxt.ui.client.widget.layout.VBoxLayout;
import com.extjs.gxt.ui.client.widget.layout.VBoxLayoutData;
import com.extjs.gxt.ui.client.widget.toolbar.ToolBar;
import com.extjs.gxt.ui.client.widget.toolbar.FillToolItem;
import com.extjs.gxt.ui.client.widget.treepanel.TreePanel;
import com.google.gwt.user.client.rpc.AsyncCallback;
import org.jahia.ajax.gwt.client.data.definition.GWTJahiaNodeType;
import org.jahia.ajax.gwt.client.data.definition.GWTJahiaItemDefinition;
import org.jahia.ajax.gwt.client.data.node.GWTJahiaNode;
import org.jahia.ajax.gwt.client.data.node.GWTJahiaGetPropertiesResult;
import org.jahia.ajax.gwt.client.messages.Messages;
import org.jahia.ajax.gwt.client.service.content.JahiaContentManagementService;
import org.jahia.ajax.gwt.client.service.definition.JahiaContentDefinitionService;
import org.jahia.ajax.gwt.client.util.content.JCRClientUtils;
import org.jahia.ajax.gwt.client.util.icons.ContentModelIconProvider;
import org.jahia.ajax.gwt.client.widget.definition.PropertiesEditor;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * The Siade Panel of the edit mode.
 * This panel will contain a create/browse/search area.
 * An area binded to the previous one dispalying the list of object type available for creation/reference.
 * A third area for displaying preview/properties of content selected anywhere in the page or in the previous area.
 * This third area allows to select the template for any particular content. 
 */
public class SidePanel extends ContentPanel {

    private boolean init = false;
    private ListStore<GWTJahiaNode> displayStore;
    private ListStore<GWTJahiaNodeType> displayTypesStore;
    private ContentPanel contentList;
    private Map<GWTJahiaNodeType, List<GWTJahiaNodeType>> definitions;
    private Grid<GWTJahiaNode> displayGrid;
    private Grid<GWTJahiaNodeType> displayTypesGrid;
    private EditLinker editLinker;
    private ContentPanel displayPanel;
    private ContentPanel repository;
    private PreviewPanel preview;
    private TabItem propertiesTabItem;
    private TabItem previewTabItem;

    private GridDragSource createGridSource;
    private TreePanelDragSource contentTreeSource;
    private GridDragSource displayGridSource;
    private DragSource querySource;
    private TabPanel displayTabs;

    public SidePanel() {
        super();
        setHeaderVisible(true);
        VBoxLayout layout = new VBoxLayout();
        layout.setVBoxLayoutAlign(VBoxLayout.VBoxLayoutAlign.STRETCH);
        setLayout(layout);

        createRepositoryPanel();
        createContentListPanel();
        createDisplayPanel();

        // add to side panel
        VBoxLayoutData vBoxData = new VBoxLayoutData(0, 0, 0, 0);
        vBoxData.setFlex(1);

        add(repository, vBoxData);
        add(contentList, vBoxData);
        add(displayPanel, vBoxData);

    }

    public void initWithLinker(EditLinker editLinker) {
        this.editLinker = editLinker;

        displayGridSource.addDNDListener(editLinker.getDndListener());
        createGridSource.addDNDListener(editLinker.getDndListener());
        contentTreeSource.addDNDListener(editLinker.getDndListener());
        querySource.addDNDListener(editLinker.getDndListener());

        preview.initWithLinker(editLinker);
    }

    private void createDisplayPanel() {
        displayPanel = new ContentPanel();
        displayPanel.setHeading(Messages.getResource("fm_information"));
        displayPanel.setLayout(new FitLayout());
        displayPanel.setBorders(false);
        displayPanel.setBodyBorder(false);
        displayPanel.getHeader().setBorders(false);
        displayPanel.setCollapsible(true);

        displayTabs = new TabPanel();
        previewTabItem = new TabItem(Messages.getResource("fm_preview"));
        previewTabItem.setLayout(new FitLayout());

        preview = new PreviewPanel();
        previewTabItem.add(preview);

//        previewDragSource = new PreviewDragSource(previewTabItem);

        displayTabs.add(previewTabItem);
        propertiesTabItem = new TabItem(Messages.getResource("fm_properties"));
        propertiesTabItem.setLayout(new FitLayout());
        displayTabs.add(previewTabItem);
        displayTabs.add(propertiesTabItem);
        displayPanel.add(displayTabs);        
    }

    private void createContentListPanel() {
        // content list panel
        contentList = new ContentPanel();
        contentList.setHeading(Messages.getResource("em_contentlist"));
        contentList.setLayout(new FitLayout());
        contentList.setCollapsible(true);
        displayStore = new ListStore<GWTJahiaNode>();
        List<ColumnConfig> displayColumns = new ArrayList<ColumnConfig>();

        ColumnConfig col = new ColumnConfig("ext", "", 40);
        col.setAlignment(Style.HorizontalAlignment.CENTER);
        col.setRenderer(new GridCellRenderer<GWTJahiaNode>() {
            public String render(GWTJahiaNode modelData, String s, ColumnData columnData, int i, int i1, ListStore<GWTJahiaNode> listStore, Grid<GWTJahiaNode> g) {
                return ContentModelIconProvider.getInstance().getIcon(modelData).getHTML();
            }
        });
        displayColumns.add(col);
        displayColumns.add(new ColumnConfig("name", Messages.getResource("fm_info_name"), 250));
//        displayColumns.add(new ColumnConfig("path", "Path", 200));
        displayGrid = new Grid<GWTJahiaNode>(displayStore, new ColumnModel(displayColumns));
        displayGrid.setBorders(false);
        displayGrid.getSelectionModel().setSelectionMode(Style.SelectionMode.SINGLE);
        displayGrid.getSelectionModel().addSelectionChangedListener(new SelectionChangedListener<GWTJahiaNode>() {
            public void selectionChanged(SelectionChangedEvent<GWTJahiaNode> gwtJahiaNodeSelectionChangedEvent) {
                editLinker.onDisplayGridSelection(gwtJahiaNodeSelectionChangedEvent.getSelectedItem());
            }
        });
        displayGridSource = new DisplayGridDragSource(displayGrid);
        contentList.add(displayGrid);

        // Second grid : display types

        displayTypesStore = new ListStore<GWTJahiaNodeType>();

        displayColumns = new ArrayList<ColumnConfig>();
        col = new ColumnConfig("ext", "", 40);
        col.setAlignment(Style.HorizontalAlignment.CENTER);
        col.setRenderer(new GridCellRenderer<GWTJahiaNodeType>() {
            public Object render(GWTJahiaNodeType model, String property, ColumnData config, int rowIndex, int colIndex, ListStore<GWTJahiaNodeType> gwtJahiaNodeTypeListStore, Grid<GWTJahiaNodeType> gwtJahiaNodeTypeGrid) {
                return ContentModelIconProvider.getInstance().getIcon(model).getHTML();
            }
        });
        displayColumns.add(col);
        displayColumns.add(new ColumnConfig("label", Messages.getResource("fm_info_name"), 250));

        displayTypesGrid = new Grid<GWTJahiaNodeType>(displayTypesStore, new ColumnModel(displayColumns));
        displayTypesGrid.setBorders(false);
        displayTypesGrid.getSelectionModel().setSelectionMode(Style.SelectionMode.SINGLE);
        createGridSource = new CreateGridDragSource(displayTypesGrid);
    }

    private ContentPanel createRepositoryPanel() {
        // repository panel
        repository = new ContentPanel();
        repository.setHeading(Messages.getResource("em_repository"));
        repository.setLayout(new FitLayout());
        repository.setBorders(false);
        repository.setBodyBorder(false);
        repository.getHeader().setBorders(false);
        repository.setCollapsible(true);

        TabPanel repositoryTabs = new TabPanel();

        // First tab : creating
        TabItem create = new TabItem(Messages.getResource("fm_newcontent"));
        create.setLayout(new FitLayout());
        final ListStore<GWTJahiaNodeType> createStore = new ListStore<GWTJahiaNodeType>();
        JahiaContentDefinitionService.App.getInstance().getNodeTypes(new AsyncCallback<Map<GWTJahiaNodeType, List<GWTJahiaNodeType>>>() {
            public void onFailure(Throwable caught) {
                MessageBox.alert("Alert", "Unable to load content definitions. Cause: "
                                          + caught.getLocalizedMessage(), null);
            }

            public void onSuccess(Map<GWTJahiaNodeType, List<GWTJahiaNodeType>> result) {
                definitions = result;
                List<GWTJahiaNodeType> list = new ArrayList<GWTJahiaNodeType>(result.keySet());
                for (GWTJahiaNodeType type : list) {
                    type.set("iconHtml", ContentModelIconProvider.getInstance().getIcon(type).getHTML());
                }
                createStore.add(list);
            }

        });

        ListView<GWTJahiaNodeType> createView = new ListView<GWTJahiaNodeType>();
        createView.setBorders(false);
        createView.setTemplate(getTemplate());
        createView.setStore(createStore);
        createView.setItemSelector("div.x-view-item");
        createView.setDisplayProperty("label");
        createView.getSelectionModel().setSelectionMode(Style.SelectionMode.SINGLE);
        createView.getSelectionModel().addSelectionChangedListener(new SelectionChangedListener<GWTJahiaNodeType>() {
            public void selectionChanged(SelectionChangedEvent<GWTJahiaNodeType> gwtJahiaNodeSelectionChangedEvent) {
                GWTJahiaNodeType selected = gwtJahiaNodeSelectionChangedEvent.getSelectedItem();
                if (selected != null) {
                    List<GWTJahiaNodeType> list = definitions.get(selected);
                    setDisplayTypesContent(list);
                } else {
                    setDisplayTypesContent(null);
                }
                editLinker.onCreateGridSelection(selected);
            }
        });
        create.add(createView);

        // Second tab : browse
        TabItem browse = new TabItem(Messages.getResource("fm_browse"));
        browse.setLayout(new FitLayout());
        // data proxy
        RpcProxy<List<GWTJahiaNode>> privateProxy = new RpcProxy<List<GWTJahiaNode>>() {
            @Override
            protected void load(Object gwtJahiaFolder, AsyncCallback<List<GWTJahiaNode>> listAsyncCallback) {
                if (init) {
                    JahiaContentManagementService.App.getInstance().getRoot(JCRClientUtils.GLOBAL_REPOSITORY, "", "", "", null, listAsyncCallback);
                } else {
                    JahiaContentManagementService.App.getInstance().ls(JCRClientUtils.GLOBAL_REPOSITORY,(GWTJahiaNode) gwtJahiaFolder, "", "", "", null, false, listAsyncCallback);
                }
            }
        };
        TreeLoader<GWTJahiaNode> loader = new BaseTreeLoader<GWTJahiaNode>(privateProxy) {
            @Override
            public boolean hasChildren(GWTJahiaNode parent) {
                return parent.hasFolderChildren();
            }

            protected void onLoadSuccess(Object gwtJahiaNode, List<GWTJahiaNode> gwtJahiaNodes) {
                super.onLoadSuccess(gwtJahiaNode, gwtJahiaNodes);
                if (init) {
                    Log.debug("setting init to false");
                    init = false;
                }
            }
        };
        TreeStore<GWTJahiaNode> treeStore = new TreeStore<GWTJahiaNode>(loader);
        TreePanel<GWTJahiaNode> m_tree = new TreePanel<GWTJahiaNode>(treeStore);
        m_tree.setIconProvider(ContentModelIconProvider.getInstance());
        m_tree.setDisplayProperty("displayName");
        m_tree.setBorders(false);
        m_tree.getSelectionModel().addSelectionChangedListener(new SelectionChangedListener<GWTJahiaNode>() {
            public void selectionChanged(SelectionChangedEvent<GWTJahiaNode> gwtJahiaNodeSelectionChangedEvent) {
                GWTJahiaNode selected = gwtJahiaNodeSelectionChangedEvent.getSelectedItem();
                if (selected != null) {
                    if (selected.hasChildren()) {
                        JahiaContentManagementService.App.getInstance().ls(null,gwtJahiaNodeSelectionChangedEvent.getSelectedItem(), null, null, null, null, false, new AsyncCallback<List<GWTJahiaNode>>() {
                            public void onFailure(Throwable throwable) {
                                // TODO
                            }

                            public void onSuccess(List<GWTJahiaNode> gwtJahiaNodes) {
                                setDisplayContent(gwtJahiaNodes);
                            }
                        });
                    } else {
                        List<GWTJahiaNode> list = new ArrayList<GWTJahiaNode>(1);
                        list.add(selected);
                        setDisplayContent(list);
                    }
                } else {
                    setDisplayContent(null);
                }
                editLinker.onBrowseTreeSelection(selected);
            }
        });
        contentTreeSource = new ContentTreeDragSource(m_tree);
        browse.add(m_tree);

        // searching
        TabItem search = new TabItem(Messages.getResource("fm_search"));
        FormPanel searchForm = new FormPanel();
        searchForm.setHeaderVisible(false);
        searchForm.setBorders(false);
        searchForm.setBodyBorder(false);
        final TextField<String> searchField = new TextField<String>();
        searchField.setFieldLabel(Messages.getResource("fm_search"));
        Button ok = new Button(Messages.getResource("fm_search"), new SelectionListener<ButtonEvent>() {
            public void componentSelected(ButtonEvent e) {
                search(searchField.getValue(), null,null);
            }
        });

        Button drag = new Button(Messages.getResource("em_drag"));
        querySource = new EditModeDragSource(drag) {
            @Override
            protected void onDragStart(DNDEvent e) {
                e.setCancelled(false);
                e.getStatus().update(searchField.getValue());
                e.getStatus().setStatus(true);
                e.setData(searchField);
                e.getStatus().setData(EditModeDNDListener.SOURCE_TYPE, EditModeDNDListener.QUERY_SOURCE_TYPE);
                e.getStatus().setData(EditModeDNDListener.SOURCE_QUERY, searchField.getValue());
                super.onDragStart(e);
            }
        };

        searchForm.add(searchField);
        HorizontalPanel h = new HorizontalPanel();
        h.add(ok);
        h.add(drag);        
        searchForm.add(h);
        search.add(searchForm);

        repositoryTabs.add(create);
        repositoryTabs.add(browse);
        repositoryTabs.add(search);
        repository.add(repositoryTabs);
        return repository;
    }

    /**
     * Method used by the search form
     *
     * @param query      the query string
     * @param date       search for items newer than date
     * @param searchRoot search within this path
     */
    private void search(String query, Date date, String searchRoot) {
        JahiaContentManagementService.App.getInstance().search(query, 500, new AsyncCallback<List<GWTJahiaNode>>() {
            public void onFailure(Throwable throwable) {
                // TODO : implement search
            }

            public void onSuccess(List<GWTJahiaNode> gwtJahiaNodes) {
                setDisplayContent(gwtJahiaNodes);
            }
        });
    }

    /**
     * Clear the display list and add the provided list.
     *
     * @param content items to add
     */
    private void setDisplayContent(List<GWTJahiaNode> content) {
        contentList.removeAll();
        displayStore.removeAll();
        if (content != null) {
            displayStore.add(content);
        }
        contentList.add(displayGrid);
        contentList.layout();
    }

    private void setDisplayTypesContent(List<GWTJahiaNodeType> content) {
        contentList.removeAll();
        displayTypesStore.removeAll();
        if (content != null) {
            displayTypesStore.add(content);
        }
        contentList.add(displayTypesGrid);
        contentList.layout();
    }

    private native String getTemplate() /*-{
    return ['<tpl for=".">',
        '<div class=x-view-item> {iconHtml} {label}</div>',
        '</tpl>',
        '<div class="x-clear"></div>'].join("");

}-*/;

    public void handleNewModuleSelection(Module selectedModule) {
        preview.handleNewModuleSelection(selectedModule);
        if (selectedModule != null) {
            if (displayTabs.getSelectedItem() == propertiesTabItem) {
                displayProperties(selectedModule.getNode());
            }
        } else {
            displayProperties(null);
        }
    }

    public void handleNewSidePanelSelection(GWTJahiaNode node) {
        preview.handleNewSidePanelSelection(node);
        displayProperties(node);

    }

    /**
     * Clear the properties panel and display the current node properties
     *
     * @param node the current node
     */
    private void displayProperties(final GWTJahiaNode node) {
        propertiesTabItem.removeAll();
        if (node != null) {
            JahiaContentManagementService.App.getInstance().getProperties(node.getPath(), new AsyncCallback<GWTJahiaGetPropertiesResult>() {
                public void onFailure(Throwable throwable) {
                    Log.debug("Cannot get properties", throwable);
                }

                public void onSuccess(GWTJahiaGetPropertiesResult result) {
                    final List<GWTJahiaNode> elements = new ArrayList<GWTJahiaNode>();
                    elements.add(node);

                    final PropertiesEditor propertiesEditor = new PropertiesEditor(result.getNodeTypes(), result.getProperties(), false, true, GWTJahiaItemDefinition.METADATA, null, null,node.isWriteable(), false);

                    ToolBar toolBar = new ToolBar();
                    propertiesEditor.setTopComponent(toolBar);
                    Button item = new Button(Messages.getResource("fm_save"));
                    item.setIconStyle("gwt-icons-save");
                    item.setEnabled(node.isWriteable() && !node.isLocked());
                    item.addSelectionListener(new SelectionListener<ButtonEvent>() {
                        public void componentSelected(ButtonEvent event) {
                            JahiaContentManagementService.App.getInstance().saveProperties(elements, propertiesEditor.getProperties(), new AsyncCallback() {
                                public void onFailure(Throwable throwable) {
                                    com.google.gwt.user.client.Window.alert("Properties save failed\n\n" + throwable.getLocalizedMessage());
                                    Log.error("failed", throwable);
                                }

                                public void onSuccess(Object o) {
                                    Info.display("", "Properties saved");
                                    //getLinker().refreshTable();
                                }
                            });
                        }
                    });
                    toolBar.add(new FillToolItem());
                    toolBar.add(item);
                    item = new Button(Messages.getResource("fm_restore"));
                    item.setIconStyle("gwt-icons-restore");
                    item.setEnabled(node.isWriteable() && !node.isLocked());

                    item.addSelectionListener(new SelectionListener<ButtonEvent>() {
                        public void componentSelected(ButtonEvent event) {
                            propertiesEditor.resetForm();
                        }
                    });
                    toolBar.add(item);
                    toolBar.setVisible(true);
                    propertiesTabItem.add(propertiesEditor);
                    propertiesTabItem.layout();
                }
            });
        }
    }

}


