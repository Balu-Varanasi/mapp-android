package nl.appcetera.mapp;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

/**
 * Polygon data management
 * @author Mathijs
 *
 */
public class PolygonData extends SQLiteOpenHelper
{
	private static final String DATABASE_NAME = "mapp.db";
	private static final int DATABASE_VERSION = 15;
	
	private static final String POLYGON_TABLE_NAME 	= "polygondata";
	private static final String POLYGON_ID 			= BaseColumns._ID;
	private static final String POLYGON_SERVER_ID	= "server_id";
	private static final String POLYGON_COLOR 		= "color";
	private static final String POLYGON_LAST_EDITED	= "last_edited";
	private static final String POLYGON_CLOSED		= "is_closed";
	private static final String POLYGON_GROUP		= "groupid";
	private static final String POLYGON_IS_NEW		= "new";
	private static final String POLYGON_HAS_CHANGED = "changed";
	private static final String POLYGON_NAME		= "name";
	private static final String POLYGON_DESCRIPTION = "description";
	
	private static final String POLYGON_POINTS_TABLE_NAME 	= "polygon_points";
	private static final String POLYGON_POINTS_ID			= "polygon_id";
	private static final String POLYGON_POINTS_X 			= "coord_x";
	private static final String POLYGON_POINTS_Y 			= "coord_y";
	private static final String POLYGON_POINTS_ORDERING		= "ordering";
	
	private static final String GROUPS_TABLE_NAME	= "groups";
	private static final String GROUPS_ID			= "groupid";
	private static final String GROUPS_OWNER		= "owner";
	private static final String GROUPS_NAME			= "group_name";
	
	private static final String GROUP_MEMBERS_TABLE_NAME = "group_members";
	private static final String GROUP_MEMBERS_ACCEPTED   = "accepted";
	
	private static final String USERS_TABLE_NAME = "users";
	private static final String USERS_ID 		 = "userid";
	private static final String USERS_EMAIL 	 = "email";
	
	private static final String POLYGON_REMOVAL_TABLE_NAME = "removed_polygons";

	public PolygonData(Context context)
	{
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
		//onUpgrade(getWritableDatabase(), 1, 2); // Handig tijdens debuggen
	}

