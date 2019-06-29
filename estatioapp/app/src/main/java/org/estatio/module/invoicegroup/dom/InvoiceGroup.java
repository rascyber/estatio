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
package org.estatio.module.invoicegroup.dom;

import java.util.SortedSet;
import java.util.TreeSet;

import javax.inject.Inject;
import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.IdentityType;
import javax.jdo.annotations.VersionStrategy;

import org.apache.isis.applib.annotation.CollectionLayout;
import org.apache.isis.applib.annotation.DomainObject;
import org.apache.isis.applib.annotation.Editing;
import org.apache.isis.applib.annotation.Property;
import org.apache.isis.applib.annotation.PropertyLayout;

import org.isisaddons.module.security.dom.tenancy.ApplicationTenancy;

import org.incode.module.base.dom.types.NameType;
import org.incode.module.base.dom.types.ReferenceType;
import org.incode.module.base.dom.utils.TitleBuilder;
import org.incode.module.base.dom.with.WithReferenceComparable;
import org.incode.module.base.dom.with.WithReferenceUnique;
import org.incode.module.country.dom.impl.Country;

import org.estatio.module.base.dom.UdoDomainObject2;
import org.estatio.module.base.dom.apptenancy.WithApplicationTenancyCountry;
import org.estatio.module.countryapptenancy.dom.EstatioApplicationTenancyRepositoryForCountry;

import lombok.Getter;
import lombok.Setter;

/**
 *
 */
@javax.jdo.annotations.PersistenceCapable(
        identityType = IdentityType.DATASTORE
        ,schema = "invoiceGroup"   // Isis' ObjectSpecId inferred from @DomainObject#objectType
)
@javax.jdo.annotations.DatastoreIdentity(
        strategy = IdGeneratorStrategy.NATIVE,
        column = "id")
@javax.jdo.annotations.Version(
        strategy = VersionStrategy.VERSION_NUMBER,
        column = "version")
@javax.jdo.annotations.Uniques({
        @javax.jdo.annotations.Unique(
                name = "InvoicingGroup_reference_UNQ", members = "reference")
})
@javax.jdo.annotations.Queries({
        @javax.jdo.annotations.Query(
                name = "findByReference", language = "JDOQL",
                value = "SELECT "
                        + "FROM org.estatio.module.invoicegroup.dom.InvoiceGroup "
                        + "WHERE reference == :reference"),
        @javax.jdo.annotations.Query(
                name = "findContainingProperty", language = "JDOQL",
                value = "SELECT "
                        + "FROM org.estatio.module.invoicegroup.dom.InvoiceGroup "
                        + "WHERE this.properties.contains(:property)")
})
@DomainObject(
        editing = Editing.DISABLED,
        bounded = true,
        objectType = "invoiceGroup.InvoiceGroup"
)
public class InvoiceGroup
        extends UdoDomainObject2<InvoiceGroup>
        implements WithReferenceComparable<InvoiceGroup>, WithReferenceUnique, WithApplicationTenancyCountry {


    public InvoiceGroup() {
        super("reference");
    }
    public InvoiceGroup(
            final String reference,
            final String name,
            final Country country) {
        super("reference");
        this.reference = reference;
        this.name = name;
        this.country = country;
    }

    public String title() {
        return TitleBuilder.start()
                .withReference(getReference())
                .withName(getName())
                .toString();
    }

    @PropertyLayout(
            named = "Application Level",
            describedAs = "Determines those users for whom this object is available to view and/or modify (derived from country)"
    )
    @Override
    public ApplicationTenancy getApplicationTenancy() {
        return appTenancyRepositoryForCountry.findOrCreateTenancyFor(country);
    }


    @javax.jdo.annotations.Column(allowsNull = "false", length = ReferenceType.Meta.MAX_LEN)
    @Property(regexPattern = ReferenceType.Meta.REGEX)
    @Getter @Setter
    private String reference;


    @javax.jdo.annotations.Column(allowsNull = "false", length = NameType.Meta.MAX_LEN)
    @Getter @Setter
    private String name;

    /**
     * Rather
     */
    @javax.jdo.annotations.Column(name = "countryId", allowsNull = "false")
    @Getter @Setter
    private Country country;

    /**
     * Modelled as a 1-N unidirectional relationship
     *
     * @see <a href="http://www.datanucleus.org:15080/products/accessplatform_5_2/jdo/mapping.html#one_many_join_bi">DN docs</a>
     */
    @CollectionLayout(defaultView = "table")
    @javax.jdo.annotations.Persistent(table = "InvoiceGroupProperty")
    @javax.jdo.annotations.Join(column = "invoiceGroupId")
    @javax.jdo.annotations.Element(column = "propertyId")
    @Getter @Setter
    private SortedSet<org.estatio.module.asset.dom.Property> properties = new TreeSet<>();

    @Inject
    EstatioApplicationTenancyRepositoryForCountry appTenancyRepositoryForCountry;

}