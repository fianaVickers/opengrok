/*
 * CDDL HEADER START
 *
 * The contents of this file are subject to the terms of the
 * Common Development and Distribution License (the "License").
 * You may not use this file except in compliance with the License.
 *
 * See LICENSE.txt included in this distribution for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL HEADER in each
 * file and include the License file at LICENSE.txt.
 * If applicable, add the following below this CDDL HEADER, with the
 * fields enclosed by brackets "[]" replaced with your own identifying
 * information: Portions Copyright [yyyy] [name of copyright owner]
 *
 * CDDL HEADER END
 */

/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
 * Portions Copyright (c) 2018, Chris Fraire <cfraire@me.com>.
 */
package org.opengrok.indexer.configuration;

import java.util.HashMap;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.opengrok.indexer.util.ClassUtil;

/**
 * Placeholder for the information about subgroups of projects and repositories.
 *
 * Supports natural ordering based on case insensitive group names.
 *
 * @author Krystof Tulinger
 * @version $Revision$
 */
public class Group implements Comparable<Group>, Nameable {

    static {
        ClassUtil.remarkTransientFields(Group.class);
    }

    private String name;
    /**
     * Group regexp pattern.
     *
     * No project matches the empty pattern of "" however this group can still
     * be used as a superior group for other groups (without duplicating the
     * projects).
     */
    private String pattern = "";
    /**
     * Compiled group pattern.
     *
     * We set up the empty compiled pattern by default to "()" to reduce code
     * complexity when performing a match for a group without a pattern.
     *
     * This pattern is updated whenever the string pattern {@link #pattern} is
     * updated.
     *
     * @see #setPattern(String)
     */
    private Pattern compiledPattern = Pattern.compile("()");
    private Group parent;
    //added this
    private HashMap<String, Group> subgroups = new TreeSet<>();

    private transient int flag;
    //added this
    private transient HashMap<String, Group> descendants = new TreeSet<>();
    private transient Set<Project> projects = new TreeSet<>();
    private transient Set<Project> repositories = new TreeSet<>();
    private transient HashMap<String, Group> parents;

    /**
     * No-arg constructor is needed for deserialization.
     */
    public Group() {
    }

    public Group(String name) {
        this(name, "");
    }

    public Group(String name, String pattern) {
        this.name = name;
        this.setPattern(pattern);
    }

    public Set<Project> getProjects() {
        return projects;
    }

    public void addProject(Project p) {
        this.projects.add(p);
    }

    public void addRepository(Project p) {
        this.repositories.add(p);
    }

    public HashMap<Group> getDescendants() {
        return descendants;
    }

    public void setDescendants(HashMap<Group> descendants) {
        this.descendants = descendants;
    }

    public void addDescendant(Group g) {
        this.descendants.add(g.name, g);
    }

    public void removeDescendant(Group g) {
        this.descendants.remove(g.name);
    }

    public Set<Project> getRepositories() {
        return repositories;
    }

    public void setSubgroups(HashMap<String, Group> subgroups) {
        this.subgroups = subgroups;
    }

    public void setProjects(Set<Project> projects) {
        this.projects = projects;
    }

    public void setRepositories(Set<Project> repositories) {
        this.repositories = repositories;
    }

    public HashMap<String, Group> getSubgroups() {
        return subgroups;
    }

    //added this
    public void addGroup(Group g) {
        g.setParent(this);
        subgroups.add(g.name, g);
        descendants.add(g.name, g);
    }

    //added this
    public HashMap<Group> getParents() {
        if (parents == null) {
            parents = new TreeSet<>();
            Group tmp = parent;
            while (tmp != null) {
                parents.add(tmp.name, tmp);
                tmp = tmp.getParent();
            }
        }
        return parents;
    }

    /**
     * Collect all related groups to this group. A related group is
     * <ul>
     * <li>any ancestor</li>
     * <li>any subgroup</li>
     * </ul>
     *
     * @return all collected related groups to this group
     */
    //added this
    public HashMap<Group> getRelatedGroups() {
        HashMap <Group> groupsTmp = new TreeSet<>();
        groupsTmp.addAll(getDescendants());
        groupsTmp.addAll(getParents());
        return groupsTmp;
    }

    /**
     * Collect all group's projects and repositories included in this group and
     * in any subgroup.
     *
     * @return all collected projects and repositories
     */
    public Set<Project> getAllProjects() {
        Set<Project> projectsTmp = new TreeSet<>();
        projectsTmp.addAll(getRepositories());
        projectsTmp.addAll(getProjects());
        for (Group grp : getDescendants()) {
            projectsTmp.addAll(grp.getAllProjects());
        }
        return projectsTmp;
    }

    public Group getParent() {
        return parent;
    }

    public void setParent(Group parent) {
        this.parent = parent;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    public String getPattern() {
        return pattern;
    }

    /**
     * Set the group pattern.
     *
     * @param pattern the regexp pattern for this group
     * @throws PatternSyntaxException when the pattern is invalid
     */
    public final void setPattern(String pattern) throws PatternSyntaxException {
        this.compiledPattern = Pattern.compile("(" + pattern + ")");
        this.pattern = pattern;
    }

    public int getFlag() {
        return flag;
    }

    public void setFlag(int flag) {
        this.flag = flag;
    }

    /**
     * Test group for a match.
     *
     * @param p project
     * @return true if project's description matches the group pattern
     */
    public boolean match(Project p) {
        return compiledPattern.matcher(p.getName()).matches();
    }

    @Override
    public int compareTo(Group o) {
        return getName().toUpperCase(Locale.ROOT).compareTo(
                o.getName().toUpperCase(Locale.ROOT));
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 41 * hash + (this.name == null ? 0 :
                this.name.toUpperCase(Locale.ROOT).hashCode());
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Group other = (Group) obj;

        int numNull = (name == null ? 1 : 0) + (other.name == null ? 1 : 0);
        switch (numNull) {
            case 0:
                return name.toUpperCase(Locale.ROOT).equals(
                        other.name.toUpperCase(Locale.ROOT));
            case 1:
                return false;
            default:
                return true;
        }
    }

    /**
     * Returns group object by its name.
     *
     * @param name name of a group
     * @return group that fits the name
     */
    public static Group getByName(String name) {
        Group ret = null;
        RuntimeEnvironment env = RuntimeEnvironment.getInstance();
        if (env.hasGroups()) {
            //added this
            Group group;
            if(group = env.getGroups().get(name) != null){
                return group;
            }
        }
        return null;
    }

    /**
     * Reduce the group hashmap to only those which match the given project based on
     * the project's description.
     *
     * @param project the project
     * @param groups set of groups
     * @return set of groups matching the project
     */
    //added this
    public static HashMap<String, Group> matching(Project project, HashMap<String, Group> groups) {
        HashMap<String, Group> copy = groups;
        copy.removeIf(g -> !g.match(project));
        //copy the groupts into a hash map
        //remove groups if group to name matches
        return copy;
    }
}
