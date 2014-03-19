/**
 * This file is part of Jahia, next-generation open source CMS:
 * Jahia's next-generation, open source CMS stems from a widely acknowledged vision
 * of enterprise application convergence - web, search, document, social and portal -
 * unified by the simplicity of web content management.
 *
 * For more information, please visit http://www.jahia.com.
 *
 * Copyright (C) 2002-2014 Jahia Solutions Group SA. All rights reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 * As a special exception to the terms and conditions of version 2.0 of
 * the GPL (or any later version), you may redistribute this Program in connection
 * with Free/Libre and Open Source Software ("FLOSS") applications as described
 * in Jahia's FLOSS exception. You should have received a copy of the text
 * describing the FLOSS exception, and it is also available here:
 * http://www.jahia.com/license
 *
 * Commercial and Supported Versions of the program (dual licensing):
 * alternatively, commercial and supported versions of the program may be used
 * in accordance with the terms and conditions contained in a separate
 * written agreement between you and Jahia Solutions Group SA.
 *
 * If you are unsure which license is appropriate for your use,
 * please contact the sales department at sales@jahia.com.
 */

package org.jahia.modules.rolesmanager;

import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.util.ISO9075;
import org.jahia.api.Constants;
import org.jahia.data.templates.JahiaTemplatesPackage;
import org.jahia.services.content.JCRContentUtils;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.nodetypes.ExtendedNodeType;
import org.jahia.services.content.nodetypes.NodeTypeRegistry;
import org.jahia.services.templates.JahiaTemplateManagerService;
import org.jahia.utils.i18n.Messages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.binding.message.MessageBuilder;
import org.springframework.binding.message.MessageContext;
import org.springframework.context.i18n.LocaleContextHolder;

import javax.jcr.*;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import java.io.Serializable;
import java.util.*;

public class RolesAndPermissionsHandler implements Serializable {

    private static final long serialVersionUID = 7910715831938629654L;

    private static Logger logger = LoggerFactory.getLogger(RolesAndPermissionsHandler.class);

//    enum Scope {CONTENT, SITE, SERVER_SETTINGS, STUDIO, JCR, OTHER};


    @Autowired
    private transient RoleTypeConfiguration roleTypes;

    @Autowired
    private transient JahiaTemplateManagerService templateManagerService;

    private RoleBean roleBean = new RoleBean();

    private String currentContext;
    private String currentGroup;
    private List<String> uuids;

    private transient List<JCRNodeWrapper> allPermissions;

    public RolesAndPermissionsHandler() {
    }

    public RoleTypeConfiguration getRoleTypes() {
        return roleTypes;
    }

    public RoleBean getRoleBean() {
        return roleBean;
    }

    public void setRoleBean(RoleBean roleBean) {
        this.roleBean = roleBean;
        this.currentContext = "current";
        this.currentGroup = roleBean.getPermissions().get(currentContext).keySet().iterator().next();
    }

    private JCRSessionWrapper getSession() throws RepositoryException {
        return getSession(LocaleContextHolder.getLocale());
    }

    private JCRSessionWrapper getSession(Locale locale) throws RepositoryException {
        return JCRSessionFactory.getInstance().getCurrentUserSession("default", locale);
    }

    public Map<String, List<RoleBean>> getRoles() throws RepositoryException {
        return getRoles(false,false);
    }

    public Map<String, List<RoleBean>> getRolesToDelete() throws RepositoryException {
        return getRoles(true,true);
    }


    public Map<String, List<RoleBean>> getSelectedRoles() throws RepositoryException {
        return getRoles(true,false);
    }

    public Map<String, List<RoleBean>> getRoles(boolean filterUUIDs, boolean getChildren) throws RepositoryException {

        QueryManager qm = getSession().getWorkspace().getQueryManager();
        StringBuilder statement = new StringBuilder("select * from [jnt:role] as role");
        if (filterUUIDs) {
            statement.append(" where ");
            Iterator<String> it = uuids.iterator();
            while (it.hasNext()) {
                String uuid = it.next();
                statement.append("[jcr:uuid] = '").append(uuid).append("'");
                if (getChildren) {
                    statement.append(" or isdescendantnode(role, ['").append(getSession().getNodeByIdentifier(uuid).getPath()).append("'])");
                }
                if (it.hasNext()) {
                    statement.append(" or ");
                }
            }
        }
        Query q = qm.createQuery(statement.toString(), Query.JCR_SQL2);
        Map<String, List<RoleBean>> all = new LinkedHashMap<String, List<RoleBean>>();
        if (!filterUUIDs) {
            for (RoleType roleType : roleTypes.getValues()) {
                all.put(roleType.getName(), new ArrayList<RoleBean>());
            }
        }

        NodeIterator ni = q.execute().getNodes();
        while (ni.hasNext()) {
            JCRNodeWrapper next = (JCRNodeWrapper) ni.next();
            if (!next.getName().equals("privileged")) {
                RoleBean role = getRole(next.getIdentifier(), false);
                String key = role.getRoleType().getName();
                if (!all.containsKey(key)) {
                    all.put(key, new ArrayList<RoleBean>());
                }
                all.get(key).add(role);
            }
        }
        for (List<RoleBean> roleBeans : all.values()) {
            Collections.sort(roleBeans, new Comparator<RoleBean>() {
                @Override
                public int compare(RoleBean o1, RoleBean o2) {
                    return o1.getPath().compareTo(o2.getPath());
                }
            });
        }

        return all;
    }

