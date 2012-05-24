/*
 * Copyright (c) 2002-2012, Mairie de Paris
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  1. Redistributions of source code must retain the above copyright notice
 *     and the following disclaimer.
 *
 *  2. Redistributions in binary form must reproduce the above copyright notice
 *     and the following disclaimer in the documentation and/or other materials
 *     provided with the distribution.
 *
 *  3. Neither the name of 'Mairie de Paris' nor 'Lutece' nor the names of its
 *     contributors may be used to endorse or promote products derived from
 *     this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * License 1.0
 */
package fr.paris.lutece.plugins.workflow.modules.assignment.web;

import fr.paris.lutece.plugins.workflow.modules.assignment.business.AssignmentHistory;
import fr.paris.lutece.plugins.workflow.modules.assignment.business.TaskAssignmentConfig;
import fr.paris.lutece.plugins.workflow.modules.assignment.business.WorkgroupConfig;
import fr.paris.lutece.plugins.workflow.modules.assignment.service.IAssignmentHistoryService;
import fr.paris.lutece.plugins.workflow.modules.assignment.service.ITaskAssignmentConfigService;
import fr.paris.lutece.plugins.workflow.utils.WorkflowUtils;
import fr.paris.lutece.plugins.workflow.web.task.TaskComponent;
import fr.paris.lutece.plugins.workflowcore.service.task.ITask;
import fr.paris.lutece.portal.service.admin.AdminUserService;
import fr.paris.lutece.portal.service.i18n.I18nService;
import fr.paris.lutece.portal.service.mailinglist.AdminMailingListService;
import fr.paris.lutece.portal.service.message.AdminMessage;
import fr.paris.lutece.portal.service.message.AdminMessageService;
import fr.paris.lutece.portal.service.template.AppTemplateService;
import fr.paris.lutece.portal.service.workgroup.AdminWorkgroupService;
import fr.paris.lutece.util.ReferenceItem;
import fr.paris.lutece.util.ReferenceList;
import fr.paris.lutece.util.html.HtmlTemplate;
import fr.paris.lutece.util.xml.XmlUtil;

import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.inject.Inject;

import javax.servlet.http.HttpServletRequest;


/**
 *
 * AssignmentTaskComponent
 *
 */
public class AssignmentTaskComponent extends TaskComponent
{
    // TEMPLATES
    private static final String TEMPLATE_TASK_ASSIGNMENT_CONFIG = "admin/plugins/workflow/modules/assignment/task_assignment_config.html";
    private static final String TEMPLATE_TASK_ASSIGNMENT_FORM = "admin/plugins/workflow/modules/assignment/task_assignment_form.html";
    private static final String TEMPLATE_TASK_ASSIGNMENT_INFORMATION = "admin/plugins/workflow/modules/assignment/task_assignment_information.html";

    //	MARKS
    private static final String MARK_CONFIG = "config";
    private static final String MARK_WORKGROUP_LIST = "workgroup_list";
    private static final String MARK_ITEM = "item";
    private static final String MARK_MAILING_LIST = "mailing_list";

    // PARAMETERS
    private static final String PARAMETER_TITLE = "title";
    private static final String PARAMETER_IS_MULTIPLE_OWNER = "is_multiple_owner";
    private static final String PARAMETER_WORKGROUPS = "workgroups";
    private static final String PARAMETER_ID_MAILING_LIST = "id_mailing_list";
    private static final String PARAMETER_MESSAGE = "message";
    private static final String PARAMETER_IS_NOTIFICATION = "is_notify";
    private static final String PARAMETER_SUBJECT = "subject";
    private static final String PARAMETER_IS_USE_USER_NAME = "is_use_user_name";

    // PROPERTIES
    private static final String FIELD_TITLE = "module.workflow.assignment.task_assignment_config.label_title";
    private static final String FIELD_WORKGROUPS = "module.workflow.assignment.task_assignment_config.label_workgroups";
    private static final String FIELD_MAILINGLIST_SUBJECT = "module.workflow.assignment.task_assignment_config.label_mailinglist_subject";
    private static final String FIELD_MAILINGLIST_MESSAGE = "module.workflow.assignment.task_assignment_config.label_mailinglist_message";
    private static final String PROPERTY_SELECT_EMPTY_CHOICE = "module.workflow.assignment.task_assignment_config.label_empty_choice";

