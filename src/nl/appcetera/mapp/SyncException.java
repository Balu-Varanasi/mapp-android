package nl.appcetera.mapp;

/**
 * Dit is een uitbereiding van de exception-klasse, waardoor we kunnen differenti‘ren tussen het afvangen normale- en SyncExceptions
 * @author Mathijs
 */

public class SyncException extends Exception
{
	
	private static final long serialVersionUID = 4152629157072575593L;

	public SyncException(String message)
	{
		super(message);
	}
}
