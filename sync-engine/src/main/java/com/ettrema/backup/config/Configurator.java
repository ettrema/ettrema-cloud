package com.ettrema.backup.config;

import com.bradmcevoy.io.FileUtils;
import com.thoughtworks.xstream.XStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.apache.commons.io.IOUtils;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author brad
 */
public class Configurator {

    private static final Logger log = LoggerFactory.getLogger( Configurator.class );

    private final File fConfigDir;
    private final File fConfigMain;

    public Configurator( File fConfigDir ) {
        this.fConfigDir = fConfigDir;
        this.fConfigMain = new File( fConfigDir, "config.xml" );
    }

    public Config load() {
        FileInputStream fin;
        try {
            fin = new FileInputStream( fConfigMain );
            log.info("opened config file: " + fConfigMain.getAbsolutePath());
        } catch( FileNotFoundException ex ) {
            log.info("not found: " + fConfigMain.getAbsolutePath());
            fConfigDir.mkdirs();
            Config config = new Config();
            config.setConfigurator( this );
            return config;
        }
        Config config;
        try {
            XStream xstream = initXstream();
            config = (Config) xstream.fromXML( fin );
        } finally {
            IOUtils.closeQuietly( fin );
        }
        initParentReferences( config );
        loadData( config );
        if( config.getThrottlePerc() == null ) {
            config.setThrottlePerc( 99 );
        }
        return config;
    }

    private void loadData( Config config ) {
        File fData = new File( fConfigDir, "data.xml" );
//        if( fData.exists() ) {
//            Document doc = getJDomDocument( fData );
        for( Job j : config.getJobs() ) {
            for( Repo r : j.getRepos() ) {
                r.setQueue( new Queue() ); // todo restore queue
            }
            // todo
//                loadData( j );
//            }
//        }
        }
    }

    private void saveData( Config config ) {
        File fData = new File( fConfigDir, "data.xml" );
        // todo
    }

    public org.jdom.Document getJDomDocument( File f ) {
        FileInputStream fin = null;
        try {
            fin = new FileInputStream( f );
            SAXBuilder builder = new SAXBuilder();
            builder.setExpandEntities( false );
            return builder.build( fin );
        } catch( JDOMException ex ) {
            throw new RuntimeException( ex );
        } catch( IOException ex ) {
            throw new RuntimeException( ex );
        } finally {
            FileUtils.close( fin );
        }
    }

    private void initAliases( XStream x ) {
        x.alias( "config", Config.class );
        x.alias( "job", Job.class );
        x.alias( "root", Root.class );
        x.alias( "dir", Dir.class );
        x.alias( "local", LocalRepo.class );
        x.alias( "dav", DavRepo.class );
        x.alias( "queue", Queue.class );
        x.alias( "conList", CopyOnWriteArrayList.class );
    }

    private Config defaultConfig() {
        Config c = new Config();
        c.setJobs( new ArrayList<Job>() );

        List<DavRepo> repoList = Arrays.asList( new DavRepo() );
        List<Root> roots = Arrays.asList( new Root( System.getProperty( "user.home" ), "Documents" ) );

        Job j = new Job( "default", repoList, roots );
        c.getJobs().add( j );

        return c;
    }

    public void save( Config c ) {
        FileOutputStream fout = null;
        try {
            fout = new FileOutputStream( fConfigMain );
        } catch( FileNotFoundException ex ) {
            throw new RuntimeException( fConfigMain.getAbsolutePath(), ex );
        }
        try {
            XStream xstream = initXstream();
            xstream.toXML( c, fout );
            System.out.println("saved to: " + fConfigMain.getAbsolutePath());
        } finally {
            IOUtils.closeQuietly( fout );
        }
    }

    private XStream initXstream() {
        XStream xstream = new XStream();
        initAliases( xstream );
        return xstream;
    }

    private void initParentReferences( Config config ) {
        config.setConfigurator( this );
        for( Job j : config.getJobs() ) {
            j.setConfig( config );
            for( Root r : j.getRoots() ) {
                r.setJob( j );
            }
            for( Repo r : j.getRepos() ) {
                r.setJob( j );
            }
        }
    }
}
