package com.ettrema.backup.engine;

import com.bradmcevoy.utils.With;
import com.ettrema.backup.config.Repo;
import com.ettrema.db.Table;
import com.ettrema.db.TableCreatorService;
import com.ettrema.db.TableDefinitionSource;
import com.ettrema.db.UseConnection;
import com.ettrema.db.dialects.Dialect;
import com.ettrema.db.types.FieldTypes;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author brad
 */
public class LocalCrcDaoImpl {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(LocalCrcDaoImpl.class);
    public static final Version2Table VERSION = new Version2Table();
    public static final CrcCacheTable CRC_CACHE = new CrcCacheTable();
    private final CrcCalculator crcCalculator;
    private final UseConnection useConnection;
    private File cachedDir;
    private Map<String, DateAndLong> cachedEntries;

    public LocalCrcDaoImpl(UseConnection useConnection, Dialect dialect, CrcCalculator crcCalculator) throws SQLException {
        this.crcCalculator = crcCalculator;
        this.useConnection = useConnection;

        TableDefinitionSource defs = new TableDefinitionSource() {

            @Override
            public List<? extends Table> getTableDefinitions() {
                return Arrays.asList(VERSION, CRC_CACHE);
            }

            @Override
            public void onCreate(Table t, Connection con) {

            }
        };
        final TableCreatorService creatorService = new TableCreatorService(null, Arrays.asList(defs), dialect);

        useConnection.use(new With<Connection, Object>() {

            @Override
            public Object use(Connection con) throws Exception {
                creatorService.processTableDefinitions(con);
                return null;
            }
        });

    }

    /**
     * Check the current table for a cached CRC. If none present, or is out of date
     * calculate a new CRC and persist it
     *
     * @param localFile
     * @return
     */
    public long getLocalCurrentCrc(final File localFile) {
        final Timestamp modDate = new Timestamp(localFile.lastModified());
        Long crc = getCachedCrc(localFile, modDate);

        if (crc == null) {
            crc = crcCalculator.getLocalCrc(localFile);
            setCachedCrc(localFile, modDate, crc);
        }
        return crc;
    }

    /**
     * Look in the versions table for an entry of this file backed up to
     * the given repo.
     *
     * If none found return null
     *
     * @param localFile
     * @param repo
     * @return
     */
    public synchronized DateAndLong getLocalBackedupCrc(final File localFile, final Repo repo) {
        if (cachedDir == null || !localFile.getParent().equals(cachedDir.getAbsolutePath())) {
            cacheCrcDir(localFile.getParentFile(), repo);
        }
        return cachedEntries.get(localFile.getName());
    }

    private void cacheCrcDir(final File parent, final Repo repo) {
        final long tm = System.currentTimeMillis();
        final String sql = VERSION.getSelect() + " WHERE "
                + VERSION.parent + " = ? "
                + "AND " + VERSION.repo + " = ?";
        this.cachedDir = parent;
        this.cachedEntries = useConnection.use(new With<Connection, Map<String, DateAndLong>>() {

            @Override
            public Map<String, DateAndLong> use(Connection con) throws Exception {
                Map<String, DateAndLong> map = new ConcurrentHashMap<String, DateAndLong>();
                PreparedStatement stmt = con.prepareStatement(sql);
                stmt.setString(1, parent.getAbsolutePath());
                stmt.setString(2, repo.getDescription());
                ResultSet rs = stmt.executeQuery();
                try {
                    while (rs.next()) {
                        long crc = rs.getLong(VERSION.crc.getName());
                        Date date = rs.getDate(VERSION.date.getName());
                        DateAndLong dl = new DateAndLong(date, crc);
                        String name = rs.getString(VERSION.name.getName());
                        map.put(name, dl);
                    }                    
                    return map;
                } finally {
                    UseConnection.close(rs);
                    UseConnection.close(stmt);
                }
            }
        });
    }

    /**
     * Called after a file has been backed up, or we've found that a local file
     * has already been backed up and is identical
     *
     * @param localFile
     * @param repo
     * @param crc
     */
    public synchronized void setLocalBackedupCrc(final File localFile, final Repo repo, final long crc) {
        final String deleteSql = "DELETE FROM " + VERSION.tableName + " WHERE "
                + VERSION.parent.getName() + " = ? AND "
                + VERSION.name.getName() + " = ? AND "
                + VERSION.repo.getName() + " = ?";

        final String insertSql = VERSION.getInsert();

        useConnection.use(new With<Connection, Object>() {

            @Override
            public Object use(Connection con) throws Exception {
                PreparedStatement stmt = con.prepareStatement(deleteSql);
                stmt.setString(1, localFile.getParent());
                stmt.setString(2, localFile.getName());
                stmt.setString(3, repo.getDescription());
                stmt.execute();
                UseConnection.close(stmt);

                stmt = con.prepareStatement(insertSql);
                stmt.setString(1, localFile.getParent());
                stmt.setString(2, localFile.getName());
                stmt.setLong(3, crc);
                stmt.setString(4, repo.getDescription());
                stmt.setTimestamp(5, new Timestamp(System.currentTimeMillis()));
                stmt.execute();
                UseConnection.close(stmt);
				
				log.trace("Set local backed up crc: " + crc + " for: " + localFile.getAbsolutePath());

                return null;
            }
        });
		
		cachedEntries = null; // flush cached entries
		cachedDir = null;
    }