	/**
	 * Maakt de tabellen als ze nog niet bestaan
	 */
	@Override
	public void onCreate(SQLiteDatabase db)
	{
		String sql = "";
		
		sql =
			"CREATE TABLE " + USERS_TABLE_NAME + " ("
			+ USERS_ID + " INTEGER NOT NULL, "
			+ USERS_EMAIL + " TEXT NOT NULL "
			+ ");";
		db.execSQL(sql);
		
		sql =
		    "CREATE TABLE " + GROUPS_TABLE_NAME + " ("
		      + GROUPS_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
		      + GROUPS_OWNER + " TEXT NOT NULL, "
		      + GROUPS_NAME + " TEXT"
		      + ");";
		db.execSQL(sql);
		db.execSQL("INSERT INTO " + GROUPS_TABLE_NAME + " (" + GROUPS_NAME + "," + GROUPS_OWNER + ") VALUES ('Default','info@mathijsvos.nl')");
		
		sql =
			"CREATE TABLE " + GROUP_MEMBERS_TABLE_NAME + " ("
				+ GROUPS_ID + " INTEGER NOT NULL, "
				+ USERS_ID + " INTEGER NOT NULL, "
				+ GROUP_MEMBERS_ACCEPTED + " INTEGER NOT NULL, "
				+ "FOREIGN KEY(" + GROUPS_ID + ") REFERENCES " + GROUPS_TABLE_NAME 
			      + "(" + GROUPS_ID + ") ON UPDATE CASCADE ON DELETE CASCADE, "
			    + "FOREIGN KEY(" + USERS_ID + ") REFERENCES " + USERS_TABLE_NAME 
			      + "(" + USERS_ID + ") ON UPDATE CASCADE ON DELETE CASCADE"
				+ ");";
		db.execSQL(sql);
		
		sql =
		    "CREATE TABLE " + POLYGON_TABLE_NAME + " ("
		      + POLYGON_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
		      + POLYGON_SERVER_ID + " INTEGER NULL, "
		      + POLYGON_COLOR + " INTEGER NOT NULL, "
		      + POLYGON_LAST_EDITED + " INTEGER, "
		      + POLYGON_CLOSED + " INTEGER, "
		      + POLYGON_GROUP + " INTEGER NOT NULL, "
		      + POLYGON_NAME + " TEXT, "
		      + POLYGON_DESCRIPTION + " TEXT, "
		      + POLYGON_IS_NEW + " INTEGER NOT NULL, "
		      + POLYGON_HAS_CHANGED + " INTEGER NOT NULL, "
		      + "FOREIGN KEY(" + POLYGON_GROUP + ") REFERENCES " + GROUPS_TABLE_NAME 
		      + "(" + GROUPS_ID + ") ON UPDATE CASCADE ON DELETE CASCADE"
		      + ");";
		 
		db.execSQL(sql);
		  
		sql =
			"CREATE TABLE " + POLYGON_POINTS_TABLE_NAME + " ("
			  + POLYGON_POINTS_ID + " INTEGER, "
			  + POLYGON_POINTS_X + " INTEGER, "
			  + POLYGON_POINTS_Y + " INTEGER, "
			  + POLYGON_POINTS_ORDERING + " INTEGER, "
			  + "FOREIGN KEY(" + POLYGON_POINTS_ID + ") REFERENCES " + POLYGON_TABLE_NAME 
			  + "(" + POLYGON_ID + ") ON UPDATE CASCADE ON DELETE CASCADE"
			  + ");";
			 
		db.execSQL(sql);
		
		sql = 
			"CREATE TABLE " + POLYGON_REMOVAL_TABLE_NAME + " ("
			  + POLYGON_ID + " INTEGER NOT NULL, "
			  + POLYGON_GROUP + " INTEGER NOT NULL"
			  + ");";
		db.execSQL(sql);
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
		db.execSQL("DROP TABLE IF EXISTS " + GROUPS_TABLE_NAME);
		db.execSQL("DROP TABLE IF EXISTS " + POLYGON_REMOVAL_TABLE_NAME);
		db.execSQL("DROP TABLE IF EXISTS " + USERS_TABLE_NAME);
		db.execSQL("DROP TABLE IF EXISTS " + GROUP_MEMBERS_TABLE_NAME);
		onCreate(db);
	}
	
	/**
	 **************************************************************
	 * POLYGONS
	 **************************************************************
	 */
	
	/**
	 * Maakt een nieuwe polygoon aan in de database en geeft het id terug
	 * @param color de kleur van de polygoon als int (voor Java.Color)
	 * @return het id dat de polygoon gekregen heeft
	 */
	public synchronized int addPolygon(int color, boolean isClosed, int group)
	{
		SQLiteDatabase db = getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put(POLYGON_COLOR, color);
		values.put(POLYGON_LAST_EDITED, System.currentTimeMillis()/1000);
		values.put(POLYGON_CLOSED, isClosed == true ? 1 : 0);
		values.put(POLYGON_GROUP, group);
		values.put(POLYGON_IS_NEW, 1);
		values.put(POLYGON_HAS_CHANGED, 0);
		return (int) db.insertOrThrow(POLYGON_TABLE_NAME, null, values);
	}
	
