package cz.incad.kramerius.services;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import cz.incad.kramerius.service.MigrateSolrIndexException;
import cz.incad.kramerius.utils.StringUtils;
import cz.incad.kramerius.utils.XMLUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.ws.rs.core.MediaType;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.StringReader;
import java.net.URLEncoder;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class IterationUtils {

    public static final String SELECT_ENDPOINT = "select";
    public static final String SEARCH_ENDPOINT = "search";
    public static final String DEFAULT_SORT_FIELD = "PID asc";

    public static Logger LOGGER = Logger.getLogger(IterationUtils.class.getName());

    private IterationUtils() {}

    /**
     * Iteration by cursor
     * @param client Client
     * @param address Solr address
     * @param masterQuery Master query
     * @param callback Callback
     * @param endCallback End callback
     * @throws ParserConfigurationException
     * @throws MigrateSolrIndexException
     * @throws SAXException
     * @throws IOException
     * @throws InterruptedException
     * @throws BrokenBarrierException
     */
    public static void cursorIteration(Client client,String address, String masterQuery,IterationCallback callback, IterationEndCallback endCallback) throws ParserConfigurationException, MigrateSolrIndexException, SAXException, IOException, InterruptedException, BrokenBarrierException {
        String cursorMark = null;
        String queryCursorMark = null;
        do {
            Element element = pidsCursorQuery(client, address, masterQuery, cursorMark);
            cursorMark = findCursorMark(element);
            queryCursorMark = findQueryCursorMark(element);
            callback.call(element, cursorMark);
        } while((cursorMark != null && queryCursorMark != null) && !cursorMark.equals(queryCursorMark));
        // callback after iteration
        endCallback.end();
    }


    /**
     * Iteration by filter
     * @param client
     * @param address
     * @param masterQuery
     * @param callback
     * @param endCallback
     * @throws MigrateSolrIndexException
     * @throws IOException
     * @throws SAXException
     * @throws ParserConfigurationException
     * @throws BrokenBarrierException
     * @throws InterruptedException
     */
    public static void queryFilterIteration(Client client, String address, String masterQuery,IterationCallback callback, IterationEndCallback endCallback) throws MigrateSolrIndexException, IOException, SAXException, ParserConfigurationException, BrokenBarrierException, InterruptedException {
        String lastPid = null;
        String previousPid = null;
        do {
            Element element = pidsFilterQuery(client, address,masterQuery,  lastPid);
            previousPid = lastPid;
            lastPid = findLastPid(element);
            callback.call(element, lastPid);
        }while(lastPid != null  && !lastPid.equals(previousPid));
        // callback after iteration
        endCallback.end();
    }

    public static void queryPaginationIteration(Client client, String address, String masterQuery,IterationCallback callback, IterationEndCallback endCallback) throws MigrateSolrIndexException, IOException, SAXException, ParserConfigurationException, BrokenBarrierException, InterruptedException {
        int offset = 0;
        int numberOfResult = Integer.MAX_VALUE;
        do {
            Element element =  paginationQuery(client, address,masterQuery,  ""+offset);
            int rows = MigrationUtils.configuredRowsSize();
            if (numberOfResult == Integer.MAX_VALUE) {
                numberOfResult = findNumberOfResults(element);
            }
            callback.call(element, ""+offset);
            offset += rows;
        }while(offset < numberOfResult);
        // callback after iteration
        endCallback.end();
    }


    private static Element pidsFilterQuery(Client client, String url, String mq, String lastPid)
            throws ParserConfigurationException, SAXException, IOException, MigrateSolrIndexException {
        int rows = MigrationUtils.configuredRowsSize();
        String fq = MigrationUtils.filterQuery();
        String fullQuery = null;
        if (StringUtils.isAnyString(fq)) {
            fullQuery = (lastPid!= null ? String.format("&rows=%d&fq=PID:%s", rows, URLEncoder.encode("[\""+lastPid+"\" TO *] AND "+fq, "UTF-8")) : String.format("&rows=%d&fq=%s", rows, URLEncoder.encode(fq,"UTF-8")));
        } else {
            fullQuery = (lastPid!= null ? String.format("&rows=%d&fq=PID:%s", rows, URLEncoder.encode("[\""+lastPid+"\" TO *]", "UTF-8")) : String.format("&rows=%d", rows));
        }

        String query = SELECT_ENDPOINT + "?q="+mq + fullQuery +"&sort=" + URLEncoder.encode(DEFAULT_SORT_FIELD, "UTF-8")+"&fl=PID";
        return executeQuery(client, url, query);
    }

    private static Element paginationQuery(Client client, String url, String mq, String offset) throws MigrateSolrIndexException, IOException, SAXException, ParserConfigurationException {
        int rows = MigrationUtils.configuredRowsSize();
        String fq = MigrationUtils.filterQuery();
        String fullQuery = null;
        if (StringUtils.isAnyString(fq)) {
            fullQuery = String.format("?q=%s&start=%s&rows=%d&fq=%s&fl=PID",mq,offset, rows,URLEncoder.encode(fq,"UTF-8"));
        } else {
            fullQuery = String.format("?q=%s&start=%s&rows=%d&fl=PID",mq,offset, rows);
        }
        String query = SELECT_ENDPOINT + fullQuery;
        return executeQuery(client, url, query);
    }


    public static Element pidsCursorQuery(Client client, String url, String mq,  String cursor)  throws ParserConfigurationException, SAXException, IOException, MigrateSolrIndexException {
        int rows = MigrationUtils.configuredRowsSize();
        String query = SELECT_ENDPOINT + "?q="+mq + (cursor!= null ? String.format("&rows=%d&cursorMark=%s", rows, cursor) : String.format("&rows=%d&cursorMark=*", rows))+"&sort=" + URLEncoder.encode(DEFAULT_SORT_FIELD, "UTF-8")+"&fl=PID";
        return IterationUtils.executeQuery(client, url, query);
    }

    static int findNumberOfResults(Element elm) {
        Element result = XMLUtils.findElement(elm, new XMLUtils.ElementsFilter() {

            @Override
            public boolean acceptElement(Element element) {
                String nodeName = element.getNodeName();
                return nodeName.equals("result");
            }
        });
        //numFound
        String attribute = result.getAttribute("numFound");
        int numfound = Integer.parseInt(attribute);
        return numfound;
    }

    static String findCursorMark(Element elm) {
        Element element = XMLUtils.findElement(elm, new XMLUtils.ElementsFilter() {
            @Override
            public boolean acceptElement(Element element) {
                String nodeName = element.getNodeName();
                boolean nextCursorMark = element.hasAttribute("name") && element.getAttribute("name").equals("nextCursorMark");
                return nodeName.equals("str") && nextCursorMark;
            }
        });
        return element != null ? element.getTextContent() : null;
    }

    static String findQueryCursorMark(Element elm) {
        Element queryParams = XMLUtils.findElement(elm, new XMLUtils.ElementsFilter() {
            @Override
            public boolean acceptElement(Element element) {
                String nodeName = element.getNodeName();
                String paramName = element.getAttribute("name");
                return nodeName.equals("lst") && paramName.equals("params");

            }
        });
        if (queryParams != null) {
            Element element = XMLUtils.findElement(elm, new XMLUtils.ElementsFilter() {
                @Override
                public boolean acceptElement(Element element) {
                    String nodeName = element.getNodeName();
                    boolean nextCursorMark = element.hasAttribute("name") && element.getAttribute("name").equals("cursorMark");
                    return nodeName.equals("str") && nextCursorMark;
                }
            });
            return element != null ? element.getTextContent() : null;
        }
        return null;
    }

    public static String findLastPid(Element elm) {
        Element result = XMLUtils.findElement(elm, new XMLUtils.ElementsFilter() {
            @Override
            public boolean acceptElement(Element element) {
                String nodeName = element.getNodeName();
                return nodeName.equals("result");
            }
        });
        if (result != null) {

            List<Element> elements = XMLUtils.getElements(result, new XMLUtils.ElementsFilter() {
                @Override
                public boolean acceptElement(Element element) {
                    String nodeName = element.getNodeName();
                    return nodeName.equals("doc");
                }
            });

            List<String> pids = elements.stream().map(item->{
                        Element str = XMLUtils.findElement(item, new XMLUtils.ElementsFilter() {
                                    @Override
                                    public boolean acceptElement(Element element) {
                                        return element.getNodeName().equals("str");
                                    }
                                }
                        );
                        return str.getTextContent();
                    }
            ).collect(Collectors.toList());

            return !pids.isEmpty() ? pids.get(pids.size() -1) : null;
        }

        return null;
    }


    public interface IterationCallback {
            public void call(Element results, String iterationToken) throws ParserConfigurationException, MigrateSolrIndexException, SAXException, IOException;
    }

    public interface IterationEndCallback {
        public void end();
    }

    public static Element executeQuery(Client client, String url, String query) throws ParserConfigurationException, SAXException, IOException {
        LOGGER.info(String.format("[" + Thread.currentThread().getName() + "] processing %s", query));
        WebResource r = client.resource(url+(url.endsWith("/") ? "" : "/")+ query);
        String t = r.accept(MediaType.APPLICATION_XML).get(String.class);
        Document parseDocument = XMLUtils.parseDocument(new StringReader(t));
        return parseDocument.getDocumentElement();
    }

}