    private void insertVersion(Connection con, File localFile, long crc, String repo, Timestamp date) throws SQLException {
        final String insertSql = VERSION.getInsert();
        PreparedStatement stmt = con.prepareStatement(insertSql);
        stmt.setString(1, localFile.getParent());
        stmt.setString(2, localFile.getName());
        stmt.setLong(3, crc);
        stmt.setString(4, repo);
        stmt.setTimestamp(5, date);
        stmt.execute();
        UseConnection.close(stmt);
    }

    private Long getCachedCrc(final File localFile, final Timestamp modDate) {
        long tm = System.currentTimeMillis();
        if (log.isTraceEnabled()) {
            log.trace("getCachedCrc: " + localFile.getAbsolutePath());
        }
        final String sql = CRC_CACHE.getSelect() + " WHERE "
                + CRC_CACHE.date.getName() + " = ? "
                + "AND " + CRC_CACHE.path.getName() + " = ?";
        Long crc = useConnection.use(new With<Connection, Long>() {

            @Override
            public Long use(Connection con) throws Exception {
                PreparedStatement stmt = con.prepareStatement(sql);
                stmt.setTimestamp(1, modDate);
                stmt.setString(2, localFile.getAbsolutePath());
                ResultSet rs = stmt.executeQuery();
                try {
                    if (rs.next()) {
                        long crc = rs.getLong(CRC_CACHE.crc.getName());
                        return crc;
                    } else {
                        return null;
                    }
                } finally {
                    UseConnection.close(rs);
                    UseConnection.close(stmt);
                }
            }
        });
        if (log.isTraceEnabled()) {
            log.trace("completed: " + (System.currentTimeMillis() - tm) + "ms");
        }
        return crc;
    }

    private void setCachedCrc(final File localFile, final Timestamp modDate, final Long crc) {
        final String deleteSql = CRC_CACHE.getDeleteBy(CRC_CACHE.path);

        final String insertSql = CRC_CACHE.getInsert();
        log.trace("insert: " + insertSql);

        useConnection.use(new With<Connection, Object>() {

            @Override
            public Object use(Connection con) throws Exception {
                PreparedStatement stmt = con.prepareStatement(deleteSql);
                stmt.setString(1, localFile.getAbsolutePath());
                stmt.execute();
                UseConnection.close(stmt);

                stmt = con.prepareStatement(insertSql);
                stmt.setString(1, localFile.getAbsolutePath());
                stmt.setLong(2, crc);
                stmt.setTimestamp(3, modDate);
                stmt.execute();
                UseConnection.close(stmt);

                return null;
            }
        });

    }

    public static class Version2Table extends com.ettrema.db.Table {

        public final Field parent = add("parent", FieldTypes.CHARACTER_VARYING, false);
        public final Field name = add("name", FieldTypes.CHARACTER_VARYING, false);
        public final Field crc = add("crc", FieldTypes.LONG, false); // the last backed up crc of this local file
        public final Field repo = add("repo", FieldTypes.CHARACTER_VARYING, false); // the repo which was backed up to
        public final Field date = add("date_backed_up", FieldTypes.TIMESTAMP, false); // the date/time which the file was backed up, or that it was first discoverd assert having been backed up;

        public Version2Table() {
            super("versions2");
        }
    }

    public static class VersionTable extends com.ettrema.db.Table {

        public final Field<String> path = add("path", FieldTypes.CHARACTER_VARYING, false);
        public final Field<Long> crc = add("crc", FieldTypes.LONG, false); // the last backed up crc of this local file
        public final Field<String> repo = add("repo", FieldTypes.CHARACTER_VARYING, false); // the repo which was backed up to
        public final Field<Date> date = add("date_backed_up", FieldTypes.TIMESTAMP, false); // the date/time which the file was backed up, or that it was first discoverd assert having been backed up;

        public VersionTable() {
            super("versions");
        }
    }

    public static class CrcCacheTable extends com.ettrema.db.Table {

        public final Field path = add("path", FieldTypes.CHARACTER_VARYING, false);
        public final Field crc = add("crc", FieldTypes.LONG, false); // the last backed up crc of this local file
        public final Field date = add("date_modified", FieldTypes.TIMESTAMP, false); // the date/time which the file was backed up, or that it was first discoverd assert having been backed up;

        public CrcCacheTable() {
            super("crc_cache");
        }
    }
}