	/**
	 * Bewerkt de polygoon met het gegeven id
	 * @param polygonid het id van de polygoon
	 * @param color de kleur van de polygoon, als integer
	 * @param isClosed of de polygoon gesloten is of niet
	 * @param name de naam van de polygoon
	 * @param description de beschrijving van de polygoon
	 */
	public synchronized void editPolygon(int polygonid, int color, boolean isClosed, String name, String description)
	{
		SQLiteDatabase db = getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put(POLYGON_COLOR, color);
		values.put(POLYGON_LAST_EDITED, System.currentTimeMillis()/1000);
		values.put(POLYGON_HAS_CHANGED, 1);
		values.put(POLYGON_CLOSED, (isClosed == true ? 1 : 0));
		values.put(POLYGON_NAME, name);
		values.put(POLYGON_DESCRIPTION, description);
		db.update(POLYGON_TABLE_NAME, values, POLYGON_ID + "=" + polygonid, null);
	}
	
	/**
	 * Bewerkt de polygoon met het gegeven id of voert hem in als ie nog niet bestaat. Verwijdert bovendien alle opgeslagen punten.
	 * @param polygonid het id van de polygoon
	 * @param group het id van de group waat de polygoon in hoort
	 * @param color de kleur van de polygoon, als integer
	 * @param name de naam van de polygoon
	 * @param description de beschrijving van de polygoon
	 */
	public synchronized void addPolygonFromServer(int polygonid, int group, int color, String name, String description, long created)
	{
		SQLiteDatabase db = getWritableDatabase();
		Cursor c = db.query(POLYGON_TABLE_NAME, new String[]{POLYGON_ID}, 
				POLYGON_ID + "=" + polygonid, null, null, null, null);
		
		if(c.getCount() > 0)
		{
			removePolygon(polygonid, false);
		}
		
		ContentValues values = new ContentValues();
		values.put(POLYGON_ID, polygonid);
		values.put(POLYGON_GROUP, group);
		values.put(POLYGON_COLOR, color);
		values.put(POLYGON_LAST_EDITED, created);
		values.put(POLYGON_CLOSED, 1);
		values.put(POLYGON_NAME, name);
		values.put(POLYGON_IS_NEW, 0);
		values.put(POLYGON_HAS_CHANGED, 0);
		values.put(POLYGON_DESCRIPTION, description);
		db.insertOrThrow(POLYGON_TABLE_NAME, null, values);
	}
	
	/**
	 * Geeft alle polygonen terug, gesorteerd op bewerkdatum
	 * @return een cursor met alle polygonen, gesorteerd op bewerkdatum
	 */
	public synchronized Cursor getAllPolygons(int group)
	{
		SQLiteDatabase db = getReadableDatabase();
		Cursor c = db.query(POLYGON_TABLE_NAME, new String[]{POLYGON_ID, POLYGON_COLOR, POLYGON_CLOSED, POLYGON_IS_NEW, POLYGON_NAME, POLYGON_DESCRIPTION}, 
				POLYGON_GROUP + "=" + group, null, null, null, POLYGON_LAST_EDITED);
		return c;
	}
	
	/**
	 * Geeft alle nieuwe, nog niet gesynchroniseerde polygonen terug
	 * @param group het groepnummer waar polygonen uit gesynct moeten worden
	 * @return een cursor met alle nieuwe polygonen
	 */
	public synchronized Cursor getNewPolygons(int group)
	{
		SQLiteDatabase db = getReadableDatabase();
		Cursor c = db.query(POLYGON_TABLE_NAME, new String[]{POLYGON_ID, POLYGON_COLOR, POLYGON_NAME, POLYGON_DESCRIPTION}, 
				POLYGON_GROUP + "=" + group + " AND " + POLYGON_IS_NEW + "=1" + " AND " + POLYGON_CLOSED + "=1", null, null, null, null);
		return c;
	}
	
	/**
	 * Geeft het aantal polygonen in de gegeven groep terug
	 * @param group het id van de group
	 * @return het aantal polygonen in de groep
	 */
	public synchronized int getNumPolygons(int group)
	{
		SQLiteDatabase db = getReadableDatabase();
		Cursor c = db.query(POLYGON_TABLE_NAME, new String[]{POLYGON_ID}, 
				POLYGON_GROUP + "=" + group, null, null, null, null);
		return c.getCount();
	}
	
