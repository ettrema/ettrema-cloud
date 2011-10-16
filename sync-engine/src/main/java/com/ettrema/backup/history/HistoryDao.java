package com.ettrema.backup.history;

import com.bradmcevoy.utils.With;
import com.ettrema.backup.config.QueueItem;
import com.ettrema.backup.engine.LocalCrcDaoImpl;
import com.ettrema.backup.utils.EventUtils;
import com.ettrema.db.Table;
import com.ettrema.db.TableCreatorService;
import com.ettrema.db.TableDefinitionSource;
import com.ettrema.db.UseConnection;
import com.ettrema.db.dialects.Dialect;
import com.ettrema.db.types.FieldTypes;
import com.ettrema.event.Event;
import com.ettrema.event.EventManager;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author brad
 */
public class HistoryDao {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger( LocalCrcDaoImpl.class );
    public static final HistoryTable HISTORY = new HistoryTable();
    private final UseConnection useConnection;
    private final EventManager eventManager;

    public HistoryDao( UseConnection useConnection, Dialect dialect, EventManager eventManager ) throws SQLException {
        this.useConnection = useConnection;
        this.eventManager = eventManager;

        TableDefinitionSource defs = new TableDefinitionSource() {

            @Override
            public List<? extends Table> getTableDefinitions() {
                return Arrays.asList( HISTORY );
            }

            @Override
            public void onCreate(Table t, Connection con) {
                
            }
        };
        final TableCreatorService creatorService = new TableCreatorService( null, Arrays.asList( defs ), dialect );

        useConnection.use( new With<Connection, Object>() {

            @Override
            public Object use( Connection con ) throws Exception {
//                con.createStatement().execute( "DROP TABLE HISTORY");
                creatorService.processTableDefinitions( con );
                return null;
            }
        } );
    }

    public void loadHistory( final ResultCollector collector ) {
        final String selectSql = HISTORY.getSelect() + " ORDER BY " + HISTORY.date.getName() + " DESC";
        useConnection.use( new With<Connection, Object>() {

            @Override
            public Object use( Connection con ) throws Exception {
                PreparedStatement stmt = con.prepareStatement( selectSql );
                ResultSet rs = null;
                rs = stmt.executeQuery();
                while( rs.next() ) {
                    String path = HISTORY.path.get( rs );
                    Long bytes = HISTORY.numBytes.get( rs );
                    String repo = HISTORY.repo.get( rs );
                    String action = HISTORY.action.get( rs );
                    String notes = HISTORY.notes.get( rs );
                    String status = HISTORY.status.get( rs );
                    Timestamp date = HISTORY.date.get( rs );
                    collector.onResult(path, bytes, repo, action, notes, status, date);
                }
                UseConnection.close( rs );
                UseConnection.close( stmt );
                return null;
            }
        } );
    }

    public interface ResultCollector {
        /**
         * Called for each result in the resultset
         *
         * @param path
         * @param bytes
         * @param repo
         * @param action
         * @param notes
         * @param status
         * @param date
         */
        public void onResult( String path, Long bytes, String repo, String action, String notes, String status, Timestamp date );
        
    }

    public void success( QueueItem item ) {
        log.trace( "success: " + item );
        insertHistory( item.getFile(), item.getBytesToUpload(), item.getRepo().getDescription(), item.getActionDescription(), null, "success" );
    }

    public void failed( QueueItem item ) {
        log.trace( "failed: " + item );
        insertHistory( item.getFile(), 0, item.getRepo().getDescription(), item.getActionDescription(), item.getNotes(), "failed" );
    }

    private void insertHistory( final File localFile, final long numBytes, final String repo, final String action, final String notes, final String status ) {
        final String insertSql = HISTORY.getInsert();

        useConnection.use( new With<Connection, Object>() {

            @Override
            public Object use( Connection con ) throws Exception {
                PreparedStatement stmt = con.prepareStatement( insertSql );
                stmt.setString( 1, localFile.getAbsolutePath() );
                stmt.setLong( 2, numBytes );
                stmt.setString( 3, repo );
                stmt.setString( 4, action );
                if( notes != null ) {
                    stmt.setString( 5, notes );
                } else {
                    stmt.setNull( 5, java.sql.Types.VARCHAR );
                }
                stmt.setString( 6, status );
                stmt.setTimestamp( 7, new java.sql.Timestamp( System.currentTimeMillis() ) );

                stmt.execute();
                UseConnection.close( stmt );
                return null;
            }
        } );
        EventUtils.fireQuietly( eventManager, new NewHistoryItemEvent( localFile, numBytes, repo, action, notes, status) );
    }

    public static class HistoryTable extends com.ettrema.db.Table {

        public final Field<String> path = add( "path", FieldTypes.CHARACTER_VARYING, false );
        public final Field<Long> numBytes = add( "num_bytes", FieldTypes.LONG, false ); // the amount of data transferred
        public final Field<String> repo = add( "repo", FieldTypes.character( 250 ), false ); // the repo which was backed up to
        public final Field<String> action = add( "action", FieldTypes.character( 50 ), false ); // the repo which was backed up to
        public final Field<String> notes = add( "notes", FieldTypes.CHARACTER_VARYING, true ); // any notes resulting from the operation
        public final Field<String> status = add( "status", FieldTypes.character( 50 ), false ); // the result of the operation
        public final Field<Timestamp> date = add( "date_backed_up", FieldTypes.TIMESTAMP, false ); // the date/time which the file was backed up, or that it was first discoverd assert having been backed up;

        public HistoryTable() {
            super( "history" );
        }
    }

    public class NewHistoryItemEvent implements Event {
        private final File localFile;
        private final long numBytes;
        private final String repo;
        private final String action;
        private final String notes;
        private final String status;

        public NewHistoryItemEvent( File localFile, long numBytes, String repo, String action, String notes, String status ) {
            this.localFile = localFile;
            this.numBytes = numBytes;
            this.repo = repo;
            this.action = action;
            this.notes = notes;
            this.status = status;
        }

        /**
         * @return the localFile
         */
        public File getLocalFile() {
            return localFile;
        }

        /**
         * @return the numBytes
         */
        public long getNumBytes() {
            return numBytes;
        }

        /**
         * @return the repo
         */
        public String getRepo() {
            return repo;
        }

        /**
         * @return the action
         */
        public String getAction() {
            return action;
        }

        /**
         * @return the notes
         */
        public String getNotes() {
            return notes;
        }

        /**
         * @return the status
         */
        public String getStatus() {
            return status;
        }

    }
}
