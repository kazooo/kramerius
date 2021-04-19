package cz.incad.kramerius.security.impl.criteria.utils;

import cz.incad.kramerius.SolrAccess;
import cz.incad.kramerius.security.*;
import cz.incad.kramerius.security.impl.criteria.*;
import cz.incad.kramerius.utils.solr.SolrUtils;
import org.w3c.dom.Document;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class CriteriaDNNTUtils {


    public static ThreadLocal<RightsReturnObject> currentThreadReturnObject = new ThreadLocal<>();


    public  static Logger LOGGER = Logger.getLogger(CriteriaDNNTUtils.class.getName());



    // check dnnt flag from solr
    public static EvaluatingResultState checkDnnt(RightCriteriumContext ctx) {
        try {
            SolrAccess solrAccess = ctx.getSolrAccess();
            String pid = ctx.getRequestedPid();
            Document doc = solrAccess.getSolrDataDocument(pid);
            String val = SolrUtils.disectDNNTFlag(doc.getDocumentElement());
            return (val !=  null && val.equals("true")) ? EvaluatingResultState.TRUE : EvaluatingResultState.NOT_APPLICABLE;
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE,e.getMessage(),e);
            return EvaluatingResultState.NOT_APPLICABLE;
        }
    }

    // allowed by dnntlabel right
    public static boolean allowedByReadDNNTLabelsRight(RightsReturnObject obj, Object[] givenObjects) {
        if (obj.getRight() != null && obj.getRight().getCriteriumWrapper() != null) {
            if (obj.getRight().getCriteriumWrapper().getRightCriterium().getQName().equals(ReadDNNTLabels.class.getName()) ||
                    obj.getRight().getCriteriumWrapper().getRightCriterium().getQName().equals(ReadDNNTLabelsIPFiltered.class.getName())) {
                Object[] rightObjects = obj.getRight().getCriteriumWrapper().getCriteriumParams().getObjects();
                for (Object go : givenObjects) {
                    for (Object objFromRight :  rightObjects) {
                        if (go.equals(rightObjects)) return true;
                    }
                }
            }
        }
        return false;
    }


    // allowed by dnnt right
    public static boolean allowedByReadDNNTFlagRight(RightsReturnObject obj) {
        if (obj.getRight() != null && obj.getRight().getCriteriumWrapper() != null) {
            if (    obj.getRight().getCriteriumWrapper().getRightCriterium().getQName().equals(ReadDNNTFlag.class.getName()) ||
                    obj.getRight().getCriteriumWrapper().getRightCriterium().getQName().equals(ReadDNNTFlagIPFiltered.class.getName()) ||
                    obj.getRight().getCriteriumWrapper().getRightCriterium().getQName().equals(ReadDNNTLabels.class.getName()) ||
                    obj.getRight().getCriteriumWrapper().getRightCriterium().getQName().equals(ReadDNNTLabelsIPFiltered.class.getName())
                ) {
                return true;
            }
        }
        return false;
    }



//    public static String getReadDNNTLabel(RightsReturnObject obj) {
//        if (obj.getRight() != null && obj.getRight().getCriteriumWrapper() != null) {
//            if (obj.getRight().getCriteriumWrapper().getRightCriterium().getQName().equals(ReadDNNTLabels.class.getName()) ||
//                    obj.getRight().getCriteriumWrapper().getRightCriterium().getQName().equals(ReadDNNTLabelsIPFiltered.class.getName())
//                    ) {
//                return obj.getRight().getCriteriumWrapper().getCriteriumParams().getObjects();
//            }
//        }
//        return null;
//
//    }


    // check if there is
    public static void checkContainsCriteriumPDFDNNT(RightCriteriumContext ctx, RightsManager manager) throws CriteriaPrecoditionException {
        //PDFDNNTFlag.class.getName()
        checkContainsCriterium(ctx, manager, PDFDNNTFlag.class);
    }

    public static void checkContainsCriterium(RightCriteriumContext ctx, RightsManager manager, Class ... clzs) throws CriteriaPrecoditionException {
        String[] pids = new String[] {SpecialObjects.REPOSITORY.getPid()};
        Right[] rights = manager.findRights(pids, SecuredActions.PDF_RESOURCE.getFormalName(), ctx.getUser());
        for (Right r : rights) {
            if (r == null) continue;
            if (r.getCriteriumWrapper() == null) continue;
            RightCriterium rightCriterium = r.getCriteriumWrapper().getRightCriterium();
            String qName = rightCriterium.getQName();
            for (Class clz:clzs) {
                if (qName.equals(clz.getName())) {
                    return;
                }
            }
        }
        List<String> collections = Arrays.stream(clzs).map(s -> s.getName()).collect(Collectors.toList());
        throw new CriteriaPrecoditionException("These flags are not set : "+collections);
    }

    public static void checkContainsCriterium(RightCriteriumContext ctx, RightsManager manager) throws CriteriaPrecoditionException {
        checkContainsCriterium(ctx, manager, PDFDNNTFlag.class);
    }

    public static String getMatchedLabel(Document solrDoc, Object[] rightsParams) {
        List<String> indexedLabels = SolrUtils.disectDNNTLabels(solrDoc.getDocumentElement());
        if (indexedLabels != null) {
            for (Object obj :  rightsParams) {
                String label = obj.toString();
                if (indexedLabels.contains(label)) return label;
            }
        }
        return null;
    }
}