	/**
	 * Geeft alle polygonen in de gegeven groep die zijn gewijzigd sinds het gegeven tijdstip
	 * @param group het id van de groep waaruit we willen lezen
	 * @param lastSync het tijdstip vanaf wanneer polygonen gewijzigd moeten zijn om terug gegeven te worden
	 * @return een Cursor met de polygonen
	 */
	public synchronized Cursor getChangedPolygons(int group)
	{
		SQLiteDatabase db = getReadableDatabase();
		Cursor c = db.query(POLYGON_TABLE_NAME, new String[]{POLYGON_ID, POLYGON_COLOR, POLYGON_NAME, POLYGON_DESCRIPTION}, 
				POLYGON_GROUP + "=" + group + " AND " + POLYGON_IS_NEW + "=0" + " " +
				"AND " + POLYGON_HAS_CHANGED + "=1", null, null, null, null);
		return c;
	}
	
	/**
	 * Geeft aan dat de polygoon gesynct is en dus niet meer nieuw/gewijzigd is
	 * @param polygonid het id van de polygoon
	 */
	public synchronized void setPolygonIsSynced(int polygonid)
	{
		SQLiteDatabase db = getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put(POLYGON_IS_NEW, 0);
		values.put(POLYGON_HAS_CHANGED, 0);
		db.update(POLYGON_TABLE_NAME, values, POLYGON_ID + "=" + polygonid, null);
	}
	
	/**
	 * Geeft een polygoon een nieuw id
	 * @param oldid het oude id
	 * @param newid het nieuwe id
	 */
	public synchronized void updatePolygonId(int oldid, int newid)
	{
		SQLiteDatabase db = getWritableDatabase();
		
		// Als er al een polygoon bestaat met het nieuwe id, dan moet die even een ander id krijgen
		Cursor c = db.query(POLYGON_TABLE_NAME, new String[]{POLYGON_ID}, POLYGON_ID + "=" + newid, null, null, null, null);
		if(c.getCount() > 0)
		{
			updatePolygonId(newid, newid+1);
		} // TODO polygoon een serverid geven, en normale id vervangen door serverid
		// om te voorkomen dat er dubbele ids komen
		
		ContentValues values = new ContentValues();
		values.put(POLYGON_ID, newid);
		db.update(POLYGON_TABLE_NAME, values, POLYGON_ID + "=" + oldid, null);
		
		// Maar ook van de punten!
		// Want ja, dat kostte Mathijs 2 uur om dat te bedenken!
		values = new ContentValues();
		values.put(POLYGON_POINTS_ID, newid);
		db.update(POLYGON_POINTS_TABLE_NAME, values, POLYGON_POINTS_ID + "=" + oldid, null);
	}
	
	/**
	 * Verwijder de polygoon en zijn punten met het gegeven id
	 * @param polygonid het id van de te verwijderen polygoon
	 * @param local of de polygoon lokaal is verwijderd of op de server (in 't laatste geval niet op de synclijst zetten)
	 */
	public synchronized void removePolygon(int polygonid, boolean local)
	{
		SQLiteDatabase db = getWritableDatabase();
		
		// Groep opvragen
		Cursor c = db.query(POLYGON_TABLE_NAME, new String[]{POLYGON_GROUP}, 
				POLYGON_ID + "=" + polygonid, null, null, null, null);
		
		if(!c.moveToFirst())
		{
			return;
		}
		
		// Punten verwijderen
		removePolygonPoints(polygonid);
		
		// Polygoon verwijderen
		db.delete(POLYGON_TABLE_NAME, POLYGON_ID + "=" + polygonid, null);
		
		if(local)
		{
			// Toevoegen aan de verwijderlijst t.b.v. synchronisatie
			ContentValues values = new ContentValues();
			values.put(POLYGON_ID, polygonid);
			values.put(POLYGON_GROUP, c.getInt(0));
			db.insertOrThrow(POLYGON_REMOVAL_TABLE_NAME, null, values);
		}
	}
	
