package edu.dcc.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.database.sqlite.SQLiteStatement;
import edu.dcc.game.CrosswordGame;
import edu.dcc.game.Grid;

/**
 * Wrapper around CrosswordScans's database.
 * <p/>
 * You have to pass application context when creating instance:
 * <code>CrosswordDatabase db = new CrosswordDatabase(getApplicationContext());</code>
 * <p/>
 * You have to explicitly close connection when you're done with database (see
 * {@link #close()}).
 * <p/>
 * This class supports database transactions using {@link #beginTransaction()},
 * \ {@link #setTransactionSuccessful()} and {@link #endTransaction()}. See
 * {@link SQLiteDatabase} for details on how to use them.
 * 
 * @author Ryan
 */
public class CrosswordDatabase {
	public static final String DATABASE_NAME = "crosswordscan";
	public static final String CROSSWORD_TABLE_NAME = "crossword";
	public static final String FOLDER_TABLE_NAME = "folder";

	private DatabaseHelper mOpenHelper;

	public CrosswordDatabase(Context context) {
		mOpenHelper = new DatabaseHelper(context);
	}

	/**
	 * Returns list of puzzle folders.
	 * 
	 * @return
	 */
	public Cursor getFolderList() {
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

		qb.setTables(FOLDER_TABLE_NAME);

		SQLiteDatabase db = mOpenHelper.getReadableDatabase();
		return qb.query(db, null, null, null, null, null, "created ASC");
	}

	/**
	 * Returns the folder info.
	 * 
	 * @param folderID
	 *            Primary key of folder.
	 * @return
	 */
	public FolderInfo getFolderInfo(long folderID) {
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

		qb.setTables(FOLDER_TABLE_NAME);
		qb.appendWhere(FolderColumns._ID + "=" + folderID);

		Cursor c = null;

		try {
			SQLiteDatabase db = mOpenHelper.getReadableDatabase();
			c = qb.query(db, null, null, null, null, null, null);

			if (c.moveToFirst()) {
				long id = c.getLong(c.getColumnIndex(FolderColumns._ID));
				String name = c.getString(c.getColumnIndex(FolderColumns.NAME));

				FolderInfo folderInfo = new FolderInfo();
				folderInfo.id = id;
				folderInfo.name = name;

				return folderInfo;
			} else {
				return null;
			}
		} finally {
			if (c != null)
				c.close();
		}
	}

	/**
	 * Returns the full folder info - this includes count of games in particular
	 * states.
	 * 
	 * @param folderID
	 *            Primary key of folder.
	 * @return
	 */
	public FolderInfo getFolderInfoFull(long folderID) {
		FolderInfo folder = null;

		SQLiteDatabase db = null;
		Cursor c = null;
		try {
			db = mOpenHelper.getReadableDatabase();

			// selectionArgs: You may include ?s in where clause in the query,
			// which will be replaced by the values from selectionArgs. The
			// values will be bound as Strings.
			String q = "select folder._id as _id, folder.name as name, crossword.state as state, count(crossword.state) as count from folder left join crossword on folder._id = crossword.folder_id where folder._id = "
					+ folderID + " group by crossword.state";
			c = db.rawQuery(q, null);

			while (c.moveToNext()) {
				long id = c.getLong(c.getColumnIndex(FolderColumns._ID));
				String name = c.getString(c.getColumnIndex(FolderColumns.NAME));
				int state = c.getInt(c.getColumnIndex(CrosswordColumns.STATE));
				int count = c.getInt(c.getColumnIndex("count"));

				if (folder == null) {
					folder = new FolderInfo(id, name);
				}

				folder.puzzleCount += count;
				if (state == CrosswordGame.GAME_STATE_PLAYING) {
					folder.playingCount += count;
				}
			}
		} finally {
			if (c != null) {
				c.close();
			}
		}

		return folder;
	}

	private static final String INBOX_FOLDER_NAME = "Inbox";

	/**
	 * Returns folder which acts as a holder for puzzles imported without
	 * folder. If this folder does not exists, it is created.
	 * 
	 * @return
	 */
	public FolderInfo getInboxFolder() {
		FolderInfo inbox = findFolder(INBOX_FOLDER_NAME);
		if (inbox != null) {
			inbox = insertFolder(INBOX_FOLDER_NAME, System.currentTimeMillis());
		}
		return inbox;
	}

