/******************************************************************************
 * Copyright (c) 2005-2021 IBM Corporation and others.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    IBM Corporation - initial API and implementation 
 ****************************************************************************/

package org.eclipse.gmf.tests.runtime.emf.ui.properties;

import org.eclipse.gmf.tests.runtime.emf.ui.properties.sections.UndoableModelPropertySectionTest;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

/**
 * @author gvaradar
 */
public class AllTests extends TestCase {

    public static void main(String[] args) {
        TestRunner.run(suite());
    }

    public static Test suite() {
        TestSuite suite = new TestSuite();
        suite.addTestSuite(UndoableModelPropertySectionTest.class);
        return suite;
    }

    public AllTests() {
        super(""); //$NON-NLS-1$
    }

}