	/**
	 * Verwijdert alle polygonen uit een groep
	 * @param group het id van de groep waar polygonen uit verwijdert moeten worden
	 * @param local geeft aan of de verwijdering lokaal of op de server gebeurde
	 */
	public synchronized void removePolygonsFromGroup(int group, boolean local)
	{
		Cursor c = getAllPolygons(group);
		if(!c.moveToFirst())
		{
			return; // Lege groep dus niks te verwijderen
		}
		
		do
		{
			removePolygon(c.getInt(0), local);
		}
		while(c.moveToNext());
	}
	
	/**
	 * Geeft alle te verwijderen polygonen terug
	 * @return een Cursor met alle te verwijderen polygonen
	 */
	public synchronized Cursor getRemovedPolygons(int group)
	{
		SQLiteDatabase db = getReadableDatabase();
		Cursor c = db.query(POLYGON_REMOVAL_TABLE_NAME, new String[]{POLYGON_ID}, POLYGON_GROUP + "=" + group, null, null, null, null);
		return c;
	}
	
	/**
	 * Interessante functienaam, minder interessante functie, haalt een polygoon uit de verwijderlijst
	 * @param polygonid het id van de te verwijderen polygoon
	 */
	public synchronized void removeRemovedPolygon(int polygonid)
	{
		SQLiteDatabase db = getWritableDatabase();
		db.delete(POLYGON_REMOVAL_TABLE_NAME, POLYGON_ID + "=" + polygonid, null);
	}
	
	/**
	 * Voegt een punt toe aan een polygoon op gegeven index
	 * @param polygonid id van de polygoon waar het punt aan toegevoegd wordt
	 * @param x positie van het punt (latitude)
	 * @param y positie van het punt (longtitude)
	 * @param ordering index van het punt, waarbij 0 het startpunt is
	 * @param local of deze punttoevoeging lokaal gedaan is of van de server komt
	 */
	public synchronized void addPolygonPoint(int polygonid, long x, long y, int ordering, boolean local)
	{
		SQLiteDatabase db = getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put(POLYGON_POINTS_ID, polygonid);
		values.put(POLYGON_POINTS_X, x);
		values.put(POLYGON_POINTS_Y, y);
		values.put(POLYGON_POINTS_ORDERING, ordering);
		db.insertOrThrow(POLYGON_POINTS_TABLE_NAME, null, values);

		// Laatst-bewerkt datum bijwerken
		values = new ContentValues();
		
		if(local)
		{
			values.put(POLYGON_HAS_CHANGED, 1);
		}
		values.put(POLYGON_LAST_EDITED, System.currentTimeMillis()/1000);
		db.update(POLYGON_TABLE_NAME, values, POLYGON_ID + "=" + polygonid, null);
	}
	
	/**
	 * Wijzig de coördinaten van een punt
	 * @param polygonid het id van de polygoon waar het te wijzigen punt bij hoort
	 * @param x nieuwe positie van het punt (latitude)
	 * @param y nieuwe positie van het punt (longtitude)
	 * @param ordering (nieuwe) index van het te wijzigen punt
	 */
	public synchronized void editPolygonPoint(int polygonid, long x, long y, int ordering)
	{
		SQLiteDatabase db = getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put(POLYGON_POINTS_X, x);
		values.put(POLYGON_POINTS_Y, y);
		values.put(POLYGON_POINTS_ORDERING, ordering);
		db.update(POLYGON_POINTS_TABLE_NAME, values, POLYGON_POINTS_ID + " = " 
				+ polygonid	+ " AND " + POLYGON_POINTS_ORDERING + " = " + ordering, null);

		// Laatst-bewerkt datum bijwerken
		values = new ContentValues();
		values.put(POLYGON_HAS_CHANGED, 1);
		values.put(POLYGON_LAST_EDITED, System.currentTimeMillis()/1000);
		db.update(POLYGON_TABLE_NAME, values, POLYGON_ID + "=" + polygonid, null);
	}
	
