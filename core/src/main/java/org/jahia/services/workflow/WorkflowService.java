/**
 * This file is part of Jahia: An integrated WCM, DMS and Portal Solution
 * Copyright (C) 2002-2011 Jahia Solutions Group SA. All rights reserved.
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
 * Commercial and Supported Versions of the program
 * Alternatively, commercial and supported versions of the program may be used
 * in accordance with the terms contained in a separate written agreement
 * between you and Jahia Solutions Group SA. If you are unsure which license is appropriate
 * for your use, please contact the sales department at sales@jahia.com.
 */

package org.jahia.services.workflow;

import org.apache.commons.lang.StringUtils;
import org.jahia.api.Constants;
import org.jahia.exceptions.JahiaRuntimeException;
import org.jahia.registries.ServicesRegistry;
import org.jahia.services.content.JCRCallback;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.JCRTemplate;
import org.jahia.services.content.decorator.JCRSiteNode;
import org.jahia.services.usermanager.*;
import org.jahia.utils.LanguageCodeConverters;
import org.slf4j.Logger;
import org.springframework.beans.BeansException;

import javax.jcr.*;
import java.util.*;

/**
 * Jahia service for managing content workflow.
 *
 * @author : rincevent
 * @since : JAHIA 6.1
 *        Created : 2 févr. 2010
 */
public class WorkflowService {
    private transient static Logger logger = org.slf4j.LoggerFactory.getLogger(WorkflowService.class);


    private Map<String, WorkflowProvider> providers = new HashMap<String, WorkflowProvider>();
    private static WorkflowService instance;
    public static final String CANDIDATE = "candidate";
    public static final String START_ROLE = "start";
    private Map<String, List<String>> workflowTypes;
    private Map<String, Map<String,String>> workflowPermissions;
    private Map<String, String> workflowTypeByDefinition;
    public static final String WORKFLOWRULES_NODE_NAME = "j:workflowRules";

    public static WorkflowService getInstance() {
        if (instance == null) {
            instance = new WorkflowService();
        }
        return instance;
    }

    public void setWorkflowTypes(Map<String, List<String>> workflowTypes) {
        this.workflowTypes = workflowTypes;
        this.workflowTypeByDefinition = new HashMap<String, String>();
        for (Map.Entry<String, List<String>> entry : workflowTypes.entrySet()) {
            for (String s : entry.getValue()) {
                workflowTypeByDefinition.put(s, entry.getKey());
            }
        }
    }

    public void setWorkflowPermissions(Map<String, Map<String, String>> workflowPermissions) {
        this.workflowPermissions = workflowPermissions;
    }

    public void registerWorkflowType(String type, String definition, Map<String,String> perms) {
        List<String> list = workflowTypes.get(type);
        if(list==null){
            list = new ArrayList<String>();
            workflowTypes.put(type,list);
        }
        if(!list.contains(definition)) {
            list.add(definition);
            workflowTypeByDefinition.put(definition,type);
        }
        this.workflowPermissions.put(definition, perms);
    }

    public Map<String, WorkflowProvider> getProviders() {
        return providers;
    }

    public void start() {
    }

    public void addProvider(WorkflowProvider provider) {
        providers.put(provider.getKey(), provider);
        /*try {
            List<WorkflowDefinition> list = getWorkflows();
            for (WorkflowDefinition definition : list) {
                addWorkflowRule("/","nt:base",definition,START_ROLE,"webmaster");
                iter()
            }
        } catch (RepositoryException e) {
            logger.error("Cannot register default workflow rule",e);
        }*/
    }

    /**
     * This method list all workflows deployed in the system
     *
     * @param locale
     * @return A list of available workflows per provider.
     */
    public List<WorkflowDefinition> getWorkflows(Locale locale) throws RepositoryException {
        List<WorkflowDefinition> workflowsByProvider = new ArrayList<WorkflowDefinition>();
        for (Map.Entry<String, WorkflowProvider> providerEntry : providers.entrySet()) {
            workflowsByProvider.addAll(providerEntry.getValue().getAvailableWorkflows(locale));
        }
        return workflowsByProvider;
    }

