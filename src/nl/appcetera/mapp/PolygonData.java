package nl.appcetera.mapp;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.provider.BaseColumns;

/**
 * Polygon data management
 * @author Mathijs
 *
 */
public class PolygonData extends SQLiteOpenHelper
{
	private static final String DATABASE_NAME = "mapp.db";
	private static final int DATABASE_VERSION = 1;
	
	public static final String POLYGON_TABLE_NAME 	= "polygondata";
	public static final String _ID 					= BaseColumns._ID;
	public static final String COLOR 				= "color";
	public static final String LAST_EDITED			= "last_edited";
	
	public static final String POLYGON_POINTS_TABLE_NAME 	= "polygon_points";
	public static final String POLYGON_ID 					= "polygon_id";
	public static final String POLYGON_X 					= "coord_x";
	public static final String POLYGON_Y 					= "coord_y";
	public static final String POLYGON_POINTS_ORDERING		= "ordering";
	
	public PolygonData(Context context, String name, CursorFactory factory,
			int version)
	{
		super(context, DATABASE_NAME, factory, DATABASE_VERSION);
	}

	/**
	 * Maakt de tabellen als ze nog niet bestaan
	 */
	@Override
	public void onCreate(SQLiteDatabase db)
	{
		  String sql =
		    "CREATE TABLE " + POLYGON_TABLE_NAME + " ("
		      + _ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
		      + COLOR + " TEXT NOT NULL, "
		      + LAST_EDITED + " INTEGER"
		      + ");";
		 
		  db.execSQL(sql);
		  
		  String sql2 =
			    "CREATE TABLE " + POLYGON_POINTS_TABLE_NAME + " ("
			      + POLYGON_ID + " INTEGER PRIMARY KEY, "
			      + POLYGON_X + " INTEGER, "
			      + POLYGON_Y + " INTEGER, "
			      + POLYGON_POINTS_ORDERING + " INTEGER, "
			      + "FOREIGN KEY(" + POLYGON_ID + ") REFERENCES " + POLYGON_TABLE_NAME 
			      + "(" + _ID + ")"
			      + ");";
			 
		  db.execSQL(sql2);
	}

	/**
	 * Upgrade de database als er een nieuwer versienummer is
	 * Momenteel door de oude te verwijderen en hem opnieuw aan te maken,
	 * misschien een handigere manier? TODO
	 */
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
	{
		db.execSQL("DROP TABLE IF EXISTS " + POLYGON_TABLE_NAME);
		db.execSQL("DROP TABLE IF EXISTS " + POLYGON_POINTS_TABLE_NAME);
		onCreate(db);
	}
	
	/**
	 * Maakt een nieuwe polygoon aan in de database en geeft het id terug
	 * @param color de kleur van de polygoon als int (voor Java.Color)
	 * @return het id dat de polygoon gekregen heeft
	 */
	public int addPolygon(int color)
	{
		SQLiteDatabase db = getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put(COLOR, color);
		values.put(LAST_EDITED, System.currentTimeMillis()/1000);
		db.insertOrThrow(POLYGON_TABLE_NAME, null, values);
		
		Cursor c = db.rawQuery("SELECT last_insert_rowid() FROM " + POLYGON_TABLE_NAME, null);
		return c.getInt(0);		
	}
	
	/**
	 * Bewerkt de polygoon met het gegeven id
	 * @param polygonid het id van de polygoon
	 * @param color de kleur van de polygoon, als integer
	 * @param isClosed of de polygoon gesloten is of niet
	 */
	public void editPolygon(int polygonid, int color)
	{
		SQLiteDatabase db = getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put(COLOR, color);
		values.put(LAST_EDITED, System.currentTimeMillis()/1000);
		db.update(POLYGON_TABLE_NAME, values, "WHERE " + _ID + "=" + polygonid, null);
	}
	
	/**
	 * Geeft alle polygonen terug, gesorteerd op bewerkdatum
	 * @return een cursor met alle polygonen, gesorteerd op bewerkdatum
	 */
	public Cursor getAllPolygons()
	{
		SQLiteDatabase db = getReadableDatabase();
		Cursor c = db.query(POLYGON_TABLE_NAME, new String[]{_ID, COLOR}, null, null, null, null, LAST_EDITED);
		return c;
	}
	
	/**
	 * Verwijder de polygoon met het gegeven id
	 * @param polygonid het id van de te verwijderen polygoon
	 */
	public void removePolygon(int polygonid)
	{
		SQLiteDatabase db = getWritableDatabase();
		db.delete(POLYGON_TABLE_NAME, "WHERE " + _ID + "=" + polygonid, null);
	}
	
	/**
	 * Voegt een punt toe aan een polygoon op gegeven index
	 * @param polygonid id van de polygoon waar het punt aan toegevoegd wordt
	 * @param x positie van het punt
	 * @param y positie van het punt
	 * @param ordering index van het punt, waarbij 0 het startpunt is
	 */
	public void addPolygonPoint(int polygonid, long x, long y, int ordering)
	{
		SQLiteDatabase db = getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put(POLYGON_ID, polygonid);
		values.put(POLYGON_X, x);
		values.put(POLYGON_Y, y);
		values.put(POLYGON_POINTS_ORDERING, ordering);
		db.insertOrThrow(POLYGON_POINTS_TABLE_NAME, null, values);
	}

}
