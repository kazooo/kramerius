package cz.incad.kramerius.security.impl.criteria;

import cz.incad.kramerius.ObjectPidsPath;
import cz.incad.kramerius.security.*;
import cz.incad.kramerius.security.impl.criteria.utils.CriteriaDNNTUtils;
import cz.incad.kramerius.utils.solr.SolrUtils;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import static cz.incad.kramerius.security.impl.criteria.utils.CriteriaDNNTUtils.checkContainsCriterium;

public class PDFDNNTFlag extends AbstractCriterium {

    public transient  static final Logger LOGGER = Logger.getLogger(ReadDNNTFlag.class.getName());


    @Override
    public EvaluatingResultState evalute() throws RightCriteriumException {
        String requestedPid = null;
        try {
            requestedPid = this.getEvaluateContext().getRequestedPid();
            if (requestedPid != null && !SpecialObjects.isSpecialObject(requestedPid)) {
                // only if
                String s = SolrUtils.disectDNNTFlag(this.evalContext.getSolrAccess().getSolrDataDocument(requestedPid).getDocumentElement());
                if (s != null && s.equals("true")) {
                    IsActionAllowed rightsResolver = this.getEvaluateContext().getRightsResolver();
                    ObjectPidsPath[] paths = this.getEvaluateContext().getSolrAccess().getPath(requestedPid);
                    for (ObjectPidsPath path : paths) {
                        RightsReturnObject obj = rightsResolver.isActionAllowed(SecuredActions.READ.getFormalName(), requestedPid, null, path);
                        if (CriteriaDNNTUtils.allowedByReadDNNTFlagRight(obj)) return EvaluatingResultState.FALSE;
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE,e.getMessage(),e);
        }

        // not applicable
        return EvaluatingResultState.NOT_APPLICABLE;
    }


    @Override
    public EvaluatingResultState mockEvaluate(DataMockExpectation dataMockExpectation) throws RightCriteriumException {
        return EvaluatingResultState.NOT_APPLICABLE;
    }

    @Override
    public RightCriteriumPriorityHint getPriorityHint() {
        return RightCriteriumPriorityHint.DNNT_EXCLUSIVE_MAX;
    }

    @Override
    public boolean isParamsNecessary() {
        return false;
    }

    @Override
    public SecuredActions[] getApplicableActions() {
        return  new SecuredActions[] {SecuredActions.PDF_RESOURCE};
    }

    @Override
    public boolean isRootLevelCriterum() {
        return true;
    }

    @Override
    public void checkPrecodition(RightsManager manager) throws CriteriaPrecoditionException {
        //checkContainsCriterium(this.evalContext, manager);
    }
}
