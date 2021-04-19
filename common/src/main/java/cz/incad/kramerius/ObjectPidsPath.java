/*
 * Copyright (C) 2010 Pavel Stastny
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package cz.incad.kramerius;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import cz.incad.kramerius.utils.StringUtils;
import cz.incad.kramerius.utils.pid.LexerException;
import cz.incad.kramerius.utils.pid.PIDParser;
import cz.incad.kramerius.virtualcollections.*;
import org.json.JSONArray;
import org.json.JSONObject;

import cz.incad.kramerius.security.SpecialObjects;
import cz.incad.kramerius.utils.IOUtils;
import cz.incad.kramerius.utils.RESTHelper;
import cz.incad.kramerius.utils.conf.KConfiguration;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Represents objects path
 * 
 * @author pavels
 */
public class ObjectPidsPath extends AbstractObjectPath {

    public static final Logger LOGGER = Logger.getLogger(ObjectPidsPath.class.getName());
    
    /** REPOSITORY path constant */
    public static ObjectPidsPath REPOSITORY_PATH = new ObjectPidsPath(SpecialObjects.REPOSITORY.getPid());

    public ObjectPidsPath(String... pathFromRootToLeaf) {
        super(pathFromRootToLeaf);
    }

    public ObjectPidsPath(List<String> pathFromRootToLeaf) {
        super(pathFromRootToLeaf.toArray(new String[pathFromRootToLeaf.size()]));
    }
    
    @Override
    public ObjectPidsPath cutHead(int indexFrom) {
        return new ObjectPidsPath(super.cutHeadInternal(indexFrom));
    }

    @Override
    public ObjectPidsPath cutTail(int indexFrom) {
        return new ObjectPidsPath(super.cutTailInternal(indexFrom));
    }

    @Override
    public ObjectPidsPath injectRepository() {
        if (isEmptyPath())
            return REPOSITORY_PATH;
        if (!this.pathFromRootToLeaf[0].equals(SpecialObjects.REPOSITORY.getPid())) {
            String[] args = new String[this.pathFromRootToLeaf.length + 1];
            args[0] = SpecialObjects.REPOSITORY.getPid();
            System.arraycopy(this.pathFromRootToLeaf, 0, args, 1, this.pathFromRootToLeaf.length);
            return new ObjectPidsPath(args);
        } else
            return this;
    }
    
    // support collections
    
    public ObjectPidsPath injectCollections(CollectionsManager col, FedoraAccess fedoraAccess) throws CollectionException {

        try {
            if (this.isEmptyPath()) return this;
            for (String pid : pathFromRootToLeaf) {
                if (CollectionPidUtils.isCollectionPid(pid)) {
                    // already enhanced
                    return this;
                }
            }
            String[] pathFromRoot = this.getPathFromRootToLeaf();
            Set<String> processingCollection = new HashSet<>();
            Map<String,List<String>> m = new HashMap<>();
            for (String pid : pathFromRoot) {
                if (SpecialObjects.REPOSITORY.getPid().equals(pid)) continue;

                // wheather given pid points to stream or page
                PIDParser parser = new PIDParser(pid);
                parser.objectPid();
                if (parser.isDatastreamPid() || parser.isPagePid()) continue;

                Document relsExt = fedoraAccess.getRelsExt(pid);
                List<Element> collections = CollectionUtils.findCollectionsElements(relsExt);
                List<String> pids = collections.stream().map(colElm -> {
                    String collectionPid = colElm.getAttributeNS(FedoraNamespaces.RDF_NAMESPACE_URI,
                            "resource");
                    if (StringUtils.isAnyString(collectionPid)) {
                        try {
                            PIDParser pidParser = new PIDParser(collectionPid);
                            if (collectionPid.startsWith(PIDParser.INFO_FEDORA_PREFIX)) {
                                pidParser.disseminationURI();
                            } else {
                                pidParser.objectPid();
                            }
                            return pidParser.getObjectPid();
                        } catch (LexerException e) {
                            LOGGER.severe("cannot parse collection pid " + collectionPid);
                            LOGGER.log(Level.SEVERE, e.getMessage(), e);
                            return "";
                        }
                    } else return "";
                }).filter(item -> { return item.length() > 0; }).collect(Collectors.toList());

                m.put(pid, new ArrayList<String>());
                pids.stream().forEach(val->{
                    if (!m.containsKey(pid)) {
                    }
                    if (!processingCollection.contains(val)) {
                        m.get(pid).add(val);
                        processingCollection.add(val);
                    }
                });
            }

            List<String> newVals = new ArrayList<String>();
            for (String p : pathFromRoot) {
                List<String> list = m.get(p);
                if (list != null) {
                    for (String vc : list) {
                        newVals.add(vc);
                    }
                }
                newVals.add(p);
            }
            return new ObjectPidsPath(newVals);
        } catch (LexerException e) {
            throw new CollectionException(e);
        } catch (IOException e) {
            throw new CollectionException(e);

        }
    }

    
    
    @Override
    public ObjectPidsPath replace(String src, String dest) {
        String[] newpath = new String[this.pathFromRootToLeaf.length];
        for (int i = 0; i < newpath.length; i++) {
            String atom = this.pathFromRootToLeaf[i];
            if (atom.equals(src)) {
                atom = dest;
            }
            newpath[i] = atom;
        }
        return new ObjectPidsPath(newpath);
    }

    @Override
    public ObjectPidsPath injectObjectBetween(String injectingObject, Between between) {
        if (between.getAfter() != null && between.getBefore() != null) {
            int bindex = Arrays.asList(this.pathFromRootToLeaf).indexOf(between.getBefore());
            int aindex = Arrays.asList(this.pathFromRootToLeaf).indexOf(between.getAfter());
            if (Math.abs(bindex - aindex) == 1) {
                return new ObjectPidsPath(
                        injectInternal(injectingObject, Math.min(bindex, aindex), Math.max(bindex, aindex)));
            } else
                throw new IllegalArgumentException("ambiguous  injecting");
        } else {
            // the end || the beginning
            String object = between.getAfter() != null ? between.getAfter() : between.getBefore();
            if (this.pathFromRootToLeaf[0].equals(object)) {
                String[] newpath = new String[this.pathFromRootToLeaf.length + 1];
                newpath[0] = injectingObject;
                System.arraycopy(this.pathFromRootToLeaf, 0, newpath, 1, this.pathFromRootToLeaf.length);
                return new ObjectPidsPath(newpath);
            } else if (this.pathFromRootToLeaf[this.pathFromRootToLeaf.length - 1].equals(object)) {
                String[] newpath = new String[this.pathFromRootToLeaf.length + 1];
                System.arraycopy(this.pathFromRootToLeaf, 0, newpath, 0, this.pathFromRootToLeaf.length);
                newpath[newpath.length - 1] = injectingObject;
                return new ObjectPidsPath(newpath);
            } else
                throw new IllegalArgumentException("ambiguous  injecting");
        }
    }

}