    public RoleBean getRole(String uuid, boolean getPermissions) throws RepositoryException {
        JCRSessionWrapper currentUserSession = getSession();

        JCRNodeWrapper role = currentUserSession.getNodeByIdentifier(uuid);

        RoleBean roleBean = new RoleBean();
        roleBean.setUuid(role.getIdentifier());
        roleBean.setName(role.getName());
        roleBean.setPath(role.getPath());
        roleBean.setDepth(role.getDepth());

        JCRNodeWrapper n;
        Map<String, I18nRoleProperties> i18nRoleProperties = new TreeMap<String, I18nRoleProperties>();
        for (Locale l : role.getExistingLocales()) {
            n = getSession(l).getNodeByIdentifier(uuid);
            if (!n.hasProperty(Constants.JCR_TITLE) && !n.hasProperty(Constants.JCR_DESCRIPTION)) {
                i18nRoleProperties.put(l.getLanguage(), null);
                continue;
            }
            I18nRoleProperties properties = new I18nRoleProperties();
            properties.setLanguage(l.getDisplayName(LocaleContextHolder.getLocale()));
            if (n.hasProperty(Constants.JCR_TITLE)) {
                properties.setTitle(n.getProperty(Constants.JCR_TITLE).getString());
            }
            if (n.hasProperty(Constants.JCR_DESCRIPTION)) {
                properties.setDescription(n.getProperty(Constants.JCR_DESCRIPTION).getString());
            }
            i18nRoleProperties.put(l.getLanguage(), properties);
        }
        roleBean.setI18nProperties(i18nRoleProperties);

        if (role.hasProperty("j:hidden")) {
            roleBean.setHidden(role.getProperty("j:hidden").getBoolean());
        }

        String roleGroup = role.getProperty("j:roleGroup").getString();

        RoleType roleType = roleTypes.get(roleGroup);
        roleBean.setRoleType(roleType);
        if (getPermissions) {
            List<String> tabs = new ArrayList<String>(roleBean.getRoleType().getScopes());

            Map<String, List<String>> permIdsMap = new HashMap<String, List<String>>();
            fillPermIds(role, tabs, permIdsMap, false);

            Map<String, List<String>> inheritedPermIdsMap = new HashMap<String, List<String>>();
            fillPermIds(role.getParent(), tabs, inheritedPermIdsMap, true);


            Map<String, Map<String, Map<String, PermissionBean>>> permsForRole = new LinkedHashMap<String, Map<String, Map<String, PermissionBean>>>();
            roleBean.setPermissions(permsForRole);

            for (String tab : tabs) {
                addPermissionsForScope(roleBean, tab, permIdsMap, inheritedPermIdsMap);
            }

            if (roleType.getAvailableNodeTypes() != null) {
                List<String> nodeTypesOnRole = new ArrayList<String>();
                if (role.hasProperty("j:nodeTypes")) {
                    for (Value value : role.getProperty("j:nodeTypes").getValues()) {
                        nodeTypesOnRole.add(value.getString());
                    }
                }


                SortedSet<NodeType> nodeTypes = new TreeSet<NodeType>();
                for (String s : roleType.getAvailableNodeTypes()) {
                    boolean includeSubtypes = false;
                    if (s.endsWith("/*")) {
                        s = StringUtils.substringBeforeLast(s,"/*");
                        includeSubtypes = true;
                    }
                    ExtendedNodeType t = NodeTypeRegistry.getInstance().getNodeType(s);
                    nodeTypes.add(new NodeType(t.getName(),t.getLabel(LocaleContextHolder.getLocale()),nodeTypesOnRole.contains(t.getName())));
                    if (includeSubtypes) {
                        for (ExtendedNodeType sub : t.getSubtypesAsList()) {
                            nodeTypes.add(new NodeType(sub.getName(),sub.getLabel(LocaleContextHolder.getLocale()),nodeTypesOnRole.contains(sub.getName())));
                        }
                    }
                }
                roleBean.setNodeTypes(nodeTypes);
            }
        }

        return roleBean;
    }

