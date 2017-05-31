package org.estatio.capex.dom.documents;

import javax.annotation.Nullable;

import org.apache.isis.applib.annotation.Mixin;

import org.estatio.capex.dom.documents.incoming.IncomingOrderOrInvoiceViewModel;
import org.estatio.dom.asset.Property;
import org.estatio.dom.invoice.DocumentTypeData;

@Mixin(method = "act")
public class HasDocument_categoriseAsInvoice
        extends HasDocument_categoriseAbstract {

    // workaround for https://issues.apache.org/jira/browse/ISIS-1628
    protected final IncomingOrderOrInvoiceViewModel viewModel;

    public HasDocument_categoriseAsInvoice(final IncomingOrderOrInvoiceViewModel viewModel) {
        super(viewModel, DocumentTypeData.INCOMING_INVOICE);
        this.viewModel = viewModel;
    }

    // workaround for https://issues.apache.org/jira/browse/ISIS-1628
    @Override
    public Object act(
            @Nullable final Property property,
            @Nullable final String comment,
            final boolean goToNext) {
        return super.act(property, comment, goToNext);
    }

    @Override
    public Property default0Act() {
        return super.default0Act();
    }

    @Override
    public boolean default2Act() {
        return super.default2Act();
    }

    @Override
    public boolean hideAct() {
        return super.hideAct();
    }
}