    public List<WorkflowDefinition> getWorkflowsForAction(String actionName, Locale locale) throws RepositoryException {
        List<String> l = workflowTypes.get(actionName);
        List<WorkflowDefinition> workflowsByProvider = new ArrayList<WorkflowDefinition>();
        for (Map.Entry<String, WorkflowProvider> providerEntry : providers.entrySet()) {
            List<WorkflowDefinition> defs = providerEntry.getValue().getAvailableWorkflows(locale);
            for (WorkflowDefinition def : defs) {
                if (l.contains(def.getKey())) {
                    workflowsByProvider.add(def);
                }
            }
        }
        return workflowsByProvider;
    }

    /**
     * This method list all possible workflows for the specified node.
     *
     * @param node
     * @param user
     * @return A list of available workflows per provider.
     */
    public Map<String, WorkflowDefinition> getPossibleWorkflows(final JCRNodeWrapper node, final JahiaUser user, Locale locale)
            throws RepositoryException {
        List<WorkflowDefinition> l = getPossibleWorkflows(node, user, null, locale);
        Map<String, WorkflowDefinition> res = new HashMap<String, WorkflowDefinition>();
        for (WorkflowDefinition workflowDefinition : l) {
            res.put(workflowTypeByDefinition.get(workflowDefinition.getKey()), workflowDefinition);
        }
        return res;
    }

    /**
     * This method return the workflow associated to an action, for the specified node.
     *
     * @param node
     * @param user
     * @return A list of available workflows per provider.
     */
    public WorkflowDefinition getPossibleWorkflowForAction(final JCRNodeWrapper node, final JahiaUser user,
                                                           final String action, final Locale locale)
            throws RepositoryException {
        final List<WorkflowDefinition> workflowDefinitionList = getPossibleWorkflows(node, user, action, locale);
        if (workflowDefinitionList.isEmpty()) {
            return null;
        }
        return workflowDefinitionList.get(0);
    }

    /**
     * This method list all possible workflows for the specified node.
     *
     * @param node
     * @param user
     * @return A list of available workflows per provider.
     */
    private List<WorkflowDefinition> getPossibleWorkflows(final JCRNodeWrapper node, final JahiaUser user,
                                                          final String action, final Locale locale)
            throws RepositoryException {
        final Set<WorkflowDefinition> workflows = new LinkedHashSet<WorkflowDefinition>();

        Collection<WorkflowRule> rules = getWorkflowRulesForAction(node, user, action, null);
        for (WorkflowRule ruledef : rules) {
            WorkflowDefinition definition =
                    lookupProvider(ruledef.getProviderKey()).getWorkflowDefinitionByKey(ruledef.getWorkflowDefinitionKey(), locale);
            workflows.add(definition);
        }
        return new LinkedList<WorkflowDefinition>(workflows);
    }