    public void revertRole() throws RepositoryException {
        roleBean = getRole(roleBean.getUuid(), true);
    }

    private void fillPermIds(JCRNodeWrapper role, List<String> tabs, Map<String, List<String>> permIdsMap, boolean recursive) throws RepositoryException {
        if (!role.isNodeType(Constants.JAHIANT_ROLE)) {
            return;
        }

        if (recursive) {
            fillPermIds(role.getParent(), tabs, permIdsMap, true);
        }

        final ArrayList<String> setPermIds = new ArrayList<String>();
        permIdsMap.put("current", setPermIds);

        if (role.hasProperty("j:permissionNames")) {
            Value[] values = role.getProperty("j:permissionNames").getValues();
            for (Value value : values) {
                String valueString = value.getString();
                if (!setPermIds.contains(valueString)) {
                    setPermIds.add(valueString);
                }
            }
        }


        NodeIterator ni = role.getNodes();
        while (ni.hasNext()) {
            JCRNodeWrapper next = (JCRNodeWrapper) ni.next();
            if (next.isNodeType("jnt:externalPermissions")) {
                try {
                    String path = next.getProperty("j:path").getString();
                    permIdsMap.put(path, new ArrayList<String>());
                    Value[] values = next.getProperty("j:permissionNames").getValues();
                    for (Value value : values) {
                        List<String> ids = permIdsMap.get(path);
                        String valueString = value.getString();
                        if (!ids.contains(valueString)) {
                            ids.add(valueString);
                        }
                        if (!tabs.contains(path)) {
                            tabs.add(path);
                        }
                    }
                } catch (RepositoryException e) {
                    logger.error("Cannot initialize role " + next.getPath(), e);
                } catch (IllegalStateException e) {
                    logger.error("Cannot initialize role " + next.getPath(), e);
                }
            }
        }
    }

    public boolean addRole(String roleName, String parentRoleId, String roleTypeString, MessageContext messageContext) throws RepositoryException {
        JCRSessionWrapper currentUserSession = getSession();

        if (StringUtils.isBlank(roleName)) {
            messageContext.addMessage(new MessageBuilder().source("roleName")
                    .defaultText(getMessage("rolesmanager.rolesAndPermissions.role.noName"))
                    .error()
                    .build());
            return false;
        }
        roleName = JCRContentUtils.generateNodeName(roleName);

        NodeIterator nodes = currentUserSession.getWorkspace().getQueryManager().createQuery(
                "select * from [" + Constants.JAHIANT_ROLE + "] as r where localname()='" + roleName + "' and isdescendantnode(r,['/roles'])",
                Query.JCR_SQL2).execute().getNodes();
        if (nodes.hasNext()) {
            messageContext.addMessage(new MessageBuilder().source("roleName")
                    .defaultText(getMessage("rolesmanager.rolesAndPermissions.role.exists"))
                    .error()
                    .build());
            return false;
        }

        JCRNodeWrapper parent;
        if (StringUtils.isBlank(parentRoleId)) {
            parent = currentUserSession.getNode("/roles");
        } else {
            parent = currentUserSession.getNodeByIdentifier(parentRoleId);
        }
        JCRNodeWrapper role = parent.addNode(roleName, "jnt:role");
        RoleType roleType = roleTypes.get(roleTypeString);
        role.setProperty("j:roleGroup", roleType.getName());
        role.setProperty("j:privilegedAccess", roleType.isPrivileged());
        if (roleType.getDefaultNodeTypes() != null) {
            List<Value> values = new ArrayList<Value>();
            for (String nodeType : roleType.getDefaultNodeTypes()) {
                values.add(currentUserSession.getValueFactory().createValue(nodeType));
            }
            role.setProperty("j:nodeTypes", values.toArray(new Value[values.size()]));
        }
        role.setProperty("j:roleGroup", roleType.getName());

        currentUserSession.save();
        this.setRoleBean(getRole(role.getIdentifier(), true));
        return true;
    }

    public void selectRoles(String uuids) throws RepositoryException {
        this.uuids = Arrays.asList(uuids.split(","));
    }

