
package cz.incad.kramerius.client.tools;

import cz.incad.kramerius.utils.UTFSort;
import java.io.IOException;

/**
 *
 * @author alberto
 */
public class UTFSortTool {
    
    public String translate(String s) throws IOException{
        UTFSort utfSort = new UTFSort();
        return utfSort.translate(s);
    }
}
