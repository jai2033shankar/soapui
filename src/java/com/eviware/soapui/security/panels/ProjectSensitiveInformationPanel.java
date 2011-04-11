/*
 *  soapUI, copyright (C) 2004-2011 eviware.com 
 *
 *  soapUI is free software; you can redistribute it and/or modify it under the 
 *  terms of version 2.1 of the GNU Lesser General Public License as published by 
 *  the Free Software Foundation.
 *
 *  soapUI is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without 
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. 
 *  See the GNU Lesser General Public License for more details at gnu.org.
 */
package com.eviware.soapui.security.panels;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.apache.xmlbeans.XmlObject;
import org.jdesktop.swingx.JXTable;

import com.eviware.soapui.config.ProjectConfig;
import com.eviware.soapui.config.SensitiveInformationConfig;
import com.eviware.soapui.impl.wsdl.support.HelpUrls;
import com.eviware.soapui.model.security.SensitiveInformationTableModel;
import com.eviware.soapui.model.testsuite.TestProperty;
import com.eviware.soapui.security.SensitiveInformationPropertyHolder;
import com.eviware.soapui.support.StringUtils;
import com.eviware.soapui.support.UISupport;
import com.eviware.soapui.support.components.JXToolBar;
import com.eviware.soapui.support.xml.XmlObjectConfigurationBuilder;
import com.eviware.soapui.support.xml.XmlObjectConfigurationReader;
import com.eviware.x.form.XFormDialog;
import com.eviware.x.form.support.ADialogBuilder;
import com.eviware.x.form.support.AField;
import com.eviware.x.form.support.AForm;
import com.eviware.x.form.support.AField.AFieldType;

public class ProjectSensitiveInformationPanel
{

	private XFormDialog dialog;
	private SensitiveInformationConfig config;
	private List<String> projectSpecificExposureList;
	public static final String PROJECT_SPECIFIC_EXPOSURE_LIST = "ProjectSpecificExposureList";
	private SensitiveInformationTableModel sensitivInformationTableModel;
	private JXTable tokenTable;
	private JPanel sensitiveInfoTableForm;
	
	public ProjectSensitiveInformationPanel( ProjectConfig projectConfig )
	{
		config = projectConfig.getSensitiveInformation();
		if( config == null )
		{
			config = SensitiveInformationConfig.Factory.newInstance();
			projectConfig.addNewSensitiveInformation();
			projectConfig.setSensitiveInformation( config );
		}
		init();
	}

	private void init()
	{
		XmlObjectConfigurationReader reader = new XmlObjectConfigurationReader( config );
		projectSpecificExposureList = StringUtils.toStringList( reader.readStrings( PROJECT_SPECIFIC_EXPOSURE_LIST ) );
		extractTokenTable();
	}
	
	private void extractTokenTable()
	{
		SensitiveInformationPropertyHolder siph = new SensitiveInformationPropertyHolder();
		for( String str : projectSpecificExposureList )
		{
			String[] tokens = str.split( "###" );
			if( tokens.length == 2 )
			{
				siph.setPropertyValue( tokens[0], tokens[1] );
			}
			else
			{
				siph.setPropertyValue( tokens[0], "" );
			}
		}
		sensitivInformationTableModel = new SensitiveInformationTableModel( siph );
	}

	public boolean build()
	{
		if( dialog == null )
			buildDialog();

		return false;
	}

	public void save()
	{
		projectSpecificExposureList = createListFromTable();
		setConfiguration( createConfiguration() );
	}
	
	private List<String> createListFromTable()
	{
		List<String> temp = new ArrayList<String>();
		for(TestProperty tp:sensitivInformationTableModel.getHolder().getPropertyList()){
			String tokenPlusDescription = tp.getName()+"###"+tp.getValue();
			temp.add( tokenPlusDescription );
		}
		return temp;
	}
	
	
	protected XmlObject createConfiguration()
	{
		XmlObjectConfigurationBuilder builder = new XmlObjectConfigurationBuilder();
		builder.add( PROJECT_SPECIFIC_EXPOSURE_LIST, projectSpecificExposureList
				.toArray( new String[projectSpecificExposureList.size()] ) );
		return builder.finish();
	}

	protected void buildDialog()
	{
		dialog = ADialogBuilder.buildDialog( SensitiveInformationConfigDialog.class );
		dialog.getFormField( SensitiveInformationConfigDialog.TOKENS ).setProperty( "component", getForm() );

	}



	// TODO : update help URL
	@AForm( description = "Configure Sensitive Information Exposure Assertion", name = "Sensitive Information Exposure Assertion", helpUrl = HelpUrls.HELP_URL_ROOT )
	protected interface SensitiveInformationConfigDialog
	{

		@AField( description = "Sensitive informations to check. Use ~ as prefix for values that are regular expressions.", name = "Sensitive Information Tokens", type = AFieldType.COMPONENT )
		public final static String TOKENS = "Sensitive Information Tokens";
	}

	public void setConfiguration( XmlObject configuration )
	{
		config.set( configuration );
	}

	public XFormDialog getDialog()
	{
		return dialog;
	}
	
	
	public JPanel getForm()
	{
		if( sensitiveInfoTableForm == null )
		{
			sensitiveInfoTableForm = new JPanel( new BorderLayout() );

			JXToolBar toolbar = UISupport.createToolbar();

			toolbar.add( UISupport.createToolbarButton( new AddTokenAction() ) );
			toolbar.add( UISupport.createToolbarButton( new RemoveTokenAction() ) );

			tokenTable = new JXTable( sensitivInformationTableModel );
			tokenTable.setPreferredSize( new Dimension( 200, 100 ) );
			sensitiveInfoTableForm.add( toolbar, BorderLayout.NORTH );
			sensitiveInfoTableForm.add( new JScrollPane( tokenTable ), BorderLayout.CENTER );
		}

		return sensitiveInfoTableForm;
	}

	class AddTokenAction extends AbstractAction
	{

		public AddTokenAction()
		{
			putValue( Action.SMALL_ICON, UISupport.createImageIcon( "/add_property.gif" ) );
			putValue( Action.SHORT_DESCRIPTION, "Adds a token to assertion" );
		}

		@Override
		public void actionPerformed( ActionEvent arg0 )
		{
			String newToken = "";
			newToken = UISupport.prompt( "Enter token", "New Token", newToken );
			String newValue = "";
			newValue = UISupport.prompt( "Enter description", "New Description", newValue );
			
			sensitivInformationTableModel.addToken(newToken, newValue);
			save();
		}

	}

	class RemoveTokenAction extends AbstractAction
	{

		public RemoveTokenAction()
		{
			putValue( Action.SMALL_ICON, UISupport.createImageIcon( "/remove_property.gif" ) );
			putValue( Action.SHORT_DESCRIPTION, "Removes token from assertion" );
		}

		@Override
		public void actionPerformed( ActionEvent arg0 )
		{
			sensitivInformationTableModel.removeRows(tokenTable.getSelectedRows());
			save();
		}

	}
}
