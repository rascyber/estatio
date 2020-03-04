/*
 *
 *  Copyright 2012-2015 Eurocommercial Properties NV
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
package org.estatio.module.capex.dom.project;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.jdo.annotations.Column;
import javax.jdo.annotations.DatastoreIdentity;
import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.IdentityType;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.Queries;
import javax.jdo.annotations.Query;
import javax.jdo.annotations.Unique;
import javax.jdo.annotations.Version;
import javax.jdo.annotations.VersionStrategy;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import com.google.common.collect.Lists;

import org.joda.time.LocalDate;

import org.apache.isis.applib.annotation.Action;
import org.apache.isis.applib.annotation.ActionLayout;
import org.apache.isis.applib.annotation.BookmarkPolicy;
import org.apache.isis.applib.annotation.Contributed;
import org.apache.isis.applib.annotation.DomainObject;
import org.apache.isis.applib.annotation.DomainObjectLayout;
import org.apache.isis.applib.annotation.Editing;
import org.apache.isis.applib.annotation.MemberOrder;
import org.apache.isis.applib.annotation.Optionality;
import org.apache.isis.applib.annotation.Parameter;
import org.apache.isis.applib.annotation.Programmatic;
import org.apache.isis.applib.annotation.Property;
import org.apache.isis.applib.annotation.PropertyLayout;
import org.apache.isis.applib.annotation.SemanticsOf;
import org.apache.isis.applib.annotation.Where;
import org.apache.isis.applib.services.clock.ClockService;
import org.apache.isis.applib.services.factory.FactoryService;
import org.apache.isis.applib.services.repository.RepositoryService;
import org.apache.isis.applib.services.wrapper.WrapperFactory;
import org.apache.isis.schema.utils.jaxbadapters.PersistentEntityAdapter;

import org.isisaddons.module.security.app.user.MeService;
import org.isisaddons.module.security.dom.tenancy.ApplicationTenancy;
import org.isisaddons.module.security.dom.tenancy.ApplicationTenancyRepository;

import org.incode.module.base.dom.types.NameType;
import org.incode.module.base.dom.types.ReferenceType;
import org.incode.module.base.dom.utils.TitleBuilder;
import org.incode.module.base.dom.with.WithReferenceUnique;
import org.incode.module.docfragment.dom.types.AtPathType;

import org.estatio.module.base.dom.UdoDomainObject;
import org.estatio.module.base.dom.apptenancy.WithApplicationTenancyGlobalAndCountry;
import org.estatio.module.capex.dom.invoice.contributions.Project_InvoiceItemsNotOnProjectItem;
import org.estatio.module.capex.dom.order.contributions.Project_OrderItemsNotOnProjectItem;
import org.estatio.module.charge.dom.Charge;
import org.estatio.module.party.dom.Party;
import org.estatio.module.tax.dom.Tax;

import lombok.Getter;
import lombok.Setter;

@PersistenceCapable(
        identityType = IdentityType.DATASTORE
        , schema = "dbo"
)
@DatastoreIdentity(strategy = IdGeneratorStrategy.NATIVE, column = "id")
@Version(strategy = VersionStrategy.VERSION_NUMBER, column = "version")
@Unique(members = { "reference" })
@Queries({
        @Query(name = "findByReference", language = "JDOQL", value = "SELECT "
                + "FROM org.estatio.module.capex.dom.project.Project "
                + "WHERE reference == :reference "),
        @Query(name = "matchByReferenceOrName", language = "JDOQL", value = "SELECT "
                + "FROM org.estatio.module.capex.dom.project.Project "
                + "WHERE reference.matches(:matcher) || name.matches(:matcher) "),
        @Query(name = "findByParent", language = "JDOQL", value = "SELECT "
                + "FROM org.estatio.module.capex.dom.project.Project "
                + "WHERE parent == :parent "),
        @Query(name = "findReviewDateInInterval", language = "JDOQL",
                value = "SELECT "
                + "FROM org.estatio.module.capex.dom.project.Project "
                + "WHERE "
                + "reviewDate != null && (reviewDate >= :intervalStartDate && reviewDate <= :intervalEndDate) "
                + "ORDER BY reviewDate")
})
@DomainObject(
        editing = Editing.DISABLED,
        objectType = "org.estatio.capex.dom.project.Project",
        autoCompleteRepository = ProjectRepository.class
)
@DomainObjectLayout(bookmarking = BookmarkPolicy.AS_ROOT)
@XmlJavaTypeAdapter(PersistentEntityAdapter.class)
public class Project extends UdoDomainObject<Project> implements
        WithReferenceUnique, WithApplicationTenancyGlobalAndCountry {

    public Project() {
        super("reference, name, startDate");
    }

    public Project(String reference) {
        this();
        setReference(reference);
    }

    public String title() {
        return TitleBuilder.start().withReference(getReference()).withName(getName()).toString();
    }

    @Column(length = ReferenceType.Meta.MAX_LEN, allowsNull = "false")
    @Property(regexPattern = ReferenceType.Meta.REGEX)
    @PropertyLayout(describedAs = "Unique reference code for this project")
    @Getter @Setter
    private String reference;

    @Column(length = NameType.Meta.MAX_LEN, allowsNull = "false")
    @Getter @Setter
    private String name;

    @Column(allowsNull = "true")
    @Getter @Setter
    private LocalDate startDate;

    @Column(allowsNull = "true")
    @Persistent
    @MemberOrder(sequence = "4")
    @Getter @Setter
    private LocalDate endDate;

    @Column(allowsNull = "false", length = AtPathType.Meta.MAX_LEN)
    @Getter @Setter
    @Property(hidden = Where.EVERYWHERE)
    @PropertyLayout(named = "Application Level Path")
    private String atPath;

    @PropertyLayout(
            named = "Application Level",
            describedAs = "Determines those users for whom this object is available to view and/or modify.",
            hidden = Where.ALL_TABLES
    )
    public ApplicationTenancy getApplicationTenancy() {
        return applicationTenancyRepository.findByPath(getAtPath());
    }

    @Persistent(mappedBy = "project", dependentElement = "true")
    @Getter @Setter
    private SortedSet<ProjectItem> items = new TreeSet<>();

    @Persistent(mappedBy = "parent", dependentElement = "true")
    @Getter @Setter
    private SortedSet<Project> children = new TreeSet<Project>();

    @Column(allowsNull = "true", name = "parentId")
    @Getter @Setter
    private Project parent;

    @Action(semantics = SemanticsOf.IDEMPOTENT)
    public Project changeProject(final String name, @Parameter(optionality = Optionality.OPTIONAL) final Project parent) {
        setName(name);
        setParent(parent);
        return this;
    }

    public String default0ChangeProject() {
        return getName();
    }

    public Project default1ChangeProject() {
        return getParent();
    }

    public List<Project> choices1ChangeProject() {
        return projectRepository.listAll()
                .stream()
                .filter(x -> !x.equals(this))
                .filter(x -> x.isParentProject() || x.getItems().size() == 0)
                .collect(Collectors.toList());
    }

	@Action(semantics = SemanticsOf.NON_IDEMPOTENT_ARE_YOU_SURE)
	public void delete(){
		repositoryService.remove(this);
	}

	public boolean hideDelete() {
    	for (ProjectItem item : getItems()){
    		if (item.hideDelete()) return true;
		}
		// TODO: use events when decoupling
        if (!factoryService.mixin(Project_InvoiceItemsNotOnProjectItem.class, this).invoiceItemsNotOnProjectItem().isEmpty() ||
            !factoryService.mixin(Project_OrderItemsNotOnProjectItem.class, this).orderItemsNotOnProjectItem().isEmpty()){
                return true;
            }
		return false;
	}
	
	@Action(semantics = SemanticsOf.IDEMPOTENT)
    public Project changeDates(@Parameter(optionality = Optionality.OPTIONAL) final LocalDate startDate, @Parameter(optionality = Optionality.OPTIONAL) final LocalDate endDate) {
        setStartDate(startDate);
        setEndDate(endDate);
        return this;
    }

    public LocalDate default0ChangeDates() {
        return getStartDate();
    }

    public LocalDate default1ChangeDates() {
        return getEndDate();
    }

    public String validateChangeDates(final LocalDate startDate, final LocalDate endDate) {
        return validateNewProject(null, startDate, endDate);
    }

    @Action(semantics = SemanticsOf.NON_IDEMPOTENT)
    public Project createParentProject(
            final @Parameter(maxLength = ReferenceType.Meta.MAX_LEN) String reference,
            final String name,
            @Parameter(optionality = Optionality.OPTIONAL) final LocalDate startDate,
            @Parameter(optionality = Optionality.OPTIONAL) final LocalDate endDate) {
        Project parent = projectRepository.create(reference, name, startDate, endDate, getAtPath(), null);
        wrapperFactory.wrap(parent).addChildProject(this);
        return parent;
    }

    public String disableCreateParentProject() {
        return parent != null ? "The project has a parent already" : null;
    }

    public String validateCreateParentProject(
            final String reference,
            final String name,
            final LocalDate startDate,
            final LocalDate endDate) {
        return validateNewProject(reference, startDate, endDate);
    }

    @Action(semantics = SemanticsOf.NON_IDEMPOTENT)
    @MemberOrder(sequence = "2", name = "children")
    public Project createChildProject(
            final @Parameter(maxLength = ReferenceType.Meta.MAX_LEN) String reference,
            final String name,
            @Parameter(optionality = Optionality.OPTIONAL) final LocalDate startDate,
            @Parameter(optionality = Optionality.OPTIONAL) final LocalDate endDate) {
        return projectRepository.create(reference, name, startDate, endDate, getAtPath(), this);
    }

    public String validateCreateChildProject(final String reference, final String name, final LocalDate startDate, final LocalDate endDate) {
        return validateNewProject(reference, startDate, endDate);
    }

    // TODO: (ECP-438) until we find out more about the process
    public String disableCreateChildProject() {
        return getItems().isEmpty() ? null : "This project cannot be a parent because it has items";
    }

    @Action(semantics = SemanticsOf.IDEMPOTENT)
    @MemberOrder(sequence = "1", name = "children")
    public Project addChildProject(final Project child) {
        child.setParent(this);
        return this;
    }

    public String validateAddChildProject(final Project child) {
        if (child.getParent() != null)
            return "The child project is linked to a parent already";
        if (child == this)
            return "A project cannot have itself as a child";
        return null;
    }

    public List<Project> autoComplete0AddChildProject(final String search) {
        return projectRepository.findProject(search)
                .stream()
                .filter(x -> x != this)
                .filter(x -> !getChildren().contains(x))
                .filter(x -> x.getAtPath().equals(getAtPath()))
                .collect(Collectors.toList());
    }

    // TODO: (ECP-438) until we find out more about the process
    public String disableAddChildProject() {
        return getItems().isEmpty() ? null : "This project cannot be a parent because it has items";
    }

    @MemberOrder(name = "items", sequence = "1")
    public Project addItem(
            final Charge charge,
            final String description,
            @Parameter(optionality = Optionality.OPTIONAL) final org.estatio.module.asset.dom.Property property,
            @Parameter(optionality = Optionality.OPTIONAL) final Tax tax
    ) {
        projectItemRepository.findOrCreate(
                this, charge, description, property, tax);
        return this;
    }

    public String disableAddItem() {
        if (isApproved()) return "This project is approved and therefore cannot be changed";
        return isParentProject() ? "This project is a parent" : null;
    }

    @Action(semantics = SemanticsOf.SAFE)
    public List<BudgetForecast> getBudgetForecasts(){
        return budgetForecastRepositoryAndFactory.findByProject(this)
                .stream()
                .sorted(Comparator.comparing(BudgetForecast::getDate).reversed())
                .collect(Collectors.toList());
    }

    @Persistent(mappedBy = "project")
    @Getter @Setter
    private SortedSet<ProjectRole> roles = new TreeSet<>();

    @MemberOrder(name = "roles", sequence = "1")
    public Project newRole(
            final Party party,
            final ProjectRoleTypeEnum type,
            @Parameter(optionality = Optionality.OPTIONAL) final LocalDate startDate,
            @Parameter(optionality = Optionality.OPTIONAL) final LocalDate endDate) {
        projectRoleRepository.create(this, party, type, startDate, endDate);
        return this;
    }

    @Property(notPersisted = true, hidden = Where.ALL_TABLES)
    public BigDecimal getBudgetedAmount() {
        return isParentProject() ? budgetedAmountWhenParentProject() : sum(ProjectItem::getBudgetedAmount);
    }

    private BigDecimal sum(final Function<ProjectItem, BigDecimal> x) {
        return Lists.newArrayList(getItems()).stream()
                .map(x)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal budgetedAmountWhenParentProject() {
        BigDecimal result = BigDecimal.ZERO;
        for (Project child : getChildren()) {
            result = result.add(child.getBudgetedAmount());
        }
        return result;
    }

    @Programmatic
    public boolean isParentProject() {
        return getChildren().isEmpty() ? false : true;
    }

    private String validateNewProject(final String reference, final LocalDate startDate, final LocalDate endDate) {
        if (projectRepository.findByReference(reference) != null)
            return "There is already a project with this reference";
        return startDate != null && endDate != null && !startDate.isBefore(endDate) ? "End date must be after start date" : null;
    }

    @Getter @Setter
    private boolean archived;

    @Action(semantics = SemanticsOf.IDEMPOTENT)
    public Project archive(){
        setArchived(true);
        return this;
    }

    public boolean hideArchive(){
        return isArchived();
    }

    @Action(semantics = SemanticsOf.IDEMPOTENT)
    public Project unArchive(){
        setArchived(false);
        return this;
    }

    public boolean hideUnArchive(){
        return !isArchived();
    }

    @Action(semantics = SemanticsOf.SAFE)
    public ProjectBudget getProjectBudget(){
        return projectBudgetRepository.findByProject(this).stream().filter(b->b.getNext()==null).findFirst().orElse(null);
    }

    @Programmatic
    public ProjectBudget getLatestCommittedBudget(){
        final Optional<ProjectBudget> budgetIfAny = projectBudgetRepository.findCommittedByProject(this).stream()
                .max(Comparator.comparing(ProjectBudget::getBudgetVersion));
        return budgetIfAny.isPresent() ? budgetIfAny.get() : null;
    }

    @Action(semantics = SemanticsOf.SAFE)
    public boolean isApproved(){
        final ProjectBudget approvedBudget = projectBudgetRepository.findByProject(this).stream()
                .filter(b -> b.getApprovedOn() != null).findFirst().orElse(null);
        return approvedBudget!=null ? true : false;
    }

    @Action(semantics = SemanticsOf.SAFE)
    public boolean isCommitted(){
        final ProjectBudget commitedBudget = projectBudgetRepository.findByProject(this).stream()
                .filter(b -> b.getCommittedOn() != null).findFirst().orElse(null);
        return commitedBudget!=null ? true : false;
    }

    @Action(semantics = SemanticsOf.NON_IDEMPOTENT)
    @ActionLayout(contributed = Contributed.AS_ACTION)
    public BudgetCreationManager createBudget(){
        return new BudgetCreationManager(this, 1);
    }

    public boolean hideCreateBudget(){
        if (getProjectBudget()!=null && getProjectBudget().getApprovedOn()!=null) return true;
        if (getLatestCommittedBudget()!=null) return true; // this is needed in case of an unapproved amendment
        return false;
    }

    @Action(semantics = SemanticsOf.NON_IDEMPOTENT)
    @ActionLayout(contributed = Contributed.AS_ACTION)
    public BudgetCreationManager amendBudget(){
        final int newVersion = getLatestCommittedBudget().getBudgetVersion() + 1;
        return new BudgetCreationManager(this, newVersion);
    }

    public boolean hideAmendBudget(){
        return !hideCreateBudget();
    }

    @Action(semantics = SemanticsOf.NON_IDEMPOTENT)
    @ActionLayout(contributed = Contributed.AS_ACTION)
    public Project approveBudget(final LocalDate approvalDate){
        final ProjectBudget unApprovedBudget = projectBudgetRepository.findByProject(this).stream()
                .filter(b -> b.getApprovedOn() == null).findFirst().orElse(null);
        if (unApprovedBudget!=null){
            unApprovedBudget.setApprovedOn(approvalDate);
            unApprovedBudget.setApprovedBy(meService.me().getUsername());
        }
        // also commit if there is a committed budget already
        if (getLatestCommittedBudget()!=null) {
            unApprovedBudget.setCommittedBy(getLatestCommittedBudget().getCommittedBy());
            unApprovedBudget.setCommittedOn(getLatestCommittedBudget().getCommittedOn());
        }
        return this;
    }

    public String disableApproveBudget(){
        final ProjectBudget unApprovedBudget = projectBudgetRepository.findByProject(this).stream()
                .filter(b -> b.getApprovedOn() == null).findFirst().orElse(null);
        if (unApprovedBudget==null) return "No budget to approve";
        return null;
    }

    public LocalDate default0ApproveBudget(){
        return clockService.now();
    }

    @Action(semantics = SemanticsOf.NON_IDEMPOTENT)
    @ActionLayout(contributed = Contributed.AS_ACTION)
    public Project commitBudget(final LocalDate commitmentDate){
        final ProjectBudget approvedButUnCommittedBudget = projectBudgetRepository.findByProject(this).stream()
                .filter(b -> b.getApprovedOn() != null)
                .filter(b->b.getCommittedOn() == null)
                .findFirst().orElse(null);
        if (approvedButUnCommittedBudget!=null){
            approvedButUnCommittedBudget.setCommittedOn(commitmentDate);
            approvedButUnCommittedBudget.setCommittedBy(meService.me().getUsername());
        }
        return this;
    }

    public boolean hideCommitBudget(){
        final ProjectBudget approvedButUnCommittedBudget = projectBudgetRepository.findByProject(this).stream()
                .filter(b -> b.getApprovedOn() != null)
                .filter(b->b.getCommittedOn() == null)
                .findFirst().orElse(null);
        if (approvedButUnCommittedBudget==null) return true;
        return false;
    }

    public LocalDate default0CommitBudget(){
        return clockService.now();
    }

    @Action(semantics = SemanticsOf.NON_IDEMPOTENT)
    @ActionLayout(contributed = Contributed.AS_ACTION)
    public ForecastCreationManager createBudgetForecast(final LocalDate date){
        return new ForecastCreationManager(this, ForecastFrequency.QUARTERLY.getStartDateFor(date));
    }

    public LocalDate default0CreateBudgetForecast(){
        return ForecastFrequency.QUARTERLY.getStartDateFor(clockService.now());
    }

    public String disableCreateBudgetForecast(){
        if (!isCommitted()) return "Project is not committed";
        return null;
    }

    public String validateCreateBudgetForecast(final LocalDate date){
        final BudgetForecast forecastIfAny = budgetForecastRepositoryAndFactory.findUnique(this, ForecastFrequency.QUARTERLY.getStartDateFor(date));
        if (forecastIfAny!=null && forecastIfAny.getSubmittedOn()!=null) return String.format("Forecast for %s is submitted already", ForecastFrequency.QUARTERLY.getStartDateFor(date).toString());
        return null;
    }


    @Programmatic
    public ProjectItem findItemForCharge(final Charge charge) {
        return projectItemRepository.findByProjectAndCharge(this, charge);
    }

    @Inject
    ApplicationTenancyRepository applicationTenancyRepository;

    @Inject
    ProjectItemRepository projectItemRepository;

    @Inject
    private ProjectRoleRepository projectRoleRepository;

    @Inject
    ProjectRepository projectRepository;

    @Inject
    WrapperFactory wrapperFactory;

	@Inject
	RepositoryService repositoryService;

	@Inject
    FactoryService factoryService;

	@Inject
    ProjectBudgetRepository projectBudgetRepository;

	@Inject MeService meService;

	@Inject ClockService clockService;

	@Inject
    BudgetForecastRepositoryAndFactory budgetForecastRepositoryAndFactory;
}
