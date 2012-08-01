/**
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */
package org.openmrs.module.auditlog.api;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.openmrs.api.context.Context;
import org.openmrs.module.auditlog.AuditLog.Action;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.openmrs.test.Verifies;

public class AuditLogServiceTest extends BaseModuleContextSensitiveTest {
	
	private static final String MODULE_TEST_DATA = "moduleTestData.xml";
	
	private static final String MODULE_TEST_DATA_AUDIT_LOGS = "moduleTestData-initialAuditLogs.xml";
	
	private AuditLogService service;
	
	@Before
	public void before() throws Exception {
		executeDataSet(MODULE_TEST_DATA);
		service = Context.getService(AuditLogService.class);
	}
	
	/**
	 * @see {@link AuditLogService#getAuditLogs(Class<*>,List<Action>,Date,Date,Integer,Integer)}
	 */
	@Test
	@Verifies(value = "should match on the specified audit log action", method = "getAuditLogs(Class<*>,List<Action>,Date,Date,Integer,Integer)")
	public void getAuditLogs_shouldMatchOnTheSpecifiedAuditLogAction() throws Exception {
		executeDataSet(MODULE_TEST_DATA_AUDIT_LOGS);
		List<Action> actions = new ArrayList<Action>();
		actions.add(Action.CREATED);//get only inserts
		Assert.assertEquals(1, service.getAuditLogs(null, actions, null, null, null, null).size());
		
		actions.add(Action.UPDATED);//get both insert and update logs
		Assert.assertEquals(3, service.getAuditLogs(null, actions, null, null, null, null).size());
		
		actions.clear();
		actions.add(Action.UPDATED);//get only updates
		Assert.assertEquals(2, service.getAuditLogs(null, actions, null, null, null, null).size());
		
		actions.clear();
		actions.add(Action.DELETED);//get only deletes
		Assert.assertEquals(1, service.getAuditLogs(null, actions, null, null, null, null).size());
	}
	
	/**
	 * @see {@link AuditLogService#getAuditLogs(Class<*>,List<Action>,Date,Date,Integer,Integer)}
	 */
	@Test
	@Verifies(value = "should return all audit logs in the database if all args are null", method = "getAuditLogs(Class<*>,List<Action>,Date,Date,Integer,Integer)")
	public void getAuditLogs_shouldReturnAllAuditLogsInTheDatabaseIfAllArgsAreNull() throws Exception {
		executeDataSet(MODULE_TEST_DATA_AUDIT_LOGS);
		Assert.assertEquals(4, service.getAuditLogs(null, null, null, null, null, null).size());
	}
}
