package com.ettrema.backup.engine;

import com.bradmcevoy.utils.With;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author brad
 */
public class StateTokenDaoImpl {

	private static final Logger log = LoggerFactory.getLogger(StateTokenDaoImpl.class);
	public static final StateTokenTable TOKENS = new StateTokenTable();
	private final UseConnection useConnection;
	private final Dialect dialect;

	public StateTokenDaoImpl(UseConnection useConnection, Dialect dialect) {
		this.useConnection = useConnection;
		this.dialect = dialect;
		TableDefinitionSource defs = new TableDefinitionSource() {

			@Override
			public List<? extends Table> getTableDefinitions() {
				return Arrays.asList(TOKENS);
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

	public List<StateToken> findForFolder(final File parent) {
		final String sql = TOKENS.getSelect() + " WHERE "
				+ TOKENS.parentPath.getName() + " = ? ";
		List<StateToken> list = useConnection.use(new With<Connection, List<StateToken>>() {

			@Override
			public List<StateToken> use(Connection con) throws Exception {
				PreparedStatement stmt = con.prepareStatement(sql);
				stmt.setString(1, parent.getAbsolutePath());
				ResultSet rs = stmt.executeQuery();
				try {
					List<StateToken> list = new ArrayList<StateToken>();
					while (rs.next()) {
						appendStateToken(rs, list);
					}
					return list;
				} finally {
					UseConnection.close(rs);
					UseConnection.close(stmt);
				}
			}
		});
		Collections.sort(list);
		return list;
	}

	private void appendStateToken(ResultSet rs, List<StateToken> list) throws SQLException {
		StateToken token = buildStateToken(rs);
		list.add(token);
	}

	private StateToken buildStateToken(ResultSet rs) throws SQLException {
		String path = TOKENS.parentPath.get(rs) + File.separator + TOKENS.name.get(rs);
		StateToken token = new StateToken(path);
		token.backedupCrc = TOKENS.backedupCrc.get(rs);
		token.backedupTime = TOKENS.backedupDate.get(rs);
		token.currentCrc = TOKENS.currentCrc.get(rs);
		token.currentTime = TOKENS.currentModDate.get(rs);
		return token;
	}

	public void saveOrUpdate(final StateToken token) {
		final String deleteSql = TOKENS.getDeleteBy(TOKENS.parentPath) + " AND " + TOKENS.name.getName() + " = ?";

		final String insertSql = TOKENS.getInsert();

		final File f = new File(token.filePath);
		useConnection.use(new With<Connection, Object>() {

			@Override
			public Object use(Connection con) throws Exception {
				PreparedStatement stmt = con.prepareStatement(deleteSql);
				stmt.setString(1, f.getParentFile().getAbsolutePath());
				stmt.setString(1, f.getName());
				stmt.execute();
				UseConnection.close(stmt);

				stmt = con.prepareStatement(insertSql);
				TOKENS.parentPath.set(stmt, 1, f.getParentFile().getAbsolutePath());
				TOKENS.name.set(stmt, 2, f.getName());
				TOKENS.currentCrc.set(stmt, 3, token.currentCrc);
				TOKENS.currentModDate.set(stmt, 4, token.currentTime);
				TOKENS.backedupCrc.set(stmt, 5, token.backedupCrc);
				TOKENS.backedupDate.set(stmt, 6, token.backedupTime);
				stmt.execute();
				UseConnection.close(stmt);

				return null;
			}
		});

	}

	public StateToken get(final File f) {
		final String sql = TOKENS.getSelect() + " WHERE "
				+ TOKENS.parentPath.getName() + " = ? AND " + TOKENS.name.getName() + " = ?";
		StateToken token = useConnection.use(new With<Connection, StateToken>() {

			@Override
			public StateToken use(Connection con) throws Exception {
				PreparedStatement stmt = con.prepareStatement(sql);
				stmt.setString(1, f.getParentFile().getAbsolutePath());
				stmt.setString(2, f.getName());
				ResultSet rs = stmt.executeQuery();
				try {
					while (rs.next()) {
						return buildStateToken(rs);
					}
					return null;
				} finally {
					UseConnection.close(rs);
					UseConnection.close(stmt);
				}
			}
		});
		return token;
	}

	public void softDelete(StateToken token) {
		final String updateSql = "UPDATE " + TOKENS.tableName + " SET " + TOKENS.currentCrc.getName() + " = ? WHERE "
				+ TOKENS.parentPath.getName() + " = ? AND " + TOKENS.name.getName() + " = ?";
		final File f = new File(token.filePath);
		useConnection.use(new With<Connection, Object>() {

			@Override
			public Object use(Connection con) throws Exception {
				PreparedStatement stmt = con.prepareStatement(updateSql);
				TOKENS.currentCrc.set(stmt,1,null);
				stmt.setString(2, f.getParentFile().getAbsolutePath());
				stmt.setString(3, f.getName());
				
				stmt.execute();
				UseConnection.close(stmt);
				return null;
			}
		});		
	}

	public void delete(StateToken token) {
		final String deleteSql = TOKENS.getDeleteBy(TOKENS.parentPath) + " AND " + TOKENS.name.getName() + " = ?";

		final File f = new File(token.filePath);
		useConnection.use(new With<Connection, Object>() {

			@Override
			public Object use(Connection con) throws Exception {
				PreparedStatement stmt = con.prepareStatement(deleteSql);
				stmt.setString(1, f.getParentFile().getAbsolutePath());
				stmt.setString(2, f.getName());
				stmt.execute();
				UseConnection.close(stmt);
				return null;
			}
		});
	}

	public static class StateTokenTable extends com.ettrema.db.Table {

		public final Field parentPath = add("parentPath", FieldTypes.CHARACTER_VARYING, false);
		public final Field<String> name = add("name", FieldTypes.CHARACTER_VARYING, false);
		public final Field<Long> currentCrc = add("currentCrc", FieldTypes.LONG, false); // the last backed up crc of this local file
		public final Field<Long> currentModDate = add("currentModDate", FieldTypes.LONG, false); // the date/time which the file was backed up, or that it was first discoverd assert having been backed up;
		public final Field<Long> backedupCrc = add("backedupCrc", FieldTypes.LONG, false); // the last backed up crc of this local file
		public final Field<Long> backedupDate = add("backedupDate", FieldTypes.LONG, false); // the date/time which the file was backed up, or that it was first discoverd assert having been backed up;

		public StateTokenTable() {
			super("stateToken");
		}
	}
}
