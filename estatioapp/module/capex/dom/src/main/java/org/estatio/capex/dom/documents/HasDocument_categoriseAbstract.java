package org.estatio.capex.dom.documents;

import java.util.List;

import javax.annotation.Nullable;
import javax.inject.Inject;

import org.apache.isis.applib.annotation.Action;
import org.apache.isis.applib.annotation.ActionLayout;
import org.apache.isis.applib.annotation.SemanticsOf;
import org.apache.isis.applib.services.message.MessageService;

import org.incode.module.document.dom.impl.docs.Document;
import org.incode.module.document.dom.impl.docs.DocumentRepository;

import org.estatio.capex.dom.documents.categorisation.IncomingDocumentCategorisationStateTransition;
import org.estatio.capex.dom.documents.incoming.IncomingOrderOrInvoiceViewModel;
import org.estatio.capex.dom.task.Task;
import org.estatio.capex.dom.task.TaskRepository;
import org.estatio.dom.asset.Property;
import org.estatio.dom.invoice.DocumentTypeData;

public abstract class HasDocument_categoriseAbstract extends DocumentOrHasDocument_categoriseAsAbstract {

    protected final IncomingOrderOrInvoiceViewModel viewModel;

    public HasDocument_categoriseAbstract(
            final IncomingOrderOrInvoiceViewModel viewModel,
            final DocumentTypeData documentTypeData) {
        super(documentTypeData);
        this.viewModel = viewModel;
    }

    @Override
    public Document getDomainObject() {
        return viewModel.getDocument();
    }

    @Action(
            semantics = SemanticsOf.IDEMPOTENT
    )
    @ActionLayout(cssClassFa = "folder-open-o")
    public Object act(
            @Nullable final Property property,
            @Nullable final String comment,
            final boolean goToNext) {
        final Document document = categoriseAndAttachPaperclip(property);

        IncomingDocumentCategorisationStateTransition transition =
                triggerStateTransition(comment);

        if (goToNext){
            Task nextTask = nextTaskElseFrom(transition);
            if(nextTask != null) {
                return nextTask;
            }
            // else fall through to returning the view model for this document
            messageService.informUser("No more tasks");
        }

        return this.viewModelFactory.createFor(document);
    }

    @Override
    public Property default0Act() {
        return super.default0Act();
    }

    public boolean default2Act(){
        return true;
    }

    @Override
    public boolean hideAct() {
        return super.hideAct();
    }

    protected Task nextTaskElseFrom(final IncomingDocumentCategorisationStateTransition transition) {
        Task taskJustCompleted = viewModel.getTask();
        if(taskJustCompleted == null) {
            taskJustCompleted = transition.getTask();
        }
        List<Task> remainingTasks =
                taskRepository.findTasksIncompleteCreatedOnAfter(taskJustCompleted.getCreatedOn());
        return !remainingTasks.isEmpty() ? remainingTasks.get(0) : null;
    }

    @Inject
    MessageService messageService;

    @Inject
    DocumentRepository documentRepository;

    @Inject
    TaskRepository taskRepository;

    @Inject
    HasDocumentAbstract.Factory viewModelFactory;

}