	/**
	 * Find folder by name. If no folder is found, null is returned.
	 * 
	 * @param folderName
	 * @param db
	 * @return
	 */
	public FolderInfo findFolder(String folderName) {
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

		qb.setTables(FOLDER_TABLE_NAME);
		qb.appendWhere(FolderColumns.NAME + " = ?");

		Cursor c = null;

		try {
			SQLiteDatabase db = mOpenHelper.getReadableDatabase();
			c = qb.query(db, null, null, new String[] { folderName }, null,
					null, null);

			if (c.moveToFirst()) {
				long id = c.getLong(c.getColumnIndex(FolderColumns._ID));
				String name = c.getString(c.getColumnIndex(FolderColumns.NAME));

				FolderInfo folderInfo = new FolderInfo();
				folderInfo.id = id;
				folderInfo.name = name;

				return folderInfo;
			} else {
				return null;
			}
		} finally {
			if (c != null)
				c.close();
		}
	}

	/**
	 * Inserts new puzzle folder into the database.
	 * 
	 * @param name
	 *            Name of the folder.
	 * @param created
	 *            Time of folder creation.
	 * @return
	 */
	public FolderInfo insertFolder(String name, long created) {
		ContentValues values = new ContentValues();
		values.put(FolderColumns.CREATED, created);
		values.put(FolderColumns.NAME, name);

		long rowId;
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		rowId = db.insert(FOLDER_TABLE_NAME, FolderColumns._ID, values);

		if (rowId > 0) {
			FolderInfo fi = new FolderInfo();
			fi.id = rowId;
			fi.name = name;
			return fi;
		}

		throw new SQLException(String.format("Failed to insert folder '%s'.",
				name));
	}

	/**
	 * Updates folder's information.
	 * 
	 * @param folderID
	 *            Primary key of folder.
	 * @param name
	 *            New name for the folder.
	 */
	public void updateFolder(long folderID, String name) {
		ContentValues values = new ContentValues();
		values.put(FolderColumns.NAME, name);

		SQLiteDatabase db = null;
		db = mOpenHelper.getWritableDatabase();
		db.update(FOLDER_TABLE_NAME, values,
				FolderColumns._ID + "=" + folderID, null);
	}

	/**
	 * Deletes given folder.
	 * 
	 * @param folderID
	 *            Primary key of folder.
	 */
	public void deleteFolder(long folderID) {
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		// delete all puzzles in folder we are going to delete
		db.delete(CROSSWORD_TABLE_NAME, CrosswordColumns.FOLDER_ID + "="
				+ folderID, null);
		// delete the folder
		db.delete(FOLDER_TABLE_NAME, FolderColumns._ID + "=" + folderID, null);
	}

	/**
	 * Returns list of puzzles in the given folder.
	 * 
	 * @param folderID
	 *            Primary key of folder.
	 * @return
	 */
	public Cursor getCrosswordList(long folderID) {
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

		qb.setTables(CROSSWORD_TABLE_NAME);
		qb.appendWhere(CrosswordColumns.FOLDER_ID + "=" + folderID);

		SQLiteDatabase db = mOpenHelper.getReadableDatabase();
		return qb.query(db, null, null, null, null, null, "created DESC");
	}

	/**
	 * Returns crossword game object.
	 * 
	 * @param crosswordID
	 *            Primary key of folder.
	 * @return
	 */
	public CrosswordGame getCrossword(long crosswordID) {
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

		qb.setTables(CROSSWORD_TABLE_NAME);
		qb.appendWhere(CrosswordColumns._ID + "=" + crosswordID);

		// Get the database and run the query

		SQLiteDatabase db = null;
		Cursor c = null;
		CrosswordGame s = null;
		try {
			db = mOpenHelper.getReadableDatabase();
			c = qb.query(db, null, null, null, null, null, null);

			if (c.moveToFirst()) {
				long id = c.getLong(c.getColumnIndex(CrosswordColumns._ID));
				long created = c.getLong(c
						.getColumnIndex(CrosswordColumns.CREATED));
				String data = c.getString(c
						.getColumnIndex(CrosswordColumns.DATA));
				long lastPlayed = c.getLong(c
						.getColumnIndex(CrosswordColumns.LAST_PLAYED));
				int state = c.getInt(c.getColumnIndex(CrosswordColumns.STATE));
				long time = c.getLong(c.getColumnIndex(CrosswordColumns.TIME));
				String title = c.getString(c
						.getColumnIndex(CrosswordColumns.TITLE));

				s = new CrosswordGame();
				s.setId(id);
				s.setTitle(title);
				s.setCreated(created);
				s.setGrid(Grid.deserialize(data));
				s.setLastPlayed(lastPlayed);
				s.setState(state);
				s.setTime(time);
			}
		} finally {
			if (c != null)
				c.close();
		}

		return s;

	}