    public boolean deleteRoles() throws RepositoryException {
        JCRSessionWrapper currentUserSession = getSession();
        for (String uuid : uuids) {
            try {
                currentUserSession.getNodeByIdentifier(uuid).remove();
            } catch (ItemNotFoundException e) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Cannot find role " + uuid);
                }
            }
        }
        currentUserSession.save();
        return true;
    }

    private void addPermissionsForScope(RoleBean roleBean, String scope, Map<String, List<String>> permIdsMap, Map<String, List<String>> inheritedPermIdsMap) throws RepositoryException {
        final Map<String, Map<String, Map<String, PermissionBean>>> permissions = roleBean.getPermissions();
        if (!permissions.containsKey(scope)) {
            permissions.put(scope, new LinkedHashMap<String, Map<String, PermissionBean>>());
        }
        List<JCRNodeWrapper> allPermissions = getAllPermissions();

        String type = null;
        final Map<String, List<String>> globalPermissionsGroups = roleTypes.getPermissionsGroups();
        final Map<String, List<String>> permissionsGroupsForRoleType = roleBean.getRoleType().getPermissionsGroups();

        if (scope.equals("current")) {
            if (roleBean.getRoleType().getDefaultNodeTypes() != null && roleBean.getRoleType().getDefaultNodeTypes().size() == 1) {
                type = roleBean.getRoleType().getDefaultNodeTypes().get(0);
            }
        } else {
            if (scope.equals("currentSite")) {
                type = "jnt:virtualsite";
            } else {
                try {
                    type = getSession().getNode(scope).getPrimaryNodeTypeName();
                } catch (RepositoryException e) {
                    logger.error("Error retrieving scope", e);
                }
            }
        }
        if (type == null || (!globalPermissionsGroups.containsKey(type) && (permissionsGroupsForRoleType == null || !permissionsGroupsForRoleType.containsKey(type)))) {
            type = "nt:base";
        }
        if (permissionsGroupsForRoleType != null && permissionsGroupsForRoleType.containsKey(type)) {
            for (String s : permissionsGroupsForRoleType.get(type)) {
                permissions.get(scope).put(s, new TreeMap<String, PermissionBean>());
            }
        } else {
            for (String s : globalPermissionsGroups.get(type)) {
                permissions.get(scope).put(s, new TreeMap<String, PermissionBean>());
            }
        }

        Map<String, PermissionBean> mappedPermissions = new HashMap<String, PermissionBean>();

        Map<String, String> allGroups = new HashMap<String, String>();
        for (String s : permissions.get(scope).keySet()) {
            for (String s1 : Arrays.asList(s.split(","))) {
                allGroups.put(s1, s);
            }
        }

        // Create mapped permissions
        for (Map.Entry<String, List<String>> entry : roleTypes.getPermissionsMapping().entrySet()) {
            String[] splitPath = entry.getKey().split("/");
            String permissionGroup = splitPath[2];
            if (allGroups.containsKey(permissionGroup)) {
                Map<String, PermissionBean> p = permissions.get(scope).get(allGroups.get(permissionGroup));
                PermissionBean bean = new PermissionBean();
                bean.setUuid(null);
                bean.setParentPath(StringUtils.substringBeforeLast(entry.getKey(), "/"));
                bean.setName(StringUtils.substringAfterLast(entry.getKey(), "/"));
                String localName = StringUtils.substringAfterLast(entry.getKey(), "/");
                if (localName.contains(":")) {
                    localName = StringUtils.substringAfter(localName, ":");
                }
                String title = StringUtils.capitalize(localName.replaceAll("([A-Z])", " $0").replaceAll("[_-]", " ").toLowerCase());
                final String rbName = localName.replaceAll("-", "_");
                bean.setTitle(Messages.getInternal("label.permission." + rbName, LocaleContextHolder.getLocale(), title));
                bean.setDescription(Messages.getInternal("label.permission." + rbName + ".description", LocaleContextHolder.getLocale(), ""));
                bean.setPath(entry.getKey());
                bean.setDepth(splitPath.length - 1);
                bean.setMappedPermissions(new TreeMap<String, PermissionBean>());
                if (p.containsKey(bean.getParentPath())) {
                    p.get(bean.getParentPath()).setHasChildren(true);
                }

                p.put(entry.getKey(), bean);

                for (String s : entry.getValue()) {
                    createMappedPermission(s, bean, mappedPermissions);
                }
            }
        }

        // Create standard permissions
        for (JCRNodeWrapper permissionNode : allPermissions) {
            JCRNodeWrapper permissionGroup = getPermissionGroupNode(permissionNode);
            final String permissionPath = getPermissionPath(permissionNode);

            if (!mappedPermissions.containsKey(permissionPath) && mappedPermissions.containsKey(getPermissionPath(permissionNode.getParent()))) {
                final PermissionBean bean = mappedPermissions.get(getPermissionPath(permissionNode.getParent()));
                createMappedPermission(permissionPath, bean, mappedPermissions);
            }

            if (allGroups.containsKey(permissionGroup.getName()) && !mappedPermissions.containsKey(permissionPath)) {
                Map<String, PermissionBean> p = permissions.get(scope).get(allGroups.get(permissionGroup.getName()));
                if (!p.containsKey(permissionPath) || permissionNode.getPath().startsWith("/permissions")) {
                    PermissionBean bean = new PermissionBean();
                    setPermissionBeanProperties(permissionNode, bean);
                    if (p.containsKey(bean.getParentPath())) {
                        p.get(bean.getParentPath()).setHasChildren(true);
                    }
                    p.put(permissionPath, bean);
                    setPermissionFlags(permissionNode, p, bean, permIdsMap.get(scope), inheritedPermIdsMap.get(scope), p.get(bean.getParentPath()));
                }
            }
            if (mappedPermissions.containsKey(permissionPath)) {
                PermissionBean bean = mappedPermissions.get(permissionPath);

                Map<String, PermissionBean> p = permissions.get(scope).get(allGroups.get(bean.getPath().split("/")[2]));
                setPermissionFlags(permissionNode, p, bean, permIdsMap.get(scope), inheritedPermIdsMap.get(scope), p.get(bean.getParentPath()));
            }
        }

        // Auto expand permissions where mapped permissions are partially set
        for (Map<String, Map<String, PermissionBean>> map : roleBean.getPermissions().values()) {
            for (Map<String, PermissionBean> map2 : map.values()) {
                final Collection<PermissionBean> values = new ArrayList<PermissionBean>(map2.values());
                for (PermissionBean bean : values) {
                    if (bean.getMappedPermissions() != null) {
                        Boolean lastValue = null;
                        for (PermissionBean value : bean.getMappedPermissions().values()) {
                            if (lastValue == null) {
                                lastValue = value.isSuperSet() || value.isSet();
                            }
                            if (!lastValue.equals(value.isSuperSet() || value.isSet())) {
                                bean.setMappedPermissionsExpanded(true);
                                bean.setSet(false);
                                bean.setSuperSet(false);
                                bean.setPartialSet(true);
                                break;
                            }
                        }
                        if (bean.isMappedPermissionsExpanded()) {
                            for (PermissionBean mapped : bean.getMappedPermissions().values()) {
                                map2.put(mapped.getPath(), mapped);
                            }
                        }
                    }
                }
            }
        }
    }

    private void createMappedPermission(String permissionPath, PermissionBean parent, Map<String, PermissionBean> mappedPermissions) throws RepositoryException {
        PermissionBean mapped = new PermissionBean();
        setPermissionBeanProperties(getSession().getNode(permissionPath), mapped);
        mapped.setPath(parent.getPath() + "/" + mapped.getName());
        mapped.setParentPath(parent.getPath());
        mapped.setDepth(parent.getDepth() + 1);
        parent.getMappedPermissions().put(permissionPath, mapped);
        mappedPermissions.put(permissionPath, parent);
    }

    private void setPermissionFlags(JCRNodeWrapper permissionNode, Map<String, PermissionBean> permissions, PermissionBean bean, List<String> permIds, List<String> inheritedPermIds, PermissionBean parentBean) throws RepositoryException {
        if ((permIds != null && permIds.contains(permissionNode.getName()))
                || (parentBean != null && parentBean.isSet())) {
            bean.setSet(true);
            if (bean.getMappedPermissions() != null && bean.getMappedPermissions().containsKey(permissionNode.getPath())) {
                bean.getMappedPermissions().get(permissionNode.getPath()).setSet(true);
            }
            while (parentBean != null && !parentBean.isSet() && !parentBean.isSuperSet()) {
                parentBean.setPartialSet(true);
                parentBean = permissions.get(parentBean.getParentPath());
            }
        }
        parentBean = permissions.get(bean.getParentPath());
        if ((inheritedPermIds != null && inheritedPermIds.contains(permissionNode.getName()))
                || (parentBean != null && parentBean.isSuperSet())) {
            bean.setSuperSet(true);
            if (bean.getMappedPermissions() != null && bean.getMappedPermissions().containsKey(permissionNode.getPath())) {
                bean.getMappedPermissions().get(permissionNode.getPath()).setSuperSet(true);
            }
            while (parentBean != null && !parentBean.isSet() && !parentBean.isSuperSet()) {
                parentBean.setPartialSet(true);
                parentBean = permissions.get(parentBean.getParentPath());
            }
        }
    }

    private String getPermissionPath(JCRNodeWrapper permissionNode) {
        String path = permissionNode.getPath();
        if (path.startsWith("/modules")) {
            path = "/permissions/" + StringUtils.substringAfter(path, "/permissions/");
        }
        return path;
    }

    private String getPermissionModule(JCRNodeWrapper permissionNode) {
        String path = permissionNode.getPath();
        if (path.startsWith("/modules/")) {
            String s = StringUtils.substringAfter(path, "/modules/");
            return StringUtils.substringBefore(s, "/");
        }
        return null;
    }

    private int getPermissionDepth(JCRNodeWrapper permissionNode) throws RepositoryException {
        String path = permissionNode.getPath();
        if (path.startsWith("/modules")) {
            return permissionNode.getDepth() - 3;
        }
        return permissionNode.getDepth();
    }

    private JCRNodeWrapper getPermissionGroupNode(JCRNodeWrapper permissionNode) throws RepositoryException {
        JCRNodeWrapper permissionGroup = (JCRNodeWrapper) permissionNode.getAncestor(2);
        if (permissionGroup.isNodeType("jnt:module")) {
            permissionGroup = (JCRNodeWrapper) permissionNode.getAncestor(5);
        }
        return permissionGroup;
    }

    private void setPermissionBeanProperties(JCRNodeWrapper permissionNode, PermissionBean bean) throws RepositoryException {
        final String module = getPermissionModule(permissionNode);

        bean.setUuid(permissionNode.getIdentifier());

        bean.setParentPath(getPermissionPath(permissionNode.getParent()));
        bean.setName(permissionNode.getName());
        String localName = permissionNode.getName();
        if (localName.contains(":")) {
            localName = StringUtils.substringAfter(localName, ":");
        }
        String title = StringUtils.capitalize(localName.replaceAll("([A-Z])", " $0").replaceAll("[_-]", " ").toLowerCase());
        final String rbName = localName.replaceAll("-", "_");
        if (module != null) {
            bean.setTitle(Messages.get(templateManagerService.getTemplatePackageById(module), "label.permission." + rbName, LocaleContextHolder.getLocale(), title));
        } else {
            bean.setTitle(Messages.getInternal("label.permission." + rbName, LocaleContextHolder.getLocale(), title));
        }
        bean.setModule(module);
        bean.setDescription(Messages.getInternal("label.permission." + rbName + ".description", LocaleContextHolder.getLocale(), ""));
        bean.setPath(getPermissionPath(permissionNode));
        bean.setDepth(getPermissionDepth(permissionNode));
    }

    public String getCurrentContext() {
        return currentContext;
    }

    public void setCurrentContext(String tab) {
        currentContext = tab;
        this.currentGroup = roleBean.getPermissions().get(currentContext).keySet().iterator().next();
    }

    public String getCurrentGroup() {
        return currentGroup;
    }

    public void setCurrentGroup(String context, String currentGroup) {
        setCurrentContext(context);
        this.currentGroup = currentGroup;
    }

    public void storeValues(String[] selectedValues, String[] partialSelectedValues) {
        Map<String, PermissionBean> permissionBeans = roleBean.getPermissions().get(currentContext).get(currentGroup);
        List<String> perms = selectedValues != null ? Arrays.asList(selectedValues) : new ArrayList<String>();
        for (PermissionBean permissionBean : permissionBeans.values()) {
            if (permissionBean.isSet() != perms.contains(permissionBean.getPath())) {
                roleBean.setDirty(true);
                permissionBean.setSet(perms.contains(permissionBean.getPath()));
                if (!permissionBean.isMappedPermissionsExpanded() && permissionBean.getMappedPermissions() != null) {
                    for (PermissionBean mapped : permissionBean.getMappedPermissions().values()) {
                        mapped.setSet(perms.contains(permissionBean.getPath()));
                    }
                }
            }

        }

        perms = partialSelectedValues != null ? Arrays.asList(partialSelectedValues) : new ArrayList<String>();
        for (PermissionBean permissionBean : permissionBeans.values()) {
            if (permissionBean.isPartialSet() != perms.contains(permissionBean.getPath())) {
                roleBean.setDirty(true);
                permissionBean.setPartialSet(perms.contains(permissionBean.getPath()));
                if (!permissionBean.isMappedPermissionsExpanded() && permissionBean.getMappedPermissions() != null) {
                    for (PermissionBean mapped : permissionBean.getMappedPermissions().values()) {
                        mapped.setPartialSet(perms.contains(permissionBean.getPath()));
                    }
                }
            }
        }
    }

    public void addContext(String newContext) throws RepositoryException {
        if (!newContext.startsWith("/")) {
            return;
        }

        if (!roleBean.getPermissions().containsKey(newContext)) {
            addPermissionsForScope(roleBean, newContext, new HashMap<String, List<String>>(), new HashMap<String, List<String>>());
        }
        setCurrentContext(newContext);
    }

    public void removeContext(String scope) throws RepositoryException {

        if (roleBean.getPermissions().containsKey(scope)) {
            roleBean.getPermissions().remove(scope);
        }
        if (currentContext.equals(scope)) {
            setCurrentContext(roleBean.getPermissions().keySet().iterator().next());
        }
    }

    public void save() throws RepositoryException {
        JCRSessionWrapper currentUserSession = getSession();

        Map<String, List<Value>> permissions = new HashMap<String, List<Value>>();

        for (Map.Entry<String, Map<String, Map<String, PermissionBean>>> entry : roleBean.getPermissions().entrySet()) {
            ArrayList<Value> permissionValues = new ArrayList<Value>();
            permissions.put(entry.getKey(), permissionValues);
            for (Map<String, PermissionBean> map : entry.getValue().values()) {
                for (PermissionBean bean : map.values()) {
                    PermissionBean parentBean = map.get(bean.getParentPath());
                    if (bean.isSet() && (parentBean == null || !parentBean.isSet())) {
                        if (bean.getMappedPermissions() != null) {
                            for (PermissionBean mapped : bean.getMappedPermissions().values()) {
                                if (mapped.isSet()) {
                                    permissionValues.add(currentUserSession.getValueFactory().createValue(mapped.getName(), PropertyType.STRING));
                                }
                            }
                        } else {
                            permissionValues.add(currentUserSession.getValueFactory().createValue(bean.getName(), PropertyType.STRING));
                        }
                    }
                }
            }
        }

        JCRNodeWrapper role = currentUserSession.getNodeByIdentifier(roleBean.getUuid());
        Set<String> externalPermissionNodes = new HashSet<String>();
        for (Map.Entry<String, List<Value>> s : permissions.entrySet()) {
            String key = s.getKey();
            if (key.equals("current")) {
                role.setProperty("j:permissionNames", permissions.get("current").toArray(new Value[permissions.get("current").size()]));
            } else {
                if (key.equals("/")) {
                    key = "root-access";
                } else {
                    key = ISO9075.encode((key.startsWith("/") ? key.substring(1) : key).replace("/", "-")) + "-access";
                }
                if (!s.getValue().isEmpty()) {
                    if (!role.hasNode(key)) {
                        JCRNodeWrapper extPermissions = role.addNode(key, "jnt:externalPermissions");
                        extPermissions.setProperty("j:path", s.getKey());
                        extPermissions.setProperty("j:permissionNames", s.getValue().toArray(new Value[s.getValue().size()]));
                    } else {
                        role.getNode(key).setProperty("j:permissionNames", s.getValue().toArray(new Value[s.getValue().size()]));
                    }
                    externalPermissionNodes.add(key);
                }
            }
        }
        NodeIterator ni = role.getNodes();
        while (ni.hasNext()) {
            JCRNodeWrapper next = (JCRNodeWrapper) ni.next();
            if (next.getPrimaryNodeTypeName().equals("jnt:externalPermissions") && !externalPermissionNodes.contains(next.getName())) {
                next.remove();
            }
        }

        JCRNodeWrapper n;
        Map<String, I18nRoleProperties> i18nRoleProperties = roleBean.getI18nProperties();
        for (String l : i18nRoleProperties.keySet()) {
            n = getSession(new Locale(l)).getNodeByIdentifier(roleBean.getUuid());
            I18nRoleProperties properties = i18nRoleProperties.get(l);
            if (properties == null) {
                if (n.hasProperty(Constants.JCR_TITLE)) {
                    n.getProperty(Constants.JCR_TITLE).remove();
                }
                if (n.hasProperty(Constants.JCR_DESCRIPTION)) {
                    n.getProperty(Constants.JCR_DESCRIPTION).remove();
                }
            } else {
                String title = properties.getTitle();
                if (StringUtils.isNotBlank(title)) {
                    n.setProperty(Constants.JCR_TITLE, title);
                } else if (n.hasProperty(Constants.JCR_TITLE)) {
                    n.getProperty(Constants.JCR_TITLE).remove();
                }
                String description = properties.getDescription();
                if (StringUtils.isNotBlank(description)) {
                    n.setProperty(Constants.JCR_DESCRIPTION, description);
                } else if (n.hasProperty(Constants.JCR_DESCRIPTION)) {
                    n.getProperty(Constants.JCR_DESCRIPTION).remove();
                }
            }
        }

        role.setProperty("j:hidden", roleBean.isHidden());
        if (roleBean.getNodeTypes() != null) {
            List<Value> values = new ArrayList<Value>();
            for (NodeType nodeType : roleBean.getNodeTypes()) {
                if (nodeType.isSet()) {
                    values.add(currentUserSession.getValueFactory().createValue(nodeType.getName()));
                }
            }
            role.setProperty("j:nodeTypes", values.toArray(new Value[values.size()]));
        }
        roleBean.setDirty(false);
        currentUserSession.save();
    }

    public List<JCRNodeWrapper> getAllPermissions() throws RepositoryException {
        if (allPermissions != null) {
            return allPermissions;
        }

        allPermissions = new ArrayList<JCRNodeWrapper>();
        JCRSessionWrapper currentUserSession = getSession();

        QueryManager qm = currentUserSession.getWorkspace().getQueryManager();
        String statement = "select * from [jnt:permission]";

        Query q = qm.createQuery(statement, Query.JCR_SQL2);
        NodeIterator ni = q.execute().getNodes();
        while (ni.hasNext()) {
            JCRNodeWrapper next = (JCRNodeWrapper) ni.next();
            int depth = 2;
            if (((JCRNodeWrapper) next.getAncestor(1)).isNodeType("jnt:modules")) {
                depth = 5;
                JahiaTemplatesPackage pack = templateManagerService.getTemplatePackageById(next.getAncestor(2).getName());
                if (pack == null || !pack.getVersion().toString().equals(next.getAncestor(3).getName())) {
                    continue;
                }
            }
            if (next.getDepth() >= depth) {
                allPermissions.add(next);
            }
        }
        Collections.sort(allPermissions, new Comparator<JCRNodeWrapper>() {
            @Override
            public int compare(JCRNodeWrapper o1, JCRNodeWrapper o2) {
                if (getPermissionPath(o1).equals(getPermissionPath(o2))) {
                    return -o1.getPath().compareTo(o2.getPath());
                }
                return getPermissionPath(o1).compareTo(getPermissionPath(o2));
            }
        });

        return allPermissions;
    }

    public void storeDetails(String[] languageCodes, String[] languageNames, String[] titles, String[] descriptions, Boolean hidden, String[] nodeTypes) {
        if (languageCodes != null) {
            Map<String, I18nRoleProperties> i18nProperties = roleBean.getI18nProperties();
            for (int i = 0; i < languageCodes.length; i++) {
                String l = languageCodes[i];
                String title;
                if (titles != null && languageCodes.length == 1) {
                    title = StringUtils.join(titles, ", ");
                } else if (titles != null && i < titles.length) {
                    title = titles[i];
                } else {
                    title = "";
                }
                String description;
                if (descriptions != null && languageCodes.length == 1) {
                    description = StringUtils.join(descriptions, ", ");
                } else if (descriptions != null && i < descriptions.length) {
                    description = descriptions[i];
                } else {
                    description = "";
                }
                if (StringUtils.isBlank(title) && StringUtils.isBlank(description)) {
                    i18nProperties.put(l, null);
                    continue;
                }
                I18nRoleProperties properties;
                if (!i18nProperties.containsKey(l) || i18nProperties.get(l) == null) {
                    properties = new I18nRoleProperties();
                    properties.setLanguage(languageNames[i]);
                    i18nProperties.put(l, properties);
                } else {
                    properties = i18nProperties.get(l);
                }
                if (!title.equals(properties.getTitle())) {
                    roleBean.setDirty(true);
                    properties.setTitle(title);
                }
                if (!description.equals(properties.getDescription())) {
                    roleBean.setDirty(true);
                    properties.setDescription(description);
                }
            }
        }

        roleBean.setHidden(hidden != null && hidden);

        if (roleBean.getNodeTypes() != null) {
            if (nodeTypes != null) {
                List<String> values = Arrays.asList(nodeTypes);
                for (NodeType nodeType : roleBean.getNodeTypes()) {
                    nodeType.setSet(values.contains(nodeType.getName()));
                }
            } else {
                for (NodeType nodeType : roleBean.getNodeTypes()) {
                    nodeType.setSet(false);
                }
            }
        }
    }

    public void expandMapped(String path) {
        Map<String, PermissionBean> permissionBeans = roleBean.getPermissions().get(currentContext).get(currentGroup);
        final PermissionBean permissionBean = permissionBeans.get(path);
        if (permissionBean != null && !permissionBean.isPartialSet()) {
            if (!permissionBean.isMappedPermissionsExpanded()) {
                permissionBean.setMappedPermissionsExpanded(true);
                for (PermissionBean mapped : permissionBean.getMappedPermissions().values()) {
                    permissionBeans.put(mapped.getPath(), mapped);
                }
            } else {
                permissionBean.setMappedPermissionsExpanded(false);
                for (PermissionBean mapped : permissionBean.getMappedPermissions().values()) {
                    permissionBeans.remove(mapped.getPath());
                }
            }

        }
    }

    private String getMessage(String key) {
        return Messages.get("resources.JahiaRolesManager", key, LocaleContextHolder.getLocale());
    }

    public Map<String, String> getAvailableLanguages() throws RepositoryException {
        Set<String> languages = new TreeSet<String>(getSession().getNodeByIdentifier(roleBean.getUuid()).getResolveSite().getLanguages());
        Map<String, I18nRoleProperties> i18nProperties = roleBean.getI18nProperties();
        for (String l : i18nProperties.keySet()) {
            if (i18nProperties.get(l) != null) {
                languages.remove(l);
            }
        }

        TreeMap<String, String> availableLanguages = new TreeMap<String, String>();
        for (String l : languages) {
            Locale locale = new Locale(l);
            availableLanguages.put(l, locale.getDisplayName(LocaleContextHolder.getLocale()));
        }
        return availableLanguages;
    }
}