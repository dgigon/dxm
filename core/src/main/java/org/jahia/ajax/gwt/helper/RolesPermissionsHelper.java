/**
 * This file is part of Jahia: An integrated WCM, DMS and Portal Solution
 * Copyright (C) 2002-2010 Jahia Solutions Group SA. All rights reserved.
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

package org.jahia.ajax.gwt.helper;

import org.jahia.ajax.gwt.client.data.GWTJahiaPermission;
import org.jahia.ajax.gwt.client.data.GWTJahiaRole;
import org.jahia.ajax.gwt.client.data.GWTRolesPermissions;
import org.jahia.ajax.gwt.client.service.GWTJahiaServiceException;
import org.jahia.services.rbac.Permission;
import org.jahia.services.rbac.PermissionIdentity;
import org.jahia.services.rbac.Role;
import org.jahia.services.rbac.RoleIdentity;
import org.jahia.services.rbac.jcr.RoleService;
import org.slf4j.Logger;

import javax.jcr.RepositoryException;
import java.util.LinkedList;
import java.util.List;

/**
 * Roles and permission GWT helper class.
 *
 * @author ktlili
 * Date: Feb 3, 2010
 * Time: 4:15:49 PM
 */
public class RolesPermissionsHelper {
    private static Logger logger = org.slf4j.LoggerFactory.getLogger(RolesPermissionsHelper.class);

    private RoleService roleService;

    /**
     * Grants the specified permissions to a role.
     *
     * @param role the role to grant permissions
     * @param gwtPermissions permissions to be granted
     * @throws GWTJahiaServiceException in case of an error
     */
    public void addRolePermissions(GWTJahiaRole role, List<GWTJahiaPermission> gwtPermissions)
            throws GWTJahiaServiceException {
        List<Permission> permissions = new LinkedList<Permission>();
        for (GWTJahiaPermission perm : gwtPermissions) {
            PermissionIdentity identity = new PermissionIdentity(perm.getName());
            identity.setPath(perm.getPath());
            permissions.add(identity);
        }
        if (logger.isDebugEnabled()) {
            logger.debug("addRolePermissions() ," + role.getName() + "," + permissions);
        }
        try {
            RoleIdentity identity = new RoleIdentity(role.getName());
            identity.setPath(role.getPath());
            roleService.grantPermissions(identity,permissions);
        } catch (RepositoryException e) {
            logger.error(e.getMessage(), e);
            throw new GWTJahiaServiceException(e.getMessage());
        }
    }

    /**
     * Get all permissions for the specified site or for the server if the site
     * is not specified.
     *
     * @return all permissions for the specified site or for the server if the
     *         site is not specified
     * @throws GWTJahiaServiceException in case of an error
     */
    private List<GWTJahiaPermission> getPermissions()
            throws GWTJahiaServiceException {
        if (logger.isDebugEnabled()) {
            logger.debug("Retrieving permissions");
        }

        List<GWTJahiaPermission> permissions = new LinkedList<GWTJahiaPermission>();
        try {
            for (Permission permission : roleService.getPermissions()) {
                permissions.add(toPermission(permission));
            }
        } catch (RepositoryException e) {
            logger.error(e.getMessage(), e);
            throw new GWTJahiaServiceException(e.getMessage());
        }

        return permissions;
    }

    /**
       * Get all roles for the specified site or for the server if the site is not
       * specified.
       *
       * @return all roles for the specified site or for the server if the site is
       *         not specified
       * @throws GWTJahiaServiceException in case of an error
       */
      private List<GWTJahiaRole> getRoles(String search) throws GWTJahiaServiceException {
          if (logger.isDebugEnabled()) {
              logger.debug("Retrieving roles server");
          }

          List<GWTJahiaRole> roles = new LinkedList<GWTJahiaRole>();
          try {
              for (Role role : roleService.getRoles()) {
                  if("*".equals(search))
                      roles.add(toRole(role));
                  else if(role.getName().matches(search)){
                      roles.add(toRole(role));
                  }
              }
          } catch (RepositoryException e) {
              logger.error(e.getMessage(), e);
              throw new GWTJahiaServiceException(e.getMessage());
          }

          return roles;
      }

      /**
       * Get all roles and all permissions
       *
       * @return all roles and all permissions
       * @throws GWTJahiaServiceException in case of an error
       */
      public GWTRolesPermissions getRolesAndPermissions()
              throws GWTJahiaServiceException {
          if (logger.isDebugEnabled()) {
              logger.debug("getting roles and permission");
          }

          GWTRolesPermissions rp = new GWTRolesPermissions();
          rp.setRoles(getRoles("*"));
          rp.setPermissions(getPermissions());
          return rp;
      }

    /**
     * Revokes specified permissions from a role.
     *
     * @param role the role to revoke permissions from
     * @param gwtPermissions permissions to be revoked
     * @throws GWTJahiaServiceException in case of an error
     */
    public void removeRolePermissions(GWTJahiaRole role, List<GWTJahiaPermission> gwtPermissions) throws GWTJahiaServiceException {
        List<Permission> permissions = new LinkedList<Permission>();
        for (GWTJahiaPermission perm : gwtPermissions) {
            PermissionIdentity identity = new PermissionIdentity(perm.getName());
            identity.setPath(perm.getPath());
            permissions.add(identity);
        }
        if (logger.isDebugEnabled()) {
            logger.debug("removeRolePermissions() ," + role.getName() + "," + permissions);
        }
        try {
            RoleIdentity identity = new RoleIdentity(role.getName());
            identity.setPath(role.getPath());
            roleService.revokePermissions(identity, permissions);
        } catch (RepositoryException e) {
            logger.error(e.getMessage(), e);
            throw new GWTJahiaServiceException(e.getMessage());
        }
    }

    /**
     * Injects the role management service instance.
     * 
     * @param roleService the role management service instance
     */
    public void setRoleService(RoleService roleService) {
        this.roleService = roleService;
    }

    private GWTJahiaPermission toPermission(Permission permission) {
        GWTJahiaPermission gwtJahiaPermission = new GWTJahiaPermission(permission.getName());
        gwtJahiaPermission.setTitle(permission.getTitle());
        gwtJahiaPermission.setDescription(permission.getDescription());
        List<Permission> childs = permission.getChilds();
        for (Permission child : childs) {
            GWTJahiaPermission permission1 = toPermission(child);
            permission1.setParent(gwtJahiaPermission);
            gwtJahiaPermission.addChild(permission1);
        }
        gwtJahiaPermission.setPath(permission.getPath());
        for (Permission permission1 : permission.getDependencies()) {
            GWTJahiaPermission jahiaPermission = toPermission(permission1);
            jahiaPermission.setParent(gwtJahiaPermission);
            gwtJahiaPermission.addDependency(jahiaPermission);
        }
        return gwtJahiaPermission;
    }

    private GWTJahiaRole toRole(Role role) {
        GWTJahiaRole gwtRole = new GWTJahiaRole(role.getName());
        List<GWTJahiaPermission> gwtPermissions = new LinkedList<GWTJahiaPermission>();
        for (Permission perm : role.getPermissions()) {
            gwtPermissions.add(toPermission(perm));
        }
        gwtRole.setPermissions(gwtPermissions);
        gwtRole.setPath(role.getPath());
        return gwtRole;
    }

}