	/**
	 * Verwijder een gegeven punt uit een polygoon
	 * @param polygonid het id van de polygoon waar een punt uit moet
	 * @param ordering de index van het te verwijderen punt
	 */
	public synchronized void removePolygonPoint(int polygonid, int ordering)
	{
		SQLiteDatabase db = getWritableDatabase();
		db.delete(POLYGON_POINTS_TABLE_NAME, POLYGON_POINTS_ID + " = " 
				+ polygonid	+ " AND " + POLYGON_POINTS_ORDERING + " = " + ordering, null);
		
		// Laatst-bewerkt datum bijwerken
		ContentValues values = new ContentValues();
		values.put(POLYGON_HAS_CHANGED, 1);
		values.put(POLYGON_LAST_EDITED, System.currentTimeMillis()/1000);
		db.update(POLYGON_TABLE_NAME, values, POLYGON_ID + "=" + polygonid, null);
	}

	/**
	 * Geeft alle bij een polygoon behorende punten terug, op juiste wijze gesorteerd
	 * @param polygonid het id van de polygoon waar je de punten bij wilt hebben
	 * @return een cursor met alle polygoonpunten
	 */
	public synchronized Cursor getAllPolygonPoints(int polygonid)
	{
		SQLiteDatabase db = getReadableDatabase();
		Cursor c = db.query(POLYGON_POINTS_TABLE_NAME, 
				new String[]{POLYGON_POINTS_X, POLYGON_POINTS_Y, POLYGON_POINTS_ORDERING},
				POLYGON_POINTS_ID + " = " + polygonid, null, null, null, POLYGON_POINTS_ORDERING);
		return c;
	}
	
	/**
	 * Verandert de volgorde van een groepje polygoonpunten, bijvoorbeeld handig
	 * als je tussenin een punt wilt toevoegen of verwijderen
	 * @param polygonid het id van de polygoon waar de punten bij horen
	 * @param index alle punten groter dan of gelijk aan deze index worden verplaatst
	 * @param diff het verschil waarmee je de punten wilt verplaatsen, 1 voor alle indices 1 omhoog, -1 voor alle indices 1 omlaag
	 */
	public synchronized void movePolygonPointsIndexes(int polygonid, int index, int diff)
	{
		SQLiteDatabase db = getWritableDatabase();
		db.execSQL("UPDATE " + POLYGON_POINTS_TABLE_NAME + " SET " + POLYGON_POINTS_ORDERING
				+ "=(" + POLYGON_POINTS_ORDERING + "+" + diff + ") WHERE "
				+ POLYGON_POINTS_ORDERING + ">=" + index + " AND " + POLYGON_POINTS_ID
				+ "=" + polygonid);
	}
	
	/**
	 * Verwijder alle bij een polygoon behorende punten
	 * @param polygonid het id van de polygoon waar alle punten van weg moeten
	 */
	private synchronized void removePolygonPoints(int polygonid)
	{
		SQLiteDatabase db = getWritableDatabase();
		db.delete(POLYGON_POINTS_TABLE_NAME, POLYGON_POINTS_ID + " = " 
				+ polygonid, null);
	}
	
	
	/**
	 **************************************************************
	 * USERS/GROUPS
	 **************************************************************
	 */
	
	/**
	 * Voegt een nieuwe gebruiker toe
	 * @param id het id van de gebruiker
	 * @param email het e-mailadres van de gebruiker
	 */
	public synchronized void addUser(int id, String email)
	{
		SQLiteDatabase db = getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put(USERS_ID, id);
		values.put(USERS_EMAIL, email);
		db.insertOrThrow(USERS_TABLE_NAME, null, values);
	}
	
	/**
	 * Geeft de gevraagde gebruiker terug
	 * @param id het id van de gebruiker
	 * @return Cursor-object met gegevens over gebruiker
	 */
	public synchronized Cursor getUser(int id)
	{
		SQLiteDatabase db = getReadableDatabase();
		Cursor c = db.query(USERS_TABLE_NAME, new String[]{USERS_EMAIL}, USERS_ID + "=" + id, null, null, null, null);
		return c;
	}
	
