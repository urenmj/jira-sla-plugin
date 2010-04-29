package com.qxlva.jira.customfield.searchers;

import com.atlassian.jira.issue.customfields.searchers.TextSearcher;

/**
 * Created by IntelliJ IDEA.
 * User: adrianp
 * Date: 29-Apr-2010
 * Time: 14:32:56
 * To change this template use File | Settings | File Templates.
 */
public class NewTextSearcher extends com.atlassian.jira.issue.customfields.searchers.TextSearcher
{

    public NewTextSearcher(com.atlassian.jira.web.FieldVisibilityManager fieldVisibilityManager, com.atlassian.jira.jql.operand.JqlOperandResolver jqlOperandResolver, com.atlassian.jira.issue.customfields.searchers.transformer.CustomFieldInputHelper customFieldInputHelper)
    {
        super(fieldVisibilityManager, jqlOperandResolver, customFieldInputHelper);
    }
}
