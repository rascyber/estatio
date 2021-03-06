/*
 *  Copyright 2014 Dan Haywood
 *
 *  Licensed under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.estatio.module.base.fixtures.security;

import org.apache.isis.applib.annotation.Programmatic;
import org.apache.isis.applib.fixturescripts.FixtureScript;
import org.apache.isis.objectstore.jdo.applib.service.support.IsisJdoSupport;

@Programmatic
public class SecurityModuleWithinEstatioTearDown extends FixtureScript {

    @Override
    protected void execute(ExecutionContext executionContext) {

        executeSqlIgnoreExceptions("delete from \"IsisSecurity.ApplicationPermission\"");
        executeSqlIgnoreExceptions("delete from \"IsisSecurity.ApplicationUserRoles\"");
        executeSqlIgnoreExceptions("delete from \"IsisSecurity.ApplicationRole\"");
        executeSqlIgnoreExceptions("delete from \"IsisSecurity.ApplicationUser\"");
        executeSqlIgnoreExceptions("delete from \"IsisSecurity.ApplicationTenancy\"");
    }

    private void executeSqlIgnoreExceptions(final String sql) {
        try {
            isisJdoSupport.executeUpdate(sql);
        } catch (Exception e) {
            // ignore
        }
    }

    @javax.inject.Inject
    private IsisJdoSupport isisJdoSupport;

}
