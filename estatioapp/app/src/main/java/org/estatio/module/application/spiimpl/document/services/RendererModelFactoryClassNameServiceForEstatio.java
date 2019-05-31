/*
 *
 *  Copyright 2012-2014 Eurocommercial Properties NV
 *
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
package org.estatio.module.application.spiimpl.document.services;

import java.util.List;

import org.apache.isis.applib.annotation.DomainService;
import org.apache.isis.applib.annotation.NatureOfService;

import org.estatio.module.document.dom.impl.applicability.RendererModelFactory;
import org.estatio.module.document.dom.services.ClassNameServiceAbstract;
import org.estatio.module.document.dom.services.ClassNameViewModel;
import org.estatio.module.document.dom.spi.RendererModelFactoryClassNameService;

@DomainService(nature = NatureOfService.DOMAIN)
public class RendererModelFactoryClassNameServiceForEstatio extends ClassNameServiceAbstract<RendererModelFactory> implements
        RendererModelFactoryClassNameService {

    public RendererModelFactoryClassNameServiceForEstatio() {
        super(RendererModelFactory.class, "org.estatio");
    }

    @Override
    public List<ClassNameViewModel> rendererModelFactoryClassNames() {
        return this.classNames();
    }
}
