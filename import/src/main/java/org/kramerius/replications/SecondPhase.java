/*
 * Copyright (C) 2012 Pavel Stastny
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
package org.kramerius.replications;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.logging.Level;

import javax.xml.ws.soap.SOAPFaultException;

import org.apache.commons.codec.binary.Base64InputStream;
import org.fedora.api.FedoraAPIM;
import org.fedora.api.ObjectFactory;
import org.kramerius.Import;
import org.kramerius.replications.pidlist.PIDsListLexer;
import org.kramerius.replications.pidlist.PIDsListParser;
import org.kramerius.replications.pidlist.PidsListCollect;
import org.xml.sax.XMLFilter;

import antlr.RecognitionException;
import antlr.TokenStreamException;

import cz.incad.kramerius.FedoraAccess;
import cz.incad.kramerius.impl.FedoraAccessImpl;
import cz.incad.kramerius.service.impl.IndexerProcessStarter;
import cz.incad.kramerius.utils.IOUtils;
import cz.incad.kramerius.utils.RESTHelper;
import cz.incad.kramerius.utils.conf.KConfiguration;
import cz.incad.kramerius.utils.pid.LexerException;
import cz.incad.kramerius.utils.pid.PIDParser;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

public class SecondPhase extends AbstractPhase  {

    static java.util.logging.Logger LOGGER = java.util.logging.Logger.getLogger(SecondPhase.class.getName());

    static String DONE_FOLDER_NAME = "DONE";
    static int MAXITEMS=20;
    
    private DONEController controller = null;
    private boolean findPid = false;
    
    @Override
    public void start(String url, String userName, String pswd) throws PhaseException {
        this.findPid = false;
        this.controller = new DONEController(new File(DONE_FOLDER_NAME), MAXITEMS);
        this.processIterate(url, userName, pswd);
    }

    public void pidEmitted(String pid, String url, String userName, String pswd) throws PhaseException {
        LOGGER.info("processing pid '"+pid+"'");
        boolean shouldSkip = (findPid && this.controller.findPid(pid) != null);
        if (!shouldSkip) {
            File foxmlfile = null;
            try {
                InputStream foxmldata = rawFOXMLData(pid, url, userName, pswd);
                foxmlfile = foxmlFile(foxmldata, pid);
                ingest(foxmlfile);
                createFOXMLDone(pid);
            } catch (LexerException e) {
                throw new PhaseException(e);
            } catch (IOException e) {
                throw new PhaseException(e);
            } finally {
                if (foxmlfile != null) foxmlfile.delete();
            }
        } else {
            LOGGER.info("skipping pid '"+pid+"'");
        }
    }


    public void ingest(File foxmlfile) {
        LOGGER.info("ingesting '"+foxmlfile.getAbsolutePath()+"'");
        //Import.ingest(foxmlfile);
    }
    
    public File foxmlFile(InputStream foxmlStream, String pid) throws LexerException, IOException, PhaseException {
        FileOutputStream fos = null;
        File foxml = createFOXMLFile(pid);
        try {
            fos = new FileOutputStream(foxml);
            IOUtils.copyStreams(foxmlStream, fos );
            return foxml;
        } finally {
            IOUtils.tryClose(fos);
        }
    }

    public InputStream rawFOXMLData(String pid, String url, String userName, String pswd) throws PhaseException {
        InputStream is = null;
        FileOutputStream fos = null;
        try {
            is = RESTHelper.inputStream(K4ReplicationProcess.foxmlURL(url, pid), userName, pswd);
            String string = IOUtils.readAsString(is, Charset.forName("UTF-8"), true);
            JSONObject jsonObject = JSONObject.fromObject(string);
            String rawFOXML = jsonObject.getString("raw");
            
            ByteArrayInputStream barr = new ByteArrayInputStream(rawFOXML.getBytes("UTF-8"));
            Base64InputStream input = new Base64InputStream(barr, true, 76,  "|".getBytes());
            
            return input;
        } catch (IOException e) {
            throw new PhaseException(e);
        } finally {
            IOUtils.tryClose(is);
            IOUtils.tryClose(fos);
        }
    }
    
    public File createFOXMLDone(String pid) throws LexerException, IOException, PhaseException {
        PIDParser pidParser = new PIDParser(pid);
        pidParser.objectPid();
        String objectId = pidParser.getObjectId();
        File importDoneFile = new File(controller.getCurrentSubFolder(), objectId+".fo.done");
        if (!importDoneFile.exists()) importDoneFile.createNewFile();
        if (!importDoneFile.exists()) throw new PhaseException("file not exists '"+importDoneFile.getAbsolutePath()+"'");
        return importDoneFile;
        
    }
    
    public File createFOXMLFile(String pid) throws LexerException, IOException, PhaseException {
        PIDParser pidParser = new PIDParser(pid);
        pidParser.objectPid();
        String objectId = pidParser.getObjectId();
        File foxmlFile = new File(objectId+".fo.xml");
        if (!foxmlFile.exists()) foxmlFile.createNewFile();
        if (!foxmlFile.exists()) throw new PhaseException("file not exists '"+foxmlFile.getAbsolutePath()+"'");
        return foxmlFile;
        
    }
    
    

    private void processIterate(String url, String userName, String pswd) throws PhaseException {
        try {
            PIDsListLexer lexer = new PIDsListLexer(new FileReader(getIterateFile()));
            PIDsListParser parser = new PIDsListParser(lexer);
            parser.setPidsListCollect(new Emitter(url, userName, pswd));
            parser.pids();
        } catch (FileNotFoundException e) {
            throw new PhaseException(e);
        } catch (RecognitionException e) {
            throw new PhaseException(e);
        } catch (TokenStreamException e) {
            throw new PhaseException(e);
        }
    }



    @Override
    public void restart(String previousProcessUUID,File previousProcessRoot, boolean phaseCompleted, String url, String userName, String pswd) throws PhaseException {
        try {
            if (!phaseCompleted) {
                this.findPid = true;
                IOUtils.copyFolders(new File(DONE_FOLDER_NAME), new File(previousProcessRoot, DONE_FOLDER_NAME));
                this.controller = new DONEController(new File(DONE_FOLDER_NAME), MAXITEMS);
                processIterate(url, userName, pswd);
            }
        } catch (IOException e) {
            throw new PhaseException(e);
        }
    }
    
    class DONEController {
        
        private File doneRoot;
        private int max;
        private int counter = 0;
        
        public DONEController(File doneRoot, int max) {
            super();
            this.doneRoot = doneRoot;
            this.max = max;
            makeSureRootExists(doneRoot);
        }

        File makeSureRootExists(File doneRoot) {
            if (!doneRoot.exists()) doneRoot.mkdirs();
            return doneRoot;
        }

        
        public File getCurrentSubFolder() {
            File[] sfiles= subfolder(this.doneRoot).listFiles();
            if ((sfiles != null) && (sfiles.length >= this.max)) {
                this.counter += 1;
            }
            return subfolder(this.doneRoot);
        }


        File subfolder(File f) {
            File sub = new File(f,""+this.counter);
            if (!sub.exists()) sub.mkdirs();
            return sub;
        }
        
        public File findPid(String pid) {
            Stack<File> procStack = new Stack<File>();
            LOGGER.info("finding pid '"+pid+"' in '"+this.doneRoot.getAbsolutePath()+"'");
            procStack.push(this.doneRoot);
            while(!procStack.isEmpty()) {
                File poppedFile = procStack.pop();
                if (poppedFile.getName().startsWith(pid)) {
                    LOGGER.info("found file '"+poppedFile.getAbsolutePath()+"'");
                    return poppedFile;
                }
                File[] subfiles = poppedFile.listFiles();
                if(subfiles != null) {
                    for (File f : subfiles) {
                        procStack.push(f);
                    }
                }
            }
            LOGGER.info("no file  starts with '"+pid+"'");
            return null;
        }
        
    }
    
    class Emitter implements PidsListCollect {
        
        private String url,userName,pswd;
        
        
        public Emitter(String url, String userName, String pswd) {
            super();
            this.url = url;
            this.userName = userName;
            this.pswd = pswd;
        }


        @Override
        public void pidEmitted(String pid) {
            try {
                if ((pid.startsWith("'")) || (pid.startsWith("\""))) {
                    pid = pid.substring(1,pid.length()-1);
                }
                SecondPhase.this.pidEmitted(pid, this.url, this.userName, this.pswd);
            } catch (PhaseException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
}