	/**
	 * Verwijder de gegeven gebruiker
	 * @param id het id van de te verwijderen gebruiker
	 */
	public synchronized void deleteUser(int id)
	{
		SQLiteDatabase db = getWritableDatabase();
		db.delete(USERS_TABLE_NAME, USERS_ID + "=" + id, null);
	}

	/**
	 * Voegt een groep toe aan de database
	 * @param id het id van de groep
	 * @param owner het e-mailadres van de eigenaar van de groep
	 * @param name de naam van de groep
	 */
	public synchronized void addGroup(int id, String owner, String name)
	{
		SQLiteDatabase db = getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put(GROUPS_ID, id);
		values.put(GROUPS_OWNER, owner);
		values.put(GROUPS_NAME, name);
		db.insertOrThrow(GROUPS_TABLE_NAME, null, values);
	}

	/**
	 * Geeft info over een groep terug
	 * @param id het id van de groep
	 * @return een cursorobject met info over de groep
	 */
	public synchronized Cursor getGroup(int id)
	{
		SQLiteDatabase db = getReadableDatabase();
		Cursor c = db.query(GROUPS_TABLE_NAME, new String[]{GROUPS_OWNER, GROUPS_NAME}, GROUPS_ID + "=" + id, null, null, null, null);
		return c;
	}
	
	/**
	 * Verwijderd de gegeven groep
	 * @param id het id van de te verwijderen groep
	 */
	public synchronized void deleteGroup(int id)
	{
		SQLiteDatabase db = getWritableDatabase();
		db.delete(GROUPS_TABLE_NAME, GROUPS_ID + "=" + id, null);
	}
	
	/**
	 * Voegt een lidmaatschap toe van een gebruiker aan een groep
	 * @param user het id van de gebruiker
	 * @param group het id van de groep
	 * @param accepted reeds geaccepteerd ja/nee
	 */
	public synchronized void addMembership(int user, int group, boolean accepted)
	{
		SQLiteDatabase db = getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put(GROUPS_ID, group);
		values.put(USERS_ID, user);
		values.put(GROUP_MEMBERS_ACCEPTED, (accepted ? 1 : 0));
		db.insertOrThrow(GROUP_MEMBERS_TABLE_NAME, null, values);
	}
	
	/**
	 * Verwijderd een lidmaatschap
	 * @param user het id van de gebruiker
	 * @param group het id van de groep
	 */
	public synchronized void deleteMembership(int user, int group)
	{
		SQLiteDatabase db = getWritableDatabase();
		db.delete(GROUP_MEMBERS_TABLE_NAME, GROUPS_ID + "=" + group + " AND " + USERS_ID + "=" + user, null);
	}
	
	/**
	 * Geeft alle geaccepteerde memberships van de gegeven gebruiker terug
	 * @param user het id van de gebruiker
	 * @return een Cursorobject met alle groepid's van de gebruiker
	 */
	public synchronized Cursor getMemberShips(int user)
	{
		SQLiteDatabase db = getReadableDatabase();
		Cursor c = db.query(GROUP_MEMBERS_TABLE_NAME, new String[]{GROUPS_ID}, 
				USERS_ID + "=" + user + " AND " + GROUP_MEMBERS_ACCEPTED + "=1", null, null, null, null);
		return c;
	}
	
	/**
	 * Accepteer een membership voor de gegeven gebruiker + groep
	 * @param user het id van de gebruiker
	 * @param group het id van de groep
	 */
	public synchronized void acceptMembership(int user, int group)
	{
		SQLiteDatabase db = getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put(GROUPS_ID, group);
		values.put(USERS_ID, user);
		values.put(GROUP_MEMBERS_ACCEPTED, 1);
		db.update(GROUP_MEMBERS_TABLE_NAME, values, null, null);
	}
}