	/**
	 * Inserts new puzzle into the database.
	 * 
	 * @param folderID
	 *            Primary key of the folder in which puzzle should be saved.
	 * @param crossword
	 * @return
	 */
	public long insertCrossword(long folderID, CrosswordGame crossword) {
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put(CrosswordColumns.DATA, crossword.getGrid().serialize());
		values.put(CrosswordColumns.CREATED, crossword.getCreated());
		values.put(CrosswordColumns.LAST_PLAYED, crossword.getLastPlayed());
		values.put(CrosswordColumns.STATE, crossword.getState());
		values.put(CrosswordColumns.TIME, crossword.getTime());
		values.put(CrosswordColumns.FOLDER_ID, folderID);
		values.put(CrosswordColumns.TITLE, crossword.getTitle());

		long rowId = db
				.insert(CROSSWORD_TABLE_NAME, FolderColumns.NAME, values);
		if (rowId > 0) {
			return rowId;
		}

		throw new SQLException("Failed to insert crossword.");
	}

	private SQLiteStatement mInsertCrosswordStatement;

	public long importCrossword(long folderID, CrosswordImportParams pars)
			throws CrosswordInvalidFormatException {
		if (pars.data == null) {
			throw new CrosswordInvalidFormatException(pars.data);
		}

		if (!Grid.isValid(pars.data, Grid.DATA_VERSION_PLAIN)) {
			if (!Grid.isValid(pars.data, Grid.DATA_VERSION_1)) {
				throw new CrosswordInvalidFormatException(pars.data);
			}
		}

		if (mInsertCrosswordStatement == null) {
			SQLiteDatabase db = mOpenHelper.getWritableDatabase();
			mInsertCrosswordStatement = db
					.compileStatement("insert into crossword (folder_id, created, state, time, last_played, data, title) values (?, ?, ?, ?, ?, ?, ?)");
		}

		mInsertCrosswordStatement.bindLong(1, folderID);
		mInsertCrosswordStatement.bindLong(2, pars.created);
		mInsertCrosswordStatement.bindLong(3, pars.state);
		mInsertCrosswordStatement.bindLong(4, pars.time);
		mInsertCrosswordStatement.bindLong(5, pars.lastPlayed);
		mInsertCrosswordStatement.bindString(6, pars.data);
		mInsertCrosswordStatement.bindString(7, pars.title);

		long rowId = mInsertCrosswordStatement.executeInsert();
		if (rowId > 0) {
			return rowId;
		}

		throw new SQLException("Failed to insert crossword.");
	}

	/**
	 * Updates crossword game in the database.
	 * 
	 * @param crossword
	 */
	public void updateCrossword(CrosswordGame crossword) {
		ContentValues values = new ContentValues();
		values.put(CrosswordColumns.DATA, crossword.getGrid().serialize());
		values.put(CrosswordColumns.LAST_PLAYED, crossword.getLastPlayed());
		values.put(CrosswordColumns.STATE, crossword.getState());
		values.put(CrosswordColumns.TIME, crossword.getTime());

		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		db.update(CROSSWORD_TABLE_NAME, values, CrosswordColumns._ID + "="
				+ crossword.getId(), null);
	}

	/**
	 * Deletes given crossword from the database.
	 * 
	 * @param crosswordID
	 */
	public void deleteCrossword(long crosswordID) {
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		db.delete(CROSSWORD_TABLE_NAME, CrosswordColumns._ID + "="
				+ crosswordID, null);
	}

	public void close() {
		if (mInsertCrosswordStatement != null) {
			mInsertCrosswordStatement.close();
		}

		mOpenHelper.close();
	}

	public void beginTransaction() {
		mOpenHelper.getWritableDatabase().beginTransaction();
	}

	public void setTransactionSuccessful() {
		mOpenHelper.getWritableDatabase().setTransactionSuccessful();
	}

	public void endTransaction() {
		mOpenHelper.getWritableDatabase().endTransaction();
	}
}