    public List<JahiaPrincipal> getAssignedRole(final JCRNodeWrapper node, final WorkflowDefinition definition,
                                                final String activityName, final String processId) throws RepositoryException {
        return JCRTemplate.getInstance().doExecuteWithSystemSession(new JCRCallback<List<JahiaPrincipal>>() {
            public List<JahiaPrincipal> doInJCR(JCRSessionWrapper session) throws RepositoryException {

                List<JahiaPrincipal> principals = new ArrayList<JahiaPrincipal>();
                JahiaUserManagerService userService = ServicesRegistry.getInstance().getJahiaUserManagerService();
                JahiaGroupManagerService groupService = ServicesRegistry.getInstance().getJahiaGroupManagerService();

                JCRSiteNode site = node.getResolveSite();

                Map<String, List<String[]>> m = node.getAclEntries();

                Collection<WorkflowRule> rules = getWorkflowRules(node, null);
                for (WorkflowRule rule : rules) {
                    if (rule.getProviderKey().equals(definition.getProvider()) && rule.getWorkflowDefinitionKey().equals(definition.getKey())) {
                        Map<String,String> perms = workflowPermissions.get(rule.getWorkflowDefinitionKey());
                        if (perms != null) {
                            String permName = perms.get(activityName);
                            if (permName != null) {
                                if (permName.indexOf("$") > -1) {
                                    Workflow w = getWorkflow(definition.getProvider(), processId, null);
                                    for (Map.Entry<String, Object> entry : w.getVariables().entrySet()) {
                                        if (entry.getValue() instanceof List) {
                                            List variable = (List) entry.getValue();
                                            for (Object workflowVariable : variable) {
                                                if (workflowVariable instanceof WorkflowVariable) {
                                                    String v = ((WorkflowVariable)workflowVariable).getValue();
                                                    permName = permName.replace("$"+entry.getKey(), v);
                                                }
                                            }
                                        }
                                    }
                                }
                                try {
                                    Set<String> s = new HashSet<String>();

                                    try {
                                        String path = "/permissions" + permName;
                                        while (path.length() >= "/permissions".length()) {
                                            JCRNodeWrapper permissionNode = session.getNode(path);
                                            for (PropertyIterator iterator = permissionNode.getWeakReferences(
                                                    "j:permissions"); iterator.hasNext();) {
                                                Property prop = iterator.nextProperty();
                                                Node roleNode = prop.getParent();
                                                s.add(roleNode.getName());
                                            }
                                            path = StringUtils.substringBeforeLast(path,"/");
                                        }
                                    } catch (PathNotFoundException e) {
                                        logger.warn("Unable to find the node for the permission " + permName);
                                    }

                                    for (Map.Entry<String, List<String[]>> entry : m.entrySet()) {
                                        for (String[] strings : entry.getValue()) {
                                            if (strings[1].equals("GRANT")) {
                                                if (s.contains(strings[2])) {
                                                    String principal = entry.getKey();
                                                    final String principalName = principal.substring(2);
                                                    if (principal.charAt(0) == 'u') {
                                                        JahiaUser jahiaUser = userService.lookupUser(principalName);
                                                        principals.add(jahiaUser);
                                                    } else if (principal.charAt(0) == 'g') {
                                                        JahiaGroup group = groupService.lookupGroup(site.getID(), principalName);
                                                        principals.add(group);
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } catch (RepositoryException e) {
                                    logger.error(e.getMessage(),e);
                                } catch (BeansException e) {
                                    logger.error(e.getMessage(),e);
                                }
                            }
                        }
                    }
                }
                return principals;
            }
        });
    }

    /**
     * This method list all currently active workflow for the specified node.
     *
     * @param node
     * @param locale
     * @return A list of active workflows per provider
     */
    public List<Workflow> getActiveWorkflows(JCRNodeWrapper node, Locale locale) {
        List<Workflow> workflows = new ArrayList<Workflow>();
        try {
            Node n = node;
            try {
                if (locale != null && node.hasTranslations()) {
                    n = node.getI18N(locale);
                }
            } catch (ItemNotFoundException e) {
                return workflows;
            }
            if (n.isNodeType(Constants.JAHIAMIX_WORKFLOW) && n.hasProperty(Constants.PROCESSID)) {
                addActiveWorkflows(workflows, n.getProperty(Constants.PROCESSID), locale);
            }
        } catch (RepositoryException e) {
            logger.error(e.getMessage(), e);
        }
        return workflows;
    }

    /**
     * This method list all currently active workflow for the specified node.
     *
     * @param node
     * @return A list of active workflows per provider
     */
    public Map<Locale, List<Workflow>> getActiveWorkflowsForAllLocales(JCRNodeWrapper node) {
        Map<Locale, List<Workflow>> workflowsByLocale = new HashMap<Locale, List<Workflow>>();
        try {
            if (node.isNodeType(Constants.JAHIAMIX_WORKFLOW)) {
                NodeIterator ni = node.getNodes("j:translation*");
                while (ni.hasNext()) {
                    Node n = ((JCRNodeWrapper) ni.next()).getRealNode();
                    final String lang = n.getProperty("jcr:language").getString();
                    if (n.hasProperty(Constants.PROCESSID)) {
                        List<Workflow> l = new ArrayList<Workflow>();
                        workflowsByLocale.put(LanguageCodeConverters.getLocaleFromCode(lang), l);
                        addActiveWorkflows(l, n.getProperty(Constants.PROCESSID), null);
                    }
                }
            }
        } catch (RepositoryException e) {
            logger.error(e.getMessage(), e);
        }
        return workflowsByLocale;
    }

    private void addActiveWorkflows(List<Workflow> workflows, Property p, Locale locale) throws RepositoryException {
        Value[] values = p.getValues();
        for (Map.Entry<String, WorkflowProvider> entry : providers.entrySet()) {
            final List<String> processIds = new ArrayList<String>(values.length);
            for (Value value : values) {
                String key = value.getString();
                String processId = StringUtils.substringAfter(key, ":");
                String providerKey = StringUtils.substringBefore(key, ":");
                if (providerKey.equals(entry.getKey())) {
                    processIds.add(processId);
                }
            }
            if (!processIds.isEmpty()) {
                List<Workflow> workflowsInformations = entry.getValue().getActiveWorkflowsInformations(processIds,
                        locale);
                workflows.addAll(workflowsInformations);
            }
        }
    }

    /**
     * This method list all actions available at execution time for a node.
     *
     * @param processId the process we want to advance
     * @param provider  The provider executing the process
     * @param locale
     * @return a set of actions per workflows per provider.
     */
    public Set<WorkflowAction> getAvailableActions(String processId, String provider, Locale locale) {
        return lookupProvider(provider).getAvailableActions(processId, locale);
    }

    /**
     * This method will call the underlying provider to signal the identified process.
     *
     * @param processId      the process we want to advance
     * @param provider       The provider executing the process
     * @param transitionName
     * @param args           List of args for the process
     */
    public void signalProcess(String processId, String transitionName, String provider, Map<String, Object> args) {
        lookupProvider(provider).signalProcess(processId, transitionName, args);
    }

    /**
     * This method will call the underlying provider to signal the identified process.
     *
     * @param processId      the process we want to advance
     * @param provider       The provider executing the process
     * @param transitionName
     * @param args           List of args for the process
     */
    public void signalProcess(String processId, String transitionName, String signalName, String provider,
                              Map<String, Object> args) {
        lookupProvider(provider).signalProcess(processId, transitionName, signalName, args);
    }

    /**
     * This method will call the underlying provider to signal the identified process.
     *
     * @param stageNode
     * @param provider  The provider executing the process
     * @param args      Map of args for the process
     */
    public String startProcess(JCRNodeWrapper stageNode, String processKey, String provider, Map<String, Object> args)
            throws RepositoryException {
        return startProcess(Arrays.asList(stageNode.getIdentifier()), stageNode.getSession(), processKey, provider,
                args);
    }

    public String startProcess(List<String> nodeIds, JCRSessionWrapper session, String processKey, String provider,
                               Map<String, Object> args) throws RepositoryException {
        args.put("nodeId", nodeIds.iterator().next());
        args.put("nodeIds", nodeIds);
        args.put("workspace", session.getWorkspace().getName());
        args.put("locale", session.getLocale());
        args.put("workflow", lookupProvider(provider).getWorkflowDefinitionByKey(processKey, session.getLocale()));
        args.put("user", session.getUser() != null ? session.getUser().getUserKey() : null);
        final String processId = lookupProvider(provider).startProcess(processKey, args);
        if (logger.isDebugEnabled()) {
            logger.debug("A workflow " + processKey + " from " + provider + " has been started on nodes: " + nodeIds +
                    " from workspace " + args.get("workspace") + " in locale " + args.get("locale") + " with id " +
                    processId);
        }
        return processId;
    }

    public synchronized void addProcessId(JCRNodeWrapper stageNode, String provider, String processId)
            throws RepositoryException {
        stageNode.checkout();
        if (!stageNode.isNodeType(Constants.JAHIAMIX_WORKFLOW)) {
            stageNode.addMixin(Constants.JAHIAMIX_WORKFLOW);
        }
        List<Value> values;
        if (stageNode.hasProperty(Constants.PROCESSID)) {
            values = new ArrayList<Value>(Arrays.asList(stageNode.getProperty(Constants.PROCESSID).getValues()));
        } else {
            values = new ArrayList<Value>();
        }
        values.add(stageNode.getSession().getValueFactory().createValue(provider + ":" + processId));
        stageNode.setProperty(Constants.PROCESSID, values.toArray(new Value[values.size()]));
        stageNode.getSession().save();
    }

    public synchronized void removeProcessId(JCRNodeWrapper stageNode, String provider, String processId)
            throws RepositoryException {
        if (!stageNode.hasProperty(Constants.PROCESSID)) {
            return;
        }
        stageNode.checkout();
        List<Value> values =
                new ArrayList<Value>(Arrays.asList(stageNode.getProperty(Constants.PROCESSID).getValues()));
        List<Value> newValues = new ArrayList<Value>();
        for (Value value : values) {
            if (!value.getString().equals(provider + ":" + processId)) {
                newValues.add(value);
            }
        }
        if (newValues.isEmpty()) {
            if (stageNode.hasProperty(Constants.PROCESSID)) {
                stageNode.getProperty(Constants.PROCESSID).remove();
            }
        } else {
            stageNode.setProperty(Constants.PROCESSID, newValues.toArray(new Value[newValues.size()]));
        }
        stageNode.getSession().save();
    }

    public List<WorkflowTask> getTasksForUser(JahiaUser user, Locale locale) {
        final List<WorkflowTask> workflowActions = new LinkedList<WorkflowTask>();
        for (Map.Entry<String, WorkflowProvider> providerEntry : providers.entrySet()) {
            workflowActions.addAll(providerEntry.getValue().getTasksForUser(user, locale));
        }
        return workflowActions;
    }

    public List<Workflow> getWorkflowsForUser(JahiaUser user, Locale locale) {
        final List<Workflow> workflow = new LinkedList<Workflow>();
        for (Map.Entry<String, WorkflowProvider> providerEntry : providers.entrySet()) {
            workflow.addAll(providerEntry.getValue().getWorkflowsForUser(user, locale));
        }
        return workflow;
    }

    public void assignTask(String taskId, String provider, JahiaUser user) {
        if (logger.isDebugEnabled()) {
            logger.debug("Assigning user " + user + " to task " + taskId);
        }
        lookupProvider(provider).assignTask(taskId, user);
    }

    public void completeTask(String taskId, String provider, String outcome, Map<String, Object> args, JahiaUser user) {
        lookupProvider(provider).completeTask(taskId, outcome, args);
    }

    public void addParticipatingGroup(String taskId, String provider, JahiaGroup group, String role) {
        lookupProvider(provider).addParticipatingGroup(taskId, group, role);
    }

    public void deleteTask(String taskId, String provider, String reason) {
        lookupProvider(provider).deleteTask(taskId, reason);
    }

    public void addWorkflowRule(final JCRNodeWrapper node, final WorkflowDefinition workflow) throws RepositoryException {
        // store the rule
        JCRNodeWrapper rules = null;
        try {
            rules = node.getNode(WORKFLOWRULES_NODE_NAME);
        } catch (RepositoryException e) {
            if(!node.isCheckedOut()) {
                node.checkout();
            }
            node.addMixin("jmix:worklfowRulesable");
            rules = node.addNode(WORKFLOWRULES_NODE_NAME, "jnt:workflowRules");
        }
        JCRNodeWrapper n;
        String wfName = workflow.getProvider() + "_" + workflow.getKey();
        if (rules.hasNode(wfName)) {
            n = rules.getNode(wfName);
        } else {
            n = rules.addNode(wfName, "jnt:workflowRule");
        }
        if(!n.isCheckedOut()) {
            n.checkout();
        }
        n.setProperty("j:workflow", workflow.getProvider() + ":" + workflow.getKey());
    }

    public void addComment(String processId, String provider, String comment, String user) {
        lookupProvider(provider).addComment(processId, comment, user);
    }

    public WorkflowTask getWorkflowTask(String taskId, String provider, Locale locale) {
        WorkflowTask workflowTask = lookupProvider(provider).getWorkflowTask(taskId, locale);
        return workflowTask;
    }

    public HistoryWorkflow getHistoryWorkflow(String id, String provider, Locale locale) {
        HistoryWorkflow wf = providers.get(provider).getHistoryWorkflows(Collections.singletonList(id), locale).get(0);
        return wf;
    }

    /**
     * Returns a list of process instance history records for the specified
     * node. This method also returns "active" (i.e. not completed) workflow
     * process instance.
     *
     * @param node   the JCR node to retrieve history records for
     * @param locale
     * @return a list of process instance history records for the specified node
     */
    public List<HistoryWorkflow> getHistoryWorkflows(JCRNodeWrapper node, Locale locale) {
        List<HistoryWorkflow> history = new LinkedList<HistoryWorkflow>();
        try {
            for (WorkflowProvider workflowProvider : providers.values()) {
                history.addAll(workflowProvider.getHistoryWorkflowsForNode(node.getIdentifier(), locale));
            }
        } catch (RepositoryException e) {
            logger.error(e.getMessage(), e);
        }
        return history;
    }

    /**
     * Returns a list of history records for workflow tasks.
     * This method also returns not completed tasks.
     *
     * @param workflowProcessId the process instance ID
     * @param providerKey       the workflow provider key
     * @param locale
     * @return a list of history records for workflow tasks
     */
    public List<HistoryWorkflowTask> getHistoryWorkflowTasks(String workflowProcessId, String providerKey,
                                                             Locale locale) {
        List<HistoryWorkflowTask> list = lookupProvider(providerKey).getHistoryWorkflowTasks(workflowProcessId, locale);
        return list;
    }

    protected WorkflowProvider lookupProvider(String key) {
        WorkflowProvider provider = providers.get(key);
        if (provider == null) {
            throw new JahiaRuntimeException("Unknown workflow provider with the key '" + key + "'");
        }

        return provider;
    }

    /**
     * This method list all currently active workflow for the specified node.
     *
     * @param node
     * @return A list of active workflows per provider
     */
    public boolean hasActivePublishWorkflow(JCRNodeWrapper node) {
        List<Workflow> workflows = new ArrayList<Workflow>();
        try {
            final List<WorkflowDefinition> forAction = getWorkflowsForAction("publish", null);
            if (node.isNodeType(Constants.JAHIAMIX_WORKFLOW) && node.hasProperty(Constants.PROCESSID)) {
                addActiveWorkflows(workflows, node.getProperty(Constants.PROCESSID), node.getSession().getLocale());
            }
            for (Workflow workflow : workflows) {
                if (forAction.contains(workflow.getWorkflowDefinition())) {
                    return true;
                }
            }
        } catch (RepositoryException e) {
            logger.error(e.getMessage(), e);
        }
        return false;
    }

    public void addWorkflowRule(JCRNodeWrapper node, String wfName)
            throws RepositoryException {
        String provider = StringUtils.substringBefore(wfName, ":");
        String wfKey = StringUtils.substringAfter(wfName, ":");
        WorkflowDefinition definition = providers.get(provider).getWorkflowDefinitionByKey(wfKey, node.getSession().getLocale());
        addWorkflowRule(node, definition);
    }

    public WorkflowRule getWorkflowRuleForAction(JCRNodeWrapper objectNode, JahiaUser user, String action, Locale locale) throws RepositoryException {
        Collection<WorkflowRule> rules = getWorkflowRulesForAction(objectNode, user, action, locale);
        if (rules.isEmpty()) {
            return null;
        } else {
            return rules.iterator().next();
        }
    }

    private Collection<WorkflowRule> getWorkflowRulesForAction(JCRNodeWrapper objectNode, JahiaUser user, String action, Locale locale) throws RepositoryException {

        Collection<WorkflowRule> results = new HashSet<WorkflowRule>();
        Collection<WorkflowRule> rules = getWorkflowRules(objectNode, locale);

        JCRSiteNode site = objectNode.getResolveSite();

        for (WorkflowRule rule : rules) {
            if (action == null || workflowTypeByDefinition.get(rule.getWorkflowDefinitionKey()).equals(action)) {
                Map<String,String> perms = workflowPermissions.get(rule.getWorkflowDefinitionKey());
                if (perms != null) {
                    String permName = perms.get("start");
                    if (permName != null) {
                        if (objectNode.hasPermission(permName)) {
                            results.add(rule);
                        }
                    }
                } else {
                    results.add(rule);
                }
            }
        }
        return results;
    }

    public Collection<WorkflowRule> getWorkflowRules(JCRNodeWrapper objectNode, Locale locale) {
        try {
            List<WorkflowRule> rules = new ArrayList<WorkflowRule>();
            recurseonRules(rules, objectNode);
            return rules;
        } catch (RepositoryException e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    private void recurseonRules(List<WorkflowRule> results, Node n)
            throws RepositoryException {
        String defPath = null;
        try {
            Set<String> foundTypes = new HashSet<String>();
            while (true) {
                if (n.hasNode(WORKFLOWRULES_NODE_NAME)) {
                    Node wfRules = n.getNode(WORKFLOWRULES_NODE_NAME);
                    NodeIterator rules = wfRules.getNodes();
                    if (defPath == null) {
                        defPath = n.getPath();
                    }
                    while (rules.hasNext()) {
                        Node rule = rules.nextNode();
                        final String wfName = rule.getProperty("j:workflow").getString();
                        String name = StringUtils.substringAfter(wfName, ":");
                        String prov = StringUtils.substringBefore(wfName, ":");
                        String wftype = workflowTypeByDefinition.get(name);
                        if (foundTypes.contains(wftype)) {
                            continue;
                        }
                        foundTypes.add(wftype);
                        results.add(new WorkflowRule(defPath, prov, name));
                        if (rule.hasProperty("j:inherit") && !rule.getProperty("j:inherit").getBoolean()) {
                            return;
                        }
                    }
                }
                n = n.getParent();
            }
        } catch (ItemNotFoundException e) {
            logger.debug(e.getMessage(), e);
        }
    }

    public Workflow getWorkflow(String provider, String id, Locale locale) {
        return  lookupProvider(provider).getWorkflow(id, locale);
    }

    public WorkflowDefinition getWorkflowDefinition(String provider, String id, Locale locale) {
        final WorkflowDefinition definition = lookupProvider(provider).getWorkflowDefinitionByKey(id, locale);
        return definition;
    }

    public String getWorkflowType(WorkflowDefinition def) {
        return workflowTypeByDefinition.get(def.getKey());
    }

    public Set<String> getTypesOfWorkflow() {
        return workflowTypes.keySet();
    }

    public void deleteProcess(String processId, String provider) {
        providers.get(provider).deleteProcess(processId);
    }

}