    // MESSAGES
    private static final String MESSAGE_MANDATORY_FIELD = "module.workflow.assignment.task_assignment_config.message.mandatory.field";
    private static final String MESSAGE_NO_CONFIGURATION_FOR_TASK_ASSIGNMENT = "module.workflow.assignment.task_assignment_config.message.no_configuration_for_task_comment";
    private static final String MESSAGE_NO_MAILINGLIST_FOR_WORKGROUP = "module.workflow.assignment.task_assignment_config.message.no_mailinglist_for_workgroup";

    // XML
    private static final String TAG_ASSIGNMENT = "assignment";
    private static final String TAG_LIST_WORKGROUP = "list-workgroup";
    private static final String TAG_WORKGROUP = "workgroup";

    // SERVICES
    @Inject
    private ITaskAssignmentConfigService _taskAssignmentConfigService;
    @Inject
    private IAssignmentHistoryService _assignmentHistoryService;

    /**
     * {@inheritDoc}
     */
    @Override
    public String doSaveConfig( HttpServletRequest request, Locale locale, ITask task )
    {
        String strError = WorkflowUtils.EMPTY_STRING;
        String strTitle = request.getParameter( PARAMETER_TITLE );
        String strIsMultipleOwner = request.getParameter( PARAMETER_IS_MULTIPLE_OWNER );
        String strIsNotification = request.getParameter( PARAMETER_IS_NOTIFICATION );
        String strIsUseUserName = request.getParameter( PARAMETER_IS_USE_USER_NAME );
        String strMessage = request.getParameter( PARAMETER_MESSAGE );
        String strSubject = request.getParameter( PARAMETER_SUBJECT );
        String[] tabWorkgroups = request.getParameterValues( PARAMETER_WORKGROUPS );

        if ( ( strTitle == null ) || strTitle.trim(  ).equals( WorkflowUtils.EMPTY_STRING ) )
        {
            strError = FIELD_TITLE;
        }

        if ( ( tabWorkgroups == null ) || ( tabWorkgroups.length == 0 ) )
        {
            strError = FIELD_WORKGROUPS;
        }

        if ( ( strIsNotification != null ) && ( ( strSubject == null ) || strSubject.equals( "" ) ) )
        {
            strError = FIELD_MAILINGLIST_SUBJECT;
        }

        if ( ( strIsNotification != null ) && ( ( strMessage == null ) || strMessage.equals( "" ) ) )
        {
            strError = FIELD_MAILINGLIST_MESSAGE;
        }

        if ( !strError.equals( WorkflowUtils.EMPTY_STRING ) )
        {
            Object[] tabRequiredFields = { I18nService.getLocalizedString( strError, locale ) };

            return AdminMessageService.getMessageUrl( request, MESSAGE_MANDATORY_FIELD, tabRequiredFields,
                AdminMessage.TYPE_STOP );
        }

        TaskAssignmentConfig config = _taskAssignmentConfigService.findByPrimaryKey( task.getId(  ),
                WorkflowUtils.getPlugin(  ) );
        Boolean bCreate = false;

        if ( config == null )
        {
            config = new TaskAssignmentConfig(  );
            config.setIdTask( task.getId(  ) );
            bCreate = true;
        }

        //add workgroups
        List<WorkgroupConfig> listWorkgroupConfig = new ArrayList<WorkgroupConfig>(  );
        WorkgroupConfig workgroupConfig;

        for ( int i = 0; i < tabWorkgroups.length; i++ )
        {
            workgroupConfig = new WorkgroupConfig(  );
            workgroupConfig.setIdTask( task.getId(  ) );
            workgroupConfig.setWorkgroupKey( tabWorkgroups[i] );

            if ( strIsNotification != null )
            {
                if ( WorkflowUtils.convertStringToInt( request.getParameter( PARAMETER_ID_MAILING_LIST + "_" +
                                tabWorkgroups[i] ) ) != -1 )
                {
                    workgroupConfig.setIdMailingList( WorkflowUtils.convertStringToInt( request.getParameter( PARAMETER_ID_MAILING_LIST +
                                "_" + tabWorkgroups[i] ) ) );
                }
                else
                {
                    return AdminMessageService.getMessageUrl( request, MESSAGE_NO_MAILINGLIST_FOR_WORKGROUP,
                        AdminMessage.TYPE_STOP );
                }
            }

            listWorkgroupConfig.add( workgroupConfig );
        }

        config.setTitle( strTitle );
        config.setNotify( strIsNotification != null );
        config.setMultipleOwner( strIsMultipleOwner != null );
        config.setWorkgroups( listWorkgroupConfig );
        config.setMessage( strMessage );
        config.setSubject( strSubject );
        config.setUseUserName( strIsUseUserName != null );

        if ( bCreate )
        {
            _taskAssignmentConfigService.create( config, WorkflowUtils.getPlugin(  ) );
        }
        else
        {
            _taskAssignmentConfigService.update( config, WorkflowUtils.getPlugin(  ) );
        }

        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String doValidateTask( int nIdResource, String strResourceType, HttpServletRequest request, Locale locale,
        ITask task )
    {
        String strError = StringUtils.EMPTY;
        String[] tabWorkgroups = request.getParameterValues( PARAMETER_WORKGROUPS + "_" + task.getId(  ) );
        TaskAssignmentConfig config = _taskAssignmentConfigService.findByPrimaryKey( task.getId(  ),
                WorkflowUtils.getPlugin(  ) );

        if ( config == null )
        {
            return AdminMessageService.getMessageUrl( request, MESSAGE_NO_CONFIGURATION_FOR_TASK_ASSIGNMENT,
                AdminMessage.TYPE_STOP );
        }

        if ( ( tabWorkgroups == null ) || ( tabWorkgroups.length == 0 ) )
        {
            strError = config.getTitle(  );
        }

        if ( StringUtils.isNotBlank( strError ) )
        {
            Object[] tabRequiredFields = { strError };

            return AdminMessageService.getMessageUrl( request, MESSAGE_MANDATORY_FIELD, tabRequiredFields,
                AdminMessage.TYPE_STOP );
        }

        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDisplayConfigForm( HttpServletRequest request, Locale locale, ITask task )
    {
        String strNothing = I18nService.getLocalizedString( PROPERTY_SELECT_EMPTY_CHOICE, locale );

        TaskAssignmentConfig config = _taskAssignmentConfigService.findByPrimaryKey( task.getId(  ),
                WorkflowUtils.getPlugin(  ) );

        ReferenceList refWorkgroups = AdminWorkgroupService.getUserWorkgroups( AdminUserService.getAdminUser( request ),
                locale );

        List<HashMap<String, Object>> listWorkgroups = new ArrayList<HashMap<String, Object>>(  );

        for ( ReferenceItem referenceItem : refWorkgroups )
        {
            if ( !referenceItem.getCode(  ).equals( AdminWorkgroupService.ALL_GROUPS ) )
            {
                HashMap<String, Object> workgroupsItem = new HashMap<String, Object>(  );
                workgroupsItem.put( MARK_ITEM, referenceItem );

                if ( ( config != null ) && ( config.getWorkgroups(  ) != null ) )
                {
                    for ( WorkgroupConfig workgroupSelected : config.getWorkgroups(  ) )
                    {
                        if ( referenceItem.getCode(  ).equals( workgroupSelected.getWorkgroupKey(  ) ) )
                        {
                            workgroupsItem.put( MARK_CONFIG, workgroupSelected );

                            break;
                        }
                    }
                }

                listWorkgroups.add( workgroupsItem );
            }
        }

        ReferenceList refMailingList = new ReferenceList(  );
        refMailingList.addItem( WorkflowUtils.CONSTANT_ID_NULL, strNothing );
        refMailingList.addAll( AdminMailingListService.getMailingLists( AdminUserService.getAdminUser( request ) ) );

        Map<String, Object> model = new HashMap<String, Object>(  );
        model.put( MARK_WORKGROUP_LIST, listWorkgroups );
        model.put( MARK_CONFIG, config );
        model.put( MARK_MAILING_LIST, refMailingList );

        HtmlTemplate template = AppTemplateService.getTemplate( TEMPLATE_TASK_ASSIGNMENT_CONFIG, locale, model );

        return template.getHtml(  );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDisplayTaskForm( int nIdResource, String strResourceType, HttpServletRequest request,
        Locale locale, ITask task )
    {
        Map<String, Object> model = new HashMap<String, Object>(  );
        TaskAssignmentConfig config = _taskAssignmentConfigService.findByPrimaryKey( task.getId(  ),
                WorkflowUtils.getPlugin(  ) );

        ReferenceList refWorkgroups = new ReferenceList(  );

        for ( ReferenceItem referenceItem : AdminWorkgroupService.getUserWorkgroups( AdminUserService.getAdminUser( 
                    request ), locale ) )
        {
            if ( !referenceItem.getCode(  ).equals( AdminWorkgroupService.ALL_GROUPS ) )
            {
                if ( ( config != null ) && ( config.getWorkgroups(  ) != null ) )
                {
                    for ( WorkgroupConfig workgroupSelected : config.getWorkgroups(  ) )
                    {
                        if ( referenceItem.getCode(  ).equals( workgroupSelected.getWorkgroupKey(  ) ) )
                        {
                            refWorkgroups.add( referenceItem );
                        }
                    }
                }
            }
        }

        model.put( MARK_CONFIG, config );
        model.put( MARK_WORKGROUP_LIST, refWorkgroups );

        HtmlTemplate template = AppTemplateService.getTemplate( TEMPLATE_TASK_ASSIGNMENT_FORM, locale, model );

        return template.getHtml(  );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDisplayTaskInformation( int nIdHistory, HttpServletRequest request, Locale locale, ITask task )
    {
        List<AssignmentHistory> listAssignmentHistory = _assignmentHistoryService.getListByHistory( nIdHistory,
                task.getId(  ), WorkflowUtils.getPlugin(  ) );
        Map<String, Object> model = new HashMap<String, Object>(  );

        TaskAssignmentConfig config = _taskAssignmentConfigService.findByPrimaryKey( task.getId(  ),
                WorkflowUtils.getPlugin(  ) );

        ReferenceList refWorkgroups = new ReferenceList(  );

        for ( ReferenceItem referenceItem : AdminWorkgroupService.getUserWorkgroups( AdminUserService.getAdminUser( 
                    request ), locale ) )
        {
            if ( !referenceItem.getCode(  ).equals( AdminWorkgroupService.ALL_GROUPS ) )
            {
                if ( listAssignmentHistory != null )
                {
                    for ( AssignmentHistory assignmentHistory : listAssignmentHistory )
                    {
                        if ( referenceItem.getCode(  ).equals( assignmentHistory.getWorkgroup(  ) ) )
                        {
                            refWorkgroups.add( referenceItem );
                        }
                    }
                }
            }
        }

        model.put( MARK_CONFIG, config );
        model.put( MARK_WORKGROUP_LIST, refWorkgroups );

        HtmlTemplate template = AppTemplateService.getTemplate( TEMPLATE_TASK_ASSIGNMENT_INFORMATION, locale, model );

        return template.getHtml(  );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getTaskInformationXml( int nIdHistory, HttpServletRequest request, Locale locale, ITask task )
    {
        List<AssignmentHistory> listAssignmentHistory = _assignmentHistoryService.getListByHistory( nIdHistory,
                task.getId(  ), WorkflowUtils.getPlugin(  ) );

        StringBuffer strXml = new StringBuffer(  );

        XmlUtil.beginElement( strXml, TAG_ASSIGNMENT );
        XmlUtil.beginElement( strXml, TAG_LIST_WORKGROUP );

        for ( ReferenceItem referenceItem : AdminWorkgroupService.getUserWorkgroups( AdminUserService.getAdminUser( 
                    request ), locale ) )
        {
            if ( !referenceItem.getCode(  ).equals( AdminWorkgroupService.ALL_GROUPS ) )
            {
                if ( listAssignmentHistory != null )
                {
                    for ( AssignmentHistory assignmentHistory : listAssignmentHistory )
                    {
                        if ( referenceItem.getCode(  ).equals( assignmentHistory.getWorkgroup(  ) ) )
                        {
                            XmlUtil.addElementHtml( strXml, TAG_WORKGROUP, referenceItem.getName(  ) );
                        }
                    }
                }
            }
        }

        XmlUtil.endElement( strXml, TAG_LIST_WORKGROUP );
        XmlUtil.endElement( strXml, TAG_ASSIGNMENT );

        return strXml.toString(  );
    }
}