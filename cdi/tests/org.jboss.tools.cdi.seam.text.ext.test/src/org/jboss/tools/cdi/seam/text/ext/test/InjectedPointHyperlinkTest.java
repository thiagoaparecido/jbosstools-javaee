/*******************************************************************************
 * Copyright (c) 2012 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.cdi.seam.text.ext.test;

import java.util.ArrayList;
import java.util.Collection;

import org.jboss.tools.cdi.core.IBean;
import org.jboss.tools.cdi.seam.solder.core.test.SeamSolderTest;
import org.jboss.tools.cdi.text.ext.CDIExtensionsMessages;
import org.jboss.tools.cdi.text.ext.hyperlink.AssignableBeansHyperlink;
import org.jboss.tools.cdi.text.ext.hyperlink.InjectedPointHyperlink;
import org.jboss.tools.cdi.text.ext.hyperlink.InjectedPointHyperlinkDetector;
import org.jboss.tools.cdi.text.ext.test.CDIHyperlinkTestUtil;
import org.jboss.tools.cdi.text.ext.test.CDIHyperlinkTestUtil.TestHyperlink;
import org.jboss.tools.cdi.text.ext.test.CDIHyperlinkTestUtil.TestRegion;

public class InjectedPointHyperlinkTest extends SeamSolderTest {
	public void testInjectedPointHyperlinkDetector() throws Exception {
		Collection<IBean> beans = getCDIProject().getBeans("/CDISolderTest/src/org/jboss/generic2/MessageManager.java",	true);
		IBean bean=null;
		for(IBean b : beans){
			bean = b;
		}
		ArrayList<TestRegion> regionList = new ArrayList<TestRegion>();
		regionList.add(new TestRegion(/*97, 6*/"Inject",   new TestHyperlink[]{
			new TestHyperlink(InjectedPointHyperlink.class, CDIExtensionsMessages.CDI_INJECTED_POINT_HYPERLINK_OPEN_INJECT_BEAN+ " MessageManager.messageDispatcherProducer()", bean),
			new TestHyperlink(AssignableBeansHyperlink.class, CDIExtensionsMessages.CDI_INJECTED_POINT_HYPERLINK_SHOW_ASSIGNABLE)
		})); // Inject
		regionList.add(new TestRegion(/*106, 7*/"Durable",   new TestHyperlink[]{
			new TestHyperlink(InjectedPointHyperlink.class, CDIExtensionsMessages.CDI_INJECTED_POINT_HYPERLINK_OPEN_INJECT_BEAN+ " MessageManager.messageDispatcherProducer()", bean),
			new TestHyperlink(AssignableBeansHyperlink.class, CDIExtensionsMessages.CDI_INJECTED_POINT_HYPERLINK_SHOW_ASSIGNABLE)
		})); // Logger
		regionList.add(new TestRegion(/*115, 28*/"MessageDispatcher dispatcher",   new TestHyperlink[]{
			new TestHyperlink(InjectedPointHyperlink.class, CDIExtensionsMessages.CDI_INJECTED_POINT_HYPERLINK_OPEN_INJECT_BEAN+ " MessageManager.messageDispatcherProducer()", bean),
			new TestHyperlink(AssignableBeansHyperlink.class, CDIExtensionsMessages.CDI_INJECTED_POINT_HYPERLINK_SHOW_ASSIGNABLE)
		})); // logger
		regionList.add(new TestRegion(/*148, 6*/"Inject",   new TestHyperlink[]{
			new TestHyperlink(InjectedPointHyperlink.class, CDIExtensionsMessages.CDI_INJECTED_POINT_HYPERLINK_OPEN_INJECT_BEAN+ " MessageManager.getPolicy()", bean),
			new TestHyperlink(AssignableBeansHyperlink.class, CDIExtensionsMessages.CDI_INJECTED_POINT_HYPERLINK_SHOW_ASSIGNABLE)
		})); // logger
		regionList.add(new TestRegion(/*157, 7*/"Durable",   new TestHyperlink[]{
			new TestHyperlink(InjectedPointHyperlink.class, CDIExtensionsMessages.CDI_INJECTED_POINT_HYPERLINK_OPEN_INJECT_BEAN+ " MessageManager.getPolicy()", bean),
			new TestHyperlink(AssignableBeansHyperlink.class, CDIExtensionsMessages.CDI_INJECTED_POINT_HYPERLINK_SHOW_ASSIGNABLE)
		})); // logger
		regionList.add(new TestRegion(/*166, 23*/"DispatcherPolicy policy",   new TestHyperlink[]{
			new TestHyperlink(InjectedPointHyperlink.class, CDIExtensionsMessages.CDI_INJECTED_POINT_HYPERLINK_OPEN_INJECT_BEAN+ " MessageManager.getPolicy()", bean),
			new TestHyperlink(AssignableBeansHyperlink.class, CDIExtensionsMessages.CDI_INJECTED_POINT_HYPERLINK_SHOW_ASSIGNABLE)
		})); // logger
		regionList.add(new TestRegion(/*281, 31*/"Durable DispatcherPolicy policy",   new TestHyperlink[]{
			new TestHyperlink(InjectedPointHyperlink.class, CDIExtensionsMessages.CDI_INJECTED_POINT_HYPERLINK_OPEN_INJECT_BEAN+ " MessageManager.getPolicy()", bean),
			new TestHyperlink(AssignableBeansHyperlink.class, CDIExtensionsMessages.CDI_INJECTED_POINT_HYPERLINK_SHOW_ASSIGNABLE)
		})); // logger
		regionList.add(new TestRegion(/*318, 6*/"policy",   new TestHyperlink[]{
			new TestHyperlink(InjectedPointHyperlink.class, CDIExtensionsMessages.CDI_INJECTED_POINT_HYPERLINK_OPEN_INJECT_BEAN+ " MessageManager.getPolicy()", bean),
			new TestHyperlink(AssignableBeansHyperlink.class, CDIExtensionsMessages.CDI_INJECTED_POINT_HYPERLINK_SHOW_ASSIGNABLE)
		})); // logger

		CDIHyperlinkTestUtil.checkRegions(getTestProject(), "src/org/jboss/generic2/DurableMessageLogger.java", regionList, new InjectedPointHyperlinkDetector());
	}
}