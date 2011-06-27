package nl.appcetera.mapp;

import android.content.Context;
import android.widget.Button;

public class GroupButton extends Button {

	private int GroupID;
	private boolean isOwner;
	
	public GroupButton(Context context, int ID, boolean isOwner) {
		super(context);
		GroupID = ID;
		this.isOwner = isOwner;
	}
	
	public boolean isOwner() {
		return isOwner;
	}
	
	public int getID() {
		return GroupID;
	}
	
}
