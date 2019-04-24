package org.estatio.module.capex.contributions;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import javax.inject.Inject;

import org.apache.isis.applib.annotation.Action;
import org.apache.isis.applib.annotation.ActionLayout;
import org.apache.isis.applib.annotation.Contributed;
import org.apache.isis.applib.annotation.Mixin;
import org.apache.isis.applib.annotation.SemanticsOf;

import org.estatio.module.capex.dom.invoice.IncomingInvoice;
import org.estatio.module.capex.dom.invoice.IncomingInvoiceRepository;
import org.estatio.module.capex.dom.invoice.approval.IncomingInvoiceApprovalState;
import org.estatio.module.party.dom.Party;

/**
 * This cannot be inlined (needs to be a mixin) because Party does not know about invoices.
 */
@Mixin(method = "coll")
public class Party_invoicesFrom {

    private final Party seller;

    public Party_invoicesFrom(final Party seller) {
        this.seller = seller;
    }

    @Action(semantics = SemanticsOf.SAFE)
    @ActionLayout(contributed = Contributed.AS_ASSOCIATION)
    public List<IncomingInvoice> coll() {
        EnumSet<IncomingInvoiceApprovalState> allStatesExceptDiscarded = EnumSet.complementOf(EnumSet.of(IncomingInvoiceApprovalState.DISCARDED));
        return incomingInvoiceRepository.findBySellerAndApprovalStates(seller, new ArrayList<>(allStatesExceptDiscarded));
    }

    @Inject
    IncomingInvoiceRepository incomingInvoiceRepository;
}
